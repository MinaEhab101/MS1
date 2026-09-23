package com.example.game

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GraphicsQuality(
    val displayName: String,
    val description: String,
    val shadowAlpha: Float,
    val particleMultiplier: Float,
    val enableReflections: Boolean,
    val enableCameraTilt: Boolean,
    val checker3DDepthLayers: Int,
    val enableDynamicShadows: Boolean,
    val enableAntiAliasing: Boolean
) {
    LOW(
        displayName = "Low (Battery Saver)",
        description = "Optimized for older devices & battery saving. 60 FPS guaranteed.",
        shadowAlpha = 0.20f,
        particleMultiplier = 0.25f,
        enableReflections = false,
        enableCameraTilt = false,
        checker3DDepthLayers = 1,
        enableDynamicShadows = false,
        enableAntiAliasing = false
    ),
    MEDIUM(
        displayName = "Medium (Balanced)",
        description = "Balanced visual fidelity and smooth gameplay for mid-range phones.",
        shadowAlpha = 0.55f,
        particleMultiplier = 0.60f,
        enableReflections = false,
        enableCameraTilt = true,
        checker3DDepthLayers = 2,
        enableDynamicShadows = true,
        enableAntiAliasing = true
    ),
    HIGH(
        displayName = "High (Cinematic)",
        description = "Full 3D lighting, dynamic cast shadows, depth blur, and reflections.",
        shadowAlpha = 0.85f,
        particleMultiplier = 1.0f,
        enableReflections = true,
        enableCameraTilt = true,
        checker3DDepthLayers = 3,
        enableDynamicShadows = true,
        enableAntiAliasing = true
    ),
    ULTRA(
        displayName = "Ultra (Royal PBR)",
        description = "Maximum cinematic 3D fidelity with multi-pass bevels & particle aura.",
        shadowAlpha = 1.0f,
        particleMultiplier = 1.5f,
        enableReflections = true,
        enableCameraTilt = true,
        checker3DDepthLayers = 4,
        enableDynamicShadows = true,
        enableAntiAliasing = true
    )
}

data class DevicePerformanceProfile(
    val totalRamGb: Float,
    val cpuCores: Int,
    val recommendedQuality: GraphicsQuality,
    val deviceTierLabel: String
)

object GraphicsSettings {
    private const val PREFS_NAME = "backgammon_king_graphics"
    private const val KEY_QUALITY = "quality_level"
    private const val KEY_AUTO_DETECT = "auto_detect_enabled"

    private val _currentQuality = MutableStateFlow(GraphicsQuality.HIGH)
    val currentQuality: StateFlow<GraphicsQuality> = _currentQuality.asStateFlow()

    private val _isAutoScaling = MutableStateFlow(true)
    val isAutoScaling: StateFlow<Boolean> = _isAutoScaling.asStateFlow()

    private val _deviceProfile = MutableStateFlow<DevicePerformanceProfile?>(null)
    val deviceProfile: StateFlow<DevicePerformanceProfile?> = _deviceProfile.asStateFlow()

    fun initialize(context: Context) {
        val detected = detectDevicePerformance(context)
        _deviceProfile.value = detected

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isAuto = prefs.getBoolean(KEY_AUTO_DETECT, true)
        _isAutoScaling.value = isAuto

        if (isAuto) {
            _currentQuality.value = detected.recommendedQuality
        } else {
            val saved = prefs.getString(KEY_QUALITY, detected.recommendedQuality.name) ?: detected.recommendedQuality.name
            _currentQuality.value = try {
                GraphicsQuality.valueOf(saved)
            } catch (_: Exception) {
                detected.recommendedQuality
            }
        }
    }

    fun setQuality(context: Context, quality: GraphicsQuality, isAuto: Boolean = false) {
        _currentQuality.value = quality
        _isAutoScaling.value = isAuto
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_QUALITY, quality.name)
            .putBoolean(KEY_AUTO_DETECT, isAuto)
            .apply()
    }

    fun detectDevicePerformance(context: Context): DevicePerformanceProfile {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalRamBytes = memoryInfo.totalMem
        val totalRamGb = totalRamBytes / (1024f * 1024f * 1024f)
        val cores = Runtime.getRuntime().availableProcessors()

        val (recommended, tierLabel) = when {
            totalRamGb >= 7.5f && cores >= 8 -> GraphicsQuality.ULTRA to "Ultra Tier (Flagship)"
            totalRamGb >= 5.5f && cores >= 6 -> GraphicsQuality.HIGH to "High Tier (Performance)"
            totalRamGb >= 3.2f && cores >= 4 -> GraphicsQuality.MEDIUM to "Mid Tier (Balanced)"
            else -> GraphicsQuality.LOW to "Entry Tier (Battery Saver)"
        }

        return DevicePerformanceProfile(
            totalRamGb = totalRamGb,
            cpuCores = cores,
            recommendedQuality = recommended,
            deviceTierLabel = tierLabel
        )
    }
}
