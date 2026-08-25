package com.example.edgeaicore.ui.playground

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.explanation.ExplanationRecord
import com.example.edgeaicore.ui.common.UniversalExplanationSheet
import com.example.ui.theme.LocalAIGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PLAYGROUND:
 * The Primary Interactive Workspace of SWAYAM GPT.
 * Integrates General Conversation, Research with RAG evidence, Document Analysis,
 * Personal Memory, and Autonomous Multi-Step Agents into a sovereign, on-device console.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaygroundScreen(
    edgeAI: EdgeAICore,
    viewModel: PlaygroundViewModel,
    modifier: Modifier = Modifier,
    onShowExplanation: (ExplanationRecord) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var sessionToRename by remember { mutableStateOf<PlaygroundSession?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var isImportDocDialogOpen by remember { mutableStateOf(false) }
    var docTitleInput by remember { mutableStateOf("") }
    var docContentInput by remember { mutableStateOf("") }

    // Auto-scroll when new messages arrive
    LaunchedEffect(state.activeSession?.messages?.size, state.isGenerating) {
        val count = state.activeSession?.messages?.size ?: 0
        if (count > 0) {
            listState.animateScrollToItem(count - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .testTag("playground_sessions_drawer")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Playground Sessions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close Drawer")
                        }
                    }

                    // New Session Action
                    Button(
                        onClick = {
                            viewModel.createNewSession(state.activeMode)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_session_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New ${state.activeMode.title} Session", fontWeight = FontWeight.Bold)
                    }

                    // Search Sessions
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Search sessions...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Sessions List
                    val filteredSessions = remember(state.sessions, state.searchQuery) {
                        if (state.searchQuery.isBlank()) state.sessions
                        else state.sessions.filter { it.title.contains(state.searchQuery, ignoreCase = true) }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredSessions, key = { it.id }) { session ->
                            val isSelected = session.id == state.activeSessionId
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectSession(session.id)
                                        scope.launch { drawerState.close() }
                                    }
                                    .testTag("session_item_${session.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${session.mode.name} • ${session.messages.size} msgs",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                sessionToRename = session
                                                renameInputText = session.title
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteSession(session.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "PLAYGROUND",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    letterSpacing = 1.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = LocalAIGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "SOVEREIGN",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LocalAIGreen,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = state.activeSession?.title ?: "Interactive Workspace",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("open_sessions_drawer_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Sessions")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.createNewSession(state.activeMode) },
                            modifier = Modifier.testTag("top_new_session_btn")
                        ) {
                            Icon(imageVector = Icons.Default.AddComment, contentDescription = "New Session")
                        }
                        IconButton(
                            onClick = { viewModel.toggleExecutionDetails(true) },
                            modifier = Modifier.testTag("top_stats_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = "Execution Parameters")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            modifier = modifier.testTag("playground_screen")
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Top Live Context Banner
                PlaygroundContextPanel(
                    contextState = state.contextState,
                    onOpenDetails = { viewModel.toggleExecutionDetails(true) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                // Message Stream LazyColumn
                val messages = state.activeSession?.messages ?: emptyList()

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            EmptyPlaygroundGreeting(
                                activeMode = state.activeMode,
                                onSamplePrompt = { viewModel.sendMessage(it) }
                            )
                        }
                    } else {
                        items(messages, key = { it.id }) { msg ->
                            PlaygroundMessageBubble(
                                message = msg,
                                onSaveToMemory = { viewModel.saveToMemory(it) },
                                onShowSources = { viewModel.showSources(it) },
                                onShowExplanation = { exp ->
                                    viewModel.showExplanation(exp)
                                    onShowExplanation(exp)
                                },
                                onRetry = { viewModel.retryLastMessage() }
                            )
                        }
                    }

                    // Bottom spacer for comfortable scrolling
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Bottom Composer
                PlaygroundComposer(
                    inputText = state.inputText,
                    onInputChanged = { viewModel.onInputChanged(it) },
                    onSendMessage = { viewModel.sendMessage() },
                    onStopGeneration = { viewModel.stopGeneration() },
                    isGenerating = state.isGenerating,
                    activeMode = state.activeMode,
                    onModeSelected = { viewModel.selectMode(it) },
                    onAttachFile = { isImportDocDialogOpen = true },
                    onStartVoice = {
                        viewModel.sendMessage("Tell me about SWAYAM on-device sovereign architecture.")
                    }
                )
            }
        }
    }

    // Grounded Sources Sheet
    state.selectedSources?.let { sources ->
        PlaygroundSourceSheet(
            sources = sources,
            onDismiss = { viewModel.dismissSources() }
        )
    }

    // Execution Details Sheet
    if (state.showExecutionDetails) {
        PlaygroundExecutionDetailsSheet(
            contextState = state.contextState,
            onDismiss = { viewModel.toggleExecutionDetails(false) }
        )
    }

    // Universal Explanation Sheet
    state.activeExplanation?.let { record ->
        UniversalExplanationSheet(
            record = record,
            onDismiss = { viewModel.dismissExplanation() }
        )
    }

    // Rename Session Dialog
    sessionToRename?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            title = { Text("Rename Session", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    label = { Text("Session Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameSession(session.id, renameInputText)
                        sessionToRename = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Ingest Document Dialog
    if (isImportDocDialogOpen) {
        AlertDialog(
            onDismissRequest = { isImportDocDialogOpen = false },
            title = { Text("Index Document to RAG Vault", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Add text content to chunk, embed, and index locally into SWAYAM's offline vector store.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = docTitleInput,
                        onValueChange = { docTitleInput = it },
                        label = { Text("Document Title (e.g. Research.txt)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = docContentInput,
                        onValueChange = { docContentInput = it },
                        label = { Text("Document Content") },
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (docTitleInput.isNotBlank() && docContentInput.isNotBlank()) {
                            viewModel.attachDocumentText(docTitleInput, docContentInput)
                            docTitleInput = ""
                            docContentInput = ""
                            isImportDocDialogOpen = false
                        }
                    },
                    enabled = docTitleInput.isNotBlank() && docContentInput.isNotBlank()
                ) {
                    Text("Index Locally")
                }
            },
            dismissButton = {
                TextButton(onClick = { isImportDocDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyPlaygroundGreeting(
    activeMode: PlaygroundMode,
    onSamplePrompt: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${activeMode.title} Workspace",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = activeMode.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            Text(
                text = "Suggested starting points:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val suggestions = when (activeMode) {
                PlaygroundMode.GENERAL -> listOf(
                    "Explain on-device zero cloud egress neural inference.",
                    "Draft an architectural summary of SWAYAM GPT.",
                    "Plan a 3-day workshop on sovereign private AI."
                )
                PlaygroundMode.RESEARCH -> listOf(
                    "Summarize key findings across my local knowledge vault.",
                    "What evidence exists in my notes regarding neural architectures?",
                    "Synthesize research from all indexed papers."
                )
                PlaygroundMode.DOCUMENTS -> listOf(
                    "What does the uploaded document say about system specifications?",
                    "Extract action items from my project documents.",
                    "Compare sections in my offline knowledge base."
                )
                PlaygroundMode.MEMORY -> listOf(
                    "What are my stored preferences for coding and communication?",
                    "Remember that my favorite runtime is LiteRT-LM GPU.",
                    "Show all encrypted memories recorded this week."
                )
                PlaygroundMode.AGENTS -> listOf(
                    "Inspect device diagnostics and compile performance report.",
                    "Audit local storage integrity and clean temporary caches.",
                    "Run autonomous tool validation across all native capabilities."
                )
            }

            suggestions.forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSamplePrompt(prompt) }
                ) {
                    Text(
                        text = "“$prompt”",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
