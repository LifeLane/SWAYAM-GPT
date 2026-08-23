package com.example.edgeaicore.core.mcp

import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.RiskLevel

/**
 * Standard MCP Trust Levels for Server Connections.
 */
enum class McpTrustLevel {
    /** Internal on-device server or loopback transport. Fully trusted. */
    TRUSTED_LOCAL,
    /** Verified private LAN / VPC server with mutual TLS / secure gateway token. */
    TRUSTED_PRIVATE,
    /** External remote MCP server explicitly authorized by the user. */
    USER_APPROVED_REMOTE,
    /** Unverified or unknown remote MCP server. Restricted from sensitive tools/context. */
    UNTRUSTED
}

/**
 * JSON-RPC 2.0 & MCP Protocol Data Classes.
 */
data class McpJsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: String,
    val method: String,
    val params: Map<String, Any?> = emptyMap()
)

data class McpJsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: String?,
    val result: Any? = null,
    val error: McpJsonRpcError? = null
)

data class McpJsonRpcError(
    val code: Int,
    val message: String,
    val data: Any? = null
)

// MCP Capability & Tool Specifications
data class McpTool(
    val name: String,
    val description: String,
    val inputSchema: Map<String, Any?> = emptyMap(),
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val privacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY,
    val requiresConfirmation: Boolean = false,
    val serverId: String = "local"
)

data class McpResource(
    val uri: String,
    val name: String,
    val description: String? = null,
    val mimeType: String = "application/json",
    val serverId: String = "local"
)

data class McpPrompt(
    val name: String,
    val description: String? = null,
    val arguments: List<McpPromptArgument> = emptyList(),
    val serverId: String = "local"
)

data class McpPromptArgument(
    val name: String,
    val description: String? = null,
    val required: Boolean = false
)

data class McpServerInfo(
    val name: String,
    val version: String,
    val description: String? = null,
    val trustLevel: McpTrustLevel = McpTrustLevel.TRUSTED_LOCAL,
    val endpoint: String? = null
)

data class McpServerCapabilities(
    val tools: Boolean = true,
    val resources: Boolean = true,
    val prompts: Boolean = true,
    val logging: Boolean = true
)

data class McpInitializeResult(
    val protocolVersion: String = "2024-11-05",
    val serverInfo: McpServerInfo,
    val capabilities: McpServerCapabilities
)

data class McpToolCallResult(
    val isError: Boolean = false,
    val content: List<McpContentItem> = emptyList(),
    val structuredData: Map<String, Any?>? = null
)

data class McpContentItem(
    val type: String = "text",
    val text: String? = null,
    val data: String? = null,
    val mimeType: String? = null
)
