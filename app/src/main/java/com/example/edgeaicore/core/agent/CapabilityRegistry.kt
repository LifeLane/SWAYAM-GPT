package com.example.edgeaicore.core.agent

import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.mcp.McpClient
import com.example.edgeaicore.core.mcp.McpTool
import com.example.edgeaicore.core.policy.PolicyEngine
import com.example.edgeaicore.core.tools.Tool
import com.example.edgeaicore.core.tools.ToolCategory
import com.example.edgeaicore.core.tools.ToolRegistry

/**
 * CapabilityRegistry: Maps high-level capabilities to local Native tools and MCP tools.
 * Filters tools dynamically by agent profile and privacy policy.
 */
class CapabilityRegistry(
    val toolRegistry: ToolRegistry,
    val mcpClient: McpClient,
    val policyEngine: PolicyEngine
) {
    /**
     * Map AgentCapability to ToolCategory.
     */
    fun getToolCategoryForCapability(capability: AgentCapability): ToolCategory {
        return when (capability) {
            AgentCapability.VISION -> ToolCategory.VISION
            AgentCapability.MEMORY -> ToolCategory.MEMORY
            AgentCapability.LOCATION -> ToolCategory.LOCATION
            AgentCapability.CALENDAR -> ToolCategory.CALENDAR
            AgentCapability.TASKS -> ToolCategory.TASKS
            AgentCapability.WEATHER -> ToolCategory.WEATHER
            AgentCapability.NAVIGATION -> ToolCategory.NAVIGATION
            AgentCapability.AUDIO -> ToolCategory.AUDIO
            AgentCapability.FILES -> ToolCategory.FILES
            AgentCapability.AI -> ToolCategory.AI
            AgentCapability.AUTOMATION -> ToolCategory.AUTOMATION
        }
    }

    /**
     * Discovers relevant tools for a given user intent, constrained strictly by profile and policy.
     * Only returns the minimum required set of tools so that LLM prompt context is never overloaded.
     */
    fun discoverRelevantTools(
        profile: AgentProfile,
        intent: String,
        userConsentGiven: Boolean = false
    ): List<Tool> {
        val lowerIntent = intent.lowercase()
        val allLocalTools = toolRegistry.getAll()
        val mcpTools = mcpClient.toolRegistry.getAllTools()

        // Filter local tools by profile's allowed capabilities
        val allowedCategories = profile.allowedCapabilities.map { getToolCategoryForCapability(it) }.toSet()

        val matchingLocal = allLocalTools.filter { tool ->
            (tool.category in allowedCategories || tool.id in profile.allowedToolIds) &&
                    tool.privacyLevel <= profile.maxPrivacyLevel &&
                    tool.riskLevel <= profile.maxAllowedRisk &&
                    tool.enabled
        }.filter { tool ->
            // Semantic keyword relevance filtering to expose only the minimal needed subset
            val keywords = (tool.name + " " + tool.description + " " + tool.id).lowercase()
            lowerIntent.split(" ").any { word -> word.length > 3 && keywords.contains(word) } ||
                    lowerIntent.contains("help") ||
                    lowerIntent.contains("run") ||
                    lowerIntent.contains("do") ||
                    lowerIntent.contains("agent")
        }

        // Apply policy filter
        val policyApprovedLocal = matchingLocal.filter { tool ->
            policyEngine.evaluateToolPolicy(tool, userConsentGiven).allowed
        }

        // If specific matching local tools were found, return top results
        if (policyApprovedLocal.isNotEmpty()) {
            return policyApprovedLocal.take(profile.toolBudget)
        }

        // Otherwise return safe default tools matching profile
        return allLocalTools.filter { it.category in allowedCategories && it.enabled }
            .take(profile.toolBudget)
    }

    /**
     * Returns all active capabilities currently available across the system.
     */
    fun getAvailableCapabilities(): List<AgentCapability> {
        return AgentCapability.values().toList()
    }
}
