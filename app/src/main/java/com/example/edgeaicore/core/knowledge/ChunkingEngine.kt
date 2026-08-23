package com.example.edgeaicore.core.knowledge

import java.util.UUID

enum class ChunkingStrategy {
    PARAGRAPH,
    HEADING,
    PAGE,
    SEMANTIC,
    FIXED_SIZE
}

data class TextChunk(
    val chunkId: String,
    val documentId: String,
    val chunkIndex: Int,
    val text: String,
    val pageNumber: Int = 1,
    val positionStart: Int = 0,
    val positionEnd: Int = 0,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * ChunkingEngine splits structured and unstructured text into manageable pieces for embedding & LLM context.
 */
class ChunkingEngine {

    fun chunkText(
        text: String,
        documentId: String,
        strategy: ChunkingStrategy = ChunkingStrategy.PARAGRAPH,
        chunkSizeChars: Int = 500,
        chunkOverlapChars: Int = 50
    ): List<TextChunk> {
        if (text.isBlank()) return emptyList()

        return when (strategy) {
            ChunkingStrategy.PARAGRAPH -> chunkByParagraph(text, documentId)
            ChunkingStrategy.HEADING -> chunkByHeading(text, documentId)
            ChunkingStrategy.PAGE -> chunkByFixedSize(text, documentId, 1500, 100)
            ChunkingStrategy.FIXED_SIZE -> chunkByFixedSize(text, documentId, chunkSizeChars, chunkOverlapChars)
            ChunkingStrategy.SEMANTIC -> chunkByParagraph(text, documentId) // semantic boundary fallback
        }
    }

    private fun chunkByParagraph(text: String, documentId: String): List<TextChunk> {
        val paragraphs = text.split(Regex("(\r?\n){2,}")).filter { it.isNotBlank() }
        val chunks = mutableListOf<TextChunk>()
        var currentPos = 0

        paragraphs.forEachIndexed { index, p ->
            val clean = p.trim()
            val start = text.indexOf(clean, currentPos).coerceAtLeast(currentPos)
            val end = start + clean.length
            currentPos = end

            chunks.add(
                TextChunk(
                    chunkId = "${documentId}_chunk_$index",
                    documentId = documentId,
                    chunkIndex = index,
                    text = clean,
                    positionStart = start,
                    positionEnd = end
                )
            )
        }
        return chunks
    }

    private fun chunkByHeading(text: String, documentId: String): List<TextChunk> {
        val lines = text.lines()
        val chunks = mutableListOf<TextChunk>()
        var currentHeading = "Section 1"
        var currentBuffer = StringBuilder()
        var chunkIndex = 0

        for (line in lines) {
            if (line.startsWith("#") || line.matches(Regex("^[0-9]+\\..*"))) {
                if (currentBuffer.isNotBlank()) {
                    chunks.add(
                        TextChunk(
                            chunkId = "${documentId}_chunk_${chunkIndex++}",
                            documentId = documentId,
                            chunkIndex = chunkIndex - 1,
                            text = currentBuffer.toString().trim(),
                            metadata = mapOf("heading" to currentHeading)
                        )
                    )
                    currentBuffer = StringBuilder()
                }
                currentHeading = line.trim('#', ' ')
            }
            currentBuffer.appendLine(line)
        }

        if (currentBuffer.isNotBlank()) {
            chunks.add(
                TextChunk(
                    chunkId = "${documentId}_chunk_${chunkIndex++}",
                    documentId = documentId,
                    chunkIndex = chunkIndex - 1,
                    text = currentBuffer.toString().trim(),
                    metadata = mapOf("heading" to currentHeading)
                )
            )
        }

        return chunks
    }

    private fun chunkByFixedSize(
        text: String,
        documentId: String,
        chunkSize: Int,
        overlap: Int
    ): List<TextChunk> {
        val chunks = mutableListOf<TextChunk>()
        var start = 0
        var chunkIndex = 0

        while (start < text.length) {
            val end = (start + chunkSize).coerceAtMost(text.length)
            val slice = text.substring(start, end).trim()
            if (slice.isNotBlank()) {
                chunks.add(
                    TextChunk(
                        chunkId = "${documentId}_chunk_${chunkIndex++}",
                        documentId = documentId,
                        chunkIndex = chunkIndex - 1,
                        text = slice,
                        positionStart = start,
                        positionEnd = end
                    )
                )
            }
            if (end == text.length) break
            start += (chunkSize - overlap).coerceAtLeast(1)
        }

        return chunks
    }
}
