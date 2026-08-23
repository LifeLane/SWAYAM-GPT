package com.example.edgeaicore.core.cloud

import android.content.Context
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.litertlm.GenerationRequest
import com.example.edgeaicore.core.litertlm.GenerationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Cloud AI Provider:
 * Communicates with Google Gemini models via GeminiApiClient.
 */
class CloudFallbackProvider(private val context: Context) {
    val geminiClient = GeminiApiClient(context)
    private var isCloudOptInEnabled = true

    fun setOptIn(enabled: Boolean) {
        isCloudOptInEnabled = enabled
    }

    suspend fun generate(request: GenerationRequest): EdgeResult<GenerationResponse> = withContext(Dispatchers.IO) {
        if (!isCloudOptInEnabled) {
            return@withContext EdgeResult.Failure(
                EdgeAIError.PrivacyViolation("Cloud AI is disabled in user privacy settings.")
            )
        }

        if (geminiClient.isConfigured()) {
            return@withContext geminiClient.generateText(request)
        } else {
            return@withContext EdgeResult.Failure(
                EdgeAIError.CloudUnavailable("Gemini API", "API key is not configured in AI Studio Secrets.")
            )
        }
    }

    fun stream(request: GenerationRequest): Flow<String> = flow {
        if (!isCloudOptInEnabled) {
            emit("Error: Cloud AI disabled in privacy settings.")
            return@flow
        }
        if (geminiClient.isConfigured()) {
            geminiClient.streamText(request).collect {
                emit(it)
            }
        } else {
            emit("Error: Gemini API key is not configured.")
        }
    }.flowOn(Dispatchers.IO)
}

