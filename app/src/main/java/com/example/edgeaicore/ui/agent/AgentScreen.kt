package com.example.edgeaicore.ui.agent

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.agent.*
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.explanation.ExplanationRecord
import com.example.edgeaicore.core.policy.ToolActionProposal
import com.example.edgeaicore.ui.common.AppCard
import com.example.edgeaicore.ui.common.ResponseActionToolbar
import com.example.edgeaicore.ui.console.RichMessageContent
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AgentScreenTab(val label: String, val icon: ImageVector) {
    COMMAND_CENTER("Neural Command", Icons.Default.SmartToy),
    RECURRING_SCHEDULER("Recurring Triggers", Icons.Default.Schedule),
    EXECUTION_LOGS("History & Logs", Icons.Default.History)
}

data class AgentCardMeta(
    val profile: AgentProfile,
    val role: String,
    val icon: ImageVector,
    val accentColor: Color,
    val capabilities: List<String>,
    val defaultStarter: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen(
    edgeAI: EdgeAICore,
    initialGoal: String? = null,
    onShowExplanation: (ExplanationRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(AgentScreenTab.COMMAND_CENTER) }

    val isExecuting by edgeAI.agent.isExecuting.collectAsStateWithLifecycle()
    val currentStateStep by edgeAI.agent.currentStateStep.collectAsStateWithLifecycle()
    val lastResult by edgeAI.agent.lastResult.collectAsStateWithLifecycle()
    val pendingProposals by edgeAI.agent.confirmationManager.proposals.collectAsStateWithLifecycle()

    val scheduledTriggers: List<AgentScheduleTrigger> by edgeAI.agent.scheduledTriggers.collectAsStateWithLifecycle(initialValue = emptyList())
    val executionLogs: List<ScheduleExecutionLog> by edgeAI.agent.executionLogs.collectAsStateWithLifecycle(initialValue = emptyList())
    val isSchedulerActive: Boolean by edgeAI.agent.scheduler.isSchedulerActive.collectAsStateWithLifecycle(initialValue = true)

    var goalInput by remember { mutableStateOf(initialGoal ?: "") }
    var selectedProfile by remember { mutableStateOf(AgentProfile.ASSISTANT) }
    var currentExecutionResult by remember { mutableStateOf<AgentExecutionResult?>(lastResult) }
    var showCreateTriggerDialog by remember { mutableStateOf(false) }
    var runningTriggerId by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    val agentMetadataList = remember {
        listOf(
            AgentCardMeta(
                profile = AgentProfile.ASSISTANT,
                role = "Multi-Step Planner & Orchestrator",
                icon = Icons.Default.SmartToy,
                accentColor = GoogleBlue,
                capabilities = listOf("Tools", "Planning", "Reasoning"),
                defaultStarter = "Analyze my day and summarize upcoming priorities"
            ),
            AgentCardMeta(
                profile = AgentProfile.MEMORY,
                role = "Episodic Recall & Vault Organizer",
                icon = Icons.Default.Psychology,
                accentColor = GoogleGreen,
                capabilities = listOf("Memory RAG", "SQLite Vault", "Recall"),
                defaultStarter = "Recall what I saved regarding budget plans and summarize key takeaways"
            ),
            AgentCardMeta(
                profile = AgentProfile.VISION,
                role = "Camera OCR & Scene Reasoner",
                icon = Icons.Default.Visibility,
                accentColor = GoogleYellow,
                capabilities = listOf("OCR", "CameraX", "Visual RAG"),
                defaultStarter = "Index all recent receipts from camera captures and categorize expenses"
            ),
            AgentCardMeta(
                profile = AgentProfile.COACH,
                role = "Wellness, Hydration & Habit Engine",
                icon = Icons.Default.FitnessCenter,
                accentColor = GoogleRed,
                capabilities = listOf("Habits", "Wellness", "Routines"),
                defaultStarter = "Create a structured 4-week hydration, workout, and sleep routine"
            ),
            AgentCardMeta(
                profile = AgentProfile.STUDY,
                role = "Research Analyst & Flashcards",
                icon = Icons.Default.School,
                accentColor = GoogleBlue,
                capabilities = listOf("PDFs", "Synthesis", "Quiz"),
                defaultStarter = "Extract core theorems from indexed documents and prepare flashcards"
            ),
            AgentCardMeta(
                profile = AgentProfile.CREATOR,
                role = "Ideation, Copywriting & Brainstorming",
                icon = Icons.Default.Brush,
                accentColor = GoogleYellow,
                capabilities = listOf("Copy", "Ideation", "Design"),
                defaultStarter = "Draft a product launch announcement for an on-device sovereign AI app"
            ),
            AgentCardMeta(
                profile = AgentProfile.PRODUCTIVITY,
                role = "Executive Automation & Tasks",
                icon = Icons.Default.Checklist,
                accentColor = GoogleGreen,
                capabilities = listOf("Tasks", "Calendar", "Auditing"),
                defaultStarter = "Summarize my unread notifications and organize morning schedule"
            ),
            AgentCardMeta(
                profile = AgentProfile.TRAVEL,
                role = "Itineraries & Geo Navigation",
                icon = Icons.Default.Explore,
                accentColor = GoogleBlue,
                capabilities = listOf("Maps", "Packing", "Timeline"),
                defaultStarter = "Build a 3-day weekend itinerary with offline checklist and packing items"
            ),
            AgentCardMeta(
                profile = AgentProfile.LIFE,
                role = "System Storage & Health Auditor",
                icon = Icons.Default.Favorite,
                accentColor = GoogleRed,
                capabilities = listOf("Storage", "Cleanup", "Security"),
                defaultStarter = "Audit on-device storage and clean temporary AI cache files"
            )
        )
    }

    fun executeGoal(goal: String, profile: AgentProfile) {
        if (goal.isBlank() || isExecuting) return
        coroutineScope.launch {
            val res = edgeAI.agent.run(
                request = goal,
                profile = profile,
                userConsentGiven = false
            )
            if (res is EdgeResult.Success) {
                currentExecutionResult = res.data
                snackbarMessage = "Agent completed task in ${res.data.steps.size} steps!"
            } else if (res is EdgeResult.Failure) {
                snackbarMessage = "Execution failed: ${res.error.message}"
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HEADER
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LocalAIGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "SOVEREIGN AGENT OS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = LocalAIGreen,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Autonomous Agents & Scheduler",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Deploy specialized agents with multi-step reasoning, native tool gateways, and recurring scheduled triggers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. TAB SWITCHER
            item {
                TabRow(
                    selectedTabIndex = currentTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[currentTab.ordinal]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    divider = {}
                ) {
                    AgentScreenTab.values().forEach { tab ->
                        val selected = currentTab == tab
                        Tab(
                            selected = selected,
                            onClick = { currentTab = tab },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(imageVector = tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = if (tab == AgentScreenTab.RECURRING_SCHEDULER) "${tab.label} (${scheduledTriggers.size})" else tab.label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // ==========================================
            // TAB 1: NEURAL COMMAND CENTER
            // ==========================================
            if (currentTab == AgentScreenTab.COMMAND_CENTER) {
                // Live Execution State Stepper
                item {
                    AgentStateProgressCard(
                        currentStateStep = currentStateStep,
                        isExecuting = isExecuting
                    )
                }

                // 3. STACKED GRID OF AVAILABLE AGENTS WITH ANIMATED ICONS
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AVAILABLE AGENTS (TAP TO SELECT & LAUNCH)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${agentMetadataList.size} Specialized Minds",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Stacked 2-Column Grid
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val rows = agentMetadataList.chunked(2)
                            rows.forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { meta ->
                                        val isSelected = selectedProfile.id == meta.profile.id
                                        Box(modifier = Modifier.weight(1f)) {
                                            AgentGridCard(
                                                meta = meta,
                                                isSelected = isSelected,
                                                onClick = {
                                                    selectedProfile = meta.profile
                                                    if (goalInput.isBlank()) {
                                                        goalInput = meta.defaultStarter
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. NEURAL COMMAND INPUT BOX
                item {
                    AppCard(
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = getProfileIcon(selectedProfile.id),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Neural Command: ${selectedProfile.name}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = LocalAIGreen.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "AUTONOMOUS ON-DEVICE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LocalAIGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = goalInput,
                                onValueChange = { goalInput = it },
                                placeholder = { Text("Enter multi-step autonomous goal for ${selectedProfile.name}...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 90.dp)
                                    .testTag("agent_goal_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Quick Starters tailored to active agent
                            val currentMeta = agentMetadataList.find { it.profile.id == selectedProfile.id }
                            if (currentMeta != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .clickable { goalInput = currentMeta.defaultStarter }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "Preset: ${currentMeta.defaultStarter}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            Button(
                                onClick = { executeGoal(goalInput, selectedProfile) },
                                enabled = goalInput.isNotBlank() && !isExecuting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("run_agent_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isExecuting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Agent Reasoning & Executing Tools...")
                                } else {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Execute Autonomous Command", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 5. PENDING SAFETY PROPOSALS
                val activePending = pendingProposals.filter { it.status == com.example.edgeaicore.core.policy.ConfirmationStatus.PENDING }
                if (activePending.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "SAFETY CONFIRMATIONS REQUIRED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error,
                                letterSpacing = 1.sp
                            )
                            activePending.forEach { proposal ->
                                PendingProposalCard(
                                    proposal = proposal,
                                    onConfirm = {
                                        coroutineScope.launch { edgeAI.agent.confirmationManager.confirm(proposal.id) }
                                    },
                                    onCancel = {
                                        coroutineScope.launch { edgeAI.agent.confirmationManager.cancel(proposal.id) }
                                    }
                                )
                            }
                        }
                    }
                }

                // 6. EXECUTION OUTCOME & TRACE
                val execution = currentExecutionResult ?: lastResult
                if (execution != null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "EXECUTION OUTCOME & TRACE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Latency: ${execution.latencyMs} ms • ${execution.steps.size} Steps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LocalAIGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Final Synthesis Box with Rich Markdown & Response Tools
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = LocalAIGreen, modifier = Modifier.size(18.dp))
                                            Text("Final Synthesis Outcome", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = if (execution.isSuccess) "COMPLETED" else "PARTIAL",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    RichMessageContent(
                                        text = execution.finalResponse,
                                        textColor = MaterialTheme.colorScheme.onSurface
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                    // Action Toolbar for Agent Outcome
                                    ResponseActionToolbar(
                                        responseText = execution.finalResponse,
                                        onTranslate = { targetLang ->
                                            coroutineScope.launch {
                                                val transRes = edgeAI.swayam.translate(execution.finalResponse, targetLang)
                                                if (transRes is EdgeResult.Success) {
                                                    Toast.makeText(context, "Translated to $targetLang", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        onRevertTranslation = {},
                                        onRegenerate = { executeGoal(goalInput, selectedProfile) },
                                        onExport = { textToSave ->
                                            coroutineScope.launch {
                                                edgeAI.memory.create(
                                                    title = "Agent Plan: ${textToSave.take(25)}...",
                                                    content = textToSave,
                                                    tags = "agent,synthesis,plan"
                                                )
                                                Toast.makeText(context, "Saved to Memory Vault!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        explanation = ExplanationRecord(
                                            featureName = "Agent Goal Synthesis",
                                            whatHappened = "Executed ${execution.steps.size} orchestrated steps and synthesized final response.",
                                            whyReason = "Autonomous goal: '$goalInput'",
                                            confidenceScore = 0.95f,
                                            dataSourcesUsed = execution.toolsExecuted.ifEmpty { listOf("Agent Runtime") },
                                            wasAiInvolved = true,
                                            providerType = com.example.edgeaicore.core.common.AIProviderType.LOCAL,
                                            privacyLevel = com.example.edgeaicore.core.common.PrivacyLevel.LOCAL_ONLY
                                        ),
                                        onShowExplanation = onShowExplanation
                                    )
                                }
                            }

                            // Steps Breakdown
                            Text(
                                text = "Orchestrated Steps (${execution.steps.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            execution.steps.forEach { step ->
                                StepItemCard(step = step)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // TAB 2: RECURRING SCHEDULER & TRIGGERS
            // ==========================================
            if (currentTab == AgentScreenTab.RECURRING_SCHEDULER) {
                item {
                    AppCard(
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Autorenew,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Autonomous Task Scheduler",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (isSchedulerActive) "Active • Background runner continuously monitors recurring triggers on NPU/GPU" else "Paused • Automated periodic triggers suspended",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSchedulerActive) LocalAIGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isSchedulerActive,
                                onCheckedChange = { edgeAI.agent.scheduler.setSchedulerActive(it) }
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONFIGURED TRIGGERS (${scheduledTriggers.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                        FilledTonalButton(
                            onClick = { showCreateTriggerDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Trigger", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(items = scheduledTriggers, key = { it.id }) { trigger ->
                    val isRunning = runningTriggerId == trigger.id
                    ScheduledTriggerCard(
                        trigger = trigger,
                        isRunning = isRunning,
                        onToggle = { edgeAI.agent.scheduler.toggleTrigger(trigger.id, it) },
                        onRunNow = {
                            coroutineScope.launch {
                                runningTriggerId = trigger.id
                                val res = edgeAI.agent.runTriggerNow(trigger.id)
                                runningTriggerId = null
                                when (res) {
                                    is EdgeResult.Success -> {
                                        snackbarMessage = "Trigger '${trigger.name}' finished in ${res.data.durationMs}ms"
                                    }
                                    is EdgeResult.Failure -> {
                                        snackbarMessage = "Trigger failed: ${res.error.message}"
                                    }
                                }
                            }
                        },
                        onDelete = {
                            edgeAI.agent.scheduler.deleteTrigger(trigger.id)
                            snackbarMessage = "Trigger deleted"
                        }
                    )
                }
            }

            // ==========================================
            // TAB 3: EXECUTION HISTORY & LOGS
            // ==========================================
            if (currentTab == AgentScreenTab.EXECUTION_LOGS) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HISTORICAL RUNS (${executionLogs.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                        if (executionLogs.isNotEmpty()) {
                            TextButton(
                                onClick = { edgeAI.agent.scheduler.clearLogs() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Logs", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (executionLogs.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "No Execution Logs Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Automated and manual agent runs will log execution duration, token count, and synthesis results here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(items = executionLogs, key = { it.id }) { log ->
                        ExecutionLogCard(log = log)
                    }
                }
            }
        }
    }

    if (showCreateTriggerDialog) {
        CreateTriggerDialog(
            profiles = edgeAI.agent.getProfiles(),
            onDismiss = { showCreateTriggerDialog = false },
            onCreate = { trigger ->
                edgeAI.agent.scheduler.addTrigger(trigger)
                showCreateTriggerDialog = false
                snackbarMessage = "Trigger '${trigger.name}' scheduled!"
            }
        )
    }
}

/**
 * Stacked Agent Grid Card with Animated Pulsating Icon.
 */
@Composable
private fun AgentGridCard(
    meta: AgentCardMeta,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "agent_card_anim_${meta.profile.id}")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isSelected) 1.15f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated Glowing Icon Container
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(meta.accentColor.copy(alpha = if (isSelected) 0.25f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = meta.icon,
                        contentDescription = null,
                        tint = meta.accentColor,
                        modifier = Modifier
                            .size(20.dp)
                            .scale(scaleAnim)
                    )
                }

                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "SELECTED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = meta.profile.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = meta.role,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 2,
                    lineHeight = 13.sp
                )
            }

            // Capability pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                meta.capabilities.take(2).forEach { cap ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = cap,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduledTriggerCard(
    trigger: AgentScheduleTrigger,
    isRunning: Boolean,
    onToggle: (Boolean) -> Unit,
    onRunNow: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val nextRunFormatted = remember(trigger.nextRunAt) {
        val diffMinutes = (trigger.nextRunAt - System.currentTimeMillis()) / (60 * 1000)
        if (diffMinutes <= 0) "Due now" else "in ~$diffMinutes min (${dateFormat.format(Date(trigger.nextRunAt))})"
    }

    AppCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = if (trigger.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getProfileIcon(trigger.targetProfileId),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = trigger.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Agent: ${trigger.targetProfileName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Switch(
                    checked = trigger.isEnabled,
                    onCheckedChange = onToggle
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = trigger.prompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(trigger.frequency.label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    shape = RoundedCornerShape(8.dp)
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text("Runs: ${trigger.executionCount}", fontSize = 10.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
                if (trigger.isEnabled) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Next: $nextRunFormatted", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            if (!trigger.lastRunSummary.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (trigger.lastRunStatus == "SUCCESS") Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (trigger.lastRunStatus == "SUCCESS") LocalAIGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Last: ${trigger.lastRunSummary}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete Trigger", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }

                Button(
                    onClick = onRunNow,
                    enabled = !isRunning,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Running...", fontSize = 12.sp)
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Run Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecutionLogCard(log: ScheduleExecutionLog) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = log.triggerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(text = "Agent: ${log.targetProfileName} • $formattedTime", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (log.status == "SUCCESS") LocalAIGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = log.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (log.status == "SUCCESS") LocalAIGreen else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = log.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Latency: ${log.durationMs}ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(text = "Steps: ${log.stepsCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (log.tokensGenerated > 0) {
                    Text(text = "Tokens: ${log.tokensGenerated}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTriggerDialog(
    profiles: List<AgentProfile>,
    onDismiss: () -> Unit,
    onCreate: (AgentScheduleTrigger) -> Unit
) {
    var triggerName by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var selectedProfile by remember { mutableStateOf(profiles.firstOrNull() ?: AgentProfile.ASSISTANT) }
    var frequency by remember { mutableStateOf(ScheduleFrequency.HOURLY) }
    var requiresConfirmation by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Schedule Recurring Task", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = triggerName,
                    onValueChange = { triggerName = it },
                    label = { Text("Trigger Name") },
                    placeholder = { Text("e.g. Periodic Web Search & Ingestion") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Agent Goal / Task Prompt") },
                    placeholder = { Text("e.g. Search latest benchmarks and save notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                Text(
                    text = "Target Agent Profile",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(profiles) { p ->
                        FilterChip(
                            selected = selectedProfile.id == p.id,
                            onClick = { selectedProfile = p },
                            label = { Text(p.name, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Text(
                    text = "Recurring Cadence",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ScheduleFrequency.values()) { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq.label, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Require Approval", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("Prompts user before sensitive actions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = requiresConfirmation, onCheckedChange = { requiresConfirmation = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (triggerName.isNotBlank() && prompt.isNotBlank()) {
                        onCreate(
                            AgentScheduleTrigger(
                                name = triggerName.trim(),
                                targetProfileId = selectedProfile.id,
                                targetProfileName = selectedProfile.name,
                                prompt = prompt.trim(),
                                frequency = frequency,
                                intervalMinutes = frequency.defaultMinutes,
                                requiresConfirmation = requiresConfirmation,
                                isEnabled = true
                            )
                        )
                    }
                },
                enabled = triggerName.isNotBlank() && prompt.isNotBlank(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Schedule Trigger")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getProfileIcon(profileId: String): ImageVector {
    return when (profileId) {
        AgentProfile.ASSISTANT.id -> Icons.Default.SmartToy
        AgentProfile.MEMORY.id -> Icons.Default.Psychology
        AgentProfile.VISION.id -> Icons.Default.Visibility
        AgentProfile.COACH.id -> Icons.Default.FitnessCenter
        AgentProfile.STUDY.id -> Icons.Default.School
        AgentProfile.CREATOR.id -> Icons.Default.Brush
        AgentProfile.PRODUCTIVITY.id -> Icons.Default.Checklist
        AgentProfile.TRAVEL.id -> Icons.Default.Explore
        AgentProfile.LIFE.id -> Icons.Default.Favorite
        else -> Icons.Default.SmartToy
    }
}

@Composable
private fun AgentStateProgressCard(
    currentStateStep: AgentStateStep,
    isExecuting: Boolean
) {
    val stepsList = listOf(
        AgentStateStep.UNDERSTANDING,
        AgentStateStep.CHECKING_MEMORY,
        AgentStateStep.PLANNING,
        AgentStateStep.EXECUTING_TOOL,
        AgentStateStep.READY
    )

    val infiniteTransition = rememberInfiniteTransition(label = "agent_stepper_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stepper_pulse"
    )

    AppCard(
        backgroundColor = if (isExecuting) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        borderColor = if (isExecuting) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isExecuting) MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha) else LocalAIGreen)
                    )
                    Text(
                        text = "STATE: ${currentStateStep.label}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isExecuting) MaterialTheme.colorScheme.primary else LocalAIGreen
                    )
                }

                Text(
                    text = if (isExecuting) "ACTIVE RUN" else "READY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = currentStateStep.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                stepsList.forEach { step ->
                    val isCurrent = step == currentStateStep
                    val isPast = when {
                        currentStateStep == AgentStateStep.READY -> step == AgentStateStep.READY
                        currentStateStep == AgentStateStep.COMPLETED -> true
                        currentStateStep == AgentStateStep.SYNTHESIZING -> step != AgentStateStep.READY
                        currentStateStep == AgentStateStep.EXECUTING_TOOL -> step in listOf(AgentStateStep.UNDERSTANDING, AgentStateStep.CHECKING_MEMORY, AgentStateStep.PLANNING, AgentStateStep.EXECUTING_TOOL)
                        currentStateStep == AgentStateStep.PLANNING -> step in listOf(AgentStateStep.UNDERSTANDING, AgentStateStep.CHECKING_MEMORY, AgentStateStep.PLANNING)
                        currentStateStep == AgentStateStep.CHECKING_MEMORY -> step in listOf(AgentStateStep.UNDERSTANDING, AgentStateStep.CHECKING_MEMORY)
                        currentStateStep == AgentStateStep.UNDERSTANDING -> step == AgentStateStep.UNDERSTANDING
                        else -> false
                    }

                    val stepColor = when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        isPast -> LocalAIGreen
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(stepColor)
                        )
                        Text(
                            text = when (step) {
                                AgentStateStep.UNDERSTANDING -> "UNDERSTAND"
                                AgentStateStep.CHECKING_MEMORY -> "MEMORY"
                                AgentStateStep.PLANNING -> "PLAN"
                                AgentStateStep.EXECUTING_TOOL -> "TOOL"
                                AgentStateStep.READY -> "READY"
                                else -> step.label
                            },
                            fontSize = 8.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepItemCard(step: AgentStep) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step ${step.stepIndex + 1}: ${step.selectedTool ?: "Reasoning"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Thought: ${step.thought}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (step.toolResult != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Result: ${step.toolResult}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingProposalCard(
    proposal: ToolActionProposal,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    text = "Action: ${proposal.toolName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = proposal.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Arguments: ${proposal.arguments}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 11.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Allow & Execute")
                }
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reject")
                }
            }
        }
    }
}
