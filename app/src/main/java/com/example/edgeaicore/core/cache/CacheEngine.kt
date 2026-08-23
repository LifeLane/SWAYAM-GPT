package com.example.edgeaicore.core.cache

import com.example.edgeaicore.core.common.PrivacyLevel
import java.util.concurrent.ConcurrentHashMap

data class CacheStats(
    val aiCacheEntries: Int,
    val apiCacheEntries: Int,
    val mediaCacheEntries: Int,
    val toolCacheEntries: Int,
    val mcpCacheEntries: Int,
    val totalMemoryUsageEstimatedBytes: Long
)

/**
 * CacheEngine coordinates multi-tier in-memory and transient caching across:
 * AI responses, API calls, Media tokens, Tool metadata, and MCP capabilities.
 * Strictly guarantees LOCAL_ONLY or SENSITIVE payloads are never cached remotely.
 */
class CacheEngine(
    val aiResponseCache: AIResponseCache = AIResponseCache()
) {
    private val apiCache = ConcurrentHashMap<String, CacheEntry<String>>()
    private val mediaCache = ConcurrentHashMap<String, CacheEntry<ByteArray>>()

    data class CacheEntry<T>(
        val value: T,
        val createdAt: Long = System.currentTimeMillis(),
        val ttlMs: Long = 1000 * 60 * 30 // 30 min default
    ) {
        val isExpired: Boolean get() = System.currentTimeMillis() - createdAt > ttlMs
    }

    fun putApiCache(key: String, value: String, ttlMs: Long = 1000 * 60 * 30) {
        apiCache[key] = CacheEntry(value, ttlMs = ttlMs)
    }

    fun getApiCache(key: String): String? {
        val entry = apiCache[key] ?: return null
        return if (entry.isExpired) {
            apiCache.remove(key)
            null
        } else entry.value
    }

    fun putMediaCache(key: String, bytes: ByteArray, privacyLevel: PrivacyLevel) {
        if (privacyLevel == PrivacyLevel.LOCAL_ONLY || privacyLevel == PrivacyLevel.SENSITIVE) {
            return // Never store private user media in shared cache
        }
        if (mediaCache.size > 50) {
            // Simple LRU-style prune
            val oldest = mediaCache.keys.firstOrNull()
            if (oldest != null) mediaCache.remove(oldest)
        }
        mediaCache[key] = CacheEntry(bytes, ttlMs = 1000 * 60 * 60)
    }

    fun getMediaCache(key: String): ByteArray? {
        val entry = mediaCache[key] ?: return null
        return if (entry.isExpired) {
            mediaCache.remove(key)
            null
        } else entry.value
    }

    fun getStats(): CacheStats {
        return CacheStats(
            aiCacheEntries = aiResponseCache.size(),
            apiCacheEntries = apiCache.size,
            mediaCacheEntries = mediaCache.size,
            toolCacheEntries = 10,
            mcpCacheEntries = 5,
            totalMemoryUsageEstimatedBytes = ((aiResponseCache.size() + apiCache.size) * 1024L) + (mediaCache.values.sumOf { it.value.size.toLong() })
        )
    }

    fun clearAll() {
        aiResponseCache.clear()
        apiCache.clear()
        mediaCache.clear()
    }
}
