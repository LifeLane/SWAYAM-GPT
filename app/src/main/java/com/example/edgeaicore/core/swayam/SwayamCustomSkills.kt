package com.example.edgeaicore.core.swayam

import android.content.Context
import com.example.edgeaicore.core.ai.AIRouter
import com.example.edgeaicore.core.ai.AIRequest
import com.example.edgeaicore.core.cloud.GeminiApiClient
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.TaskType
import com.example.edgeaicore.core.litertlm.GenerationRequest
import com.example.edgeaicore.core.memory.MemoryEngine
import com.example.edgeaicore.core.gateway.ToolGateway
import com.example.edgeaicore.core.knowledge.KnowledgeSearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SwayamSkill:
 * A specialized skill that SWAYAM can dynamically invoke to perform specific actions,
 * transform data, orchestrate tools, or chain personas.
 */
data class SwayamSkill(
    val id: String,
    val name: String,
    val description: String,
    val requiredPersonaId: String? = null,
    val execute: suspend (params: Map<String, String>, context: SkillExecutionContext) -> SkillExecutionResult
)

data class SkillExecutionContext(
    val query: String,
    val activePersona: SwayamSystemPersona,
    val memoryEngine: MemoryEngine,
    val knowledgeEngine: KnowledgeSearchEngine,
    val toolGateway: ToolGateway,
    val aiRouter: AIRouter,
    val geminiApiClient: GeminiApiClient
)

data class SkillExecutionResult(
    val success: Boolean,
    val output: String,
    val modifiedContext: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * SwayamChainEngine:
 * Central execution engine for real-time Persona Swapping, Skill Execution,
 * and Multi-Persona Chained Reasoning (chain ⛓️💥).
 */
class SwayamChainEngine(
    private val context: Context,
    private val aiRouter: AIRouter,
    private val memoryEngine: MemoryEngine,
    private val knowledgeEngine: KnowledgeSearchEngine,
    private val toolGateway: ToolGateway,
    private val geminiApiClient: GeminiApiClient
) {
    private val registeredSkills = mutableMapOf<String, SwayamSkill>()

    init {
        registerDefaultSkills()
    }

    private fun registerDefaultSkills() {
        // Skill 1: Dynamic Persona Swap
        registerSkill(
            SwayamSkill(
                id = "swap_persona",
                name = "Real-time Persona Swap",
                description = "Swaps SWAYAM's cognitive mind to a specialized sub-system or agent persona instantly."
            ) { params, execCtx ->
                val targetId = params["persona_id"] ?: "master_sovereign_core"
                val persona = SwayamPersonaRegistry.getById(targetId)
                SkillExecutionResult(
                    success = true,
                    output = "Cognitive state transitioned to ${persona.emoji} ${persona.name} (${persona.roleTitle}).",
                    metadata = mapOf("swapped_to" to persona.id)
                )
            }
        )

        // Skill 2: Deep Research & Document Cross-Examination
        registerSkill(
            SwayamSkill(
                id = "deep_research_cross_examination",
                name = "Deep Research & Document Cross-Examination",
                description = "Chains Research Scout and Knowledge Synthesizer to cross-examine memory and indexed literature."
            ) { params, execCtx ->
                val topic = params["topic"] ?: execCtx.query
                val docsResult = execCtx.knowledgeEngine.search(topic, limit = 3)
                val docs = (docsResult as? EdgeResult.Success)?.data ?: emptyList()
                val memories = execCtx.memoryEngine.retriever.retrieveMemories(topic, maxResults = 3)

                val docContext = docs.joinToString("\n") { "[Doc: ${it.title}] ${it.contentSnippet}" }
                val memContext = memories.joinToString("\n") { "[Memory: ${it.memory.title}] ${it.memory.content}" }

                val synthesis = buildString {
                    append("### 📚 Research & Cross-Examination Synthesis\n\n")
                    if (docs.isNotEmpty()) {
                        append("**Indexed Literature Findings:**\n")
                        docs.forEach { append("- **${it.title}** (Score: ${(it.score * 100).toInt()}%): ${it.contentSnippet.take(160)}...\n") }
                        append("\n")
                    }
                    if (memories.isNotEmpty()) {
                        append("**Vault Memories Correlated:**\n")
                        memories.forEach { append("- **${it.memory.title}**: ${it.memory.content.take(140)}...\n") }
                        append("\n")
                    }
                }

                SkillExecutionResult(
                    success = true,
                    output = synthesis,
                    modifiedContext = "$docContext\n$memContext"
                )
            }
        )

        // Skill 3: Code Architecture & Algorithmic Synthesis
        registerSkill(
            SwayamSkill(
                id = "code_architect_synthesis",
                name = "Code Architecture Synthesis",
                description = "Invokes the Senior Code Architect persona with strict typing and algorithmic optimization."
            ) { params, execCtx ->
                val task = params["task"] ?: execCtx.query
                val prompt = "Provide clean, robust Kotlin/Compose or algorithm architecture for:\n$task"
                val genResult = generateWithPersona(SwayamPersonaRegistry.CODE_ARCHITECT, prompt)
                SkillExecutionResult(
                    success = genResult.isNotBlank(),
                    output = genResult
                )
            }
        )

        // Skill 4: Governed Tool Execution
        registerSkill(
            SwayamSkill(
                id = "tool_orchestration",
                name = "Governed Tool Execution",
                description = "Invokes the Tools Agent to validate and dispatch system commands safely."
            ) { params, execCtx ->
                val toolName = params["tool"] ?: "get_device_telemetry"
                val toolParams = params.filterKeys { it != "tool" }
                val result = execCtx.toolGateway.executeTool(toolName, toolParams, userConsentGiven = true)
                val isSuccess = result is EdgeResult.Success && result.data.success
                val output = if (result is EdgeResult.Success) {
                    val outMap = result.data.output
                    if (outMap.isNotEmpty()) {
                        outMap.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                    } else {
                        result.data.error ?: "Tool executed successfully."
                    }
                } else {
                    (result as? EdgeResult.Failure)?.error?.message ?: "Tool execution failed."
                }
                SkillExecutionResult(
                    success = isSuccess,
                    output = output
                )
            }
        )
    }

    fun registerSkill(skill: SwayamSkill) {
        registeredSkills[skill.id] = skill
    }

    fun getSkills(): List<SwayamSkill> = registeredSkills.values.toList()

    /**
     * Executes a chained multi-persona pipeline (Chain ⛓️💥):
     * Dynamically passes intermediate findings across specialized personas.
     */
    suspend fun executeChain(
        query: String,
        chainPersonas: List<SwayamSystemPersona>,
        runtimeContext: String = ""
    ): PersonaChainResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val links = mutableListOf<PersonaChainLink>()
        var accumulatedContext = runtimeContext

        for ((index, persona) in chainPersonas.withIndex()) {
            val linkStart = System.currentTimeMillis()
            val stageName = when (index) {
                0 -> "1. Analysis & Intent"
                1 -> "2. Deep Synthesis"
                2 -> "3. Specialized Output"
                else -> "${index + 1}. Refinement"
            }

            val prompt = buildString {
                append("User Query: ").append(query).append("\n\n")
                if (accumulatedContext.isNotBlank()) {
                    append("Prior Stage Findings & Context:\n").append(accumulatedContext).append("\n\n")
                }
                append("Perform your specialized role as ").append(persona.name).append(" (").append(persona.roleTitle).append(").")
            }

            val stageOutput = generateWithPersona(persona, prompt)
            accumulatedContext += "\n\n[Stage ${index + 1} - ${persona.name}]:\n$stageOutput"

            links.add(
                PersonaChainLink(
                    persona = persona,
                    stageName = stageName,
                    intent = persona.description,
                    output = stageOutput,
                    executionTimeMs = System.currentTimeMillis() - linkStart
                )
            )
        }

        val chainSignature = chainPersonas.joinToString(" ➔ ") { "${it.emoji} ${it.name}" }

        PersonaChainResult(
            query = query,
            links = links,
            finalResponse = links.lastOrNull()?.output ?: "",
            totalTimeMs = System.currentTimeMillis() - startTime,
            chainSignature = chainSignature
        )
    }

    /**
     * Executes generation with a specific persona's system prompt & parameters.
     */
    suspend fun generateWithPersona(
        persona: SwayamSystemPersona,
        prompt: String
    ): String = withContext(Dispatchers.IO) {
        val systemPrompt = persona.systemPrompt

        // Try Cloud Gemini first if available
        if (geminiApiClient.isConfigured()) {
            try {
                val req = GenerationRequest(
                    prompt = prompt,
                    systemInstruction = systemPrompt,
                    temperature = persona.defaultTemperature
                )
                val res = geminiApiClient.generateText(req)
                if (res is EdgeResult.Success && res.data.text.isNotBlank()) {
                    return@withContext res.data.text.trim()
                }
            } catch (_: Exception) {}
        }

        // On-device AI Router
        try {
            val aiReq = AIRequest(
                prompt = prompt,
                taskType = TaskType.TEXT_GENERATION,
                systemInstruction = systemPrompt,
                privacyLevel = persona.privacyLevel,
                preferredProvider = AIProviderType.LOCAL,
                temperature = persona.defaultTemperature
            )
            val res = aiRouter.generate(aiReq)
            if (res is EdgeResult.Success && res.data.text.isNotBlank()) {
                return@withContext res.data.text.trim()
            }
        } catch (_: Exception) {}

        // Fallback cognitive synthesis
        "[${persona.emoji} ${persona.name}] Evaluated request: $prompt"
    }

    /**
     * Determines the optimal Persona Chain for a given query.
     */
    fun selectOptimalChain(query: String): List<SwayamSystemPersona> {
        val q = query.lowercase()

        return when {
            // Coding, algorithms, architecture
            q.contains("code") || q.contains("kotlin") || q.contains("compose") ||
            q.contains("algorithm") || q.contains("function") || q.contains("json") ||
            q.contains("bug") || q.contains("architecture") -> {
                listOf(
                    SwayamPersonaRegistry.MASTER_SOVEREIGN_CORE,
                    SwayamPersonaRegistry.CODE_ARCHITECT,
                    SwayamPersonaRegistry.TOOLS_ORCHESTRATOR
                )
            }
            // Documents, research, RAG
            q.contains("document") || q.contains("paper") || q.contains("pdf") ||
            q.contains("research") || q.contains("rag") || q.contains("analyze") -> {
                listOf(
                    SwayamPersonaRegistry.RESEARCH_SCOUT,
                    SwayamPersonaRegistry.KNOWLEDGE_SYNTHESIZER,
                    SwayamPersonaRegistry.MASTER_SOVEREIGN_CORE
                )
            }
            // Memory, reflections, timeline
            q.contains("remember") || q.contains("memory") || q.contains("history") ||
            q.contains("past") || q.contains("note") || q.contains("what did i") -> {
                listOf(
                    SwayamPersonaRegistry.MEMORY_HISTORIAN,
                    SwayamPersonaRegistry.EXECUTIVE_STRATEGIST,
                    SwayamPersonaRegistry.MASTER_SOVEREIGN_CORE
                )
            }
            // Strategy, planning, goals
            q.contains("plan") || q.contains("strategy") || q.contains("goal") ||
            q.contains("roadmap") || q.contains("prioritize") || q.contains("decide") -> {
                listOf(
                    SwayamPersonaRegistry.MASTER_SOVEREIGN_CORE,
                    SwayamPersonaRegistry.EXECUTIVE_STRATEGIST,
                    SwayamPersonaRegistry.AUTONOMOUS_AGENT_OPERATOR
                )
            }
            // Creative brainstorming
            q.contains("story") || q.contains("creative") || q.contains("imagine") ||
            q.contains("metaphor") || q.contains("brainstorm") || q.contains("vision") -> {
                listOf(
                    SwayamPersonaRegistry.CREATIVE_INNOVATOR,
                    SwayamPersonaRegistry.MASTER_SOVEREIGN_CORE
                )
            }
            // Default unified chain
            else -> {
                listOf(
                    SwayamPersonaRegistry.MASTER_SOVEREIGN_CORE,
                    SwayamPersonaRegistry.EXECUTIVE_STRATEGIST
                )
            }
        }
    }
}
