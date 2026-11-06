package com.example.mikrotik.core.network

import android.net.TrafficStats

/** Reads Android's cumulative device-wide network counters, across Wi-Fi and mobile data. */
object DeviceNetworkUsageReader {
    fun totalBytesSinceBoot(): Long? {
        val received = TrafficStats.getTotalRxBytes()
        val transmitted = TrafficStats.getTotalTxBytes()
        if (received < 0L || transmitted < 0L) return null

        return try {
            Math.addExact(received, transmitted)
        } catch (_: ArithmeticException) {
            null
        }
    }
}
