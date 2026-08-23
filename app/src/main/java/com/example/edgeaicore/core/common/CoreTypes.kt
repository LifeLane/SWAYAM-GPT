package com.example.edgeaicore.core.common

/**
 * Standardized privacy levels for data and AI operations in EdgeAI Core.
 */
enum class PrivacyLevel {
    /** Strictly local - never leaves the device under any circumstance. */
    LOCAL_ONLY,
    /** Sensitive user data - local by default, requires explicit confirmation for private server. */
    SENSITIVE,
    /** Private application data - local or encrypted private server depending on settings. */
    PRIVATE,
    /** Public / non-sensitive data - cloud fallback permitted if user consented. */
    PUBLIC
}

/**
 * AI Provider identification.
 */
enum class AIProviderType {
    LOCAL,
    PRIVATE_SERVER,
    CLOUD,
    DEMO
}

/**
 * Hardware execution backends for on-device inference.
 */
enum class ExecutionBackend {
    CPU,
    GPU,
    NPU,
    AUTO
}

/**
 * AI Task category definitions.
 */
enum class TaskType {
    TEXT_GENERATION,
    CHAT,
    VISION_PERCEPTION,
    OBJECT_DETECTION,
    POSE_ESTIMATION,
    HAND_TRACKING,
    FACE_LANDMARKS,
    IMAGE_CLASSIFICATION,
    EMBEDDING,
    SUMMARIZATION,
    AGENT_REASONING,
    MEMORY_SEARCH
}

/**
 * Risk classification for Agent and Tool actions.
 */
enum class RiskLevel {
    /** Zero side-effects, purely passive or read-only query. */
    NONE,
    /** Read-only or trivial reversible actions (e.g. search, local query). */
    LOW,
    /** Minor state changes (e.g. saving note, creating non-urgent reminder). */
    MEDIUM,
    /** Consequential actions (calendar event, message transmission, memory deletion). */
    HIGH,
    /** Irreversible or system-level actions (device setting changes, data purge). */
    CRITICAL
}

/**
 * Standardized error hierarchy for EdgeAI Core subsystems.
 */
sealed class EdgeAIError(val code: String, message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ModelUnavailable(val modelId: String) : 
        EdgeAIError("ERR_MODEL_UNAVAILABLE", "Requested model '$modelId' is not installed or available.")
        
    class InsufficientMemory(val requiredMb: Long, val availableMb: Long) : 
        EdgeAIError("ERR_INSUFFICIENT_MEMORY", "Insufficient RAM for operation: required ${requiredMb}MB, available ${availableMb}MB.")
        
    class InferenceTimeout(val timeoutMs: Long) : 
        EdgeAIError("ERR_INFERENCE_TIMEOUT", "Inference timed out after ${timeoutMs}ms.")
        
    class PermissionDenied(val permission: String) : 
        EdgeAIError("ERR_PERMISSION_DENIED", "Required permission '$permission' was denied.")
        
    class CameraUnavailable(val reason: String = "Camera hardware unavailable") : 
        EdgeAIError("ERR_CAMERA_UNAVAILABLE", reason)
        
    class Offline(val detail: String = "Device is currently offline") : 
        EdgeAIError("ERR_OFFLINE", detail)
        
    class PrivateServerUnavailable(val endpoint: String, val statusCode: Int? = null) : 
        EdgeAIError("ERR_PRIVATE_SERVER_UNAVAILABLE", "Private AI server unreachable at $endpoint (status: $statusCode)")
        
    class CloudUnavailable(val providerName: String, val detail: String) : 
        EdgeAIError("ERR_CLOUD_UNAVAILABLE", "Cloud provider '$providerName' unavailable: $detail")
        
    class InvalidResponse(val detail: String) : 
        EdgeAIError("ERR_INVALID_RESPONSE", "Invalid response from AI provider: $detail")
        
    class StorageFull(val requiredBytes: Long, val availableBytes: Long) : 
        EdgeAIError("ERR_STORAGE_FULL", "Storage space full: required $requiredBytes bytes, available $availableBytes bytes.")
        
    class PrivacyViolation(val reason: String) : 
        EdgeAIError("ERR_PRIVACY_VIOLATION", "Privacy rule violation: $reason")

    class PolicyViolation(val reason: String) :
        EdgeAIError("ERR_POLICY_VIOLATION", "Policy engine rejection: $reason")

    class ToolExecutionError(val toolId: String, val detail: String) :
        EdgeAIError("ERR_TOOL_EXECUTION", "Execution failure in tool '$toolId': $detail")

    class ToolConfirmationRequired(val toolId: String, val proposalId: String) :
        EdgeAIError("ERR_CONFIRMATION_REQUIRED", "Tool '$toolId' requires explicit user confirmation (proposal: $proposalId)")

    class McpProtocolError(val serverId: String, val detail: String) :
        EdgeAIError("ERR_MCP_PROTOCOL", "MCP error on '$serverId': $detail")

    class McpSecurityViolation(val serverId: String, val reason: String) :
        EdgeAIError("ERR_MCP_SECURITY", "MCP security violation on '$serverId': $reason")

    class RateLimitExceeded(val target: String, val limit: Int) :
        EdgeAIError("ERR_RATE_LIMIT", "Rate limit of $limit exceeded for $target")

    class AgentBudgetExceeded(val reason: String) :
        EdgeAIError("ERR_AGENT_BUDGET_EXCEEDED", "Agent budget constraint exceeded: $reason")
        
    class DatabaseError(val detail: String, cause: Throwable? = null) : 
        EdgeAIError("ERR_DATABASE", detail, cause)
        
    class StorageError(val detail: String, cause: Throwable? = null) : 
        EdgeAIError("ERR_STORAGE", detail, cause)
        
    class SecurityViolation(val reason: String, cause: Throwable? = null) : 
        EdgeAIError("ERR_SECURITY_VIOLATION", reason, cause)
        
    class IngestionError(val detail: String, cause: Throwable? = null) : 
        EdgeAIError("ERR_INGESTION", detail, cause)
        
    class SyncError(val detail: String, cause: Throwable? = null) : 
        EdgeAIError("ERR_SYNC", detail, cause)
        
    class ActionRejected(val reason: String) : 
        EdgeAIError("ERR_ACTION_REJECTED", reason)
        
    class Unknown(detail: String, cause: Throwable? = null) : 
        EdgeAIError("ERR_UNKNOWN", detail, cause)
}

/**
 * Standard Result wrapper for EdgeAI operations.
 */
sealed class EdgeResult<out T> {
    data class Success<out T>(val data: T) : EdgeResult<T>()
    data class Failure(val error: EdgeAIError) : EdgeResult<Nothing>() {
        constructor(throwable: Throwable) : this(
            if (throwable is EdgeAIError) throwable 
            else EdgeAIError.Unknown(throwable.message ?: throwable.javaClass.simpleName, throwable)
        )
        constructor(message: String) : this(EdgeAIError.Unknown(message))
    }
    
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw error
    }
}

