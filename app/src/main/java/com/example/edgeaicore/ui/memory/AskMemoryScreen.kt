package com.example.edgeaicore.ui.memory

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourcesUsed: List<String> = emptyList(),
    val provider: AIProviderType = AIProviderType.LOCAL,
    val confidence: Float = 0.95f,
    val explanation: ExplanationRecord? = null,
    val translatedText: String? = null,
    val activeLanguage: String? = null,
    val isTranslating: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskMemoryScreen(
    edgeAI: EdgeAICore,
    initialQuery: String = "",
    onNavigateBack: () -> Unit,
    onShowExplanation: (ExplanationRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf(initialQuery) }
    var isThinking by remember { mutableStateOf(false) }
    var isListeningVoice by remember { mutableStateOf(false) }
    var isToneMenuOpen by remember { mutableStateOf(false) }

    val personaState by edgeAI.swayam.personaState.collectAsStateWithLifecycle()

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                isUser = false,
                text = "Hello! I am **SWAYAM**, your personal on-device AI operating mind.\n\n" +
                        "Ask me anything about your stored memories, research documents in the RAG vault, or system tasks. Every output comes equipped with tools to **Copy**, **Translate (Hindi/Bengali)**, **Share**, and **Export**.",
                provider = AIProviderType.LOCAL,
                confidence = 1.0f,
                explanation = ExplanationRecord(
                    featureName = "SWAYAM Initializer",
                    whatHappened = "Booted SWAYAM neural mind with on-device sovereign memory and zero-egress protocol.",
                    whyReason = "User opened Ask Memory session.",
                    confidenceScore = 1.0f,
                    dataSourcesUsed = listOf("Swayam Sovereign Core"),
                    wasAiInvolved = true,
                    providerType = AIProviderType.LOCAL,
                    privacyLevel = PrivacyLevel.LOCAL_ONLY
                )
            )
        )
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
                    privacyLevel = PrivacyLevel.LOCAL_ONLY,
                    userConsent = true
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
                            text = "I encountered an issue processing your request: ${response.error.message}",
                            provider = AIProviderType.LOCAL
                        )
                    )
                }
            }
            isThinking = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Voice Recognition Intent Launcher
    val voiceRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListeningVoice = false
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = spokenText
                Toast.makeText(context, "Heard: \"$spokenText\"", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Audio Permission Request Launcher
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecognition(context, voiceRecognitionLauncher) { isListeningVoice = true }
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input.", Toast.LENGTH_SHORT).show()
        }
    }

    fun activateMicrophone() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecognition(context, voiceRecognitionLauncher) { isListeningVoice = true }
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Process initial query if provided
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && messages.size == 1) {
            submitQuery(initialQuery)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("SWAYAM Mind", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = LocalAIGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "SOVEREIGN CORE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = LocalAIGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("On-Device Neural Engine • Hindi / Bengali Ready", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Quick-Access Tone Switcher Button & Dropdown Menu
                    Box {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { isToneMenuOpen = true }
                                .padding(horizontal = 2.dp)
                                .testTag("swayam_tone_menu_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(personaState.responseStyle.emoji, fontSize = 13.sp)
                                Text(
                                    text = personaState.responseStyle.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch Tone",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isToneMenuOpen,
                            onDismissRequest = { isToneMenuOpen = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .widthIn(min = 260.dp)
                        ) {
                            Text(
                                text = "SWITCH SWAYAM TONE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                letterSpacing = 1.sp
                            )
                            HorizontalDivider()

                            ResponseStyle.values().forEach { style ->
                                val isSelected = personaState.responseStyle == style
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(style.emoji, fontSize = 16.sp)
                                                Text(
                                                    text = style.displayName,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isSelected) {
                                                    Spacer(Modifier.weight(1f))
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = LocalAIGreen,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = style.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        isToneMenuOpen = false
                                        edgeAI.swayam.setResponseStyle(style)
                                        Toast.makeText(context, "SWAYAM tone switched to ${style.displayName}", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("tone_option_${style.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        onShowExplanation = onShowExplanation,
                        onTranslate = { targetLang ->
                            val index = messages.indexOfFirst { it.id == msg.id }
                            if (index != -1) {
                                messages[index] = msg.copy(isTranslating = true)
                                coroutineScope.launch {
                                    val transResult = edgeAI.swayam.translate(msg.text, targetLang)
                                    val translated = if (transResult is EdgeResult.Success) transResult.data else msg.text
                                    val curIndex = messages.indexOfFirst { it.id == msg.id }
                                    if (curIndex != -1) {
                                        messages[curIndex] = msg.copy(
                                            translatedText = translated,
                                            activeLanguage = targetLang,
                                            isTranslating = false
                                        )
                                    }
                                }
                            }
                        },
                        onRevertTranslation = {
                            val index = messages.indexOfFirst { it.id == msg.id }
                            if (index != -1) {
                                messages[index] = msg.copy(translatedText = null, activeLanguage = null, isTranslating = false)
                            }
                        },
                        onRegenerate = {
                            val msgIndex = messages.indexOfFirst { it.id == msg.id }
                            val prevUserMsg = messages.take(msgIndex).lastOrNull { it.isUser }
                            if (prevUserMsg != null) {
                                submitQuery(prevUserMsg.text)
                            } else {
                                submitQuery(msg.text)
                            }
                        },
                        onExport = { textToExport ->
                            coroutineScope.launch {
                                val firstLine = textToExport.lines().firstOrNull { it.isNotBlank() } ?: "SWAYAM Output"
                                val title = if (firstLine.length > 30) firstLine.take(28) + "..." else firstLine
                                edgeAI.memory.create(
                                    title = title.replace("#", "").trim(),
                                    content = textToExport,
                                    tags = "swayam,exported,chat"
                                )
                                Toast.makeText(context, "Saved note to Memory Vault!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                if (isThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                text = "SWAYAM reasoning on-device in ${personaState.responseStyle.displayName} tone...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Followup Suggestions & Active Persona Indicator
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                items(listOf(
                    "What can you help me with?",
                    "What did I save today?",
                    "Where are my documents?",
                    "How do I use RAG?",
                    "Show high priority tasks"
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

            // Voice Listening Banner (when active)
            if (isListeningVoice) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Listening to speech... Speak clearly to SWAYAM",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Chat Input Bar with Microphone Activation
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                    // Voice Input Microphone Button
                    IconButton(
                        onClick = { activateMicrophone() },
                        modifier = Modifier.testTag("ask_memory_mic_btn")
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
                        placeholder = { Text("Ask SWAYAM or tap mic to speak...", fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ask_memory_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) {
                                submitQuery(inputText)
                            }
                        })
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                submitQuery(inputText)
                            }
                        },
                        modifier = Modifier.testTag("ask_memory_send_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper to launch Android SpeechRecognizer Intent.
 */
private fun startVoiceRecognition(
    context: android.content.Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onStarted: () -> Unit
) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to SWAYAM GPT (Hands-Free Voice Input)...")
        }
        onStarted()
        launcher.launch(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Voice recognition not supported on this device.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun ChatBubble(
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
            color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = if (!message.isUser) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null,
            modifier = Modifier.widthIn(max = 350.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (message.isTranslating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Text("Translating with SWAYAM multi-lingual engine...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val displayText = message.translatedText ?: message.text
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // If sources were used
                if (!message.isUser && message.sourcesUsed.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LocalAIGreen.copy(alpha = 0.1f),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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

        // Output Response Action Toolbar (Only for AI responses)
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
                modifier = Modifier.widthIn(max = 350.dp)
            )
        }
    }
}
