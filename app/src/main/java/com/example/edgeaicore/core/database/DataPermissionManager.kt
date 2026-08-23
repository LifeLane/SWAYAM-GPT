package com.example.edgeaicore.core.database

import android.content.Context
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.RiskLevel

enum class DataPermission {
    READ,
    CREATE,
    UPDATE,
    DELETE,
    EXPORT,
    SHARE,
    SYNC
}

/**
 * DataPermissionManager manages access grants and checks permissions
 * against privacy levels, risk categories, and user consent constraints.
 */
class DataPermissionManager(private val context: Context? = null) {

    /**
     * Checks if a request is authorized based on data privacy level, required permission,
     * caller identity, and explicit user consent.
     */
    fun checkPermission(
        permission: DataPermission,
        dataPrivacyLevel: DataPrivacyLevel,
        isAgentCaller: Boolean,
        userConsentGiven: Boolean,
        targetResource: String
    ): EdgeResult<Unit> {
        // Strict guard: Raw SQL execution and direct low-level SQLite handle access is strictly forbidden for Agent/LLM callers
        if (isAgentCaller && (targetResource.contains("raw_sql") || targetResource.contains("sqlite_master") || targetResource.contains("raw_database"))) {
            return EdgeResult.Failure(
                SecurityException("Direct raw database and arbitrary SQL execution by LLM agents is strictly prohibited.")
            )
        }

        // Strict guard: Agent caller trying to export, purge, or share data requires explicit consent
        if (isAgentCaller && (permission == DataPermission.EXPORT || permission == DataPermission.DELETE || permission == DataPermission.SHARE || permission == DataPermission.SYNC)) {
            if (!userConsentGiven) {
                return EdgeResult.Failure(
                    SecurityException("Data operation $permission on $targetResource requires explicit user consent.")
                )
            }
        }

        // Strict guard: LOCAL_ONLY data cannot be synced or exported externally without user approval
        if (dataPrivacyLevel == DataPrivacyLevel.LOCAL_ONLY && permission == DataPermission.SYNC) {
            return EdgeResult.Failure(
                IllegalStateException("Data marked LOCAL_ONLY cannot be synchronized to remote or private servers.")
            )
        }

        return EdgeResult.Success(Unit)
    }

    /**
     * Returns required risk level for data operations.
     */
    fun getRiskLevelForOperation(permission: DataPermission): RiskLevel {
        return when (permission) {
            DataPermission.READ -> RiskLevel.LOW
            DataPermission.CREATE -> RiskLevel.LOW
            DataPermission.UPDATE -> RiskLevel.MEDIUM
            DataPermission.DELETE -> RiskLevel.HIGH
            DataPermission.EXPORT -> RiskLevel.HIGH
            DataPermission.SHARE -> RiskLevel.HIGH
            DataPermission.SYNC -> RiskLevel.MEDIUM
        }
    }
}
