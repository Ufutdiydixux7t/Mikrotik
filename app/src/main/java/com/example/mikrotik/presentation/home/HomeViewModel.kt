package com.example.mikrotik.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mikrotik.core.hotspot.ClientDiscoveryManager
import com.example.mikrotik.core.hotspot.HotspotController
import com.example.mikrotik.core.network.DeviceNetworkUsageReader
import com.example.mikrotik.core.network.NetworkController
import com.example.mikrotik.core.network.UsageMeasurementStore
import com.example.mikrotik.domain.model.ConnectedDevice
import com.example.mikrotik.domain.model.HotspotState
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class TotalUsageSummary(
    val todayBytes: Long = 0L,
    val monthBytes: Long = 0L,
    val yearBytes: Long = 0L,
    val isAvailable: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hotspotController: HotspotController,
    private val clientDiscoveryManager: ClientDiscoveryManager,
    private val networkController: NetworkController
) : ViewModel() {

    private val _hotspotState = MutableStateFlow<HotspotState>(HotspotState.Disabled)
    val hotspotState: StateFlow<HotspotState> = _hotspotState.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val connectedDevices: StateFlow<List<ConnectedDevice>> = _connectedDevices.asStateFlow()

    private val _isWifiActive = MutableStateFlow(false)
    val isWifiActive: StateFlow<Boolean> = _isWifiActive.asStateFlow()

    private val _isDataActive = MutableStateFlow(false)
    val isDataActive: StateFlow<Boolean> = _isDataActive.asStateFlow()

    private val usageHistory = context.getSharedPreferences(USAGE_HISTORY_PREFS, Context.MODE_PRIVATE)
    private val _usageSummary = MutableStateFlow(TotalUsageSummary())
    val usageSummary: StateFlow<TotalUsageSummary> = _usageSummary.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refreshAll()
        startPeriodicPolling()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            
            // 1. Hotspot Status
            val state = hotspotController.getHotspotStatus()
            _hotspotState.value = state

            // 2. Connected Devices
            val iface = if (state is HotspotState.Enabled) state.interfaceName else null
            _connectedDevices.value = if (iface == null) {
                emptyList()
            } else {
                clientDiscoveryManager.getConnectedDevices(iface)
            }

            // 3. WiFi & Mobile Data status via Root
            _isWifiActive.value = networkController.isWifiEnabled()
            _isDataActive.value = networkController.isMobileDataEnabled()
            refreshTotalUsage()

            _isRefreshing.value = false
        }
    }

    fun toggleHotspot() {
        viewModelScope.launch {
            val currentState = _hotspotState.value
            if (currentState is HotspotState.Enabled || currentState is HotspotState.Enabling) {
                _hotspotState.value = HotspotState.Enabling // show enabling/disabling state
                val newState = hotspotController.stopHotspot()
                _hotspotState.value = newState
                if (newState is HotspotState.Error) {
                    _errorMessage.value = newState.message
                }
            } else {
                _hotspotState.value = HotspotState.Enabling
                val newState = hotspotController.startHotspot()
                _hotspotState.value = newState
                if (newState is HotspotState.Error) {
                    _errorMessage.value = newState.message
                }
            }
            refreshAll()
        }
    }

    fun toggleWifi() {
        viewModelScope.launch {
            val current = _isWifiActive.value
            val target = !current
            // Optimistic update
            _isWifiActive.value = target
            
            val (success, err) = networkController.setWifiEnabled(target)
            if (!success) {
                // Revert if failed
                _isWifiActive.value = current
                _errorMessage.value = "فشل تبديل حالة الواي فاي: $err"
            }
            refreshAll()
        }
    }

    fun toggleMobileData() {
        viewModelScope.launch {
            val current = _isDataActive.value
            val target = !current
            // Optimistic update
            _isDataActive.value = target

            val (success, err) = networkController.setMobileDataEnabled(target)
            if (!success) {
                // Revert if failed
                _isDataActive.value = current
                _errorMessage.value = "فشل تبديل حالة البيانات: $err"
            }
            refreshAll()
        }
    }

    private fun startPeriodicPolling() {
        viewModelScope.launch {
            while (true) {
                delay(10000) // Poll every 10 seconds
                val state = hotspotController.getHotspotStatus()
                _hotspotState.value = state
                val iface = if (state is HotspotState.Enabled) state.interfaceName else null
                _connectedDevices.value = if (iface == null) {
                    emptyList()
                } else {
                    clientDiscoveryManager.getConnectedDevices(iface)
                }
                _isWifiActive.value = networkController.isWifiEnabled()
                _isDataActive.value = networkController.isMobileDataEnabled()
                refreshTotalUsage()
            }
        }
    }

    private fun refreshTotalUsage() {
        val totalBytes = DeviceNetworkUsageReader.totalBytesSinceBoot()
        if (totalBytes == null) {
            _usageSummary.value = _usageSummary.value.copy(isAvailable = false)
            return
        }

        val now = System.currentTimeMillis()
        val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        val previousTotal = usageHistory.getLong(KEY_LAST_TOTAL, -1L)
        val previousTime = usageHistory.getLong(KEY_LAST_TIME, -1L)
        if (previousTotal < 0L || previousTime < 0L) {
            usageHistory.edit()
                .putLong(KEY_LAST_TOTAL, totalBytes)
                .putLong(KEY_LAST_TIME, now)
                .apply()
            _usageSummary.value = readUsageSummary(today, isAvailable = true)
            return
        }

        val counterReset = totalBytes < previousTotal
        val delta = if (counterReset) totalBytes else totalBytes - previousTotal
        val allocations = if (counterReset || now <= previousTime) {
            mapOf(today to delta)
        } else {
            distributeDeltaByDay(delta, previousTime, now)
        }
        val editor = usageHistory.edit()
        allocations.forEach { (date, amount) ->
            val key = "$DAY_KEY_PREFIX$date"
            editor.putLong(key, saturatingAdd(usageHistory.getLong(key, 0L), amount))
        }
        editor.putLong(KEY_LAST_TOTAL, totalBytes)
            .putLong(KEY_LAST_TIME, now)
            .apply()
        pruneOldDailyEntries(today)
        _usageSummary.value = readUsageSummary(today, isAvailable = true)
    }

    private fun distributeDeltaByDay(delta: Long, fromMillis: Long, toMillis: Long): Map<LocalDate, Long> {
        if (delta <= 0L || toMillis <= fromMillis) return emptyMap()
        val zone = ZoneId.systemDefault()
        val wholeInterval = toMillis - fromMillis
        val allocations = linkedMapOf<LocalDate, Long>()
        var cursor = fromMillis
        var allocated = 0L
        var visitedDays = 0

        while (cursor < toMillis && visitedDays < MAX_DAYS_PER_SAMPLE) {
            val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
            val midnight = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val segmentEnd = minOf(toMillis, midnight.takeIf { it > cursor } ?: (cursor + MILLIS_PER_DAY))
            val segmentDuration = (segmentEnd - cursor).coerceAtLeast(1L)
            val share = if (segmentEnd == toMillis) {
                delta - allocated
            } else {
                BigDecimal.valueOf(delta)
                    .multiply(BigDecimal.valueOf(segmentDuration))
                    .divide(BigDecimal.valueOf(wholeInterval), 0, RoundingMode.DOWN)
                    .longValueExact()
            }
            allocations[date] = saturatingAdd(allocations[date] ?: 0L, share)
            allocated = saturatingAdd(allocated, share)
            cursor = segmentEnd
            visitedDays++
        }

        if (cursor < toMillis) {
            val date = Instant.ofEpochMilli(toMillis).atZone(zone).toLocalDate()
            allocations[date] = saturatingAdd(allocations[date] ?: 0L, delta - allocated)
        }
        return allocations
    }

    private fun readUsageSummary(today: LocalDate, isAvailable: Boolean): TotalUsageSummary {
        val entries = usageHistory.all
        val monthPrefix = "$DAY_KEY_PREFIX${today.year}-${today.monthValue.toString().padStart(2, '0')}-"
        val yearPrefix = "$DAY_KEY_PREFIX${today.year}-"
        fun sumForPrefix(prefix: String): Long = entries.asSequence()
            .filter { it.key.startsWith(prefix) }
            .fold(0L) { total, entry -> saturatingAdd(total, (entry.value as? Long) ?: 0L) }

        return TotalUsageSummary(
            todayBytes = usageHistory.getLong("$DAY_KEY_PREFIX$today", 0L),
            monthBytes = sumForPrefix(monthPrefix),
            yearBytes = sumForPrefix(yearPrefix),
            isAvailable = isAvailable
        )
    }

    private fun pruneOldDailyEntries(today: LocalDate) {
        val oldestToKeep = today.minusDays(HISTORY_RETENTION_DAYS).toString()
        val editor = usageHistory.edit()
        var changed = false
        usageHistory.all.keys
            .filter { it.startsWith(DAY_KEY_PREFIX) && it.removePrefix(DAY_KEY_PREFIX) < oldestToKeep }
            .forEach { editor.remove(it); changed = true }
        if (changed) editor.apply()
    }

    private fun saturatingAdd(left: Long, right: Long): Long =
        if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

    private companion object {
        const val USAGE_HISTORY_PREFS = UsageMeasurementStore.DEVICE_USAGE_PREFERENCES
        const val KEY_LAST_TOTAL = UsageMeasurementStore.DEVICE_LAST_TOTAL_KEY
        const val KEY_LAST_TIME = UsageMeasurementStore.DEVICE_LAST_SAMPLE_TIME_KEY
        const val DAY_KEY_PREFIX = UsageMeasurementStore.DAY_KEY_PREFIX
        const val HISTORY_RETENTION_DAYS = 400L
        const val MAX_DAYS_PER_SAMPLE = 5000
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
