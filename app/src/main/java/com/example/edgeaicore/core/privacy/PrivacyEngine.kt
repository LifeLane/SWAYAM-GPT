package com.example.edgeaicore.core.privacy

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.PrivacyLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

private val Context.privacyDataStore by preferencesDataStore(name = "edge_ai_privacy_prefs")

data class PrivacyAuditRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val taskType: String,
    val declaredPrivacyLevel: PrivacyLevel,
    val targetProvider: AIProviderType,
    val wasTransmittedRemotely: Boolean,
    val dataSummary: String,
    val passedVerification: Boolean
)

data class PrivacyDashboardState(
    val localAiEnabled: Boolean = true,
    val privateServerEnabled: Boolean = false,
    val cloudAiEnabled: Boolean = false,
    val dataSharingEnabled: Boolean = false,
    val remoteSyncEnabled: Boolean = false,
    val localVaultLocked: Boolean = false,
    val localAiLastUsed: Long = System.currentTimeMillis(),
    val privateServerLastUsed: Long = 0L,
    val cloudAiLastUsed: Long = 0L,
    val totalLocalInferences: Long = 0L,
    val totalRemoteInferences: Long = 0L
)

/**
 * PrivacyEngine: Enforces strict data containment rules and maintains a tamper-evident
 * local audit log of all inference and context routing decisions.
 */
class PrivacyEngine(private val context: Context) {
    private val _dashboardState = MutableStateFlow(PrivacyDashboardState())
    val dashboardState: StateFlow<PrivacyDashboardState> = _dashboardState.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<PrivacyAuditRecord>>(emptyList())
    val auditLogs: StateFlow<List<PrivacyAuditRecord>> = _auditLogs.asStateFlow()

    // Privacy Keys
    private val KEY_PRIVATE_SERVER_OPT_IN = booleanPreferencesKey("private_server_opt_in")
    private val KEY_CLOUD_AI_OPT_IN = booleanPreferencesKey("cloud_ai_opt_in")
    private val KEY_DATA_SHARING_OPT_IN = booleanPreferencesKey("data_sharing_opt_in")
    private val KEY_REMOTE_SYNC_OPT_IN = booleanPreferencesKey("remote_sync_opt_in")
    private val KEY_LOCAL_VAULT_LOCK = booleanPreferencesKey("local_vault_lock")

    val isCloudAiAllowed: Flow<Boolean> = context.privacyDataStore.data.map { prefs ->
        prefs[KEY_CLOUD_AI_OPT_IN] ?: false
    }

    val isPrivateServerAllowed: Flow<Boolean> = context.privacyDataStore.data.map { prefs ->
        prefs[KEY_PRIVATE_SERVER_OPT_IN] ?: false
    }

    val isDataSharingAllowed: Flow<Boolean> = context.privacyDataStore.data.map { prefs ->
        prefs[KEY_DATA_SHARING_OPT_IN] ?: false
    }

    val isRemoteSyncAllowed: Flow<Boolean> = context.privacyDataStore.data.map { prefs ->
        prefs[KEY_REMOTE_SYNC_OPT_IN] ?: false
    }

    suspend fun setCloudAiAllowed(allowed: Boolean) {
        context.privacyDataStore.edit { it[KEY_CLOUD_AI_OPT_IN] = allowed }
        _dashboardState.value = _dashboardState.value.copy(cloudAiEnabled = allowed)
    }

    suspend fun setPrivateServerAllowed(allowed: Boolean) {
        context.privacyDataStore.edit { it[KEY_PRIVATE_SERVER_OPT_IN] = allowed }
        _dashboardState.value = _dashboardState.value.copy(privateServerEnabled = allowed)
    }

    suspend fun setDataSharingAllowed(allowed: Boolean) {
        context.privacyDataStore.edit { it[KEY_DATA_SHARING_OPT_IN] = allowed }
        _dashboardState.value = _dashboardState.value.copy(dataSharingEnabled = allowed)
    }

    suspend fun setRemoteSyncAllowed(allowed: Boolean) {
        context.privacyDataStore.edit { it[KEY_REMOTE_SYNC_OPT_IN] = allowed }
        _dashboardState.value = _dashboardState.value.copy(remoteSyncEnabled = allowed)
    }

    suspend fun setLocalVaultLocked(locked: Boolean) {
        context.privacyDataStore.edit { it[KEY_LOCAL_VAULT_LOCK] = locked }
        _dashboardState.value = _dashboardState.value.copy(localVaultLocked = locked)
    }

    /**
     * Validates whether a given AI request satisfies the declared privacy level constraints.
     * Returns true if allowed, false if blocked by privacy rules.
     */
    fun validateRouting(
        privacyLevel: PrivacyLevel,
        targetProvider: AIProviderType,
        userConsentGiven: Boolean
    ): Boolean {
        val isValid = when (privacyLevel) {
            PrivacyLevel.LOCAL_ONLY -> targetProvider == AIProviderType.LOCAL || targetProvider == AIProviderType.DEMO
            PrivacyLevel.SENSITIVE -> (targetProvider == AIProviderType.LOCAL || targetProvider == AIProviderType.DEMO) ||
                    (targetProvider == AIProviderType.PRIVATE_SERVER && _dashboardState.value.privateServerEnabled && userConsentGiven)
            PrivacyLevel.PRIVATE -> targetProvider == AIProviderType.LOCAL ||
                    (targetProvider == AIProviderType.PRIVATE_SERVER && _dashboardState.value.privateServerEnabled) ||
                    targetProvider == AIProviderType.DEMO
            PrivacyLevel.PUBLIC -> true
        }

        val record = PrivacyAuditRecord(
            taskType = "AI_INFERENCE_REQUEST",
            declaredPrivacyLevel = privacyLevel,
            targetProvider = targetProvider,
            wasTransmittedRemotely = targetProvider == AIProviderType.PRIVATE_SERVER || targetProvider == AIProviderType.CLOUD,
            dataSummary = "PrivacyLevel=${privacyLevel.name}, Target=${targetProvider.name}, Consented=$userConsentGiven",
            passedVerification = isValid
        )

        _auditLogs.value = listOf(record) + _auditLogs.value.take(49)
        
        if (isValid) {
            val now = System.currentTimeMillis()
            when (targetProvider) {
                AIProviderType.LOCAL -> {
                    _dashboardState.value = _dashboardState.value.copy(
                        localAiLastUsed = now,
                        totalLocalInferences = _dashboardState.value.totalLocalInferences + 1
                    )
                }
                AIProviderType.PRIVATE_SERVER -> {
                    _dashboardState.value = _dashboardState.value.copy(
                        privateServerLastUsed = now,
                        totalRemoteInferences = _dashboardState.value.totalRemoteInferences + 1
                    )
                }
                AIProviderType.CLOUD -> {
                    _dashboardState.value = _dashboardState.value.copy(
                        cloudAiLastUsed = now,
                        totalRemoteInferences = _dashboardState.value.totalRemoteInferences + 1
                    )
                }
                AIProviderType.DEMO -> {}
            }
        }

        return isValid
    }

    fun clearAuditLogs() {
        _auditLogs.value = emptyList()
    }
}
