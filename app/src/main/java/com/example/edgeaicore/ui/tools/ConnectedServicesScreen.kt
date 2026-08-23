package com.example.edgeaicore.ui.tools

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.ui.common.AppCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class ServiceConnectionItem(
    val id: String,
    val name: String,
    val tier: String, // "Local", "Private", "Connected"
    val status: String,
    val isConnected: Boolean,
    val permissions: String,
    val toolsAvailableCount: Int,
    val lastUsed: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedServicesScreen(
    edgeAI: EdgeAICore,
    onBack: () -> Unit,
    onOpenDeveloperModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val connectedMcpServers by edgeAI.mcp.connectedServers.collectAsStateWithLifecycle()
    var showAdvancedDetails by remember { mutableStateOf(false) }

    val services = remember(connectedMcpServers) {
        listOf(
            ServiceConnectionItem(
                id = "local_engine",
                name = "Local On-Device Engine",
                tier = "Local",
                status = "Active (Sovereign)",
                isConnected = true,
                permissions = "Camera, Encrypted Storage, SQLite Vault",
                toolsAvailableCount = 7,
                lastUsed = "Just now"
            ),
            ServiceConnectionItem(
                id = "private_gateway",
                name = "Private Server AI Gateway",
                tier = "Private",
                status = "Connected (mTLS Encrypted)",
                isConnected = true,
                permissions = "Private LLM Relay, Knowledge Indexing",
                toolsAvailableCount = 4,
                lastUsed = "10 mins ago"
            ),
            ServiceConnectionItem(
                id = "mcp_workspace",
                name = "Workspace MCP Host",
                tier = "Connected",
                status = "Connected",
                isConnected = true,
                permissions = "Calendar, Task Engine, Device Telemetry",
                toolsAvailableCount = connectedMcpServers.size.coerceAtLeast(3),
                lastUsed = "1 hour ago"
            )
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Connected Services", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenDeveloperModal) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = "Developer Inspector")
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
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "External and on-device connectors providing tool endpoints to the Agent runtime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(services, key = { it.id }) { service ->
                AppCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = if (service.isConnected) LocalAIGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = service.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (service.isConnected) LocalAIGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = service.tier.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (service.isConnected) LocalAIGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Status: ${service.status}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Permissions: ${service.permissions}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${service.toolsAvailableCount} tools active • Last used: ${service.lastUsed}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* Manage access */ },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Manage Access", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { /* Connect/Disconnect */ },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            colors = if (service.isConnected) ButtonDefaults.filledTonalButtonColors() else ButtonDefaults.buttonColors()
                        ) {
                            Text(if (service.isConnected) "Disconnect" else "Connect", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Advanced Inspector Card Trigger
            item {
                AppCard(
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    onClick = onOpenDeveloperModal
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Advanced Subsystem Diagnostics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = "View latency, transport, and raw MCP endpoints", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}
