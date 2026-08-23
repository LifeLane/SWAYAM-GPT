package com.example.edgeaicore.ui.memory

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.ai.AIRequest
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.explanation.ExplanationRecord
import com.example.edgeaicore.core.memory.RankedMemory
import com.example.edgeaicore.ui.common.AIStatusCard
import com.example.edgeaicore.ui.common.AppCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourcesUsed: List<String> = emptyList(),
    val provider: AIProviderType = AIProviderType.LOCAL,
    val confidence: Float = 0.95f,
    val explanation: ExplanationRecord? = null
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
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf(initialQuery) }
    var isThinking by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                isUser = false,
                text = "I'm your on-device intelligence. Ask me about anything in your personal memories, documents, or captured observations.",
                provider = AIProviderType.LOCAL
            )
        )
    }

    // Process initial query if provided
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && messages.size == 1) {
            val userMsg = ChatMessage(isUser = true, text = initialQuery)
            messages.add(userMsg)
            inputText = ""
            isThinking = true

            coroutineScope.launch {
                val matches: List<RankedMemory> = edgeAI.memory.search(initialQuery)
                val contextStr = if (matches.isNotEmpty()) {
                    "RELEVANT ON-DEVICE MEMORIES:\n" + matches.joinToString("\n") {
                        "- ${it.memory.title}: ${it.memory.content}"
                    }
                } else null

                val response = edgeAI.ai.generate(
                    AIRequest(
                        prompt = initialQuery,
                        context = contextStr,
                        privacyLevel = PrivacyLevel.LOCAL_ONLY
                    )
                )

                val replyText = when (response) {
                    is EdgeResult.Success -> response.data.text
                    is EdgeResult.Failure -> "I couldn't retrieve that from memory: ${response.error.message}"
                }

                val explanation = ExplanationRecord(
                    featureName = "Personal Memory Recall",
                    whatHappened = "Retrieved ${matches.size} memories via local vector cosine similarity and answered using LiteRT-LM.",
                    whyReason = "User asked about stored memory: '$initialQuery'",
                    confidenceScore = if (matches.isNotEmpty()) matches.first().score else 0.85f,
                    dataSourcesUsed = matches.map { it.memory.title },
                    wasAiInvolved = true,
                    providerType = AIProviderType.LOCAL,
                    privacyLevel = PrivacyLevel.LOCAL_ONLY
                )

                messages.add(
                    ChatMessage(
                        isUser = false,
                        text = replyText,
                        sourcesUsed = matches.map { it.memory.title },
                        provider = AIProviderType.LOCAL,
                        explanation = explanation
                    )
                )
                isThinking = false
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ask Memory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Local LiteRT-LM • Zero Data Egress", style = MaterialTheme.typography.labelSmall, color = LocalAIGreen)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        onShowExplanation = onShowExplanation
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
                                text = "Searching memories & reasoning on-device...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Followup Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                items(listOf("What did I save today?", "Where are my documents?", "Show high priority tasks")) { suggestion ->
                    SuggestionChip(
                        onClick = {
                            inputText = suggestion
                        },
                        label = { Text(suggestion, fontSize = 11.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Chat Input Bar
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
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask about anything in memory...", fontSize = 14.sp) },
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
                            if (inputText.isNotBlank() && !isThinking) {
                                val query = inputText
                                inputText = ""
                                messages.add(ChatMessage(isUser = true, text = query))
                                isThinking = true

                                coroutineScope.launch {
                                    val matches = edgeAI.memory.search(query)
                                    val contextStr = if (matches.isNotEmpty()) {
                                        "RELEVANT ON-DEVICE MEMORIES:\n" + matches.joinToString("\n") {
                                            "- ${it.memory.title}: ${it.memory.content}"
                                        }
                                    } else null

                                    val response = edgeAI.ai.generate(
                                        AIRequest(
                                            prompt = query,
                                            context = contextStr,
                                            privacyLevel = PrivacyLevel.LOCAL_ONLY
                                        )
                                    )

                                    val replyText = when (response) {
                                        is EdgeResult.Success -> response.data.text
                                        is EdgeResult.Failure -> "I couldn't retrieve that: ${response.error.message}"
                                    }

                                    val explanation = ExplanationRecord(
                                        featureName = "Personal Memory Recall",
                                        whatHappened = "Retrieved ${matches.size} memories via on-device semantic vector retrieval.",
                                        whyReason = "Prompt query: '$query'",
                                        confidenceScore = if (matches.isNotEmpty()) matches.first().score else 0.85f,
                                        dataSourcesUsed = matches.map { it.memory.title },
                                        wasAiInvolved = true,
                                        providerType = AIProviderType.LOCAL,
                                        privacyLevel = PrivacyLevel.LOCAL_ONLY
                                    )

                                    messages.add(
                                        ChatMessage(
                                            isUser = false,
                                            text = replyText,
                                            sourcesUsed = matches.map { it.memory.title },
                                            provider = AIProviderType.LOCAL,
                                            explanation = explanation
                                        )
                                    )
                                    isThinking = false
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        })
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isThinking) {
                                val query = inputText
                                inputText = ""
                                messages.add(ChatMessage(isUser = true, text = query))
                                isThinking = true

                                coroutineScope.launch {
                                    val matches = edgeAI.memory.search(query)
                                    val contextStr = if (matches.isNotEmpty()) {
                                        "RELEVANT ON-DEVICE MEMORIES:\n" + matches.joinToString("\n") {
                                            "- ${it.memory.title}: ${it.memory.content}"
                                        }
                                    } else null

                                    val response = edgeAI.ai.generate(
                                        AIRequest(
                                            prompt = query,
                                            context = contextStr,
                                            privacyLevel = PrivacyLevel.LOCAL_ONLY
                                        )
                                    )

                                    val replyText = when (response) {
                                        is EdgeResult.Success -> response.data.text
                                        is EdgeResult.Failure -> "I couldn't retrieve that: ${response.error.message}"
                                    }

                                    val explanation = ExplanationRecord(
                                        featureName = "Personal Memory Recall",
                                        whatHappened = "Retrieved ${matches.size} memories via on-device semantic vector retrieval.",
                                        whyReason = "Prompt query: '$query'",
                                        confidenceScore = if (matches.isNotEmpty()) matches.first().score else 0.85f,
                                        dataSourcesUsed = matches.map { it.memory.title },
                                        wasAiInvolved = true,
                                        providerType = AIProviderType.LOCAL,
                                        privacyLevel = PrivacyLevel.LOCAL_ONLY
                                    )

                                    messages.add(
                                        ChatMessage(
                                            isUser = false,
                                            text = replyText,
                                            sourcesUsed = matches.map { it.memory.title },
                                            provider = AIProviderType.LOCAL,
                                            explanation = explanation
                                        )
                                    )
                                    isThinking = false
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        },
                        modifier = Modifier.testTag("ask_memory_send_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onShowExplanation: (ExplanationRecord) -> Unit
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
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )

                // If sources were used
                if (!message.isUser && message.sourcesUsed.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Sources: ${message.sourcesUsed.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalAIGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Explanation link for AI responses
        if (!message.isUser && message.explanation != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .padding(top = 4.dp, start = 4.dp)
                    .clickable { onShowExplanation(message.explanation) }
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Why this answer?",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
