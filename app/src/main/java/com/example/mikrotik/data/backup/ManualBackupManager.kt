package com.example.mikrotik.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.example.mikrotik.data.db.AppDatabase
import com.example.mikrotik.data.db.entity.VoucherEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Performs a conservative merge; active sessions and any network/root credentials are excluded. */
@Singleton
class ManualBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    suspend fun exportJson(): String {
        val vouchers = database.voucherDao().getAllVouchersSnapshot()
        val ui = context.getSharedPreferences(UI_PREFERENCES, Context.MODE_PRIVATE)
        val portal = context.getSharedPreferences(PORTAL_PREFERENCES, Context.MODE_PRIVATE)
        val budget = context.getSharedPreferences(BUDGET_PREFERENCES, Context.MODE_PRIVATE)
        val names = context.getSharedPreferences(DEVICE_NAMES_PREFERENCES, Context.MODE_PRIVATE)
        val source = context.getSharedPreferences(SOURCE_USAGE_PREFERENCES, Context.MODE_PRIVATE)
        val clients = context.getSharedPreferences(CLIENT_USAGE_PREFERENCES, Context.MODE_PRIVATE)
        val home = context.getSharedPreferences(HOME_USAGE_PREFERENCES, Context.MODE_PRIVATE)

        val capacities = linkedMapOf<String, BackupCapacity>()
        budget.all.keys.filter { it.startsWith("capacity_bytes_") }.forEach { key ->
            val type = key.removePrefix("capacity_bytes_")
            val bytes = budget.getLong(key, 0L)
            val unit = budget.getString("capacity_unit_$type", "GB") ?: "GB"
            capacities[type] = BackupCapacity(bytes.coerceAtLeast(0L), unit)
        }
        if ("A" !in capacities && budget.contains("capacity_bytes")) {
            capacities["A"] = BackupCapacity(
                budget.getLong("capacity_bytes", 0L).coerceAtLeast(0L),
                budget.getString("capacity_unit", "GB") ?: "GB"
            )
        }
        val deviceNames = names.all.mapNotNull { (key, value) ->
            val mac = key.removePrefix("name_").replace('_', ':')
            val name = value as? String
            if (key.startsWith("name_") && MAC_PATTERN.matches(mac) && name != null && name.isNotBlank()) mac to name else null
        }.toMap()

        return BackupJsonCodec.encode(
            BackupDocument(
                exportedAt = System.currentTimeMillis(),
                vouchers = vouchers.map { v -> BackupVoucher(
                    code = v.code, durationMinutes = v.durationMinutes, dataLimitBytes = v.dataLimitBytes,
                    boundMacAddress = v.boundMacAddress, isUsed = v.isUsed, isBlocked = v.isBlocked,
                    createdAt = v.createdAt, firstUsedAt = v.firstUsedAt, bytesUsedSoFar = v.bytesUsedSoFar,
                    packageType = v.packageType
                ) },
                language = ui.getString("ui_language", "ar"),
                darkMode = ui.getBoolean("dark_mode_enabled", false),
                appColorTheme = ui.getString("app_color_theme", "default"),
                portalName = portal.getString("portal_name", null)?.trim()
                    ?.takeUnless { it.isNullOrEmpty() || it == LEGACY_PORTAL_NAME } ?: "Mikrotik",
                portalTheme = portal.getString("portal_theme", "green"),
                packageBudgetEnabled = budget.getBoolean("package_budget_enabled", true),
                selectedPackageType = budget.getString("selected_package_type", "A"),
                capacities = capacities,
                deviceNames = deviceNames,
                sourceDailyBytes = dailyLongValues(source, SOURCE_DAY_PREFIX),
                clientDailyBytes = dailyLongValues(clients, CLIENT_DAY_PREFIX),
                homeDailyBytes = dailyLongValues(home, HOME_DAY_PREFIX),
                clientTotalBytes = if (clients.contains(CLIENT_TOTAL_KEY)) clients.getLong(CLIENT_TOTAL_KEY, 0L).coerceAtLeast(0L) else null
            )
        )
    }

    suspend fun restoreJson(json: String): RestoreSummary {
        // Parse and validate the complete document before making any persistent change.
        val backup = BackupJsonCodec.decode(json)
        var inserted = 0
        database.withTransaction {
            backup.vouchers.forEach { v ->
                val rowId = database.voucherDao().insertVoucherIfAbsent(
                    VoucherEntity(
                        code = v.code, durationMinutes = v.durationMinutes, dataLimitBytes = v.dataLimitBytes,
                        boundMacAddress = v.boundMacAddress, isUsed = v.isUsed, isBlocked = v.isBlocked,
                        createdAt = v.createdAt, firstUsedAt = v.firstUsedAt, bytesUsedSoFar = v.bytesUsedSoFar,
                        packageType = v.packageType
                    )
                )
                if (rowId != -1L) inserted++
            }
        }

        mergeUiSettings(backup)
        mergePortalSettings(backup)
        mergeBudgetSettings(backup)
        mergeDeviceNames(backup)
        mergeUsage(SOURCE_USAGE_PREFERENCES, backup.sourceDailyBytes, SOURCE_DAY_PREFIX)
        mergeUsage(CLIENT_USAGE_PREFERENCES, backup.clientDailyBytes, CLIENT_DAY_PREFIX)
        mergeUsage(HOME_USAGE_PREFERENCES, backup.homeDailyBytes, HOME_DAY_PREFIX)
        backup.clientTotalBytes?.let { mergeLongMaximum(CLIENT_USAGE_PREFERENCES, CLIENT_TOTAL_KEY, it) }
        return RestoreSummary(insertedVouchers = inserted, skippedExistingVouchers = backup.vouchers.size - inserted)
    }

    private fun mergeUiSettings(d: BackupDocument) {
        val p = context.getSharedPreferences(UI_PREFERENCES, Context.MODE_PRIVATE)
        val e = p.edit()
        d.language?.let { e.putString("ui_language", it) }
        d.darkMode?.let { e.putBoolean("dark_mode_enabled", it) }
        d.appColorTheme?.let { e.putString("app_color_theme", it) }
        e.apply()
    }

    private fun mergePortalSettings(d: BackupDocument) {
        val p = context.getSharedPreferences(PORTAL_PREFERENCES, Context.MODE_PRIVATE)
        val e = p.edit()
        d.portalName?.let { e.putString("portal_name", it) }
        d.portalTheme?.let { e.putString("portal_theme", it) }
        e.apply()
    }

    private fun mergeBudgetSettings(d: BackupDocument) {
        val p = context.getSharedPreferences(BUDGET_PREFERENCES, Context.MODE_PRIVATE)
        val e = p.edit()
        d.packageBudgetEnabled?.let { e.putBoolean("package_budget_enabled", it) }
        d.selectedPackageType?.let { e.putString("selected_package_type", it) }
        d.capacities.forEach { (type, cap) ->
            val bytesKey = "capacity_bytes_$type"
            e.putLong(bytesKey, cap.bytes).putString("capacity_unit_$type", cap.unit)
        }
        e.apply()
    }

    private fun mergeDeviceNames(d: BackupDocument) {
        val p = context.getSharedPreferences(DEVICE_NAMES_PREFERENCES, Context.MODE_PRIVATE)
        val e = p.edit()
        d.deviceNames.forEach { (mac, name) ->
            val key = "name_${mac.lowercase().replace(':', '_')}"
            if (!p.contains(key)) e.putString(key, name)
        }
        e.apply()
    }

    private fun mergeUsage(preferencesName: String, incoming: Map<String, Long>, prefix: String) {
        if (incoming.isEmpty()) return
        val p = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val e = p.edit()
        incoming.forEach { (date, bytes) ->
            val key = "$prefix$date"
            val current = if (p.contains(key)) p.getLong(key, 0L) else 0L
            if (bytes > current) e.putLong(key, bytes)
        }
        e.apply()
    }

    private fun mergeLongMaximum(preferencesName: String, key: String, incoming: Long) {
        val p = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        if (!p.contains(key) || p.getLong(key, 0L) < incoming) p.edit().putLong(key, incoming).apply()
    }

    private fun dailyLongValues(p: android.content.SharedPreferences, prefix: String): Map<String, Long> =
        p.all.mapNotNull { (key, value) ->
            if (key.startsWith(prefix) && value is Long) key.removePrefix(prefix) to value.coerceAtLeast(0L) else null
        }.toMap()

    data class RestoreSummary(val insertedVouchers: Int, val skippedExistingVouchers: Int)

    private companion object {
        const val UI_PREFERENCES = "mikrotik_ui_preferences"
        const val PORTAL_PREFERENCES = "portal_appearance"
        const val LEGACY_PORTAL_NAME = "AL-MADDI NET"
        const val BUDGET_PREFERENCES = "voucher_package_budget_v1"
        const val DEVICE_NAMES_PREFERENCES = "dashboard_device_names"
        const val SOURCE_USAGE_PREFERENCES = "upstream_usage_history_v2"
        const val SOURCE_DAY_PREFIX = "source_day:"
        const val CLIENT_USAGE_PREFERENCES = "voucher_client_usage_history_v2"
        const val CLIENT_DAY_PREFIX = "day:"
        const val CLIENT_TOTAL_KEY = "voucher_total_since_reset"
        const val HOME_USAGE_PREFERENCES = "home_upstream_usage_history_v1"
        const val HOME_DAY_PREFIX = "day:"
        val MAC_PATTERN = Regex("(?i)(?:[0-9a-f]{2}:){5}[0-9a-f]{2}")
    }
}
