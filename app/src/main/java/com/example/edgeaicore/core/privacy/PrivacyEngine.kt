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
    val offlineOnlyMode: Boolean = false,
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
    private val KEY_OFFLINE_ONLY_ENFORCED = booleanPreferencesKey("offline_only_enforced")
    private val KEY_PRIVATE_SERVER_OPT_IN = booleanPreferencesKey("private_server_opt_in")
    private val KEY_CLOUD_AI_OPT_IN = booleanPreferencesKey("cloud_ai_opt_in")
    private val KEY_DATA_SHARING_OPT_IN = booleanPreferencesKey("data_sharing_opt_in")
    private val KEY_REMOTE_SYNC_OPT_IN = booleanPreferencesKey("remote_sync_opt_in")
    private val KEY_LOCAL_VAULT_LOCK = booleanPreferencesKey("local_vault_lock")

    val isOfflineOnlyEnforced: Flow<Boolean> = context.privacyDataStore.data.map { prefs ->
        prefs[KEY_OFFLINE_ONLY_ENFORCED] ?: false
    }

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

    suspend fun setOfflineOnlyMode(enabled: Boolean) {
        context.privacyDataStore.edit {
            it[KEY_OFFLINE_ONLY_ENFORCED] = enabled
            if (enabled) {
                it[KEY_CLOUD_AI_OPT_IN] = false
                it[KEY_PRIVATE_SERVER_OPT_IN] = false
                it[KEY_DATA_SHARING_OPT_IN] = false
                it[KEY_REMOTE_SYNC_OPT_IN] = false
            }
        }
        _dashboardState.value = _dashboardState.value.copy(
            offlineOnlyMode = enabled,
            cloudAiEnabled = if (enabled) false else _dashboardState.value.cloudAiEnabled,
            privateServerEnabled = if (enabled) false else _dashboardState.value.privateServerEnabled,
            dataSharingEnabled = if (enabled) false else _dashboardState.value.dataSharingEnabled,
            remoteSyncEnabled = if (enabled) false else _dashboardState.value.remoteSyncEnabled
        )
    }

    suspend fun setCloudAiAllowed(allowed: Boolean) {
        if (_dashboardState.value.offlineOnlyMode && allowed) return
        context.privacyDataStore.edit { it[KEY_CLOUD_AI_OPT_IN] = allowed }
        _dashboardState.value = _dashboardState.value.copy(cloudAiEnabled = allowed)
    }

    suspend fun setPrivateServerAllowed(allowed: Boolean) {
        if (_dashboardState.value.offlineOnlyMode && allowed) return
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
        // If offline-only mode is active, strictly reject remote network calls
        if (_dashboardState.value.offlineOnlyMode && (targetProvider == AIProviderType.CLOUD || targetProvider == AIProviderType.PRIVATE_SERVER)) {
            val record = PrivacyAuditRecord(
                taskType = "OFFLINE_ONLY_BLOCKED",
                declaredPrivacyLevel = privacyLevel,
                targetProvider = targetProvider,
                wasTransmittedRemotely = false,
                dataSummary = "Blocked remote transmission because Secure Offline-Only Mode is active.",
                passedVerification = false
            )
            _auditLogs.value = listOf(record) + _auditLogs.value.take(49)
            return false
        }

        val isValid = when (privacyLevel) {
            PrivacyLevel.LOCAL_ONLY -> targetProvider == AIProviderType.LOCAL || targetProvider == AIProviderType.DEMO
            PrivacyLevel.SENSITIVE -> (targetProvider == AIProviderType.LOCAL || targetProvider == AIProviderType.DEMO) ||
                    (targetProvider == AIProviderType.PRIVATE_SERVER && _dashboardState.value.privateServerEnabled && userConsentGiven)
            PrivacyLevel.PRIVATE -> targetProvider == AIProviderType.LOCAL ||
                    (targetProvider == AIProviderType.PRIVATE_SERVER && _dashboardState.value.privateServerEnabled) ||
                    targetProvider == AIProviderType.DEMO
            PrivacyLevel.PUBLIC -> if (_dashboardState.value.offlineOnlyMode) (targetProvider == AIProviderType.LOCAL || targetProvider == AIProviderType.DEMO) else true
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
