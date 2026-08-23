package com.example.edgeaicore.core.cloud

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.litertlm.GenerationRequest
import com.example.edgeaicore.core.litertlm.GenerationResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Production-ready Gemini API Client for SWAYAM GPT.
 * Integrates Google Gemini models (Gemini 2.5 Flash / Gemini 3.5 Flash)
 * with support for text generation, system instructions, RAG context, and multimodal vision/OCR.
 */
class GeminiApiClient(private val context: Context) {

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val DEFAULT_MODEL = "gemini-2.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") key else ""
        } catch (_: Exception) {
            ""
        }
    }

    fun isConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun generateText(request: GenerationRequest): EdgeResult<GenerationResponse> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext EdgeResult.Failure(
                EdgeAIError.CloudUnavailable("Gemini API", "API key not configured in AI Studio Secrets.")
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val model = if (request.modelId.contains("gemini", ignoreCase = true)) request.modelId else DEFAULT_MODEL
            val endpoint = "$BASE_URL$model:generateContent?key=$apiKey"

            // Construct Gemini Request JSON
            val requestJson = JSONObject()
            val contentsArray = JSONArray()

            // System prompt + context + user prompt
            val promptBuilder = StringBuilder()
            if (!request.context.isNullOrBlank()) {
                promptBuilder.append("--- RELEVANT RETRIEVED CONTEXT & MEMORIES ---\n")
                promptBuilder.append(request.context)
                promptBuilder.append("\n-----------------------------------------------\n\n")
            }
            promptBuilder.append(request.prompt)

            val contentObj = JSONObject()
            contentObj.put("role", "user")
            val partsArray = JSONArray()
            val textPart = JSONObject()
            textPart.put("text", promptBuilder.toString())
            partsArray.put(textPart)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)

            requestJson.put("contents", contentsArray)

            // System Instruction
            val sysInstruction = request.systemInstruction ?: "You are SWAYAM GPT, a sovereign personal intelligence assistant. Provide articulate, well-structured, and helpful responses based on user context and personal memories."
            val sysObj = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", sysInstruction))
            sysObj.put("parts", sysParts)
            requestJson.put("systemInstruction", sysObj)

            // Generation Config
            val genConfig = JSONObject()
            genConfig.put("temperature", request.temperature)
            genConfig.put("topK", request.topK)
            genConfig.put("topP", request.topP)
            genConfig.put("maxOutputTokens", request.maxTokens.coerceAtLeast(128))
            if (request.stopSequences.isNotEmpty()) {
                val stopArray = JSONArray()
                request.stopSequences.forEach { stopArray.put(it) }
                genConfig.put("stopSequences", stopArray)
            }
            requestJson.put("generationConfig", genConfig)

            val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val httpRequest = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val httpResponse = okHttpClient.newCall(httpRequest).execute()
            val responseBody = httpResponse.body?.string() ?: ""

            if (!httpResponse.isSuccessful) {
                Log.e(TAG, "Gemini API error: ${httpResponse.code} - $responseBody")
                return@withContext EdgeResult.Failure(
                    EdgeAIError.CloudUnavailable("Gemini API", "HTTP ${httpResponse.code}: ${httpResponse.message}")
                )
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val generatedText = parts?.optJSONObject(0)?.optString("text")

            if (generatedText.isNullOrBlank()) {
                return@withContext EdgeResult.Failure(
                    EdgeAIError.Unknown("Received empty text candidate from Gemini API.")
                )
            }

            val latency = System.currentTimeMillis() - startTime
            val tokenCount = (generatedText.length / 3.8).toInt().coerceAtLeast(1)
            val tokensPerSec = if (latency > 0) (tokenCount.toDouble() / (latency.toDouble() / 1000.0)) else 0.0

            EdgeResult.Success(
                GenerationResponse(
                    text = generatedText.trim(),
                    model = model,
                    latencyMs = latency,
                    tokensGenerated = tokenCount,
                    tokensPerSecond = tokensPerSec,
                    provider = AIProviderType.CLOUD,
                    source = "Google Gemini ($model)"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API execution error", e)
            EdgeResult.Failure(EdgeAIError.CloudUnavailable("Gemini API", e.message ?: "Network error"))
        }
    }

    suspend fun analyzeImage(bitmap: Bitmap, prompt: String = "Analyze this image in detail. If it contains a document or text, perform full OCR transcription. If it contains objects or scenes, list all detected objects with labels and description."): EdgeResult<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext EdgeResult.Failure(
                EdgeAIError.CloudUnavailable("Gemini API", "API key not configured.")
            )
        }

        try {
            val endpoint = "$BASE_URL$DEFAULT_MODEL:generateContent?key=$apiKey"

            // Convert Bitmap to base64 jpeg
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val requestJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // Text part
            partsArray.put(JSONObject().put("text", prompt))

            // Image part
            val inlineData = JSONObject()
            inlineData.put("mimeType", "image/jpeg")
            inlineData.put("data", base64Image)
            partsArray.put(JSONObject().put("inlineData", inlineData))

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val httpRequest = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val httpResponse = okHttpClient.newCall(httpRequest).execute()
            val responseBody = httpResponse.body?.string() ?: ""

            if (!httpResponse.isSuccessful) {
                return@withContext EdgeResult.Failure(
                    EdgeAIError.CloudUnavailable("Gemini Vision", "HTTP ${httpResponse.code}: ${httpResponse.message}")
                )
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val generatedText = parts?.optJSONObject(0)?.optString("text") ?: "No vision output"

            EdgeResult.Success(generatedText.trim())
        } catch (e: Exception) {
            EdgeResult.Failure(EdgeAIError.CloudUnavailable("Gemini Vision", e.message ?: "Analysis failed"))
        }
    }

    fun streamText(request: GenerationRequest): Flow<String> = flow {
        val result = generateText(request)
        when (result) {
            is EdgeResult.Success -> {
                val words = result.data.text.split(" ")
                for (word in words) {
                    emit("$word ")
                    kotlinx.coroutines.delay(20)
                }
            }
            is EdgeResult.Failure -> {
                emit("AI Generation Error: ${result.error.message}")
            }
        }
    }.flowOn(Dispatchers.IO)
}
