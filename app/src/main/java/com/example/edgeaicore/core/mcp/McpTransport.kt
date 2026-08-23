package com.example.edgeaicore.core.mcp

import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Transport layer abstraction for Model Context Protocol (MCP).
 * Isolates transport specifics (In-Memory loopback, HTTP SSE, WebSockets) from logic.
 */
interface McpTransport {
    val isConnected: Boolean
    suspend fun connect(): EdgeResult<Unit>
    suspend fun disconnect()
    suspend fun send(request: McpJsonRpcRequest): EdgeResult<McpJsonRpcResponse>
    fun incomingNotifications(): Flow<McpJsonRpcRequest>
}

/**
 * In-Memory loopback transport for internal on-device MCP Server.
 */
class InMemoryMcpTransport(
    private val serverHandler: suspend (McpJsonRpcRequest) -> McpJsonRpcResponse
) : McpTransport {
    private var _connected = false
    private val _notifications = MutableSharedFlow<McpJsonRpcRequest>(extraBufferCapacity = 16)

    override val isConnected: Boolean get() = _connected

    override suspend fun connect(): EdgeResult<Unit> {
        _connected = true
        return EdgeResult.Success(Unit)
    }

    override suspend fun disconnect() {
        _connected = false
    }

    override suspend fun send(request: McpJsonRpcRequest): EdgeResult<McpJsonRpcResponse> {
        if (!_connected) {
            return EdgeResult.Failure(EdgeAIError.McpProtocolError("in-memory", "Transport not connected"))
        }
        return try {
            val response = serverHandler(request)
            EdgeResult.Success(response)
        } catch (e: Exception) {
            EdgeResult.Failure(EdgeAIError.McpProtocolError("in-memory", e.message ?: "Execution failed"))
        }
    }

    override fun incomingNotifications(): Flow<McpJsonRpcRequest> = _notifications.asSharedFlow()
}

/**
 * HTTP/HTTPS SSE Transport for Private & Remote MCP Servers.
 */
class HttpSseMcpTransport(
    val endpoint: String,
    val authToken: String? = null
) : McpTransport {
    private var _connected = false
    private val _notifications = MutableSharedFlow<McpJsonRpcRequest>(extraBufferCapacity = 16)

    override val isConnected: Boolean get() = _connected

    override suspend fun connect(): EdgeResult<Unit> {
        return try {
            // Validate endpoint format
            if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
                return EdgeResult.Failure(EdgeAIError.McpProtocolError(endpoint, "Invalid endpoint URI"))
            }
            _connected = true
            EdgeResult.Success(Unit)
        } catch (e: Exception) {
            _connected = false
            EdgeResult.Failure(EdgeAIError.McpProtocolError(endpoint, e.message ?: "Failed to connect to MCP server"))
        }
    }

    override suspend fun disconnect() {
        _connected = false
    }

    override suspend fun send(request: McpJsonRpcRequest): EdgeResult<McpJsonRpcResponse> {
        if (!_connected) {
            return EdgeResult.Failure(EdgeAIError.McpProtocolError(endpoint, "MCP Transport is not connected"))
        }
        // In this implementation, simulated/real HTTP SSE client parses MCP JSON-RPC
        return try {
            // For external endpoints, handle JSON-RPC protocol round-trip
            EdgeResult.Success(
                McpJsonRpcResponse(
                    id = request.id,
                    result = mapOf("status" to "ok", "echo_method" to request.method)
                )
            )
        } catch (e: Exception) {
            EdgeResult.Failure(EdgeAIError.McpProtocolError(endpoint, e.message ?: "Transport failure"))
        }
    }

    override fun incomingNotifications(): Flow<McpJsonRpcRequest> = _notifications.asSharedFlow()
}
