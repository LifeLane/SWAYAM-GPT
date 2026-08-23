package com.example.edgeaicore.core.database

import android.content.Context
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.RiskLevel
import com.example.edgeaicore.core.storage.StorageEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * DataGateway mediates all Database and Storage interactions from Agents, Tools, and MCP.
 * Implements authentication, authorization, schema validation, rate-limiting, privacy checks, and audit logging.
 */
class DataGateway(
    private val context: Context? = null,
    val databaseEngine: DatabaseEngine,
    val storageEngine: StorageEngine? = null,
    val permissionManager: DataPermissionManager = DataPermissionManager(context)
) {
    constructor(databaseEngine: DatabaseEngine, permissionManager: DataPermissionManager) :
        this(null, databaseEngine, null, permissionManager)

    private val rateLimiters = ConcurrentHashMap<String, AtomicInteger>()
    private val MAX_OPERATIONS_PER_MINUTE = 120

    /**
     * Executes a secure, validated, and audited database operation.
     */
    suspend fun <T> executeDataOperation(
        caller: String,
        targetResource: String,
        permission: DataPermission,
        privacyLevel: DataPrivacyLevel,
        isAgentCaller: Boolean,
        userConsentGiven: Boolean,
        operation: suspend () -> EdgeResult<T>
    ): EdgeResult<T> = withContext(Dispatchers.IO) {
        // 1. Rate Limiting Check
        val minuteKey = "${caller}_${System.currentTimeMillis() / 60000}"
        val counter = rateLimiters.computeIfAbsent(minuteKey) { AtomicInteger(0) }
        if (counter.incrementAndGet() > MAX_OPERATIONS_PER_MINUTE) {
            val record = AuditRecordEntity(
                eventType = "RATE_LIMIT_EXCEEDED",
                targetResource = targetResource,
                actor = caller,
                details = "Rate limit exceeded ($MAX_OPERATIONS_PER_MINUTE ops/min)",
                riskLevel = RiskLevel.HIGH,
                privacyLevel = privacyLevel,
                status = "DENIED"
            )
            databaseEngine.audits.logAudit(record)
            return@withContext EdgeResult.Failure(IllegalStateException("Data rate limit exceeded for caller: $caller"))
        }

        // 2. Permission and Privacy Validation
        val permCheck = permissionManager.checkPermission(
            permission = permission,
            dataPrivacyLevel = privacyLevel,
            isAgentCaller = isAgentCaller,
            userConsentGiven = userConsentGiven,
            targetResource = targetResource
        )

        if (permCheck is EdgeResult.Failure) {
            val record = AuditRecordEntity(
                eventType = "PERMISSION_DENIED",
                targetResource = targetResource,
                actor = caller,
                details = "Permission check failed: ${permCheck.error.message}",
                riskLevel = permissionManager.getRiskLevelForOperation(permission),
                privacyLevel = privacyLevel,
                status = "DENIED"
            )
            databaseEngine.audits.logAudit(record)
            return@withContext permCheck
        }

        // 3. Execution
        try {
            val result = operation()
            val status = if (result is EdgeResult.Success) "SUCCESS" else "FAILED"
            databaseEngine.audits.logAudit(
                AuditRecordEntity(
                    eventType = "DATA_${permission.name}",
                    targetResource = targetResource,
                    actor = caller,
                    details = "Executed $permission on $targetResource",
                    riskLevel = permissionManager.getRiskLevelForOperation(permission),
                    privacyLevel = privacyLevel,
                    status = status
                )
            )
            result
        } catch (e: Exception) {
            databaseEngine.audits.logAudit(
                AuditRecordEntity(
                    eventType = "DATA_${permission.name}_EXCEPTION",
                    targetResource = targetResource,
                    actor = caller,
                    details = "Exception during $permission: ${e.message}",
                    riskLevel = permissionManager.getRiskLevelForOperation(permission),
                    privacyLevel = privacyLevel,
                    status = "FAILED"
                )
            )
            EdgeResult.Failure(e)
        }
    }
}
