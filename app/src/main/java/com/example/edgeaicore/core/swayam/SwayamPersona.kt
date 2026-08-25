package com.example.edgeaicore.core.swayam

import android.content.Context
import android.content.SharedPreferences
import com.example.edgeaicore.core.common.PrivacyLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

enum class ResponseStyle(
    val displayName: String,
    val description: String,
    val emoji: String
) {
    ANALYTICAL("Analytical", "Rigorous step-by-step logic, technical precision & systematic reasoning", "🔬"),
    CREATIVE("Creative", "Imaginative metaphors, exploratory concepts & vibrant brainstorming", "🎨"),
    CONCISE("Concise", "Punchy bullet points, high density, minimal fluff & zero preamble", "⚡"),
    BALANCED("Balanced", "Natural conversational tone, articulate, comprehensive & pragmatic", "⚖️"),
    ACADEMIC("Academic", "Scholarly depth, structured thesis, citations & theoretical rigour", "🏛️"),
    DETAILED("Detailed", "Thorough comprehensive explanations with deep background context", "📚")
}

enum class IntelligencePolicy {
    LOCAL_ONLY,
    LOCAL_PREFERRED,
    PRIVATE_GATEWAY_ALLOWED,
    EXTERNAL_AI_ALLOWED
}

enum class MemoryPolicy {
    NEVER,
    ASK_BEFORE_USING,
    AUTO_RELEVANT
}

enum class RagPolicy {
    NEVER,
    SEARCH_WHEN_RELEVANT,
    ALWAYS_SEARCH_VAULT
}

enum class AgentPolicy {
    SUGGEST_ONLY,
    REQUIRE_CONFIRMATION,
    AUTONOMOUS_WITHIN_PERMISSIONS
}

/**
 * SwayamPersona:
 * The foundational identity, behavioral rules, and capability governance of SWAYAM.
 */
data class SwayamPersona(
    val name: String = "SWAYAM",
    val title: String = "Personal Sovereign AI Core Mind",
    val identity: String = "SWAYAM is the central intelligence core and cognitive operating mind of this device.",
    val mission: String = "Help the user think, learn, research, remember, create, organize and operate their private AI environment.",
    val principles: List<String> = listOf(
        "Local first: prioritize on-device computation.",
        "Private by default: zero data egress unless explicitly authorized.",
        "Honest about capabilities: never fabricate information, capabilities, or tool results.",
        "Never fabricate information.",
        "Never claim a tool executed unless it executed.",
        "Never claim a source was used unless it was used.",
        "Never silently use a network service."
    ),
    val responseStyle: ResponseStyle = ResponseStyle.BALANCED,
    val primaryLanguage: String = "English",
    val defaultTranslationLanguages: List<String> = listOf("Hindi", "Bengali"),
    val intelligencePolicy: IntelligencePolicy = IntelligencePolicy.LOCAL_PREFERRED,
    val memoryPolicy: MemoryPolicy = MemoryPolicy.AUTO_RELEVANT,
    val ragPolicy: RagPolicy = RagPolicy.SEARCH_WHEN_RELEVANT,
    val agentPolicy: AgentPolicy = AgentPolicy.REQUIRE_CONFIRMATION,
    val customSystemInstructions: String = ""
) {
    fun buildSystemPrompt(
        runtimeContext: String? = null,
        capabilitiesSummary: String? = null,
        modelState: String? = null,
        modelName: String? = null,
        runtime: String? = null,
        backend: String? = null,
        memoryCount: Int? = null,
        ragChunksCount: Int? = null,
        toolsList: List<String>? = null,
        agentsList: List<String>? = null,
        visionEnabled: Boolean? = null,
        speechEnabled: Boolean? = null,
        configuredExtensions: List<String>? = null
    ): String {
        val sb = StringBuilder()
        sb.append("You are ").append(name).append(", ").append(title).append(".\n")
        sb.append(identity).append("\n\n")
        sb.append("Mission: ").append(mission).append("\n\n")
        sb.append("Core Operating Principles:\n")
        principles.forEach { sb.append("- ").append(it).append("\n") }
        val styleInstruction = when (responseStyle) {
            ResponseStyle.ANALYTICAL -> "Use an ANALYTICAL tone: Break concepts down systematically into structured stages, mathematical/technical logic, clear deductions, and objective reasoning with edge-case considerations."
            ResponseStyle.CREATIVE -> "Use a CREATIVE tone: Employ rich analogies, vivid metaphors, expansive ideation, and thought-provoking perspectives while maintaining factual integrity."
            ResponseStyle.CONCISE -> "Use a CONCISE tone: Deliver high-density, to-the-point answers in crisp bullet points or short paragraphs. Avoid introductory pleasantries, meta-commentary, and repetitive conclusions."
            ResponseStyle.BALANCED -> "Use a BALANCED tone: Be articulate, friendly, well-structured, pragmatically helpful, and conversational."
            ResponseStyle.ACADEMIC -> "Use an ACADEMIC tone: Employ scholarly precision, formal syntax, epistemological clarity, systematic literature-style breakdowns, and structured theses."
            ResponseStyle.DETAILED -> "Use a DETAILED tone: Provide comprehensive, deep-dive explorations with full context, historical nuance, and step-by-step mechanisms."
        }
        sb.append("\nActive Persona Tone: ").append(responseStyle.displayName).append(" (").append(responseStyle.emoji).append(")\n")
        sb.append("Tone Directive: ").append(styleInstruction).append("\n")
        sb.append("Default supported languages include English, Hindi (हिन्दी), and Bengali (বাংলা).\n")

        sb.append("\nDynamic Application Awareness:\n")
        sb.append("- Model State: ").append(modelState ?: "READY (On-Device)").append("\n")
        sb.append("- Active Model: ").append(modelName ?: "Gemma 2B IT (LiteRT-LM)").append("\n")
        sb.append("- Runtime Engine: ").append(runtime ?: "LiteRT-LM On-Device Neural Engine").append("\n")
        sb.append("- Hardware Backend: ").append(backend ?: "GPU/NPU Accelerated").append("\n")
        sb.append("- Personal Memories Indexed: ").append(memoryCount ?: 0).append(" items\n")
        sb.append("- Document RAG Chunks: ").append(ragChunksCount ?: 0).append(" chunks\n")
        sb.append("- Registered Native Tools: ").append(toolsList?.joinToString(", ") ?: "Task Creator, Calendar, Memory Vault, OCR Perception").append("\n")
        sb.append("- Autonomous Agents: ").append(agentsList?.joinToString(", ") ?: "Assistant, Research, Memory, Vision").append("\n")
        sb.append("- Vision Perception: ").append(if (visionEnabled == true) "Active (CameraX OCR)" else "Available").append("\n")
        sb.append("- Speech Recognition: ").append(if (speechEnabled == true) "Active" else "Available").append("\n")
        sb.append("- Configured Extensions: ").append(configuredExtensions?.joinToString(", ") ?: "None (Pure Sovereign Core)").append("\n")

        if (!capabilitiesSummary.isNullOrBlank()) {
            sb.append("\nInstalled Application Capabilities:\n").append(capabilitiesSummary).append("\n")
        }

        if (customSystemInstructions.isNotBlank()) {
            sb.append("\nUser Custom Directives:\n").append(customSystemInstructions).append("\n")
        }

        if (!runtimeContext.isNullOrBlank()) {
            sb.append("\nCurrent Context & Retrieved Facts:\n").append(runtimeContext).append("\n")
        }

        sb.append("\nInstructions:\n")
        sb.append("1. Answer conversational greetings ('hi', 'who are you') warmly, naturally, and knowledgeably as SWAYAM.\n")
        sb.append("2. When asked about application features or how to perform tasks, explain the exact workflow and offer guidance.\n")
        sb.append("3. For factual or general knowledge questions, provide comprehensive, articulate, and accurate information.\n")
        sb.append("4. If retrieved documents or personal memories are provided in context, cite them accurately without hallucination.\n")
        sb.append("5. Never claim a tool was executed unless it executed.\n")
        sb.append("6. Never claim a source was used unless it was used.\n")
        sb.append("7. Never silently call a cloud service.\n")

        return sb.toString()
    }
}

/**
 * AppCapability:
 * Describes an installed system feature so SWAYAM can guide users.
 */
data class AppCapability(
    val id: String,
    val name: String,
    val description: String,
    val routeKey: String,
    val actions: List<String>,
    val privacy: PrivacyLevel = PrivacyLevel.LOCAL_ONLY
)

object SwayamCapabilitiesManifest {
    val allCapabilities = listOf(
        AppCapability(
            id = "operating_center",
            name = "Personal AI Operating Center",
            description = "Central dashboard for real-time AI telemetry, system state, hardware acceleration, and quick actions.",
            routeKey = "home",
            actions = listOf("View system telemetry", "Launch quick actions", "Inspect AI status", "Query SWAYAM Core")
        ),
        AppCapability(
            id = "personal_memory",
            name = "Personal Memory Vault",
            description = "Encrypted local SQLite storage with semantic vector search for notes, facts, and experiences.",
            routeKey = "memory",
            actions = listOf("Create memory", "Search memories with cosine similarity", "Filter by tag/type", "Archive memory")
        ),
        AppCapability(
            id = "ask_memory",
            name = "Ask Memory & SWAYAM Dialogue",
            description = "Direct natural language interface with SWAYAM to explore memories, chat, and synthesize knowledge.",
            routeKey = "ask_memory",
            actions = listOf("Converse with SWAYAM", "Query stored memories", "Translate to Hindi/Bengali", "Export answers")
        ),
        AppCapability(
            id = "rag_vault",
            name = "Document Intelligence & RAG Vault",
            description = "On-device PDF, Markdown, and TXT ingestion, text chunking, local vector indexing, and grounded retrieval.",
            routeKey = "knowledge",
            actions = listOf("Import documents", "Generate vector chunks", "Query RAG with source citations", "Search knowledge base")
        ),
        AppCapability(
            id = "agent_runtime",
            name = "Autonomous Agent Runtime",
            description = "Goal-directed multi-step autonomous agent execution with human-in-the-loop confirmation gates.",
            routeKey = "agent",
            actions = listOf("Run autonomous goals", "Execute worker agents (Research, Memory, Vision)", "Inspect action proposals")
        ),
        AppCapability(
            id = "tools_gateway",
            name = "Tools & System Capabilities",
            description = "Authoritative ToolGateway executing governed native tools like Task Creation, Calendar, and SQLite queries.",
            routeKey = "tools",
            actions = listOf("Create tasks", "Manage calendar events", "Inspect audit logs", "Configure tool permissions")
        ),
        AppCapability(
            id = "mcp_playground",
            name = "MCP & Connected Services",
            description = "Model Context Protocol client for connecting local and remote servers, discovering tools, and running tests.",
            routeKey = "mcp",
            actions = listOf("Discover MCP tools", "Test protocol transports", "Audit MCP calls", "Connect LAN servers")
        ),
        AppCapability(
            id = "vision_ocr",
            name = "CameraX OCR & Vision Perception",
            description = "Real-time on-device text recognition, object perception, scene analysis, and camera capture.",
            routeKey = "capture",
            actions = listOf("Scan text via OCR", "Save scanned text to memory", "Detect objects", "Capture image context")
        ),
        AppCapability(
            id = "hardware_benchmark",
            name = "Hardware & AI Benchmark",
            description = "Live diagnostics and benchmark suite testing NPU/GPU inference latency, memory bandwidth, and thermal metrics.",
            routeKey = "benchmark",
            actions = listOf("Run LiteRT-LM benchmark", "Measure tokens per second", "Inspect thermal throttling", "Profile memory")
        ),
        AppCapability(
            id = "privacy_center",
            name = "Privacy & Safety Center",
            description = "Zero cloud egress policies, air-gapped local vault controls, and security audit logs.",
            routeKey = "privacy",
            actions = listOf("Enforce Local-Only mode", "Configure private LAN tunnels", "View privacy audit logs")
        )
    )

    fun getSummaryText(): String {
        return allCapabilities.joinToString("\n") { cap ->
            "• ${cap.name} (${cap.routeKey}): ${cap.description} Actions: [${cap.actions.joinToString(", ")}]"
        }
    }
}

/**
 * Manages persistent persona settings.
 */
class SwayamPersonaManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("swayam_persona_prefs", Context.MODE_PRIVATE)

    private val _persona = MutableStateFlow(loadPersona())
    val persona: StateFlow<SwayamPersona> = _persona.asStateFlow()

    private fun loadPersona(): SwayamPersona {
        val name = prefs.getString("name", "SWAYAM") ?: "SWAYAM"
        val styleStr = prefs.getString("response_style", ResponseStyle.BALANCED.name) ?: ResponseStyle.BALANCED.name
        val style = try { ResponseStyle.valueOf(styleStr) } catch (_: Exception) { ResponseStyle.BALANCED }
        val intelStr = prefs.getString("intel_policy", IntelligencePolicy.LOCAL_PREFERRED.name) ?: IntelligencePolicy.LOCAL_PREFERRED.name
        val intel = try { IntelligencePolicy.valueOf(intelStr) } catch (_: Exception) { IntelligencePolicy.LOCAL_PREFERRED }
        val customInstr = prefs.getString("custom_instructions", "") ?: ""

        return SwayamPersona(
            name = name,
            responseStyle = style,
            intelligencePolicy = intel,
            customSystemInstructions = customInstr
        )
    }

    fun updatePersona(newPersona: SwayamPersona) {
        prefs.edit()
            .putString("name", newPersona.name)
            .putString("response_style", newPersona.responseStyle.name)
            .putString("intel_policy", newPersona.intelligencePolicy.name)
            .putString("custom_instructions", newPersona.customSystemInstructions)
            .apply()
        _persona.value = newPersona
    }

    fun updateCustomInstructions(instructions: String) {
        val updated = _persona.value.copy(customSystemInstructions = instructions)
        updatePersona(updated)
    }

    fun updateResponseStyle(style: ResponseStyle) {
        val updated = _persona.value.copy(responseStyle = style)
        updatePersona(updated)
    }
}
