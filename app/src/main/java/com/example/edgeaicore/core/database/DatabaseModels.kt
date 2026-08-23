package com.example.edgeaicore.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.RiskLevel

/**
 * Data Lifecycle States across EdgeAI Core storage.
 */
enum class DataLifecycleState {
    CREATED,
    ACTIVE,
    ARCHIVED,
    DELETED,
    PURGED
}

/**
 * Data Privacy Levels for all stored objects.
 */
enum class DataPrivacyLevel {
    LOCAL_ONLY,
    SENSITIVE,
    PRIVATE,
    PUBLIC;

    fun toCorePrivacyLevel(): PrivacyLevel = when (this) {
        LOCAL_ONLY -> PrivacyLevel.LOCAL_ONLY
        SENSITIVE -> PrivacyLevel.SENSITIVE
        PRIVATE -> PrivacyLevel.PRIVATE
        PUBLIC -> PrivacyLevel.PUBLIC
    }
}

// ---------------------------------------------------------------------------
// Core Database Entities
// ---------------------------------------------------------------------------

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String? = null,
    val role: String = "owner",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isLocalOnly: Boolean = true,
    val lifecycleState: DataLifecycleState = DataLifecycleState.ACTIVE
)

@Entity(tableName = "user_profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val displayName: String,
    val avatarPath: String? = null,
    val bio: String? = null,
    val preferredAiMode: String = "LOCAL",
    val privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String,
    val category: String = "general",
    val isEncrypted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH, URGENT
    val dueDate: Long? = null,
    val category: String = "General",
    val tags: String = "",
    val privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lifecycleState: DataLifecycleState = DataLifecycleState.ACTIVE
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val startTime: Long,
    val endTime: Long,
    val location: String? = null,
    val isAllDay: Boolean = false,
    val reminderMinutes: Int = 15,
    val privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY,
    val createdAt: Long = System.currentTimeMillis(),
    val lifecycleState: DataLifecycleState = DataLifecycleState.ACTIVE
)

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String,
    val details: String,
    val sourceModule: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val success: Boolean = true
)

@Entity(tableName = "ai_interactions")
data class AIInteractionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String? = null,
    val prompt: String,
    val response: String,
    val provider: String, // LOCAL_LITERT, PRIVATE_SERVER, CLOUD
    val modelName: String,
    val latencyMs: Long,
    val tokensUsed: Int,
    val privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY,
    val timestamp: Long = System.currentTimeMillis(),
    val lifecycleState: DataLifecycleState = DataLifecycleState.ACTIVE
)

@Entity(tableName = "agent_sessions")
data class AgentSessionEntity(
    @PrimaryKey val sessionId: String,
    val profileId: String,
    val userGoal: String,
    val status: String, // RUNNING, COMPLETED, FAILED, CANCELLED
    val stepsJson: String,
    val totalSteps: Int,
    val tokensUsed: Int,
    val latencyMs: Long,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null
)

@Entity(tableName = "tool_calls")
data class ToolCallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String? = null,
    val toolId: String,
    val toolName: String,
    val argumentsJson: String,
    val resultJson: String,
    val riskLevel: RiskLevel,
    val executionSuccess: Boolean,
    val executionTimeMs: Long,
    val userConsented: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "mcp_servers")
data class McpServerEntity(
    @PrimaryKey val serverId: String,
    val name: String,
    val transportType: String, // IN_MEMORY, HTTP_SSE, WEBSOCKET, STDIO
    val endpointUrl: String? = null,
    val trustLevel: String = "USER_APPROVED_REMOTE",
    val isEnabled: Boolean = true,
    val toolCount: Int = 0,
    val lastPingMs: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "mcp_tools")
data class McpToolEntity(
    @PrimaryKey val toolId: String,
    val serverId: String,
    val name: String,
    val description: String,
    val inputSchemaJson: String,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val isEnabled: Boolean = true
)

@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val triggerType: String,
    val triggerConfigJson: String,
    val toolId: String,
    val toolArgumentsJson: String,
    val requiresConfirmation: Boolean = true,
    val isEnabled: Boolean = true,
    val lastTriggeredAt: Long? = null,
    val triggerCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val category: String = "system",
    val isRead: Boolean = false,
    val actionUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "model_metadata")
data class ModelMetadataEntity(
    @PrimaryKey val modelId: String,
    val name: String,
    val version: String,
    val format: String, // TFLITE, ONNX, GGUF, LITERT
    val sizeBytes: Long,
    val localFilePath: String? = null,
    val isInstalled: Boolean = false,
    val recommendedBackend: String = "GPU",
    val quantization: String = "INT4",
    val checksumSha256: String? = null,
    val downloadedAt: Long? = null
)

@Entity(tableName = "usage_metrics")
data class UsageMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metricKey: String,
    val category: String, // INFERENCE, STORAGE, MCP, TOOLS
    val countValue: Long = 1L,
    val durationMs: Long = 0L,
    val dateString: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_records")
data class AuditRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val targetResource: String,
    val actor: String = "system", // USER, AGENT, MCP, BACKGROUND
    val details: String,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY,
    val status: String = "SUCCESS", // SUCCESS, DENIED, FAILED, CANCELLED
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val tier: String = "PRO_ON_DEVICE",
    val isActive: Boolean = true,
    val expiresAt: Long? = null,
    val featuresJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "media_metadata")
data class MediaMetadataEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val mediaType: String, // IMAGE, AUDIO, VIDEO, DOCUMENT
    val mimeType: String,
    val relativePath: String,
    val sizeBytes: Long,
    val checksumSha256: String,
    val thumbnailPath: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY,
    val source: String = "device_camera",
    val createdAt: Long = System.currentTimeMillis(),
    val lifecycleState: DataLifecycleState = DataLifecycleState.ACTIVE
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val fileExtension: String, // pdf, txt, docx, csv, md
    val mimeType: String,
    val relativeStoragePath: String,
    val sizeBytes: Long,
    val checksumSha256: String,
    val extractedTextSummary: String = "",
    val totalPages: Int = 1,
    val totalChunks: Int = 0,
    val isIndexed: Boolean = false,
    val privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lifecycleState: DataLifecycleState = DataLifecycleState.ACTIVE
)

@Entity(tableName = "embeddings")
data class EmbeddingEntity(
    @PrimaryKey val embeddingId: String,
    val sourceId: String,
    val sourceType: String, // MEMORY, DOCUMENT_CHUNK, KNOWLEDGE_ITEM
    val modelId: String = "embedding-gemma-384",
    val dimension: Int = 384,
    val vectorJson: String, // FloatArray JSON serialized
    val contentHash: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "knowledge_items")
data class KnowledgeItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val type: String, // DOCUMENT, NOTE, IMAGE, WEB, MEMORY, STRUCTURED
    val source: String = "USER",
    val sourceId: String? = null,
    val tags: String = "",
    val privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY,
    val embeddingReference: String? = null,
    val checksumSha256: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lifecycleState: DataLifecycleState = DataLifecycleState.ACTIVE
)

@Entity(tableName = "knowledge_chunks")
data class KnowledgeChunkEntity(
    @PrimaryKey val chunkId: String,
    val documentId: String,
    val chunkIndex: Int,
    val text: String,
    val pageNumber: Int = 1,
    val positionStart: Int = 0,
    val positionEnd: Int = 0,
    val embeddingReference: String? = null,
    val metadataJson: String = "{}"
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: String,
    val operation: String, // INSERT, UPDATE, DELETE
    val payloadJson: String,
    val attempts: Int = 0,
    val lastAttemptAt: Long? = null,
    val status: String = "PENDING", // PENDING, IN_PROGRESS, FAILED, CONFLICT
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "agent_logs")
data class AgentLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String? = null,
    val agentName: String = "SWAYAM Agent",
    val level: String = "INFO", // DEBUG, INFO, WARN, ERROR, SUCCESS
    val tag: String = "GENERAL", // THOUGHT, ACTION, TOOL_CALL, PERCEPTION, EMBEDDING, LLM_INFERENCE, MEMORY, REASONING
    val message: String,
    val metadataJson: String? = null,
    val latencyMs: Long = 0L,
    val tokenCount: Int = 0,
    val privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY,
    val timestamp: Long = System.currentTimeMillis()
)

