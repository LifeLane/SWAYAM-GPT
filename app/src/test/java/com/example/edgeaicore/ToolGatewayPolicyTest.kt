package com.example.edgeaicore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.edgeaicore.core.common.RiskLevel
import com.example.edgeaicore.core.policy.ConfirmationStatus
import com.example.edgeaicore.core.common.EdgeResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ToolGatewayPolicyTest {

    private lateinit var context: Context
    private lateinit var edgeAI: EdgeAICore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        edgeAI = EdgeAICore.getInstance(context)
    }

    @Test
    fun testLowRiskToolExecutesDirectly() = runBlocking {
        val result = edgeAI.gateway.execute(
            toolId = "tasks.create",
            arguments = mapOf("task" to "Read documentation"),
            userConsentGiven = true
        )

        assertTrue(result is EdgeResult.Success)
        val data = (result as EdgeResult.Success).data
        assertTrue(data.success)
        assertEquals("tasks.create", data.toolId)
    }

    @Test
    fun testCriticalRiskToolRequiresConfirmation() = runBlocking {
        // device.modifySystem is CRITICAL risk with requiresConfirmation = true
        val resultWithoutConfirmation = edgeAI.gateway.execute(
            toolId = "device.modifySystem",
            arguments = mapOf("setting" to "cpu_governor", "value" to "performance"),
            userConsentGiven = false
        )

        assertTrue(resultWithoutConfirmation is EdgeResult.Failure)

        // Verify proposal was created in confirmation manager
        val pending = edgeAI.agent.confirmationManager.proposals.value
        assertTrue(pending.any { it.toolId == "device.modifySystem" && it.status == ConfirmationStatus.PENDING })
    }

    @Test
    fun testPreConfirmedCriticalRiskToolExecutesSuccessfully() = runBlocking {
        val proposal = edgeAI.agent.confirmationManager.createProposal(
            toolId = "device.modifySystem",
            toolName = "Modify System",
            description = "Modify system settings",
            arguments = mapOf("setting" to "brightness", "value" to "80%"),
            riskLevel = RiskLevel.CRITICAL
        )
        edgeAI.agent.confirmationManager.confirm(proposal.id)

        val result = edgeAI.gateway.execute(
            toolId = "device.modifySystem",
            arguments = mapOf("setting" to "brightness", "value" to "80%"),
            userConsentGiven = true,
            preConfirmedProposalId = proposal.id
        )

        assertTrue(result is EdgeResult.Success)
    }
}
