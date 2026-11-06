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

    private suspend fun setMacBlock(macAddress: String, block: Boolean): Boolean {
        val mac = macAddress.lowercase()
        if (!macPattern.matches(mac)) return false
        val v4 = setMacRule("iptables", filterChainV4, mac, "DROP", block)
        val v6 = setMacRule("ip6tables", filterChainV6, mac, "DROP", block)
        return v4 && v6
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
            return rootShellManager.executeCommand("$binary -D $chain -m mac --mac-source $mac -j $target").isSuccess
        }
        return true
    }
}
