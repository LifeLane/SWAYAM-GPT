package com.example.edgeaicore.core.demo

import com.example.edgeaicore.core.agent.AgentExecutionResult
import com.example.edgeaicore.core.agent.AgentProfile
import com.example.edgeaicore.core.agent.AgentStep
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.RiskLevel
import com.example.edgeaicore.core.gateway.ToolExecutionResult
import com.example.edgeaicore.core.litertlm.GenerationRequest
import com.example.edgeaicore.core.litertlm.GenerationResponse
import com.example.edgeaicore.core.mcp.McpJsonRpcRequest
import com.example.edgeaicore.core.mcp.McpJsonRpcResponse
import com.example.edgeaicore.core.mcp.McpServerInfo
import com.example.edgeaicore.core.mcp.McpTool
import com.example.edgeaicore.core.mcp.McpTrustLevel

/**
 * Development & Testing Mock Components.
 * Every response is prominently labeled with "[DEMO]" prefix to guarantee
 * that synthetic testing data is never confused with production on-device execution.
 */

class MockMcpServer(
    val serverId: String = "demo-remote-mcp"
) {
    val serverInfo = McpServerInfo(
        name = "Mock-Remote-MCP-Server",
        version = "1.0.0-DEMO",
        description = "[DEMO] Sandbox MCP server for testing integration flows",
        trustLevel = McpTrustLevel.USER_APPROVED_REMOTE
    )

    fun handleRequest(request: McpJsonRpcRequest): McpJsonRpcResponse {
        return when (request.method) {
            "initialize" -> McpJsonRpcResponse(
                id = request.id,
                result = mapOf("protocolVersion" to "2024-11-05", "serverInfo" to mapOf("name" to "[DEMO] Sandbox MCP"))
            )
            "tools/list" -> McpJsonRpcResponse(
                id = request.id,
                result = mapOf(
                    "tools" to listOf(
                        mapOf(
                            "name" to "sandbox.echo",
                            "description" to "[DEMO] Echo test tool",
                            "inputSchema" to mapOf("message" to "string"),
                            "riskLevel" to "LOW",
                            "privacyLevel" to "PUBLIC",
                            "requiresConfirmation" to false
                        )
                    )
                )
            )
            "tools/call" -> {
                val args = request.params["arguments"] as? Map<String, Any?>
                val msg = args?.get("message") ?: "sample"
                McpJsonRpcResponse(
                    id = request.id,
                    result = mapOf("content" to listOf(mapOf("type" to "text", "text" to "[DEMO] Sandbox echo: $msg")))
                )
            }
            else -> McpJsonRpcResponse(id = request.id, result = mapOf("status" to "[DEMO] OK"))
        }
    }
}

class MockToolGateway {
    suspend fun executeDemoTool(toolId: String, args: Map<String, Any?>): EdgeResult<ToolExecutionResult> {
        return EdgeResult.Success(
            ToolExecutionResult(
                toolId = toolId,
                success = true,
                output = mapOf("status" to "[DEMO] Simulated tool result", "args" to args),
                latencyMs = 12L
            )
        )
    }
}

class MockPrivateServer {
    suspend fun generateDemo(request: GenerationRequest): EdgeResult<GenerationResponse> {
        return EdgeResult.Success(
            GenerationResponse(
                text = "[DEMO] Synthetic inference response for: '${request.prompt}'",
                model = "Mock-Llama-3-DEMO",
                latencyMs = 45L,
                tokensGenerated = 18,
                tokensPerSecond = 400.0,
                provider = AIProviderType.DEMO,
                source = "[DEMO] Mock Private AI Server"
            )
        )
    }
}

class MockAgent {
    suspend fun runDemo(request: String, profile: AgentProfile = AgentProfile.ASSISTANT): EdgeResult<AgentExecutionResult> {
        return EdgeResult.Success(
            AgentExecutionResult(
                prompt = request,
                profile = profile,
                finalResponse = "[DEMO] Agent completed simulated loop for: '$request'",
                steps = listOf(
                    AgentStep(
                        stepIndex = 1,
                        thought = "[DEMO] Evaluating request parameters",
                        selectedTool = "device.status",
                        observation = "[DEMO] Checked capabilities successfully."
                    )
                ),
                toolsExecuted = listOf("device.status"),
                latencyMs = 28L,
                isSuccess = true,
                tokensUsed = 32
            )
        )
    }
}
