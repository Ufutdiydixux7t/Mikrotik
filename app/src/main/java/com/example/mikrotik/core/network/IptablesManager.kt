package com.example.mikrotik.core.network

import com.example.mikrotik.core.root.RootShellManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IptablesManager @Inject constructor(
    private val rootShellManager: RootShellManager
) {
    private val filterChainV4 = "MM_CAPTIVE_F"
    private val filterChainV6 = "MM_CAPTIVE6_F"
    private val natChain = "MM_CAPTIVE_N"
    private val macPattern = Regex("(?i)^[0-9a-f]{2}(:[0-9a-f]{2}){5}$")
    private val interfacePattern = Regex("^[A-Za-z0-9_.-]{1,15}$")
    private val stationHeaderPattern = Regex("(?i)^\\s*Station\\s+([0-9a-f]{2}(?::[0-9a-f]{2}){5})")

    /** Reads per-station Wi-Fi byte counters, covering all IP versions when the driver exposes them. */
    suspend fun getWifiStationUsageBytes(macAddress: String, interfaceName: String): Long? {
        val mac = macAddress.lowercase()
        if (!macPattern.matches(mac) || !interfacePattern.matches(interfaceName)) return null

        val iw = rootShellManager.executeCommand("iw dev $interfaceName station get $mac")
        parseStationUsageBytes(iw.stdout)?.let { return it }

        val iwDump = rootShellManager.executeCommand("iw dev $interfaceName station dump")
        if (iwDump.isSuccess) {
            parseStationDumpUsageBytes(iwDump.stdout, mac)?.let { return it }
        }

        val hostapd = rootShellManager.executeCommand("hostapd_cli -i $interfaceName sta $mac")
        val hostapdOutput = hostapd.stdout + hostapd.stderr
        if (hostapd.isSuccess && !hostapdOutput.joinToString(" ").contains("FAIL", ignoreCase = true)) {
            return parseStationUsageBytes(hostapdOutput)
        }
        return null
    }

    private fun parseStationUsageBytes(output: List<String>): Long? {
        val text = output.joinToString("\n")
        val received = parseStationDirectionBytes(text, "rx") ?: return null
        val transmitted = parseStationDirectionBytes(text, "tx") ?: return null
        return if (Long.MAX_VALUE - received < transmitted) Long.MAX_VALUE else received + transmitted
    }

    private fun parseStationDumpUsageBytes(output: List<String>, targetMac: String): Long? {
        var currentMac: String? = null
        val stationLines = mutableListOf<String>()
        for (line in output) {
            val header = stationHeaderPattern.find(line)
            if (header != null) {
                if (currentMac == targetMac) {
                    parseStationUsageBytes(stationLines)?.let { return it }
                }
                currentMac = header.groupValues[1].lowercase()
                stationLines.clear()
            }
            if (currentMac == targetMac) stationLines += line
        }
        return if (currentMac == targetMac) parseStationUsageBytes(stationLines) else null
    }

    private fun parseStationDirectionBytes(text: String, direction: String): Long? {
        val wideCounter = Regex("(?im)^\\s*${Regex.escape(direction)}[_ ]bytes64\\s*[:=]\\s*(\\d+)\\b")
            .find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()
        if (wideCounter != null) return wideCounter
        return Regex("(?im)^\\s*${Regex.escape(direction)}[_ ]bytes\\s*[:=]\\s*(\\d+)\\b")
            .find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()
    }

    /** Installs fail-closed forwarding filters scoped only to the active AP interface. */
    suspend fun installCaptiveGate(interfaceName: String): Boolean {
        if (!interfacePattern.matches(interfaceName)) return false

        val v4 = installDropChain("iptables", filterChainV4, interfaceName)
        val v6 = installDropChain("ip6tables", filterChainV6, interfaceName)
        return v4 && v6
    }

    private suspend fun installDropChain(binary: String, chain: String, interfaceName: String): Boolean {
        val created = rootShellManager.executeCommand("$binary -N $chain")
        if (!created.isSuccess && !rootShellManager.executeCommand("$binary -S $chain").isSuccess) return false
        val dropRule = "$binary -C $chain -j DROP"
        if (!rootShellManager.executeCommand(dropRule).isSuccess &&
            !rootShellManager.executeCommand("$binary -A $chain -j DROP").isSuccess
        ) return false

        val jump = "$binary -C FORWARD -i $interfaceName -j $chain"
        if (!rootShellManager.executeCommand(jump).isSuccess) {
            if (!rootShellManager.executeCommand("$binary -I FORWARD 1 -i $interfaceName -j $chain").isSuccess) {
                return false
            }
        }
        return true
    }

    /** Redirects unauthenticated HTTP only; authenticated client MACs return to normal NAT rules. */
    suspend fun applyCaptivePortalRedirect(interfaceName: String, localPort: Int = 8080): Boolean {
        if (!interfacePattern.matches(interfaceName) || localPort !in 1..65535) return false

        val created = rootShellManager.executeCommand("iptables -t nat -N $natChain")
        if (!created.isSuccess && !rootShellManager.executeCommand("iptables -t nat -S $natChain").isSuccess) return false
        val redirect = "iptables -t nat -C $natChain -p tcp --dport 80 -j REDIRECT --to-ports $localPort"
        if (!rootShellManager.executeCommand(redirect).isSuccess &&
            !rootShellManager.executeCommand("iptables -t nat -A $natChain -p tcp --dport 80 -j REDIRECT --to-ports $localPort").isSuccess
        ) return false

        val jump = "iptables -t nat -C PREROUTING -i $interfaceName -j $natChain"
        if (!rootShellManager.executeCommand(jump).isSuccess) {
            if (!rootShellManager.executeCommand("iptables -t nat -I PREROUTING 1 -i $interfaceName -j $natChain").isSuccess) {
                return false
            }
        }
        return true
    }

    /** Adds an authenticated client's MAC to both IPv4/IPv6 forwarding allow-lists and portal bypass. */
    suspend fun authorizeMacAddress(macAddress: String): Boolean {
        val mac = macAddress.lowercase()
        if (!macPattern.matches(mac)) return false

        val v4 = ensureMacRule("iptables", filterChainV4, mac, "ACCEPT")
        val v6 = ensureMacRule("ip6tables", filterChainV6, mac, "ACCEPT")
        val nat = ensureMacRule("iptables -t nat", natChain, mac, "RETURN")
        return v4 && v6 && nat
    }

    /** Reads forwarded upload/download byte counters for one authenticated hotspot client. */
    suspend fun getDeviceUsageBytes(macAddress: String, ipAddress: String): Long? {
        val mac = macAddress.lowercase()
        val octets = ipAddress.split('.')
        if (!macPattern.matches(mac) || octets.size != 4 ||
            octets.any { it.toIntOrNull()?.let { value -> value in 0..255 } != true }
        ) return null

        val replyChain = rootShellManager.executeCommand("iptables -S MM_CAPTIVE_R")
        if (!replyChain.isSuccess) return null

        // Put a per-device download counter before the existing subnet-wide accept rule.
        val downloadRule = "iptables -C MM_CAPTIVE_R -d $ipAddress -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT"
        if (!rootShellManager.executeCommand(downloadRule).isSuccess &&
            !rootShellManager.executeCommand("iptables -I MM_CAPTIVE_R 1 -d $ipAddress -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT").isSuccess
        ) return null

        val uploadCounters = rootShellManager.executeCommand("iptables -L $filterChainV4 -v -x -n")
        val downloadCounters = rootShellManager.executeCommand("iptables -L MM_CAPTIVE_R -v -x -n")
        if (!uploadCounters.isSuccess || !downloadCounters.isSuccess) return null

        val uploaded = sumAcceptRuleBytes(uploadCounters.stdout) { tokens ->
            tokens.any { it.equals(mac, ignoreCase = true) }
        }
        val downloaded = sumAcceptRuleBytes(downloadCounters.stdout) { tokens ->
            tokens.getOrNull(8) == ipAddress
        }
        return uploaded + downloaded
    }

    private fun sumAcceptRuleBytes(lines: List<String>, matchesDevice: (List<String>) -> Boolean): Long =
        lines.asSequence()
            .map { it.trim().split("\\s+".toRegex()) }
            .filter { tokens -> tokens.size >= 9 && tokens[2].equals("ACCEPT", ignoreCase = true) && matchesDevice(tokens) }
            .sumOf { tokens -> tokens[1].toLongOrNull() ?: 0L }

    /** Resets the per-IP download counter before a newly accepted voucher starts tracking. */
    suspend fun resetDeviceDownloadCounter(ipAddress: String): Boolean {
        val octets = ipAddress.split('.')
        if (octets.size != 4 || octets.any { it.toIntOrNull()?.let { value -> value in 0..255 } != true }) return false
        if (!rootShellManager.executeCommand("iptables -S MM_CAPTIVE_R").isSuccess) return false

        val rule = "iptables -C MM_CAPTIVE_R -d $ipAddress -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT"
        if (!rootShellManager.executeCommand(rule).isSuccess) return true
        return rootShellManager.executeCommand(
            "iptables -D MM_CAPTIVE_R -d $ipAddress -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT"
        ).isSuccess
    }

    /** Clears stale per-device allow-rule counters before authorizing a different voucher. */
    suspend fun resetDeviceUsageCounters(macAddress: String, ipAddress: String): Boolean {
        val mac = macAddress.lowercase()
        val octets = ipAddress.split('.')
        if (!macPattern.matches(mac) || octets.size != 4 ||
            octets.any { it.toIntOrNull()?.let { value -> value in 0..255 } != true }
        ) return false

        val v4 = removeRuleIfPresent(
            "iptables -C $filterChainV4 -m mac --mac-source $mac -j ACCEPT",
            "iptables -D $filterChainV4 -m mac --mac-source $mac -j ACCEPT"
        )
        val v6 = removeRuleIfPresent(
            "ip6tables -C $filterChainV6 -m mac --mac-source $mac -j ACCEPT",
            "ip6tables -D $filterChainV6 -m mac --mac-source $mac -j ACCEPT"
        )
        val nat = removeRuleIfPresent(
            "iptables -t nat -C $natChain -m mac --mac-source $mac -j RETURN",
            "iptables -t nat -D $natChain -m mac --mac-source $mac -j RETURN"
        )
        val download = resetDeviceDownloadCounter(ipAddress)
        return v4 && v6 && nat && download
    }

    private suspend fun removeRuleIfPresent(checkCommand: String, deleteCommand: String): Boolean {
        if (!rootShellManager.executeCommand(checkCommand).isSuccess) return true
        return rootShellManager.executeCommand(deleteCommand).isSuccess
    }

    /** Enables IPv4 forwarding and NAT only for clients in the detected hotspot subnet. */
    suspend fun installClientRouting(interfaceName: String): Boolean {
        if (!interfacePattern.matches(interfaceName)) return false
        val subnet = findIpv4Subnet(interfaceName) ?: return false
        if (!rootShellManager.executeCommand("echo 1 > /proc/sys/net/ipv4/ip_forward").isSuccess) return false

        val natChain = "MM_CAPTIVE_M"
        if (!ensureChain("iptables -t nat", natChain)) return false
        val masquerade = "iptables -t nat -C $natChain -s $subnet -j MASQUERADE"
        if (!rootShellManager.executeCommand(masquerade).isSuccess &&
            !rootShellManager.executeCommand("iptables -t nat -A $natChain -s $subnet -j MASQUERADE").isSuccess
        ) return false
        val natJump = "iptables -t nat -C POSTROUTING -j $natChain"
        if (!rootShellManager.executeCommand(natJump).isSuccess &&
            !rootShellManager.executeCommand("iptables -t nat -I POSTROUTING 1 -j $natChain").isSuccess
        ) return false

        val replyChain = "MM_CAPTIVE_R"
        if (!ensureChain("iptables", replyChain)) return false
        val replyRule = "iptables -C $replyChain -d $subnet -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT"
        if (!rootShellManager.executeCommand(replyRule).isSuccess &&
            !rootShellManager.executeCommand("iptables -I $replyChain 1 -d $subnet -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT").isSuccess
        ) return false
        val replyJump = "iptables -C FORWARD -o $interfaceName -j $replyChain"
        if (!rootShellManager.executeCommand(replyJump).isSuccess &&
            !rootShellManager.executeCommand("iptables -I FORWARD 1 -o $interfaceName -j $replyChain").isSuccess
        ) return false
        return true
    }

    private suspend fun ensureChain(binary: String, chain: String): Boolean {
        val created = rootShellManager.executeCommand("$binary -N $chain")
        return created.isSuccess || rootShellManager.executeCommand("$binary -S $chain").isSuccess
    }

    private suspend fun findIpv4Subnet(interfaceName: String): String? {
        val result = rootShellManager.executeCommand("ip -o -4 addr show dev $interfaceName")
        val cidr = result.stdout.asSequence()
            .flatMap { Regex("(?:^|\\s)inet\\s+([0-9.]+/[0-9]{1,2})").findAll(it) }
            .map { it.groupValues[1] }
            .firstOrNull() ?: return null
        val address = cidr.substringBefore('/').split('.').map { it.toIntOrNull() ?: return null }
        val prefix = cidr.substringAfter('/').toIntOrNull()?.takeIf { it in 1..32 } ?: return null
        if (address.size != 4 || address.any { it !in 0..255 }) return null
        val ip = address.fold(0L) { acc, octet -> (acc shl 8) or octet.toLong() }
        val mask = (0xFFFF_FFFFL shl (32 - prefix)) and 0xFFFF_FFFFL
        val network = ip and mask
        val networkText = (3 downTo 0).joinToString(".") { shift ->
            ((network shr (shift * 8)) and 0xFF).toString()
        }
        return "$networkText/$prefix"
    }

    suspend fun removeClientRouting(interfaceName: String) {
        if (!interfacePattern.matches(interfaceName)) return
        rootShellManager.executeCommand("iptables -D FORWARD -o $interfaceName -j MM_CAPTIVE_R")
        rootShellManager.executeCommand("iptables -F MM_CAPTIVE_R")
        rootShellManager.executeCommand("iptables -X MM_CAPTIVE_R")
        rootShellManager.executeCommand("iptables -t nat -D POSTROUTING -j MM_CAPTIVE_M")
        rootShellManager.executeCommand("iptables -t nat -F MM_CAPTIVE_M")
        rootShellManager.executeCommand("iptables -t nat -X MM_CAPTIVE_M")
    }

    suspend fun removeCaptivePortalRedirect(interfaceName: String, localPort: Int = 8080) {
        if (!interfacePattern.matches(interfaceName)) return
        rootShellManager.executeCommand("iptables -t nat -D PREROUTING -i $interfaceName -j $natChain")
        rootShellManager.executeCommand("iptables -t nat -F $natChain")
        rootShellManager.executeCommand("iptables -t nat -X $natChain")
    }

    suspend fun removeCaptiveGate(interfaceName: String) {
        if (!interfacePattern.matches(interfaceName)) return
        rootShellManager.executeCommand("iptables -D FORWARD -i $interfaceName -j $filterChainV4")
        rootShellManager.executeCommand("ip6tables -D FORWARD -i $interfaceName -j $filterChainV6")
        rootShellManager.executeCommand("iptables -F $filterChainV4")
        rootShellManager.executeCommand("ip6tables -F $filterChainV6")
        rootShellManager.executeCommand("iptables -X $filterChainV4")
        rootShellManager.executeCommand("ip6tables -X $filterChainV6")
    }

    suspend fun blockMacAddress(macAddress: String): Boolean = setMacBlock(macAddress, block = true)

    suspend fun unblockMacAddress(macAddress: String): Boolean = setMacBlock(macAddress, block = false)

    /** Blocks reply traffic to an expired client before the established-connection accept rule. */
    suspend fun blockClientIpAddress(ipAddress: String): Boolean = setClientIpBlock(ipAddress, block = true)

    /** Removes a temporary expiry block after the client successfully presents a valid voucher. */
    suspend fun unblockClientIpAddress(ipAddress: String): Boolean = setClientIpBlock(ipAddress, block = false)

    suspend fun revokeMacAddress(macAddress: String): Boolean {
        val mac = macAddress.lowercase()
        if (!macPattern.matches(mac)) return false
        val identity = rootShellManager.executeCommand("id")
        if (!identity.isSuccess || !identity.stdout.joinToString(" ").contains("uid=0")) return false

        val v4 = setMacRule("iptables", filterChainV4, mac, "ACCEPT", add = false)
        val v6 = setMacRule("ip6tables", filterChainV6, mac, "ACCEPT", add = false)
        val portalBypass = setMacRule("iptables -t nat", natChain, mac, "RETURN", add = false)
        return v4 && v6 && portalBypass
    }

    private suspend fun setMacBlock(macAddress: String, block: Boolean): Boolean {
        val mac = macAddress.lowercase()
        if (!macPattern.matches(mac)) return false
        val v4 = setMacRule("iptables", filterChainV4, mac, "DROP", block)
        val v6 = setMacRule("ip6tables", filterChainV6, mac, "DROP", block)
        return v4 && v6
    }

    private suspend fun setClientIpBlock(ipAddress: String, block: Boolean): Boolean {
        val octets = ipAddress.split('.')
        if (octets.size != 4 || octets.any { it.toIntOrNull()?.let { value -> value in 0..255 } != true }) {
            return false
        }

        val rule = "iptables -C MM_CAPTIVE_R -d $ipAddress -j DROP"
        val exists = rootShellManager.executeCommand(rule).isSuccess
        if (block && !exists) {
            return rootShellManager.executeCommand("iptables -I MM_CAPTIVE_R 1 -d $ipAddress -j DROP").isSuccess
        }
        if (!block && exists) {
            repeat(MAX_DUPLICATE_RULE_REMOVALS) {
                if (!rootShellManager.executeCommand(rule).isSuccess) return true
                if (!rootShellManager.executeCommand("iptables -D MM_CAPTIVE_R -d $ipAddress -j DROP").isSuccess) {
                    return false
                }
            }
            return !rootShellManager.executeCommand(rule).isSuccess
        }
        return true
    }

    private suspend fun ensureMacRule(binary: String, chain: String, mac: String, target: String): Boolean {
        val check = "$binary -C $chain -m mac --mac-source $mac -j $target"
        if (rootShellManager.executeCommand(check).isSuccess) return true
        return rootShellManager.executeCommand("$binary -I $chain 1 -m mac --mac-source $mac -j $target").isSuccess
    }

    private suspend fun setMacRule(
        binary: String,
        chain: String,
        mac: String,
        target: String,
        add: Boolean
    ): Boolean {
        val rule = "$binary -C $chain -m mac --mac-source $mac -j $target"
        val exists = rootShellManager.executeCommand(rule).isSuccess
        if (add && !exists) {
            return rootShellManager.executeCommand("$binary -I $chain 1 -m mac --mac-source $mac -j $target").isSuccess
        }
        if (!add && exists) {
            repeat(MAX_DUPLICATE_RULE_REMOVALS) {
                if (!rootShellManager.executeCommand(rule).isSuccess) return true
                if (!rootShellManager.executeCommand("$binary -D $chain -m mac --mac-source $mac -j $target").isSuccess) {
                    return false
                }
            }
            return !rootShellManager.executeCommand(rule).isSuccess
        }
        return true
    }

    private companion object {
        const val MAX_DUPLICATE_RULE_REMOVALS = 64
    }
}
