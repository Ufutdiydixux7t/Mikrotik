package com.example.mikrotik.core.network

import com.example.mikrotik.core.root.RootShellManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IptablesManager @Inject constructor(
    private val rootShellManager: RootShellManager
) {

    suspend fun applyCaptivePortalRedirect(interfaceName: String, localPort: Int = 8080): Boolean {
        // 1. Enable IP Forwarding
        rootShellManager.executeCommand("echo 1 > /proc/sys/net/ipv4/ip_forward")

        // 2. Enable NAT Masquerade so connected clients get internet access through the phone
        rootShellManager.executeCommand("iptables -t nat -A POSTROUTING -j MASQUERADE")

        // 3. Add NAT redirect rule for port 80 to localPort on the AP interface
        val cmd = "iptables -t nat -A PREROUTING -i $interfaceName -p tcp --dport 80 -j REDIRECT --to-ports $localPort"
        val res = rootShellManager.executeCommand(cmd)

        // Also try wlan0 just in case interface name differs
        if (!res.isSuccess && interfaceName != "wlan0") {
            rootShellManager.executeCommand("iptables -t nat -A PREROUTING -i wlan0 -p tcp --dport 80 -j REDIRECT --to-ports $localPort")
        }

        return true
    }

    suspend fun removeCaptivePortalRedirect(interfaceName: String, localPort: Int = 8080) {
        rootShellManager.executeCommand("iptables -t nat -D POSTROUTING -j MASQUERADE")
        rootShellManager.executeCommand("iptables -t nat -D PREROUTING -i $interfaceName -p tcp --dport 80 -j REDIRECT --to-ports $localPort")
        rootShellManager.executeCommand("iptables -t nat -D PREROUTING -i wlan0 -p tcp --dport 80 -j REDIRECT --to-ports $localPort")
    }
}
