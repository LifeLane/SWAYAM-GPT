package com.example.edgeaicore.core.ai

import android.content.Context
import com.example.edgeaicore.core.cache.AIResponseCache
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.TaskType
import com.example.edgeaicore.core.diagnostics.PerformanceMonitor
import com.example.edgeaicore.core.litertlm.GenerationRequest
import com.example.edgeaicore.core.litertlm.GenerationResponse
import com.example.edgeaicore.core.privacy.PrivacyEngine
import kotlinx.coroutines.flow.Flow

data class AIRequest(
    val prompt: String,
    val taskType: TaskType = TaskType.TEXT_GENERATION,
    val systemInstruction: String? = null,
    val context: String? = null,
    val privacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY,
    val requiresVision: Boolean = false,
    val requiresLargeModel: Boolean = false,
    val offlineRequired: Boolean = false,
    val userConsent: Boolean = false,
    val preferredProvider: AIProviderType = AIProviderType.LOCAL,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val maxTokens: Int = 1024,
    val modelId: String = "gemma-2b-it-litert",
    val stopSequences: List<String> = emptyList()
)

/**
 * Intelligent AI Router:
 * The central routing arbiter of EdgeAI Core.
 * Priority:
 * 1. Local AI (On-Device LiteRT-LM)
 * 2. Private Server (if large model required / local unavailable, and private server enabled & consented)
 * 3. Cloud Fallback (if cloud explicitly enabled, consented, and privacy rules allow)
 * 4. Graceful unavailable state (NEVER silently leak data)
 */
class AIRouter(
    private val context: Context,
    val localProvider: LocalAIProvider,
    private val privateServerProvider: PrivateServerAIProvider,
    private val cloudProvider: CloudFallbackAIProvider,
    private val privacyEngine: PrivacyEngine,
    private val cache: AIResponseCache,
    private val performanceMonitor: PerformanceMonitor
) {
    suspend fun generate(request: AIRequest): EdgeResult<GenerationResponse> {
        val targetProviderType = determineTargetProvider(request)

        // 1. Enforce Privacy Gatekeeper
        val isPrivacyApproved = privacyEngine.validateRouting(
            privacyLevel = request.privacyLevel,
            targetProvider = targetProviderType,
            userConsentGiven = request.userConsent
        )

        if (!isPrivacyApproved) {
            return EdgeResult.Failure(
                EdgeAIError.PrivacyViolation("Request with privacy level ${request.privacyLevel} cannot be routed to $targetProviderType without explicit user authorization.")
            )
        }

        val genReq = GenerationRequest(
            prompt = request.prompt,
            systemInstruction = request.systemInstruction,
            context = request.context,
            temperature = request.temperature,
            topK = request.topK,
            topP = request.topP,
            maxTokens = request.maxTokens,
            modelId = request.modelId,
            stopSequences = request.stopSequences
        )

        // 2. Check AI Response Cache
        val cached = cache.get(genReq, request.privacyLevel)
        if (cached != null) {
            return EdgeResult.Success(cached.copy(source = "${cached.source} (Cached)"))
        }

        // 3. Dispatch to Approved Provider
        val result = when (targetProviderType) {
            AIProviderType.LOCAL -> localProvider.generate(genReq)
            AIProviderType.PRIVATE_SERVER -> privateServerProvider.generate(genReq)
            AIProviderType.CLOUD -> cloudProvider.generate(genReq)
            AIProviderType.DEMO -> MockAIProvider().generate(genReq)
        }

        if (result is EdgeResult.Success) {
            cache.put(genReq, result.data, request.privacyLevel)
            performanceMonitor.recordInference(
                latencyMs = result.data.latencyMs,
                tokensGenerated = result.data.tokensGenerated,
                success = true,
                modelId = result.data.model
            )
        }

        return result
    }

    fun stream(request: AIRequest): Flow<String> {
        val target = determineTargetProvider(request)
        val genReq = GenerationRequest(
            prompt = request.prompt,
            systemInstruction = request.systemInstruction,
            context = request.context,
            temperature = request.temperature,
            topK = request.topK,
            topP = request.topP,
            maxTokens = request.maxTokens,
            modelId = request.modelId,
            stopSequences = request.stopSequences
        )
        return when (target) {
            AIProviderType.LOCAL -> localProvider.stream(genReq)
            AIProviderType.PRIVATE_SERVER -> privateServerProvider.stream(genReq)
            AIProviderType.CLOUD -> cloudProvider.stream(genReq)
            AIProviderType.DEMO -> MockAIProvider().stream(genReq)
        }
    }

    fun determineTargetProvider(request: AIRequest): AIProviderType {
        if (request.privacyLevel == PrivacyLevel.LOCAL_ONLY) {
            return AIProviderType.LOCAL
        }

        if (request.offlineRequired) {
            return AIProviderType.LOCAL
        }

        // If a heavy reasoning task is requested and private server is configured
        if (request.requiresLargeModel && request.userConsent) {
            return AIProviderType.PRIVATE_SERVER
        }

        // Honor user preferred provider if privacy allows
        return when (request.preferredProvider) {
            AIProviderType.LOCAL -> AIProviderType.LOCAL
            AIProviderType.PRIVATE_SERVER -> if (request.privacyLevel != PrivacyLevel.LOCAL_ONLY) AIProviderType.PRIVATE_SERVER else AIProviderType.LOCAL
            AIProviderType.CLOUD -> if (request.privacyLevel == PrivacyLevel.PUBLIC && request.userConsent) AIProviderType.CLOUD else AIProviderType.LOCAL
            AIProviderType.DEMO -> AIProviderType.DEMO
        }
    }
}
