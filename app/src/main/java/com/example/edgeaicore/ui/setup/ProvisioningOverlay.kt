package com.example.edgeaicore.ui.setup

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.models.ProvisioningProgress
import com.example.edgeaicore.core.models.ProvisioningStage
import com.example.ui.theme.LocalAIGreen

/**
 * First-Launch / Preparing Private AI Environment Banner & Modal.
 * Shows unambiguous, non-technical progress when provisioning local models.
 */
@Composable
fun ProvisioningOverlay(
    edgeAI: EdgeAICore,
    modifier: Modifier = Modifier
) {
    val progress by edgeAI.provisioning.progress.collectAsStateWithLifecycle()

    if (progress.stage == ProvisioningStage.READY) {
        return
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
            .testTag("provisioning_overlay_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f), RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. BRAND ICON & PULSE
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (progress.stage == ProvisioningStage.ERROR) Icons.Default.ErrorOutline else Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = if (progress.stage == ProvisioningStage.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // 2. HEADER
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "SWAYAM GPT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = if (progress.stage == ProvisioningStage.ERROR) "Setup Paused" else "Preparing your private AI environment",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Internet is required once to download the local AI model. After setup, SWAYAM operates 100% offline.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 3. PROGRESS BAR & STEP STATUS
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = progress.currentStepText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (progress.progress > 0f) {
                                Text(
                                    text = "${(progress.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { progress.progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (progress.stage == ProvisioningStage.ERROR) MaterialTheme.colorScheme.error else LocalAIGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Real Download Stats (Speed, Remaining Bytes, ETA)
                        if (progress.stage == ProvisioningStage.DOWNLOADING && progress.totalBytes > 0) {
                            val mbDownloaded = progress.bytesDownloaded / (1024.0 * 1024.0)
                            val mbTotal = progress.totalBytes / (1024.0 * 1024.0)
                            val speedKb = progress.downloadSpeedBytesPerSec / 1024.0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = String.format("%.1f / %.1f MB", mbDownloaded, mbTotal),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (speedKb > 1024) String.format("%.1f MB/s", speedKb / 1024.0) else String.format("%.0f KB/s", speedKb),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 4. STAGES CHECKLIST
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StageRow(
                            label = "Device Compatibility & RAM Check",
                            isComplete = progress.stage.ordinal > ProvisioningStage.CHECKING_DEVICE.ordinal,
                            isActive = progress.stage == ProvisioningStage.CHECKING_DEVICE
                        )
                        StageRow(
                            label = "Storage Verification (~1.5 GB free)",
                            isComplete = progress.stage.ordinal > ProvisioningStage.CHECKING_STORAGE.ordinal,
                            isActive = progress.stage == ProvisioningStage.CHECKING_STORAGE
                        )
                        StageRow(
                            label = "Neural Weights & Vector Embeddings",
                            isComplete = progress.stage.ordinal > ProvisioningStage.DOWNLOADING.ordinal,
                            isActive = progress.stage == ProvisioningStage.DOWNLOADING
                        )
                        StageRow(
                            label = "Cryptographic Checksum & Safety Verification",
                            isComplete = progress.stage.ordinal > ProvisioningStage.VERIFYING.ordinal,
                            isActive = progress.stage == ProvisioningStage.VERIFYING
                        )
                        StageRow(
                            label = "On-Device Inference Self-Test",
                            isComplete = progress.selfTestPassed,
                            isActive = progress.stage == ProvisioningStage.RUNNING_SELF_TEST
                        )
                    }

                    // 5. DEVICE SPECS PILL
                    progress.deviceSpecs?.let { specs ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target: ${progress.activeModelName.ifBlank { "Gemma 2B IT" }}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${specs.recommendedBackend.name} Accelerated",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 6. ACTION BUTTON (Retry or Manual)
                    if (progress.stage == ProvisioningStage.ERROR || progress.canRetry) {
                        Button(
                            onClick = { edgeAI.provisioning.retryProvisioning() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("provisioning_retry_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Local AI Setup", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StageRow(
    label: String,
    isComplete: Boolean,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isComplete) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = LocalAIGreen,
                modifier = Modifier.size(16.dp)
            )
        } else if (isActive) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.RadioButtonUnchecked,
                contentDescription = "Pending",
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isActive || isComplete) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isComplete) MaterialTheme.colorScheme.onSurface else if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
