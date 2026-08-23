package com.example.edgeaicore.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.explanation.ExplanationRecord
import com.example.edgeaicore.core.memory.MemoryEntity
import com.example.edgeaicore.ui.agent.AgentScreen
import com.example.edgeaicore.ui.automation.RoutinesScreen
import com.example.edgeaicore.ui.benchmark.BenchmarkScreen
import com.example.edgeaicore.ui.capture.CaptureScreen
import com.example.edgeaicore.ui.common.UniversalExplanationSheet
import com.example.edgeaicore.ui.document.DocumentIntelligenceScreen
import com.example.edgeaicore.ui.home.HomeScreen
import com.example.edgeaicore.ui.memory.AskMemoryScreen
import com.example.edgeaicore.ui.memory.MemoryDetailSheet
import com.example.edgeaicore.ui.memory.MemoryScreen
import com.example.edgeaicore.ui.models.ModelCenterScreen
import com.example.edgeaicore.ui.privacy.PrivacyCenterScreen
import com.example.edgeaicore.ui.profile.ProfileScreen
import com.example.edgeaicore.ui.storage.StorageCenterScreen
import com.example.edgeaicore.ui.tools.ConnectedServicesScreen
import com.example.edgeaicore.ui.tools.ToolPlaygroundScreen
import com.example.edgeaicore.ui.tools.ToolsScreen
import com.example.edgeaicore.ui.voice.AudioJournalScreen

enum class MainDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
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

    var agentInitialGoal by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Tablet / Foldable Navigation Rail
            if (isWideScreen && currentSubDestination == null) {
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
                }
            }

            // Main Content Area
            Scaffold(
                modifier = Modifier.weight(1f),
                bottomBar = {
                    if (!isWideScreen && currentSubDestination == null) {
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
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = androidx.compose.ui.Alignment.TopCenter
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
                                AskMemoryScreen(
                                    edgeAI = edgeAI,
                                    initialQuery = sub.initialPrompt,
                                    onNavigateBack = { currentSubDestination = null },
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
                                    onNavigateToAsk = { prompt -> currentSubDestination = SubDestination.Ask(prompt) }
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
                                    onNavigateToAsk = { prompt -> currentSubDestination = SubDestination.Ask(prompt) }
                                )
                            }
                            is SubDestination.Routines -> {
                                RoutinesScreen(
                                    edgeAI = edgeAI,
                                    onBack = { currentSubDestination = null },
                                    onNavigateToAsk = { prompt -> currentSubDestination = SubDestination.Ask(prompt) }
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
                                    onNavigateToAsk = { prompt -> currentSubDestination = SubDestination.Ask(prompt) },
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
                                    onShowExplanation = { activeExplanation = it }
                                )
                            }
                            MainDestination.MEMORY -> {
                                MemoryScreen(
                                    edgeAI = edgeAI,
                                    onNavigateToAskMemory = { prompt -> currentSubDestination = SubDestination.Ask(prompt) },
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

    // Memory Detail Bottom Sheet
    if (selectedMemoryForDetail != null) {
        MemoryDetailSheet(
            memory = selectedMemoryForDetail!!,
            edgeAI = edgeAI,
            onDismiss = { selectedMemoryForDetail = null },
            onAskAIAboutMemory = { prompt ->
                selectedMemoryForDetail = null
                currentSubDestination = SubDestination.Ask(prompt)
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
}
