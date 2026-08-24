package com.example.edgeaicore.ui.privacy

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.ui.common.AppCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyCenterScreen(
    edgeAI: EdgeAICore,
    onBack: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val privacyState by edgeAI.privacy.state.collectAsStateWithLifecycle()
    val auditLogs by edgeAI.privacy.auditLogs.collectAsStateWithLifecycle()
    var exportResultText by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Safety Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("privacy_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToStorage,
                        modifier = Modifier.testTag("privacy_storage_shortcut_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Storage, contentDescription = "Storage Vault")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. GUARANTEE BANNER
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (privacyState.localVaultLocked) MaterialTheme.colorScheme.primaryContainer else LocalAIGreen.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (privacyState.localVaultLocked) MaterialTheme.colorScheme.primary else LocalAIGreen.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("privacy_guarantee_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (privacyState.localVaultLocked) Icons.Default.Lock else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (privacyState.localVaultLocked) MaterialTheme.colorScheme.primary else LocalAIGreen,
                            modifier = Modifier.size(30.dp)
                        )
                        Column {
                            Text(
                                text = if (privacyState.localVaultLocked) "Air-Gapped Vault Quarantine Locked" else "Zero Cloud Leakage Guarantee Active",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (privacyState.localVaultLocked) MaterialTheme.colorScheme.primary else LocalAIGreen
                            )
                            Text(
                                text = "All prompts, embeddings, perception frames, and personal records remain strictly on-device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 2. GRANULAR PRIVACY TOGGLES
            item {
                PrivacyTogglesSection(
                    offlineOnlyMode = privacyState.offlineOnlyMode,
                    dataSharingEnabled = privacyState.dataSharingEnabled,
                    cloudAiEnabled = privacyState.cloudAiEnabled,
                    remoteSyncEnabled = privacyState.remoteSyncEnabled,
                    localVaultLocked = privacyState.localVaultLocked,
                    privateServerEnabled = privacyState.privateServerEnabled,
                    onToggleOfflineOnly = { offline ->
                        coroutineScope.launch { edgeAI.privacy.setOfflineOnlyMode(offline) }
                    },
                    onToggleDataSharing = { allowed ->
                        coroutineScope.launch { edgeAI.privacy.setDataSharingAllowed(allowed) }
                    },
                    onToggleCloudAi = { allowed ->
                        coroutineScope.launch { edgeAI.privacy.setCloudAllowed(allowed) }
                    },
                    onToggleRemoteSync = { allowed ->
                        coroutineScope.launch { edgeAI.privacy.setRemoteSyncAllowed(allowed) }
                    },
                    onToggleVaultLock = { locked ->
                        coroutineScope.launch { edgeAI.privacy.setLocalVaultLocked(locked) }
                    },
                    onTogglePrivateServer = { allowed ->
                        coroutineScope.launch { edgeAI.privacy.setPrivateServerAllowed(allowed) }
                    }
                )
            }

            // 3. DATA SOVEREIGNTY & EXPORT CONTROLS
            item {
                AppCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("privacy_sovereignty_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "DATA SOVEREIGNTY & EXPORT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "You own 100% of your data. Export your entire memory vault to open JSON format or execute a verifiable zero-trace wipe.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = edgeAI.export.create()
                                        exportResultText = when (res) {
                                            is com.example.edgeaicore.core.common.EdgeResult.Success -> "Export JSON Created (${res.data.length} bytes encrypted payload)"
                                            is com.example.edgeaicore.core.common.EdgeResult.Failure -> "Export Failed: ${res.error.message}"
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_data_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Data", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { showClearConfirm = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("clear_all_data_btn")
                            ) {
                                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Purge Vault", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 4. TRANSPARENCY & AUDIT LOGS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRANSPARENCY & AUDIT LOGS (${auditLogs.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        if (auditLogs.isNotEmpty()) {
                            TextButton(
                                onClick = { edgeAI.privacy.clearLogs() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Clear Logs", fontSize = 11.sp)
                            }
                        }
                    }

                    if (auditLogs.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, tint = LocalAIGreen, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "All inferences strictly on-device. Zero unverified transmissions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        auditLogs.take(8).forEach { log ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = log.targetProvider.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(
                                            text = if (!log.passedVerification) "BLOCKED" else if (log.wasTransmittedRemotely) "REMOTE" else "LOCAL ONLY",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (!log.passedVerification) MaterialTheme.colorScheme.error else LocalAIGreen
                                        )
                                    }
                                    Text(text = "Privacy Level: ${log.declaredPrivacyLevel.name} • Task: ${log.taskType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = log.dataSummary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (exportResultText != null) {
        AlertDialog(
            onDismissRequest = { exportResultText = null },
            title = { Text("Data Export", fontWeight = FontWeight.Bold) },
            text = { Text(exportResultText.orEmpty()) },
            confirmButton = { Button(onClick = { exportResultText = null }) { Text("OK") } }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Permanently Purge Vault?", fontWeight = FontWeight.Bold) },
            text = { Text("This will wipe all memories, local vector embeddings, and cached responses.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            edgeAI.memory.clear()
                            edgeAI.ai.clearCache()
                            showClearConfirm = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Purge Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Reusable Privacy Toggles Component for granular security controls.
 */
@Composable
fun PrivacyTogglesSection(
    offlineOnlyMode: Boolean,
    dataSharingEnabled: Boolean,
    cloudAiEnabled: Boolean,
    remoteSyncEnabled: Boolean,
    localVaultLocked: Boolean,
    privateServerEnabled: Boolean,
    onToggleOfflineOnly: (Boolean) -> Unit,
    onToggleDataSharing: (Boolean) -> Unit,
    onToggleCloudAi: (Boolean) -> Unit,
    onToggleRemoteSync: (Boolean) -> Unit,
    onToggleVaultLock: (Boolean) -> Unit,
    onTogglePrivateServer: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("privacy_toggles_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "GRANULAR PRIVACY & ROUTING CONTROLS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            // 0. Secure Offline-Only Mode
            PrivacyToggleRow(
                icon = Icons.Default.Shield,
                iconTint = if (offlineOnlyMode) LocalAIGreen else MaterialTheme.colorScheme.primary,
                title = "Secure 'Offline-Only' Mode",
                subtitle = "Strictly disables all cloud API requests and private gateway routing, enforcing 100% on-device LLM computation for peak data privacy.",
                isChecked = offlineOnlyMode,
                onCheckedChange = onToggleOfflineOnly,
                testTag = "switch_offline_only_mode"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 1. Air-Gapped Quarantine Lock
            PrivacyToggleRow(
                icon = Icons.Default.Lock,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Air-Gapped Vault Quarantine",
                subtitle = "Force all subsystems into 100% offline local mode. Blocks all outbound socket connections.",
                isChecked = localVaultLocked,
                onCheckedChange = onToggleVaultLock,
                testTag = "switch_vault_quarantine"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 2. Data Sharing & Telemetry Toggle
            PrivacyToggleRow(
                icon = Icons.Default.ShareLocation,
                iconTint = if (dataSharingEnabled) MaterialTheme.colorScheme.error else LocalAIGreen,
                title = "Data Sharing & Telemetry",
                subtitle = "Send anonymous usage diagnostics or error telemetry to external analytics servers. (Default: OFF)",
                isChecked = dataSharingEnabled,
                onCheckedChange = onToggleDataSharing,
                testTag = "switch_data_sharing"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 3. Cloud AI Offloading Toggle
            PrivacyToggleRow(
                icon = Icons.Default.CloudQueue,
                iconTint = CloudAIBorder,
                title = "Cloud AI Offloading",
                subtitle = "Allow selective offloading of complex multi-step reasoning to external cloud AI models when on-device LLM reaches capacity limits.",
                isChecked = cloudAiEnabled,
                onCheckedChange = onToggleCloudAi,
                testTag = "switch_cloud_ai"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 4. Remote Syncing Toggle
            PrivacyToggleRow(
                icon = Icons.Default.Sync,
                iconTint = PrivateServerAmber,
                title = "Remote Cloud Syncing",
                subtitle = "Automatically synchronize encrypted memory snapshots across secondary devices and cloud storage.",
                isChecked = remoteSyncEnabled,
                onCheckedChange = onToggleRemoteSync,
                testTag = "switch_remote_sync"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 5. Private Home Server AI Toggle
            PrivacyToggleRow(
                icon = Icons.Default.Dns,
                iconTint = PrivateServerAmber,
                title = "Private Self-Hosted AI Server",
                subtitle = "Route heavy reasoning prompts to your personal home/office GPU node over encrypted Tailscale/Wireguard tunnel.",
                isChecked = privateServerEnabled,
                onCheckedChange = onTogglePrivateServer,
                testTag = "switch_private_server"
            )
        }
    }
}

@Composable
private fun PrivacyToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(32.dp).padding(top = 2.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
