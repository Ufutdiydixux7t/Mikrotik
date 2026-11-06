package com.example.mikrotik.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.mikrotik.core.hotspot.ClientDiscoveryManager
import com.example.mikrotik.core.network.IptablesManager
import com.example.mikrotik.data.db.dao.ActiveSessionDao
import com.example.mikrotik.data.db.dao.BlockedMacDao
import com.example.mikrotik.data.db.dao.VoucherDao
import com.example.mikrotik.data.db.entity.BlockedMacEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class BandwidthTrackerService : Service() {

    @Inject lateinit var clientDiscoveryManager: ClientDiscoveryManager
    @Inject lateinit var activeSessionDao: ActiveSessionDao
    @Inject lateinit var voucherDao: VoucherDao
    @Inject lateinit var blockedMacDao: BlockedMacDao
    @Inject lateinit var iptablesManager: IptablesManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var trackingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        startTrackingJob()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "bandwidth_tracker_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Mini MikroTik Bandwidth Tracker",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Mini MikroTik Router Engine")
            .setContentText("جاري تتبع استهلاك البيانات وتطبيق حظر الكوتا اللحظي...")
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    private suspend fun trackBandwidthAndEnforceQuotas() {
        val devices = clientDiscoveryManager.getConnectedDevices("ap0")
        for (device in devices) {
            val mac = device.macAddress
            
            // Check if MAC is blocked
            if (blockedMacDao.isMacBlocked(mac)) {
                iptablesManager.blockMacAddress(mac)
                continue
            }

            // Check active session
            val session = activeSessionDao.getSessionByMac(mac)
            if (session != null && session.isActive) {
                val voucher = voucherDao.getVoucherByCode(session.voucherCode)
                if (voucher != null) {
                    val firstUse = voucher.firstUsedAt
                    val isDurationExpired = firstUse != null &&
                        System.currentTimeMillis() >= firstUse + voucher.durationMinutes * 60_000L
                    val isQuotaExceeded = voucher.bytesUsedSoFar >= voucher.dataLimitBytes

                    if (isDurationExpired || isQuotaExceeded) {
                        // Enforce block
                        iptablesManager.blockMacAddress(mac)
                        blockedMacDao.blockMac(BlockedMacEntity(macAddress = mac, reason = "تجاوز الكوتا أو مدة البطاقة"))
                        activeSessionDao.deactivateSession(mac)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        trackingJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
