package com.example.edgeaicore.core.context

import android.content.Context
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.mediapipe.VisionResult
import com.example.edgeaicore.core.memory.MemoryEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

interface ContextProvider {
    val id: String
    val displayName: String
    val privacyLevel: PrivacyLevel
    val requiredPermission: String?
    var isEnabled: Boolean
    val lastUpdated: Long
    suspend fun getContextSummary(): String?
}

data class ContextSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String,
    val location: String?,
    val weather: String?,
    val nextCalendarEvent: String?,
    val activeTask: String?,
    val recentMemorySummary: String?,
    val currentActivity: String?,
    val visionPerception: String?,
    val userPreferences: Map<String, String> = emptyMap()
) {
    fun toPromptContext(): String {
        val list = mutableListOf<String>()
        list.add("System Time: $formattedTime")
        if (!location.isNullOrBlank()) list.add("Location: $location")
        if (!weather.isNullOrBlank()) list.add("Weather: $weather")
        if (!nextCalendarEvent.isNullOrBlank()) list.add("Next Event: $nextCalendarEvent")
        if (!activeTask.isNullOrBlank()) list.add("Active Task: $activeTask")
        if (!recentMemorySummary.isNullOrBlank()) list.add("Recent Context: $recentMemorySummary")
        if (!currentActivity.isNullOrBlank()) list.add("User Activity: $currentActivity")
        if (!visionPerception.isNullOrBlank()) list.add("Visual Scene: $visionPerception")
        return list.joinToString("\n")
    }
}

class ContextEngine(
    private val context: Context,
    private val memoryEngine: MemoryEngine
) {
    private val _snapshot = MutableStateFlow(createEmptySnapshot())
    val snapshot: StateFlow<ContextSnapshot> = _snapshot.asStateFlow()

    // Modular Context Providers
    var locationProviderEnabled: Boolean = true
    var calendarProviderEnabled: Boolean = true
    var taskProviderEnabled: Boolean = true
    var weatherProviderEnabled: Boolean = true
    var activityProviderEnabled: Boolean = true
    var visionProviderEnabled: Boolean = true

    private var latestVisionSummary: String? = null

    fun updateVisionContext(result: VisionResult) {
        if (visionProviderEnabled) {
            latestVisionSummary = result.toCompactSummary()
            refreshSnapshot()
        }
    }

    fun refreshSnapshot(): ContextSnapshot {
        val now = System.currentTimeMillis()
        val timeStr = SimpleDateFormat("EEEE, MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(now))

        val loc = if (locationProviderEnabled) "On-Device Geolocation (Local Sandbox)" else null
        val weather = if (weatherProviderEnabled) "Local Estimate: 21°C, Clear" else null
        val calendar = if (calendarProviderEnabled) "10:30 AM - EdgeAI Core Architecture Sync" else null
        val task = if (taskProviderEnabled) "Verify on-device LiteRT-LM pipeline" else null
        val activity = if (activityProviderEnabled) "Stationary / Developer Mode" else null

        val snap = ContextSnapshot(
            timestamp = now,
            formattedTime = timeStr,
            location = loc,
            weather = weather,
            nextCalendarEvent = calendar,
            activeTask = task,
            recentMemorySummary = "Encrypted Local Memory Store Active",
            currentActivity = activity,
            visionPerception = if (visionProviderEnabled) latestVisionSummary else null,
            userPreferences = mapOf("privacy_mode" to "strict_local", "latency_profile" to "balanced")
        )
        _snapshot.value = snap
        return snap
    }

    private fun createEmptySnapshot(): ContextSnapshot {
        val now = System.currentTimeMillis()
        val timeStr = SimpleDateFormat("EEEE, MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(now))
        return ContextSnapshot(
            timestamp = now,
            formattedTime = timeStr,
            location = null,
            weather = null,
            nextCalendarEvent = null,
            activeTask = null,
            recentMemorySummary = null,
            currentActivity = null,
            visionPerception = null
        )
    }
}
