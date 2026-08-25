package com.example.edgeaicore.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore by preferencesDataStore(name = "edge_ai_user_preferences")

data class EdgeUserSettings(
    val communicationStyle: String = "Concise & Technical",
    val latencyProfile: String = "Balanced", // HighPerformance, Balanced, BatterySaver
    val preferredAiBackend: String = "Auto (NPU/GPU/CPU)",
    val defaultPrivacyLevel: String = "LOCAL_ONLY",
    // Controllable System Prompt & Technical LLM Parameters
    val systemPrompt: String = "You are SWAYAM GPT, a sovereign personal intelligence assistant. Provide articulate, well-structured, precise, and helpful responses based on on-device context and personal memories.",
    val activeModelId: String = "gemma-2b-it-litert",
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val maxOutputTokens: Int = 1024,
    val contextWindowSize: Int = 8192,
    val presencePenalty: Float = 0.0f,
    val frequencyPenalty: Float = 0.0f,
    val streamResponse: Boolean = true,
    val stopSequences: String = "",
    // Controllable Skills & Capabilities
    val enabledSkills: Set<String> = setOf(
        "VISION_OCR",
        "MEMORY_RECALL",
        "TASK_MANAGEMENT",
        "CALENDAR_EVENTS",
        "AUDIO_JOURNAL",
        "DOCUMENT_VAULT",
        "MCP_TOOLS",
        "DEVICE_AUTOMATION"
    ),
    // Controllable Agent Actions
    val enabledActions: Set<String> = setOf(
        "CREATE_TASK",
        "CREATE_REMINDER",
        "SAVE_MEMORY",
        "CREATE_CALENDAR_EVENT",
        "START_TIMER",
        "OPEN_MAP",
        "OPEN_SCREEN"
    ),
    val requireHumanConfirmationForHighRisk: Boolean = true
)

class PreferenceEngine(private val context: Context) {
    private val KEY_STYLE = stringPreferencesKey("comm_style")
    private val KEY_PROFILE = stringPreferencesKey("latency_profile")
    private val KEY_BACKEND = stringPreferencesKey("preferred_backend")
    private val KEY_DEFAULT_PRIVACY = stringPreferencesKey("default_privacy")
    
    // Technical parameters keys
    private val KEY_SYSTEM_PROMPT = stringPreferencesKey("ai_system_prompt")
    private val KEY_ACTIVE_MODEL = stringPreferencesKey("ai_active_model")
    private val KEY_TEMPERATURE = floatPreferencesKey("ai_temperature")
    private val KEY_TOP_K = intPreferencesKey("ai_top_k")
    private val KEY_TOP_P = floatPreferencesKey("ai_top_p")
    private val KEY_MAX_TOKENS = intPreferencesKey("ai_max_tokens")
    private val KEY_CONTEXT_WINDOW = intPreferencesKey("ai_context_window")
    private val KEY_PRESENCE_PENALTY = floatPreferencesKey("ai_presence_penalty")
    private val KEY_FREQ_PENALTY = floatPreferencesKey("ai_freq_penalty")
    private val KEY_STREAM = booleanPreferencesKey("ai_stream")
    private val KEY_STOP_SEQS = stringPreferencesKey("ai_stop_sequences")
    private val KEY_ENABLED_SKILLS = stringSetPreferencesKey("ai_enabled_skills")
    private val KEY_ENABLED_ACTIONS = stringSetPreferencesKey("ai_enabled_actions")
    private val KEY_REQUIRE_CONFIRMATION = booleanPreferencesKey("ai_require_confirmation")

    val settings: Flow<EdgeUserSettings> = context.userPrefsDataStore.data.map { prefs ->
        EdgeUserSettings(
            communicationStyle = prefs[KEY_STYLE] ?: "Concise & Technical",
            latencyProfile = prefs[KEY_PROFILE] ?: "Balanced",
            preferredAiBackend = prefs[KEY_BACKEND] ?: "Auto (NPU/GPU/CPU)",
            defaultPrivacyLevel = prefs[KEY_DEFAULT_PRIVACY] ?: "LOCAL_ONLY",
            systemPrompt = prefs[KEY_SYSTEM_PROMPT] ?: "You are SWAYAM GPT, a sovereign personal intelligence assistant. Provide articulate, well-structured, precise, and helpful responses based on on-device context and personal memories.",
            activeModelId = prefs[KEY_ACTIVE_MODEL] ?: "gemma-2b-it-litert",
            temperature = prefs[KEY_TEMPERATURE] ?: 0.7f,
            topK = prefs[KEY_TOP_K] ?: 40,
            topP = prefs[KEY_TOP_P] ?: 0.95f,
            maxOutputTokens = prefs[KEY_MAX_TOKENS] ?: 1024,
            contextWindowSize = prefs[KEY_CONTEXT_WINDOW] ?: 8192,
            presencePenalty = prefs[KEY_PRESENCE_PENALTY] ?: 0.0f,
            frequencyPenalty = prefs[KEY_FREQ_PENALTY] ?: 0.0f,
            streamResponse = prefs[KEY_STREAM] ?: true,
            stopSequences = prefs[KEY_STOP_SEQS] ?: "",
            enabledSkills = prefs[KEY_ENABLED_SKILLS] ?: setOf(
                "VISION_OCR",
                "MEMORY_RECALL",
                "TASK_MANAGEMENT",
                "CALENDAR_EVENTS",
                "AUDIO_JOURNAL",
                "DOCUMENT_VAULT",
                "MCP_TOOLS",
                "DEVICE_AUTOMATION"
            ),
            enabledActions = prefs[KEY_ENABLED_ACTIONS] ?: setOf(
                "CREATE_TASK",
                "CREATE_REMINDER",
                "SAVE_MEMORY",
                "CREATE_CALENDAR_EVENT",
                "START_TIMER",
                "OPEN_MAP",
                "OPEN_SCREEN"
            ),
            requireHumanConfirmationForHighRisk = prefs[KEY_REQUIRE_CONFIRMATION] ?: true
        )
    }

    suspend fun updateSettings(newSettings: EdgeUserSettings) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[KEY_STYLE] = newSettings.communicationStyle
            prefs[KEY_PROFILE] = newSettings.latencyProfile
            prefs[KEY_BACKEND] = newSettings.preferredAiBackend
            prefs[KEY_DEFAULT_PRIVACY] = newSettings.defaultPrivacyLevel
            prefs[KEY_SYSTEM_PROMPT] = newSettings.systemPrompt
            prefs[KEY_ACTIVE_MODEL] = newSettings.activeModelId
            prefs[KEY_TEMPERATURE] = newSettings.temperature
            prefs[KEY_TOP_K] = newSettings.topK
            prefs[KEY_TOP_P] = newSettings.topP
            prefs[KEY_MAX_TOKENS] = newSettings.maxOutputTokens
            prefs[KEY_CONTEXT_WINDOW] = newSettings.contextWindowSize
            prefs[KEY_PRESENCE_PENALTY] = newSettings.presencePenalty
            prefs[KEY_FREQ_PENALTY] = newSettings.frequencyPenalty
            prefs[KEY_STREAM] = newSettings.streamResponse
            prefs[KEY_STOP_SEQS] = newSettings.stopSequences
            prefs[KEY_ENABLED_SKILLS] = newSettings.enabledSkills
            prefs[KEY_ENABLED_ACTIONS] = newSettings.enabledActions
            prefs[KEY_REQUIRE_CONFIRMATION] = newSettings.requireHumanConfirmationForHighRisk
        }
    }
}

