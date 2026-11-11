package com.example.mikrotik.presentation.voucher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import com.example.mikrotik.core.hotspot.HotspotController
import com.example.mikrotik.core.network.IptablesManager
import com.example.mikrotik.data.db.dao.ActiveSessionDao
import com.example.mikrotik.data.db.dao.VoucherDao
import com.example.mikrotik.data.db.entity.VoucherEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class VoucherViewModel @Inject constructor(
    private val voucherDao: VoucherDao,
    private val activeSessionDao: ActiveSessionDao,
    private val iptablesManager: IptablesManager,
    private val hotspotController: HotspotController,
    @ApplicationContext context: Context
) : ViewModel() {

    private val deviceNamePreferences = context.getSharedPreferences("dashboard_device_names", Context.MODE_PRIVATE)
    private val packageBudgetStore = VoucherPackageBudgetStore(context)
    private val _packageCapacityBytes = MutableStateFlow(packageBudgetStore.capacityBytes())
    val packageCapacityBytes: StateFlow<Long> = _packageCapacityBytes.asStateFlow()
    private val _packageCapacityUnit = MutableStateFlow(packageBudgetStore.capacityUnit())
    val packageCapacityUnit: StateFlow<VoucherDataUnit> = _packageCapacityUnit.asStateFlow()
    private val createMutex = Mutex()

    val activeVoucherNames: StateFlow<Map<String, String>> = activeSessionDao.getActiveSessionsFlow()
        .map { sessions ->
            val map = mutableMapOf<String, String>()
            for (session in sessions) {
                val key = "name_${session.macAddress.lowercase().replace(':', '_')}"
                val name = deviceNamePreferences.getString(key, null)?.trim()?.takeIf { it.isNotEmpty() }
                if (name != null) {
                    map[session.voucherCode] = name
                }
            }
            map
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError

    val vouchers: StateFlow<List<VoucherEntity>> = voucherDao.getAllVouchersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val packageBudgetClock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(PACKAGE_BUDGET_REFRESH_MS)
        }
    }
    val packageBudget: StateFlow<VoucherPackageBudget> = combine(
        vouchers,
        _packageCapacityBytes,
        packageBudgetClock
    ) { currentVouchers, capacityBytes, now ->
        packageBudgetStore.snapshot(currentVouchers, capacityBytes, now)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        VoucherPackageBudget(
            capacityBytes = _packageCapacityBytes.value,
            committedBytes = packageBudgetStore.committedBytes(emptyList(), System.currentTimeMillis())
        )
    )

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

    fun updatePackageCapacity(amount: String, unit: VoucherDataUnit) {
        val bytes = VoucherSizeParser.toBytes(amount, unit) ?: return
        packageBudgetStore.saveCapacityBytes(bytes, unit)
        _packageCapacityBytes.value = bytes
        _packageCapacityUnit.value = unit
    }

    fun updatePackageCapacityUnit(unit: VoucherDataUnit) {
        packageBudgetStore.saveCapacityUnit(unit)
        _packageCapacityUnit.value = unit
    }

    fun createVoucher(
        durationDays: Int,
        dataAmount: String,
        dataUnit: VoucherDataUnit,
        packageCapacityAmount: String,
        packageCapacityUnit: VoucherDataUnit
    ) {
        val dataBytes = VoucherSizeParser.toBytes(dataAmount, dataUnit)
        val packageCapacity = VoucherSizeParser.toBytes(packageCapacityAmount, packageCapacityUnit)
        if (durationDays <= 0) {
            _creationError.value = "أدخل عدداً صحيحاً من الأيام أكبر من صفر."
            return
        }
        if (packageCapacity == null) {
            _creationError.value = "أدخل حجم الباقة الإجمالي واختر MB أو GB."
            return
        }
        if (dataBytes == null) {
            _creationError.value = "أدخل حجماً صحيحاً أكبر من صفر واختر MB أو GB."
            return
        }

        _creationError.value = null
        _creationSuccess.value = null
        viewModelScope.launch {
            createMutex.withLock {
                try {
                    val existingVouchers = voucherDao.getAllVouchersSnapshot()
                    val committedBytes = packageBudgetStore.committedBytes(
                        existingVouchers,
                        System.currentTimeMillis()
                    )
                    packageBudgetStore.saveCapacityBytes(packageCapacity, packageCapacityUnit)
                    _packageCapacityBytes.value = packageCapacity
                    _packageCapacityUnit.value = packageCapacityUnit
                    if (packageCapacity < committedBytes) {
                        _creationError.value = "عذراً، باقتك غير كافية؛ البطاقات الحالية تجاوزت حجمها."
                        return@withLock
                    }

                    val remainingBytes = packageCapacity - committedBytes
                    if (dataBytes > remainingBytes) {
                        _creationError.value = "عذراً، باقتك غير كافية. المتبقي ${formatBudgetBytes(remainingBytes)} فقط."
                        return@withLock
                    }

                    val durationMinutes = durationDays * 24 * 60L
                    for (attempt in 0 until MAX_CODE_GENERATION_ATTEMPTS) {
                        val code = (1000..9999).random().toString()
                        val voucher = VoucherEntity(
                            code = code,
                            durationMinutes = durationMinutes,
                            dataLimitBytes = dataBytes,
                            createdAt = System.currentTimeMillis()
                        )
                        if (voucherDao.insertVoucherIfAbsent(voucher) != -1L) {
                            _creationSuccess.value = code
                            return@withLock
                        }
                    }
                    _creationError.value = "تعذر إنشاء رمز فريد من أربعة أرقام؛ أعد المحاولة."
                } catch (exception: Exception) {
                    _creationError.value = "تعذر إنشاء البطاقة: ${exception.message ?: "خطأ غير معروف"}"
                }
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

                val usageRecorded = withContext(Dispatchers.IO) {
                    packageBudgetStore.recordVoucherDeletion(latestVoucher ?: voucher)
                }
                if (!usageRecorded) {
                    _deleteError.value = "تعذر حفظ رصيد الباقة المتبقي؛ لم تُحذف البطاقة."
                    onComplete(false)
                    return@launch
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

    private companion object {
        const val MAX_CODE_GENERATION_ATTEMPTS = 128
        const val PACKAGE_BUDGET_REFRESH_MS = 30_000L
    }
}

private fun formatBudgetBytes(bytes: Long): String {
    val megabyte = 1024.0 * 1024.0
    val gigabyte = megabyte * 1024.0
    return if (bytes >= 1024L * 1024L * 1024L) {
        "${"%.2f".format(java.util.Locale.US, bytes / gigabyte)} GB"
    } else {
        "${"%.0f".format(java.util.Locale.US, bytes / megabyte)} MB"
    }
}
