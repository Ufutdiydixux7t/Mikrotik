package com.example.mikrotik.core.hotspot

/** Selects the best current IPv4 mapping for a hotspot client without trusting stale ARP rows. */
internal object ClientAddressResolver {
    private val macPattern = Regex("(?i)^[0-9a-f]{2}(:[0-9a-f]{2}){5}$")

    fun findIpv4ForMac(
        macAddress: String,
        interfaceName: String,
        neighborLines: List<String>,
        arpLines: List<String>,
        dhcpLeaseLines: List<String>
    ): String? {
        val targetMac = macAddress.trim().lowercase()
        if (!macPattern.matches(targetMac) || !interfaceName.matches(Regex("[A-Za-z0-9_.-]{1,32}"))) return null

        val candidates = mutableListOf<Pair<Int, String>>()

        neighborLines.forEach { line ->
            val tokens = line.trim().split("\\s+".toRegex()).filter(String::isNotBlank)
            val ip = tokens.firstOrNull() ?: return@forEach
            val devIndex = tokens.indexOf("dev")
            val lladdrIndex = tokens.indexOf("lladdr")
            if (!isValidIpv4(ip) || devIndex < 0 || tokens.getOrNull(devIndex + 1) != interfaceName || lladdrIndex < 0) return@forEach
            if (tokens.getOrNull(lladdrIndex + 1)?.equals(targetMac, ignoreCase = true) != true) return@forEach

            val state = tokens.lastOrNull()?.uppercase() ?: return@forEach
            val priority = when (state) {
                "REACHABLE" -> 0
                "DELAY" -> 1
                "PROBE" -> 2
                "PERMANENT", "NOARP" -> 3
                "STALE" -> 5
                else -> return@forEach // Ignore FAILED, INCOMPLETE, and unknown states.
            }
            candidates += priority to ip
        }

        dhcpLeaseLines.forEach { line ->
            val tokens = line.trim().split("\\s+".toRegex()).filter(String::isNotBlank)
            if (tokens.size < 4 || tokens[1].equals(targetMac, ignoreCase = true).not()) return@forEach
            val ip = tokens[2]
            if (isValidIpv4(ip)) candidates += 4 to ip
        }

        arpLines.drop(1).forEach { line ->
            val tokens = line.trim().split("\\s+".toRegex()).filter(String::isNotBlank)
            if (tokens.size < 6 || tokens[5] != interfaceName || !tokens[3].equals(targetMac, ignoreCase = true)) return@forEach
            val flags = tokens[2].removePrefix("0x").toIntOrNull(16) ?: return@forEach
            val ip = tokens[0]
            if ((flags and 0x2) != 0 && isValidIpv4(ip)) candidates += 6 to ip
        }

        return candidates.minByOrNull { it.first }?.second
    }

    private fun isValidIpv4(value: String): Boolean {
        val parts = value.split('.')
        return parts.size == 4 && parts.all { it.toIntOrNull()?.let { octet -> octet in 0..255 } == true }
    }
}
