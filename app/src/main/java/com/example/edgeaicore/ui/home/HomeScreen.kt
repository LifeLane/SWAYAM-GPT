package com.example.edgeaicore.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.explanation.ExplanationRecord
import com.example.edgeaicore.core.memory.MemoryEntity
import com.example.edgeaicore.ui.common.AIStatus
import com.example.edgeaicore.ui.common.AIStatusCard
import com.example.edgeaicore.ui.common.AppCard
import com.example.edgeaicore.ui.common.SwayamBrandHeader
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    edgeAI: EdgeAICore,
    onNavigateToAsk: (String) -> Unit,
    onNavigateToCapture: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToAgent: (String?) -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToDocumentIntel: () -> Unit = {},
    onNavigateToBenchmark: () -> Unit = {},
    onNavigateToAudioJournal: () -> Unit = {},
    onNavigateToRoutines: () -> Unit = {},
    onOpenOperatingCenter: () -> Unit = {},
    onShowExplanation: (ExplanationRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val privacyState by edgeAI.privacy.state.collectAsStateWithLifecycle()
    val isDemoMode by edgeAI.demo.isDemoMode.collectAsStateWithLifecycle()
    val memoryCount by edgeAI.memory.count.collectAsStateWithLifecycle(initialValue = 0)
    val latestMemories: List<MemoryEntity> by edgeAI.memory.activeMemories.collectAsStateWithLifecycle(initialValue = emptyList())
    val lastAgentResult by edgeAI.agent.lastResult.collectAsStateWithLifecycle()
    val specs = remember { edgeAI.diagnostics.specs() }

    var quickInputText by remember { mutableStateOf("") }

    // Dynamic Time-Based Greeting
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "GOOD MORNING"
            in 12..16 -> "GOOD AFTERNOON"
            in 17..21 -> "GOOD EVENING"
            else -> "WELCOME BACK"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. GREETING & HEADER
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SwayamBrandHeader(
                    subtitle = "Personal On-Device Sovereign AI Core"
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Personal AI Operating Center",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = onOpenOperatingCenter,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), CircleShape)
                            .testTag("home_open_engine_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = "Engine Control", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // 2. REUSABLE AI STATUS COMPONENT
        item {
            AIStatus(
                providerType = if (isDemoMode) AIProviderType.DEMO else if (privacyState.cloudAiEnabled) AIProviderType.CLOUD else AIProviderType.LOCAL,
                isOffline = !privacyState.cloudAiEnabled && !privacyState.privateServerEnabled,
                isDemo = isDemoMode,
                hardwareAccelerator = "${specs.recommendedBackend.name} ACCELERATED",
                onClick = onOpenOperatingCenter
            )
        }

        // 3. PRIMARY PROMPT ACTION BAR ("How can I help?")
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    TextField(
                        value = quickInputText,
                        onValueChange = { quickInputText = it },
                        placeholder = {
                            Text(
                                text = "Ask anything or describe a goal...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            if (quickInputText.isNotBlank()) {
                                onNavigateToAsk(quickInputText)
                                quickInputText = ""
                            }
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_search_input")
                    )
                    IconButton(
                        onClick = {
                            if (quickInputText.isNotBlank()) {
                                onNavigateToAsk(quickInputText)
                                quickInputText = ""
                            } else {
                                onNavigateToCapture()
                            }
                        },
                        modifier = Modifier.testTag("home_submit_btn")
                    ) {
                        Icon(
                            imageVector = if (quickInputText.isNotBlank()) Icons.Default.ArrowForward else Icons.Default.CameraAlt,
                            contentDescription = "Submit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 4. QUICK ACTIONS BAR
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "QUICK ACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        label = "Ask",
                        icon = Icons.Default.ChatBubbleOutline,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToAsk("") }
                    )
                    QuickActionButton(
                        label = "Capture",
                        icon = Icons.Default.CameraAlt,
                        color = LocalAIGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCapture
                    )
                    QuickActionButton(
                        label = "Remember",
                        icon = Icons.Default.BookmarkAdd,
                        color = PrivateServerAmber,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMemory
                    )
                    QuickActionButton(
                        label = "Agent",
                        icon = Icons.Default.SmartToy,
                        color = CloudAIBorder,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToAgent(null) }
                    )
                }
            }
        }

        // 5. CONTEXT & RECENT ACTIVITY CARDS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "CONTEXT & INTELLIGENCE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                // Recent Memory Card
                val latestMem = latestMemories.firstOrNull()
                AppCard(
                    onClick = onNavigateToMemory,
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
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
                                shape = CircleShape,
                                color = LocalAIGreen.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = LocalAIGreen, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text(text = "Recent Memory", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "$memoryCount items indexed in private vault",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (latestMem != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = latestMem.content.take(90) + if (latestMem.content.length > 90) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (latestMem.tags.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tags: ${latestMem.tags}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LocalAIGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Document Intelligence & RAG Vault Card
                AppCard(
                    onClick = onNavigateToDocumentIntel,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    modifier = Modifier.testTag("home_doc_intel_card")
                ) {
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
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text(text = "Document Intelligence & RAG Vault", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Ingested PDFs, markdown notes & semantic source citations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Offline Audio Journaling Card
                AppCard(
                    onClick = onNavigateToAudioJournal,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("home_audio_journal_card")
                ) {
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
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text(text = "Offline Audio Journal & STT", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "On-device speech transcription & speaker diarization",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Autonomous Routines & Triggers Card
                AppCard(
                    onClick = onNavigateToRoutines,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("home_routines_card")
                ) {
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
                                shape = CircleShape,
                                color = PrivateServerAmber.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = PrivateServerAmber, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text(text = "Autonomous Routines & Briefs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Morning briefing, geofences & overnight vector maintenance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Recent Agent Activity Card
                AppCard(
                    onClick = { onNavigateToAgent(null) },
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
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
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.AutoMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text(text = "Autonomous Agent", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (lastAgentResult != null) "Last: ${lastAgentResult?.profile?.name ?: "Assistant"} • ${lastAgentResult?.steps?.size ?: 0} steps" else "Ready to orchestrate tools and goals",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // 3. Device Health & Edge Hardware Status Card
                AppCard(
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    shape = CircleShape,
                                    color = LocalAIGreen.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.HealthAndSafety,
                                            contentDescription = null,
                                            tint = LocalAIGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Device Health & Hardware Status",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "All edge neural subsystems healthy",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LocalAIGreen.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "OPTIMAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAIGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Compact specs grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "RAM ALLOCATED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${specs.totalRamMb} MB",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "ACCELERATION",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = specs.recommendedBackend.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "STORAGE VAULT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Encrypted",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Tools & Connected Ecosystem
                AppCard(
                    onClick = onNavigateToTools,
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
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
                                shape = CircleShape,
                                color = PrivateServerAmber.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.Extension, contentDescription = null, tint = PrivateServerAmber, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text(text = "On-Device Tools", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Vision, Memory, Notes, Device & Automations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("quick_action_$label"),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
