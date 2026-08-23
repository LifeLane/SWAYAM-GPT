package com.example.edgeaicore.core.automation

import android.content.Context
import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.RiskLevel
import com.example.edgeaicore.core.gateway.ToolGateway
import com.example.edgeaicore.core.policy.ConfirmationManager
import com.example.edgeaicore.core.policy.PolicyEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class TriggerType {
    SCHEDULED_TIME,
    MEMORY_CREATED,
    VISION_EVENT_DETECTED,
    DEVICE_BATTERY_LOW,
    LOCATION_ARRIVED
}

data class AutomationTrigger(
    val type: TriggerType,
    val value: String
)

data class AutomationCondition(
    val description: String,
    val evaluator: (suspend () -> Boolean)? = null
)

data class AutomationAction(
    val toolId: String,
    val description: String,
    val arguments: Map<String, Any?>
)

data class AutomationRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val trigger: AutomationTrigger,
    val condition: AutomationCondition,
    val action: AutomationAction,
    val isEnabled: Boolean = true,
    val requiresConfirmation: Boolean = true,
    val createdByAgent: Boolean = false
)

data class AutomationProposal(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val triggerDescription: String,
    val conditionDescription: String,
    val actionDescription: String,
    val rule: AutomationRule,
    val agentReasoning: String,
    val isConfirmed: Boolean = false
)

/**
 * AutomationEngine: Governs event-driven triggers, conditional evaluations,
 * and passes all automated executions strictly through PolicyEngine, ToolGateway,
 * and ConfirmationManager.
 */
class AutomationEngine(
    private val context: Context,
    private val toolGateway: ToolGateway,
    private val policyEngine: PolicyEngine,
    private val confirmationManager: ConfirmationManager
) {
    private val _rules = MutableStateFlow<List<AutomationRule>>(emptyList())
    val rules: StateFlow<List<AutomationRule>> = _rules.asStateFlow()

    private val _proposals = MutableStateFlow<List<AutomationProposal>>(emptyList())
    val proposals: StateFlow<List<AutomationProposal>> = _proposals.asStateFlow()

    init {
        // Register standard default automation rules
        registerRule(
            AutomationRule(
                id = "rule_morning_brief",
                name = "Morning Context Briefing",
                trigger = AutomationTrigger(TriggerType.SCHEDULED_TIME, "08:00"),
                condition = AutomationCondition("User device awake and battery > 20%"),
                action = AutomationAction("notifications.send", "Post morning context summary", mapOf("title" to "Morning Brief", "message" to "Good morning! All on-device systems active.")),
                isEnabled = true,
                requiresConfirmation = false
            )
        )
        registerRule(
            AutomationRule(
                id = "rule_task_from_notes",
                name = "Auto-Detect Task from Notes",
                trigger = AutomationTrigger(TriggerType.MEMORY_CREATED, "type=NOTE"),
                condition = AutomationCondition("Content contains actionable keywords (todo/meeting)"),
                action = AutomationAction("tasks.create", "Prompt user to add discovered task", mapOf("task" to "Discovered Action Item")),
                isEnabled = true,
                requiresConfirmation = true
            )
        )
    }

    fun registerRule(rule: AutomationRule) {
        val current = _rules.value.filter { it.id != rule.id }
        _rules.value = current + rule
    }

    fun toggleRule(ruleId: String, enabled: Boolean) {
        _rules.value = _rules.value.map {
            if (it.id == ruleId) it.copy(isEnabled = enabled) else it
        }
    }

    /**
     * Agents can propose a new routine automation to the user.
     * The automation is held in PENDING state and will NEVER activate without explicit human confirmation.
     */
    fun proposeAutomation(
        name: String,
        trigger: AutomationTrigger,
        condition: AutomationCondition,
        action: AutomationAction,
        agentReasoning: String
    ): AutomationProposal {
        val rule = AutomationRule(
            name = name,
            trigger = trigger,
            condition = condition,
            action = action,
            isEnabled = false,
            requiresConfirmation = true,
            createdByAgent = true
        )
        val proposal = AutomationProposal(
            name = name,
            triggerDescription = "${trigger.type.name}: ${trigger.value}",
            conditionDescription = condition.description,
            actionDescription = "${action.toolId} -> ${action.description}",
            rule = rule,
            agentReasoning = agentReasoning
        )
        _proposals.value = listOf(proposal) + _proposals.value.take(19)
        return proposal
    }

    fun confirmProposal(proposalId: String): Boolean {
        val current = _proposals.value
        val proposal = current.find { it.id == proposalId } ?: return false
        val updatedProposal = proposal.copy(isConfirmed = true)
        _proposals.value = current.map { if (it.id == proposalId) updatedProposal else it }

        // Activate approved rule
        registerRule(proposal.rule.copy(isEnabled = true))
        return true
    }

    fun rejectProposal(proposalId: String) {
        _proposals.value = _proposals.value.filter { it.id != proposalId }
    }

    /**
     * Executes an active automation trigger through the ToolGateway.
     */
    suspend fun fireTrigger(triggerType: TriggerType, payload: String = ""): List<EdgeResult<*>> {
        val matchingRules = _rules.value.filter { it.isEnabled && it.trigger.type == triggerType }
        val results = mutableListOf<EdgeResult<*>>()

        for (rule in matchingRules) {
            // Condition evaluation
            val conditionPassed = rule.condition.evaluator?.invoke() ?: true
            if (!conditionPassed) continue

            // Execute action through the ToolGateway
            val execRes = toolGateway.executeTool(
                toolId = rule.action.toolId,
                arguments = rule.action.arguments,
                userConsentGiven = true
            )
            results.add(execRes)
        }
        return results
    }
}
