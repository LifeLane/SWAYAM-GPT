package com.example.edgeaicore.ui.common

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edgeaicore.core.analytics.LocalAnalyticsProvider
import com.example.edgeaicore.core.analytics.ToolFunctionType
import com.example.edgeaicore.core.analytics.ToolTimeSeriesPoint
import com.example.ui.theme.*

enum class ChartTimeRange(val label: String, val days: Int) {
    HOURS_24("24 Hours", 1),
    DAYS_7("7 Days", 7),
    DAYS_30("30 Days", 30)
}

/**
 * Recharts-style Interactive Tool Functions Usage Frequency Visualization.
 * Renders high-fidelity stacked time-series bar & area curves, tool distribution cards,
 * interactive day/point inspect tooltips, and real-time live event reactivity.
 */
@Composable
fun ToolUsageFrequencyChart(
    analytics: LocalAnalyticsProvider,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableStateOf(ChartTimeRange.DAYS_7) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    val history by analytics.toolUsageHistory.collectAsStateWithLifecycle()

    val timeSeriesPoints = remember(history, selectedRange) {
        analytics.getTimeSeriesPoints(selectedRange.days)
    }

    val distribution = remember(history) {
        analytics.getToolDistribution()
    }

    val totalInvocations = remember(distribution) {
        distribution.values.sum().coerceAtLeast(1)
    }

    val activePoint = selectedPointIndex?.let { idx ->
        timeSeriesPoints.getOrNull(idx)
    } ?: timeSeriesPoints.lastOrNull()

    AppCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .testTag("tool_usage_frequency_chart_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header & Range Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "SWAYAM GPT TOOL FREQUENCY",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Text(
                        text = "Real-time usage volume over time (Copy, Translate, Share, Export)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                // Range Selector Pills
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ChartTimeRange.values().forEach { range ->
                        val isSelected = selectedRange == range
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedRange = range }
                        ) {
                            Text(
                                text = range.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Interactive Tooltip / Active Point Metric Pill
            activePoint?.let { point ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${point.timeLabel} Activity",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${point.totalCount} total tool executions",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Mini metrics row
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToolMiniBadge(label = "Copy", count = point.copyCount, color = Color(0xFF4285F4))
                            ToolMiniBadge(label = "Trans", count = point.translateCount, color = Color(0xFF0F9D58))
                            ToolMiniBadge(label = "Share", count = point.shareCount, color = Color(0xFFF4B400))
                            ToolMiniBadge(label = "Export", count = point.exportCount, color = Color(0xFF9C27B0))
                            ToolMiniBadge(label = "Voice", count = point.voiceCount, color = Color(0xFFFF7043))
                        }
                    }
                }
            }

            // Stacked Bar & Trend Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                RechartsBarCanvas(
                    points = timeSeriesPoints,
                    selectedIndex = selectedPointIndex,
                    onSelectIndex = { selectedPointIndex = it }
                )
            }

            // Color-Coded Tool Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(label = "Copy", color = Color(0xFF4285F4))
                LegendItem(label = "Translate", color = Color(0xFF0F9D58))
                LegendItem(label = "Share", color = Color(0xFFF4B400))
                LegendItem(label = "Export", color = Color(0xFF9C27B0))
                LegendItem(label = "Voice", color = Color(0xFFFF7043))
                LegendItem(label = "Regen", color = Color(0xFFEA4335))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Tool Distribution Cards (Grid breakdown)
            Text(
                text = "TOOL ADOPTION BREAKDOWN",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val copyCount = distribution[ToolFunctionType.COPY] ?: 0
                    ToolDistributionCard(
                        title = "Copy Tool",
                        count = copyCount,
                        percent = (copyCount.toFloat() / totalInvocations * 100).toInt(),
                        color = Color(0xFF4285F4),
                        icon = Icons.Default.ContentCopy,
                        modifier = Modifier.weight(1f)
                    )

                    val translateCount = distribution[ToolFunctionType.TRANSLATE] ?: 0
                    ToolDistributionCard(
                        title = "Translate (HI/BN)",
                        count = translateCount,
                        percent = (translateCount.toFloat() / totalInvocations * 100).toInt(),
                        color = Color(0xFF0F9D58),
                        icon = Icons.Default.Translate,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val shareCount = distribution[ToolFunctionType.SHARE] ?: 0
                    ToolDistributionCard(
                        title = "Share Intent",
                        count = shareCount,
                        percent = (shareCount.toFloat() / totalInvocations * 100).toInt(),
                        color = Color(0xFFF4B400),
                        icon = Icons.Default.Share,
                        modifier = Modifier.weight(1f)
                    )

                    val exportCount = distribution[ToolFunctionType.EXPORT] ?: 0
                    ToolDistributionCard(
                        title = "Vault Export",
                        count = exportCount,
                        percent = (exportCount.toFloat() / totalInvocations * 100).toInt(),
                        color = Color(0xFF9C27B0),
                        icon = Icons.Default.SaveAlt,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val voiceCount = distribution[ToolFunctionType.VOICE_MIC] ?: 0
                    ToolDistributionCard(
                        title = "Voice Speech-to-Text",
                        count = voiceCount,
                        percent = (voiceCount.toFloat() / totalInvocations * 100).toInt(),
                        color = Color(0xFFFF7043),
                        icon = Icons.Default.Mic,
                        modifier = Modifier.weight(1f)
                    )

                    val regenCount = distribution[ToolFunctionType.REGENERATE] ?: 0
                    ToolDistributionCard(
                        title = "Regenerate Core",
                        count = regenCount,
                        percent = (regenCount.toFloat() / totalInvocations * 100).toInt(),
                        color = Color(0xFFEA4335),
                        icon = Icons.Default.Refresh,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RechartsBarCanvas(
    points: List<ToolTimeSeriesPoint>,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit
) {
    if (points.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No tool invocations recorded yet", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val maxTotal = remember(points) {
        points.maxOfOrNull { it.totalCount }?.coerceAtLeast(10) ?: 10
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val barWidthWithGap = size.width / points.size.toFloat()
                    val tappedIndex = (offset.x / barWidthWithGap).toInt().coerceIn(0, points.size - 1)
                    onSelectIndex(tappedIndex)
                }
            }
    ) {
        val w = size.width
        val h = size.height - 24.dp.toPx() // Reserve space for x-axis labels
        val barCount = points.size
        val totalSlotWidth = w / barCount.toFloat()
        val barWidth = (totalSlotWidth * 0.58f).coerceAtLeast(10f).coerceAtMost(32f)

        // Draw horizontal grid lines
        val gridLines = 3
        for (i in 0..gridLines) {
            val y = h - (i * (h / gridLines.toFloat()))
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        // Draw stacked bars for each point
        points.forEachIndexed { index, point ->
            val centerX = (index * totalSlotWidth) + (totalSlotWidth / 2f)
            val barLeft = centerX - (barWidth / 2f)
            val isSelected = selectedIndex == index

            val total = point.totalCount.toFloat().coerceAtLeast(0.5f)
            val heightRatio = (total / maxTotal.toFloat()).coerceIn(0.05f, 1.0f)
            val totalBarHeight = h * heightRatio

            // Base highlight background if selected
            if (isSelected) {
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.12f),
                    topLeft = Offset(centerX - (totalSlotWidth / 2f), 0f),
                    size = Size(totalSlotWidth, h + 20.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }

            // Stacked tool components
            var currentY = h
            val colors = listOf(
                Color(0xFF4285F4) to point.copyCount,
                Color(0xFF0F9D58) to point.translateCount,
                Color(0xFFF4B400) to point.shareCount,
                Color(0xFF9C27B0) to point.exportCount,
                Color(0xFFFF7043) to point.voiceCount,
                Color(0xFFEA4335) to point.regenerateCount
            )

            for ((color, count) in colors) {
                if (count > 0 && total > 0) {
                    val segmentHeight = (count.toFloat() / total) * totalBarHeight
                    val segmentTop = currentY - segmentHeight
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(barLeft, segmentTop),
                        size = Size(barWidth, segmentHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    currentY = segmentTop
                }
            }

            // Indicator dot at the top of the bar
            if (isSelected) {
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = Offset(centerX, currentY - 6.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun ToolMiniBadge(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToolDistributionCard(
    title: String,
    count: Int,
    percent: Int,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$count uses",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$percent%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
