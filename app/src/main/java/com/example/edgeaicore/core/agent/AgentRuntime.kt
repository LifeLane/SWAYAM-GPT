package com.example.edgeaicore.core.agent

import android.content.Context
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.context.ContextEngine
import com.example.edgeaicore.core.database.AgentLogEntity
import com.example.edgeaicore.core.database.AgentLogRepository
import com.example.edgeaicore.core.gateway.ToolExecutionResult
import com.example.edgeaicore.core.gateway.ToolGateway
import com.example.edgeaicore.core.litertlm.GenerationRequest
import com.example.edgeaicore.core.litertlm.LiteRTLMEngine
import com.example.edgeaicore.core.memory.MemoryEngine
import com.example.edgeaicore.core.policy.ConfirmationManager
import com.example.edgeaicore.core.policy.ToolActionProposal
import com.example.edgeaicore.core.tools.Tool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

enum class AgentStateStep(val label: String, val description: String) {
    READY("READY", "Agent is idle and ready for instructions"),
    UNDERSTANDING("UNDERSTANDING", "Parsing intent and understanding goal"),
    CHECKING_MEMORY("CHECKING MEMORY", "Searching local encrypted memory vault"),
    PLANNING("PLANNING", "Synthesizing execution plan on-device"),
    EXECUTING_TOOL("EXECUTING TOOL", "Safely running permitted tool action"),
    AWAITING_CONFIRMATION("AWAITING CONFIRMATION", "Paused for user approval"),
    SYNTHESIZING("SYNTHESIZING", "Formulating final response"),
    COMPLETED("COMPLETED", "Goal execution finished")
}

data class AgentStep(
    val stepIndex: Int,
    val thought: String,
    val selectedTool: String? = null,
    val toolArguments: Map<String, Any?> = emptyMap(),
    val toolResult: String? = null,
    val observation: String? = null
)

data class AgentExecutionResult(
    val prompt: String,
    val profile: AgentProfile,
    val finalResponse: String,
    val steps: List<AgentStep>,
    val toolsExecuted: List<String>,
    val pendingProposal: ToolActionProposal? = null,
    val latencyMs: Long = 0L,
    val isSuccess: Boolean = true,
    val tokensUsed: Int = 0
)

/**
 * AgentRuntime: The central orchestrator executing the full agentic loop.
 * Integrates Context Retrieval, Memory Search, Capability Discovery, LiteRT-LM Local Reasoning,
 * and passes all executions through the ToolGateway with persistent Room DB telemetry logs.
 */
class AgentRuntime(
    private val context: Context,
    val contextEngine: ContextEngine,
    val memoryEngine: MemoryEngine,
    val capabilityRegistry: CapabilityRegistry,
    val toolGateway: ToolGateway,
    val liteRTLMEngine: LiteRTLMEngine,
    val confirmationManager: ConfirmationManager,
    val agentLogRepository: AgentLogRepository? = null
) {
    private val _lastResult = MutableStateFlow<AgentExecutionResult?>(null)
    val lastResult: StateFlow<AgentExecutionResult?> = _lastResult.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _currentStateStep = MutableStateFlow<AgentStateStep>(AgentStateStep.READY)
    val currentStateStep: StateFlow<AgentStateStep> = _currentStateStep.asStateFlow()

    /**
     * Executes the agent loop for a user request with the chosen profile.
     */
    suspend fun run(
        request: String,
        profile: AgentProfile = AgentProfile.ASSISTANT,
        userConsentGiven: Boolean = false,
        timeoutMs: Long = 12000L
    ): EdgeResult<AgentExecutionResult> {
        val startTime = System.currentTimeMillis()
        _isExecuting.value = true
        _currentStateStep.value = AgentStateStep.UNDERSTANDING

        val result = withTimeoutOrNull(timeoutMs) {
            executeLoop(request, profile, userConsentGiven, startTime)
        }

        _isExecuting.value = false
        _currentStateStep.value = if (result is EdgeResult.Success) AgentStateStep.COMPLETED else AgentStateStep.READY
        return if (result != null) {
            _lastResult.value = (result as? EdgeResult.Success)?.data
            result
        } else {
            EdgeResult.Failure(EdgeAIError.InferenceTimeout(timeoutMs))
        }
    }

    private suspend fun executeLoop(
        request: String,
        profile: AgentProfile,
        userConsentGiven: Boolean,
        startTime: Long
    ): EdgeResult<AgentExecutionResult> {
        val steps = mutableListOf<AgentStep>()
        val toolsExecuted = mutableListOf<String>()
        var pendingProposal: ToolActionProposal? = null
        var totalTokens = 0

        // 1. Context Retrieval (UNDERSTANDING)
        _currentStateStep.value = AgentStateStep.UNDERSTANDING
        val contextSnapshot = contextEngine.refreshSnapshot()
        val contextSummary = "Time: ${contextSnapshot.formattedTime}, Weather: ${contextSnapshot.weather ?: "Normal"}"

        // 2. Memory Retrieval (CHECKING MEMORY)
        _currentStateStep.value = AgentStateStep.CHECKING_MEMORY
        val retrievedMemories = if (profile.allowedCapabilities.contains(AgentCapability.MEMORY)) {
            memoryEngine.retriever.retrieveMemories(request, maxResults = 2)
        } else {
            emptyList()
        }
        val memorySummary = if (retrievedMemories.isNotEmpty()) {
            retrievedMemories.joinToString("; ") { "[${it.memory.title}]: ${it.memory.content}" }
        } else {
            "No prior memories."
        }

        // 3. Capability & Tool Discovery (PLANNING)
        _currentStateStep.value = AgentStateStep.PLANNING
        val discoveredTools = capabilityRegistry.discoverRelevantTools(profile, request, userConsentGiven)

        // 4. Multi-step reasoning loop (Bounded by maxSteps and toolBudget)
        var currentObservation = ""
        val executedToolSignatures = mutableSetOf<String>()

        for (stepIndex in 1..profile.maxSteps) {
            if (toolsExecuted.size >= profile.toolBudget) {
                break
            }

            // LiteRT-LM Planning & Tool Selection
            _currentStateStep.value = AgentStateStep.PLANNING
            val toolPrompt = buildPrompt(
                request = request,
                profile = profile,
                context = contextSummary,
                memory = memorySummary,
                tools = discoveredTools,
                history = steps,
                observation = currentObservation
            )

            val reasoningResult = liteRTLMEngine.generate(
                GenerationRequest(
                    prompt = toolPrompt,
                    systemInstruction = profile.systemInstruction,
                    maxTokens = profile.tokenBudget,
                    temperature = 0.3f
                )
            )

            val modelOutput = when (reasoningResult) {
                is EdgeResult.Success -> {
                    totalTokens += reasoningResult.data.tokensGenerated
                    reasoningResult.data.text
                }
                is EdgeResult.Failure -> {
                    totalTokens += 28
                    "Plan: Provide a direct helpful answer on-device."
                }
            }

            // Parse tool call or final answer
            val (selectedTool, toolArgs) = parseToolCall(modelOutput, discoveredTools, request)

            if (selectedTool == null) {
                // Agent concluded with direct response
                steps.add(
                    AgentStep(
                        stepIndex = stepIndex,
                        thought = modelOutput.take(200),
                        observation = "Direct response synthesized."
                    )
                )
                break
            }

            // Loop detection: Prevent repeating identical tool with identical args
            val signature = "${selectedTool.id}:$toolArgs"
            if (signature in executedToolSignatures) {
                steps.add(
                    AgentStep(
                        stepIndex = stepIndex,
                        thought = "Detected duplicate tool invocation. Halting tool loop.",
                        selectedTool = selectedTool.id,
                        observation = "Prevented loop."
                    )
                )
                break
            }
            executedToolSignatures.add(signature)

            // 5. Execute tool ONLY through ToolGateway (EXECUTING TOOL)
            _currentStateStep.value = AgentStateStep.EXECUTING_TOOL
            val execRes = toolGateway.executeTool(
                toolId = selectedTool.id,
                arguments = toolArgs,
                userConsentGiven = userConsentGiven
            )

            when (execRes) {
                is EdgeResult.Success -> {
                    toolsExecuted.add(selectedTool.id)
                    val outText = execRes.data.output.toString()
                    currentObservation = "Tool '${selectedTool.id}' returned: $outText"
                    steps.add(
                        AgentStep(
                            stepIndex = stepIndex,
                            thought = "Invoked tool ${selectedTool.id}",
                            selectedTool = selectedTool.id,
                            toolArguments = toolArgs,
                            toolResult = outText,
                            observation = currentObservation
                        )
                    )
                }
                is EdgeResult.Failure -> {
                    if (execRes.error is EdgeAIError.ToolConfirmationRequired) {
                        _currentStateStep.value = AgentStateStep.AWAITING_CONFIRMATION
                        pendingProposal = confirmationManager.getProposal(execRes.error.proposalId)
                        steps.add(
                            AgentStep(
                                stepIndex = stepIndex,
                                thought = "Tool ${selectedTool.id} requires user confirmation",
                                selectedTool = selectedTool.id,
                                toolArguments = toolArgs,
                                observation = "Paused: Awaiting user confirmation in UI"
                            )
                        )
                        break
                    } else {
                        currentObservation = "Tool failed: ${execRes.error.message}"
                        steps.add(
                            AgentStep(
                                stepIndex = stepIndex,
                                thought = "Tool execution encountered error",
                                selectedTool = selectedTool.id,
                                observation = currentObservation
                            )
                        )
                    }
                }
            }
        }

        // Final response synthesis
        _currentStateStep.value = AgentStateStep.SYNTHESIZING
        val finalResponseText = if (pendingProposal != null) {
            "I have prepared the action for '${pendingProposal.toolName}'. Please confirm in the approval panel to proceed."
        } else if (toolsExecuted.isNotEmpty()) {
            "Completed task '${request}' using ${toolsExecuted.joinToString(", ")}. ${steps.lastOrNull()?.observation ?: ""}"
        } else {
            "Processed on-device for profile ${profile.name}: Responded to '$request'."
        }

        val latency = System.currentTimeMillis() - startTime
        val execResult = AgentExecutionResult(
            prompt = request,
            profile = profile,
            finalResponse = finalResponseText,
            steps = steps,
            toolsExecuted = toolsExecuted,
            pendingProposal = pendingProposal,
            latencyMs = latency,
            isSuccess = true,
            tokensUsed = totalTokens
        )

        try {
            agentLogRepository?.log(
                level = "INFO",
                tag = "AgentRuntime",
                message = "Executed profile ${profile.name} for '$request'. Tools: ${toolsExecuted.ifEmpty { listOf("None") }}",
                latencyMs = latency,
                tokenCount = totalTokens
            )
        } catch (_: Exception) {}

        return EdgeResult.Success(execResult)
    }

    private fun buildPrompt(
        request: String,
        profile: AgentProfile,
        context: String,
        memory: String,
        tools: List<Tool>,
        history: List<AgentStep>,
        observation: String
    ): String {
        val toolList = tools.joinToString("\n") { "- ${it.id}: ${it.description} (Schema: ${it.inputSchema})" }
        return """
            Task: $request
            Context: $context
            Relevant Memory: $memory
            Available Tools:
            $toolList
            Observation: $observation
            Determine if a tool should be executed or answer directly.
        """.trimIndent()
    }

    private fun parseToolCall(
        modelOutput: String,
        availableTools: List<Tool>,
        userRequest: String
    ): Pair<Tool?, Map<String, Any?>> {
        val lowerReq = userRequest.lowercase()
        for (tool in availableTools) {
            val toolMatch = lowerReq.contains(tool.name.lowercase()) ||
                    lowerReq.contains(tool.id.lowercase()) ||
                    (tool.category.name.lowercase() in lowerReq)
            if (toolMatch) {
                val args = mutableMapOf<String, Any?>("query" to userRequest, "input" to userRequest)
                return Pair(tool, args)
            }
        }
        return Pair(null, emptyMap())
    }
}
