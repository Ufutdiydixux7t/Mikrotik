package com.example.mikrotik.core.network

import com.example.mikrotik.core.root.RootShellManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkController @Inject constructor(
    private val rootShellManager: RootShellManager
) {

    /**
     * Checks if Wi-Fi interface is UP on the device.
     */
    suspend fun isWifiEnabled(): Boolean = withContext(Dispatchers.IO) {
        val res = rootShellManager.executeCommand("ip link show wlan0")
        if (res.isSuccess && res.stdout.joinToString().contains("UP")) {
            return@withContext true
        }
        val wifiCmdRes = rootShellManager.executeCommand("cmd wifi status")
        if (wifiCmdRes.stdout.joinToString().lowercase().contains("wifi is enabled")) {
            return@withContext true
        }
        val settingsRes = rootShellManager.executeCommand("settings get global wifi_on")
        settingsRes.stdout.joinToString().trim() == "1"
    }

    /**
     * Directly enables or disables Wi-Fi via Root without opening Notification Shade.
     */
    suspend fun setWifiEnabled(enable: Boolean): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val settingVal = if (enable) "1" else "0"
        val svcAction = if (enable) "enable" else "disable"
        val cmdAction = if (enable) "enabled" else "disabled"

        val logs = mutableListOf<String>()

        // 1. Try 'svc wifi' first (most reliable on Samsung)
        val res1 = rootShellManager.executeCommand("svc wifi $svcAction")
        logs.add("svc wifi: success=${res1.isSuccess}, out=${res1.stdout}, err=${res1.stderr}")

        // 2. Try 'settings put global wifi_on'
        val res2 = rootShellManager.executeCommand("settings put global wifi_on $settingVal")
        logs.add("settings wifi_on: success=${res2.isSuccess}")

        // 3. Try 'cmd wifi set-wifi-enabled'
        val res3 = rootShellManager.executeCommand("cmd wifi set-wifi-enabled $cmdAction")
        logs.add("cmd wifi: success=${res3.isSuccess}")

        // Broadcast wifi state change
        rootShellManager.executeCommand("am broadcast -a android.net.wifi.WIFI_STATE_CHANGED")

        val success = res1.isSuccess || res2.isSuccess || res3.isSuccess
        if (success) {
            Pair(true, "Success")
        } else {
            Pair(false, logs.joinToString(" | "))
        }
    }

    /**
     * Checks if Mobile Data is enabled.
     */
    suspend fun isMobileDataEnabled(): Boolean = withContext(Dispatchers.IO) {
        val res = rootShellManager.executeCommand("cmd connectivity mobile-data status")
        if (res.isSuccess && res.stdout.joinToString().contains("enabled")) {
            return@withContext true
        }
        val settingsRes = rootShellManager.executeCommand("settings get global mobile_data")
        if (settingsRes.stdout.joinToString().trim() == "1") {
            return@withContext true
        }
        val ipLinkRes = rootShellManager.executeCommand("ip link show")
        ipLinkRes.stdout.any { line ->
            (line.contains("rmnet") || line.contains("ccmni") || line.contains("pdp")) && line.contains("UP")
        }
    }

    /**
     * Directly enables or disables Mobile Data via Root without opening Notification Shade.
     */
    suspend fun setMobileDataEnabled(enable: Boolean): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val svcAction = if (enable) "enable" else "disable"
        val settingVal = if (enable) "1" else "0"

        val logs = mutableListOf<String>()

        // 1. Try 'svc data'
        val res1 = rootShellManager.executeCommand("svc data $svcAction")
        logs.add("svc data: success=${res1.isSuccess}")

        // 2. Try 'cmd connectivity mobile-data'
        val cmdAction = if (enable) "enable" else "disable"
        val res2 = rootShellManager.executeCommand("cmd connectivity mobile-data $cmdAction")
        logs.add("cmd connectivity: success=${res2.isSuccess}")

        // 3. Try 'settings put global mobile_data'
        val res3 = rootShellManager.executeCommand("settings put global mobile_data $settingVal")
        logs.add("settings mobile_data: success=${res3.isSuccess}")

        val success = res1.isSuccess || res2.isSuccess || res3.isSuccess
        if (success) {
            Pair(true, "Success")
        } else {
            Pair(false, logs.joinToString(" | "))
        }
    }
}
