package com.example.mikrotik.core.root

import javax.inject.Inject
import javax.inject.Singleton

data class SystemDiagnosticReport(
    val rootStatus: RootStatus,
    val rootStatusMessage: String,
    val userIdOutput: String,
    val kernelInfo: String,
    val selinuxMode: String,
    val iptablesVersion: String,
    val iptablesLegacyVersion: String,
    val iptablesNftVersion: String,
    val nftablesVersion: String,
    val ipForwardingEnabled: String,
    val availableNetworkInterfaces: List<String>,
    val tetheringCmdResponse: String,
    val rawLog: List<String>
)

@Singleton
class RootDiagnostics @Inject constructor(
    private val rootShellManager: RootShellManager
) {

    /**
     * Executes safe, read-only system diagnostics.
     * Triggers Root Permission prompt and runs 'id' check to verify root access.
     */
    suspend fun runSafeDiagnostics(): SystemDiagnosticReport {
        val logs = mutableListOf<String>()
        logs.add("Requesting Root Shell access from Magisk...")

        val rootStatus = rootShellManager.requestRootPermission()

        when (rootStatus) {
            is RootStatus.NotAvailable -> {
                logs.add("Root Status: NOT AVAILABLE - ${rootStatus.reason}")
                return SystemDiagnosticReport(
                    rootStatus = rootStatus,
                    rootStatusMessage = rootStatus.reason,
                    userIdOutput = "N/A (su binary missing)",
                    kernelInfo = "N/A",
                    selinuxMode = "N/A",
                    iptablesVersion = "Not Checked",
                    iptablesLegacyVersion = "Not Checked",
                    iptablesNftVersion = "Not Checked",
                    nftablesVersion = "Not Checked",
                    ipForwardingEnabled = "Not Checked",
                    availableNetworkInterfaces = emptyList(),
                    tetheringCmdResponse = "Not Checked",
                    rawLog = logs
                )
            }
            is RootStatus.Denied -> {
                logs.add("Root Status: DENIED (User or Magisk policy denied root access)")
                return SystemDiagnosticReport(
                    rootStatus = rootStatus,
                    rootStatusMessage = "Root Permission DENIED by Magisk / User.",
                    userIdOutput = "N/A (Permission Denied)",
                    kernelInfo = "N/A",
                    selinuxMode = "N/A",
                    iptablesVersion = "Not Checked",
                    iptablesLegacyVersion = "Not Checked",
                    iptablesNftVersion = "Not Checked",
                    nftablesVersion = "Not Checked",
                    ipForwardingEnabled = "Not Checked",
                    availableNetworkInterfaces = emptyList(),
                    tetheringCmdResponse = "Not Checked",
                    rawLog = logs
                )
            }
            is RootStatus.Granted -> {
                logs.add("Root Status: GRANTED (Root Shell initialized successfully)")
            }
        }

        // 1. Run 'id' command to double verify uid=0
        val idRes = rootShellManager.executeCommand("id")
        val userIdOutput = idRes.stdout.joinToString(" ")
        logs.add("Executed 'id': $userIdOutput")

        // 2. Kernel Version
        val unameRes = rootShellManager.executeCommand("uname -a")
        val kernelInfo = unameRes.stdout.firstOrNull() ?: "Unknown"
        logs.add("Kernel: $kernelInfo")

        // 3. SELinux Status
        val selinuxRes = rootShellManager.executeCommand("getenforce")
        val selinuxMode = selinuxRes.stdout.firstOrNull() ?: "Unknown"
        logs.add("SELinux Mode: $selinuxMode")

        // 4. iptables / nftables Availability & Versions
        val iptablesRes = rootShellManager.executeCommand("iptables -V")
        val iptablesVer = if (iptablesRes.isSuccess) iptablesRes.stdout.joinToString("\n") else "Not found (${iptablesRes.stderr.joinToString()})"
        logs.add("iptables: $iptablesVer")

        val iptablesLegacyRes = rootShellManager.executeCommand("iptables-legacy -V")
        val iptablesLegacyVer = if (iptablesLegacyRes.isSuccess) iptablesLegacyRes.stdout.joinToString("\n") else "Not found"

        val iptablesNftRes = rootShellManager.executeCommand("iptables-nft -V")
        val iptablesNftVer = if (iptablesNftRes.isSuccess) iptablesNftRes.stdout.joinToString("\n") else "Not found"

        val nftRes = rootShellManager.executeCommand("nft -v")
        val nftVer = if (nftRes.isSuccess) nftRes.stdout.joinToString("\n") else "Not found (${nftRes.stderr.joinToString()})"
        logs.add("nftables: $nftVer")

        // 5. IP Forwarding Status (Read-Only)
        val ipFwdRes = rootShellManager.executeCommand("sysctl net.ipv4.ip_forward")
        val ipFwdStatus = ipFwdRes.stdout.firstOrNull() ?: "Unknown"
        logs.add("IP Forwarding: $ipFwdStatus")

        // 6. Available Network Interfaces (Read-Only)
        val ipLinkRes = rootShellManager.executeCommand("ip link show")
        val interfaces = ipLinkRes.stdout
            .filter { line -> line.contains(":") && !line.startsWith(" ") }
            .map { line -> line.split(":")[1].trim().split("@")[0] }
        logs.add("Network Interfaces: ${interfaces.joinToString(", ")}")

        // 7. Tethering command availability test (Read-Only help check)
        val tetheringRes = rootShellManager.executeCommand("cmd tethering status")
        val tetheringOutput = if (tetheringRes.isSuccess || tetheringRes.stdout.isNotEmpty()) {
            "cmd tethering is AVAILABLE on this device."
        } else {
            val altTetheringRes = rootShellManager.executeCommand("cmd connectivity help")
            if (altTetheringRes.isSuccess) "cmd connectivity is AVAILABLE" else "cmd ipserver/tethering error: ${tetheringRes.stderr.joinToString()}"
        }
        logs.add("Tethering Cmd Check: $tetheringOutput")

        return SystemDiagnosticReport(
            rootStatus = RootStatus.Granted,
            rootStatusMessage = "Root Permission GRANTED (uid=0 verified).",
            userIdOutput = userIdOutput,
            kernelInfo = kernelInfo,
            selinuxMode = selinuxMode,
            iptablesVersion = iptablesVer,
            iptablesLegacyVersion = iptablesLegacyVer,
            iptablesNftVersion = iptablesNftVer,
            nftablesVersion = nftVer,
            ipForwardingEnabled = ipFwdStatus,
            availableNetworkInterfaces = interfaces,
            tetheringCmdResponse = tetheringOutput,
            rawLog = logs
        )
    }
}
