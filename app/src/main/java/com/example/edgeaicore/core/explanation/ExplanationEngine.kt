package com.example.edgeaicore.core.explanation

import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.ExecutionBackend
import com.example.edgeaicore.core.common.PrivacyLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class ExplanationRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val featureName: String,
    val whatHappened: String,
    val whyReason: String,
    val confidenceScore: Float,
    val dataSourcesUsed: List<String> = emptyList(),
    val wasAiInvolved: Boolean = true,
    val providerType: AIProviderType = AIProviderType.LOCAL,
    val privacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY,
    val executionBackend: ExecutionBackend = ExecutionBackend.CPU,
    val runtimeEngine: String = "LiteRT-LM On-Device Runtime",
    val networkUsed: Boolean = false,
    val latencyMs: Long = 0L,
    val modelName: String = "Gemma 2B IT (LiteRT-LM)",
    val memoriesUsed: List<String> = emptyList(),
    val ragSources: List<String> = emptyList(),
    val toolsUsed: List<String> = emptyList(),
    val agentsUsed: List<String> = emptyList(),
    val tokensGenerated: Int = 0,
    val tokensPerSecond: Double = 0.0,
    val isOfflineMode: Boolean = true
)

class ExplanationEngine {
    private val _history = MutableStateFlow<List<ExplanationRecord>>(emptyList())
    val history: StateFlow<List<ExplanationRecord>> = _history.asStateFlow()

    fun record(
        featureName: String,
        whatHappened: String,
        whyReason: String,
        confidenceScore: Float,
        dataSourcesUsed: List<String>,
        wasAiInvolved: Boolean = true,
        providerType: AIProviderType = AIProviderType.LOCAL,
        privacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY,
        executionBackend: ExecutionBackend = ExecutionBackend.CPU,
        runtimeEngine: String = "LiteRT-LM On-Device Runtime",
        networkUsed: Boolean = false,
        latencyMs: Long = 0L,
        modelName: String = "Gemma 2B IT (LiteRT-LM)",
        memoriesUsed: List<String> = emptyList(),
        ragSources: List<String> = emptyList(),
        toolsUsed: List<String> = emptyList(),
        agentsUsed: List<String> = emptyList(),
        tokensGenerated: Int = 0,
        tokensPerSecond: Double = 0.0,
        isOfflineMode: Boolean = true
    ): ExplanationRecord {
        val record = ExplanationRecord(
            featureName = featureName,
            whatHappened = whatHappened,
            whyReason = whyReason,
            confidenceScore = confidenceScore,
            dataSourcesUsed = dataSourcesUsed,
            wasAiInvolved = wasAiInvolved,
            providerType = providerType,
            privacyLevel = privacyLevel,
            executionBackend = executionBackend,
            runtimeEngine = runtimeEngine,
            networkUsed = networkUsed,
            latencyMs = latencyMs,
            modelName = modelName,
            memoriesUsed = memoriesUsed,
            ragSources = ragSources,
            toolsUsed = toolsUsed,
            agentsUsed = agentsUsed,
            tokensGenerated = tokensGenerated,
            tokensPerSecond = tokensPerSecond,
            isOfflineMode = isOfflineMode
        )
        _history.value = listOf(record) + _history.value.take(49)
        return record
    }

    fun clear() {
        _history.value = emptyList()
    }
}

