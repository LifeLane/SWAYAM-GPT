package com.example.edgeaicore.core.knowledge

import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.database.DataPrivacyLevel
import com.example.edgeaicore.core.database.EdgeDatabase
import com.example.edgeaicore.core.embeddings.EmbeddingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class KnowledgeSearchResult(
    val id: String,
    val documentId: String = id,
    val chunkIndex: Int = 0,
    val pageNumber: Int = 1,
    val title: String,
    val contentSnippet: String,
    val score: Float,
    val matchType: String, // KEYWORD, SEMANTIC, HYBRID
    val source: String,
    val privacyLevel: DataPrivacyLevel
)

/**
 * KnowledgeSearchEngine performs high-performance Hybrid Search:
 * Token/Keyword matching + Local Semantic Cosine Similarity + Ranking + Context Deduplication.
 */
class KnowledgeSearchEngine(
    private val database: EdgeDatabase,
    private val knowledgeEngine: KnowledgeEngine,
    private val embeddingEngine: EmbeddingEngine
) {
    private val chunkDao = database.knowledgeChunkDao()

    suspend fun search(
        query: String,
        limit: Int = 5,
        minScore: Float = 0.2f
    ): EdgeResult<List<KnowledgeSearchResult>> = withContext(Dispatchers.IO) {
        try {
            val cleanQuery = query.trim().lowercase()
            if (cleanQuery.isBlank()) return@withContext EdgeResult.Success(emptyList())

            val resultsMap = mutableMapOf<String, KnowledgeSearchResult>()

            // 1. Direct Keyword Search on Knowledge Items
            val allItems = knowledgeEngine.repository.getAllSync()
            val tokens = cleanQuery.split(Regex("[^a-zA-Z0-9]+")).filter { it.length > 1 }

            for (item in allItems) {
                val titleLower = item.title.lowercase()
                val contentLower = item.content.lowercase()
                val tagsLower = item.tags.map { it.lowercase() }

                val exactPhraseMatch = titleLower.contains(cleanQuery) || contentLower.contains(cleanQuery)
                val matchingTokens = tokens.count { token ->
                    titleLower.contains(token) || contentLower.contains(token) || tagsLower.any { it.contains(token) }
                }

                if (exactPhraseMatch || (tokens.isNotEmpty() && matchingTokens > 0)) {
                    var score = 0.4f
                    if (exactPhraseMatch) score += 0.4f
                    if (tokens.isNotEmpty()) score += (matchingTokens.toFloat() / tokens.size) * 0.2f
                    if (titleLower.contains(cleanQuery) || tokens.any { titleLower.contains(it) }) score += 0.2f

                    resultsMap[item.id] = KnowledgeSearchResult(
                        id = item.id,
                        documentId = item.id,
                        chunkIndex = 0,
                        pageNumber = 1,
                        title = item.title,
                        contentSnippet = item.content.take(300),
                        score = score,
                        matchType = "KEYWORD",
                        source = item.source,
                        privacyLevel = item.privacyLevel
                    )
                }
            }

            // 2. Semantic Search on Knowledge Chunks
            val chunks = chunkDao.getAllChunksSync()
            val queryEmbeddingRes = embeddingEngine.generateEmbedding(cleanQuery)

            if (queryEmbeddingRes is EdgeResult.Success) {
                val qVector = queryEmbeddingRes.data
                for (chunk in chunks) {
                    val chunkVector = if (!chunk.embeddingReference.isNullOrBlank()) {
                        com.example.edgeaicore.core.embeddings.VectorMath.deserializeVector(chunk.embeddingReference)
                    } else {
                        val embRes = embeddingEngine.generateEmbedding(chunk.text)
                        if (embRes is EdgeResult.Success) embRes.data else null
                    }

                    if (chunkVector != null && chunkVector.isNotEmpty()) {
                        val similarity = com.example.edgeaicore.core.embeddings.VectorMath.cosineSimilarity(qVector, chunkVector)
                        if (similarity >= minScore) {
                            val parent = allItems.find { it.id == chunk.documentId }
                            val existing = resultsMap[chunk.documentId]
                            val combinedScore = if (existing != null) (existing.score + similarity) / 1.5f else similarity
                            val matchType = if (existing != null) "HYBRID" else "SEMANTIC"

                            resultsMap[chunk.documentId] = KnowledgeSearchResult(
                                id = chunk.chunkId,
                                documentId = chunk.documentId,
                                chunkIndex = chunk.chunkIndex,
                                pageNumber = chunk.pageNumber,
                                title = parent?.title ?: "Document Chunk",
                                contentSnippet = chunk.text.take(300),
                                score = combinedScore,
                                matchType = matchType,
                                source = parent?.source ?: "doc_chunk",
                                privacyLevel = parent?.privacyLevel ?: DataPrivacyLevel.LOCAL_ONLY
                            )
                        }
                    }
                }
            }

            // 3. Ranking and Deduplication
            val sorted = resultsMap.values
                .sortedByDescending { it.score }
                .take(limit)

            EdgeResult.Success(sorted)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    /**
     * Formats top search results as compact, privacy-safe context strings for agent LLM prompts with exact citations.
     */
    suspend fun getFormattedAgentContext(query: String, maxItems: Int = 3): String {
        return when (val res = search(query, limit = maxItems)) {
            is EdgeResult.Success -> {
                if (res.data.isEmpty()) ""
                else buildString {
                    appendLine("--- RELEVANT KNOWLEDGE BASE CONTEXT (ON-DEVICE RAG) ---")
                    res.data.forEachIndexed { i, item ->
                        appendLine("[${i + 1}] Source: ${item.title} (Page ${item.pageNumber}, Chunk ${item.chunkIndex}, Match: ${item.matchType}, Relevance: ${"%.2f".format(item.score)})")
                        appendLine("    Excerpt: \"${item.contentSnippet}\"")
                    }
                    appendLine("---------------------------------------------------------")
                }
            }
            is EdgeResult.Failure -> ""
        }
    }
}
