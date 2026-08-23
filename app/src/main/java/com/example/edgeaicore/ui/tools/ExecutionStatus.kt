package com.example.edgeaicore.ui.tools

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgeaicore.ui.common.AppCard
import com.example.ui.theme.*

enum class ToolExecutionStage(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    INITIATED(
        title = "Initiated",
        description = "Validating security policies, schema, & sandbox permissions...",
        icon = Icons.Default.Security
    ),
    PROCESSING(
        title = "Processing",
        description = "Executing tool payload on sandboxed on-device runtime...",
        icon = Icons.Default.HourglassTop
    ),
    SUCCESS(
        title = "Execution Succeeded",
        description = "Completed locally with verified zero data egress.",
        icon = Icons.Default.CheckCircle
    ),
    ERROR(
        title = "Execution Failed",
        description = "An error occurred during tool execution.",
        icon = Icons.Default.Error
    )
}

data class ToolExecutionState(
    val toolId: String,
    val toolName: String,
    val stage: ToolExecutionStage,
    val durationMs: Long = 0L,
    val outputSnippet: String? = null,
    val errorMessage: String? = null,
    val serverName: String = "On-Device Sandbox",
    val dataEgressBytes: Long = 0L
)

/**
 * ExecutionStatus UI Component:
 * Tracks and animates the lifecycle of MCP tool invocations
 * (Initiated -> Processing -> Success / Error) with transparent feedback.
 */
@Composable
fun ExecutionStatus(
    state: ToolExecutionState,
    onDismiss: () -> Unit = {},
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val stageColor = when (state.stage) {
        ToolExecutionStage.INITIATED -> MaterialTheme.colorScheme.primary
        ToolExecutionStage.PROCESSING -> PrivateServerAmber
        ToolExecutionStage.SUCCESS -> LocalAIGreen
        ToolExecutionStage.ERROR -> MaterialTheme.colorScheme.error
    }

    AppCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = stageColor.copy(alpha = 0.6f),
        modifier = modifier.testTag("tool_execution_status_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row
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
                        color = stageColor.copy(alpha = 0.15f),
                        modifier = Modifier
                            .size(36.dp)
                            .then(
                                if (state.stage == ToolExecutionStage.PROCESSING) Modifier.scale(pulseScale) else Modifier
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = state.stage.icon,
                                contentDescription = null,
                                tint = stageColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = state.toolName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "MCP Host: ${state.serverName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Stage Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = stageColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = state.stage.title.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = stageColor,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Step Progress Timeline Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExecutionStepBullet(
                    label = "1. Initiated",
                    isActive = state.stage == ToolExecutionStage.INITIATED,
                    isCompleted = state.stage != ToolExecutionStage.INITIATED,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ExecutionStepBullet(
                    label = "2. Processing",
                    isActive = state.stage == ToolExecutionStage.PROCESSING,
                    isCompleted = state.stage == ToolExecutionStage.SUCCESS || state.stage == ToolExecutionStage.ERROR,
                    color = PrivateServerAmber,
                    modifier = Modifier.weight(1f)
                )
                ExecutionStepBullet(
                    label = if (state.stage == ToolExecutionStage.ERROR) "3. Error" else "3. Success",
                    isActive = state.stage == ToolExecutionStage.SUCCESS || state.stage == ToolExecutionStage.ERROR,
                    isCompleted = state.stage == ToolExecutionStage.SUCCESS,
                    color = if (state.stage == ToolExecutionStage.ERROR) MaterialTheme.colorScheme.error else LocalAIGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            // Status Description & Live Feedback
            AnimatedContent(
                targetState = state.stage,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "status_description_transition"
            ) { stage ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stage.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (stage == ToolExecutionStage.PROCESSING) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = PrivateServerAmber
                        )
                    }
                }
            }

            // Output Snippet on Success
            if (state.stage == ToolExecutionStage.SUCCESS && !state.outputSnippet.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "RETURNED PAYLOAD",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${state.durationMs} ms • 0 bytes egress",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalAIGreen
                            )
                        }
                        Text(
                            text = state.outputSnippet,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 4
                        )
                    }
                }
            }

            // Error Message on Error
            if (state.stage == ToolExecutionStage.ERROR && !state.errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.errorMessage,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Actions Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.stage == ToolExecutionStage.ERROR && onRetry != null) {
                    FilledTonalButton(
                        onClick = onRetry,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("retry_execution_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (state.stage == ToolExecutionStage.SUCCESS || state.stage == ToolExecutionStage.ERROR) {
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("dismiss_execution_btn")
                    ) {
                        Text("Dismiss", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecutionStepBullet(
    label: String,
    isActive: Boolean,
    isCompleted: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isCompleted || isActive) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive || isCompleted) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
