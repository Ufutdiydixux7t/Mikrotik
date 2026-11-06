package com.example.mikrotik.presentation.voucher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mikrotik.core.network.IptablesManager
import com.example.mikrotik.data.db.dao.ActiveSessionDao
import com.example.mikrotik.data.db.dao.VoucherDao
import com.example.mikrotik.data.db.entity.VoucherEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoucherViewModel @Inject constructor(
    private val voucherDao: VoucherDao,
    private val activeSessionDao: ActiveSessionDao,
    private val iptablesManager: IptablesManager
) : ViewModel() {

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError

    val vouchers: StateFlow<List<VoucherEntity>> = voucherDao.getAllVouchersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createVoucher(durationDays: Int, dataGigabytes: Int) {
        viewModelScope.launch {
            val code = "MDI-" + (1000..9999).random()
            val durationMinutes = durationDays * 24 * 60L
            val dataBytes = dataGigabytes * 1024L * 1024L * 1024L

            val voucher = VoucherEntity(
                code = code,
                durationMinutes = durationMinutes,
                dataLimitBytes = dataBytes,
                createdAt = System.currentTimeMillis()
            )
            voucherDao.insertVoucher(voucher)
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

                for (macAddress in macAddresses) {
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
