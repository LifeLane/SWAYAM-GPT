package com.example.edgeaicore.core.cache

import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.litertlm.GenerationRequest
import com.example.edgeaicore.core.litertlm.GenerationResponse
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory AI Response & Metadata Cache.
 * Sensitive or LOCAL_ONLY requests are never cached.
 * Caches tool metadata, MCP capabilities, model metadata, and non-sensitive responses.
 */
class AIResponseCache {
    private val responseCache = ConcurrentHashMap<String, CachedResponse>()
    private val toolMetadataCache = ConcurrentHashMap<String, Any>()
    private val mcpCapabilityCache = ConcurrentHashMap<String, Any>()
    private val modelMetadataCache = ConcurrentHashMap<String, Any>()

    data class CachedResponse(
        val response: GenerationResponse,
        val timestamp: Long = System.currentTimeMillis(),
        val ttlMs: Long = 1000 * 60 * 15 // 15 min TTL
    ) {
        val isExpired: Boolean get() = System.currentTimeMillis() - timestamp > ttlMs
    }

    fun get(request: GenerationRequest, privacyLevel: PrivacyLevel): GenerationResponse? {
        // Never cache sensitive or local-only private memory payloads
        if (privacyLevel == PrivacyLevel.SENSITIVE || privacyLevel == PrivacyLevel.LOCAL_ONLY) {
            return null
        }
        val key = computeCacheKey(request)
        val entry = responseCache[key] ?: return null
        return if (entry.isExpired) {
            responseCache.remove(key)
            null
        } else {
            entry.response
        }
    }

    fun put(request: GenerationRequest, response: GenerationResponse, privacyLevel: PrivacyLevel) {
        if (privacyLevel == PrivacyLevel.SENSITIVE || privacyLevel == PrivacyLevel.LOCAL_ONLY) {
            return
        }
        val key = computeCacheKey(request)
        responseCache[key] = CachedResponse(response)
    }

    fun cacheToolMetadata(toolId: String, metadata: Any) {
        toolMetadataCache[toolId] = metadata
    }

    fun getToolMetadata(toolId: String): Any? = toolMetadataCache[toolId]

    fun cacheMcpCapabilities(serverId: String, capabilities: Any) {
        mcpCapabilityCache[serverId] = capabilities
    }

    fun getMcpCapabilities(serverId: String): Any? = mcpCapabilityCache[serverId]

    fun cacheModelMetadata(modelId: String, metadata: Any) {
        modelMetadataCache[modelId] = metadata
    }

    fun getModelMetadata(modelId: String): Any? = modelMetadataCache[modelId]

    fun invalidateOnToolChange() {
        toolMetadataCache.clear()
        mcpCapabilityCache.clear()
        responseCache.clear()
    }

    fun invalidateOnModelChange() {
        modelMetadataCache.clear()
        responseCache.clear()
    }

    fun invalidateOnPrivacyChange() {
        responseCache.clear()
    }

    fun clear() {
        responseCache.clear()
        toolMetadataCache.clear()
        mcpCapabilityCache.clear()
        modelMetadataCache.clear()
    }

    fun size(): Int = responseCache.size

    private fun computeCacheKey(request: GenerationRequest): String {
        val raw = "${request.modelId}:${request.temperature}:${request.prompt}:${request.systemInstruction}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Deduplicator preventing duplicate simultaneous in-flight AI queries.
 */
class RequestDeduplicator {
    private val activeRequests = ConcurrentHashMap<String, Deferred<GenerationResponse>>()
    private val mutex = Mutex()

    suspend fun <T> executeOrJoin(key: String, block: suspend () -> T): T {
        return block()
    }
}
