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
    private val interfaceNameRegex = Regex("^[A-Za-z0-9_.-]{1,32}$")
    private val stationMacRegex = Regex("(?i)[0-9a-f]{2}(?::[0-9a-f]{2}){5}")
    private val activeNeighborStates = setOf("REACHABLE", "DELAY", "PROBE")

    /** Resolves current addresses in one root-shell round trip for all active voucher clients. */
    suspend fun findIpv4AddressesForMacs(macAddresses: Collection<String>, interfaceName: String): Map<String, String> =
        withContext(Dispatchers.IO) {
            val macs = macAddresses.map { it.trim().lowercase() }.filter { macRegex.matches(it) }.distinct()
            if (macs.isEmpty() || !interfaceNameRegex.matches(interfaceName)) return@withContext emptyMap()

            val snapshot = rootShellManager.executeCommand(
                "echo __MM_NEIGH__; ip neigh show dev $interfaceName 2>/dev/null; " +
                    "echo __MM_ARP__; cat /proc/net/arp 2>/dev/null; " +
                    "echo __MM_LEASES__; cat /data/misc/dhcp/dnsmasq.leases 2>/dev/null"
            )
            if (!snapshot.isSuccess) return@withContext emptyMap()

            val neighborLines = mutableListOf<String>()
            val arpLines = mutableListOf<String>()
            val leaseLines = mutableListOf<String>()
            var section = 0
            snapshot.stdout.forEach { line ->
                when (line.trim()) {
                    "__MM_NEIGH__" -> section = 1
                    "__MM_ARP__" -> section = 2
                    "__MM_LEASES__" -> section = 3
                    else -> when (section) {
                        1 -> neighborLines += line
                        2 -> arpLines += line
                        3 -> leaseLines += line
                    }
                }
            }

            macs.mapNotNull { mac ->
                ClientAddressResolver.findIpv4ForMac(
                    macAddress = mac,
                    interfaceName = interfaceName,
                    neighborLines = neighborLines,
                    arpLines = arpLines,
                    dhcpLeaseLines = leaseLines
                )?.let { mac to it }
            }.toMap()
        }

    /** Resolves a client from neighbor/ARP entries scoped to the actual hotspot interface. */
    suspend fun findMacAddressForIp(ipAddress: String, interfaceName: String): String? = withContext(Dispatchers.IO) {
        if (!isValidIpv4(ipAddress) || !interfaceNameRegex.matches(interfaceName)) return@withContext null

        val result = rootShellManager.executeCommand(
            "ip neigh show to $ipAddress dev $interfaceName 2>/dev/null; " +
                "cat /proc/net/arp 2>/dev/null"
        )
        val lines = result.stdout

        val neighborMac = lines.asSequence()
            .map { it.split("\\s+".toRegex()).filter(String::isNotBlank) }
            .firstOrNull { it.firstOrNull() == ipAddress && it.contains("dev") && it.getOrNull(it.indexOf("dev") + 1) == interfaceName && it.contains("lladdr") }
            ?.firstOrNull { macRegex.matches(it) && it != "00:00:00:00:00:00" }
            ?.lowercase()
        if (neighborMac != null) return@withContext neighborMac

        val arpMac = lines.asSequence()
            .map { it.trim().split("\\s+".toRegex()) }
            .firstOrNull {
                it.size >= 6 && it[0] == ipAddress && it[1].startsWith("0x") &&
                    (it[2].removePrefix("0x").toIntOrNull(16)?.let { flags -> flags and 0x2 != 0 } == true) &&
                    it[5] == interfaceName
            }
            ?.get(3)?.lowercase()
            ?.takeIf { macRegex.matches(it) && it != "00:00:00:00:00:00" }
        if (arpMac != null) return@withContext arpMac
        null
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
        val safeInterface = interfaceName?.takeIf(interfaceNameRegex::matches)
        val associatedMacs = safeInterface?.let { readAssociatedStationMacs(it) }
        val devices = mutableMapOf<String, ConnectedDevice>()

        val neighborCommand = safeInterface?.let { "ip neigh show dev $it" } ?: "ip neigh show"
        val neighborResult = rootShellManager.executeCommand(neighborCommand)
        if (neighborResult.isSuccess) {
            for (line in neighborResult.stdout) {
                val tokens = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
                val lladdrIndex = tokens.indexOf("lladdr")
                if (tokens.size < 4 || lladdrIndex < 0) continue

                val ip = tokens.first()
                val mac = tokens.getOrNull(lladdrIndex + 1)?.lowercase() ?: continue
                val state = tokens.last().uppercase()
                val connected = if (associatedMacs != null) {
                    mac in associatedMacs
                } else {
                    state in activeNeighborStates
                }

                if (connected && isValidIpv4(ip) && macRegex.matches(mac) && mac != "00:00:00:00:00:00") {
                    devices[mac] = ConnectedDevice(
                        ipAddress = ip,
                        macAddress = mac,
                        connectionState = "CONNECTED",
                        interfaceName = safeInterface ?: "ap"
                    )
                }
            }
        }

        // ARP is supplementary only: it may enrich a known associated client, never add an old entry by itself.
        val arpResult = rootShellManager.executeCommand("cat /proc/net/arp")
        if (arpResult.isSuccess) {
            for (line in arpResult.stdout.drop(1)) {
                val tokens = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
                if (tokens.size < 6) continue

                val ip = tokens[0]
                val flags = tokens[2].removePrefix("0x").toIntOrNull(16) ?: 0
                val mac = tokens[3].lowercase()
                val iface = tokens[5]
                val alreadyConnected = if (associatedMacs != null) mac in associatedMacs else mac in devices

                if ((flags and 0x2) == 0 || !alreadyConnected || !isValidIpv4(ip) || !macRegex.matches(mac)) continue
                if (safeInterface != null && iface != safeInterface) continue

                val existing = devices[mac]
                devices[mac] = existing?.copy(ipAddress = ip) ?: ConnectedDevice(
                    ipAddress = ip,
                    macAddress = mac,
                    connectionState = "CONNECTED",
                    interfaceName = iface
                )
            }
        }

        // A station can be associated before its IP appears in neighbor/ARP tables.
        associatedMacs?.forEach { mac ->
            if (mac !in devices) {
                devices[mac] = ConnectedDevice(
                    ipAddress = "غير معروف",
                    macAddress = mac,
                    connectionState = "CONNECTED",
                    interfaceName = safeInterface ?: "ap"
                )
            }
        }

        val leasesResult = rootShellManager.executeCommand("cat /data/misc/dhcp/dnsmasq.leases")
        if (leasesResult.isSuccess) {
            for (line in leasesResult.stdout) {
                val tokens = line.split("\\s+".toRegex())
                if (tokens.size >= 4) {
                    val mac = tokens[1].lowercase()
                    val name = tokens[3]
                    val leaseIp = tokens[2]
                    val existing = devices[mac]
                    if (existing != null && associatedMacs != null && mac in associatedMacs && isValidIpv4(leaseIp)) {
                        devices[mac] = existing.copy(
                            ipAddress = leaseIp,
                            deviceName = name.takeIf { it != "*" } ?: existing.deviceName
                        )
                    } else if (existing != null && name != "*") {
                        devices[mac] = existing.copy(deviceName = name)
                    }
                }
            }
        }

        devices.values.sortedBy { it.deviceName ?: it.ipAddress }
    }

    /** Returns null if station commands are unavailable; an empty set means they succeeded and no stations are associated. */
    private suspend fun readAssociatedStationMacs(interfaceName: String): Set<String>? {
        val iwResult = rootShellManager.executeCommand("iw dev $interfaceName station dump")
        val stationMacs = mutableSetOf<String>()
        var stationSourceAvailable = false
        if (iwResult.isSuccess) {
            stationSourceAvailable = true
            stationMacs += extractMacAddresses(iwResult.stdout)
        }

        val hostapdResult = rootShellManager.executeCommand("hostapd_cli -i $interfaceName all_sta")
        val hostapdOutput = hostapdResult.stdout.joinToString("\n")
        if (hostapdResult.isSuccess && !hostapdOutput.contains("FAIL", ignoreCase = true)) {
            stationSourceAvailable = true
            stationMacs += extractMacAddresses(hostapdResult.stdout)
        }

        return if (stationSourceAvailable) stationMacs else null
    }

    private fun extractMacAddresses(lines: List<String>): Set<String> = stationMacRegex
        .findAll(lines.joinToString("\n"))
        .map { it.value.lowercase() }
        .filter { it != "00:00:00:00:00:00" }
        .toSet()
}
