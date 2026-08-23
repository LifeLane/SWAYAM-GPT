package com.example.edgeaicore.core.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.edgeaicore.core.common.ExecutionBackend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileFilter

data class DeviceSpecs(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val cpuCores: Int,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val totalStorageGb: Double,
    val availableStorageGb: Double,
    val isGpuAvailable: Boolean,
    val isNpuAvailable: Boolean,
    val recommendedBackend: ExecutionBackend
)

data class DiagnosticsMetrics(
    val cameraFps: Double = 0.0,
    val lastInferenceLatencyMs: Long = 0,
    val averageInferenceLatencyMs: Long = 0,
    val tokensPerSecond: Double = 0.0,
    val totalInferences: Long = 0,
    val successfulInferences: Long = 0,
    val memoryUsageMb: Long = 0,
    val batteryPercent: Int = 100,
    val isBatteryCharging: Boolean = false,
    val activeBackend: ExecutionBackend = ExecutionBackend.AUTO,
    val activeModelId: String = "None",
    val isThermalThrottled: Boolean = false,
    val networkLatencyMs: Long = 0,
    // Extended MCP & Agent Engine Diagnostics
    val mcpConnectedServers: Int = 1,
    val mcpServerLatencyMs: Long = 0,
    val toolInvocationLatencyMs: Long = 0,
    val agentStepCount: Int = 0,
    val agentTokensUsed: Int = 0,
    val privateServerLatencyMs: Long = 0,
    val localInferenceLatencyMs: Long = 0,
    val providerSelected: String = "LOCAL",
    val policyDecisionsCount: Long = 0,
    val toolFailuresCount: Long = 0
)

class DeviceCapabilityManager(private val context: Context) {

    fun getDeviceSpecs(): DeviceSpecs {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availableRamMb = memInfo.availMem / (1024 * 1024)

        val stat = StatFs(Environment.getDataDirectory().path)
        val totalStorageGb = (stat.blockCountLong * stat.blockSizeLong) / (1024.0 * 1024.0 * 1024.0)
        val availableStorageGb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024.0 * 1024.0 * 1024.0)

        val cpuCores = getNumberOfCores()
        val hasNpu = detectNpuSupport()
        val hasGpu = true // Modern Android devices running minSdk 24 have GLES 3.0+ / Vulkan

        val recommendedBackend = when {
            hasNpu && totalRamMb >= 4096 -> ExecutionBackend.NPU
            hasGpu && totalRamMb >= 3072 -> ExecutionBackend.GPU
            else -> ExecutionBackend.CPU
        }

        return DeviceSpecs(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            cpuCores = cpuCores,
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            totalStorageGb = totalStorageGb,
            availableStorageGb = availableStorageGb,
            isGpuAvailable = hasGpu,
            isNpuAvailable = hasNpu,
            recommendedBackend = recommendedBackend
        )
    }

    private fun detectNpuSupport(): Boolean {
        // Safe check for NNAPI/NPU hardware features on Android 10+
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val soc = Build.SOC_MODEL.lowercase()
            soc.contains("tensor") || soc.contains("snapdragon") || soc.contains("dimensity") || soc.contains("exynos")
        } else {
            false
        }
    }

    private fun getNumberOfCores(): Int {
        return try {
            val dir = File("/sys/devices/system/cpu/")
            val files = dir.listFiles(FileFilter { file ->
                file.name.matches(Regex("cpu[0-9]+"))
            })
            files?.size ?: Runtime.getRuntime().availableProcessors()
        } catch (e: Exception) {
            Runtime.getRuntime().availableProcessors()
        }
    }
}

class PerformanceMonitor(private val context: Context) {
    private val _metrics = MutableStateFlow(DiagnosticsMetrics())
    val metrics: StateFlow<DiagnosticsMetrics> = _metrics.asStateFlow()

    private val latencyHistory = mutableListOf<Long>()

    fun recordInference(
        latencyMs: Long,
        tokensGenerated: Int = 0,
        success: Boolean = true,
        modelId: String = "gemma-2b-it-litert",
        backend: ExecutionBackend = ExecutionBackend.GPU
    ) {
        synchronized(latencyHistory) {
            latencyHistory.add(latencyMs)
            if (latencyHistory.size > 50) latencyHistory.removeAt(0)
        }

        val avgLatency = if (latencyHistory.isNotEmpty()) latencyHistory.average().toLong() else latencyMs
        val tokensPerSec = if (latencyMs > 0 && tokensGenerated > 0) {
            (tokensGenerated.toDouble() / (latencyMs.toDouble() / 1000.0))
        } else {
            0.0
        }

        val runtime = Runtime.getRuntime()
        val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

        val (batteryPct, isCharging) = getBatteryInfo()

        _metrics.value = _metrics.value.copy(
            lastInferenceLatencyMs = latencyMs,
            averageInferenceLatencyMs = avgLatency,
            tokensPerSecond = tokensPerSec,
            totalInferences = _metrics.value.totalInferences + 1,
            successfulInferences = _metrics.value.successfulInferences + (if (success) 1 else 0),
            memoryUsageMb = usedMemMb,
            batteryPercent = batteryPct,
            isBatteryCharging = isCharging,
            activeBackend = backend,
            activeModelId = modelId
        )
    }

    fun updateCameraFps(fps: Double) {
        _metrics.value = _metrics.value.copy(cameraFps = fps)
    }

    fun updateNetworkLatency(latencyMs: Long) {
        _metrics.value = _metrics.value.copy(networkLatencyMs = latencyMs)
    }

    private fun getBatteryInfo(): Pair<Int, Boolean> {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, ifilter)
            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            Pair(pct, isCharging)
        } catch (e: Exception) {
            Pair(100, false)
        }
    }
}
