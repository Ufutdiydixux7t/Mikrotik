package com.example.mikrotik.core.root

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class CommandResult(
    val exitCode: Int,
    val stdout: List<String>,
    val stderr: List<String>
) {
    val isSuccess: Boolean get() = exitCode == 0
}

sealed class RootStatus {
    object Granted : RootStatus()
    object Denied : RootStatus()
    data class NotAvailable(val reason: String) : RootStatus()
}

@Singleton
class RootShellManager @Inject constructor() {

    /**
     * Checks if 'su' binary exists on common system paths.
     */
    fun isSuBinaryAvailable(): Boolean {
        val suPaths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/data/adb/ksu/bin/su"
        )
        for (path in suPaths) {
            if (File(path).exists()) return true
        }
        // Also attempt running 'which su' as fallback
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val exit = process.waitFor()
            exit == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Initiates a root shell request via LibSU, prompting Magisk / KernelSU for superuser access.
     * Executes 'id' command to strictly verify uid=0 (root).
     */
    suspend fun requestRootPermission(): RootStatus = withContext(Dispatchers.IO) {
        if (!isSuBinaryAvailable()) {
            return@withContext RootStatus.NotAvailable(
                "Magisk or KernelSU binary (su) was not found on this device."
            )
        }

        try {
            // Get root shell instance - LibSU prompts Magisk for root access if not already granted
            val shell = Shell.getShell()
            if (!shell.isRoot) {
                return@withContext RootStatus.Denied
            }

            // Verify with 'id' command
            val idResult = executeCommand("id")
            val idOutput = idResult.stdout.joinToString(" ")

            if (idResult.isSuccess && idOutput.contains("uid=0")) {
                RootStatus.Granted
            } else {
                RootStatus.Denied
            }
        } catch (e: Exception) {
            RootStatus.Denied
        }
    }

    /**
     * Executes a shell command via Root Shell safely.
     */
    suspend fun executeCommand(command: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            val result = Shell.cmd(command).exec()
            CommandResult(
                exitCode = result.code,
                stdout = result.out,
                stderr = result.err
            )
        } catch (e: Exception) {
            CommandResult(
                exitCode = -1,
                stdout = emptyList(),
                stderr = listOf(e.message ?: "Unknown root shell error")
            )
        }
    }
}
