package com.example.mikrotik.presentation.voucher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mikrotik.core.hotspot.HotspotController
import com.example.mikrotik.core.network.IptablesManager
import com.example.mikrotik.data.db.dao.ActiveSessionDao
import com.example.mikrotik.data.db.dao.VoucherDao
import com.example.mikrotik.data.db.entity.VoucherEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoucherViewModel @Inject constructor(
    private val voucherDao: VoucherDao,
    private val activeSessionDao: ActiveSessionDao,
    private val iptablesManager: IptablesManager,
    private val hotspotController: HotspotController
) : ViewModel() {

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError

    val vouchers: StateFlow<List<VoucherEntity>> = voucherDao.getAllVouchersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _creationError = MutableStateFlow<String?>(null)
    val creationError: StateFlow<String?> = _creationError.asStateFlow()

    private val _creationSuccess = MutableStateFlow<String?>(null)
    val creationSuccess: StateFlow<String?> = _creationSuccess.asStateFlow()

    fun clearCreationError() {
        _creationError.value = null
    }

    fun dismissCreationSuccess() {
        _creationSuccess.value = null
    }

    fun createVoucher(durationDays: Int, dataAmount: String, dataUnit: VoucherDataUnit) {
        val dataBytes = VoucherSizeParser.toBytes(dataAmount, dataUnit)
        if (durationDays <= 0) {
            _creationError.value = "أدخل عدداً صحيحاً من الأيام أكبر من صفر."
            return
        }
        if (dataBytes == null) {
            _creationError.value = "أدخل حجماً صحيحاً أكبر من صفر واختر MB أو GB."
            return
        }

        _creationError.value = null
        _creationSuccess.value = null
        viewModelScope.launch {
            try {
                val code = "MDI-" + (1000..9999).random()
                val durationMinutes = durationDays * 24 * 60L

                val voucher = VoucherEntity(
                    code = code,
                    durationMinutes = durationMinutes,
                    dataLimitBytes = dataBytes,
                    createdAt = System.currentTimeMillis()
                )
                voucherDao.insertVoucher(voucher)
                _creationSuccess.value = code
            } catch (exception: Exception) {
                _creationError.value = "تعذر إنشاء البطاقة: ${exception.message ?: "خطأ غير معروف"}"
            }
        }
    }

    fun deleteVoucher(voucher: VoucherEntity, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _deleteError.value = null
            try {
                voucherDao.blockVoucherForDeletion(voucher.code)
                val latestVoucher = voucherDao.getVoucherByCode(voucher.code)
                val sessions = activeSessionDao.getSessionsByVoucherCode(voucher.code)
                val macAddresses = (
                    sessions.map { it.macAddress } +
                        listOfNotNull(voucher.boundMacAddress, latestVoucher?.boundMacAddress)
                    ).distinct()

                val sessionMacAddresses = sessions.map { it.macAddress }.toSet()
                for (session in sessions) {
                    val ipBlocked = iptablesManager.blockClientIpAddress(session.ipAddress)
                    val revoked = iptablesManager.revokeMacAddress(session.macAddress)
                    val macBlocked = iptablesManager.blockMacAddress(session.macAddress)
                    if (!ipBlocked || !revoked || !macBlocked) {
                        _deleteError.value = "تعذر قطع اتصال أحد الأجهزة؛ لم تُحذف البطاقة. تحقق من صلاحية Root وحاول مجدداً."
                        onComplete(false)
                        return@launch
                    }
                    if (session.isActive) {
                        hotspotController.requestClientReauthentication(session.macAddress)
                    }
                }

                for (macAddress in macAddresses.filterNot { it in sessionMacAddresses }) {
                    if (!iptablesManager.revokeMacAddress(macAddress)) {
                        _deleteError.value = "تعذر سحب السماح من أحد الأجهزة؛ لم تُحذف البطاقة. تحقق من صلاحية Root وحاول مجدداً."
                        onComplete(false)
                        return@launch
                    }
                }

                sessions.forEach { session ->
                    activeSessionDao.deleteSession(session.macAddress)
                }
                voucherDao.deleteVoucher(voucher.code)
                onComplete(true)
            } catch (exception: Exception) {
                _deleteError.value = "تعذر حذف البطاقة: ${exception.message ?: "خطأ غير معروف"}"
                onComplete(false)
            }
        }
    }
}
