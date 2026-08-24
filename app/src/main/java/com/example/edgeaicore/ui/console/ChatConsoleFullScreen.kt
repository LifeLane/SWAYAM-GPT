package com.example.edgeaicore.ui.console

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.explanation.ExplanationRecord
import com.example.edgeaicore.core.swayam.ResponseStyle
import com.example.edgeaicore.core.swayam.SwayamRequest
import com.example.edgeaicore.ui.common.ResponseActionToolbar
import com.example.edgeaicore.ui.memory.ChatMessage
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ConsoleChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messages: MutableList<ChatMessage> = mutableListOf()
)

data class SwayamControlConfig(
    val systemPrompt: String = "You are SWAYAM, an on-device sovereign AI operating mind. Provide clear, precise, and well-structured answers with code, tables, and JSON where applicable.",
    val selectedModel: String = "Gemma 2B (LiteRT-LM Edge)",
    val modelProvider: AIProviderType = AIProviderType.LOCAL,
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val maxTokens: Int = 1024,
    val contextWindowLimit: String = "8K Tokens",
    val isMemoryVaultEnabled: Boolean = true,
    val isKnowledgeVaultEnabled: Boolean = true,
    val isAutoTrimContext: Boolean = true,
    val isToolGatewayAutonomous: Boolean = true,
    val isWebSearchToolEnabled: Boolean = true,
    val isCodeSandboxEnabled: Boolean = true,
    val isVisionOcrEnabled: Boolean = true,
    val isSystemAuditorEnabled: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConsoleFullScreen(
    edgeAI: EdgeAICore,
    onClose: () -> Unit,
    onShowExplanation: (ExplanationRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var controlConfig by remember { mutableStateOf(SwayamControlConfig()) }
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var isListeningVoice by remember { mutableStateOf(false) }

    // Session Management
    val sessions = remember {
        mutableStateListOf(
            ConsoleChatSession(
                title = "Primary Sovereign Session",
                messages = mutableListOf(
                    ChatMessage(
                        isUser = false,
                        text = "### ⚡ SWAYAM Full-Screen Neural Console Initialized\n\n" +
                                "I am connected to your on-device engine with full support for **Codes**, **Markdown**, **JSON Structures**, **Metric Charts**, and **Tables**.\n\n" +
                                "```json\n{\n  \"engine\": \"LiteRT-LM Gemma 2B\",\n  \"privacy\": \"Zero Cloud Egress\",\n  \"context\": \"8K Window Active\",\n  \"tools\": [\"Memory Vault\", \"RAG\", \"Code Sandbox\"]\n}\n```\n\n" +
                                "Tap the **Hamburger Menu (≡)** at the top left to adjust **Swayam Controls** (System Prompt, Temperature, Context, Model, Tools Gateway & Sessions).",
                        provider = AIProviderType.LOCAL,
                        confidence = 1.0f,
                        explanation = ExplanationRecord(
                            featureName = "Console Boot",
                            whatHappened = "Initialized full-screen console with rich rendering engine.",
                            whyReason = "User launched dedicated Chat Console.",
                            confidenceScore = 1.0f,
                            dataSourcesUsed = listOf("Swayam Control Core"),
                            wasAiInvolved = true,
                            providerType = AIProviderType.LOCAL,
                            privacyLevel = PrivacyLevel.LOCAL_ONLY
                        )
                    )
                )
            )
        )
    }
    var currentSessionIndex by remember { mutableIntStateOf(0) }
    val activeSession = sessions.getOrNull(currentSessionIndex) ?: sessions.first()
    val messages = activeSession.messages

    BackHandler {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            onClose()
        }
    }

    fun submitQuery(query: String) {
        if (query.isBlank() || isThinking) return
        val userMsg = ChatMessage(isUser = true, text = query)
        messages.add(userMsg)
        inputText = ""
        isThinking = true

        coroutineScope.launch {
            val response = edgeAI.swayam.process(
                SwayamRequest(
                    prompt = query,
                    privacyLevel = if (controlConfig.modelProvider == AIProviderType.LOCAL) PrivacyLevel.LOCAL_ONLY else PrivacyLevel.PRIVATE,
                    userConsent = true,
                    temperature = controlConfig.temperature,
                    topP = controlConfig.topP,
                    topK = controlConfig.topK,
                    maxTokens = controlConfig.maxTokens,
                    preferredProvider = controlConfig.modelProvider,
                    modelId = when (controlConfig.selectedModel) {
                        "Gemini 2.0 Flash" -> "gemini-2.0-flash"
                        "Gemini 1.5 Pro" -> "gemini-1.5-pro"
                        else -> "gemma-2b-it-cpu"
                    }
                )
            )

            when (response) {
                is EdgeResult.Success -> {
                    val swayamRes = response.data
                    messages.add(
                        ChatMessage(
                            isUser = false,
                            text = swayamRes.text,
                            sourcesUsed = swayamRes.sources,
                            provider = swayamRes.provider,
                            confidence = swayamRes.confidence,
                            explanation = swayamRes.explanation
                        )
                    )
                }
                is EdgeResult.Failure -> {
                    messages.add(
                        ChatMessage(
                            isUser = false,
                            text = "❌ Error executing inference: ${response.error.message}",
                            provider = AIProviderType.LOCAL
                        )
                    )
                }
            }
            isThinking = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Voice recognition handler
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListeningVoice = false
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = spokenText
                submitQuery(spokenText)
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechRecognition(context, voiceLauncher) { isListeningVoice = true }
        } else {
            Toast.makeText(context, "Microphone permission required for speech.", Toast.LENGTH_SHORT).show()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                SwayamControlDrawerContent(
                    config = controlConfig,
                    onConfigChange = { controlConfig = it },
                    sessions = sessions,
                    activeSessionIndex = currentSessionIndex,
                    onSelectSession = { idx ->
                        currentSessionIndex = idx
                        coroutineScope.launch { drawerState.close() }
                    },
                    onNewSession = {
                        val newSession = ConsoleChatSession(
                            title = "Chat Session #${sessions.size + 1}",
                            messages = mutableListOf(
                                ChatMessage(
                                    isUser = false,
                                    text = "Started new chat session. Ready for prompt, code evaluation, or system orchestration.",
                                    provider = AIProviderType.LOCAL
                                )
                            )
                        )
                        sessions.add(newSession)
                        currentSessionIndex = sessions.size - 1
                        coroutineScope.launch { drawerState.close() }
                    },
                    onDeleteSession = { idx ->
                        if (sessions.size > 1) {
                            sessions.removeAt(idx)
                            currentSessionIndex = (currentSessionIndex - 1).coerceAtLeast(0)
                        } else {
                            sessions[0].messages.clear()
                        }
                    },
                    onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Hamburger Menu + Title + Model Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("console_hamburger_menu_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Swayam Control Sidebar",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "SWAYAM Console",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = LocalAIGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LocalAIGreen,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${controlConfig.selectedModel} • ${controlConfig.contextWindowLimit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Right: Actions (New Chat, Settings, Close)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val newSession = ConsoleChatSession(
                                        title = "Chat Session #${sessions.size + 1}",
                                        messages = mutableListOf(
                                            ChatMessage(
                                                isUser = false,
                                                text = "New chat initialized. Ask anything or execute tools.",
                                                provider = AIProviderType.LOCAL
                                            )
                                        )
                                    )
                                    sessions.add(newSession)
                                    currentSessionIndex = sessions.size - 1
                                    Toast.makeText(context, "New chat created", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("console_new_chat_btn")
                            ) {
                                Icon(imageVector = Icons.Default.AddComment, contentDescription = "New Chat", modifier = Modifier.size(20.dp))
                            }

                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("console_tune_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Tune, contentDescription = "Swayam Parameters", modifier = Modifier.size(20.dp))
                            }

                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.testTag("console_close_fullscreen_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Fullscreen Console",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ConsoleChatBubble(
                            message = msg,
                            onShowExplanation = onShowExplanation,
                            onTranslate = { targetLang ->
                                val idx = messages.indexOfFirst { it.id == msg.id }
                                if (idx != -1) {
                                    messages[idx] = msg.copy(isTranslating = true)
                                    coroutineScope.launch {
                                        val transRes = edgeAI.swayam.translate(msg.text, targetLang)
                                        val translated = if (transRes is EdgeResult.Success) transRes.data else msg.text
                                        val cIdx = messages.indexOfFirst { it.id == msg.id }
                                        if (cIdx != -1) {
                                            messages[cIdx] = msg.copy(
                                                translatedText = translated,
                                                activeLanguage = targetLang,
                                                isTranslating = false
                                            )
                                        }
                                    }
                                }
                            },
                            onRevertTranslation = {
                                val idx = messages.indexOfFirst { it.id == msg.id }
                                if (idx != -1) {
                                    messages[idx] = msg.copy(translatedText = null, activeLanguage = null, isTranslating = false)
                                }
                            },
                            onRegenerate = {
                                val idx = messages.indexOfFirst { it.id == msg.id }
                                val prevUser = messages.take(idx).lastOrNull { it.isUser }
                                if (prevUser != null) {
                                    submitQuery(prevUser.text)
                                } else {
                                    submitQuery(msg.text)
                                }
                            },
                            onExport = { textToSave ->
                                coroutineScope.launch {
                                    edgeAI.memory.create(
                                        title = "Console Note: ${textToSave.take(25)}...",
                                        content = textToSave,
                                        tags = "console,swayam,saved"
                                    )
                                    Toast.makeText(context, "Saved to Memory Vault!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    if (isThinking) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text(
                                        text = "SWAYAM reasoning on-device (${controlConfig.selectedModel})...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Prompt Starters
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    items(listOf(
                        "Format a sample JSON schema",
                        "Show system memory stats in a table",
                        "Write Kotlin code for AES encryption",
                        "Compare on-device vs cloud latency",
                        "List recent saved memories"
                    )) { suggestion ->
                        SuggestionChip(
                            onClick = {
                                inputText = suggestion
                                submitQuery(suggestion)
                            },
                            label = { Text(suggestion, fontSize = 11.sp) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Input Bar with Voice & Send
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val check = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                if (check == PackageManager.PERMISSION_GRANTED) {
                                    startSpeechRecognition(context, voiceLauncher) { isListeningVoice = true }
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.testTag("console_mic_btn")
                        ) {
                            Icon(
                                imageVector = if (isListeningVoice) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = if (isListeningVoice) Color.Red else MaterialTheme.colorScheme.primary
                            )
                        }

                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask console, generate code, or execute tools...", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("console_input_field"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (inputText.isNotBlank()) submitQuery(inputText)
                            })
                        )

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) submitQuery(inputText)
                            },
                            modifier = Modifier.testTag("console_send_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Prompt",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleChatBubble(
    message: ChatMessage,
    onShowExplanation: (ExplanationRecord) -> Unit,
    onTranslate: (targetLanguage: String) -> Unit,
    onRevertTranslation: () -> Unit,
    onRegenerate: () -> Unit,
    onExport: (text: String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            border = if (!message.isUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)) else null,
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (message.isTranslating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Text("Translating output...", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    val displayText = message.translatedText ?: message.text
                    RichMessageContent(
                        text = displayText,
                        textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        isUser = message.isUser
                    )
                }

                // Sources grounding badge
                if (!message.isUser && message.sourcesUsed.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LocalAIGreen.copy(alpha = 0.12f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = LocalAIGreen, modifier = Modifier.size(12.dp))
                            Text(
                                text = "Grounded in: ${message.sourcesUsed.joinToString(", ")}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = LocalAIGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Output tools set for AI responses
        if (!message.isUser) {
            Spacer(modifier = Modifier.height(4.dp))
            ResponseActionToolbar(
                responseText = message.text,
                translatedText = message.translatedText,
                activeLanguage = message.activeLanguage,
                onTranslate = onTranslate,
                onRevertTranslation = onRevertTranslation,
                onRegenerate = onRegenerate,
                onExport = onExport,
                explanation = message.explanation,
                onShowExplanation = onShowExplanation,
                modifier = Modifier.widthIn(max = 480.dp)
            )
        }
    }
}

/**
 * Collapsible Swayam Control Sidebar Content.
 */
@Composable
private fun SwayamControlDrawerContent(
    config: SwayamControlConfig,
    onConfigChange: (SwayamControlConfig) -> Unit,
    sessions: List<ConsoleChatSession>,
    activeSessionIndex: Int,
    onSelectSession: (Int) -> Unit,
    onNewSession: () -> Unit,
    onDeleteSession: (Int) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Drawer Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Swayam Control",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            IconButton(onClick = onCloseDrawer) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Sidebar")
            }
        }

        HorizontalDivider()

        // 1. SESSIONS & CHAT HISTORY
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHAT SESSIONS (${sessions.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                TextButton(
                    onClick = onNewSession,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("New Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            sessions.forEachIndexed { idx, s ->
                val isSelected = idx == activeSessionIndex
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSession(idx) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = s.title,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                        if (sessions.size > 1) {
                            IconButton(
                                onClick = { onDeleteSession(idx) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // 2. MODEL SELECTION
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "MODEL ENGINE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            val models = listOf(
                Pair("Gemma 2B (LiteRT-LM Edge)", AIProviderType.LOCAL),
                Pair("Gemini 2.0 Flash", AIProviderType.CLOUD),
                Pair("Gemini 1.5 Pro", AIProviderType.CLOUD),
                Pair("DeepSeek R1 Edge", AIProviderType.LOCAL),
                Pair("Claude 3.5 Sonnet", AIProviderType.CLOUD)
            )

            models.forEach { (modelName, provider) ->
                val isSelected = config.selectedModel == modelName
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onConfigChange(config.copy(selectedModel = modelName, modelProvider = provider))
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = modelName, style = MaterialTheme.typography.bodySmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                            Text(
                                text = if (provider == AIProviderType.LOCAL) "100% On-Device • Zero Egress" else "Cloud Gateway Endpoint",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = if (provider == AIProviderType.LOCAL) LocalAIGreen else MaterialTheme.colorScheme.primary
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // 3. SYSTEM PROMPT & PERSONA
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "SYSTEM PROMPT & PERSONA",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf(
                    Pair("Sovereign Core", "You are SWAYAM, an on-device sovereign AI operating mind. Provide clear, concise answers."),
                    Pair("Code Architect", "You are an expert software architect. Provide clean code snippets in Kotlin/JSON with syntax highlights."),
                    Pair("Deep Researcher", "You are a research scientist. Provide detailed, analytical, citation-backed explanations.")
                )) { (presetName, presetPrompt) ->
                    FilterChip(
                        selected = config.systemPrompt == presetPrompt,
                        onClick = { onConfigChange(config.copy(systemPrompt = presetPrompt)) },
                        label = { Text(presetName, fontSize = 10.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            OutlinedTextField(
                value = config.systemPrompt,
                onValueChange = { onConfigChange(config.copy(systemPrompt = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                textStyle = MaterialTheme.typography.bodySmall,
                shape = RoundedCornerShape(10.dp)
            )
        }

        HorizontalDivider()

        // 4. TECHNICAL PARAMETERS
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "TECHNICAL PARAMETERS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            // Temperature
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Temperature", style = MaterialTheme.typography.bodySmall)
                    Text(String.format("%.2f", config.temperature), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = config.temperature,
                    onValueChange = { onConfigChange(config.copy(temperature = it)) },
                    valueRange = 0.0f..1.0f
                )
            }

            // Top-P
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Top-P", style = MaterialTheme.typography.bodySmall)
                    Text(String.format("%.2f", config.topP), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = config.topP,
                    onValueChange = { onConfigChange(config.copy(topP = it)) },
                    valueRange = 0.1f..1.0f
                )
            }

            // Max Tokens
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Max Output Tokens", style = MaterialTheme.typography.bodySmall)
                    Text("${config.maxTokens}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = config.maxTokens.toFloat(),
                    onValueChange = { onConfigChange(config.copy(maxTokens = it.toInt())) },
                    valueRange = 256f..4096f,
                    steps = 15
                )
            }
        }

        HorizontalDivider()

        // 5. CONTEXT WINDOW & RAG GROUNDING
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "CONTEXT WINDOW & GROUNDING",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SQLite Memory Vault RAG", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = config.isMemoryVaultEnabled,
                    onCheckedChange = { onConfigChange(config.copy(isMemoryVaultEnabled = it)) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Document Knowledge Vault", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = config.isKnowledgeVaultEnabled,
                    onCheckedChange = { onConfigChange(config.copy(isKnowledgeVaultEnabled = it)) }
                )
            }
        }

        HorizontalDivider()

        // 6. TOOLS SELECTION & GATEWAY
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "TOOLS & GATEWAY CONTROLS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Autonomous Execution Gateway", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = config.isToolGatewayAutonomous,
                    onCheckedChange = { onConfigChange(config.copy(isToolGatewayAutonomous = it)) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Code Sandbox Interpreter", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = config.isCodeSandboxEnabled,
                    onCheckedChange = { onConfigChange(config.copy(isCodeSandboxEnabled = it)) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vision OCR & Camera", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = config.isVisionOcrEnabled,
                    onCheckedChange = { onConfigChange(config.copy(isVisionOcrEnabled = it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun startSpeechRecognition(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onStarted: () -> Unit
) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak prompt into Chat Console...")
        }
        onStarted()
        launcher.launch(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Voice recognition not supported on device.", Toast.LENGTH_SHORT).show()
    }
}
