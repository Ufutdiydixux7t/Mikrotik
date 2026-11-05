package com.example.mikrotik.presentation.voucher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mikrotik.data.db.dao.VoucherDao
import com.example.mikrotik.data.db.entity.VoucherEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoucherViewModel @Inject constructor(
    private val voucherDao: VoucherDao
) : ViewModel() {

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
}
