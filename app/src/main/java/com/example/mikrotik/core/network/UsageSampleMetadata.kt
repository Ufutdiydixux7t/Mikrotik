package com.example.mikrotik.core.network

/** The counter source(s) that actually returned a value in a single tracker poll. */
enum class UsageSampleSource {
    WIFI_STATION,
    IPTABLES,
    BOTH,
    UNAVAILABLE
}

/** Pure helpers shared by the tracker, dashboard, and unit tests. */
internal object UsageSampleMetadata {
    const val PREFERENCES_NAME = "voucher_usage_counters"

    fun source(wifiAvailable: Boolean, iptablesAvailable: Boolean): UsageSampleSource = when {
        wifiAvailable && iptablesAvailable -> UsageSampleSource.BOTH
        wifiAvailable -> UsageSampleSource.WIFI_STATION
        iptablesAvailable -> UsageSampleSource.IPTABLES
        else -> UsageSampleSource.UNAVAILABLE
    }

    /** Keep metadata isolated per voucher session and normalize MAC case like the tracker counters do. */
    fun identity(macAddress: String, voucherCode: String): String =
        "${voucherCode}_${macAddress.trim().lowercase().replace(':', '_')}"

    fun sourcePreferenceKey(identity: String): String = "sample_${identity}_source"
    fun timestampPreferenceKey(identity: String): String = "sample_${identity}_at"
}
