package com.example.mikrotik.core.hotspot

import com.example.mikrotik.core.root.RootShellManager
import com.example.mikrotik.domain.model.ConnectedDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientDiscoveryManager @Inject constructor(
    private val rootShellManager: RootShellManager
) {
    private val macRegex = Regex("(?i)^[0-9a-f]{2}(:[0-9a-f]{2}){5}$")

    /** Resolves one client directly from neighbor, ARP, and DHCP lease tables. */
    suspend fun findMacAddressForIp(ipAddress: String): String? = withContext(Dispatchers.IO) {
        if (!isValidIpv4(ipAddress)) return@withContext null

        val neighbor = rootShellManager.executeCommand("ip neigh show to $ipAddress")
        val neighborMac = neighbor.stdout.asSequence()
            .flatMap { it.split("\\s+".toRegex()).asSequence() }
            .map { it.lowercase() }
            .firstOrNull { macRegex.matches(it) && it != "00:00:00:00:00:00" }
        if (neighborMac != null) return@withContext neighborMac

        val arp = rootShellManager.executeCommand("cat /proc/net/arp")
        val arpMac = arp.stdout.asSequence().drop(1)
            .map { it.trim().split("\\s+".toRegex()) }
            .firstOrNull { it.size >= 4 && it[0] == ipAddress }
            ?.get(3)?.lowercase()
            ?.takeIf { macRegex.matches(it) && it != "00:00:00:00:00:00" }
        if (arpMac != null) return@withContext arpMac

        val leases = rootShellManager.executeCommand("cat /data/misc/dhcp/dnsmasq.leases")
        leases.stdout.asSequence()
            .map { it.trim().split("\\s+".toRegex()) }
            .firstOrNull { it.size >= 3 && it[2] == ipAddress && macRegex.matches(it[1]) }
            ?.get(1)?.lowercase()
    }

    private fun isValidIpv4(value: String): Boolean {
        val parts = value.split('.')
        return parts.size == 4 && parts.all { part -> part.toIntOrNull()?.let { it in 0..255 } == true }
    }

    /**
     * Discovers all devices connected to the Hotspot using safe read-only commands
     * (ip neighbor, /proc/net/arp, and dhcp leases).
     */
    suspend fun getConnectedDevices(interfaceName: String? = null): List<ConnectedDevice> = withContext(Dispatchers.IO) {
        val devices = mutableMapOf<String, ConnectedDevice>()

        // 1. Read from 'ip neighbor show'
        val cmd = if (interfaceName != null) "ip neighbor show dev $interfaceName" else "ip neighbor show"
        val ipNeighborRes = rootShellManager.executeCommand(cmd)

        if (ipNeighborRes.isSuccess) {
            for (line in ipNeighborRes.stdout) {
                // Example output: 192.168.43.45 dev ap0 lladdr a4:c3:f0:12:34:56 REACHABLE
                val tokens = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
                if (tokens.size >= 4 && tokens.contains("lladdr")) {
                    val ip = tokens[0]
                    val lladdrIndex = tokens.indexOf("lladdr")
                    val mac = tokens.getOrNull(lladdrIndex + 1)?.lowercase()
                    val state = tokens.lastOrNull() ?: "UNKNOWN"

                    if (ip.isNotBlank() && mac != null && mac != "00:00:00:00:00:00" && !mac.contains("failed")) {
                        devices[mac] = ConnectedDevice(
                            ipAddress = ip,
                            macAddress = mac,
                            connectionState = state,
                            interfaceName = interfaceName ?: "ap"
                        )
                    }
                }
            }
        }

        // 2. Fallback / supplementary check via '/proc/net/arp'
        val arpRes = rootShellManager.executeCommand("cat /proc/net/arp")
        if (arpRes.isSuccess) {
            for (line in arpRes.stdout.drop(1)) { // Skip header line
                val tokens = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
                if (tokens.size >= 6) {
                    val ip = tokens[0]
                    val mac = tokens[3].lowercase()
                    val iface = tokens[5]

                    if (mac != "00:00:00:00:00:00" && !devices.containsKey(mac)) {
                        if (interfaceName == null || iface == interfaceName) {
                            devices[mac] = ConnectedDevice(
                                ipAddress = ip,
                                macAddress = mac,
                                connectionState = "CONNECTED",
                                interfaceName = iface
                            )
                        }
                    }
                }
            }
        }

        // 3. Try enriching device names from dnsmasq leases
        val leasesRes = rootShellManager.executeCommand("cat /data/misc/dhcp/dnsmasq.leases")
        if (leasesRes.isSuccess) {
            for (line in leasesRes.stdout) {
                val tokens = line.split("\\s+".toRegex())
                if (tokens.size >= 4) {
                    val mac = tokens[1].lowercase()
                    val name = tokens[3]
                    if (devices.containsKey(mac) && name != "*") {
                        val existing = devices[mac]!!
                        devices[mac] = existing.copy(deviceName = name)
                    }
                }
            }
        }

        devices.values.toList()
    }
}
