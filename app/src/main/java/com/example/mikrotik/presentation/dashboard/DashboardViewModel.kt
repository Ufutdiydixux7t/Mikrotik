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

    fun getDeviceName(macAddress: String): String =
        deviceNamePreferences.getString(nameKey(macAddress), null)
            ?.trim()?.takeIf { it.isNotEmpty() }
            ?: "جهاز ${macAddress.takeLast(5)}"

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun blockDevice(macAddress: String, reason: String = "حظر يدوياً من لوحة التحكم") {
        viewModelScope.launch {
            try {
                val session = activeSessionDao.getSessionByMac(macAddress)
                val voucherCode = session?.voucherCode ?: voucherDao.getVoucherByMac(macAddress)?.code
                if (voucherCode != null) voucherDao.blockVoucherForDeletion(voucherCode)
                blockedMacDao.blockMac(BlockedMacEntity(macAddress = macAddress, reason = reason))

                val ipBlocked = session?.let { iptablesManager.blockClientIpAddress(it.ipAddress) } ?: true
                val accessRevoked = iptablesManager.revokeMacAddress(macAddress)
                val macBlocked = iptablesManager.blockMacAddress(macAddress)
                if (!ipBlocked || !accessRevoked || !macBlocked) {
                    _actionMessage.value = "سُجل الجهاز ضمن قائمة الحظر، لكن تعذر تطبيق بعض قواعد الشبكة. أبقِ نقطة الاتصال مفعلة وحاول مجدداً."
                    return@launch
                }

                if (voucherCode != null) voucherDao.deleteVoucher(voucherCode)
                activeSessionDao.deactivateSession(macAddress)
                hotspotController.requestClientReauthentication(macAddress)
                _actionMessage.value = "تم حظر الجهاز وقطع الإنترنت عنه وحذف البطاقة المستخدمة. بعد رفع الحظر سيحتاج إلى بطاقة جديدة."
            } catch (exception: Exception) {
                _actionMessage.value = "تعذر حظر الجهاز: ${exception.message ?: "خطأ غير معروف"}"
            }
        }
    }

    fun unblockDevice(macAddress: String) {
        viewModelScope.launch {
            try {
                val session = activeSessionDao.getSessionByMac(macAddress)
                val voucherCode = session?.voucherCode ?: voucherDao.getVoucherByMac(macAddress)?.code
                if (voucherCode != null) {
                    voucherDao.blockVoucherForDeletion(voucherCode)
                    voucherDao.deleteVoucher(voucherCode)
                }

                val ipUnblocked = session?.let { iptablesManager.unblockClientIpAddress(it.ipAddress) } ?: true
                val accessRevoked = iptablesManager.revokeMacAddress(macAddress)
                val macUnblocked = iptablesManager.unblockMacAddress(macAddress)
                if (!ipUnblocked || !accessRevoked || !macUnblocked) {
                    _actionMessage.value = "لم يكتمل رفع الحظر بسبب تعذر تحديث قواعد الشبكة. أعد المحاولة مع تشغيل Root ونقطة الاتصال."
                    return@launch
                }

                activeSessionDao.deleteSession(macAddress)
                blockedMacDao.unblockMac(macAddress)
                val reconnectRequested = hotspotController.requestClientReauthentication(macAddress)
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
}
