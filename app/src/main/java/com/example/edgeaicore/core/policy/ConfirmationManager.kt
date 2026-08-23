package com.example.edgeaicore.core.policy

import com.example.edgeaicore.core.common.RiskLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class ConfirmationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    EXPIRED
}

/**
 * Structured tool action confirmation proposal.
 */
data class ToolActionProposal(
    val id: String = UUID.randomUUID().toString(),
    val toolId: String,
    val toolName: String,
    val description: String,
    val arguments: Map<String, Any?>,
    val riskLevel: RiskLevel,
    val timestamp: Long = System.currentTimeMillis(),
    val status: ConfirmationStatus = ConfirmationStatus.PENDING,
    val resultMessage: String? = null
)

/**
 * ConfirmationManager: Handles human-in-the-loop review and approval flows.
 * HIGH and CRITICAL tool actions are held here until user explicitly confirms in UI.
 * LLMs cannot self-approve proposals.
 */
class ConfirmationManager {
    private val _proposals = MutableStateFlow<List<ToolActionProposal>>(emptyList())
    val proposals: StateFlow<List<ToolActionProposal>> = _proposals.asStateFlow()

    fun createProposal(
        toolId: String,
        toolName: String,
        description: String,
        arguments: Map<String, Any?>,
        riskLevel: RiskLevel
    ): ToolActionProposal {
        val proposal = ToolActionProposal(
            toolId = toolId,
            toolName = toolName,
            description = description,
            arguments = arguments,
            riskLevel = riskLevel
        )
        _proposals.value = listOf(proposal) + _proposals.value.take(49)
        return proposal
    }

    fun confirm(proposalId: String): ToolActionProposal? {
        val current = _proposals.value
        val index = current.indexOfFirst { it.id == proposalId }
        if (index >= 0 && current[index].status == ConfirmationStatus.PENDING) {
            val updated = current[index].copy(status = ConfirmationStatus.CONFIRMED)
            val newList = current.toMutableList()
            newList[index] = updated
            _proposals.value = newList
            return updated
        }
        return null
    }

    fun cancel(proposalId: String, reason: String = "User cancelled action"): ToolActionProposal? {
        val current = _proposals.value
        val index = current.indexOfFirst { it.id == proposalId }
        if (index >= 0 && current[index].status == ConfirmationStatus.PENDING) {
            val updated = current[index].copy(
                status = ConfirmationStatus.CANCELLED,
                resultMessage = reason
            )
            val newList = current.toMutableList()
            newList[index] = updated
            _proposals.value = newList
            return updated
        }
        return null
    }

    fun getProposal(proposalId: String): ToolActionProposal? {
        return _proposals.value.find { it.id == proposalId }
    }

    fun clear() {
        _proposals.value = emptyList()
    }
}
