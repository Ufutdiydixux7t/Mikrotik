package com.example.mikrotik.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mikrotik.core.hotspot.HotspotController
import com.example.mikrotik.core.network.IptablesManager
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
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class BandwidthTrackerService : Service() {

    @Inject lateinit var activeSessionDao: ActiveSessionDao
    @Inject lateinit var voucherDao: VoucherDao
    @Inject lateinit var blockedMacDao: BlockedMacDao
    @Inject lateinit var iptablesManager: IptablesManager
    @Inject lateinit var hotspotController: HotspotController

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var trackingJob: Job? = null
    private var lastBlockedEnforcementAt = 0L
    private var cachedApInterface: String? = null
    private var lastApInterfaceLookupAt = 0L
    private val usagePreferences by lazy { getSharedPreferences("voucher_usage_counters", MODE_PRIVATE) }
    private val clientUsageHistory by lazy { getSharedPreferences(CLIENT_USAGE_HISTORY_PREFS, MODE_PRIVATE) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        startTrackingJob()
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
        enforceBlockedDevicesIfDue()
        val sessions = activeSessionDao.getActiveSessionsSnapshot()
            .distinctBy { it.macAddress.lowercase() }
        val apInterface = if (sessions.isNotEmpty()) getCachedApInterface() else null
        for (session in sessions) {
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
            val counterSource = if (wifiCounterBytes != null) "wifi" else "iptables"
            val counterBytes = wifiCounterBytes ?: iptablesManager.getDeviceUsageBytes(mac, session.ipAddress)
            if (counterBytes != null) {
                markClientUsageAvailable()
                val identity = "${voucher.code}_${mac.lowercase().replace(':', '_')}"
                val legacyCounterKey = "last_$identity"
                val counterKey = "${legacyCounterKey}_$counterSource"
                val sourceMarkerKey = "source_$identity"
                val previousSource = usagePreferences.getString(sourceMarkerKey, null)
                val previousBytes = when {
                    previousSource == counterSource -> usagePreferences.getLong(counterKey, -1L)
                    previousSource == null && counterSource == "iptables" ->
                        usagePreferences.getLong(legacyCounterKey, -1L)
                    else -> -1L
                }
                val baselineOnly = (previousSource == null && counterSource == "wifi") ||
                    (previousSource != null && previousSource != counterSource)
                val addedBytes = when {
                    previousBytes < 0L -> if (baselineOnly) 0L else counterBytes
                    counterBytes >= previousBytes -> counterBytes - previousBytes
                    else -> counterBytes // The station or firewall counters were reset.
                }
                usagePreferences.edit()
                    .putLong(counterKey, counterBytes)
                    .putString(sourceMarkerKey, counterSource)
                    .apply()

                if (addedBytes > 0L) {
                    val nextUsage = if (Long.MAX_VALUE - voucher.bytesUsedSoFar < addedBytes) {
                        Long.MAX_VALUE
                    } else {
                        voucher.bytesUsedSoFar + addedBytes
                    }
                    val updatedVoucher = voucher.copy(
                        bytesUsedSoFar = nextUsage
                    )
                    voucherDao.updateVoucher(updatedVoucher)
                    voucher = updatedVoucher
                    recordClientUsage(addedBytes)
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

    private fun markClientUsageAvailable() {
        if (!clientUsageHistory.getBoolean(CLIENT_USAGE_TRACKING_AVAILABLE, false)) {
            clientUsageHistory.edit().putBoolean(CLIENT_USAGE_TRACKING_AVAILABLE, true).apply()
        }
    }

    private fun recordClientUsage(bytes: Long) {
        if (bytes <= 0L) return
        val dateKey = "$CLIENT_USAGE_DAY_KEY_PREFIX${LocalDate.now(ZoneId.systemDefault())}"
        val previous = clientUsageHistory.getLong(dateKey, 0L)
        val total = if (previous > Long.MAX_VALUE - bytes) Long.MAX_VALUE else previous + bytes
        clientUsageHistory.edit()
            .putLong(dateKey, total)
            .putBoolean(CLIENT_USAGE_TRACKING_AVAILABLE, true)
            .apply()
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
        if (!hotspotController.requestClientReauthentication(mac)) {
            Log.w("BandwidthTracker", "Client will rely on the next captive probe to reopen login for $mac")
        }
        Log.i("BandwidthTracker", "Expired voucher disconnected ($reason) for $mac")
    }

    override fun onDestroy() {
        trackingJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val BLOCKED_ENFORCEMENT_INTERVAL_MS = 5_000L
        const val AP_INTERFACE_REFRESH_INTERVAL_MS = 5_000L
        const val CLIENT_USAGE_HISTORY_PREFS = "voucher_client_usage_history_v1"
        const val CLIENT_USAGE_DAY_KEY_PREFIX = "day:"
        const val CLIENT_USAGE_TRACKING_AVAILABLE = "tracking_available"
    }
}
