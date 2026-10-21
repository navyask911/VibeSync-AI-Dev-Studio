package com.vibesync.promptstudio.presentation.devoptions

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.view.WindowManager
import java.util.Locale

data class DeviceHardwareInfo(
    val androidRelease: String,
    val apiLevel: Int,
    val kernelVersion: String,
    val socModel: String,
    val cpuAbi: String,
    val refreshRateHz: Float,
    val availableRamBytes: Long,
    val totalRamBytes: Long
) {
    companion object {
        fun read(context: Context): DeviceHardwareInfo {
            val memoryInfo = ActivityManager.MemoryInfo()
            (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
                ?.getMemoryInfo(memoryInfo)

            val windowManager = context.getSystemService(WindowManager::class.java)
            val refreshRate = runCatching {
                windowManager?.defaultDisplay?.refreshRate ?: 0f
            }.getOrDefault(0f)

            val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Build.SOC_MODEL.takeIf { it.isNotBlank() && it != "unknown" }
            } else {
                null
            } ?: Build.HARDWARE

            return DeviceHardwareInfo(
                androidRelease = Build.VERSION.RELEASE ?: "Unknown",
                apiLevel = Build.VERSION.SDK_INT,
                kernelVersion = System.getProperty("os.version") ?: "Unavailable",
                socModel = soc.ifBlank { "Unavailable" },
                cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unavailable",
                refreshRateHz = refreshRate,
                availableRamBytes = memoryInfo.availMem,
                totalRamBytes = memoryInfo.totalMem
            )
        }

        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "Unavailable"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var amount = bytes.toDouble()
            var unit = 0
            while (amount >= 1024 && unit < units.lastIndex) {
                amount /= 1024
                unit++
            }
            return String.format(Locale.getDefault(), "%.1f %s", amount, units[unit])
        }
    }
}
