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
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.RiskLevel
import com.example.edgeaicore.core.tools.Tool
import com.example.edgeaicore.ui.common.AppCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CustomMcpEndpoint(
    val id: String,
    val name: String,
    val endpointUrl: String,
    val transport: String,
    val trustLevel: String,
    val isConnected: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolPlaygroundScreen(
    edgeAI: EdgeAICore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var registeredTools by remember { mutableStateOf<List<Tool>>(emptyList()) }
    var selectedTool by remember { mutableStateOf<Tool?>(null) }
    var jsonArgumentsInput by remember { mutableStateOf("{\n  \"query\": \"recent memories\",\n  \"limit\": 5\n}") }
    var executionState by remember { mutableStateOf<ToolExecutionState?>(null) }

    // Custom MCP Endpoints Store
    var customEndpoints by remember {
        mutableStateOf(
            listOf(
                CustomMcpEndpoint(
                    id = "ep-1",
                    name = "HomeAssistant Local Bridge",
                    endpointUrl = "http://192.168.1.120:8123/mcp",
                    transport = "HTTP SSE / JSON-RPC 2.0",
                    trustLevel = "SANDBOXED_LOCAL",
                    isConnected = true
                ),
                CustomMcpEndpoint(
                    id = "ep-2",
                    name = "Private Obsidian Vault Bridge",
                    endpointUrl = "unix:///data/local/tmp/obsidian.sock",
                    transport = "UNIX DOMAIN SOCKET",
                    trustLevel = "AIR_GAPPED",
                    isConnected = true
                )
            )
        )
    }

    var showAddEndpointDialog by remember { mutableStateOf(false) }
    var newEpName by remember { mutableStateOf("") }
    var newEpUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        registeredTools = edgeAI.tools.getAll()
        if (registeredTools.isNotEmpty()) {
            selectedTool = registeredTools.first()
        }
    }

    fun runPlaygroundExecution(tool: Tool) {
        coroutineScope.launch {
            val startTime = System.currentTimeMillis()
            executionState = ToolExecutionState(
                toolId = tool.id,
                toolName = tool.name,
                stage = ToolExecutionStage.INITIATED,
                serverName = "Playground ToolGateway (${tool.provider.name})"
            )
            delay(400)

            executionState = executionState?.copy(stage = ToolExecutionStage.PROCESSING)
            delay(500)

            val result = edgeAI.tools.execute(
                toolId = tool.id,
                arguments = mapOf("rawJson" to jsonArgumentsInput),
                userConsentGiven = true
            )
            val elapsed = System.currentTimeMillis() - startTime

            executionState = when (result) {
                is EdgeResult.Success -> {
                    executionState?.copy(
                        stage = ToolExecutionStage.SUCCESS,
                        durationMs = elapsed,
                        outputSnippet = result.data.output.toString()
                    )
                }
                is EdgeResult.Failure -> {
                    executionState?.copy(
                        stage = ToolExecutionStage.ERROR,
                        durationMs = elapsed,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("MCP & Tool Playground", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tool_playground_back_btn")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddEndpointDialog = true },
                        modifier = Modifier.testTag("btn_add_mcp_endpoint")
                    ) {
                        Icon(imageVector = Icons.Default.AddLink, contentDescription = "Add MCP Endpoint")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HEADER
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column {
                            Text("Interactive Tool Sandbox & MCP Inspector", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Test tool payloads against the on-device ToolGateway with live telemetry and zero data egress.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 2. LIVE EXECUTION STATUS (If active)
            val exec = executionState
            if (exec != null) {
                item {
                    ExecutionStatus(
                        state = exec,
                        onDismiss = { executionState = null },
                        onRetry = { selectedTool?.let { runPlaygroundExecution(it) } }
                    )
                }
            }

            // 3. TOOL SELECTOR & ARGUMENT EDITOR
            item {
                AppCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("tool_playground_editor_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "TOOL PAYLOAD TESTER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        Text("Select Tool to Test:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

                        // Tools Dropdown / Chip selector
                        var expandedTools by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expandedTools = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedTool?.name ?: "Select a Tool",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = expandedTools,
                                onDismissRequest = { expandedTools = false }
                            ) {
                                registeredTools.forEach { tool ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(tool.name, fontWeight = FontWeight.Bold)
                                                Text(tool.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        },
                                        onClick = {
                                            selectedTool = tool
                                            expandedTools = false
                                        }
                                    )
                                }
                            }
                        }

                        // JSON Parameter Input
                        Text("JSON Input Arguments:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = jsonArgumentsInput,
                            onValueChange = { jsonArgumentsInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(10.dp),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )

                        // Run Button
                        Button(
                            onClick = {
                                selectedTool?.let { runPlaygroundExecution(it) }
                            },
                            enabled = selectedTool != null,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_execute_playground_payload")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dispatch Local Tool Payload", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. CUSTOM MCP ENDPOINTS LIST
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CUSTOM MCP ENDPOINTS (${customEndpoints.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    TextButton(
                        onClick = { showAddEndpointDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Server", fontSize = 11.sp)
                    }
                }
            }

            items(customEndpoints, key = { it.id }) { ep ->
                AppCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("custom_mcp_card_${ep.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = CircleShape, color = LocalAIGreen, modifier = Modifier.size(8.dp)) {}
                                Text(text = ep.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Text(text = ep.endpointUrl, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "Transport: ${ep.transport} • Trust: ${ep.trustLevel}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LocalAIGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "CONNECTED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalAIGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddEndpointDialog) {
        AlertDialog(
            onDismissRequest = { showAddEndpointDialog = false },
            title = { Text("Register MCP Server Endpoint", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Connect to a local server running JSON-RPC 2.0 or Server-Sent Events (SSE).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = newEpName,
                        onValueChange = { newEpName = it },
                        label = { Text("Server Name") },
                        placeholder = { Text("e.g. Local PostgreSQL MCP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = newEpUrl,
                        onValueChange = { newEpUrl = it },
                        label = { Text("Endpoint URL / Socket Path") },
                        placeholder = { Text("http://127.0.0.1:8080/mcp") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newEpName.isNotBlank() && newEpUrl.isNotBlank()) {
                            customEndpoints = customEndpoints + CustomMcpEndpoint(
                                id = "ep-${System.currentTimeMillis()}",
                                name = newEpName,
                                endpointUrl = newEpUrl,
                                transport = "HTTP SSE / JSON-RPC 2.0",
                                trustLevel = "SANDBOXED_LOCAL",
                                isConnected = true
                            )
                            showAddEndpointDialog = false
                            newEpName = ""
                            newEpUrl = ""
                        }
                    },
                    enabled = newEpName.isNotBlank() && newEpUrl.isNotBlank()
                ) {
                    Text("Add Endpoint")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEndpointDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
