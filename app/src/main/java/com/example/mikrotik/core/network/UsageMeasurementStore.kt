package com.example.mikrotik.core.network

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.ZoneId

/** Daily counters used to compare upstream traffic with traffic metered for hotspot clients. */
data class UsageMeasurementSnapshot(
    val sourceTodayBytes: Long,
    val sourceMonthBytes: Long,
    val sourceYearBytes: Long,
    val clientsTodayBytes: Long,
    val clientsTotalBytes: Long,
    val phoneEstimateTodayBytes: Long,
    val clientsExceedSourceBytes: Long,
    val sourceCounterAvailable: Boolean
)

/** Independent cumulative source-traffic totals used only by the Home dashboard. */
data class HomeUsageSnapshot(
    val todayBytes: Long,
    val monthBytes: Long,
    val yearBytes: Long,
    val counterAvailable: Boolean
)

/**
 * Stores two independent measurements: active upstream interface traffic and metered
 * hotspot-client traffic. Phone usage is an estimate: upstream minus client traffic.
 */
object UsageMeasurementStore {
    const val DEVICE_USAGE_PREFERENCES = "device_usage_history"
    const val DEVICE_LAST_TOTAL_KEY = "last_total_bytes"
    const val DEVICE_LAST_SAMPLE_TIME_KEY = "last_sample_time"
    const val DAY_KEY_PREFIX = "day:"

    private const val SOURCE_PREFERENCES = "upstream_usage_history_v2"
    private const val SOURCE_DAY_PREFIX = "source_day:"
    private const val SOURCE_LAST_INTERFACE = "source_last_interface"
    private const val SOURCE_LAST_TOTAL = "source_last_total"
    private const val SOURCE_BASELINE_PENDING = "source_baseline_pending"

    private const val HOME_PREFERENCES = "home_upstream_usage_history_v1"
    private const val HOME_DAY_PREFIX = "day:"
    private const val HOME_LAST_INTERFACE = "last_interface"
    private const val HOME_LAST_TOTAL = "last_total"
    private const val HOME_MIGRATION_COMPLETE = "source_history_migrated"

    private const val CLIENT_PREFERENCES = "voucher_client_usage_history_v2"
    private const val CLIENT_TOTAL_KEY = "voucher_total_since_reset"
    private const val CLIENT_DAY_PREFIX = "day:"
    private val lock = Any()

    fun read(context: Context): UsageMeasurementSnapshot = synchronized(lock) {
        val today = LocalDate.now(ZoneId.systemDefault())
        val sourcePreferences = context.getSharedPreferences(SOURCE_PREFERENCES, Context.MODE_PRIVATE)
        val clientPreferences = context.getSharedPreferences(CLIENT_PREFERENCES, Context.MODE_PRIVATE)
        val sourceToday = sourcePreferences.getLong("$SOURCE_DAY_PREFIX$today", 0L)
        val clientsToday = clientPreferences.getLong("$CLIENT_DAY_PREFIX$today", 0L)
        val monthPrefix = "$SOURCE_DAY_PREFIX${today.year}-${today.monthValue.toString().padStart(2, '0')}-"
        val yearPrefix = "$SOURCE_DAY_PREFIX${today.year}-"

        UsageMeasurementSnapshot(
            sourceTodayBytes = sourceToday,
            sourceMonthBytes = sumForPrefix(sourcePreferences, monthPrefix),
            sourceYearBytes = sumForPrefix(sourcePreferences, yearPrefix),
            clientsTodayBytes = clientsToday,
            clientsTotalBytes = getOrMigrateClientTotal(clientPreferences),
            phoneEstimateTodayBytes = (sourceToday - clientsToday).coerceAtLeast(0L),
            clientsExceedSourceBytes = (clientsToday - sourceToday).coerceAtLeast(0L),
            sourceCounterAvailable = DeviceNetworkUsageReader.activeUpstreamCounter(context) != null
        )
    }

    /** Reads the Home counter, which is deliberately separate from resettable measurement data. */
    fun readHomeUsage(context: Context): HomeUsageSnapshot = synchronized(lock) {
        val today = LocalDate.now(ZoneId.systemDefault())
        val preferences = context.getSharedPreferences(HOME_PREFERENCES, Context.MODE_PRIVATE)
        val monthPrefix = "$HOME_DAY_PREFIX${today.year}-${today.monthValue.toString().padStart(2, '0')}-"
        val yearPrefix = "$HOME_DAY_PREFIX${today.year}-"
        HomeUsageSnapshot(
            todayBytes = preferences.getLong("$HOME_DAY_PREFIX$today", 0L),
            monthBytes = sumForPrefix(preferences, monthPrefix),
            yearBytes = sumForPrefix(preferences, yearPrefix),
            counterAvailable = DeviceNetworkUsageReader.activeUpstreamCounter(context) != null
        )
    }

    /**
     * Samples the active Internet interface for both independent ledgers. The measurement ledger
     * can be reset from Settings; the Home ledger has its own baseline and is never reset there.
     */
    fun sampleSourceTraffic(context: Context): Boolean = synchronized(lock) {
        val counter = DeviceNetworkUsageReader.activeUpstreamCounter(context) ?: return@synchronized false

        val todayKey = "${SOURCE_DAY_PREFIX}${LocalDate.now(ZoneId.systemDefault())}"
        val preferences = context.getSharedPreferences(SOURCE_PREFERENCES, Context.MODE_PRIVATE)
        val previousInterface = preferences.getString(SOURCE_LAST_INTERFACE, null)
        val previousTotal = preferences.getLong(SOURCE_LAST_TOTAL, -1L)
        val baselinePending = preferences.getBoolean(SOURCE_BASELINE_PENDING, false)

        val delta = when {
            baselinePending -> 0L
            previousInterface != counter.interfaceName -> 0L
            previousTotal < 0L -> 0L
            counter.totalBytes >= previousTotal -> counter.totalBytes - previousTotal
            else -> counter.totalBytes // Interface counters restarted (for example after reboot).
        }
        val sourceEditor = preferences.edit()
            .putString(SOURCE_LAST_INTERFACE, counter.interfaceName)
            .putLong(SOURCE_LAST_TOTAL, counter.totalBytes)
            .remove(SOURCE_BASELINE_PENDING)
        if (delta > 0L) {
            val todayTotal = preferences.getLong(todayKey, 0L)
            sourceEditor.putLong(todayKey, saturatingAdd(todayTotal, delta))
        }
        sourceEditor.apply()

        val homePreferences = context.getSharedPreferences(HOME_PREFERENCES, Context.MODE_PRIVATE)
        val homePreviousInterface = homePreferences.getString(HOME_LAST_INTERFACE, null)
        val homePreviousTotal = homePreferences.getLong(HOME_LAST_TOTAL, -1L)
        val homeDelta = when {
            homePreviousInterface != counter.interfaceName -> 0L
            homePreviousTotal < 0L -> 0L
            counter.totalBytes >= homePreviousTotal -> counter.totalBytes - homePreviousTotal
            else -> counter.totalBytes
        }
        val homeEditor = homePreferences.edit()
            .putString(HOME_LAST_INTERFACE, counter.interfaceName)
            .putLong(HOME_LAST_TOTAL, counter.totalBytes)
        if (!homePreferences.getBoolean(HOME_MIGRATION_COMPLETE, false)) {
            preferences.all.forEach { (key, value) ->
                if (key.startsWith(SOURCE_DAY_PREFIX) && value is Long) {
                    val homeKey = "$HOME_DAY_PREFIX${key.removePrefix(SOURCE_DAY_PREFIX)}"
                    val existingHomeTotal = homePreferences.getLong(homeKey, 0L)
                    homeEditor.putLong(homeKey, saturatingAdd(existingHomeTotal, value))
                }
            }
            if (delta > 0L) {
                val homeTodayKey = "$HOME_DAY_PREFIX${LocalDate.now(ZoneId.systemDefault())}"
                val migratedToday = saturatingAdd(
                    homePreferences.getLong(homeTodayKey, 0L),
                    preferences.getLong(todayKey, 0L)
                )
                homeEditor.putLong(homeTodayKey, saturatingAdd(migratedToday, delta))
            }
            homeEditor.putBoolean(HOME_MIGRATION_COMPLETE, true)
        } else if (homeDelta > 0L) {
            val homeTodayKey = "$HOME_DAY_PREFIX${LocalDate.now(ZoneId.systemDefault())}"
            val homeTodayTotal = homePreferences.getLong(homeTodayKey, 0L)
            homeEditor.putLong(homeTodayKey, saturatingAdd(homeTodayTotal, homeDelta))
        }
        homeEditor.apply()
        true
    }

    /** Records newly observed bytes from authenticated hotspot-client counters. */
    fun recordVoucherUsage(context: Context, bytes: Long) {
        if (bytes <= 0L) return
        synchronized(lock) {
            val preferences = context.getSharedPreferences(CLIENT_PREFERENCES, Context.MODE_PRIVATE)
            val todayKey = "$CLIENT_DAY_PREFIX${LocalDate.now(ZoneId.systemDefault())}"
            val todayTotal = preferences.getLong(todayKey, 0L)
            val totalSinceReset = getOrMigrateClientTotal(preferences)
            preferences.edit()
                .putLong(todayKey, saturatingAdd(todayTotal, bytes))
                .putLong(CLIENT_TOTAL_KEY, saturatingAdd(totalSinceReset, bytes))
                .apply()
        }
    }

    /** Clears displayed measurements and re-baselines the upstream counter immediately when possible. */
    fun reset(context: Context): Boolean = synchronized(lock) {
        val sourcePreferences = context.getSharedPreferences(SOURCE_PREFERENCES, Context.MODE_PRIVATE)
        val sourceCounter = DeviceNetworkUsageReader.activeUpstreamCounter(context)
        val sourceEditor = sourcePreferences.edit()
        sourcePreferences.all.keys
            .filter { it.startsWith(SOURCE_DAY_PREFIX) }
            .forEach(sourceEditor::remove)
        if (sourceCounter != null) {
            sourceEditor
                .putString(SOURCE_LAST_INTERFACE, sourceCounter.interfaceName)
                .putLong(SOURCE_LAST_TOTAL, sourceCounter.totalBytes)
                .remove(SOURCE_BASELINE_PENDING)
        } else {
            sourceEditor.putBoolean(SOURCE_BASELINE_PENDING, true)
        }
        sourceEditor.apply()

        context.getSharedPreferences(CLIENT_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        sourceCounter != null
    }

    private fun getOrMigrateClientTotal(preferences: SharedPreferences): Long {
        if (preferences.contains(CLIENT_TOTAL_KEY)) return preferences.getLong(CLIENT_TOTAL_KEY, 0L)
        val previousDailyTotal = sumForPrefix(preferences, CLIENT_DAY_PREFIX)
        preferences.edit().putLong(CLIENT_TOTAL_KEY, previousDailyTotal).apply()
        return previousDailyTotal
    }

    private fun sumForPrefix(preferences: SharedPreferences, prefix: String): Long =
        preferences.all.asSequence()
            .filter { it.key.startsWith(prefix) }
            .fold(0L) { total, entry ->
                saturatingAdd(total, (entry.value as? Long) ?: 0L)
            }

    private fun saturatingAdd(left: Long, right: Long): Long =
        if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right
}
