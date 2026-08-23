package com.example.edgeaicore.core.agent

import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.RiskLevel
import com.example.edgeaicore.core.tools.ToolCategory

/**
 * High-level system capabilities supported by EdgeAI Core.
 */
enum class AgentCapability {
    VISION,
    MEMORY,
    LOCATION,
    CALENDAR,
    TASKS,
    WEATHER,
    NAVIGATION,
    AUDIO,
    FILES,
    AI,
    AUTOMATION
}

/**
 * Configurable Agent Profile.
 * Defines operational boundaries, allowed capabilities, risk policies, and system instructions.
 */
data class AgentProfile(
    val id: String,
    val name: String,
    val description: String,
    val systemInstruction: String,
    val allowedCapabilities: Set<AgentCapability>,
    val allowedToolIds: Set<String> = emptySet(),
    val maxPrivacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY,
    val maxAllowedRisk: RiskLevel = RiskLevel.MEDIUM,
    val maxSteps: Int = 4,
    val toolBudget: Int = 3,
    val tokenBudget: Int = 512
) {
    companion object {
        val ASSISTANT = AgentProfile(
            id = "profile_assistant",
            name = "General Assistant",
            description = "All-around proactive on-device assistant",
            systemInstruction = "You are a helpful, privacy-first on-device AI assistant. Answer concisely and use tools when needed.",
            allowedCapabilities = setOf(AgentCapability.MEMORY, AgentCapability.TASKS, AgentCapability.CALENDAR, AgentCapability.WEATHER, AgentCapability.AUTOMATION),
            maxPrivacyLevel = PrivacyLevel.LOCAL_ONLY,
            maxSteps = 4
        )

        val MEMORY = AgentProfile(
            id = "profile_memory",
            name = "Memory Lens",
            description = "Specialized episodic memory retrieval and contextual capture",
            systemInstruction = "You are an intelligent memory recall agent. Retrieve and synthesize relevant user experiences.",
            allowedCapabilities = setOf(AgentCapability.MEMORY, AgentCapability.FILES, AgentCapability.AI),
            maxPrivacyLevel = PrivacyLevel.LOCAL_ONLY,
            maxSteps = 3
        )

        val VISION = AgentProfile(
            id = "profile_vision",
            name = "Vision Sense",
            description = "Real-time perception and visual environment reasoning",
            systemInstruction = "You are an on-device perception reasoning agent. Analyze visual detections and environment state.",
            allowedCapabilities = setOf(AgentCapability.VISION, AgentCapability.LOCATION, AgentCapability.AI),
            maxPrivacyLevel = PrivacyLevel.LOCAL_ONLY,
            maxSteps = 3
        )

        val COACH = AgentProfile(
            id = "profile_coach",
            name = "Health & Habit Coach",
            description = "Proactive habits, fitness suggestions, and wellness tracking",
            systemInstruction = "You are an encouraging habit and wellness coach. Help the user achieve their daily routines.",
            allowedCapabilities = setOf(AgentCapability.TASKS, AgentCapability.CALENDAR, AgentCapability.MEMORY, AgentCapability.AUTOMATION),
            maxPrivacyLevel = PrivacyLevel.LOCAL_ONLY,
            maxSteps = 4
        )

        val STUDY = AgentProfile(
            id = "profile_study",
            name = "Study Companion",
            description = "Synthesizes concepts, flashcards, and notes into structured learning",
            systemInstruction = "You are an expert tutor. Break down complex topics into clear, digestible explanations.",
            allowedCapabilities = setOf(AgentCapability.MEMORY, AgentCapability.AI, AgentCapability.FILES),
            maxPrivacyLevel = PrivacyLevel.LOCAL_ONLY,
            maxSteps = 3
        )

        val CREATOR = AgentProfile(
            id = "profile_creator",
            name = "Creator Studio",
            description = "Creative drafting, content structuring, and creative ideation",
            systemInstruction = "You are a creative writing and structuring partner. Generate crisp, punchy ideas.",
            allowedCapabilities = setOf(AgentCapability.AI, AgentCapability.MEMORY, AgentCapability.AUDIO),
            maxPrivacyLevel = PrivacyLevel.LOCAL_ONLY,
            maxSteps = 4
        )

        val PRODUCTIVITY = AgentProfile(
            id = "profile_productivity",
            name = "Productivity Agent",
            description = "Automates task queues, calendar scheduling, and workflow actions",
            systemInstruction = "You are an efficient executive assistant. Keep schedules organized and tasks on track.",
            allowedCapabilities = setOf(AgentCapability.TASKS, AgentCapability.CALENDAR, AgentCapability.AUTOMATION, AgentCapability.MEMORY),
            maxPrivacyLevel = PrivacyLevel.LOCAL_ONLY,
            maxSteps = 5
        )

        val TRAVEL = AgentProfile(
            id = "profile_travel",
            name = "Travel Navigator",
            description = "Location-aware navigation, weather briefings, and itinerary management",
            systemInstruction = "You are a local travel guide and itinerary manager. Provide safe navigation tips.",
            allowedCapabilities = setOf(AgentCapability.NAVIGATION, AgentCapability.WEATHER, AgentCapability.LOCATION, AgentCapability.MEMORY),
            maxPrivacyLevel = PrivacyLevel.LOCAL_ONLY,
            maxSteps = 4
        )

        val LIFE = AgentProfile(
            id = "profile_life",
            name = "Life OS",
            description = "Unified personal operating system for daily life orchestration",
            systemInstruction = "You are a unified personal agent orchestration engine. Coordinate daily activities safely.",
            allowedCapabilities = AgentCapability.values().toSet(),
            maxPrivacyLevel = PrivacyLevel.LOCAL_ONLY,
            maxSteps = 5
        )
    }
}

/**
 * Registry for Agent Profiles allowing future applications to register custom profiles dynamically.
 */
class AgentProfileRegistry {
    private val profiles = mutableMapOf<String, AgentProfile>()

    init {
        register(AgentProfile.ASSISTANT)
        register(AgentProfile.MEMORY)
        register(AgentProfile.VISION)
        register(AgentProfile.COACH)
        register(AgentProfile.STUDY)
        register(AgentProfile.CREATOR)
        register(AgentProfile.PRODUCTIVITY)
        register(AgentProfile.TRAVEL)
        register(AgentProfile.LIFE)
    }

    fun register(profile: AgentProfile) {
        profiles[profile.id] = profile
    }

    fun get(profileId: String): AgentProfile? = profiles[profileId]

    fun getAll(): List<AgentProfile> = profiles.values.toList()
}
