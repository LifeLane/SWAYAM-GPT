package com.example.edgeaicore.core.memory

import android.content.Context
import androidx.room.Room
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.embeddings.EmbeddingEngine
import com.example.edgeaicore.core.embeddings.VectorMath
import com.example.edgeaicore.core.storage.EncryptionVaultStatus
import com.example.edgeaicore.core.storage.LocalEncryptionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class RankedMemory(
    val memory: MemoryEntity,
    val score: Float
)

class MemoryRetriever(
    private val memoryDao: MemoryDao,
    private val embeddingEngine: EmbeddingEngine,
    private val encryptionEngine: LocalEncryptionEngine? = null
) {
    suspend fun retrieveMemories(
        query: String,
        maxResults: Int = 5,
        minSimilarity: Float = 0.25f,
        typeFilter: MemoryType? = null,
        maxPrivacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY
    ): List<RankedMemory> = withContext(Dispatchers.Default) {
        val rawMemories = memoryDao.getAllActiveMemoriesSync()
        if (rawMemories.isEmpty()) return@withContext emptyList()

        val allMemories = rawMemories.map { memory ->
            if (encryptionEngine != null) {
                memory.copy(
                    title = encryptionEngine.decryptString(memory.title),
                    summary = encryptionEngine.decryptString(memory.summary),
                    content = encryptionEngine.decryptString(memory.content),
                    tags = encryptionEngine.decryptString(memory.tags)
                )
            } else memory
        }

        val queryEmbedding = embeddingEngine.getEmbedding(query)
        val queryLower = query.lowercase().trim()

        val scoredList = allMemories.mapNotNull { memory ->
            // Filter by privacy level permission
            if (memory.privacyLevel > maxPrivacyLevel) return@mapNotNull null
            if (typeFilter != null && memory.type != typeFilter) return@mapNotNull null

            var score = 0f

            // 1. Exact / Partial text matches
            if (memory.title.lowercase().contains(queryLower)) score += 0.5f
            if (memory.content.lowercase().contains(queryLower)) score += 0.4f
            if (memory.tags.lowercase().contains(queryLower)) score += 0.3f

            // 2. Vector Semantic Similarity
            val memoryVector = if (!memory.embeddingReference.isNullOrBlank()) {
                VectorMath.deserializeVector(memory.embeddingReference)
            } else {
                embeddingEngine.getEmbedding("${memory.title} ${memory.content}")
            }
            val semanticScore = VectorMath.cosineSimilarity(queryEmbedding, memoryVector)
            score += semanticScore * 0.6f

            if (score >= minSimilarity) {
                RankedMemory(memory, score)
            } else {
                null
            }
        }

        scoredList.sortedByDescending { it.score }.take(maxResults)
    }
}

class MemoryContextBuilder(
    private val retriever: MemoryRetriever
) {
    suspend fun buildMemoryContext(
        query: String,
        maxMemories: Int = 4,
        maxPrivacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY
    ): String {
        val relevant = retriever.retrieveMemories(
            query = query,
            maxResults = maxMemories,
            maxPrivacyLevel = maxPrivacyLevel
        )

        if (relevant.isEmpty()) {
            return "I couldn't find that in your saved memories."
        }

        val sb = StringBuilder("RELEVANT ON-DEVICE MEMORIES:\n")
        relevant.forEachIndexed { idx, ranked ->
            val m = ranked.memory
            sb.append("${idx + 1}. [${m.type.name}] ${m.title}: ${m.content} (Tags: ${m.tags}, Privacy: ${m.privacyLevel})\n")
        }
        return sb.toString().trim()
    }
}

/**
 * High-level MemoryEngine exposing CRUD, indexing, vector search, and hardware AES-256-GCM encryption at rest.
 */
class MemoryEngine(
    private val context: Context,
    private val embeddingEngine: EmbeddingEngine,
    val encryptionEngine: LocalEncryptionEngine = LocalEncryptionEngine(context)
) {
    private val database: EdgeMemoryDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            EdgeMemoryDatabase::class.java,
            "edge_ai_memories.db"
        ).build()
    }

    val memoryDao: MemoryDao by lazy { database.memoryDao() }
    val retriever: MemoryRetriever by lazy { MemoryRetriever(memoryDao, embeddingEngine, encryptionEngine) }
    val contextBuilder: MemoryContextBuilder by lazy { MemoryContextBuilder(retriever) }

    fun getAllActiveMemories(): Flow<List<MemoryEntity>> = memoryDao.getAllActiveMemories().map { list ->
        list.map { decryptMemory(it) }
    }

    fun searchMemories(query: String): Flow<List<MemoryEntity>> = memoryDao.getAllActiveMemories().map { list ->
        val q = query.lowercase().trim()
        list.map { decryptMemory(it) }.filter {
            q.isBlank() || it.title.contains(q, true) || it.content.contains(q, true) || it.tags.contains(q, true)
        }
    }

    suspend fun getMemoryById(id: Long): MemoryEntity? = withContext(Dispatchers.IO) {
        memoryDao.getMemoryById(id)?.let { decryptMemory(it) }
    }

    fun getFavoriteMemories(): Flow<List<MemoryEntity>> = memoryDao.getFavoriteMemories().map { list ->
        list.map { decryptMemory(it) }
    }

    fun getMemoryCount(): Flow<Int> = memoryDao.getCount()

    fun getVaultStatus(): EncryptionVaultStatus = encryptionEngine.runCryptographicSelfTest()

    private fun decryptMemory(memory: MemoryEntity): MemoryEntity {
        return memory.copy(
            title = encryptionEngine.decryptString(memory.title),
            summary = encryptionEngine.decryptString(memory.summary),
            content = encryptionEngine.decryptString(memory.content),
            tags = encryptionEngine.decryptString(memory.tags)
        )
    }

    suspend fun createMemory(
        title: String,
        content: String,
        type: MemoryType = MemoryType.NOTE,
        tags: String = "",
        privacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY,
        location: String? = null
    ): MemoryEntity = withContext(Dispatchers.IO) {
        val summary = if (content.length > 80) content.take(77) + "..." else content
        val vector = embeddingEngine.getEmbedding("$title $content $tags")
        val serializedVector = VectorMath.serializeVector(vector)

        // Encrypt fields at rest with hardware AES-256-GCM
        val encTitle = encryptionEngine.encryptString(title)
        val encSummary = encryptionEngine.encryptString(summary)
        val encContent = encryptionEngine.encryptString(content)
        val encTags = encryptionEngine.encryptString(tags)

        val memory = MemoryEntity(
            title = encTitle,
            summary = encSummary,
            content = encContent,
            type = type,
            tags = encTags,
            privacyLevel = privacyLevel,
            location = location,
            embeddingReference = serializedVector,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = memoryDao.insertMemory(memory)
        
        // Return decrypted version for UI state
        MemoryEntity(
            id = id,
            title = title,
            summary = summary,
            content = content,
            type = type,
            tags = tags,
            privacyLevel = privacyLevel,
            location = location,
            embeddingReference = serializedVector,
            createdAt = memory.createdAt,
            updatedAt = memory.updatedAt
        )
    }

    suspend fun updateMemory(memory: MemoryEntity) = withContext(Dispatchers.IO) {
        val vector = embeddingEngine.getEmbedding("${memory.title} ${memory.content} ${memory.tags}")
        val encTitle = encryptionEngine.encryptString(memory.title)
        val encSummary = encryptionEngine.encryptString(memory.summary)
        val encContent = encryptionEngine.encryptString(memory.content)
        val encTags = encryptionEngine.encryptString(memory.tags)

        val updated = memory.copy(
            title = encTitle,
            summary = encSummary,
            content = encContent,
            tags = encTags,
            embeddingReference = VectorMath.serializeVector(vector),
            updatedAt = System.currentTimeMillis()
        )
        memoryDao.updateMemory(updated)
    }

    suspend fun deleteMemory(memory: MemoryEntity) = withContext(Dispatchers.IO) {
        memoryDao.deleteMemory(memory)
    }

    suspend fun deleteMemory(id: Long) = withContext(Dispatchers.IO) {
        val mem = memoryDao.getMemoryById(id)
        if (mem != null) {
            memoryDao.deleteMemory(mem)
        }
    }

    suspend fun deleteMemory(id: String) = withContext(Dispatchers.IO) {
        val longId = id.toLongOrNull()
        if (longId != null) {
            deleteMemory(longId)
        }
    }

    suspend fun toggleFavorite(memory: MemoryEntity) = withContext(Dispatchers.IO) {
        memoryDao.updateMemory(memory.copy(isFavorite = !memory.isFavorite))
    }

    suspend fun archiveMemory(memory: MemoryEntity) = withContext(Dispatchers.IO) {
        memoryDao.updateMemory(memory.copy(isArchived = true))
    }

    suspend fun clearAllMemories() = withContext(Dispatchers.IO) {
        memoryDao.deleteAllMemories()
    }
}

