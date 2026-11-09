package com.example.mikrotik.core.network

/** Pure arithmetic used to reconcile overlapping per-client traffic counters. */
internal object UsageCounterMath {
    fun delta(previous: Long, current: Long): Long = when {
        previous < 0L -> 0L
        current >= previous -> current - previous
        else -> current // A driver or firewall counter restarted.
    }

    fun saturatingAdd(left: Long, right: Long): Long =
        if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

    /** Wi-Fi and iptables observe overlapping traffic; never sum their cumulative totals. */
    fun reconciledTotal(existingUsage: Long, wifiTotal: Long, iptablesTotal: Long): Long =
        maxOf(existingUsage, wifiTotal, iptablesTotal)
}
