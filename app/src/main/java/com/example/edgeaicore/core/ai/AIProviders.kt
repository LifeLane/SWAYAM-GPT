package com.example.edgeaicore.core.ai

import android.content.Context
import com.example.edgeaicore.core.cloud.CloudFallbackProvider
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.ExecutionBackend
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.TaskType
import com.example.edgeaicore.core.litertlm.GenerationRequest
import com.example.edgeaicore.core.litertlm.GenerationResponse
import com.example.edgeaicore.core.litertlm.LiteRTLMEngine
import com.example.edgeaicore.core.server.PrivateAIClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface AIProvider {
    val providerType: AIProviderType
    suspend fun isAvailable(): Boolean
    suspend fun generate(request: GenerationRequest): EdgeResult<GenerationResponse>
    fun stream(request: GenerationRequest): Flow<String>
}

class LocalAIProvider(
    val liteRTLMEngine: LiteRTLMEngine
) : AIProvider {
    override val providerType: AIProviderType = AIProviderType.LOCAL

    override suspend fun isAvailable(): Boolean {
        return liteRTLMEngine.isReady()
    }

    override suspend fun generate(request: GenerationRequest): EdgeResult<GenerationResponse> {
        if (!liteRTLMEngine.isReady()) {
            val initRes = liteRTLMEngine.initialize(request.modelId, ExecutionBackend.GPU)
            if (initRes is EdgeResult.Failure) {
                return EdgeResult.Failure(
                    EdgeAIError.ModelUnavailable("SWAYAM local intelligence is unavailable because no verified local model is loaded.")
                )
            }
        }
        return liteRTLMEngine.generate(request)
    }

    override fun stream(request: GenerationRequest): Flow<String> {
        return liteRTLMEngine.stream(request)
    }
}

class PrivateServerAIProvider(
    private val privateAIClient: PrivateAIClient
) : AIProvider {
    override val providerType: AIProviderType = AIProviderType.PRIVATE_SERVER

    override suspend fun isAvailable(): Boolean {
        return privateAIClient.isServerReachable()
    }

    override suspend fun generate(request: GenerationRequest): EdgeResult<GenerationResponse> {
        return privateAIClient.generate(request)
    }

    override fun stream(request: GenerationRequest): Flow<String> {
        return privateAIClient.stream(request)
    }
}

class CloudFallbackAIProvider(
    private val cloudProvider: CloudFallbackProvider
) : AIProvider {
    override val providerType: AIProviderType = AIProviderType.CLOUD

    override suspend fun isAvailable(): Boolean {
        return true
    }

    override suspend fun generate(request: GenerationRequest): EdgeResult<GenerationResponse> {
        return cloudProvider.generate(request)
    }

    override fun stream(request: GenerationRequest): Flow<String> {
        return cloudProvider.stream(request)
    }
}

class MockAIProvider : AIProvider {
    override val providerType: AIProviderType = AIProviderType.DEMO

    override suspend fun isAvailable(): Boolean = true

    override suspend fun generate(request: GenerationRequest): EdgeResult<GenerationResponse> {
        return EdgeResult.Success(
            GenerationResponse(
                text = "[DEMO AI] Simulated inference output for development: '${request.prompt}'",
                model = "demo-mock-v1",
                latencyMs = 12,
                tokensGenerated = 18,
                tokensPerSecond = 150.0,
                provider = AIProviderType.DEMO,
                source = "Mock Development Provider"
            )
        )
    }

    override fun stream(request: GenerationRequest): Flow<String> = flow {
        emit("[DEMO AI] Simulated response stream.")
    }
}
