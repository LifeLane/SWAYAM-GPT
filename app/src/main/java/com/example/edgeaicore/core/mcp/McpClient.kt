package com.example.edgeaicore.core.mcp

import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.RiskLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * High-level MCP Client Manager.
 * Orchestrates multi-server MCP connections, capability/tool/resource/prompt discovery,
 * and secure remote/local MCP tool invocations.
 */
class McpClient(
    val securityManager: McpSecurityManager = McpSecurityManager()
) {
    private val sessions = mutableMapOf<String, McpSession>()

    val toolRegistry = McpToolRegistry()
    val resourceRegistry = McpResourceRegistry()
    val promptRegistry = McpPromptRegistry()

    private val _connectedServers = MutableStateFlow<List<McpServerInfo>>(emptyList())
    val connectedServers: StateFlow<List<McpServerInfo>> = _connectedServers.asStateFlow()

    suspend fun registerAndConnectSession(
        serverId: String,
        transport: McpTransport,
        trustLevel: McpTrustLevel
    ): EdgeResult<McpInitializeResult> {
        securityManager.authorizeServer(serverId, trustLevel)
        val session = McpSession(serverId, transport, trustLevel)
        val initRes = session.initialize()

        if (initRes is EdgeResult.Success) {
            sessions[serverId] = session
            updateConnectedServers()
            // Automatically discover capabilities
            discoverCapabilities(serverId)
        }
        return initRes
    }

    suspend fun disconnectSession(serverId: String) {
        sessions[serverId]?.close()
        sessions.remove(serverId)
        toolRegistry.unregisterServer(serverId)
        resourceRegistry.unregisterServer(serverId)
        promptRegistry.unregisterServer(serverId)
        securityManager.revokeServer(serverId)
        updateConnectedServers()
    }

    private fun updateConnectedServers() {
        _connectedServers.value = sessions.values.mapNotNull { it.serverInfo }
    }

    suspend fun discoverCapabilities(serverId: String): EdgeResult<Unit> {
        val session = sessions[serverId]
            ?: return EdgeResult.Failure(EdgeAIError.McpProtocolError(serverId, "Session not found"))

        // 1. Discover Tools
        val toolReq = McpJsonRpcRequest(
            id = UUID.randomUUID().toString(),
            method = "tools/list"
        )
        val toolRes = session.executeWithTimeout(toolReq)
        if (toolRes is EdgeResult.Success) {
            val map = toolRes.data.result as? Map<String, Any?> ?: emptyMap()
            val toolsList = (map["tools"] as? List<Map<String, Any?>>)?.map { tMap ->
                McpTool(
                    name = tMap["name"] as? String ?: "unnamed",
                    description = tMap["description"] as? String ?: "",
                    inputSchema = tMap["inputSchema"] as? Map<String, Any?> ?: emptyMap(),
                    riskLevel = RiskLevel.valueOf(tMap["riskLevel"] as? String ?: "LOW"),
                    privacyLevel = PrivacyLevel.valueOf(tMap["privacyLevel"] as? String ?: "LOCAL_ONLY"),
                    requiresConfirmation = tMap["requiresConfirmation"] as? Boolean ?: false,
                    serverId = serverId
                )
            } ?: emptyList()
            toolRegistry.registerTools(serverId, toolsList)
        }

        // 2. Discover Resources
        val resReq = McpJsonRpcRequest(
            id = UUID.randomUUID().toString(),
            method = "resources/list"
        )
        val resRes = session.executeWithTimeout(resReq)
        if (resRes is EdgeResult.Success) {
            val map = resRes.data.result as? Map<String, Any?> ?: emptyMap()
            val rList = (map["resources"] as? List<Map<String, Any?>>)?.map { rMap ->
                McpResource(
                    uri = rMap["uri"] as? String ?: "",
                    name = rMap["name"] as? String ?: "",
                    description = rMap["description"] as? String,
                    mimeType = rMap["mimeType"] as? String ?: "application/json",
                    serverId = serverId
                )
            } ?: emptyList()
            resourceRegistry.registerResources(serverId, rList)
        }

        // 3. Discover Prompts
        val promptReq = McpJsonRpcRequest(
            id = UUID.randomUUID().toString(),
            method = "prompts/list"
        )
        val promptRes = session.executeWithTimeout(promptReq)
        if (promptRes is EdgeResult.Success) {
            val map = promptRes.data.result as? Map<String, Any?> ?: emptyMap()
            val pList = (map["prompts"] as? List<Map<String, Any?>>)?.map { pMap ->
                McpPrompt(
                    name = pMap["name"] as? String ?: "",
                    description = pMap["description"] as? String,
                    serverId = serverId
                )
            } ?: emptyList()
            promptRegistry.registerPrompts(serverId, pList)
        }

        return EdgeResult.Success(Unit)
    }

    suspend fun callTool(
        serverId: String,
        toolName: String,
        arguments: Map<String, Any?>,
        declaredPrivacy: PrivacyLevel = PrivacyLevel.LOCAL_ONLY
    ): EdgeResult<McpToolCallResult> {
        val session = sessions[serverId]
            ?: return EdgeResult.Failure(EdgeAIError.McpProtocolError(serverId, "Session '$serverId' not found"))

        // Security check
        val secCheck = securityManager.validateToolInvocation(serverId, toolName, declaredPrivacy)
        if (secCheck is EdgeResult.Failure) return secCheck

        val req = McpJsonRpcRequest(
            id = UUID.randomUUID().toString(),
            method = "tools/call",
            params = mapOf(
                "name" to toolName,
                "arguments" to arguments
            )
        )

        val res = session.executeWithTimeout(req)
        return when (res) {
            is EdgeResult.Success -> {
                val resultMap = res.data.result as? Map<String, Any?>
                if (resultMap == null) {
                    EdgeResult.Success(McpToolCallResult(content = listOf(McpContentItem(text = res.data.result.toString()))))
                } else {
                    val isError = resultMap["isError"] as? Boolean ?: false
                    val contentList = (resultMap["content"] as? List<Map<String, Any?>>)?.map { c ->
                        McpContentItem(
                            type = c["type"] as? String ?: "text",
                            text = c["text"] as? String,
                            data = c["data"] as? String
                        )
                    } ?: listOf(McpContentItem(text = resultMap.toString()))
                    EdgeResult.Success(McpToolCallResult(isError, contentList, resultMap))
                }
            }
            is EdgeResult.Failure -> res
        }
    }

    suspend fun readResource(serverId: String, uri: String): EdgeResult<String> {
        val session = sessions[serverId]
            ?: return EdgeResult.Failure(EdgeAIError.McpProtocolError(serverId, "Session '$serverId' not found"))

        val req = McpJsonRpcRequest(
            id = UUID.randomUUID().toString(),
            method = "resources/read",
            params = mapOf("uri" to uri)
        )

        val res = session.executeWithTimeout(req)
        return when (res) {
            is EdgeResult.Success -> {
                val map = res.data.result as? Map<String, Any?>
                val text = map?.get("contents")?.toString() ?: res.data.result.toString()
                EdgeResult.Success(text)
            }
            is EdgeResult.Failure -> res
        }
    }

    suspend fun getPrompt(serverId: String, promptName: String, arguments: Map<String, Any?> = emptyMap()): EdgeResult<String> {
        val session = sessions[serverId]
            ?: return EdgeResult.Failure(EdgeAIError.McpProtocolError(serverId, "Session '$serverId' not found"))

        val req = McpJsonRpcRequest(
            id = UUID.randomUUID().toString(),
            method = "prompts/get",
            params = mapOf("name" to promptName, "arguments" to arguments)
        )

        val res = session.executeWithTimeout(req)
        return when (res) {
            is EdgeResult.Success -> {
                val map = res.data.result as? Map<String, Any?>
                val desc = map?.get("description")?.toString() ?: "Prompt template for $promptName"
                EdgeResult.Success(desc)
            }
            is EdgeResult.Failure -> res
        }
    }
}
