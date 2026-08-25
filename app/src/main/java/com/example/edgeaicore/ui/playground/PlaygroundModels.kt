package com.example.edgeaicore.ui.playground

import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.ExecutionBackend
import com.example.edgeaicore.core.explanation.ExplanationRecord
import java.util.UUID

enum class MessageRole {
    USER, ASSISTANT, SYSTEM, TOOL
}

data class PlaygroundSource(
    val title: String,
    val snippet: String,
    val relevance: Float,
    val sourceType: String = "Document",
    val documentId: String? = null,
    val pageNumber: Int? = null,
    val section: String? = null
)

data class PlaygroundMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole = MessageRole.ASSISTANT,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val model: String = "gemma-2b-it-litert",
    val provider: AIProviderType = AIProviderType.LOCAL,
    val runtime: String = "LiteRT-LM On-Device",
    val backend: ExecutionBackend = ExecutionBackend.AUTO,
    val latencyMs: Long = 0L,
    val tokensGenerated: Int = 0,
    val tokensPerSecond: Double = 0.0,
    val sources: List<PlaygroundSource> = emptyList(),
    val memoryUsed: List<String> = emptyList(),
    val documentsUsed: List<String> = emptyList(),
    val toolsUsed: List<String> = emptyList(),
    val agentUsed: List<String> = emptyList(),
    val networkUsed: Boolean = false,
    val executionMode: PlaygroundMode = PlaygroundMode.GENERAL,
    val status: String = "SUCCESS",
    val isStreaming: Boolean = false,
    val explanation: ExplanationRecord? = null
)

data class PlaygroundSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val mode: PlaygroundMode = PlaygroundMode.GENERAL,
    val modelId: String = "gemma-2b-it-litert",
    val messages: List<PlaygroundMessage> = emptyList(),
    val documentReferences: List<String> = emptyList(),
    val memoryReferences: List<String> = emptyList(),
    val agentReferences: List<String> = emptyList(),
    val tags: List<String> = emptyList()
)

data class PlaygroundContextState(
    val activeMode: PlaygroundMode = PlaygroundMode.GENERAL,
    val activeModelName: String = "Gemma 2B Local",
    val isMemoryActive: Boolean = true,
    val isRagActive: Boolean = true,
    val activeSourcesCount: Int = 0,
    val toolsReadyCount: Int = 8,
    val isNetworkOffline: Boolean = true,
    val latencyLastMs: Long = 0L,
    val executionBackend: ExecutionBackend = ExecutionBackend.GPU
)

enum class AttachmentStatus {
    PROCESSING, INDEXED, READY, FAILED
}

data class PlaygroundAttachment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val status: AttachmentStatus = AttachmentStatus.PROCESSING,
    val progress: Float = 0f,
    val extractedChunks: Int = 0,
    val errorMessage: String? = null
)
