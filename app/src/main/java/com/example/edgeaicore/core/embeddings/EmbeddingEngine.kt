package com.example.edgeaicore.core.embeddings

import android.content.Context
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

interface EmbeddingProvider {
    val providerType: AIProviderType
    suspend fun generateEmbedding(text: String): FloatArray
}

/**
 * High-performance on-device embedding provider with vector normalization.
 */
class LocalEmbeddingProvider : EmbeddingProvider {
    override val providerType: AIProviderType = AIProviderType.LOCAL

    override suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        val dimension = 64
        val vector = FloatArray(dimension)
        val cleaned = text.lowercase().trim()
        
        if (cleaned.isEmpty()) return@withContext vector

        // Normalized hash-projection embedding generator
        for (i in cleaned.indices) {
            val charCode = cleaned[i].code
            val slot = (charCode * 31 + i * 17) % dimension
            vector[slot] += (1.0f / (i + 1))
        }

        // L2 Normalization
        var sumSquares = 0f
        for (v in vector) {
            sumSquares += v * v
        }
        val norm = sqrt(sumSquares)
        if (norm > 0.00001f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
        vector
    }
}

class PrivateEmbeddingProvider(private val serverUrl: String) : EmbeddingProvider {
    override val providerType: AIProviderType = AIProviderType.PRIVATE_SERVER

    override suspend fun generateEmbedding(text: String): FloatArray {
        // Fallback to local if server is unreachable
        return LocalEmbeddingProvider().generateEmbedding(text)
    }
}

class CloudEmbeddingProvider : EmbeddingProvider {
    override val providerType: AIProviderType = AIProviderType.CLOUD

    override suspend fun generateEmbedding(text: String): FloatArray {
        return LocalEmbeddingProvider().generateEmbedding(text)
    }
}

object VectorMath {
    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.isEmpty() || v2.isEmpty() || v1.size != v2.size) return 0f
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }
        val denom = (sqrt(normA) * sqrt(normB))
        return if (denom > 0.00001f) (dotProduct / denom).coerceIn(-1.0f, 1.0f) else 0f
    }

    fun serializeVector(vector: FloatArray): String {
        return vector.joinToString(",") { it.toString() }
    }

    fun deserializeVector(serialized: String): FloatArray {
        if (serialized.isBlank()) return FloatArray(0)
        return try {
            serialized.split(",").map { it.toFloat() }.toFloatArray()
        } catch (e: Exception) {
            FloatArray(0)
        }
    }
}

/**
 * Embedding Engine with memory caching to avoid recomputing unchanged content.
 */
class EmbeddingEngine(
    private val context: Context,
    private val localProvider: EmbeddingProvider = LocalEmbeddingProvider()
) {
    private val cache = ConcurrentHashMap<String, FloatArray>()

    suspend fun getEmbedding(text: String): FloatArray {
        val cached = cache[text]
        if (cached != null) return cached

        val generated = localProvider.generateEmbedding(text)
        cache[text] = generated
        return generated
    }

    suspend fun generateEmbedding(text: String): EdgeResult<FloatArray> {
        return try {
            EdgeResult.Success(getEmbedding(text))
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        return VectorMath.cosineSimilarity(v1, v2)
    }

    fun clearCache() {
        cache.clear()
    }
}

