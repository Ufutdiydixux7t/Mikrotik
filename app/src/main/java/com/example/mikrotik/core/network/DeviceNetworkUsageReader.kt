package com.example.mikrotik.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.net.NetworkCapabilities

/** Cumulative byte counters for the active upstream Internet interface. */
data class InterfaceTrafficCounter(
    val interfaceName: String,
    val totalBytes: Long
)

object DeviceNetworkUsageReader {

    /**
     * Reads RX + TX on Android's active Internet interface. Unlike device-wide counters,
     * this avoids adding the SoftAP interface to the upstream total a second time.
     */
    fun activeUpstreamCounter(context: Context): InterfaceTrafficCounter? {
        val connectivity = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val network = connectivity.activeNetwork ?: return null
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return null
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return null

        val interfaceName = connectivity.getLinkProperties(network)?.interfaceName
            ?.takeIf { it.matches(Regex("^[A-Za-z0-9_.-]{1,15}$")) }
            ?: return null
        val received = TrafficStats.getRxBytes(interfaceName)
        val transmitted = TrafficStats.getTxBytes(interfaceName)
        if (received < 0L || transmitted < 0L) return null

        val total = try {
            Math.addExact(received, transmitted)
        } catch (_: ArithmeticException) {
            return null
        }
        return InterfaceTrafficCounter(interfaceName, total)
    }

    /** Legacy device-wide counter retained for diagnostics and compatibility. */
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
