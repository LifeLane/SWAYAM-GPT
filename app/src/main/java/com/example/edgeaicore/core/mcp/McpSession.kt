package com.example.edgeaicore.core.mcp

import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * Manages an active MCP Session over a given McpTransport.
 * Provides timeout, keepalive, and error boundary handling.
 */
class McpSession(
    val serverId: String,
    val transport: McpTransport,
    val trustLevel: McpTrustLevel = McpTrustLevel.TRUSTED_LOCAL,
    val defaultTimeoutMs: Long = 8000L
) {
    var serverInfo: McpServerInfo? = null
        private set
    var capabilities: McpServerCapabilities? = null
        private set

    val isConnected: Boolean get() = transport.isConnected

    suspend fun initialize(): EdgeResult<McpInitializeResult> {
        val connectRes = transport.connect()
        if (connectRes is EdgeResult.Failure) return connectRes

        val req = McpJsonRpcRequest(
            id = UUID.randomUUID().toString(),
            method = "initialize",
            params = mapOf(
                "protocolVersion" to "2024-11-05",
                "clientInfo" to mapOf("name" to "EdgeAI-Core-Agent", "version" to "2.4.0"),
                "capabilities" to mapOf("tools" to true, "resources" to true, "prompts" to true)
            )
        )

        val res = executeWithTimeout(req)
        return when (res) {
            is EdgeResult.Success -> {
                val map = res.data.result as? Map<String, Any?> ?: emptyMap()
                val infoMap = map["serverInfo"] as? Map<String, Any?> ?: emptyMap()
                val info = McpServerInfo(
                    name = infoMap["name"] as? String ?: serverId,
                    version = infoMap["version"] as? String ?: "1.0",
                    description = infoMap["description"] as? String,
                    trustLevel = trustLevel
                )
                this.serverInfo = info
                this.capabilities = McpServerCapabilities()
                EdgeResult.Success(McpInitializeResult("2024-11-05", info, McpServerCapabilities()))
            }
            is EdgeResult.Failure -> res
        }
    }

    suspend fun close() {
        transport.disconnect()
    }

    suspend fun executeWithTimeout(
        request: McpJsonRpcRequest,
        timeoutMs: Long = defaultTimeoutMs
    ): EdgeResult<McpJsonRpcResponse> {
        val result = withTimeoutOrNull(timeoutMs) {
            transport.send(request)
        }
        return result ?: EdgeResult.Failure(
            EdgeAIError.McpProtocolError(serverId, "MCP Request '${request.method}' timed out after ${timeoutMs}ms")
        )
    }
}
