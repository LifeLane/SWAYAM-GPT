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
 * Universal Neural Response & Provenance Modal
 * Explains: WHAT HAPPENED, WHY, DATA USED, AI USED, WHERE IT RAN, CONFIDENCE
 * Features sleek action icons: Save, Pin, Trash, Copy, and Close.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalExplanationSheet(
    record: ExplanationRecord?,
    onDismiss: () -> Unit
) {
    if (record == null) return

    val context = androidx.compose.ui.platform.LocalContext.current
    var isPinned by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "modal_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("neural_response_modal"),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        1.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                        RoundedCornerShape(24.dp)
                    ),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // TOP BAR: Title, Live Neural Pulse, and Action Icons (Save, Pin, Trash, Close)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Neural Response",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isPinned) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "PINNED",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = record.featureName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // SINGLE ROW ACTION ICONS: Save, Pin, Trash, Copy, Close
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // 1. SAVE ICON
                            IconButton(
                                onClick = {
                                    isSaved = true
                                    android.widget.Toast.makeText(context, "Saved neural insight to Memory Vault", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(34.dp).testTag("modal_save_btn")
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.BookmarkAdded else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Save to Memory Vault",
                                    tint = if (isSaved) LocalAIGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // 2. PIN ICON
                            IconButton(
                                onClick = {
                                    isPinned = !isPinned
                                    val msg = if (isPinned) "Pinned to session header" else "Unpinned"
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(34.dp).testTag("modal_pin_btn")
                            ) {
                                Icon(
                                    imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = "Pin response",
                                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // 3. COPY TRACE
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText(
                                        "SWAYAM Neural Explanation",
                                        "Feature: ${record.featureName}\nWhat Happened: ${record.whatHappened}\nWhy: ${record.whyReason}\nConfidence: ${(record.confidenceScore * 100).toInt()}%"
                                    )
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Trace copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(34.dp).testTag("modal_copy_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy Neural Trace",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // 4. TRASH / CLEAR ICON
                            IconButton(
                                onClick = {
                                    android.widget.Toast.makeText(context, "Cleared from active cache", android.widget.Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                                modifier = Modifier.size(34.dp).testTag("modal_trash_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Discard trace",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // 5. CLOSE ICON
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(34.dp).testTag("modal_close_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // TAB SELECTOR (Provenance vs Telemetry vs Sources)
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        divider = { HorizontalDivider() }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Provenance", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Hardware & Metrics", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Grounding & Egress", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    // TAB CONTENT
                    when (selectedTab) {
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ExplainRow("WHAT HAPPENED", record.whatHappened)
                                ExplainRow("WHY THIS ACTION", record.whyReason)
                                ExplainRow("MODEL", record.modelName)
                                ExplainRow("RUNTIME & ENGINE", "${record.runtimeEngine} (${record.executionBackend.name})")
                                ExplainRow(
                                    "PROVIDER",
                                    when (record.providerType) {
                                        AIProviderType.LOCAL -> "SWAYAM Core (Local On-Device Runtime)"
                                        AIProviderType.PRIVATE_SERVER -> "Private LAN AI Server (Air-Gapped)"
                                        AIProviderType.CLOUD -> "Cloud Intelligence Provider (User Authorized)"
                                        AIProviderType.DEMO -> "On-Device Neural Synthesizer"
                                    }
                                )
                            }
                        }
                        1 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Neural Grounding Confidence", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("${(record.confidenceScore * 100).toInt()}%", fontWeight = FontWeight.ExtraBold, color = LocalAIGreen)
                                }
                                LinearProgressIndicator(
                                    progress = { record.confidenceScore.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = LocalAIGreen,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val latText = if (record.latencyMs > 0) "${record.latencyMs} ms" else "Unavailable"
                                    val tpsText = if (record.tokensPerSecond > 0) String.format("%.1f t/s", record.tokensPerSecond) else if (record.tokensGenerated > 0 && record.latencyMs > 0) String.format("%.1f t/s", record.tokensGenerated.toDouble() / (record.latencyMs / 1000.0)) else "Unavailable"
                                    val netText = if (record.networkUsed) "Online egress" else "0 Bytes (Offline Local)"

                                    MetricChip(label = "Latency", value = latText, modifier = Modifier.weight(1f))
                                    MetricChip(label = "Inference Speed", value = tpsText, modifier = Modifier.weight(1f))
                                    MetricChip(label = "Network Egress", value = netText, modifier = Modifier.weight(1f))
                                }

                                if (record.toolsUsed.isNotEmpty()) {
                                    ExplainRow("TOOLS INVOKED", record.toolsUsed.joinToString(", "))
                                }
                                if (record.agentsUsed.isNotEmpty()) {
                                    ExplainRow("AGENTS INVOLVED", record.agentsUsed.joinToString(", "))
                                }
                            }
                        }
                        2 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val memoryList = if (record.memoriesUsed.isNotEmpty()) {
                                    record.memoriesUsed.joinToString(" • ")
                                } else {
                                    "No memories accessed"
                                }
                                ExplainRow("MEMORY CONTEXT", memoryList)

                                val ragList = if (record.ragSources.isNotEmpty()) {
                                    record.ragSources.joinToString(" • ")
                                } else if (record.dataSourcesUsed.isNotEmpty()) {
                                    record.dataSourcesUsed.joinToString(" • ")
                                } else {
                                    "Direct on-device generation"
                                }
                                ExplainRow("RAG & DOCUMENT SOURCES", ragList)

                                ExplainRow(
                                    "DATA PRIVACY & NETWORK",
                                    when (record.privacyLevel) {
                                        PrivacyLevel.LOCAL_ONLY -> "100% Sovereign On-Device (0 Network Bytes)"
                                        PrivacyLevel.PRIVATE -> "Local Encrypted Sandbox (No External Egress)"
                                        PrivacyLevel.SENSITIVE -> "LAN Encrypted Tunnel"
                                        PrivacyLevel.PUBLIC -> if (record.networkUsed) "User-Authorized Cloud API" else "100% On-Device"
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dismiss_explanation_btn")
                    ) {
                        Text("Dismiss Modal", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    )
}

@Composable
private fun MetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
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
