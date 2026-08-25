package com.example.edgeaicore.ui.playground

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.ExecutionBackend
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.explanation.ExplanationRecord
import com.example.edgeaicore.core.swayam.SwayamRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class PlaygroundUiState(
    val sessions: List<PlaygroundSession> = emptyList(),
    val activeSessionId: String = "",
    val activeSession: PlaygroundSession? = null,
    val inputText: String = "",
    val activeMode: PlaygroundMode = PlaygroundMode.GENERAL,
    val isGenerating: Boolean = false,
    val contextState: PlaygroundContextState = PlaygroundContextState(),
    val selectedSources: List<PlaygroundSource>? = null,
    val activeExplanation: ExplanationRecord? = null,
    val showExecutionDetails: Boolean = false,
    val showModelSelector: Boolean = false,
    val searchQuery: String = "",
    val attachments: List<PlaygroundAttachment> = emptyList(),
    val isDrawerOpen: Boolean = false
)

class PlaygroundViewModel(
    private val context: Context,
    private val edgeAI: EdgeAICore
) : ViewModel() {

    private val repository = PlaygroundRepository(context)

    private val _state = MutableStateFlow(PlaygroundUiState())
    val state: StateFlow<PlaygroundUiState> = _state.asStateFlow()

    private var activeGenerationJob: Job? = null

    init {
        loadSessions()
        refreshContextState()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            val sessions = repository.loadSessions()
            val initialSession = sessions.firstOrNull()
            _state.update { current ->
                current.copy(
                    sessions = sessions,
                    activeSessionId = initialSession?.id ?: "",
                    activeSession = initialSession,
                    activeMode = initialSession?.mode ?: PlaygroundMode.GENERAL
                )
            }
        }
    }

    fun refreshContextState() {
        viewModelScope.launch {
            val specs = edgeAI.diagnostics.specs()
            _state.update { current ->
                current.copy(
                    contextState = PlaygroundContextState(
                        activeMode = current.activeMode,
                        activeModelName = current.activeSession?.modelId ?: "gemma-2b-it-litert",
                        isMemoryActive = true,
                        isRagActive = true,
                        activeSourcesCount = current.activeSession?.messages?.lastOrNull()?.sources?.size ?: 0,
                        toolsReadyCount = edgeAI.tools.getAll().size,
                        isNetworkOffline = true,
                        executionBackend = specs.recommendedBackend
                    )
                )
            }
        }
    }

    fun onInputChanged(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun selectMode(mode: PlaygroundMode) {
        _state.update { current ->
            val updatedSession = current.activeSession?.copy(mode = mode, updatedAt = System.currentTimeMillis())
            val updatedSessions = current.sessions.map { if (it.id == updatedSession?.id) updatedSession else it }
            current.copy(
                activeMode = mode,
                activeSession = updatedSession,
                sessions = updatedSessions,
                contextState = current.contextState.copy(activeMode = mode)
            )
        }
        persistCurrentSessions()
    }

    fun selectSession(sessionId: String) {
        val session = _state.value.sessions.find { it.id == sessionId } ?: return
        _state.update { current ->
            current.copy(
                activeSessionId = sessionId,
                activeSession = session,
                activeMode = session.mode,
                isDrawerOpen = false
            )
        }
        refreshContextState()
    }

    fun createNewSession(mode: PlaygroundMode = PlaygroundMode.GENERAL) {
        viewModelScope.launch {
            val newSession = PlaygroundSession(
                title = "New ${mode.title} Session",
                mode = mode,
                modelId = "gemma-2b-it-litert",
                messages = listOf(
                    PlaygroundMessage(
                        role = MessageRole.ASSISTANT,
                        content = "Initialized new sovereign session in **${mode.title}** mode. What would you like to explore?",
                        provider = AIProviderType.LOCAL,
                        runtime = "LiteRT-LM On-Device",
                        executionMode = mode
                    )
                )
            )
            val updatedSessions = listOf(newSession) + _state.value.sessions
            _state.update { current ->
                current.copy(
                    sessions = updatedSessions,
                    activeSessionId = newSession.id,
                    activeSession = newSession,
                    activeMode = mode,
                    isDrawerOpen = false
                )
            }
            repository.saveSessions(updatedSessions)
            refreshContextState()
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        _state.update { current ->
            val updated = current.sessions.map {
                if (it.id == sessionId) it.copy(title = newTitle.trim(), updatedAt = System.currentTimeMillis()) else it
            }
            val active = if (current.activeSessionId == sessionId) current.activeSession?.copy(title = newTitle.trim()) else current.activeSession
            current.copy(sessions = updated, activeSession = active)
        }
        persistCurrentSessions()
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            val remaining = _state.value.sessions.filter { it.id != sessionId }
            val fallback = remaining.firstOrNull() ?: PlaygroundSession(
                title = "Playground Session",
                mode = PlaygroundMode.GENERAL,
                messages = emptyList()
            )
            val finalSessions = if (remaining.isEmpty()) listOf(fallback) else remaining
            _state.update { current ->
                current.copy(
                    sessions = finalSessions,
                    activeSessionId = fallback.id,
                    activeSession = fallback,
                    activeMode = fallback.mode
                )
            }
            repository.saveSessions(finalSessions)
            refreshContextState()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun toggleDrawer(isOpen: Boolean) {
        _state.update { it.copy(isDrawerOpen = isOpen) }
    }

    fun sendMessage(customPrompt: String? = null) {
        val prompt = (customPrompt ?: _state.value.inputText).trim()
        if (prompt.isBlank() || _state.value.isGenerating) return

        val userMessage = PlaygroundMessage(
            role = MessageRole.USER,
            content = prompt,
            timestamp = System.currentTimeMillis()
        )

        val currentSession = _state.value.activeSession ?: return
        val updatedMessages = currentSession.messages + userMessage
        val sessionWithUser = currentSession.copy(
            messages = updatedMessages,
            title = if (currentSession.messages.isEmpty() || currentSession.title.startsWith("New ")) {
                prompt.take(30)
            } else currentSession.title,
            updatedAt = System.currentTimeMillis()
        )

        _state.update { current ->
            current.copy(
                inputText = "",
                activeSession = sessionWithUser,
                sessions = current.sessions.map { if (it.id == sessionWithUser.id) sessionWithUser else it },
                isGenerating = true
            )
        }

        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                
                // Formulate prompt prefix if in specific mode
                val modePrefix = when (_state.value.activeMode) {
                    PlaygroundMode.RESEARCH -> "[RESEARCH EVIDENCE MODE] "
                    PlaygroundMode.DOCUMENTS -> "[DOCUMENT CITATION RAG MODE] "
                    PlaygroundMode.MEMORY -> "[PERSONAL MEMORY QUERY] "
                    PlaygroundMode.AGENTS -> "[AUTONOMOUS AGENT TASK] "
                    PlaygroundMode.GENERAL -> ""
                }

                val fullPrompt = if (modePrefix.isNotEmpty() && !prompt.startsWith("[")) {
                    "$modePrefix$prompt"
                } else prompt

                val request = SwayamRequest(
                    prompt = fullPrompt,
                    conversationId = currentSession.id,
                    privacyLevel = PrivacyLevel.LOCAL_ONLY,
                    preferredProvider = AIProviderType.LOCAL,
                    modelId = currentSession.modelId
                )

                val responseRes = edgeAI.swayamCore.process(request)
                val duration = (System.currentTimeMillis() - startTime).coerceAtLeast(1)

                when (responseRes) {
                    is EdgeResult.Success -> {
                        val resp = responseRes.data
                        val playgroundSources = resp.sources.mapIndexed { idx, src ->
                            PlaygroundSource(
                                title = "Source ${idx + 1}",
                                snippet = src,
                                relevance = 0.85f - (idx * 0.05f),
                                sourceType = "Document / Knowledge Chunk"
                            )
                        }

                        val assistantMessage = PlaygroundMessage(
                            role = MessageRole.ASSISTANT,
                            content = resp.text,
                            timestamp = System.currentTimeMillis(),
                            model = currentSession.modelId,
                            provider = resp.provider,
                            runtime = "LiteRT-LM On-Device Neural Engine",
                            latencyMs = resp.latencyMs.takeIf { it > 0 } ?: duration,
                            tokensGenerated = resp.tokensGenerated.takeIf { it > 0 } ?: (resp.text.length / 4),
                            tokensPerSecond = if (resp.tokensPerSecond > 0) resp.tokensPerSecond else ((resp.text.length / 4.0) / (duration / 1000.0)),
                            sources = playgroundSources,
                            memoryUsed = resp.memoriesUsed,
                            toolsUsed = resp.toolsUsed,
                            agentUsed = resp.agentsUsed,
                            networkUsed = resp.networkUsed,
                            executionMode = _state.value.activeMode,
                            status = "SUCCESS",
                            explanation = resp.explanation
                        )

                        val finalMessages = sessionWithUser.messages + assistantMessage
                        val finalSession = sessionWithUser.copy(
                            messages = finalMessages,
                            updatedAt = System.currentTimeMillis()
                        )

                        _state.update { current ->
                            current.copy(
                                activeSession = finalSession,
                                sessions = current.sessions.map { if (it.id == finalSession.id) finalSession else it },
                                isGenerating = false
                            )
                        }
                        persistCurrentSessions()
                        refreshContextState()
                    }
                    is EdgeResult.Failure -> {
                        val errorMessage = PlaygroundMessage(
                            role = MessageRole.ASSISTANT,
                            content = "⚠️ **Inference Error**: ${responseRes.error.message ?: "Unable to complete local inference."}\n\nPlease check model provisioning or retry.",
                            timestamp = System.currentTimeMillis(),
                            model = currentSession.modelId,
                            provider = AIProviderType.LOCAL,
                            status = "ERROR",
                            latencyMs = duration
                        )

                        val finalMessages = sessionWithUser.messages + errorMessage
                        val finalSession = sessionWithUser.copy(messages = finalMessages)

                        _state.update { current ->
                            current.copy(
                                activeSession = finalSession,
                                sessions = current.sessions.map { if (it.id == finalSession.id) finalSession else it },
                                isGenerating = false
                            )
                        }
                        persistCurrentSessions()
                    }
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(isGenerating = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        _state.update { it.copy(isGenerating = false) }
        Toast.makeText(context, "Generation stopped", Toast.LENGTH_SHORT).show()
    }

    fun retryLastMessage() {
        val messages = _state.value.activeSession?.messages ?: return
        val lastUserMessage = messages.lastOrNull { it.role == MessageRole.USER }
        if (lastUserMessage != null) {
            sendMessage(lastUserMessage.content)
        }
    }

    fun saveToMemory(content: String) {
        viewModelScope.launch {
            try {
                edgeAI.memory.create(
                    title = "Playground Note",
                    content = content
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Saved to SWAYAM encrypted memory", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to save memory: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun attachDocumentText(fileName: String, content: String) {
        viewModelScope.launch {
            val attachment = PlaygroundAttachment(
                name = fileName,
                sizeBytes = content.length.toLong(),
                mimeType = "text/plain",
                status = AttachmentStatus.PROCESSING,
                progress = 0.2f
            )

            _state.update { it.copy(attachments = it.attachments + attachment) }

            try {
                // Ingest into Knowledge base
                edgeAI.knowledge.ingestion.ingestDocument(
                    title = fileName,
                    rawText = content
                )

                delay(100)

                _state.update { current ->
                    current.copy(
                        attachments = current.attachments.map {
                            if (it.id == attachment.id) it.copy(
                                status = AttachmentStatus.READY,
                                progress = 1.0f,
                                extractedChunks = (content.length / 500).coerceAtLeast(1)
                            ) else it
                        }
                    )
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "$fileName indexed into RAG vault", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                _state.update { current ->
                    current.copy(
                        attachments = current.attachments.map {
                            if (it.id == attachment.id) it.copy(
                                status = AttachmentStatus.FAILED,
                                errorMessage = e.message
                            ) else it
                        }
                    )
                }
            }
        }
    }

    fun showSources(sources: List<PlaygroundSource>) {
        _state.update { it.copy(selectedSources = sources) }
    }

    fun dismissSources() {
        _state.update { it.copy(selectedSources = null) }
    }

    fun showExplanation(record: ExplanationRecord) {
        _state.update { it.copy(activeExplanation = record) }
    }

    fun dismissExplanation() {
        _state.update { it.copy(activeExplanation = null) }
    }

    fun toggleExecutionDetails(show: Boolean) {
        _state.update { it.copy(showExecutionDetails = show) }
    }

    private fun persistCurrentSessions() {
        viewModelScope.launch {
            repository.saveSessions(_state.value.sessions)
        }
    }
}
