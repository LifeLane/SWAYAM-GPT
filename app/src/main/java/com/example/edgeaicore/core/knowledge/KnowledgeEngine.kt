package com.example.edgeaicore.core.knowledge

import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.database.DataPrivacyLevel
import com.example.edgeaicore.core.database.EdgeDatabase
import com.example.edgeaicore.core.database.KnowledgeDao
import com.example.edgeaicore.core.database.KnowledgeItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

enum class KnowledgeType {
    DOCUMENT,
    NOTE,
    IMAGE,
    WEB,
    MEMORY,
    STRUCTURED
}

data class KnowledgeItem(
    val id: String,
    val title: String,
    val content: String,
    val type: KnowledgeType,
    val source: String = "USER",
    val sourceId: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY,
    val embeddingReference: String? = null,
    val checksumSha256: String = ""
) {
    fun toEntity(): KnowledgeItemEntity = KnowledgeItemEntity(
        id = id,
        title = title,
        content = content,
        type = type.name,
        source = source,
        sourceId = sourceId,
        tags = tags.joinToString(","),
        privacyLevel = privacyLevel,
        embeddingReference = embeddingReference,
        checksumSha256 = checksumSha256,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromEntity(entity: KnowledgeItemEntity): KnowledgeItem = KnowledgeItem(
            id = entity.id,
            title = entity.title,
            content = entity.content,
            type = try { KnowledgeType.valueOf(entity.type) } catch (_: Exception) { KnowledgeType.NOTE },
            source = entity.source,
            sourceId = entity.sourceId,
            tags = if (entity.tags.isBlank()) emptyList() else entity.tags.split(",").map { it.trim() },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            privacyLevel = entity.privacyLevel,
            embeddingReference = entity.embeddingReference,
            checksumSha256 = entity.checksumSha256
        )
    }
}

/**
 * KnowledgeRepository manages persistence of knowledge entities in Room.
 */
class KnowledgeRepository(private val database: EdgeDatabase) {
    private val dao: KnowledgeDao = database.knowledgeDao()

    fun observeAll(): Flow<List<KnowledgeItemEntity>> = dao.getAllKnowledgeItems()
    fun search(query: String): Flow<List<KnowledgeItemEntity>> = dao.searchKnowledge(query)
    fun getCount(): Flow<Int> = dao.getCount()

    suspend fun getById(id: String): KnowledgeItem? = withContext(Dispatchers.IO) {
        dao.getKnowledgeById(id)?.let { KnowledgeItem.fromEntity(it) }
    }

    suspend fun getAllSync(): List<KnowledgeItem> = withContext(Dispatchers.IO) {
        dao.getAllKnowledgeSync().map { KnowledgeItem.fromEntity(it) }
    }

    suspend fun save(item: KnowledgeItem): EdgeResult<String> = withContext(Dispatchers.IO) {
        try {
            dao.insertKnowledge(item.toEntity())
            EdgeResult.Success(item.id)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    suspend fun delete(id: String, softDelete: Boolean = true): EdgeResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (softDelete) dao.softDeleteKnowledge(id) else dao.purgeKnowledge(id)
            EdgeResult.Success(Unit)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }
}

/**
 * KnowledgeEngine coordinates ingestion, indexing, chunking, and semantic search.
 */
class KnowledgeEngine(
    val database: EdgeDatabase,
    val repository: KnowledgeRepository = KnowledgeRepository(database),
    val chunking: ChunkingEngine = ChunkingEngine()
) {
    suspend fun createKnowledge(
        title: String,
        content: String,
        type: KnowledgeType = KnowledgeType.NOTE,
        source: String = "USER",
        tags: List<String> = emptyList(),
        privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY
    ): EdgeResult<KnowledgeItem> = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(content.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            val item = KnowledgeItem(
                id = UUID.randomUUID().toString(),
                title = title,
                content = content,
                type = type,
                source = source,
                tags = tags,
                privacyLevel = privacyLevel,
                checksumSha256 = hash
            )
            repository.save(item)
            EdgeResult.Success(item)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }
}
