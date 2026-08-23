package com.example.edgeaicore.core.server

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.privateServerDataStore by preferencesDataStore(name = "edge_ai_private_server_prefs")

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class AuthenticationState {
    UNAUTHENTICATED,
    AUTHENTICATED,
    EXPIRED,
    FAILED
}

/**
 * Secure configuration abstraction for Private AI Server Gateway.
 */
data class PrivateServerConfig(
    val enabled: Boolean = false,
    val baseUrl: String = "https://gateway.private-ai.lan:8443",
    val authenticationState: AuthenticationState = AuthenticationState.UNAUTHENTICATED,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val availableModels: List<String> = listOf("Llama-3.1-8B-Instruct-Private", "Mistral-7B-Instruct-Private"),
    val hostedMcpServers: List<String> = listOf("PrivateMemory", "PrivateKnowledge", "PrivateAnalytics"),
    val lastHealthCheck: Long = 0L,
    val activeGpuCluster: String = "NVIDIA RTX 4090 / L40S Cluster"
)

class PrivateServerConfigManager(private val context: Context) {
    private val KEY_ENABLED = booleanPreferencesKey("ps_enabled")
    private val KEY_BASE_URL = stringPreferencesKey("ps_base_url")

    val configFlow: Flow<PrivateServerConfig> = context.privateServerDataStore.data.map { prefs ->
        PrivateServerConfig(
            enabled = prefs[KEY_ENABLED] ?: false,
            baseUrl = prefs[KEY_BASE_URL] ?: "https://gateway.private-ai.lan:8443"
        )
    }

    suspend fun updateConfig(enabled: Boolean, baseUrl: String) {
        context.privateServerDataStore.edit { prefs ->
            prefs[KEY_ENABLED] = enabled
            prefs[KEY_BASE_URL] = baseUrl
        }
    }
}
