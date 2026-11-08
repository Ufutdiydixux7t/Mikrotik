package com.example.mikrotik.core.network

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.ZoneId

/** Snapshot shown by the measurement page. Card totals are accumulated since the last reset. */
data class UsageMeasurementSnapshot(
    val deviceTodayBytes: Long,
    val voucherTotalBytes: Long,
    val vouchersTodayBytes: Long,
    val deviceCounterAvailable: Boolean
)

/** Shared preferences-backed usage summaries, kept separate from voucher quota counters. */
object UsageMeasurementStore {
    const val DEVICE_USAGE_PREFERENCES = "device_usage_history"
    const val DEVICE_LAST_TOTAL_KEY = "last_total_bytes"
    const val DEVICE_LAST_SAMPLE_TIME_KEY = "last_sample_time"
    const val DAY_KEY_PREFIX = "day:"

    private const val VOUCHER_USAGE_PREFERENCES = "voucher_client_usage_history_v1"
    private const val VOUCHER_TOTAL_KEY = "voucher_total_since_reset"
    private val lock = Any()

    fun read(context: Context): UsageMeasurementSnapshot = synchronized(lock) {
        val today = LocalDate.now(ZoneId.systemDefault()).toString()
        val devicePreferences = context.getSharedPreferences(DEVICE_USAGE_PREFERENCES, Context.MODE_PRIVATE)
        val voucherPreferences = context.getSharedPreferences(VOUCHER_USAGE_PREFERENCES, Context.MODE_PRIVATE)

        UsageMeasurementSnapshot(
            deviceTodayBytes = devicePreferences.getLong("$DAY_KEY_PREFIX$today", 0L),
            voucherTotalBytes = getOrMigrateVoucherTotal(voucherPreferences),
            vouchersTodayBytes = voucherPreferences.getLong("$DAY_KEY_PREFIX$today", 0L),
            deviceCounterAvailable = DeviceNetworkUsageReader.totalBytesSinceBoot() != null
        )
    }

    fun recordVoucherUsage(context: Context, bytes: Long) {
        if (bytes <= 0L) return
        synchronized(lock) {
            val preferences = context.getSharedPreferences(VOUCHER_USAGE_PREFERENCES, Context.MODE_PRIVATE)
            val todayKey = "$DAY_KEY_PREFIX${LocalDate.now(ZoneId.systemDefault())}"
            val todayTotal = preferences.getLong(todayKey, 0L)
            val totalSinceReset = getOrMigrateVoucherTotal(preferences)
            preferences.edit()
                .putLong(todayKey, saturatingAdd(todayTotal, bytes))
                .putLong(VOUCHER_TOTAL_KEY, saturatingAdd(totalSinceReset, bytes))
                .apply()
        }
    }

    /** Clears displayed totals and sets the phone counter baseline so old traffic is not re-added. */
    fun reset(context: Context): Boolean = synchronized(lock) {
        val now = System.currentTimeMillis()
        val currentDeviceTotal = DeviceNetworkUsageReader.totalBytesSinceBoot()
        val devicePreferences = context.getSharedPreferences(DEVICE_USAGE_PREFERENCES, Context.MODE_PRIVATE)
        val deviceEditor = devicePreferences.edit().clear()
        if (currentDeviceTotal != null) {
            deviceEditor
                .putLong(DEVICE_LAST_TOTAL_KEY, currentDeviceTotal)
                .putLong(DEVICE_LAST_SAMPLE_TIME_KEY, now)
        }
        deviceEditor.apply()

        context.getSharedPreferences(VOUCHER_USAGE_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        currentDeviceTotal != null
    }

    private fun saturatingAdd(left: Long, right: Long): Long =
        if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

    private fun getOrMigrateVoucherTotal(preferences: SharedPreferences): Long {
        if (preferences.contains(VOUCHER_TOTAL_KEY)) {
            return preferences.getLong(VOUCHER_TOTAL_KEY, 0L)
        }
        val previousDailyTotal = preferences.all.asSequence()
            .filter { it.key.startsWith(DAY_KEY_PREFIX) }
            .fold(0L) { total, entry ->
                saturatingAdd(total, (entry.value as? Long) ?: 0L)
            }
        preferences.edit().putLong(VOUCHER_TOTAL_KEY, previousDailyTotal).apply()
        return previousDailyTotal
    }
}
