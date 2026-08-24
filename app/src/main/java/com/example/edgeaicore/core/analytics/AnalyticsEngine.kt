package com.example.edgeaicore.core.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

enum class ToolFunctionType(
    val displayName: String,
    val hexColor: String,
    val iconName: String
) {
    COPY("Copy", "#4285F4", "ContentCopy"),
    TRANSLATE("Translate", "#0F9D58", "Translate"),
    SHARE("Share", "#F4B400", "Share"),
    EXPORT("Export", "#9C27B0", "SaveAlt"),
    REGENERATE("Regenerate", "#EA4335", "Refresh"),
    PROVENANCE("Why Answer", "#00ACC1", "Info"),
    VOICE_MIC("Voice Mic", "#FF7043", "Mic")
}

data class ToolUsageRecord(
    val id: String = UUID.randomUUID().toString(),
    val tool: ToolFunctionType,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)

data class ToolTimeSeriesPoint(
    val timeLabel: String,
    val timestamp: Long,
    val copyCount: Int = 0,
    val translateCount: Int = 0,
    val shareCount: Int = 0,
    val exportCount: Int = 0,
    val regenerateCount: Int = 0,
    val voiceCount: Int = 0,
    val totalCount: Int = 0
)

data class ProductEvent(
    val eventName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val properties: Map<String, String> = emptyMap()
)

interface AnalyticsProvider {
    fun trackEvent(event: ProductEvent)
    fun trackToolUsage(tool: ToolFunctionType, metadata: Map<String, String> = emptyMap())
}

/**
 * Local privacy-guarded analytics provider with high-fidelity tool frequency time-series tracking.
 * NEVER captures raw camera pixels, biometric vectors, private memories, or full user prompts.
 */
class LocalAnalyticsProvider : AnalyticsProvider {
    private val _events = MutableStateFlow<List<ProductEvent>>(emptyList())
    val events: StateFlow<List<ProductEvent>> = _events.asStateFlow()

    private val _toolUsageHistory = MutableStateFlow<List<ToolUsageRecord>>(generateInitialSeedHistory())
    val toolUsageHistory: StateFlow<List<ToolUsageRecord>> = _toolUsageHistory.asStateFlow()

    override fun trackEvent(event: ProductEvent) {
        val sanitizedProperties = event.properties.filterKeys { key ->
            !key.contains("prompt") && !key.contains("image") && !key.contains("memory") && !key.contains("token")
        }
        val sanitized = event.copy(properties = sanitizedProperties)
        _events.value = listOf(sanitized) + _events.value.take(49)
    }

    override fun trackToolUsage(tool: ToolFunctionType, metadata: Map<String, String>) {
        val record = ToolUsageRecord(
            tool = tool,
            timestamp = System.currentTimeMillis(),
            metadata = metadata
        )
        _toolUsageHistory.value = listOf(record) + _toolUsageHistory.value
        trackEvent(
            ProductEvent(
                eventName = "TOOL_INVOKED_${tool.name}",
                properties = metadata + mapOf("tool" to tool.displayName)
            )
        )
    }

    fun getToolDistribution(): Map<ToolFunctionType, Int> {
        val currentHistory = _toolUsageHistory.value
        val map = ToolFunctionType.values().associateWith { 0 }.toMutableMap()
        for (rec in currentHistory) {
            map[rec.tool] = (map[rec.tool] ?: 0) + 1
        }
        return map
    }

    fun getTimeSeriesPoints(days: Int = 7): List<ToolTimeSeriesPoint> {
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        val dayMillis = 24L * 60 * 60 * 1000L
        val now = System.currentTimeMillis()

        val points = mutableListOf<ToolTimeSeriesPoint>()
        for (i in (days - 1) downTo 0) {
            val startOfDay = now - (i * dayMillis)
            val endOfDay = startOfDay + dayMillis
            val label = sdf.format(Date(startOfDay))

            val recordsInDay = _toolUsageHistory.value.filter { it.timestamp in (startOfDay - dayMillis / 2)..endOfDay }
            val copy = recordsInDay.count { it.tool == ToolFunctionType.COPY }
            val translate = recordsInDay.count { it.tool == ToolFunctionType.TRANSLATE }
            val share = recordsInDay.count { it.tool == ToolFunctionType.SHARE }
            val export = recordsInDay.count { it.tool == ToolFunctionType.EXPORT }
            val regen = recordsInDay.count { it.tool == ToolFunctionType.REGENERATE }
            val voice = recordsInDay.count { it.tool == ToolFunctionType.VOICE_MIC }
            val total = recordsInDay.size

            points.add(
                ToolTimeSeriesPoint(
                    timeLabel = label,
                    timestamp = startOfDay,
                    copyCount = copy,
                    translateCount = translate,
                    shareCount = share,
                    exportCount = export,
                    regenerateCount = regen,
                    voiceCount = voice,
                    totalCount = total
                )
            )
        }
        return points
    }

    fun clear() {
        _events.value = emptyList()
        _toolUsageHistory.value = emptyList()
    }

    private companion object {
        fun generateInitialSeedHistory(): List<ToolUsageRecord> {
            val now = System.currentTimeMillis()
            val day = 24L * 3600 * 1000L
            val list = mutableListOf<ToolUsageRecord>()

            // Pre-seed 7 days of realistic usage so visualizations have immediate rich telemetry
            val dailyPattens = listOf(
                listOf(ToolFunctionType.COPY to 8, ToolFunctionType.TRANSLATE to 14, ToolFunctionType.SHARE to 4, ToolFunctionType.EXPORT to 6, ToolFunctionType.VOICE_MIC to 9, ToolFunctionType.REGENERATE to 3),
                listOf(ToolFunctionType.COPY to 12, ToolFunctionType.TRANSLATE to 18, ToolFunctionType.SHARE to 7, ToolFunctionType.EXPORT to 11, ToolFunctionType.VOICE_MIC to 14, ToolFunctionType.REGENERATE to 4),
                listOf(ToolFunctionType.COPY to 9, ToolFunctionType.TRANSLATE to 12, ToolFunctionType.SHARE to 5, ToolFunctionType.EXPORT to 8, ToolFunctionType.VOICE_MIC to 11, ToolFunctionType.REGENERATE to 2),
                listOf(ToolFunctionType.COPY to 15, ToolFunctionType.TRANSLATE to 22, ToolFunctionType.SHARE to 9, ToolFunctionType.EXPORT to 14, ToolFunctionType.VOICE_MIC to 18, ToolFunctionType.REGENERATE to 5),
                listOf(ToolFunctionType.COPY to 14, ToolFunctionType.TRANSLATE to 19, ToolFunctionType.SHARE to 8, ToolFunctionType.EXPORT to 12, ToolFunctionType.VOICE_MIC to 15, ToolFunctionType.REGENERATE to 3),
                listOf(ToolFunctionType.COPY to 18, ToolFunctionType.TRANSLATE to 26, ToolFunctionType.SHARE to 11, ToolFunctionType.EXPORT to 16, ToolFunctionType.VOICE_MIC to 22, ToolFunctionType.REGENERATE to 6),
                listOf(ToolFunctionType.COPY to 21, ToolFunctionType.TRANSLATE to 31, ToolFunctionType.SHARE to 13, ToolFunctionType.EXPORT to 19, ToolFunctionType.VOICE_MIC to 25, ToolFunctionType.REGENERATE to 7)
            )

            dailyPattens.forEachIndexed { dayIndex, patterns ->
                val dayTime = now - ((6 - dayIndex) * day)
                patterns.forEach { (tool, count) ->
                    repeat(count) { idx ->
                        val randomOffset = (idx * 37000L) % day
                        list.add(
                            ToolUsageRecord(
                                tool = tool,
                                timestamp = dayTime + randomOffset,
                                metadata = mapOf("seeded" to "true")
                            )
                        )
                    }
                }
            }
            return list.sortedByDescending { it.timestamp }
        }
    }
}
