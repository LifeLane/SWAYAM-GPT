package com.example.edgeaicore.core.memory

import android.content.Context
import androidx.room.Room
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.embeddings.EmbeddingEngine
import com.example.edgeaicore.core.embeddings.VectorMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class RankedMemory(
    val memory: MemoryEntity,
    val score: Float
)

class MemoryRetriever(
    private val memoryDao: MemoryDao,
    private val embeddingEngine: EmbeddingEngine
) {
    suspend fun retrieveMemories(
        query: String,
        maxResults: Int = 5,
        minSimilarity: Float = 0.25f,
        typeFilter: MemoryType? = null,
        maxPrivacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY
    ): List<RankedMemory> = withContext(Dispatchers.Default) {
        val allMemories = memoryDao.getAllActiveMemoriesSync()
        if (allMemories.isEmpty()) return@withContext emptyList()

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
 * High-level MemoryEngine exposing CRUD, indexing, and vector search operations.
 */
class MemoryEngine(
    private val context: Context,
    private val embeddingEngine: EmbeddingEngine
) {
    private val database: EdgeMemoryDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            EdgeMemoryDatabase::class.java,
            "edge_ai_memories.db"
        ).fallbackToDestructiveMigration().build()
    }

    val memoryDao: MemoryDao by lazy { database.memoryDao() }
    val retriever: MemoryRetriever by lazy { MemoryRetriever(memoryDao, embeddingEngine) }
    val contextBuilder: MemoryContextBuilder by lazy { MemoryContextBuilder(retriever) }

    fun getAllActiveMemories(): Flow<List<MemoryEntity>> = memoryDao.getAllActiveMemories()

    fun searchMemories(query: String): Flow<List<MemoryEntity>> = memoryDao.searchMemories(query)

    fun getFavoriteMemories(): Flow<List<MemoryEntity>> = memoryDao.getFavoriteMemories()

    fun getMemoryCount(): Flow<Int> = memoryDao.getCount()

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

        val memory = MemoryEntity(
            title = title,
            summary = summary,
            content = content,
            type = type,
            tags = tags,
            privacyLevel = privacyLevel,
            location = location,
            embeddingReference = serializedVector,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = memoryDao.insertMemory(memory)
        memory.copy(id = id)
    }

    suspend fun updateMemory(memory: MemoryEntity) = withContext(Dispatchers.IO) {
        val vector = embeddingEngine.getEmbedding("${memory.title} ${memory.content} ${memory.tags}")
        val updated = memory.copy(
            embeddingReference = VectorMath.serializeVector(vector),
            updatedAt = System.currentTimeMillis()
        )
        memoryDao.updateMemory(updated)
    }

    suspend fun deleteMemory(memory: MemoryEntity) = withContext(Dispatchers.IO) {
        memoryDao.deleteMemory(memory)
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
