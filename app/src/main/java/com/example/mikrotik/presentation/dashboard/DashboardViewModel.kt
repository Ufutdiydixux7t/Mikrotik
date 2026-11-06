package com.example.mikrotik.presentation.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mikrotik.core.network.IptablesManager
import com.example.mikrotik.data.db.dao.ActiveSessionDao
import com.example.mikrotik.data.db.dao.BlockedMacDao
import com.example.mikrotik.data.db.dao.VoucherDao
import com.example.mikrotik.data.db.entity.ActiveSessionEntity
import com.example.mikrotik.data.db.entity.BlockedMacEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceUsageInfo(
    val bytesUsed: Long,
    val quotaBytes: Long,
    val displayName: String,
    val isMeasured: Boolean
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val activeSessionDao: ActiveSessionDao,
    private val blockedMacDao: BlockedMacDao,
    private val voucherDao: VoucherDao,
    private val iptablesManager: IptablesManager,
    @ApplicationContext context: Context
) : ViewModel() {

    private val deviceNamePreferences = context.getSharedPreferences("dashboard_device_names", Context.MODE_PRIVATE)

    val activeSessions: StateFlow<List<ActiveSessionEntity>> = activeSessionDao.getActiveSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedMacs: StateFlow<List<BlockedMacEntity>> = blockedMacDao.getBlockedMacsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deviceUsage = MutableStateFlow<Map<String, DeviceUsageInfo>>(emptyMap())
    val deviceUsage: StateFlow<Map<String, DeviceUsageInfo>> = _deviceUsage.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                val latest = activeSessions.value
                val refreshedUsage = mutableMapOf<String, DeviceUsageInfo>()
                for (session in latest) {
                    refreshedUsage[session.macAddress] = loadUsageInfo(session)
                }
                _deviceUsage.value = refreshedUsage
                delay(5_000)
            }
        }
    }

    private suspend fun loadUsageInfo(session: ActiveSessionEntity): DeviceUsageInfo {
        val voucher = voucherDao.getVoucherByCode(session.voucherCode)
        val liveCounterBytes = iptablesManager.getDeviceUsageBytes(session.macAddress, session.ipAddress)
        val storedVoucherBytes = voucher?.bytesUsedSoFar ?: 0L
        val sessionFallbackBytes = session.bytesDownloaded + session.bytesUploaded
        val savedName = deviceNamePreferences.getString(nameKey(session.macAddress), null)
            ?.trim()?.takeIf { it.isNotEmpty() }
        val detectedName = savedName ?: "جهاز ${session.macAddress.takeLast(5)}"
        return DeviceUsageInfo(
            bytesUsed = maxOf(liveCounterBytes ?: 0L, storedVoucherBytes, sessionFallbackBytes).coerceAtLeast(0L),
            quotaBytes = voucher?.dataLimitBytes?.coerceAtLeast(0L) ?: 0L,
            displayName = detectedName,
            isMeasured = liveCounterBytes != null || storedVoucherBytes > 0L || sessionFallbackBytes > 0L
        )
    }

    fun saveDeviceName(macAddress: String, name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        deviceNamePreferences.edit().putString(nameKey(macAddress), cleanName).apply()
        _deviceUsage.value = _deviceUsage.value.toMutableMap().apply {
            val oldValue = get(macAddress)
            if (oldValue != null) put(macAddress, oldValue.copy(displayName = cleanName))
        }
    }

    private fun nameKey(macAddress: String) = "name_${macAddress.lowercase().replace(':', '_')}"

    fun blockDevice(macAddress: String, reason: String = "حظر يدوياً من لوحة التحكم") {
        viewModelScope.launch {
            iptablesManager.blockMacAddress(macAddress)
            blockedMacDao.blockMac(BlockedMacEntity(macAddress = macAddress, reason = reason))
            activeSessionDao.deactivateSession(macAddress)
        }
    }

    fun unblockDevice(macAddress: String) {
        viewModelScope.launch {
            iptablesManager.unblockMacAddress(macAddress)
            blockedMacDao.unblockMac(macAddress)
        }
    }
}
