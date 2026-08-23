package com.example.edgeaicore.ui.tools

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.RiskLevel
import com.example.edgeaicore.core.tools.Tool
import com.example.edgeaicore.core.tools.ToolCategory
import com.example.edgeaicore.ui.common.AppCard
import com.example.edgeaicore.ui.common.GoogleFilterChip
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    edgeAI: EdgeAICore,
    onNavigateToConnectedServices: () -> Unit,
    onNavigateToPlayground: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var registeredTools by remember { mutableStateOf<List<Tool>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<ToolCategory?>(null) }
    
    // Live Execution Tracking State
    var activeExecutionState by remember { mutableStateOf<ToolExecutionState?>(null) }

    LaunchedEffect(Unit) {
        registeredTools = edgeAI.tools.getAll()
    }

    val filteredTools = remember(registeredTools, selectedCategory) {
        if (selectedCategory == null) registeredTools
        else registeredTools.filter { it.category == selectedCategory }
    }

    fun executeToolWithLifecycle(tool: Tool) {
        coroutineScope.launch {
            val startTime = System.currentTimeMillis()
            // 1. INITIATED
            activeExecutionState = ToolExecutionState(
                toolId = tool.id,
                toolName = tool.name,
                stage = ToolExecutionStage.INITIATED,
                serverName = "Local ToolGateway (${tool.provider.name})"
            )
            delay(350)

            // 2. PROCESSING
            activeExecutionState = activeExecutionState?.copy(
                stage = ToolExecutionStage.PROCESSING
            )
            delay(450)

            // 3. EXECUTE
            val result = edgeAI.tools.execute(
                toolId = tool.id,
                arguments = emptyMap(),
                userConsentGiven = true
            )
            val elapsed = System.currentTimeMillis() - startTime

            // 4. OUTCOME
            activeExecutionState = when (result) {
                is EdgeResult.Success -> {
                    activeExecutionState?.copy(
                        stage = ToolExecutionStage.SUCCESS,
                        durationMs = elapsed,
                        outputSnippet = result.data.output.toString()
                    )
                }
                is EdgeResult.Failure -> {
                    activeExecutionState?.copy(
                        stage = ToolExecutionStage.ERROR,
                        durationMs = elapsed,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. HEADER & CONNECTED SERVICES PROMO
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "TOOL ECOSYSTEM & GOVERNANCE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Tools & Capabilities",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "On-device capabilities governed strictly by human consent and ToolGateway policies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. ACTIVE MCP EXECUTION STATUS COMPONENT (If running or completed)
        val execState = activeExecutionState
        if (execState != null) {
            item {
                ExecutionStatus(
                    state = execState,
                    onDismiss = { activeExecutionState = null },
                    onRetry = {
                        val tool = registeredTools.firstOrNull { it.id == execState.toolId }
                        if (tool != null) executeToolWithLifecycle(tool)
                    }
                )
            }
        }

        // 3. CONNECTED SERVICES & PLAYGROUND SHORTCUT CARDS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppCard(
                    onClick = onNavigateToPlayground,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    modifier = Modifier.testTag("tool_playground_shortcut_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                }
                            }
                            Column {
                                Text("Interactive Tool & MCP Playground", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Test JSON-RPC payloads & custom endpoints", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                AppCard(
                    onClick = onNavigateToConnectedServices,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("connected_services_shortcut_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PrivateServerAmber.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.Hub, contentDescription = null, tint = PrivateServerAmber, modifier = Modifier.size(22.dp))
                                }
                            }
                            Column {
                                Text("MCP Servers & Connected Services", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Local loopback & secure remote protocol", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // 4. CATEGORY FILTERS
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    GoogleFilterChip(
                        text = "All (${registeredTools.size})",
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null }
                    )
                }
                items(ToolCategory.values()) { category ->
                    val count = registeredTools.count { it.category == category }
                    if (count > 0) {
                        GoogleFilterChip(
                            text = "${category.name.lowercase().replaceFirstChar { it.uppercase() }} ($count)",
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }
        }

        // 5. TOOLS LIST
        items(filteredTools, key = { it.id }) { tool ->
            val isCurrentExecuting = activeExecutionState?.toolId == tool.id &&
                    (activeExecutionState?.stage == ToolExecutionStage.INITIATED || activeExecutionState?.stage == ToolExecutionStage.PROCESSING)

            ToolItemCard(
                tool = tool,
                isExecuting = isCurrentExecuting,
                onToggleEnabled = { enabled ->
                    edgeAI.tools.setEnabled(tool.id, enabled)
                    registeredTools = edgeAI.tools.getAll()
                },
                onRunTool = { executeToolWithLifecycle(tool) }
            )
        }
    }
}

@Composable
private fun ToolItemCard(
    tool: Tool,
    isExecuting: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onRunTool: () -> Unit
) {
    AppCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("tool_card_${tool.id}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when (tool.riskLevel) {
                            RiskLevel.NONE, RiskLevel.LOW -> LocalAIGreen.copy(alpha = 0.15f)
                            RiskLevel.MEDIUM -> PrivateServerAmber.copy(alpha = 0.15f)
                            RiskLevel.HIGH, RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (tool.category) {
                                    ToolCategory.MEMORY -> Icons.Default.Psychology
                                    ToolCategory.VISION -> Icons.Default.Visibility
                                    ToolCategory.CALENDAR -> Icons.Default.CalendarMonth
                                    ToolCategory.TASKS -> Icons.Default.Checklist
                                    ToolCategory.NOTIFICATIONS -> Icons.Default.Notifications
                                    ToolCategory.FILES -> Icons.Default.Folder
                                    ToolCategory.DEVICE -> Icons.Default.Smartphone
                                    else -> Icons.Default.Build
                                },
                                contentDescription = null,
                                tint = when (tool.riskLevel) {
                                    RiskLevel.NONE, RiskLevel.LOW -> LocalAIGreen
                                    RiskLevel.MEDIUM -> PrivateServerAmber
                                    RiskLevel.HIGH, RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(text = tool.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Provider: ${tool.provider.name} • Privacy: ${tool.privacyLevel.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = tool.enabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("switch_tool_${tool.id}")
                )
            }

            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (tool.riskLevel) {
                        RiskLevel.NONE, RiskLevel.LOW -> LocalAIGreen.copy(alpha = 0.12f)
                        RiskLevel.MEDIUM -> PrivateServerAmber.copy(alpha = 0.12f)
                        RiskLevel.HIGH, RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    }
                ) {
                    Text(
                        text = "RISK: ${tool.riskLevel.name}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (tool.riskLevel) {
                            RiskLevel.NONE, RiskLevel.LOW -> LocalAIGreen
                            RiskLevel.MEDIUM -> PrivateServerAmber
                            RiskLevel.HIGH, RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                FilledTonalButton(
                    onClick = onRunTool,
                    enabled = tool.enabled && !isExecuting,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("test_run_tool_${tool.id}")
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Test Execution", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
