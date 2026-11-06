package com.example.mikrotik.core.hotspot

import com.example.mikrotik.core.captive.CaptivePortalServer
import com.example.mikrotik.core.network.IptablesManager
import com.example.mikrotik.core.root.RootShellManager
import com.example.mikrotik.domain.model.HotspotState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HotspotController @Inject constructor(
    private val rootShellManager: RootShellManager,
    private val iptablesManager: IptablesManager,
    private val captivePortalServer: CaptivePortalServer
) {

    /**
     * Checks if the Wi-Fi Hotspot is currently enabled on the device
     * and detects the active network interface name.
     */
    suspend fun getHotspotStatus(): HotspotState = withContext(Dispatchers.IO) {
        val activeInterface = findActiveApInterface()
        if (activeInterface != null) {
            if (configureCaptivePortal(activeInterface)) {
                HotspotState.Enabled(activeInterface)
            } else {
                HotspotState.Error("نقطة الاتصال تعمل، لكن تعذر تأمينها ببوابة البطاقات على $activeInterface")
            }
        } else {
            HotspotState.Disabled
        }
    }

    /**
     * Attempts to start the Wi-Fi Hotspot via robust root commands tested for Samsung Android 12.
     */
    suspend fun startHotspot(): HotspotState = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()

        // 1. Set global soft_ap_enabled to 1
        val res1 = rootShellManager.executeCommand("settings put global soft_ap_enabled 1")
        logs.add("settings soft_ap: success=${res1.isSuccess}")

        // 2. Broadcast Samsung Wi-Fi AP state changed intent
        val res2 = rootShellManager.executeCommand("am broadcast -a com.samsung.android.net.wifi.WIFI_AP_STATE_CHANGED --ei wifi_ap_state 13")
        logs.add("broadcast ap state: success=${res2.isSuccess}")

        // 3. Broadcast tether state changed
        val res3 = rootShellManager.executeCommand("am broadcast -a android.net.conn.TETHER_STATE_CHANGED")
        logs.add("broadcast tether: success=${res3.isSuccess}")

        // Wait briefly for the network interface to initialize
        delay(2500)

        val activeIface = findActiveApInterface()
        if (activeIface != null) {
            if (!configureCaptivePortal(activeIface)) {
                return@withContext HotspotState.Error("تعذر تثبيت قواعد حجب الأجهزة غير المسجلة على $activeIface")
            }
            HotspotState.Enabled(activeIface)
        } else {
            val detailedErr = logs.joinToString(" | ")
            HotspotState.Error("Hotspot failed. Logs: $detailedErr")
        }
    }

    /**
     * Attempts to stop the Wi-Fi Hotspot via root commands.
     */
    suspend fun stopHotspot(): HotspotState = withContext(Dispatchers.IO) {
        val activeIface = findActiveApInterface()
        rootShellManager.executeCommand("settings put global soft_ap_enabled 0")
        rootShellManager.executeCommand("am broadcast -a com.samsung.android.net.wifi.WIFI_AP_STATE_CHANGED --ei wifi_ap_state 11")
        rootShellManager.executeCommand("am broadcast -a android.net.conn.TETHER_STATE_CHANGED")
        
        delay(1500)

        val newIface = findActiveApInterface()
        if (newIface == null) {
            if (activeIface != null) {
                iptablesManager.removeCaptivePortalRedirect(activeIface, 8080)
                iptablesManager.removeCaptiveGate(activeIface)
            }
            captivePortalServer.stopServer()
            HotspotState.Disabled
        } else {
            HotspotState.Error("Hotspot interface $newIface is still active; security rules remain enabled.")
        }
    }

    private suspend fun configureCaptivePortal(interfaceName: String): Boolean {
        if (!iptablesManager.installCaptiveGate(interfaceName)) return false
        if (!iptablesManager.applyCaptivePortalRedirect(interfaceName, 8080)) return false
        captivePortalServer.startServer()
        return captivePortalServer.isServerRunning()
    }

    /**
     * Discovers active Wi-Fi AP interface (e.g., ap0, swlan0, wlan0, wlan1) by inspecting 'ip addr show'.
     */
    suspend fun findActiveApInterface(): String? = withContext(Dispatchers.IO) {
        val ipAddrRes = rootShellManager.executeCommand("ip addr show")
        if (!ipAddrRes.isSuccess) return@withContext null

        var currentIface: String? = null
        var isUp = false
        var hasHotspotIp = false

        for (line in ipAddrRes.stdout) {
            if (line.contains(":") && !line.startsWith(" ")) {
                if (currentIface != null && isUp && hasHotspotIp) {
                    return@withContext currentIface
                }

                val parts = line.split(":")
                currentIface = parts.getOrNull(1)?.trim()?.split("@")?.get(0)
                isUp = line.contains("UP")
                hasHotspotIp = false
            } else if (line.trim().startsWith("inet ")) {
                val ip = line.trim().split(" ")[1].split("/")[0]
                if (ip.startsWith("192.168.43.") || ip.startsWith("192.168.49.") || ip.startsWith("192.168.50.") || ip.startsWith("192.168.1.")) {
                    hasHotspotIp = true
                }
            }
        }

        if (currentIface != null && isUp && hasHotspotIp) {
            return@withContext currentIface
        }

        // Fallback: check for standard AP interface names that are UP
        val apCandidates = listOf("ap0", "swlan0", "softap0", "wlan1")
        for (candidate in apCandidates) {
            val checkRes = rootShellManager.executeCommand("ip link show $candidate")
            if (checkRes.isSuccess && checkRes.stdout.joinToString().contains("UP")) {
                return@withContext candidate
            }
        }

        null
    }
}
