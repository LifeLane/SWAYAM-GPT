package com.example.edgeaicore.core.mcp

import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class McpAuditEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val serverId: String,
    val action: String,
    val target: String,
    val trustLevel: McpTrustLevel,
    val success: Boolean,
    val details: String
)

/**
 * Enforces MCP Trust Hierarchy, Server Allowlist, Rate Limiting, and Security Boundaries.
 * Treats all remote/untrusted server descriptions and prompts as untrusted input.
 */
class McpSecurityManager {
    private val _approvedServers = MutableStateFlow<Map<String, McpTrustLevel>>(
        mapOf("local" to McpTrustLevel.TRUSTED_LOCAL)
    )
    val approvedServers: StateFlow<Map<String, McpTrustLevel>> = _approvedServers.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<McpAuditEntry>>(emptyList())
    val auditLogs: StateFlow<List<McpAuditEntry>> = _auditLogs.asStateFlow()

    // Simple sliding-window rate limiter per server
    private val invocationTimestamps = mutableMapOf<String, MutableList<Long>>()
    private val maxInvocationsPerMinute = 60

    fun authorizeServer(serverId: String, trustLevel: McpTrustLevel) {
        val current = _approvedServers.value.toMutableMap()
        current[serverId] = trustLevel
        _approvedServers.value = current
        logAudit(serverId, "AUTHORIZE_SERVER", serverId, trustLevel, true, "Trust level set to $trustLevel")
    }

    fun revokeServer(serverId: String) {
        val current = _approvedServers.value.toMutableMap()
        current.remove(serverId)
        _approvedServers.value = current
        logAudit(serverId, "REVOKE_SERVER", serverId, McpTrustLevel.UNTRUSTED, true, "Server authorization revoked")
    }

    fun getTrustLevel(serverId: String): McpTrustLevel {
        return _approvedServers.value[serverId] ?: McpTrustLevel.UNTRUSTED
    }

    fun validateToolInvocation(
        serverId: String,
        toolName: String,
        requiredPrivacy: PrivacyLevel
    ): EdgeResult<Unit> {
        val trust = getTrustLevel(serverId)

        // Rate limit check
        val now = System.currentTimeMillis()
        val timestamps = invocationTimestamps.getOrPut(serverId) { mutableListOf() }
        timestamps.removeAll { it < now - 60_000 }
        if (timestamps.size >= maxInvocationsPerMinute) {
            logAudit(serverId, "TOOL_CALL_REJECTED", toolName, trust, false, "Rate limit exceeded")
            return EdgeResult.Failure(EdgeAIError.RateLimitExceeded("MCP Server $serverId", maxInvocationsPerMinute))
        }
        timestamps.add(now)

        // Trust Level Boundary Checks
        if (trust == McpTrustLevel.UNTRUSTED) {
            logAudit(serverId, "TOOL_CALL_REJECTED", toolName, trust, false, "Untrusted server rejected")
            return EdgeResult.Failure(EdgeAIError.McpSecurityViolation(serverId, "Untrusted MCP servers cannot execute tools"))
        }

        if (requiredPrivacy == PrivacyLevel.LOCAL_ONLY && trust != McpTrustLevel.TRUSTED_LOCAL) {
            logAudit(serverId, "TOOL_CALL_REJECTED", toolName, trust, false, "LOCAL_ONLY tool call attempted from remote server")
            return EdgeResult.Failure(EdgeAIError.PrivacyViolation("LOCAL_ONLY tools can only be invoked by TRUSTED_LOCAL MCP sessions"))
        }

        logAudit(serverId, "TOOL_CALL_ALLOWED", toolName, trust, true, "Validation passed")
        return EdgeResult.Success(Unit)
    }

    fun logAudit(
        serverId: String,
        action: String,
        target: String,
        trustLevel: McpTrustLevel,
        success: Boolean,
        details: String
    ) {
        val entry = McpAuditEntry(
            serverId = serverId,
            action = action,
            target = target,
            trustLevel = trustLevel,
            success = success,
            details = details
        )
        _auditLogs.value = (listOf(entry) + _auditLogs.value).take(100)
    }
}
