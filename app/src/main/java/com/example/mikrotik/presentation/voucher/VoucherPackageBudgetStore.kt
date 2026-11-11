package com.example.mikrotik.presentation.voucher

import android.content.Context
import com.example.mikrotik.data.db.entity.VoucherEntity

data class VoucherPackageBudget(
    val capacityBytes: Long,
    val committedBytes: Long
) {
    val remainingBytes: Long
        get() = (capacityBytes - committedBytes).coerceAtLeast(0L)
}

/** Persists the upstream package cap and usage from vouchers that have been deleted. */
internal class VoucherPackageBudgetStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun capacityBytes(): Long = preferences.getLong(KEY_CAPACITY_BYTES, 0L).coerceAtLeast(0L)

    fun saveCapacityBytes(bytes: Long) {
        preferences.edit().putLong(KEY_CAPACITY_BYTES, bytes.coerceAtLeast(0L)).apply()
    }

    /** Call before deleting the voucher row; rows still present are excluded from this ledger. */
    fun recordVoucherDeletion(voucher: VoucherEntity): Boolean {
        val identity = voucherIdentity(voucher)
        val entries = preferences.getStringSet(KEY_DELETED_USAGE, emptySet()).orEmpty().toMutableSet()
        if (entries.any { it.substringBefore('|') == identity }) return true
        entries += "$identity|${voucher.bytesUsedSoFar.coerceAtLeast(0L)}"
        return preferences.edit().putStringSet(KEY_DELETED_USAGE, entries).commit()
    }

    fun committedBytes(vouchers: List<VoucherEntity>, now: Long): Long {
        val currentIdentities = vouchers.mapTo(HashSet()) { voucherIdentity(it) }
        val deletedUsage = preferences.getStringSet(KEY_DELETED_USAGE, emptySet()).orEmpty()
            .asSequence()
            .mapNotNull { entry ->
                val identity = entry.substringBefore('|')
                val bytes = entry.substringAfter('|', "").toLongOrNull()?.coerceAtLeast(0L)
                if (identity !in currentIdentities && bytes != null) bytes else null
            }
            .fold(0L, ::saturatingAdd)

        val currentCommitment = vouchers.fold(0L) { total, voucher ->
            val used = voucher.bytesUsedSoFar.coerceAtLeast(0L)
            val limit = voucher.dataLimitBytes.coerceAtLeast(0L)
            val expiredByTime = voucher.firstUsedAt?.let { firstUse ->
                now >= firstUse + voucher.durationMinutes.coerceAtLeast(0L) * 60_000L
            } ?: false
            val committedForVoucher = if (used >= limit || expiredByTime) used else maxOf(used, limit)
            saturatingAdd(total, committedForVoucher)
        }
        return saturatingAdd(currentCommitment, deletedUsage)
    }

    fun snapshot(vouchers: List<VoucherEntity>, capacity: Long, now: Long): VoucherPackageBudget =
        VoucherPackageBudget(
            capacityBytes = capacity.coerceAtLeast(0L),
            committedBytes = committedBytes(vouchers, now)
        )

    private fun voucherIdentity(voucher: VoucherEntity): String =
        "${voucher.code}@${voucher.createdAt}"

    private fun saturatingAdd(left: Long, right: Long): Long =
        if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

    private companion object {
        const val PREFERENCES_NAME = "voucher_package_budget_v1"
        const val KEY_CAPACITY_BYTES = "capacity_bytes"
        const val KEY_DELETED_USAGE = "deleted_voucher_usage"
    }
}
