package com.example.mikrotik.presentation.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mikrotik.core.hotspot.HotspotController
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
    private val hotspotController: HotspotController,
    @ApplicationContext context: Context
) : ViewModel() {

    private val deviceNamePreferences = context.getSharedPreferences("dashboard_device_names", Context.MODE_PRIVATE)

    val activeSessions: StateFlow<List<ActiveSessionEntity>> = activeSessionDao.getActiveSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedMacs: StateFlow<List<BlockedMacEntity>> = blockedMacDao.getBlockedMacsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deviceUsage = MutableStateFlow<Map<String, DeviceUsageInfo>>(emptyMap())
    val deviceUsage: StateFlow<Map<String, DeviceUsageInfo>> = _deviceUsage.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _blockedVoucherCodes = MutableStateFlow<Map<String, String>>(emptyMap())
    val blockedVoucherCodes: StateFlow<Map<String, String>> = _blockedVoucherCodes.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                val latest = activeSessions.value.distinctBy { it.macAddress.lowercase() }
                val refreshedUsage = mutableMapOf<String, DeviceUsageInfo>()
                for (session in latest) {
                    val usageInfo = loadUsageInfo(session)
                    refreshedUsage[session.macAddress] = usageInfo
                }
                _deviceUsage.value = refreshedUsage

                delay(2_000)


                val blockedCodes = mutableMapOf<String, String>()
                for (blocked in blockedMacs.value.distinctBy { it.macAddress.lowercase() }) {
                    val voucherCode = activeSessionDao.getSessionByMac(blocked.macAddress)?.voucherCode
                    if (!voucherCode.isNullOrBlank()) {
                        blockedCodes[blocked.macAddress.lowercase()] = voucherCode
                    }
                }
                _blockedVoucherCodes.value = blockedCodes
                delay(5_000)
            }
        }
    }

    private suspend fun loadUsageInfo(session: ActiveSessionEntity): DeviceUsageInfo {
        val voucher = voucherDao.getVoucherByCode(session.voucherCode)
        val storedVoucherBytes = voucher?.bytesUsedSoFar ?: 0L
        val sessionFallbackBytes = session.bytesDownloaded + session.bytesUploaded
        val savedName = deviceNamePreferences.getString(nameKey(session.macAddress), null)
            ?.trim()?.takeIf { it.isNotEmpty() }
        val detectedName = savedName ?: session.voucherCode
        return DeviceUsageInfo(
            bytesUsed = maxOf(storedVoucherBytes, sessionFallbackBytes).coerceAtLeast(0L),
            quotaBytes = voucher?.dataLimitBytes?.coerceAtLeast(0L) ?: 0L,
            displayName = detectedName,
            isMeasured = voucher != null || sessionFallbackBytes > 0L
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

    fun getDeviceName(
        macAddress: String,
        fallbackName: String = ""
    ): String =
        deviceNamePreferences.getString(nameKey(macAddress), null)
            ?.trim()?.takeIf { it.isNotEmpty() }
            ?: fallbackName

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun blockDevice(macAddress: String, reason: String = "حظر يدوياً من لوحة التحكم") {
        val mac = normalizeMac(macAddress)
        viewModelScope.launch {
            try {
                val sessions = activeSessionDao.getSessionsByMac(mac)
                val voucherCodes = (
                    sessions.map { it.voucherCode } + voucherDao.getVouchersByMac(mac).map { it.code }
                    ).distinct()

                // Keep the database and portal fail-closed even if a root firewall command needs retrying.
                sessions.firstOrNull()?.voucherCode?.let { code ->
                    _blockedVoucherCodes.value = _blockedVoucherCodes.value + (mac to code)
                }
                blockedMacDao.blockMac(BlockedMacEntity(macAddress = mac, reason = reason))
                voucherCodes.forEach { voucherDao.blockVoucherForDeletion(it) }

                val macBlocked = iptablesManager.blockMacAddress(mac)
                val ipBlocked = sessions.map { iptablesManager.blockClientIpAddress(it.ipAddress) }.all { it }
                val accessRevoked = iptablesManager.revokeMacAddress(mac)
                hotspotController.requestClientReauthentication(mac)

                if (!ipBlocked || !accessRevoked || !macBlocked) {
                    _actionMessage.value = "سُجل الجهاز ضمن قائمة الحظر، لكن تعذر تطبيق بعض قواعد الشبكة. أبقِ نقطة الاتصال مفعلة وحاول مجدداً."
                    return@launch
                }

                voucherCodes.forEach { voucherDao.deleteVoucher(it) }
                activeSessionDao.deactivateSessionsByMac(mac)
                _actionMessage.value = "تم حظر الجهاز وقطع الإنترنت عنه وحذف البطاقة المستخدمة. بعد رفع الحظر سيحتاج إلى بطاقة جديدة."
            } catch (exception: Exception) {
                _actionMessage.value = "تعذر حظر الجهاز: ${exception.message ?: "خطأ غير معروف"}"
            }
        }
    }

    fun unblockDevice(macAddress: String) {
        val mac = normalizeMac(macAddress)
        viewModelScope.launch {
            try {
                val sessions = activeSessionDao.getSessionsByMac(mac)
                val voucherCodes = (
                    sessions.map { it.voucherCode } + voucherDao.getVouchersByMac(mac).map { it.code }
                    ).distinct()
                voucherCodes.forEach {
                    voucherDao.blockVoucherForDeletion(it)
                    voucherDao.deleteVoucher(it)
                }

                // Revoke all old authorization before removing the MAC DROP rule.
                val ipUnblocked = sessions.map { iptablesManager.unblockClientIpAddress(it.ipAddress) }.all { it }
                val accessRevoked = iptablesManager.revokeMacAddress(mac)
                if (!ipUnblocked || !accessRevoked) {
                    _actionMessage.value = "لم يكتمل رفع الحظر بسبب تعذر تحديث قواعد الشبكة. أعد المحاولة مع تشغيل Root ونقطة الاتصال."
                    return@launch
                }

                val macUnblocked = iptablesManager.unblockMacAddress(mac)
                if (!macUnblocked) {
                    _actionMessage.value = "لم يكتمل رفع الحظر بسبب تعذر تحديث قواعد الشبكة. أعد المحاولة مع تشغيل Root ونقطة الاتصال."
                    return@launch
                }

                activeSessionDao.deleteSessionsByMac(mac)
                blockedMacDao.unblockMac(mac)
                _blockedVoucherCodes.value = _blockedVoucherCodes.value - mac
                val reconnectRequested = hotspotController.requestClientReauthentication(mac)
                _actionMessage.value = if (reconnectRequested) {
                    "تم رفع الحظر. سيُعاد فحص البوابة وسيحتاج الجهاز إلى تسجيل الدخول ببطاقة جديدة."
                } else {
                    "تم رفع الحظر، لكن تعذر إجبار الجهاز على إعادة الاتصال. سيحتاج إلى تسجيل الدخول عند محاولته فتح الإنترنت."
                }
            } catch (exception: Exception) {
                _actionMessage.value = "تعذر رفع الحظر: ${exception.message ?: "خطأ غير معروف"}"
            }
        }
    }

    private fun normalizeMac(macAddress: String): String = macAddress.trim().lowercase()
}
