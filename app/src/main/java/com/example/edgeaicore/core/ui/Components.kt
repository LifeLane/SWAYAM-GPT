package com.example.edgeaicore.core.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * Geometric Balance Card Container
 */
@Composable
fun EdgeCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
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
 * Signature Geometric Hero Card with radial dot matrix, concentric dashed rings,
 * and 45-degree diamond icon badge.
 */
@Composable
fun GeometricHeroCard(
    modifier: Modifier = Modifier,
    title: String = "Engine Initialized",
    subtitle: String = "Core kernel is active and listening for architectural instructions.",
    statusText: String = "Awaiting Command",
    icon: ImageVector = Icons.Default.Bolt,
    onStatusClick: (() -> Unit)? = null
) {
    val dotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "hero_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(32.dp))
            .drawBehind {
                val step = 20.dp.toPx()
                var x = step / 2
                while (x < size.width) {
                    var y = step / 2
                    while (y < size.height) {
                        drawCircle(
                            color = dotColor,
                            radius = 1.5.dp.toPx(),
                            center = Offset(x, y)
                        )
                        y += step
                    }
                    x += step
                }
            },
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Concentric Rings & Rotated Diamond Badge
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.2f),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
                // Inner dashed ring with pulse
                Canvas(modifier = Modifier.size(126.dp)) {
                    drawCircle(
                        color = primaryColor.copy(alpha = pulseAlpha),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                        )
                    )
                }
                // 45-degree diamond center badge
                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .rotate(45f),
                    shape = RoundedCornerShape(20.dp),
                    color = primaryColor,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(34.dp)
                                .rotate(-45f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Pill Status Badge with pinging dot
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .clip(CircleShape)
                    .then(if (onStatusClick != null) Modifier.clickable(onClick = onStatusClick) else Modifier)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                    Text(
                        text = statusText.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }
    }
}

/**
 * Geometric Balance 2-column Metric Card
 */
@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    progress: Float? = null,
    activeDots: Pair<Int, Int>? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.size(1.dp))
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeColor
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Visual balance indicators (Progress bar or Node mesh dots)
            if (progress != null) {
                Spacer(modifier = Modifier.height(2.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = badgeColor,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
            } else if (activeDots != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val (active, total) = activeDots
                    for (i in 0 until total) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (i < active) badgeColor else MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Geometric Console / Terminal Card
 */
@Composable
fun GeometricTerminalCard(
    modifier: Modifier = Modifier,
    lines: List<String> = listOf("CORE_READY: true", "LOCAL_MEMORY: SYNCHRONIZED", "LITERT_ENGINE: STANDBY")
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorVisible by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_blink"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, GeoTerminalBorder, RoundedCornerShape(24.dp)),
        color = GeoTerminalDark,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            lines.forEach { line ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeoTerminalPrompt
                    )
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = GeoTerminalText
                    )
                }
            }

            // Blinking prompt line
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeoTerminalPrompt
                )
                Text(
                    text = if (cursorVisible > 0.5f) "_" else " ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeoTerminalPrompt
                )
            }
        }
    }
}

/**
 * Geometric Status Pill
 */
@Composable
fun AIStatusPill(
    providerType: AIProviderType,
    modifier: Modifier = Modifier
) {
    val (label, bg, fg, icon) = when (providerType) {
        AIProviderType.LOCAL -> Quad("LOCAL AI", LocalAIGreenContainer, LocalAIGreen, Icons.Default.Memory)
        AIProviderType.PRIVATE_SERVER -> Quad("PRIVATE AI", PrivateServerAmberContainer, PrivateServerAmber, Icons.Default.Dns)
        AIProviderType.CLOUD -> Quad("CLOUD AI", CloudAIPurpleContainer, CloudAIPurple, Icons.Default.Cloud)
        AIProviderType.DEMO -> Quad("DEMO ACTIVE", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Icons.Default.PlayArrow)
    }

    Surface(
        modifier = modifier.clip(CircleShape),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = fg,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
fun PrivacyBadge(
    level: PrivacyLevel,
    modifier: Modifier = Modifier
) {
    val (label, bg, fg) = when (level) {
        PrivacyLevel.LOCAL_ONLY -> Triple("LOCAL ONLY", LocalAIGreenContainer, LocalAIGreen)
        PrivacyLevel.SENSITIVE -> Triple("SENSITIVE", PrivateServerAmberContainer, PrivateServerAmber)
        PrivacyLevel.PRIVATE -> Triple("PRIVATE", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface)
        PrivacyLevel.PUBLIC -> Triple("PUBLIC", CloudAIPurpleContainer, CloudAIPurple)
    }

    Surface(
        modifier = modifier.clip(CircleShape),
        color = bg
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = fg,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun RiskBadge(
    risk: RiskLevel,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (risk) {
        RiskLevel.NONE -> Pair("NO RISK", LocalAIGreen)
        RiskLevel.LOW -> Pair("LOW RISK", RiskLow)
        RiskLevel.MEDIUM -> Pair("MEDIUM RISK", RiskMedium)
        RiskLevel.HIGH -> Pair("HIGH RISK", RiskHigh)
        RiskLevel.CRITICAL -> Pair("CRITICAL", RiskHigh)
    }
    Surface(
        modifier = modifier.clip(CircleShape),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun OfflineBanner(
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = OfflineGray
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OFFLINE MODE — Full Local AI & Memory Active",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun AIThinkingIndicator(
    text: String = "LiteRT-LM Inferring...",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun ExplanationModal(
    record: ExplanationRecord?,
    onDismiss: () -> Unit
) {
    if (record == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AI Transparency & Provenance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExplainItem(title = "Feature", value = record.featureName)
                ExplainItem(title = "What Happened?", value = record.whatHappened)
                ExplainItem(title = "Why?", value = record.whyReason)
                ExplainItem(title = "Confidence", value = "${(record.confidenceScore * 100).toInt()}% Verified")
                ExplainItem(title = "Data Sources Used", value = record.dataSourcesUsed.joinToString(", "))
                ExplainItem(title = "Inference Engine", value = record.providerType.name)
                ExplainItem(title = "Privacy Boundary", value = record.privacyLevel.name)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("dismiss_explanation_button")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ExplainItem(title: String, value: String) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
