package com.example.edgeaicore.core.policy

import android.content.Context
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.RiskLevel
import com.example.edgeaicore.core.privacy.PrivacyEngine
import com.example.edgeaicore.core.tools.Tool
import com.example.edgeaicore.core.tools.ToolProviderType

/**
 * Outcome of policy evaluation for a tool or AI execution request.
 */
data class PolicyDecision(
    val allowed: Boolean,
    val reason: String,
    val requiresUserConfirmation: Boolean = false,
    val isRemoteTransmission: Boolean = false
)

/**
 * PolicyEngine: Central arbiter for data privacy, capability boundaries,
 * risk gating, and remote execution policies.
 */
class PolicyEngine(
    private val context: Context,
    private val privacyEngine: PrivacyEngine
) {
    /**
     * Evaluates whether a tool execution satisfies strict privacy, risk, and provider boundaries.
     */
    fun evaluateToolPolicy(
        tool: Tool,
        userConsentGiven: Boolean = false,
        callerTrust: PrivacyLevel = PrivacyLevel.LOCAL_ONLY
    ): PolicyDecision {
        // 1. Tool Enabled check
        if (!tool.enabled) {
            return PolicyDecision(allowed = false, reason = "Tool '${tool.id}' is disabled by user configuration.")
        }

        // 2. Privacy Level Boundary Rules
        when (tool.privacyLevel) {
            PrivacyLevel.LOCAL_ONLY -> {
                if (tool.provider == ToolProviderType.MCP_REMOTE) {
                    return PolicyDecision(
                        allowed = false,
                        reason = "LOCAL_ONLY tool '${tool.id}' cannot be executed over remote MCP provider."
                    )
                }
            }
            PrivacyLevel.SENSITIVE -> {
                val privateServerAllowed = privacyEngine.dashboardState.value.privateServerEnabled
                if (tool.provider == ToolProviderType.MCP_REMOTE) {
                    return PolicyDecision(
                        allowed = false,
                        reason = "SENSITIVE data tool '${tool.id}' cannot be routed to public remote MCP."
                    )
                }
                if (tool.provider == ToolProviderType.PRIVATE_SERVER && (!privateServerAllowed || !userConsentGiven)) {
                    return PolicyDecision(
                        allowed = false,
                        reason = "SENSITIVE tool requires explicit private server authorization."
                    )
                }
            }
            PrivacyLevel.PRIVATE -> {
                val privateServerAllowed = privacyEngine.dashboardState.value.privateServerEnabled
                if (tool.provider == ToolProviderType.PRIVATE_SERVER && !privateServerAllowed) {
                    return PolicyDecision(
                        allowed = false,
                        reason = "Private server access is disabled in Privacy Center."
                    )
                }
                if (tool.provider == ToolProviderType.MCP_REMOTE) {
                    return PolicyDecision(
                        allowed = false,
                        reason = "PRIVATE tools cannot be dispatched to untrusted remote MCP."
                    )
                }
            }
            PrivacyLevel.PUBLIC -> {
                val cloudAllowed = privacyEngine.dashboardState.value.cloudAiEnabled
                if (tool.provider == ToolProviderType.MCP_REMOTE && !cloudAllowed) {
                    return PolicyDecision(
                        allowed = false,
                        reason = "Remote MCP execution is disabled in Privacy Center."
                    )
                }
            }
        }

        // 3. Risk Level Evaluation: HIGH and CRITICAL must never execute automatically
        val requiresConfirmation = tool.requiresConfirmation ||
                tool.riskLevel == RiskLevel.HIGH ||
                tool.riskLevel == RiskLevel.CRITICAL

        return PolicyDecision(
            allowed = true,
            reason = "Policy criteria satisfied for tool '${tool.id}'",
            requiresUserConfirmation = requiresConfirmation,
            isRemoteTransmission = tool.provider == ToolProviderType.MCP_REMOTE || tool.provider == ToolProviderType.PRIVATE_SERVER
        )
    }

    /**
     * Evaluates whether an AI routing request satisfies privacy containment rules.
     */
    fun evaluateAiRoutingPolicy(
        privacyLevel: PrivacyLevel,
        targetProvider: AIProviderType,
        userConsentGiven: Boolean
    ): Boolean {
        return privacyEngine.validateRouting(privacyLevel, targetProvider, userConsentGiven)
    }
}
