package com.example.mikrotik.presentation.voucher

import android.content.Context
import com.example.mikrotik.data.db.entity.VoucherEntity
import java.util.Locale

internal object VoucherPackageType {
    const val DEFAULT = "A"
    const val LEGACY = "LEGACY"
    private val validType = Regex("[\\p{L}\\p{N}_-]{1,12}")

    fun normalize(value: String?): String? {
        val normalized = value?.trim()?.uppercase(Locale.ROOT) ?: return null
        if (normalized == LEGACY) return null
        return normalized.takeIf(validType::matches)
    }
}

data class VoucherPackageBudget(
    val capacityBytes: Long,
    val committedBytes: Long
) {
    val remainingBytes: Long
        get() = (capacityBytes - committedBytes).coerceAtLeast(0L)
}

/** Stores independent package capacities and voucher commitments for each package type. */
internal class VoucherPackageBudgetStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun capacityBytes(packageType: String): Long {
        val type = VoucherPackageType.normalize(packageType) ?: return 0L
        migrateLegacyCapacityForDefaultType(type)
        return preferences.getLong(capacityKey(type), 0L).coerceAtLeast(0L)
    }

    fun capacityUnit(packageType: String): VoucherDataUnit {
        val type = VoucherPackageType.normalize(packageType) ?: return VoucherDataUnit.GB
        migrateLegacyCapacityForDefaultType(type)
        return preferences.getString(capacityUnitKey(type), null)
            ?.let { saved -> VoucherDataUnit.entries.firstOrNull { it.name == saved } }
            ?: VoucherDataUnit.GB
    }

    fun saveCapacityBytes(packageType: String, bytes: Long, unit: VoucherDataUnit) {
        val type = VoucherPackageType.normalize(packageType) ?: return
        preferences.edit()
            .putLong(capacityKey(type), bytes.coerceAtLeast(0L))
            .putString(capacityUnitKey(type), unit.name)
            .apply()
    }

    fun saveCapacityUnit(packageType: String, unit: VoucherDataUnit) {
        val type = VoucherPackageType.normalize(packageType) ?: return
        preferences.edit().putString(capacityUnitKey(type), unit.name).apply()
    }

    /** Call before deleting the voucher row; rows still present are excluded from this ledger. */
    fun recordVoucherDeletion(voucher: VoucherEntity): Boolean {
        val type = VoucherPackageType.normalize(voucher.packageType) ?: VoucherPackageType.LEGACY
        val key = deletedUsageKey(type)
        val identity = voucherIdentity(voucher)
        val entries = preferences.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        if (entries.any { it.substringBefore('|') == identity }) return true
        entries += "$identity|${voucher.bytesUsedSoFar.coerceAtLeast(0L)}"
        return preferences.edit().putStringSet(key, entries).commit()
    }

    fun committedBytes(vouchers: List<VoucherEntity>, packageType: String, now: Long): Long {
        val type = VoucherPackageType.normalize(packageType) ?: return 0L
        val typedVouchers = vouchers.filter {
            VoucherPackageType.normalize(it.packageType) == type
        }
        val currentIdentities = typedVouchers.mapTo(HashSet()) { voucherIdentity(it) }
        val deletedUsage = preferences.getStringSet(deletedUsageKey(type), emptySet()).orEmpty()
            .asSequence()
            .mapNotNull { entry ->
                val identity = entry.substringBefore('|')
                val bytes = entry.substringAfter('|', "").toLongOrNull()?.coerceAtLeast(0L)
                if (identity !in currentIdentities && bytes != null) bytes else null
            }
            .fold(0L, ::saturatingAdd)

        val currentCommitment = typedVouchers.fold(0L) { total, voucher ->
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

    fun snapshot(
        vouchers: List<VoucherEntity>,
        packageType: String,
        now: Long
    ): VoucherPackageBudget = VoucherPackageBudget(
        capacityBytes = capacityBytes(packageType),
        committedBytes = committedBytes(vouchers, packageType, now)
    )

    private fun migrateLegacyCapacityForDefaultType(type: String) {
        if (type != VoucherPackageType.DEFAULT) return
        val newCapacityKey = capacityKey(type)
        if (preferences.contains(newCapacityKey) || !preferences.contains(KEY_LEGACY_CAPACITY_BYTES)) return

        val editor = preferences.edit()
            .putLong(newCapacityKey, preferences.getLong(KEY_LEGACY_CAPACITY_BYTES, 0L))
        preferences.getString(KEY_LEGACY_CAPACITY_UNIT, null)?.let {
            editor.putString(capacityUnitKey(type), it)
        }
        editor.apply()
    }

    private fun capacityKey(type: String) = "capacity_bytes_$type"
    private fun capacityUnitKey(type: String) = "capacity_unit_$type"
    private fun deletedUsageKey(type: String) = "deleted_voucher_usage_$type"
    private fun voucherIdentity(voucher: VoucherEntity): String = "${voucher.code}@${voucher.createdAt}"

    private fun saturatingAdd(left: Long, right: Long): Long =
        if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

    private companion object {
        const val PREFERENCES_NAME = "voucher_package_budget_v1"
        const val KEY_LEGACY_CAPACITY_BYTES = "capacity_bytes"
        const val KEY_LEGACY_CAPACITY_UNIT = "capacity_unit"
    }
}
