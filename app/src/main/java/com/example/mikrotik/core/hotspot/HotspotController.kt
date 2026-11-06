package com.example.mikrotik.core.hotspot

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.mikrotik.core.captive.CaptivePortalServer
import com.example.mikrotik.core.network.IptablesManager
import com.example.mikrotik.core.root.RootShellManager
import com.example.mikrotik.core.service.BandwidthTrackerService
import com.example.mikrotik.domain.model.HotspotState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HotspotController @Inject constructor(
    private val rootShellManager: RootShellManager,
    private val iptablesManager: IptablesManager,
    private val captivePortalServer: CaptivePortalServer,
    @ApplicationContext private val context: Context
) {

    @Volatile private var trackerStartRequested = false

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
            stopBandwidthTracker()
            HotspotState.Disabled
        }
    }

    /** Forces only this client to reassociate so Android/iOS immediately rechecks the captive portal. */
    suspend fun requestClientReauthentication(macAddress: String): Boolean = withContext(Dispatchers.IO) {
        val mac = macAddress.lowercase()
        if (!Regex("(?i)^[0-9a-f]{2}(:[0-9a-f]{2}){5}$").matches(mac)) return@withContext false

        val interfaceName = findActiveApInterface() ?: return@withContext false
        fun commandSucceeded(result: com.example.mikrotik.core.root.CommandResult): Boolean =
            result.isSuccess && !(result.stdout + result.stderr).joinToString(" ").contains("FAIL", ignoreCase = true)

        val deauthentication = rootShellManager.executeCommand(
            "hostapd_cli -i $interfaceName deauthenticate $mac"
        )
        if (commandSucceeded(deauthentication)) {
            Log.i("HotspotController", "Requested captive-portal recheck for $mac via hostapd deauthentication")
            return@withContext true
        }

        val disassociation = rootShellManager.executeCommand(
            "hostapd_cli -i $interfaceName disassociate $mac"
        )
        if (commandSucceeded(disassociation)) {
            Log.i("HotspotController", "Requested captive-portal recheck for $mac via hostapd disassociation")
            return@withContext true
        }

        val stationDelete = rootShellManager.executeCommand("iw dev $interfaceName station del $mac")
        if (commandSucceeded(stationDelete)) {
            Log.i("HotspotController", "Requested captive-portal recheck for $mac via iw station delete")
            return@withContext true
        }

        val output = (deauthentication.stderr + disassociation.stderr + stationDelete.stderr).joinToString(" ")
        Log.w("HotspotController", "Could not force client reassociation for $mac on $interfaceName: $output")
        false
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
                iptablesManager.removeClientRouting(activeIface)
            }
            captivePortalServer.stopServer()
            stopBandwidthTracker()
            HotspotState.Disabled
        } else {
            HotspotState.Error("Hotspot interface $newIface is still active; security rules remain enabled.")
        }
    }

    private suspend fun configureCaptivePortal(interfaceName: String): Boolean {
        Log.i("HotspotController", "Configuring captive portal and client routing on AP interface $interfaceName")
        if (!iptablesManager.installCaptiveGate(interfaceName)) return false
        if (!iptablesManager.applyCaptivePortalRedirect(interfaceName, 8080)) return false
        if (!iptablesManager.installClientRouting(interfaceName)) return false
        captivePortalServer.startServer()
        return captivePortalServer.isServerRunning() && startBandwidthTracker()
    }

    @Synchronized
    private fun startBandwidthTracker(): Boolean {
        if (trackerStartRequested) return true
        return try {
            val intent = Intent(context, BandwidthTrackerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                ContextCompat.startForegroundService(context, intent)
            }
            trackerStartRequested = true
            true
        } catch (exception: Exception) {
            trackerStartRequested = false
            Log.e("HotspotController", "Could not start bandwidth tracking service", exception)
            false
        }
    }

    @Synchronized
    private fun stopBandwidthTracker() {
        try {
            context.stopService(Intent(context, BandwidthTrackerService::class.java))
        } finally {
            trackerStartRequested = false
        }
    }

    /**
     * Discovers active Wi-Fi AP interface (e.g., ap0, swlan0, wlan0, wlan1) by inspecting 'ip addr show'.
     */
    suspend fun findActiveApInterface(): String? = withContext(Dispatchers.IO) {
        val ipAddrRes = rootShellManager.executeCommand("ip addr show")
        if (!ipAddrRes.isSuccess) return@withContext null

        val apInterfaceCandidates = mutableListOf<String>()
        val standardSubnetCandidates = mutableListOf<String>()
        var currentIface: String? = null
        var isUp = false
        var hasStandardHotspotIp = false
        var hasPrivateApIp = false

        fun collectInterfaceCandidate() {
            val name = currentIface ?: return
            if (!isUp) return
            if (hasPrivateApIp) {
                apInterfaceCandidates += name
            } else if (hasStandardHotspotIp) {
                if (isLikelyApInterface(name)) apInterfaceCandidates += name
                else standardSubnetCandidates += name
            }
        }

        for (line in ipAddrRes.stdout) {
            if (line.contains(":") && !line.startsWith(" ")) {
                collectInterfaceCandidate()

                val parts = line.split(":")
                currentIface = parts.getOrNull(1)?.trim()?.split("@")?.get(0)
                isUp = hasUpFlag(line)
                hasStandardHotspotIp = false
                hasPrivateApIp = false
            } else if (line.trim().startsWith("inet ")) {
                val ip = line.trim().split("\\s+".toRegex()).getOrNull(1)?.substringBefore('/') ?: continue
                if (ip.startsWith("192.168.43.") || ip.startsWith("192.168.49.") || ip.startsWith("192.168.50.")) {
                    hasStandardHotspotIp = true
                }
                if (ip.startsWith("192.168.1.") && currentIface?.let { isLikelyApInterface(it) } == true) {
                    hasPrivateApIp = true
                }
            }
        }

        collectInterfaceCandidate()
        val preferredApInterface = apInterfaceCandidates.firstOrNull()
        if (preferredApInterface != null) return@withContext preferredApInterface
        val standardSubnetInterface = standardSubnetCandidates.firstOrNull()
        if (standardSubnetInterface != null) return@withContext standardSubnetInterface

        // Fallback: check for standard AP interface names that are UP
        val apCandidates = listOf("ap0", "swlan0", "softap0", "wlan1")
        for (candidate in apCandidates) {
            val checkRes = rootShellManager.executeCommand("ip link show $candidate")
            val addressRes = rootShellManager.executeCommand("ip -o -4 addr show dev $candidate")
            val hasIpv4Address = addressRes.isSuccess && addressRes.stdout.any {
                Regex("\\binet\\s+[0-9.]+/[0-9]{1,2}").containsMatchIn(it)
            }
            if (checkRes.isSuccess && hasUpFlag(checkRes.stdout.joinToString("\n")) && hasIpv4Address) {
                return@withContext candidate
            }
        }

        null
    }

    private fun isLikelyApInterface(name: String): Boolean {
        val normalized = name.lowercase()
        return normalized.startsWith("ap") ||
            normalized.startsWith("swlan") ||
            normalized.contains("softap") ||
            normalized == "wlan1" ||
            normalized.startsWith("br-ap") ||
            normalized.contains("ap_br")
    }

    private fun hasUpFlag(interfaceLine: String): Boolean {
        val flags = Regex("<([^>]*)>").find(interfaceLine)?.groupValues?.getOrNull(1) ?: return false
        return flags.split(',').any { it.trim() == "UP" }
    }
}
