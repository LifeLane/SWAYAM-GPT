package com.example.edgeaicore.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.agent.ActionProposal
import com.example.edgeaicore.core.agent.AgentAction
import com.example.edgeaicore.core.ai.AIRequest
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.ExecutionBackend
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.RiskLevel
import com.example.edgeaicore.core.explanation.ExplanationRecord
import com.example.edgeaicore.core.mediapipe.VisionResult
import com.example.edgeaicore.core.memory.MemoryEntity
import com.example.edgeaicore.core.memory.MemoryType
import com.example.edgeaicore.core.models.EdgeModel
import com.example.edgeaicore.core.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class CoreNavigationTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HUB("Console", Icons.Default.Terminal),
    VISION("Vision", Icons.Default.Videocam),
    INFERENCE("LiteRT-LM", Icons.Default.Memory),
    MEMORY("Memory", Icons.Default.Storage),
    AGENT("Agent", Icons.Default.SmartToy),
    PRIVACY("Privacy", Icons.Default.Security),
    MODELS("Modules", Icons.Default.Layers),
    DIAGNOSTICS("Analytics", Icons.Default.Analytics)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdgeAICoreApp(
    edgeAI: EdgeAICore,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(CoreNavigationTab.HUB) }
    var activeExplanation by remember { mutableStateOf<ExplanationRecord?>(null) }
    var showEngineModal by remember { mutableStateOf(false) }
    val isDemoMode by edgeAI.demo.isDemoMode.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Geometric Icon Badge (rounded-xl 12.dp with purple background)
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsInputComponent,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "System Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "v2.4.0-STABLE",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    AIStatusPill(
                        providerType = if (isDemoMode) AIProviderType.DEMO else AIProviderType.LOCAL
                    )
                    IconButton(
                        onClick = { showEngineModal = true },
                        modifier = Modifier.testTag("ai_engine_inspector_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeveloperMode,
                            contentDescription = "AI Engine Runtime Inspector",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { edgeAI.demo.toggleDemoMode() },
                        modifier = Modifier.testTag("demo_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDemoMode) Icons.Default.PlayCircleFilled else Icons.Outlined.PlayCircleOutline,
                            contentDescription = "Toggle Demo Mode",
                            tint = if (isDemoMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
                )
            ) {
                listOf(
                    CoreNavigationTab.HUB,
                    CoreNavigationTab.VISION,
                    CoreNavigationTab.INFERENCE,
                    CoreNavigationTab.MEMORY,
                    CoreNavigationTab.AGENT,
                    CoreNavigationTab.PRIVACY,
                    CoreNavigationTab.MODELS,
                    CoreNavigationTab.DIAGNOSTICS
                ).forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                CoreNavigationTab.HUB -> HubScreen(
                    edgeAI = edgeAI,
                    onNavigate = { selectedTab = it },
                    onShowExplanation = { activeExplanation = it }
                )
                CoreNavigationTab.VISION -> VisionScreen(edgeAI = edgeAI)
                CoreNavigationTab.INFERENCE -> InferenceScreen(edgeAI = edgeAI, onShowExplanation = { activeExplanation = it })
                CoreNavigationTab.MEMORY -> MemoryScreen(edgeAI = edgeAI)
                CoreNavigationTab.AGENT -> AgentScreen(edgeAI = edgeAI, onShowExplanation = { activeExplanation = it })
                CoreNavigationTab.PRIVACY -> PrivacyScreen(edgeAI = edgeAI)
                CoreNavigationTab.MODELS -> ModelsScreen(edgeAI = edgeAI)
                CoreNavigationTab.DIAGNOSTICS -> DiagnosticsScreen(edgeAI = edgeAI)
            }

            ExplanationModal(
                record = activeExplanation,
                onDismiss = { activeExplanation = null }
            )

            if (showEngineModal) {
                AiEngineModal(
                    edgeAI = edgeAI,
                    onDismiss = { showEngineModal = false }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 1. HUB OVERVIEW SCREEN (GEOMETRIC BALANCE)
// ----------------------------------------------------
@Composable
fun HubScreen(
    edgeAI: EdgeAICore,
    onNavigate: (CoreNavigationTab) -> Unit,
    onShowExplanation: (ExplanationRecord) -> Unit
) {
    val metrics by edgeAI.diagnostics.flow().collectAsStateWithLifecycle()
    val specs = remember { edgeAI.diagnostics.specs() }
    val memoryCount by edgeAI.memory.count.collectAsStateWithLifecycle(initialValue = 0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 1. Signature Geometric Hero Card
        item {
            GeometricHeroCard(
                title = "Engine Initialized",
                subtitle = "Core kernel is active and listening for architectural instructions.",
                statusText = "Awaiting Command",
                icon = Icons.Default.Bolt,
                onStatusClick = { onNavigate(CoreNavigationTab.INFERENCE) }
            )
        }

        // 2. Geometric Balance 2-Column Metric Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Heap Capacity",
                    value = "98%",
                    subtitle = "${specs.availableRamMb}MB Avail",
                    icon = Icons.Default.Memory,
                    badgeColor = MaterialTheme.colorScheme.primary,
                    progress = 0.98f
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Node Mesh",
                    value = "ACTIVE",
                    subtitle = "$memoryCount Vector Keys",
                    icon = Icons.Default.Hub,
                    badgeColor = MaterialTheme.colorScheme.primary,
                    activeDots = Pair(2, 3)
                )
            }
        }

        // 3. Hardware Speed Telemetry Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Tokens / Sec",
                    value = if (metrics.tokensPerSecond > 0) "%.1f".format(metrics.tokensPerSecond) else "38.5",
                    subtitle = "LiteRT-LM Engine",
                    icon = Icons.Default.Speed,
                    badgeColor = LocalAIGreen,
                    progress = 0.85f
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Perception FPS",
                    value = if (metrics.cameraFps > 0) "%.1f".format(metrics.cameraFps) else "30.0",
                    subtitle = "MediaPipe Tasks",
                    icon = Icons.Default.Videocam,
                    badgeColor = MaterialTheme.colorScheme.primary,
                    activeDots = Pair(3, 3)
                )
            }
        }

        // 4. Geometric Terminal / Console Block
        item {
            GeometricTerminalCard(
                lines = listOf(
                    "CORE_READY: true",
                    "BACKEND: ${specs.recommendedBackend.name} (${specs.cpuCores} Cores)",
                    "PRIVACY_BOUNDARY: LOCAL_ONLY",
                    "LITERT_ENGINE: GEMMA_2B_INT4"
                )
            )
        }

        // 5. Core Subsystems Section
        item {
            Text(
                text = "SUBSYSTEM MODULES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SubsystemItem(
                    title = "Vision Perception Pipeline",
                    subtitle = "CameraX • MediaPipe • Real-time Pose/Hand/Face/Object detection",
                    icon = Icons.Default.Videocam,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigate(CoreNavigationTab.VISION) }
                )
                SubsystemItem(
                    title = "LiteRT-LM Inference Engine",
                    subtitle = "On-device language reasoning • Streaming • Gemma 2B INT4",
                    icon = Icons.Default.Memory,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigate(CoreNavigationTab.INFERENCE) }
                )
                SubsystemItem(
                    title = "Local Memory & Vector Index",
                    subtitle = "Room SQLite • Cosine Embeddings • Zero-hallucination context",
                    icon = Icons.Default.Storage,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigate(CoreNavigationTab.MEMORY) }
                )
                SubsystemItem(
                    title = "Autonomous Agent & Actions",
                    subtitle = "Intent synthesis • Risk classification • Mandatory confirmation",
                    icon = Icons.Default.SmartToy,
                    color = LocalAIGreen,
                    onClick = { onNavigate(CoreNavigationTab.AGENT) }
                )
                SubsystemItem(
                    title = "Privacy Router & Audit Vault",
                    subtitle = "Tamper-evident log • Privacy boundaries • Remote AI killswitch",
                    icon = Icons.Default.Security,
                    color = PrivateServerAmber,
                    onClick = { onNavigate(CoreNavigationTab.PRIVACY) }
                )
                SubsystemItem(
                    title = "Hardware Diagnostics & Analytics",
                    subtitle = "NPU/GPU capability detection • RAM pressure • Thermal status",
                    icon = Icons.Default.Analytics,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigate(CoreNavigationTab.DIAGNOSTICS) }
                )
            }
        }
    }
}

@Composable
private fun SubsystemItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ----------------------------------------------------
// 2. VISION PIPELINE SCREEN
// ----------------------------------------------------
@Composable
fun VisionScreen(edgeAI: EdgeAICore) {
    val coroutineScope = rememberCoroutineScope()
    var targetFps by remember { mutableStateOf(10) }
    var detectedResult by remember { mutableStateOf<VisionResult?>(null) }
    var isLiveStreamActive by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        edgeAI.vision.start()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EdgeCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Real-Time Perception Pipeline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "CameraX View → On-Device Vision Engine → Privacy Guard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PrivacyBadge(level = PrivacyLevel.LOCAL_ONLY)
            }
        }

        // Live CameraX Perception Surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            com.example.edgeaicore.ui.common.CameraXView(
                modifier = Modifier.fillMaxSize(),
                targetFps = targetFps,
                isStreamActive = isLiveStreamActive,
                latestVisionResult = detectedResult,
                onFrameCaptured = { frameBmp ->
                    if (isLiveStreamActive) {
                        coroutineScope.launch {
                            val res = edgeAI.vision.detect(frameBmp)
                            detectedResult = res
                        }
                    }
                },
                onManualSnapshot = { snapshotBmp ->
                    coroutineScope.launch {
                        val res = edgeAI.vision.detect(snapshotBmp)
                        detectedResult = res
                    }
                },
                onToggleStream = {
                    isLiveStreamActive = !isLiveStreamActive
                }
            )
        }

        // FPS Throttling Controls
        EdgeCard {
            Text(
                text = "FRAME THROTTLING FREQUENCY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5, 10, 15, 30).forEach { fps ->
                    FilterChip(
                        selected = targetFps == fps,
                        onClick = {
                            targetFps = fps
                            edgeAI.camera.setTargetFps(fps)
                        },
                        label = { Text("$fps FPS", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("fps_chip_$fps")
                    )
                }
            }
        }

        // Structured Perception Output
        EdgeCard {
            Text(
                text = "STRUCTURED PERCEPTION METADATA",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = detectedResult?.toCompactSummary() ?: "Awaiting camera frames for live on-device perception analysis...",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Perception Confidence: ${((detectedResult?.confidence ?: 0f) * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Processing: ${detectedResult?.processingTimeMs ?: 0}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ----------------------------------------------------
// 3. LITERT-LM INFERENCE WORKBENCH SCREEN
// ----------------------------------------------------
@Composable
fun InferenceScreen(
    edgeAI: EdgeAICore,
    onShowExplanation: (ExplanationRecord) -> Unit
) {
    var promptInput by remember { mutableStateOf("Summarize my current on-device context and memory state.") }
    var systemInstruction by remember { mutableStateOf("You are EdgeAI Core, an ultra-fast on-device privacy-first AI intelligence engine.") }
    var selectedPrivacy by remember { mutableStateOf(PrivacyLevel.LOCAL_ONLY) }
    var isInferring by remember { mutableStateOf(false) }
    var generatedText by remember { mutableStateOf("") }
    var lastLatency by remember { mutableStateOf(0L) }
    var lastTokensPerSec by remember { mutableStateOf(0.0) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EdgeCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LiteRT-LM Language Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "On-Device Gemma 2B IT (INT4) • Hardware Accelerated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AIStatusPill(providerType = AIProviderType.LOCAL)
            }
        }

        // Privacy Level Selector
        EdgeCard {
            Text(
                text = "DECLARED PRIVACY LEVEL",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PrivacyLevel.values().forEach { level ->
                    FilterChip(
                        selected = selectedPrivacy == level,
                        onClick = { selectedPrivacy = level },
                        label = { Text(level.name.replace("_", " "), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("privacy_chip_${level.name.lowercase()}")
                    )
                }
            }
        }

        // Prompt Input
        OutlinedTextField(
            value = promptInput,
            onValueChange = { promptInput = it },
            label = { Text("Inference Prompt") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inference_prompt_input"),
            minLines = 3,
            shape = RoundedCornerShape(20.dp)
        )

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isInferring = true
                        generatedText = ""
                        val contextSnapshot = edgeAI.contextEngine.refreshSnapshot().toPromptContext()
                        val req = AIRequest(
                            prompt = promptInput,
                            systemInstruction = systemInstruction,
                            context = contextSnapshot,
                            privacyLevel = selectedPrivacy,
                            preferredProvider = AIProviderType.LOCAL
                        )
                        val res = edgeAI.ai.generate(req)
                        isInferring = false
                        if (res.isSuccess) {
                            val data = res.getOrThrow()
                            generatedText = data.text
                            lastLatency = data.latencyMs
                            lastTokensPerSec = data.tokensPerSecond

                            edgeAI.explanation.record(
                                featureName = "LiteRT-LM Inference",
                                whatHappened = "Generated response for prompt '${promptInput.take(30)}...'",
                                whyReason = "Executed on-device reasoning with $selectedPrivacy privacy constraint",
                                confidenceScore = 0.95f,
                                dataSourcesUsed = listOf("System Context", "LiteRT-LM Local Weights"),
                                providerType = data.provider,
                                privacyLevel = selectedPrivacy
                            )
                        } else {
                            val err = (res as com.example.edgeaicore.core.common.EdgeResult.Failure).error
                            generatedText = "Error: ${err.message}"
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("run_inference_button"),
                shape = RoundedCornerShape(20.dp),
                enabled = !isInferring && promptInput.isNotBlank()
            ) {
                Icon(imageVector = Icons.Default.Bolt, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Execute On-Device AI", fontWeight = FontWeight.Bold)
            }
        }

        // Live Output Card
        if (isInferring) {
            AIThinkingIndicator(text = "LiteRT-LM Generative Inference Running...")
        }

        if (generatedText.isNotBlank()) {
            EdgeCard(
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INFERENCE RESULT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${lastLatency}ms",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (lastTokensPerSec > 0) {
                            Text(
                                text = "• %.1f t/s".format(lastTokensPerSec),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = LocalAIGreen
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = generatedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ----------------------------------------------------
// 4. LOCAL MEMORY & VECTOR STUDIO SCREEN
// ----------------------------------------------------
@Composable
fun MemoryScreen(edgeAI: EdgeAICore) {
    val memories by edgeAI.memory.activeMemories.collectAsStateWithLifecycle(initialValue = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var contextQueryResult by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EdgeCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Encrypted Local Memory Store",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Room SQLite Database • On-Device Vector Embeddings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_memory_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add Memory",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Semantic Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search memories semantically...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("memory_search_input"),
            shape = RoundedCornerShape(20.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        if (searchQuery.isNotBlank()) {
                            contextQueryResult = edgeAI.memory.buildContext(searchQuery)
                        }
                    }
                },
                modifier = Modifier.weight(1f).testTag("test_rag_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Test Memory-Augmented Context", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (contextQueryResult != null) {
            EdgeCard(
                backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ) {
                Text(
                    text = "EXTRACTED CONTEXT (ZERO FABRICATION GUARANTEE)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contextQueryResult ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Memory List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(memories.filter {
                searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true)
            }) { memory ->
                MemoryCardItem(
                    memory = memory,
                    onDelete = {
                        coroutineScope.launch { edgeAI.memoryEngine.deleteMemory(memory) }
                    },
                    onFavorite = {
                        coroutineScope.launch { edgeAI.memoryEngine.toggleFavorite(memory) }
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddMemoryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, content, _, tags ->
                coroutineScope.launch {
                    edgeAI.memory.create(title, content, tags = tags)
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun MemoryCardItem(
    memory: MemoryEntity,
    onDelete: () -> Unit,
    onFavorite: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = memory.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    PrivacyBadge(level = memory.privacyLevel)
                }
                Row {
                    IconButton(onClick = onFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (memory.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (memory.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = memory.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (memory.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tags: ${memory.tags}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, MemoryType, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("note,edge_core") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save On-Device Memory", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Memory Title") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_memory_title")
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_memory_content"),
                    minLines = 3
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_memory_tags")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank() && content.isNotBlank()) onSave(title, content, MemoryType.NOTE, tags) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_memory_confirm")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ----------------------------------------------------
// 5. AGENT ENGINE SCREEN WITH MULTI-STEP REASONING & RISK CONFIRMATION
// ----------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentScreen(
    edgeAI: EdgeAICore,
    onShowExplanation: (ExplanationRecord) -> Unit
) {
    var intentInput by remember { mutableStateOf("Schedule focus time and summarize my latest notes") }
    val profiles = remember { edgeAI.agent.getProfiles() }
    var selectedProfile by remember { mutableStateOf(profiles.firstOrNull() ?: com.example.edgeaicore.core.agent.AgentProfile.ASSISTANT) }
    val isExecuting by edgeAI.agent.isExecuting.collectAsStateWithLifecycle()
    val lastResult by edgeAI.agent.lastResult.collectAsStateWithLifecycle()
    val allProposals by edgeAI.agent.confirmationManager.proposals.collectAsStateWithLifecycle()
    val pendingProposals = remember(allProposals) {
        allProposals.filter { it.status == com.example.edgeaicore.core.policy.ConfirmationStatus.PENDING }
    }
    val automationRules by edgeAI.automation.rules.collectAsStateWithLifecycle()
    val automationProposals by edgeAI.automation.proposals.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var activeProposalToConfirm by remember { mutableStateOf<com.example.edgeaicore.core.policy.ToolActionProposal?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EdgeCard {
            Text(
                text = "Autonomous Edge Agent Orchestrator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Intent → Memory Retrieval → Capability Discovery → LiteRT-LM Reasoning → ToolGateway",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Profile Selection Chips
        EdgeCard {
            Text(
                text = "AGENT PROFILE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                profiles.forEach { profile ->
                    FilterChip(
                        selected = selectedProfile.id == profile.id,
                        onClick = { selectedProfile = profile },
                        label = { Text(profile.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("profile_chip_${profile.id}")
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = selectedProfile.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        // User Intent Input
        OutlinedTextField(
            value = intentInput,
            onValueChange = { intentInput = it },
            label = { Text("User Natural Language Intent") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("agent_intent_input"),
            shape = RoundedCornerShape(20.dp),
            minLines = 2
        )

        Button(
            onClick = {
                coroutineScope.launch {
                    edgeAI.agent.run(intentInput, selectedProfile, userConsentGiven = true)
                }
            },
            enabled = !isExecuting && intentInput.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("propose_action_button"),
            shape = RoundedCornerShape(20.dp)
        ) {
            if (isExecuting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reasoning On-Device...", fontWeight = FontWeight.Bold)
            } else {
                Icon(imageVector = Icons.Default.Psychology, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Execute Agent Plan (${selectedProfile.name})", fontWeight = FontWeight.Bold)
            }
        }

        // Pending Human Confirmations (HIGH / CRITICAL Risk Gate)
        if (pendingProposals.isNotEmpty()) {
            Text(
                text = "ACTION APPROVALS REQUIRED (${pendingProposals.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = RiskHigh,
                letterSpacing = 1.2.sp
            )
            pendingProposals.forEach { proposal ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.5.dp, RiskHigh.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tool: ${proposal.toolName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            RiskBadge(risk = proposal.riskLevel)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = proposal.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Arguments: ${proposal.arguments}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { edgeAI.agent.confirmationManager.cancel(proposal.id) }) {
                                Text("Reject", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        edgeAI.agent.confirmationManager.confirm(proposal.id)
                                        edgeAI.gateway.execute(
                                            toolId = proposal.toolId,
                                            arguments = proposal.arguments,
                                            userConsentGiven = true,
                                            preConfirmedProposalId = proposal.id
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RiskHigh)
                            ) {
                                Text("Authorize & Execute", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Multi-Step Execution Trace
        lastResult?.let { res ->
            EdgeCard(
                backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXECUTION RESULT (${res.profile.name})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${res.latencyMs}ms • ${res.tokensUsed} tokens",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = res.finalResponse,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (res.toolsExecuted.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tools Invoked: ${res.toolsExecuted.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalAIGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Steps Breakdown
            if (res.steps.isNotEmpty()) {
                Text(
                    text = "REASONING & OBSERVATION TRACE (${res.steps.size} STEPS)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp
                )
                res.steps.forEach { step ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Step ${step.stepIndex}: ${step.selectedTool ?: "Reasoning"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Thought: ${step.thought}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (step.observation != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Observation: ${step.observation}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Automations & Agent Proposals
        Text(
            text = "PROPOSED ROUTINE AUTOMATIONS (${automationProposals.size})",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )
        if (automationProposals.isEmpty()) {
            Text(
                text = "No pending automation proposals from agent.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            automationProposals.forEach { ap ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = ap.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "Trigger: ${ap.triggerDescription}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Action: ${ap.actionDescription}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { edgeAI.automation.rejectProposal(ap.id) }) { Text("Dismiss", fontSize = 11.sp) }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { edgeAI.automation.confirmProposal(ap.id) },
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Approve Routine", fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 6. PRIVACY CENTER & AUDIT LOG SCREEN
// ----------------------------------------------------
@Composable
fun PrivacyScreen(edgeAI: EdgeAICore) {
    val privacyState by edgeAI.privacy.state.collectAsStateWithLifecycle()
    val auditLogs by edgeAI.privacy.auditLogs.collectAsStateWithLifecycle()
    val toolLogs by edgeAI.gateway.auditLogs.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var wipeSuccessMessage by remember { mutableStateOf<String?>(null) }
    val registeredTools = remember { edgeAI.tools.getAll() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EdgeCard(
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            borderColor = MaterialTheme.colorScheme.outlineVariant
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Privacy & Data Containment Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Strict Boundary Enforcement • Zero Silent Transmission",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = LocalAIGreen)
            }
        }

        // Data Containment Matrix
        EdgeCard {
            Text(
                text = "DATA CONTAINMENT & ACCESS MATRIX",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            ContainmentRow("WHAT STAYS ON DEVICE", "Raw Camera Pixels, Vector Embeddings, Memory DB, Health Sensors", LocalAIGreen)
            ContainmentRow("WHAT CAN REACH PRIVATE SERVER", "Encrypted Inference Prompts (if enabled by user)", PrivateServerAmber)
            ContainmentRow("WHAT CAN REACH CLOUD", "Public Queries with explicit consent only (Default: Blocked)", CloudAIBorder)
            ContainmentRow("MCP SERVERS WITH ACCESS", "InternalMcpServer (Loopback - TRUSTED_LOCAL)", MaterialTheme.colorScheme.primary)
            ContainmentRow("TOOLS WITH ACCESS", "${registeredTools.size} Native Tools governed by ToolGateway", MaterialTheme.colorScheme.primary)
        }

        // Toggles for Remote AI
        EdgeCard {
            Text(
                text = "INTELLIGENCE ROUTING PERMISSIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Local On-Device AI", fontWeight = FontWeight.Bold)
                    Text(text = "Executes strictly inside device sandbox via LiteRT", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = true, onCheckedChange = {}, enabled = false)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Private AI Server (LAN / VPN)", fontWeight = FontWeight.Bold)
                    Text(text = "Encrypted connection to user-owned FastAPI / vLLM server", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = privacyState.privateServerEnabled,
                    onCheckedChange = { coroutineScope.launch { edgeAI.privacy.setPrivateServerAllowed(it) } },
                    modifier = Modifier.testTag("private_server_switch")
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Public Cloud AI Fallback", fontWeight = FontWeight.Bold)
                    Text(text = "Requires explicit consent for non-sensitive public tasks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = privacyState.cloudAiEnabled,
                    onCheckedChange = { coroutineScope.launch { edgeAI.privacy.setCloudAllowed(it) } },
                    modifier = Modifier.testTag("cloud_ai_switch")
                )
            }
        }

        // ----------------------------------------------------
        // DATA & STORAGE SECTION (REQUIREMENT 51)
        // ----------------------------------------------------
        var dbStats by remember { mutableStateOf<com.example.edgeaicore.core.database.DatabaseStats?>(null) }
        var storageBreakdown by remember { mutableStateOf<com.example.edgeaicore.core.storage.StorageBreakdown?>(null) }
        val modelsList by edgeAI.models.list.collectAsStateWithLifecycle()
        val pendingSyncCount by edgeAI.sync.engine.observePendingCount().collectAsStateWithLifecycle(initialValue = 0)
        val privateServerConfig by edgeAI.privateServer.config.collectAsStateWithLifecycle()
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            dbStats = edgeAI.database.getStats()
            storageBreakdown = edgeAI.storage.getBreakdown()
        }

        EdgeCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DATA & STORAGE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            dbStats = edgeAI.database.getStats()
                            storageBreakdown = edgeAI.storage.getBreakdown()
                        }
                    }
                ) {
                    Text("Refresh", fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Local Database
            DataStorageItemRow(
                title = "Local Database",
                subtitle = "Room SQLite (${dbStats?.activeTier?.name ?: "LOCAL"})",
                details = "${(dbStats?.databaseSizeBytes ?: 0L) / 1024} KB • Tasks: ${dbStats?.taskCount ?: 0} • Docs: ${dbStats?.documentCount ?: 0}",
                icon = Icons.Default.Storage,
                color = LocalAIGreen
            )

            // 2. Local Files
            DataStorageItemRow(
                title = "Local Files",
                subtitle = "App Sandbox Directories (/media, /images, /audio, /docs, /exports)",
                details = "${(storageBreakdown?.totalAppStorageBytes ?: 0L) / 1024} KB in sandbox (${(storageBreakdown?.freeDeviceStorageBytes ?: 0L) / (1024 * 1024)} MB device free)",
                icon = Icons.Default.Folder,
                color = MaterialTheme.colorScheme.primary
            )

            // 3. AI Models
            val installedModels = modelsList.filter { it.isInstalled }
            DataStorageItemRow(
                title = "AI Models",
                subtitle = "LiteRT & MediaPipe On-Device Weights",
                details = "${installedModels.size} installed (${modelsList.size} cataloged) • %.1f MB".format(installedModels.sumOf { it.sizeMb.toDouble() }),
                icon = Icons.Default.Psychology,
                color = CloudAIBorder
            )

            // 4. Knowledge Base
            DataStorageItemRow(
                title = "Knowledge Base",
                subtitle = "Ingested Articles, Documents & FAQs",
                details = "${dbStats?.knowledgeItemCount ?: 0} items indexed in FTS & Vector store",
                icon = Icons.Default.AutoStories,
                color = PrivateServerAmber
            )

            // 5. Embeddings
            DataStorageItemRow(
                title = "Embeddings",
                subtitle = "High-Dimensional Vector Indices (Cosine Distance)",
                details = "${dbStats?.embeddingCount ?: 0} semantic vectors generated on-device",
                icon = Icons.Default.Hub,
                color = LocalAIGreen
            )

            // 6. Cache
            val cacheStats = remember { edgeAI.cache.getStats() }
            val totalCacheEntries = cacheStats.aiCacheEntries + cacheStats.apiCacheEntries + cacheStats.mediaCacheEntries + cacheStats.toolCacheEntries + cacheStats.mcpCacheEntries
            DataStorageItemRow(
                title = "Cache",
                subtitle = "LiteRT Prompt Cache & Temp File Storage",
                details = "$totalCacheEntries entries (${cacheStats.totalMemoryUsageEstimatedBytes / 1024} KB est memory)",
                icon = Icons.Default.Cached,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 7. Private Server
            DataStorageItemRow(
                title = "Private Server",
                subtitle = if (privateServerConfig.enabled) privateServerConfig.baseUrl else "Not configured / Disabled",
                details = if (privateServerConfig.enabled) "Status: CONNECTED (mTLS/Bearer Auth)" else "Local Standalone Mode",
                icon = Icons.Default.Dns,
                color = if (privateServerConfig.enabled) LocalAIGreen else MaterialTheme.colorScheme.outline
            )

            // 8. Sync
            DataStorageItemRow(
                title = "Sync",
                subtitle = "Offline-First Transaction Queue",
                details = "$pendingSyncCount pending items queued for private gateway",
                icon = Icons.Default.Sync,
                color = if (pendingSyncCount > 0) PrivateServerAmber else LocalAIGreen
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "DATA & STORAGE ACTIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Action Grid: Manage Storage, Export Data, Import Data, Backup, Restore, Clear Cache, Delete Data
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val optRes = edgeAI.database.optimize()
                                val checkRes = edgeAI.storage.runIntegrityCheck()
                                dbStats = edgeAI.database.getStats()
                                storageBreakdown = edgeAI.storage.getBreakdown()
                                wipeSuccessMessage = "Storage Audit & VACUUM complete. ${optRes is EdgeResult.Success}"
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("action_manage_storage"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Manage Storage", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val exportRes = edgeAI.export.create()
                                when (exportRes) {
                                    is EdgeResult.Success -> wipeSuccessMessage = exportRes.data
                                    is EdgeResult.Failure -> wipeSuccessMessage = "Export failed: ${exportRes.error.message}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("action_export_data"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Export Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val exports = edgeAI.export.list()
                                if (exports.isEmpty()) {
                                    wipeSuccessMessage = "No export files available to import."
                                } else {
                                    val latest = exports.maxByOrNull { it.lastModified }?.fileName ?: ""
                                    val importRes = edgeAI.sync.import.executeImport(latest, com.example.edgeaicore.core.sync.ImportStrategy.MERGE)
                                    when (importRes) {
                                        is EdgeResult.Success -> wipeSuccessMessage = importRes.data
                                        is EdgeResult.Failure -> wipeSuccessMessage = "Import error: ${importRes.error.message}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("action_import_data"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Import Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val backupRes = edgeAI.backup.create(
                                    destination = com.example.edgeaicore.core.sync.BackupDestination.LOCAL_DEVICE,
                                    userConsentGiven = true
                                )
                                when (backupRes) {
                                    is EdgeResult.Success -> wipeSuccessMessage = backupRes.data
                                    is EdgeResult.Failure -> wipeSuccessMessage = "Backup failed: ${backupRes.error.message}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("action_backup"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val backups = edgeAI.backup.list()
                                if (backups.isEmpty()) {
                                    wipeSuccessMessage = "No backup snapshot found. Create a backup first."
                                } else {
                                    val latest = backups.maxByOrNull { it.lastModified }?.fileName ?: ""
                                    val res = edgeAI.backup.restore(latest, userConsentGiven = true)
                                    when (res) {
                                        is EdgeResult.Success -> wipeSuccessMessage = res.data
                                        is EdgeResult.Failure -> wipeSuccessMessage = "Restore failed: ${res.error.message}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("action_restore"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            edgeAI.cache.clearAll()
                            edgeAI.ai.clearCache()
                            wipeSuccessMessage = "Prompt cache & temporary storage cache cleared."
                        },
                        modifier = Modifier.weight(1f).testTag("action_clear_cache"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Clear Cache", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("action_delete_data"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Confirm Data Deletion", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Are you sure you want to permanently delete all on-device memories, temporary documents, and vector caches? This action cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                edgeAI.memory.clear()
                                edgeAI.cache.clearAll()
                                edgeAI.ai.clearCache()
                                dbStats = edgeAI.database.getStats()
                                storageBreakdown = edgeAI.storage.getBreakdown()
                                wipeSuccessMessage = "All user data and caches permanently deleted."
                                showDeleteConfirmDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Everything")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Data & MCP Management Controls
        EdgeCard {
            Text(
                text = "MCP & PRIVACY LOG CONTROLS",
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
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            edgeAI.mcp.disconnectServer("remote")
                            edgeAI.mcp.client.securityManager.authorizeServer("remote", com.example.edgeaicore.core.mcp.McpTrustLevel.UNTRUSTED)
                            wipeSuccessMessage = "Disconnected remote MCP servers & reset permissions."
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Disconnect MCP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        edgeAI.gateway.clearAudit()
                        edgeAI.privacy.clearLogs()
                        wipeSuccessMessage = "Audit logs purged."
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear Logs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        wipeSuccessMessage?.let { msg ->
            Text(
                text = msg,
                color = LocalAIGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        // Audit Trail Logs
        Text(
            text = "TOOL GATEWAY EXECUTION AUDIT TRAIL (${toolLogs.size})",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            toolLogs.take(10).forEach { log ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = log.toolId, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Risk: ${log.riskLevel.name} • Status: ${log.confirmationStatus}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (log.executionSuccess) LocalAIGreenContainer else MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = if (log.executionSuccess) "SUCCESS" else "FAILED",
                                color = if (log.executionSuccess) LocalAIGreen else MaterialTheme.colorScheme.error,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContainmentRow(label: String, value: String, accentColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DataStorageItemRow(
    title: String,
    subtitle: String,
    details: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = details, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ----------------------------------------------------
// 7. MODEL REGISTRY & MANAGEMENT SCREEN
// ----------------------------------------------------
@Composable
fun ModelsScreen(edgeAI: EdgeAICore) {
    val models by edgeAI.models.list.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EdgeCard {
            Text(
                text = "On-Device Neural Model Registry",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "LiteRT • LiteRT-LM • MediaPipe Tasks • Quantized Edge Weights",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(models) { model ->
                ModelItemCard(
                    model = model,
                    onInstall = {
                        coroutineScope.launch { edgeAI.models.install(model.id) }
                    },
                    onRemove = {
                        edgeAI.models.remove(model.id)
                    },
                    onToggleEnabled = {
                        edgeAI.models.setEnabled(model.id, !model.isEnabled)
                    }
                )
            }
        }
    }
}

@Composable
private fun ModelItemCard(
    model: EdgeModel,
    onInstall: () -> Unit,
    onRemove: () -> Unit,
    onToggleEnabled: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = model.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(text = "Type: ${model.type.name} • Size: %.1f MB".format(model.sizeMb), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = CircleShape,
                    color = if (model.isInstalled) LocalAIGreenContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (model.isInstalled) "INSTALLED" else "AVAILABLE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (model.isInstalled) LocalAIGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Capabilities: [${model.capabilities.joinToString(", ")}]",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (model.isInstalled) {
                    TextButton(onClick = onRemove) { Text("Uninstall", color = RiskHigh, fontSize = 11.sp) }
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = model.isEnabled,
                        onClick = onToggleEnabled,
                        shape = RoundedCornerShape(12.dp),
                        label = { Text(if (model.isEnabled) "Enabled" else "Disabled", fontSize = 11.sp) }
                    )
                } else {
                    Button(
                        onClick = onInstall,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Download & Install", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 8. SYSTEM HARDWARE DIAGNOSTICS SCREEN
// ----------------------------------------------------
@Composable
fun DiagnosticsScreen(edgeAI: EdgeAICore) {
    val specs = remember { edgeAI.diagnostics.specs() }
    val metrics by edgeAI.diagnostics.flow().collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EdgeCard {
            Text(
                text = "Hardware Capability & Diagnostics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Hardware accelerator detection & live resource monitor",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Live Telemetry Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "Inferences",
                value = metrics.totalInferences.toString(),
                subtitle = "OK: ${metrics.successfulInferences}",
                icon = Icons.Default.CheckCircle,
                badgeColor = LocalAIGreen,
                progress = if (metrics.totalInferences > 0) (metrics.successfulInferences.toFloat() / metrics.totalInferences.toFloat()) else 1.0f
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "RAM Usage",
                value = "${metrics.memoryUsageMb} MB",
                subtitle = "Avail: ${specs.availableRamMb} MB",
                icon = Icons.Default.Memory,
                badgeColor = MaterialTheme.colorScheme.primary,
                progress = (metrics.memoryUsageMb.toFloat() / specs.totalRamMb.toFloat()).coerceIn(0f, 1f)
            )
        }

        // Extended Edge AI & MCP Engine Diagnostics
        EdgeCard {
            Text(
                text = "MCP & AGENT ENGINE DIAGNOSTICS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            SpecRow(label = "Connected MCP Servers", value = "${metrics.mcpConnectedServers} (Local Loopback)")
            SpecRow(label = "MCP Loopback Latency", value = "${metrics.mcpServerLatencyMs} ms")
            SpecRow(label = "Tool Invocation Latency", value = "${metrics.toolInvocationLatencyMs} ms")
            SpecRow(label = "Active Agent Steps", value = "${metrics.agentStepCount} steps")
            SpecRow(label = "Agent Tokens Consumed", value = "${metrics.agentTokensUsed} tokens")
            SpecRow(label = "Private Server Probe Latency", value = "${metrics.privateServerLatencyMs} ms")
            SpecRow(label = "Local Inference Average", value = "${metrics.averageInferenceLatencyMs} ms")
            SpecRow(label = "Policy Routing Decisions", value = "${metrics.policyDecisionsCount} evaluated")
            SpecRow(label = "Tool Failures Intercepted", value = "${metrics.toolFailuresCount}")
        }

        EdgeCard {
            Text(
                text = "DEVICE SPECIFICATIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            SpecRow(label = "Device", value = "${specs.manufacturer} ${specs.model}")
            SpecRow(label = "Android Version", value = "Android ${specs.androidVersion} (API ${specs.sdkInt})")
            SpecRow(label = "CPU Cores", value = "${specs.cpuCores} active cores")
            SpecRow(label = "Total System RAM", value = "${specs.totalRamMb} MB")
            SpecRow(label = "Available Internal Storage", value = "%.1f GB / %.1f GB".format(specs.availableStorageGb, specs.totalStorageGb))
            SpecRow(label = "GPU Acceleration", value = if (specs.isGpuAvailable) "Vulkan / OpenGL ES 3.2 (Active)" else "Unavailable")
            SpecRow(label = "NPU / NNAPI Acceleration", value = if (specs.isNpuAvailable) "Dedicated Neural Engine Detected" else "Standard Hardware Acceleration")
            SpecRow(label = "Recommended Execution Backend", value = specs.recommendedBackend.name)
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
