package com.example.edgeaicore.core.explanation

import com.example.edgeaicore.core.common.AIProviderType
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
    val dataSourcesUsed: List<String>,
    val wasAiInvolved: Boolean,
    val providerType: AIProviderType = AIProviderType.LOCAL,
    val privacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY
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
        privacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY
    ): ExplanationRecord {
        val record = ExplanationRecord(
            featureName = featureName,
            whatHappened = whatHappened,
            whyReason = whyReason,
            confidenceScore = confidenceScore,
            dataSourcesUsed = dataSourcesUsed,
            wasAiInvolved = wasAiInvolved,
            providerType = providerType,
            privacyLevel = privacyLevel
        )
        _history.value = listOf(record) + _history.value.take(49)
        return record
    }

    fun clear() {
        _history.value = emptyList()
    }
}
