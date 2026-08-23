package com.example.edgeaicore.ui.common

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.RiskLevel
import com.example.edgeaicore.core.explanation.ExplanationRecord
import com.example.ui.theme.*

/**
 * Universal Card Container following Material 3 guidelines.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .then(clickableModifier),
        color = backgroundColor,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

/**
 * Reusable AIStatus component displaying current connectivity and processing state
 * (LOCAL AI, PRIVATE AI, CLOUD AI, OFFLINE), ensuring the user always knows where intelligence is running.
 */
@Composable
fun AIStatus(
    providerType: AIProviderType = AIProviderType.LOCAL,
    isOffline: Boolean = false,
    isDemo: Boolean = false,
    hardwareAccelerator: String = "NPU / GPU ACCELERATED",
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (compact) {
        AIStatusBarPill(
            providerType = providerType,
            isOffline = isOffline,
            onClick = onClick,
            modifier = modifier
        )
    } else {
        AIStatusCard(
            providerType = providerType,
            isOffline = isOffline,
            isDemo = isDemo,
            hardwareAccelerator = hardwareAccelerator,
            onClick = onClick,
            modifier = modifier
        )
    }
}

/**
 * AI Status Component (LOCAL AI / PRIVATE AI / CLOUD AI / OFFLINE)
 * Tells the user where intelligence is running at a glance.
 */
@Composable
fun AIStatusCard(
    providerType: AIProviderType,
    isOffline: Boolean = false,
    isDemo: Boolean = false,
    hardwareAccelerator: String = "NPU / GPU",
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusText = when {
        isOffline -> "OFFLINE"
        isDemo -> "DEMO AI"
        providerType == AIProviderType.LOCAL -> "LOCAL AI"
        providerType == AIProviderType.PRIVATE_SERVER -> "PRIVATE AI"
        providerType == AIProviderType.CLOUD -> "CLOUD AI"
        else -> "LOCAL AI"
    }

    val stateSubtitle = when {
        isOffline -> "Running 100% on device"
        isDemo -> "Simulated edge environment"
        providerType == AIProviderType.LOCAL -> "Ready • On-Device Neural Engine"
        providerType == AIProviderType.PRIVATE_SERVER -> "Connected • Private Encrypted Tunnel"
        providerType == AIProviderType.CLOUD -> "Active • Consent Verified"
        else -> "Ready • On-Device"
    }

    val (badgeBg, badgeFg, icon) = when {
        isOffline -> Triple(OfflineGray.copy(alpha = 0.15f), OfflineGray, Icons.Default.CloudOff)
        isDemo -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, Icons.Default.Science)
        providerType == AIProviderType.LOCAL -> Triple(LocalAIGreen.copy(alpha = 0.15f), LocalAIGreen, Icons.Default.Memory)
        providerType == AIProviderType.PRIVATE_SERVER -> Triple(PrivateServerAmber.copy(alpha = 0.15f), PrivateServerAmber, Icons.Default.Dns)
        providerType == AIProviderType.CLOUD -> Triple(CloudAIBorder.copy(alpha = 0.15f), CloudAIBorder, Icons.Default.Cloud)
        else -> Triple(LocalAIGreen.copy(alpha = 0.15f), LocalAIGreen, Icons.Default.Memory)
    }

    AppCard(
        modifier = modifier.testTag("ai_status_card"),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        borderColor = badgeFg.copy(alpha = 0.35f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = badgeBg,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = badgeFg, modifier = Modifier.size(22.dp))
                    }
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(badgeFg)
                        )
                    }
                    Text(
                        text = stateSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = hardwareAccelerator,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Compact AI Status Bar pill for top bars or headers
 */
@Composable
fun AIStatusBarPill(
    providerType: AIProviderType,
    isOffline: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (label, color, icon) = when {
        isOffline -> Triple("OFFLINE", OfflineGray, Icons.Default.CloudOff)
        providerType == AIProviderType.LOCAL -> Triple("LOCAL AI", LocalAIGreen, Icons.Default.Memory)
        providerType == AIProviderType.PRIVATE_SERVER -> Triple("PRIVATE AI", PrivateServerAmber, Icons.Default.Dns)
        providerType == AIProviderType.CLOUD -> Triple("CLOUD AI", CloudAIBorder, Icons.Default.Cloud)
        else -> Triple("LOCAL AI", LocalAIGreen, Icons.Default.Memory)
    }

    Surface(
        modifier = modifier
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = color.copy(alpha = 0.14f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                letterSpacing = 0.6.sp
            )
        }
    }
}

/**
 * AI Multi-Stage Processing Component with live execution progression.
 */
@Composable
fun AIProcessingStages(
    stageTitle: String = "ANALYZING",
    stages: List<Pair<String, Boolean>>, // Label to Completed
    isProcessing: Boolean = true,
    error: String? = null,
    onRetry: (() -> Unit)? = null,
    onContinueWithoutAI: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "processing_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    AppCard(
        modifier = modifier.testTag("ai_processing_card"),
        backgroundColor = if (error != null) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        borderColor = if (error != null) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (error == null && isProcessing) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha))
                        )
                    }
                    Text(
                        text = if (error != null) "AI UNAVAILABLE" else stageTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                if (isProcessing && error == null) {
                    Text(
                        text = "Working on-device...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (onRetry != null) {
                        Button(
                            onClick = onRetry,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("retry_ai_btn")
                        ) {
                            Text("Try Again", fontSize = 12.sp)
                        }
                    }
                    if (onContinueWithoutAI != null) {
                        OutlinedButton(
                            onClick = onContinueWithoutAI,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("continue_without_ai_btn")
                        ) {
                            Text("Continue without AI", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    stages.forEach { (name, done) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (done) LocalAIGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (done) FontWeight.Bold else FontWeight.Normal,
                                color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Universal Explanation Modal Sheet / Dialog
 * Explains: WHAT HAPPENED, WHY, DATA USED, AI USED, WHERE IT RAN, CONFIDENCE
 */
@Composable
fun UniversalExplanationSheet(
    record: ExplanationRecord?,
    onDismiss: () -> Unit
) {
    if (record == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Column {
                    Text(
                        text = "Why this result?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = record.featureName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExplainRow("WHAT HAPPENED", record.whatHappened)
                ExplainRow("WHY", record.whyReason)
                ExplainRow("DATA USED", record.dataSourcesUsed.ifEmpty { listOf("On-device memory & local prompt") }.joinToString(", "))
                ExplainRow("AI USED", record.providerType.name)
                ExplainRow("WHERE IT RAN", when (record.privacyLevel) {
                    PrivacyLevel.LOCAL_ONLY -> "100% On-Device (Isolated Sandbox)"
                    PrivacyLevel.PRIVATE -> "On-Device / Private Gateway"
                    PrivacyLevel.SENSITIVE -> "Private Server Tunnel"
                    PrivacyLevel.PUBLIC -> "Cloud Engine"
                })
                ExplainRow("CONFIDENCE", "${(record.confidenceScore * 100).toInt()}% Verified")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("dismiss_explanation_btn")
            ) {
                Text("Got it")
            }
        }
    )
}

@Composable
private fun ExplainRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
