package com.example.edgeaicore.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.edgeaicore.core.ai.AIRequest
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.database.AgentLogEntity
import com.example.edgeaicore.core.preferences.EdgeUserSettings
import com.example.edgeaicore.core.ui.EdgeCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class EngineModalTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    STATUS("Status & Metrics", Icons.Default.Speed),
    CONTROL_PANEL("AI Parameters", Icons.Default.Tune),
    SKILLS_ACTIONS("Skills & Actions", Icons.Default.Extension),
    PLAYGROUND("Live Test Bench", Icons.Default.PlayCircle)
}

/**
 * Developer & Power-User AI ENGINE Inspector & Technical Control Center.
 * Comprehensive diagnostics, controllable hyperparameters, prompt playground,
 * system prompt controls, skill toggles, and real-time execution logs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiEngineModal(
    edgeAI: EdgeAICore,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(EngineModalTab.STATUS) }

    val privacyState by edgeAI.privacy.state.collectAsStateWithLifecycle()
    val metrics by edgeAI.diagnostics.flow().collectAsStateWithLifecycle()
    val specs = remember { edgeAI.diagnostics.specs() }
    val memoryCount by edgeAI.memory.count.collectAsStateWithLifecycle(initialValue = 0)
    val agentResult by edgeAI.agent.lastResult.collectAsStateWithLifecycle()
    val connectedMcpServers by edgeAI.mcp.connectedServers.collectAsStateWithLifecycle()
    val registeredTools = remember { edgeAI.tools.getAll() }
    val models by edgeAI.models.list.collectAsStateWithLifecycle()
    val userSettings by edgeAI.preferences.settings.collectAsStateWithLifecycle(initialValue = EdgeUserSettings())

    // Editable states for Technical Control Panel
    var editSystemPrompt by remember(userSettings) { mutableStateOf(userSettings.systemPrompt) }
    var editModelId by remember(userSettings) { mutableStateOf(userSettings.activeModelId) }
    var editTemperature by remember(userSettings) { mutableStateOf(userSettings.temperature) }
    var editTopK by remember(userSettings) { mutableStateOf(userSettings.topK.toFloat()) }
    var editTopP by remember(userSettings) { mutableStateOf(userSettings.topP) }
    var editMaxTokens by remember(userSettings) { mutableStateOf(userSettings.maxOutputTokens.toFloat()) }
    var editContextWindow by remember(userSettings) { mutableStateOf(userSettings.contextWindowSize.toFloat()) }
    var editPresencePenalty by remember(userSettings) { mutableStateOf(userSettings.presencePenalty) }
    var editFrequencyPenalty by remember(userSettings) { mutableStateOf(userSettings.frequencyPenalty) }
    var editStreamResponse by remember(userSettings) { mutableStateOf(userSettings.streamResponse) }
    var editStopSequences by remember(userSettings) { mutableStateOf(userSettings.stopSequences) }
    var editEnabledSkills by remember(userSettings) { mutableStateOf(userSettings.enabledSkills) }
    var editEnabledActions by remember(userSettings) { mutableStateOf(userSettings.enabledActions) }
    var editRequireConfirmation by remember(userSettings) { mutableStateOf(userSettings.requireHumanConfirmationForHighRisk) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Playground state
    var playgroundPrompt by remember { mutableStateOf("Explain how on-device AI preserves privacy while maintaining high reasoning quality.") }
    var playgroundResponse by remember { mutableStateOf("") }
    var playgroundLatency by remember { mutableStateOf(0L) }
    var playgroundTokensPerSec by remember { mutableStateOf(0.0) }
    var playgroundModelUsed by remember { mutableStateOf("") }
    var isPlaygroundRunning by remember { mutableStateOf(false) }

    // Real-time Agent Logs
    val recentLogs by edgeAI.database.agentLogs.observeRecentLogs(limit = 15).collectAsStateWithLifecycle(initialValue = emptyList<AgentLogEntity>())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Engine Control & Status",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hardware Telemetry • Hyperparameters • Action Governance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(12.dp))

            // Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
            ) {
                EngineModalTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(tab.title, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.testTag("engine_tab_${tab.name.lowercase()}")
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    EngineModalTab.STATUS -> {
                        // 1. LIVE INFERENCE METRICS & SYSTEM STATUS
                        EdgeCard {
                            Text(
                                text = "LIVE INFERENCE TELEMETRY & STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricStatusBadge(
                                    label = "Selected Model",
                                    value = userSettings.activeModelId,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1.4f)
                                )
                                MetricStatusBadge(
                                    label = "Avg Latency",
                                    value = "${metrics.averageInferenceLatencyMs} ms",
                                    color = LocalAIGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricStatusBadge(
                                    label = "Throughput",
                                    value = "${String.format("%.1f", metrics.tokensPerSecond)} t/s",
                                    color = Color(0xFF64B5F6),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            EngineInfoRow("System Status", if (!metrics.isThermalThrottled) "OPERATIONAL (Optimal)" else "THROTTLED (Thermal)")
                            EngineInfoRow("Inferences Processed", "${metrics.totalInferences} queries (Success: ${metrics.successfulInferences})")
                            val successRate = if (metrics.totalInferences > 0) (metrics.successfulInferences.toFloat() / metrics.totalInferences * 100) else 100f
                            EngineInfoRow("Inference Success Rate", "${String.format("%.1f", successRate)}% without errors")
                            EngineInfoRow("Active Accelerator", "${specs.recommendedBackend.name} (${if (specs.isGpuAvailable) "GPU/Vulkan" else "CPU"})")
                            EngineInfoRow("Available RAM", "${specs.availableRamMb} MB free / ${specs.totalRamMb} MB total")
                        }

                        // 2. TRI-TIER ROUTING STATUS
                        EdgeCard {
                            Text(
                                text = "INTELLIGENCE ROUTING TIERS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            EngineTierRow(
                                tier = "LOCAL AI",
                                details = "LiteRT-LM (Gemma 2B INT4) • Zero Cloud Exposure",
                                status = "ONLINE (DEFAULT)",
                                statusColor = LocalAIGreen
                            )
                            EngineTierRow(
                                tier = "PRIVATE AI",
                                details = "vLLM / SGLang Gateway • Encrypted LAN Tunnel",
                                status = if (privacyState.privateServerEnabled) "ENABLED (LAN)" else "DISABLED",
                                statusColor = if (privacyState.privateServerEnabled) PrivateServerAmber else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            EngineTierRow(
                                tier = "CLOUD AI",
                                details = "Gemini 2.5 Flash Fallback • User Consent Verified",
                                status = if (privacyState.cloudAiEnabled) "STANDBY / ACTIVE" else "DISABLED",
                                statusColor = if (privacyState.cloudAiEnabled) CloudAIBorder else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 3. RECENT AGENT & REASONING LOGS
                        EdgeCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RECENT AGENT EXECUTION LOGS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${recentLogs.size} logs recorded",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            if (recentLogs.isEmpty()) {
                                Text(
                                    text = "No recent agent execution logs yet. Logs are securely recorded in the local SQLite vault upon agent actions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (log in recentLogs.take(5)) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "[${log.level}]",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (log.level == "ERROR") Color.Red else LocalAIGreen
                                                )
                                                Text(
                                                    text = "${log.tag}: ${log.message}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (log.latencyMs > 0) {
                                                    Text(
                                                        text = "${log.latencyMs}ms",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    EngineModalTab.CONTROL_PANEL -> {
                        // SYSTEM PROMPT CONTROLS
                        EdgeCard {
                            Text(
                                text = "SYSTEM PROMPT INSTRUCTION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = editSystemPrompt,
                                onValueChange = { editSystemPrompt = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("control_panel_system_prompt_input"),
                                textStyle = MaterialTheme.typography.bodySmall,
                                minLines = 3,
                                maxLines = 6,
                                placeholder = { Text("Enter custom system prompt for the sovereign AI...") }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            // Quick Preset Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AssistChip(
                                    onClick = {
                                        editSystemPrompt = "You are SWAYAM GPT, a sovereign personal intelligence assistant. Provide articulate, well-structured, precise, and helpful responses based on on-device context and personal memories."
                                    },
                                    label = { Text("Default", fontSize = 11.sp) }
                                )
                                AssistChip(
                                    onClick = {
                                        editSystemPrompt = "You are a concise, technical reasoning engine. Answer with bullet points, zero fluff, and provide actionable Kotlin/Compose code where requested."
                                    },
                                    label = { Text("Concise Coder", fontSize = 11.sp) }
                                )
                                AssistChip(
                                    onClick = {
                                        editSystemPrompt = "You are a private memory archivist. Focus strictly on retrieving personal knowledge vault entries with zero hallucination."
                                    },
                                    label = { Text("Vault Archivist", fontSize = 11.sp) }
                                )
                            }
                        }

                        // ACTIVE MODEL SELECTION
                        EdgeCard {
                            Text(
                                text = "ACTIVE INFERENCE MODEL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val availableModelOptions = listOf(
                                "gemini-2.5-flash" to "Gemini 2.5 Flash (Cloud/High Speed)",
                                "gemini-2.5-pro" to "Gemini 2.5 Pro (Cloud/Complex Logic)",
                                "gemma-2b-it-litert" to "Gemma 2B INT4 (Local LiteRT-LM)",
                                "gemma-7b-it" to "Gemma 7B (Private Server)",
                                "phi-3.5-mini" to "Phi-3.5 Mini 3.8B (Local NPU)"
                            )

                            availableModelOptions.forEach { (modelId, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editModelId = modelId }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = editModelId == modelId,
                                        onClick = { editModelId = modelId }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (editModelId == modelId) FontWeight.Bold else FontWeight.Normal)
                                        Text(text = "ID: $modelId", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // TECHNICAL HYPERPARAMETERS (TopK, TopP, Temp, MaxTokens, Context Window)
                        EdgeCard {
                            Text(
                                text = "TECHNICAL HYPERPARAMETERS & SAMPLING",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Temperature
                            ParameterSliderRow(
                                label = "Temperature",
                                value = editTemperature,
                                valueFormatted = String.format("%.2f", editTemperature),
                                valueRange = 0.0f..2.0f,
                                onValueChange = { editTemperature = it },
                                subtitle = "Lower = deterministic & factual; Higher = creative & varied"
                            )

                            // Top-K
                            ParameterSliderRow(
                                label = "Top-K Sampling",
                                value = editTopK,
                                valueFormatted = editTopK.toInt().toString(),
                                valueRange = 1f..100f,
                                onValueChange = { editTopK = it },
                                subtitle = "Restricts token candidate pool to top K probabilities"
                            )

                            // Top-P
                            ParameterSliderRow(
                                label = "Top-P (Nucleus Sampling)",
                                value = editTopP,
                                valueFormatted = String.format("%.2f", editTopP),
                                valueRange = 0.05f..1.0f,
                                onValueChange = { editTopP = it },
                                subtitle = "Dynamically cuts candidate tokens cumulative probability"
                            )

                            // Max Output Tokens
                            ParameterSliderRow(
                                label = "Max Output Tokens",
                                value = editMaxTokens,
                                valueFormatted = editMaxTokens.toInt().toString(),
                                valueRange = 128f..4096f,
                                onValueChange = { editMaxTokens = it },
                                subtitle = "Upper bound on tokens generated per single response"
                            )

                            // Context Window Size
                            ParameterSliderRow(
                                label = "Context Window Buffer",
                                value = editContextWindow,
                                valueFormatted = "${editContextWindow.toInt()} tokens",
                                valueRange = 1024f..16384f,
                                onValueChange = { editContextWindow = it },
                                subtitle = "Local conversation history & retrieved memory token budget"
                            )

                            // Streaming Switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Token Streaming Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Render tokens live as generated by the inference engine", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = editStreamResponse,
                                    onCheckedChange = { editStreamResponse = it },
                                    modifier = Modifier.testTag("control_panel_stream_switch")
                                )
                            }
                        }

                        // SAVE BUTTON
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val updated = userSettings.copy(
                                        systemPrompt = editSystemPrompt,
                                        activeModelId = editModelId,
                                        temperature = editTemperature,
                                        topK = editTopK.toInt(),
                                        topP = editTopP,
                                        maxOutputTokens = editMaxTokens.toInt(),
                                        contextWindowSize = editContextWindow.toInt(),
                                        streamResponse = editStreamResponse,
                                        enabledSkills = editEnabledSkills,
                                        enabledActions = editEnabledActions,
                                        requireHumanConfirmationForHighRisk = editRequireConfirmation
                                    )
                                    edgeAI.preferences.updateSettings(updated)
                                    saveSuccessMessage = "AI Engine parameters and system configuration successfully applied!"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("control_panel_save_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save & Apply Parameters", fontWeight = FontWeight.Bold)
                        }

                        saveSuccessMessage?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalAIGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    EngineModalTab.SKILLS_ACTIONS -> {
                        // CONTROLLABLE SKILLS
                        EdgeCard {
                            Text(
                                text = "CONTROLLABLE AI SKILLS & CAPABILITIES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val skillList = listOf(
                                "VISION_OCR" to "Vision & CameraX OCR Perception",
                                "MEMORY_RECALL" to "Memory Vault & Semantic Vector Recall",
                                "TASK_MANAGEMENT" to "Task & Agenda Orchestration",
                                "CALENDAR_EVENTS" to "Calendar & Event Scheduling",
                                "AUDIO_JOURNAL" to "Voice & Audio Journal Processing",
                                "DOCUMENT_VAULT" to "Document & Encrypted File Ingestion",
                                "MCP_TOOLS" to "Model Context Protocol (MCP) Tools",
                                "DEVICE_AUTOMATION" to "On-Device Intent Automation"
                            )

                            skillList.forEach { (skillKey, label) ->
                                val isEnabled = editEnabledSkills.contains(skillKey)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { checked ->
                                            editEnabledSkills = if (checked) {
                                                editEnabledSkills + skillKey
                                            } else {
                                                editEnabledSkills - skillKey
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // CONTROLLABLE ACTIONS
                        EdgeCard {
                            Text(
                                text = "AUTONOMOUS AGENT ACTION GOVERNANCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val actionList = listOf(
                                "CREATE_TASK" to "Create & Schedule Tasks",
                                "CREATE_REMINDER" to "Set Alarms & Reminders",
                                "SAVE_MEMORY" to "Auto-Persist Insights to Memory Vault",
                                "CREATE_CALENDAR_EVENT" to "Book Calendar Events",
                                "START_TIMER" to "Launch Native Countdown Timers",
                                "OPEN_MAP" to "Trigger Navigation & Maps Intent",
                                "OPEN_SCREEN" to "Navigate In-App Subsystems"
                            )

                            actionList.forEach { (actionKey, label) ->
                                val isEnabled = editEnabledActions.contains(actionKey)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    Checkbox(
                                        checked = isEnabled,
                                        onCheckedChange = { checked ->
                                            editEnabledActions = if (checked == true) {
                                                editEnabledActions + actionKey
                                            } else {
                                                editEnabledActions - actionKey
                                            }
                                        }
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("High-Risk Confirmation Gate", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("Always prompt user dialog before executing state-mutating actions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = editRequireConfirmation,
                                    onCheckedChange = { editRequireConfirmation = it }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val updated = userSettings.copy(
                                        enabledSkills = editEnabledSkills,
                                        enabledActions = editEnabledActions,
                                        requireHumanConfirmationForHighRisk = editRequireConfirmation
                                    )
                                    edgeAI.preferences.updateSettings(updated)
                                    saveSuccessMessage = "Skill permissions & action policies updated successfully."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Apply Governance Policy", fontWeight = FontWeight.Bold)
                        }
                    }

                    EngineModalTab.PLAYGROUND -> {
                        // LIVE PLAYGROUND TEST BENCH
                        EdgeCard {
                            Text(
                                text = "INTERACTIVE PARAMETER PLAYGROUND",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Test current configuration (${editModelId} • Temp: ${String.format("%.2f", editTemperature)} • TopK: ${editTopK.toInt()} • TopP: ${String.format("%.2f", editTopP)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = playgroundPrompt,
                                onValueChange = { playgroundPrompt = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("playground_prompt_input"),
                                label = { Text("Test Prompt") },
                                minLines = 2,
                                maxLines = 4
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    isPlaygroundRunning = true
                                    playgroundResponse = ""
                                    coroutineScope.launch {
                                        val req = AIRequest(
                                            prompt = playgroundPrompt,
                                            systemInstruction = editSystemPrompt,
                                            temperature = editTemperature,
                                            topK = editTopK.toInt(),
                                            topP = editTopP,
                                            maxTokens = editMaxTokens.toInt(),
                                            modelId = editModelId,
                                            privacyLevel = PrivacyLevel.LOCAL_ONLY,
                                            userConsent = true
                                        )
                                        val res = edgeAI.ai.generate(req)
                                        when (res) {
                                            is EdgeResult.Success -> {
                                                playgroundResponse = res.data.text
                                                playgroundLatency = res.data.latencyMs
                                                playgroundTokensPerSec = res.data.tokensPerSecond
                                                playgroundModelUsed = res.data.model
                                            }
                                            is EdgeResult.Failure -> {
                                                playgroundResponse = "Error: ${res.error.message}"
                                            }
                                        }
                                        isPlaygroundRunning = false
                                    }
                                },
                                enabled = !isPlaygroundRunning && playgroundPrompt.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("playground_run_btn")
                            ) {
                                if (isPlaygroundRunning) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Executing Inference...")
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Run Inference Test", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (playgroundResponse.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "INFERENCE RESULT",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "$playgroundLatency ms • ${String.format("%.1f", playgroundTokensPerSec)} t/s",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LocalAIGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = playgroundResponse,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricStatusBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ParameterSliderRow(
    label: String,
    value: Float,
    valueFormatted: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text(
                text = valueFormatted,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EngineTierRow(tier: String, details: String, status: String, statusColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = tier, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text(text = details, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = statusColor.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
        ) {
            Text(
                text = status,
                color = statusColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun EngineInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
    }
}
