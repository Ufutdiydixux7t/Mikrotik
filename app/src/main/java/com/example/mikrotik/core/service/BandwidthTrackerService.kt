package com.example.mikrotik.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
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
import javax.inject.Inject

@AndroidEntryPoint
class BandwidthTrackerService : Service() {

    @Inject lateinit var activeSessionDao: ActiveSessionDao
    @Inject lateinit var voucherDao: VoucherDao
    @Inject lateinit var blockedMacDao: BlockedMacDao
    @Inject lateinit var iptablesManager: IptablesManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var trackingJob: Job? = null
    private val usagePreferences by lazy { getSharedPreferences("voucher_usage_counters", MODE_PRIVATE) }

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
                delay(5_000)
            }
        }
    }

    private suspend fun trackBandwidthAndEnforceQuotas() {
        val sessions = activeSessionDao.getActiveSessionsSnapshot()
        for (session in sessions) {
            val mac = session.macAddress
            if (blockedMacDao.isMacBlocked(mac)) {
                iptablesManager.blockMacAddress(mac)
                activeSessionDao.deactivateSession(mac)
                continue
            }

            var voucher = voucherDao.getVoucherByCode(session.voucherCode)
            if (voucher == null || voucher.isBlocked) {
                iptablesManager.revokeMacAddress(mac)
                activeSessionDao.deactivateSession(mac)
                continue
            }

            val counterBytes = iptablesManager.getDeviceUsageBytes(mac, session.ipAddress)
            if (counterBytes != null) {
                val counterKey = "last_${voucher.code}_${mac.lowercase().replace(':', '_')}"
                val previousBytes = usagePreferences.getLong(counterKey, -1L)
                val addedBytes = when {
                    previousBytes < 0L -> counterBytes
                    counterBytes >= previousBytes -> counterBytes - previousBytes
                    else -> counterBytes // iptables counters were reset with the hotspot rules
                }
                usagePreferences.edit().putLong(counterKey, counterBytes).apply()

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
                // Revoke portal bypass so HTTP traffic returns to the login page; don't add a permanent manual block.
                val revoked = iptablesManager.revokeMacAddress(mac)
                if (!revoked) {
                    // Fail closed if the captive-portal allowance cannot be removed.
                    iptablesManager.blockMacAddress(mac)
                    Log.e("BandwidthTracker", "Could not revoke portal access for expired voucher on $mac")
                }
                activeSessionDao.deactivateSession(mac)
                Log.i(
                    "BandwidthTracker",
                    "Expired voucher disconnected (${if (isQuotaExceeded) "quota" else "duration"}) for $mac"
                )
            }
        }
    }

    override fun onDestroy() {
        trackingJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
