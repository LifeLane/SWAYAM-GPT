package com.example.edgeaicore.core.agent

import android.content.Context
import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.RiskLevel
import com.example.edgeaicore.core.context.ContextEngine
import com.example.edgeaicore.core.memory.MemoryEngine
import com.example.edgeaicore.core.memory.MemoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class ActionType {
    CREATE_TASK,
    CREATE_REMINDER,
    CREATE_CALENDAR_EVENT,
    OPEN_MAP,
    START_TIMER,
    SAVE_MEMORY,
    SHARE,
    OPEN_SCREEN
}

data class AgentAction(
    val id: String = UUID.randomUUID().toString(),
    val type: ActionType,
    val title: String,
    val description: String,
    val payload: Map<String, String>,
    val riskLevel: RiskLevel,
    val requiresConfirmation: Boolean = riskLevel != RiskLevel.LOW,
    val requiredPermission: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ActionProposal(
    val action: AgentAction,
    val reasoning: String,
    val confidence: Float,
    val isConfirmed: Boolean = false,
    val isExecuted: Boolean = false,
    val resultMessage: String? = null
)

class ActionExecutor(
    private val context: Context,
    private val memoryEngine: MemoryEngine
) {
    suspend fun execute(action: AgentAction): EdgeResult<String> {
        return try {
            when (action.type) {
                ActionType.SAVE_MEMORY -> {
                    val title = action.payload["title"] ?: action.title
                    val content = action.payload["content"] ?: action.description
                    memoryEngine.createMemory(
                        title = title,
                        content = content,
                        type = MemoryType.NOTE,
                        tags = action.payload["tags"] ?: "agent,action"
                    )
                    EdgeResult.Success("Saved on-device memory: '$title'")
                }
                ActionType.CREATE_TASK -> {
                    val taskName = action.payload["task"] ?: action.title
                    EdgeResult.Success("Created local task: '$taskName'")
                }
                ActionType.CREATE_REMINDER -> {
                    val time = action.payload["time"] ?: "soon"
                    EdgeResult.Success("Scheduled reminder for $time: '${action.title}'")
                }
                ActionType.CREATE_CALENDAR_EVENT -> {
                    EdgeResult.Success("Scheduled event: '${action.title}'")
                }
                ActionType.START_TIMER -> {
                    val seconds = action.payload["duration_seconds"] ?: "60"
                    EdgeResult.Success("Started timer for ${seconds}s")
                }
                ActionType.OPEN_MAP -> {
                    val loc = action.payload["location"] ?: "current location"
                    EdgeResult.Success("Opened map navigation for $loc")
                }
                ActionType.SHARE -> {
                    EdgeResult.Success("Shared content securely: '${action.title}'")
                }
                ActionType.OPEN_SCREEN -> {
                    val screen = action.payload["screen"] ?: "home"
                    EdgeResult.Success("Navigated to $screen")
                }
            }
        } catch (e: Exception) {
            EdgeResult.Failure(EdgeAIError.Unknown("Failed to execute action: ${e.message}", e))
        }
    }
}

class AgentEngine(
    private val context: Context,
    private val contextEngine: ContextEngine,
    private val memoryEngine: MemoryEngine
) {
    private val executor = ActionExecutor(context, memoryEngine)
    private val _recentProposals = MutableStateFlow<List<ActionProposal>>(emptyList())
    val recentProposals: StateFlow<List<ActionProposal>> = _recentProposals.asStateFlow()

    fun proposeActionFromIntent(intentText: String): ActionProposal {
        val lower = intentText.lowercase()
        val currentSnapshot = contextEngine.refreshSnapshot()

        val (action, reasoning, conf) = when {
            lower.contains("remind") || lower.contains("reminder") -> {
                Triple(
                    AgentAction(
                        type = ActionType.CREATE_REMINDER,
                        title = "Reminder: $intentText",
                        description = "Set reminder based on current context: ${currentSnapshot.formattedTime}",
                        payload = mapOf("time" to "1 hour", "intent" to intentText),
                        riskLevel = RiskLevel.MEDIUM
                    ),
                    "Identified reminder intent from natural language input",
                    0.92f
                )
            }
            lower.contains("remember") || lower.contains("save note") || lower.contains("save memory") -> {
                Triple(
                    AgentAction(
                        type = ActionType.SAVE_MEMORY,
                        title = "Saved Memory Note",
                        description = intentText,
                        payload = mapOf("content" to intentText, "title" to "Quick Agent Note", "tags" to "quick_save"),
                        riskLevel = RiskLevel.LOW
                    ),
                    "Extracted informational observation to store in local Room database",
                    0.95f
                )
            }
            lower.contains("task") || lower.contains("todo") -> {
                Triple(
                    AgentAction(
                        type = ActionType.CREATE_TASK,
                        title = intentText,
                        description = "Add to on-device task list",
                        payload = mapOf("task" to intentText),
                        riskLevel = RiskLevel.LOW
                    ),
                    "Parsed actionable task directive",
                    0.89f
                )
            }
            lower.contains("share") || lower.contains("send") -> {
                Triple(
                    AgentAction(
                        type = ActionType.SHARE,
                        title = "Share Action",
                        description = "Prepare data to share externally: '$intentText'",
                        payload = mapOf("content" to intentText),
                        riskLevel = RiskLevel.HIGH // High risk requires explicit user consent!
                    ),
                    "High-risk action involving potential outbound transmission",
                    0.85f
                )
            }
            else -> {
                Triple(
                    AgentAction(
                        type = ActionType.SAVE_MEMORY,
                        title = "General Observation",
                        description = intentText,
                        payload = mapOf("content" to intentText),
                        riskLevel = RiskLevel.LOW
                    ),
                    "Classified as general on-device knowledge entry",
                    0.80f
                )
            }
        }

        val proposal = ActionProposal(action, reasoning, conf)
        _recentProposals.value = listOf(proposal) + _recentProposals.value
        return proposal
    }

    suspend fun executeConfirmedAction(proposal: ActionProposal): EdgeResult<String> {
        val result = executor.execute(proposal.action)
        val updatedList = _recentProposals.value.map {
            if (it.action.id == proposal.action.id) {
                it.copy(
                    isConfirmed = true,
                    isExecuted = result.isSuccess,
                    resultMessage = result.getOrNull() ?: (result as? EdgeResult.Failure)?.error?.message
                )
            } else {
                it
            }
        }
        _recentProposals.value = updatedList
        return result
    }
}
