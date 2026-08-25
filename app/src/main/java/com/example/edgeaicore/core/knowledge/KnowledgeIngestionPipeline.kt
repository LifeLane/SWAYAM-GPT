package com.example.edgeaicore.core.knowledge

import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.database.DataPrivacyLevel
import com.example.edgeaicore.core.database.EdgeDatabase
import com.example.edgeaicore.core.database.KnowledgeChunkEntity
import com.example.edgeaicore.core.embeddings.EmbeddingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class IngestionResult(
    val knowledgeItemId: String,
    val totalChunksGenerated: Int,
    val embeddingsCreated: Int,
    val success: Boolean
)

/**
 * KnowledgeIngestionPipeline transforms raw input (text, docs, transcription, JSON)
 * through validation, normalization, chunking, embedding generation, and DB indexing.
 */
class KnowledgeIngestionPipeline(
    private val database: EdgeDatabase,
    private val knowledgeEngine: KnowledgeEngine,
    private val embeddingEngine: EmbeddingEngine
) {
    private val chunkDao = database.knowledgeChunkDao()

    suspend fun ingestDocument(
        title: String,
        rawText: String,
        type: KnowledgeType = KnowledgeType.DOCUMENT,
        source: String = "file_import",
        tags: List<String> = emptyList(),
        privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY,
        chunkStrategy: ChunkingStrategy = ChunkingStrategy.PARAGRAPH
    ): EdgeResult<IngestionResult> = withContext(Dispatchers.IO) {
        try {
            // 1. Validation & Normalization
            val cleanText = rawText.trim().replace("\r\n", "\n")
            if (cleanText.isBlank()) {
                return@withContext EdgeResult.Failure(IllegalArgumentException("Ingested content cannot be empty"))
            }

            // 2. Create Parent Knowledge Item
            val itemRes = knowledgeEngine.createKnowledge(
                title = title,
                content = cleanText.take(500), // Summary / preview in parent record
                type = type,
                source = source,
                tags = tags,
                privacyLevel = privacyLevel
            )
            if (itemRes is EdgeResult.Failure) {
                return@withContext EdgeResult.Failure(itemRes.error)
            }
            val parentItem = (itemRes as EdgeResult.Success).data

            // 3. Chunking
            val chunks = knowledgeEngine.chunking.chunkText(cleanText, parentItem.id, chunkStrategy)

            // 4. Embedding & Chunk Indexing
            var embeddedCount = 0
            val chunkEntities = mutableListOf<KnowledgeChunkEntity>()
            val embeddingEntities = mutableListOf<com.example.edgeaicore.core.database.EmbeddingEntity>()
            val embeddingDao = database.embeddingDao()

            for (chunk in chunks) {
                val embRes = embeddingEngine.generateEmbedding(chunk.text)
                var embRef: String? = null
                if (embRes is EdgeResult.Success) {
                    embeddedCount++
                    val fullVector = embRes.data
                    embRef = com.example.edgeaicore.core.embeddings.VectorMath.serializeVector(fullVector)

                    // Also persist in EmbeddingEntity table
                    val contentHash = java.security.MessageDigest.getInstance("SHA-256")
                        .digest(chunk.text.toByteArray(Charsets.UTF_8))
                        .joinToString("") { "%02x".format(it) }

                    embeddingEntities.add(
                        com.example.edgeaicore.core.database.EmbeddingEntity(
                            embeddingId = UUID.randomUUID().toString(),
                            sourceId = chunk.chunkId,
                            sourceType = "DOCUMENT_CHUNK",
                            modelId = "local-mobile-embedding-384",
                            dimension = fullVector.size,
                            vectorJson = embRef,
                            contentHash = contentHash,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }

                chunkEntities.add(
                    KnowledgeChunkEntity(
                        chunkId = chunk.chunkId,
                        documentId = parentItem.id,
                        chunkIndex = chunk.chunkIndex,
                        text = chunk.text,
                        pageNumber = chunk.pageNumber,
                        positionStart = chunk.positionStart,
                        positionEnd = chunk.positionEnd,
                        embeddingReference = embRef,
                        metadataJson = "{}"
                    )
                )
            }

            chunkDao.insertChunks(chunkEntities)
            if (embeddingEntities.isNotEmpty()) {
                embeddingDao.insertAll(embeddingEntities)
            }

            EdgeResult.Success(
                IngestionResult(
                    knowledgeItemId = parentItem.id,
                    totalChunksGenerated = chunks.size,
                    embeddingsCreated = embeddedCount,
                    success = true
                )
            )
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }
}
