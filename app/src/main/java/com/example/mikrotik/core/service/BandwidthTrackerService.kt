package com.example.mikrotik.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mikrotik.core.hotspot.HotspotController
import com.example.mikrotik.core.network.IptablesManager
import com.example.mikrotik.core.network.UsageCounterMath
import com.example.mikrotik.core.network.UsageMeasurementStore
import com.example.mikrotik.core.session.SessionTransitionLock
import com.example.mikrotik.data.db.dao.ActiveSessionDao
import com.example.mikrotik.data.db.dao.BlockedMacDao
import com.example.mikrotik.data.db.dao.VoucherDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BandwidthTrackerService : Service() {

    @Inject lateinit var activeSessionDao: ActiveSessionDao
    @Inject lateinit var voucherDao: VoucherDao
    @Inject lateinit var blockedMacDao: BlockedMacDao
    @Inject lateinit var iptablesManager: IptablesManager
    @Inject lateinit var hotspotController: HotspotController
    @Inject lateinit var sessionTransitionLock: SessionTransitionLock

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var trackingJob: Job? = null
    private var lastBlockedEnforcementAt = 0L
    private var cachedApInterface: String? = null
    private var lastApInterfaceLookupAt = 0L
    private var lastSourceTrafficSampleAt = 0L
    private var lastIptablesVerificationAt = 0L
    private var lastWakeLockRenewalAt = 0L
    private var trackingWakeLock: PowerManager.WakeLock? = null
    private val lastCounterDiagnosticAt = mutableMapOf<String, Long>()
    private val usagePreferences by lazy { getSharedPreferences("voucher_usage_counters", MODE_PRIVATE) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        acquireTrackingWakeLock()
        startTrackingJob()
    }

    private fun acquireTrackingWakeLock() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            trackingWakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MiniMikrotik:BandwidthTracker"
            ).apply { setReferenceCounted(false) }
            renewTrackingWakeLockIfNeeded()
        } catch (exception: Exception) {
            Log.e("BandwidthTracker", "Could not keep the tracking CPU awake", exception)
        }
    }

    private fun renewTrackingWakeLockIfNeeded() {
        val wakeLock = trackingWakeLock ?: return
        val now = SystemClock.elapsedRealtime()
        if (wakeLock.isHeld && now - lastWakeLockRenewalAt < WAKE_LOCK_RENEW_INTERVAL_MS) return
        try {
            if (wakeLock.isHeld) wakeLock.release()
            wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
            lastWakeLockRenewalAt = now
        } catch (exception: Exception) {
            Log.e("BandwidthTracker", "Could not renew the tracking wake lock", exception)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (trackingJob?.isActive != true) startTrackingJob()
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val channelId = "bandwidth_tracker_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Mini MikroTik Bandwidth Tracker",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Mini MikroTik Router Engine")
            .setContentText("تتم مراقبة استهلاك البطاقات وفصل الجلسات المنتهية تلقائياً")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1001, notification)
    }

    private fun startTrackingJob() {
        trackingJob = serviceScope.launch {
            while (isActive) {
                renewTrackingWakeLockIfNeeded()
                try {
                    trackBandwidthAndEnforceQuotas()
                } catch (exception: Exception) {
                    Log.e("BandwidthTracker", "Failed to update voucher usage", exception)
                }
                delay(250L)
            }
        }
    }

    private suspend fun trackBandwidthAndEnforceQuotas() {
        sampleSourceTrafficIfDue()
        enforceBlockedDevicesIfDue()
        val sessions = activeSessionDao.getActiveSessionsSnapshot()
            .distinctBy { it.macAddress.lowercase() }
        val apInterface = if (sessions.isNotEmpty()) getCachedApInterface() else null
        val nowElapsed = SystemClock.elapsedRealtime()
        val verifyWithIptables = lastIptablesVerificationAt == 0L ||
            nowElapsed - lastIptablesVerificationAt >= IPTABLES_VERIFICATION_INTERVAL_MS
        if (verifyWithIptables) lastIptablesVerificationAt = nowElapsed
        for (session in sessions) {
            sessionTransitionLock.lock()
            try {
            val currentSession = activeSessionDao.getSessionByMac(session.macAddress)
            if (currentSession == null ||
                !currentSession.isActive ||
                !currentSession.voucherCode.equals(session.voucherCode, ignoreCase = true) ||
                currentSession.ipAddress != session.ipAddress ||
                currentSession.connectedAt != session.connectedAt
            ) {
                Log.d("BandwidthTracker", "Skipping stale session snapshot for ${session.macAddress}")
                continue
            }

            val mac = session.macAddress
            if (blockedMacDao.isMacBlocked(mac)) {
                val matchingSessions = activeSessionDao.getSessionsByMac(mac)
                val macBlocked = iptablesManager.blockMacAddress(mac)
                val ipBlocked = matchingSessions.map { iptablesManager.blockClientIpAddress(it.ipAddress) }.all { it }
                val accessRevoked = iptablesManager.revokeMacAddress(mac)
                if (ipBlocked && accessRevoked && macBlocked) {
                    val voucherCodes = (
                        matchingSessions.map { it.voucherCode } + voucherDao.getVouchersByMac(mac).map { it.code }
                        ).distinct()
                    voucherCodes.forEach {
                        voucherDao.blockVoucherForDeletion(it)
                        voucherDao.deleteVoucher(it)
                    }
                    activeSessionDao.deactivateSessionsByMac(mac)
                    hotspotController.requestClientReauthentication(mac)
                } else {
                    Log.e(
                        "BandwidthTracker",
                        "Retrying device block because firewall rules were incomplete for $mac " +
                            "(ipBlocked=$ipBlocked, accessRevoked=$accessRevoked, macBlocked=$macBlocked)"
                    )
                }
                continue
            }

            var voucher = voucherDao.getVoucherByCode(session.voucherCode)
            if (voucher == null || voucher.isBlocked) {
                disconnectExpiredSession(session, "deleted or blocked voucher")
                continue
            }

            val wifiCounterBytes = apInterface?.let { iptablesManager.getWifiStationUsageBytes(mac, it) }
            val iptablesCounterBytes = if (wifiCounterBytes == null || verifyWithIptables) {
                iptablesManager.getDeviceUsageBytes(mac, session.ipAddress)
            } else {
                null
            }
            if (wifiCounterBytes != null || iptablesCounterBytes != null) {
                val identity = "${voucher.code}_${mac.lowercase().replace(':', '_')}"
                val initializedKey = "counter_v2_initialized_$identity"
                val wifiLastKey = "last_${identity}_wifi"
                val iptablesLastKey = "last_${identity}_iptables"
                val wifiTotalKey = "total_${identity}_wifi"
                val iptablesTotalKey = "total_${identity}_iptables"
                val wasInitialized = usagePreferences.getBoolean(initializedKey, false)
                val usageEditor = usagePreferences.edit()
                var addedBytes = 0L
                var reconciledUsage = voucher.bytesUsedSoFar

                if (!wasInitialized) {
                    // Establish both baselines together. Wi-Fi counters may include traffic
                    // from before voucher login; existing voucher usage is retained as base.
                    if (wifiCounterBytes != null) usageEditor.putLong(wifiLastKey, wifiCounterBytes)
                    if (iptablesCounterBytes != null) usageEditor.putLong(iptablesLastKey, iptablesCounterBytes)
                    usageEditor
                        .putLong(wifiTotalKey, voucher.bytesUsedSoFar)
                        .putLong(iptablesTotalKey, voucher.bytesUsedSoFar)
                        .putBoolean(initializedKey, true)
                } else {
                    var wifiTotal = usagePreferences.getLong(wifiTotalKey, voucher.bytesUsedSoFar)
                    var iptablesTotal = usagePreferences.getLong(iptablesTotalKey, voucher.bytesUsedSoFar)

                    if (wifiCounterBytes != null) {
                        val previous = usagePreferences.getLong(wifiLastKey, -1L)
                        val delta = UsageCounterMath.delta(previous, wifiCounterBytes)
                        wifiTotal = UsageCounterMath.saturatingAdd(wifiTotal, delta)
                        usageEditor.putLong(wifiLastKey, wifiCounterBytes)
                    }
                    if (iptablesCounterBytes != null) {
                        val previous = usagePreferences.getLong(iptablesLastKey, -1L)
                        val delta = UsageCounterMath.delta(previous, iptablesCounterBytes)
                        iptablesTotal = UsageCounterMath.saturatingAdd(iptablesTotal, delta)
                        usageEditor.putLong(iptablesLastKey, iptablesCounterBytes)
                    }

                    // The sources overlap, so use the higher cumulative estimate, never their sum.
                    reconciledUsage = UsageCounterMath.reconciledTotal(
                        voucher.bytesUsedSoFar,
                        wifiTotal,
                        iptablesTotal
                    )
                    addedBytes = reconciledUsage - voucher.bytesUsedSoFar
                    usageEditor
                        .putLong(wifiTotalKey, wifiTotal)
                        .putLong(iptablesTotalKey, iptablesTotal)
                }
                usageEditor.apply()

                val lastDiagnosticAt = lastCounterDiagnosticAt[identity] ?: 0L
                if (nowElapsed - lastDiagnosticAt >= COUNTER_DIAGNOSTIC_INTERVAL_MS) {
                    Log.i(
                        "BandwidthTracker",
                        "counterCheck voucher=${voucher.code} mac=$mac " +
                            "wifi=${wifiCounterBytes ?: "unavailable"} " +
                            "iptables=${iptablesCounterBytes ?: "not-sampled"} " +
                            "used=${maxOf(voucher.bytesUsedSoFar, reconciledUsage)} added=$addedBytes"
                    )
                    lastCounterDiagnosticAt[identity] = nowElapsed
                }

                if (addedBytes > 0L) {
                    val updatedVoucher = voucher.copy(
                        bytesUsedSoFar = reconciledUsage
                    )
                    voucherDao.updateVoucher(updatedVoucher)
                    voucher = updatedVoucher
                    UsageMeasurementStore.recordVoucherUsage(this@BandwidthTrackerService, addedBytes)
                }
            } else {
                Log.w("BandwidthTracker", "Could not read traffic counters for $mac; quota usage was not changed")
            }

            val now = System.currentTimeMillis()
            val firstUse = voucher.firstUsedAt
            val isDurationExpired = firstUse != null &&
                now >= firstUse + voucher.durationMinutes * 60_000L
            val isQuotaExceeded = voucher.bytesUsedSoFar >= voucher.dataLimitBytes

            if (isDurationExpired || isQuotaExceeded) {
                disconnectExpiredSession(
                    session,
                    if (isQuotaExceeded) "quota" else "duration"
                )
            }
            } finally {
                sessionTransitionLock.unlock()
            }
        }
    }

    private fun sampleSourceTrafficIfDue() {
        val now = SystemClock.elapsedRealtime()
        if (lastSourceTrafficSampleAt != 0L && now - lastSourceTrafficSampleAt < SOURCE_USAGE_SAMPLE_INTERVAL_MS) return
        lastSourceTrafficSampleAt = now
        if (!UsageMeasurementStore.sampleSourceTraffic(this)) {
            Log.d("BandwidthTracker", "Active upstream traffic counter is not available")
        }
    }

    private suspend fun getCachedApInterface(): String? {
        val now = SystemClock.elapsedRealtime()
        if (lastApInterfaceLookupAt == 0L || now - lastApInterfaceLookupAt >= AP_INTERFACE_REFRESH_INTERVAL_MS) {
            cachedApInterface = hotspotController.findActiveApInterface()
            lastApInterfaceLookupAt = now
        }
        return cachedApInterface
    }

    private suspend fun enforceBlockedDevicesIfDue() {
        val now = SystemClock.elapsedRealtime()
        if (lastBlockedEnforcementAt != 0L && now - lastBlockedEnforcementAt < BLOCKED_ENFORCEMENT_INTERVAL_MS) return
        lastBlockedEnforcementAt = now

        val blockedDevices = blockedMacDao.getBlockedMacsSnapshot()
            .distinctBy { it.macAddress.lowercase() }
        for (blocked in blockedDevices) {
            val mac = blocked.macAddress.trim().lowercase()
            val sessions = activeSessionDao.getSessionsByMac(mac)
            val macBlocked = iptablesManager.blockMacAddress(mac)
            val ipsBlocked = sessions.map { iptablesManager.blockClientIpAddress(it.ipAddress) }.all { it }
            val accessRevoked = iptablesManager.revokeMacAddress(mac)
            if (macBlocked && ipsBlocked && accessRevoked) {
                val voucherCodes = (
                    sessions.map { it.voucherCode } + voucherDao.getVouchersByMac(mac).map { it.code }
                    ).distinct()
                voucherCodes.forEach {
                    voucherDao.blockVoucherForDeletion(it)
                    voucherDao.deleteVoucher(it)
                }
                activeSessionDao.deactivateSessionsByMac(mac)
            } else {
                Log.e(
                    "BandwidthTracker",
                    "Could not re-apply blocked-device firewall rules for $mac " +
                        "(macBlocked=$macBlocked, ipsBlocked=$ipsBlocked, accessRevoked=$accessRevoked)"
                )
            }
        }
    }

    private suspend fun disconnectExpiredSession(
        session: com.example.mikrotik.data.db.entity.ActiveSessionEntity,
        reason: String
    ) {
        val mac = session.macAddress
        // Stop replies to existing flows first; then revoke access and drop new client traffic.
        val ipBlocked = iptablesManager.blockClientIpAddress(session.ipAddress)
        val revoked = iptablesManager.revokeMacAddress(mac)
        val macBlocked = iptablesManager.blockMacAddress(mac)
        if (!ipBlocked || !revoked || !macBlocked) {
            Log.e(
                "BandwidthTracker",
                "Could not fully revoke expired voucher access for $mac " +
                    "(ipBlocked=$ipBlocked, revoked=$revoked, macBlocked=$macBlocked)"
            )
        }

        activeSessionDao.deactivateSession(mac)
        activeSessionDao.deleteSession(mac)
        // Keep the Wi-Fi association intact. Deauthing here makes some client OSes wait
        // for a manual Wi-Fi reconnect before they run captive-portal detection again.
        // The revoked MAC/IP firewall rules still block Internet, while unauthenticated
        // HTTP requests are redirected to the local login portal.
        Log.i(
            "BandwidthTracker",
            "Expired voucher blocked ($reason) for $mac; keeping Wi-Fi association for captive-portal recheck"
        )
    }

    override fun onDestroy() {
        trackingJob?.cancel()
        serviceScope.cancel()
        trackingWakeLock?.let { wakeLock ->
            if (wakeLock.isHeld) wakeLock.release()
        }
        trackingWakeLock = null
        super.onDestroy()
    }

    private companion object {
        const val BLOCKED_ENFORCEMENT_INTERVAL_MS = 5_000L
        const val AP_INTERFACE_REFRESH_INTERVAL_MS = 5_000L
        const val SOURCE_USAGE_SAMPLE_INTERVAL_MS = 5_000L
        const val IPTABLES_VERIFICATION_INTERVAL_MS = 2_000L
        const val COUNTER_DIAGNOSTIC_INTERVAL_MS = 30_000L
        const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1_000L
        const val WAKE_LOCK_RENEW_INTERVAL_MS = 5 * 60 * 1_000L
    }
}
