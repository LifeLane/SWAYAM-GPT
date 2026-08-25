package com.example.edgeaicore.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.explanation.ExplanationRecord
import com.example.edgeaicore.core.memory.MemoryEntity
import com.example.edgeaicore.ui.agent.AgentScreen
import com.example.edgeaicore.ui.automation.RoutinesScreen
import com.example.edgeaicore.ui.benchmark.BenchmarkScreen
import com.example.edgeaicore.ui.capture.CaptureScreen
import com.example.edgeaicore.ui.common.UniversalExplanationSheet
import com.example.edgeaicore.ui.console.ChatConsoleFullScreen
import com.example.edgeaicore.ui.document.DocumentIntelligenceScreen
import com.example.edgeaicore.ui.home.HomeScreen
import com.example.edgeaicore.ui.memory.MemoryDetailSheet
import com.example.edgeaicore.ui.memory.MemoryScreen
import com.example.edgeaicore.ui.models.ModelCenterScreen
import com.example.edgeaicore.ui.privacy.PrivacyCenterScreen
import com.example.edgeaicore.ui.profile.ProfileScreen
import com.example.edgeaicore.ui.setup.ProvisioningOverlay
import com.example.edgeaicore.ui.storage.StorageCenterScreen
import com.example.edgeaicore.ui.tools.ConnectedServicesScreen
import com.example.edgeaicore.ui.tools.ToolPlaygroundScreen
import com.example.edgeaicore.ui.tools.ToolsScreen
import com.example.edgeaicore.ui.voice.AudioJournalScreen
import com.example.edgeaicore.ui.playground.PlaygroundScreen
import com.example.edgeaicore.ui.playground.PlaygroundViewModel
import com.example.ui.theme.LocalAIGreen

enum class MainDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    PLAYGROUND("Playground", Icons.Filled.Terminal, Icons.Outlined.Terminal),
    MEMORY("Memory", Icons.Filled.Psychology, Icons.Outlined.Psychology),
    AGENT("Agent", Icons.Filled.SmartToy, Icons.Outlined.SmartToy),
    TOOLS("Tools", Icons.Filled.Extension, Icons.Outlined.Extension),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

sealed class SubDestination {
    data class Ask(val initialPrompt: String) : SubDestination()
    object Capture : SubDestination()
    object PrivacyCenter : SubDestination()
    object ModelCenter : SubDestination()
    object StorageCenter : SubDestination()
    object ConnectedServices : SubDestination()
    object DocumentIntelligence : SubDestination()
    object Benchmark : SubDestination()
    object AudioJournal : SubDestination()
    object Routines : SubDestination()
    object ToolPlayground : SubDestination()
}

@Composable
fun AppShell(
    edgeAI: EdgeAICore,
    modifier: Modifier = Modifier
) {
    var currentMainDestination by remember { mutableStateOf(MainDestination.HOME) }
    var currentSubDestination by remember { mutableStateOf<SubDestination?>(null) }

    var selectedMemoryForDetail by remember { mutableStateOf<MemoryEntity?>(null) }
    var activeExplanation by remember { mutableStateOf<ExplanationRecord?>(null) }
    var showDeveloperModal by remember { mutableStateOf(false) }
    var isChatConsoleOpen by remember { mutableStateOf(false) }
    var chatConsoleInitialPrompt by remember { mutableStateOf<String?>(null) }

    val openUnifiedConsole: (String?) -> Unit = { prompt ->
        chatConsoleInitialPrompt = prompt
        isChatConsoleOpen = true
    }

    var agentInitialGoal by remember { mutableStateOf<String?>(null) }
    val playgroundViewModel = remember { PlaygroundViewModel(edgeAI.context, edgeAI) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Tablet / Foldable Navigation Rail
            if (isWideScreen && currentSubDestination == null && !isChatConsoleOpen) {
                NavigationRail(
                    modifier = Modifier.widthIn(min = 80.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    MainDestination.values().forEach { destination ->
                        NavigationRailItem(
                            selected = currentMainDestination == destination,
                            onClick = { currentMainDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = if (currentMainDestination == destination) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.title
                                )
                            },
                            label = { Text(destination.title) },
                            modifier = Modifier.testTag("nav_rail_${destination.name.lowercase()}")
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Wide screen Chat Console Quick Button
                    IconButton(
                        onClick = { isChatConsoleOpen = true },
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .testTag("wide_chat_console_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Open Chat Console",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Main Content Area
            Scaffold(
                modifier = Modifier.weight(1f),
                bottomBar = {
                    if (currentSubDestination == null && !isChatConsoleOpen) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        ) {
                            // Dedicated Sleek Chat Console Tab Above Navigation
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { isChatConsoleOpen = true }
                                    .testTag("chat_console_tab_btn"),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                shadowElevation = 3.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "SWAYAM Neural Console",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = LocalAIGreen.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "ON-DEVICE",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = LocalAIGreen,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Swayam Control • Rich Markdown, Codes, Charts & Gateways",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.OpenInFull,
                                        contentDescription = "Open Full Screen Console",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (!isWideScreen) {
                                NavigationBar(
                                    modifier = Modifier.testTag("main_bottom_nav")
                                ) {
                                    MainDestination.values().forEach { destination ->
                                        NavigationBarItem(
                                            selected = currentMainDestination == destination,
                                            onClick = { currentMainDestination = destination },
                                            icon = {
                                                Icon(
                                                    imageVector = if (currentMainDestination == destination) destination.selectedIcon else destination.unselectedIcon,
                                                    contentDescription = destination.title
                                                )
                                            },
                                            label = { Text(destination.title) },
                                            modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1200.dp)
                    ) {
                        val sub = currentSubDestination
                        if (sub != null) {
                            when (sub) {
                                is SubDestination.Ask -> {
                                    ChatConsoleFullScreen(
                                        edgeAI = edgeAI,
                                        initialPrompt = sub.initialPrompt,
                                        onClose = { currentSubDestination = null },
                                        onShowExplanation = { activeExplanation = it }
                                    )
                                }
                                is SubDestination.Capture -> {
                                    CaptureScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null },
                                        onMemorySaved = { currentSubDestination = null }
                                    )
                                }
                                is SubDestination.PrivacyCenter -> {
                                    PrivacyCenterScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null },
                                        onNavigateToStorage = { currentSubDestination = SubDestination.StorageCenter }
                                    )
                                }
                                is SubDestination.ModelCenter -> {
                                    ModelCenterScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null }
                                    )
                                }
                                is SubDestination.StorageCenter -> {
                                    StorageCenterScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null },
                                        onNavigateToModels = { currentSubDestination = SubDestination.ModelCenter },
                                        onNavigateToMemories = {
                                            currentSubDestination = null
                                            currentMainDestination = MainDestination.MEMORY
                                        }
                                    )
                                }
                                is SubDestination.ConnectedServices -> {
                                    ConnectedServicesScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null },
                                        onOpenDeveloperModal = { showDeveloperModal = true }
                                    )
                                }
                                is SubDestination.DocumentIntelligence -> {
                                    DocumentIntelligenceScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null },
                                        onNavigateToAsk = { prompt -> openUnifiedConsole(prompt) }
                                    )
                                }
                                is SubDestination.Benchmark -> {
                                    BenchmarkScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null }
                                    )
                                }
                                is SubDestination.AudioJournal -> {
                                    AudioJournalScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null },
                                        onNavigateToAsk = { prompt -> openUnifiedConsole(prompt) }
                                    )
                                }
                                is SubDestination.Routines -> {
                                    RoutinesScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null },
                                        onNavigateToAsk = { prompt -> openUnifiedConsole(prompt) }
                                    )
                                }
                                is SubDestination.ToolPlayground -> {
                                    ToolPlaygroundScreen(
                                        edgeAI = edgeAI,
                                        onBack = { currentSubDestination = null }
                                    )
                                }
                            }
                        } else {
                            when (currentMainDestination) {
                                MainDestination.HOME -> {
                                    HomeScreen(
                                        edgeAI = edgeAI,
                                        onNavigateToAsk = { prompt ->
                                            if (prompt.isNotBlank()) playgroundViewModel.sendMessage(prompt)
                                            currentMainDestination = MainDestination.PLAYGROUND
                                        },
                                        onNavigateToCapture = { currentSubDestination = SubDestination.Capture },
                                        onNavigateToMemory = { currentMainDestination = MainDestination.MEMORY },
                                        onNavigateToAgent = { goal ->
                                            agentInitialGoal = goal
                                            currentMainDestination = MainDestination.AGENT
                                        },
                                        onNavigateToTools = { currentMainDestination = MainDestination.TOOLS },
                                        onNavigateToDocumentIntel = { currentSubDestination = SubDestination.DocumentIntelligence },
                                        onNavigateToBenchmark = { currentSubDestination = SubDestination.Benchmark },
                                        onNavigateToAudioJournal = { currentSubDestination = SubDestination.AudioJournal },
                                        onNavigateToRoutines = { currentSubDestination = SubDestination.Routines },
                                        onOpenOperatingCenter = { showDeveloperModal = true },
                                        onShowExplanation = { activeExplanation = it }
                                    )
                                }
                                MainDestination.PLAYGROUND -> {
                                    PlaygroundScreen(
                                        edgeAI = edgeAI,
                                        viewModel = playgroundViewModel,
                                        onShowExplanation = { activeExplanation = it }
                                    )
                                }
                                MainDestination.MEMORY -> {
                                    MemoryScreen(
                                        edgeAI = edgeAI,
                                        onNavigateToAskMemory = { prompt ->
                                            if (prompt.isNotBlank()) playgroundViewModel.sendMessage(prompt)
                                            currentMainDestination = MainDestination.PLAYGROUND
                                        },
                                        onSelectMemory = { memory -> selectedMemoryForDetail = memory }
                                    )
                                }
                                MainDestination.AGENT -> {
                                    AgentScreen(
                                        edgeAI = edgeAI,
                                        initialGoal = agentInitialGoal,
                                        onShowExplanation = { activeExplanation = it }
                                    )
                                }
                                MainDestination.TOOLS -> {
                                    ToolsScreen(
                                        edgeAI = edgeAI,
                                        onNavigateToConnectedServices = { currentSubDestination = SubDestination.ConnectedServices },
                                        onNavigateToPlayground = { currentSubDestination = SubDestination.ToolPlayground }
                                    )
                                }
                                MainDestination.PROFILE -> {
                                    ProfileScreen(
                                        edgeAI = edgeAI,
                                        onNavigateToPrivacy = { currentSubDestination = SubDestination.PrivacyCenter },
                                        onNavigateToModels = { currentSubDestination = SubDestination.ModelCenter },
                                        onNavigateToStorage = { currentSubDestination = SubDestination.StorageCenter },
                                        onNavigateToServices = { currentSubDestination = SubDestination.ConnectedServices },
                                        onNavigateToDocumentIntel = { currentSubDestination = SubDestination.DocumentIntelligence },
                                        onNavigateToBenchmark = { currentSubDestination = SubDestination.Benchmark },
                                        onNavigateToAudioJournal = { currentSubDestination = SubDestination.AudioJournal },
                                        onNavigateToRoutines = { currentSubDestination = SubDestination.Routines },
                                        onNavigateToToolPlayground = { currentSubDestination = SubDestination.ToolPlayground },
                                        onOpenDeveloperModal = { showDeveloperModal = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Screen SWAYAM Chat Console Overlay
    if (isChatConsoleOpen) {
        ChatConsoleFullScreen(
            edgeAI = edgeAI,
            initialPrompt = chatConsoleInitialPrompt,
            onClose = {
                isChatConsoleOpen = false
                chatConsoleInitialPrompt = null
            },
            onShowExplanation = { activeExplanation = it }
        )
    }

    // Memory Detail Bottom Sheet
    selectedMemoryForDetail?.let { mem ->
        MemoryDetailSheet(
            memory = mem,
            edgeAI = edgeAI,
            onDismiss = { selectedMemoryForDetail = null },
            onAskAIAboutMemory = { prompt ->
                selectedMemoryForDetail = null
                if (prompt.isNotBlank()) playgroundViewModel.sendMessage(prompt)
                currentMainDestination = MainDestination.PLAYGROUND
            },
            onMemoryDeleted = {
                selectedMemoryForDetail = null
            }
        )
    }

    // Universal Explanation Modal Sheet
    if (activeExplanation != null) {
        UniversalExplanationSheet(
            record = activeExplanation,
            onDismiss = { activeExplanation = null }
        )
    }

    // Advanced Developer / Diagnostics Modal
    if (showDeveloperModal) {
        AiEngineModal(
            edgeAI = edgeAI,
            onDismiss = { showDeveloperModal = false }
        )
    }

    // First-Launch / Preparing Environment Overlay
    ProvisioningOverlay(edgeAI = edgeAI)
}
