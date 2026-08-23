package com.example.edgeaicore.core.tools

import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.RiskLevel

/**
 * Standard tool categories across EdgeAI Core.
 */
enum class ToolCategory {
    MEMORY,
    VISION,
    AUDIO,
    LOCATION,
    CALENDAR,
    TASKS,
    NOTIFICATIONS,
    FILES,
    DEVICE,
    WEATHER,
    NAVIGATION,
    PRODUCTIVITY,
    AI,
    AUTOMATION
}

/**
 * Tool Provider Source.
 */
enum class ToolProviderType {
    NATIVE_LOCAL,
    MCP_INTERNAL,
    MCP_REMOTE,
    PRIVATE_SERVER
}

/**
 * Structured tool definition for the EdgeAI Core Agent Engine.
 */
data class Tool(
    val id: String,
    val name: String,
    val description: String,
    val category: ToolCategory,
    val inputSchema: Map<String, String> = emptyMap(),
    val outputSchema: Map<String, String> = emptyMap(),
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val privacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY,
    val requiredPermissions: List<String> = emptyList(),
    val requiresConfirmation: Boolean = riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL,
    val provider: ToolProviderType = ToolProviderType.NATIVE_LOCAL,
    val enabled: Boolean = true,
    val handler: (suspend (Map<String, Any?>) -> EdgeResult<Map<String, Any?>>)? = null
)

/**
 * Tool Registry: Centralized registry for all local and MCP-provided agent tools.
 */
class ToolRegistry {
    private val toolsMap = mutableMapOf<String, Tool>()

    fun register(tool: Tool) {
        toolsMap[tool.id] = tool
    }

    fun unregister(toolId: String) {
        toolsMap.remove(toolId)
    }

    fun get(toolId: String): Tool? = toolsMap[toolId]

    fun getAll(): List<Tool> = toolsMap.values.toList()

    fun getByCategory(category: ToolCategory): List<Tool> =
        toolsMap.values.filter { it.category == category && it.enabled }

    fun getByPrivacy(maxPrivacy: PrivacyLevel): List<Tool> =
        toolsMap.values.filter { it.privacyLevel <= maxPrivacy && it.enabled }

    fun setEnabled(toolId: String, enabled: Boolean) {
        val tool = toolsMap[toolId] ?: return
        toolsMap[toolId] = tool.copy(enabled = enabled)
    }

    fun clear() {
        toolsMap.clear()
    }
}
