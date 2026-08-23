package com.example.edgeaicore.ui.agent

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.agent.AgentExecutionResult
import com.example.edgeaicore.core.agent.AgentProfile
import com.example.edgeaicore.core.agent.AgentStateStep
import com.example.edgeaicore.core.agent.AgentStep
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.explanation.ExplanationRecord
import com.example.edgeaicore.core.policy.ToolActionProposal
import com.example.edgeaicore.ui.common.AppCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen(
    edgeAI: EdgeAICore,
    initialGoal: String? = null,
    onShowExplanation: (ExplanationRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isExecuting by edgeAI.agent.isExecuting.collectAsStateWithLifecycle()
    val currentStateStep by edgeAI.agent.currentStateStep.collectAsStateWithLifecycle()
    val lastResult by edgeAI.agent.lastResult.collectAsStateWithLifecycle()
    val pendingProposals by edgeAI.agent.confirmationManager.proposals.collectAsStateWithLifecycle()

    var goalInput by remember { mutableStateOf(initialGoal ?: "") }
    var selectedProfile by remember { mutableStateOf(AgentProfile.ASSISTANT) }
    var currentExecutionResult by remember { mutableStateOf<AgentExecutionResult?>(lastResult) }

    val presetGoals = remember {
        listOf(
            "Summarize my unread notifications and organize morning schedule",
            "Index all recent receipts from camera captures",
            "Audit on-device storage and clean temporary AI cache",
            "Prepare weekly focus plan based on personal memories"
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. HEADER
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "AUTONOMOUS REASONING",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Agent Runtime",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Executes multi-step goals through ToolGateway with strict human confirmation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. LIVE EXECUTION STATE STEPPER (UNDERSTANDING, CHECKING MEMORY, READY)
        item {
            AgentStateProgressCard(
                currentStateStep = currentStateStep,
                isExecuting = isExecuting
            )
        }

        // 3. AGENT PROFILE SELECTOR
        item {
            val profiles = remember { edgeAI.agent.getProfiles() }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SELECT AGENT PROFILE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(profiles) { profile ->
                        FilterChip(
                            selected = selectedProfile.id == profile.id,
                            onClick = { selectedProfile = profile },
                            label = { Text(profile.name, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (profile.id) {
                                        AgentProfile.ASSISTANT.id -> Icons.Default.SmartToy
                                        AgentProfile.MEMORY.id -> Icons.Default.Psychology
                                        AgentProfile.VISION.id -> Icons.Default.Visibility
                                        AgentProfile.COACH.id -> Icons.Default.FitnessCenter
                                        AgentProfile.STUDY.id -> Icons.Default.School
                                        AgentProfile.CREATOR.id -> Icons.Default.Brush
                                        AgentProfile.PRODUCTIVITY.id -> Icons.Default.Checklist
                                        AgentProfile.TRAVEL.id -> Icons.Default.Explore
                                        else -> Icons.Default.SmartToy
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // 4. GOAL INPUT CARD (Natural language command issuer)
        item {
            AppCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Natural Language Command",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LocalAIGreen.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "ON-DEVICE REASONING",
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
                        placeholder = { Text("e.g. Find all receipts from yesterday and summarize my grocery budget") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp)
                            .testTag("agent_goal_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (goalInput.isNotBlank() && !isExecuting) {
                                val currentGoal = goalInput
                                coroutineScope.launch {
                                    val res = edgeAI.agent.run(
                                        request = currentGoal,
                                        profile = selectedProfile,
                                        userConsentGiven = false
                                    )
                                    if (res is EdgeResult.Success) {
                                        currentExecutionResult = res.data
                                    }
                                }
                            }
                        },
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
                            Text("Agent Reasoning & Executing...")
                        } else {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Execute Autonomous Command", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. PRESET INSPIRATIONS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SUGGESTED COMMANDS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                presetGoals.forEach { preset ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { goalInput = preset },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.TrendingFlat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(text = preset, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // 6. PENDING HUMAN-IN-THE-LOOP ACTION PROPOSALS
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
                                coroutineScope.launch {
                                    edgeAI.agent.confirmationManager.confirm(proposal.id)
                                }
                            },
                            onCancel = {
                                coroutineScope.launch {
                                    edgeAI.agent.confirmationManager.cancel(proposal.id)
                                }
                            }
                        )
                    }
                }
            }
        }

        // 7. EXECUTION TRACE & STEPS
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
                            text = "EXECUTION TRACE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Latency: ${execution.latencyMs} ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalAIGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Final Response Box
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = LocalAIGreen, modifier = Modifier.size(18.dp))
                                Text("Final Synthesis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = execution.finalResponse,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
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
}

/**
 * Visual State Step Stepper showing clear, non-technical state steps:
 * 'READY', 'UNDERSTANDING', 'CHECKING MEMORY', 'PLANNING', 'EXECUTING TOOL'
 */
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

            // Step progression pill row
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
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
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
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
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
