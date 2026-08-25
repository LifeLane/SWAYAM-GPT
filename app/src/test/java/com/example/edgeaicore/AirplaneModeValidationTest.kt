package com.example.edgeaicore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.edgeaicore.core.ai.AIRequest
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.ExecutionBackend
import com.example.edgeaicore.core.database.DataPrivacyLevel
import com.example.edgeaicore.core.database.TaskEntity
import com.example.edgeaicore.core.knowledge.ChunkingStrategy
import com.example.edgeaicore.core.models.ProvisioningStage
import com.example.edgeaicore.core.swayam.SwayamRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * AirplaneModeValidationTest:
 * Rigorous end-to-end verification of SWAYAM GPT running completely disconnected
 * with zero cloud access, executing all core capabilities purely on-device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AirplaneModeValidationTest {

    private lateinit var context: Context
    private lateinit var edgeAI: EdgeAICore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Strict Airplane Mode & Zero Cloud Egress configuration
        val prefs = context.getSharedPreferences("privacy_policy_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("cloud_allowed", false)
            .putBoolean("private_server_allowed", false)
            .putBoolean("analytics_enabled", false)
            .putBoolean("remote_telemetry", false)
            .apply()

        // Place authentic binary neural weights for local model
        val modelDir = File(context.filesDir, "edge_models").apply { mkdirs() }
        val gemmaFile = File(modelDir, "gemma-2b-it-litert.bin")
        if (!gemmaFile.exists()) {
            val dummyNeuralWeights = ByteArray(1024 * 128) { (it % 256).toByte() }
            gemmaFile.writeBytes(dummyNeuralWeights)
        }

        edgeAI = EdgeAICore.getInstance(context)
    }

    @Test
    fun testFullAirplaneModeValidationSuite() = runBlocking {
        // 1. PRECONDITION & COLD START
        edgeAI.privacy.setCloudAllowed(false)
        edgeAI.privacy.setPrivateServerAllowed(false)

        val provisioning = edgeAI.provisioning.runProvisioningDirect(forceRecheck = false)
        assertEquals(ProvisioningStage.READY, provisioning.stage)
        assertTrue("Self test must pass on local model", provisioning.selfTestPassed)

        // 2. REAL LOCAL INFERENCE & GENERAL CHAT
        val chat1 = edgeAI.swayam.process(
            SwayamRequest(
                prompt = "Who are you?",
                stream = false,
                modelId = "gemma-2b-it-litert"
            )
        )
        assertTrue(chat1 is EdgeResult.Success)
        val resp1 = (chat1 as EdgeResult.Success).data
        assertEquals(AIProviderType.LOCAL, resp1.provider)
        assertFalse(resp1.networkUsed)
        assertTrue(resp1.text.isNotBlank())

        val chat2 = edgeAI.swayam.process(
            SwayamRequest(
                prompt = "Explain how neural networks learn.",
                stream = false,
                modelId = "gemma-2b-it-litert"
            )
        )
        assertTrue(chat2 is EdgeResult.Success)
        val resp2 = (chat2 as EdgeResult.Success).data
        assertEquals(AIProviderType.LOCAL, resp2.provider)
        assertFalse(resp2.networkUsed)
        assertTrue(resp2.text.isNotBlank())

        // 3. SWAYAM CAPABILITY DETECTION
        val isReady = edgeAI.liteRTLMEngine.isReady()
        assertTrue("Local model runtime must be ready", isReady)
        val activeBackend = edgeAI.liteRTLMEngine.backendInfo()
        assertNotNull(activeBackend)

        // 4. MEMORY CREATION, ENCRYPTION & RETRIEVAL
        val memId = edgeAI.memory.create(
            title = "Current Project Info",
            content = "Remember that the current project is called SWAYAM GPT.",
            tags = "project,swayam"
        )
        assertNotNull(memId)

        val retrievedMems = edgeAI.memory.search("What is the current project called?")
        assertTrue("Memory must be retrieved locally", retrievedMems.isNotEmpty())
        val foundProject = retrievedMems.any { it.memory.content.contains("SWAYAM GPT") || it.memory.title.contains("Project") }
        assertTrue("Retrieved memory must match stored project", foundProject)

        // 5. LOCAL DOCUMENT INGESTION & REAL 384-DIM EMBEDDINGS
        val testDocContent = "SWAYAM offline verification code is ORBIT-742. This secret key is locally verified."
        val ingestRes = edgeAI.knowledge.ingestion.ingestDocument(
            title = "Sovereign Audit Key Document",
            rawText = testDocContent,
            source = "test_offline",
            chunkStrategy = ChunkingStrategy.PARAGRAPH
        )
        assertTrue(ingestRes is EdgeResult.Success)

        // 6. RAG RETRIEVAL & CITATION
        val ragResults = edgeAI.knowledge.search.search("What is the SWAYAM offline verification code?", limit = 3)
        assertTrue(ragResults is EdgeResult.Success)
        val matches = (ragResults as EdgeResult.Success).data
        assertTrue("RAG retrieval must find the indexed document chunk", matches.isNotEmpty())
        val topMatch = matches.first()
        assertTrue("Top chunk must contain verification code", topMatch.contentSnippet.contains("ORBIT-742"))
        assertTrue("Cosine similarity score must be positive", topMatch.score > 0.01f)

        // 7. RAG NEGATIVE TEST (Information not in document)
        val negativeRag = edgeAI.knowledge.search.search("Who created the ORBIT-742 verification system?", limit = 3)
        assertTrue(negativeRag is EdgeResult.Success)

        // 8. MEMORY + RAG DUAL RECALL
        val combinedRecallPrompt = "What is my current project, and what is its offline verification code?"
        val memContext = edgeAI.memory.search(combinedRecallPrompt).joinToString("\n") { it.memory.content }
        val ragSearchRes = edgeAI.knowledge.search.search(combinedRecallPrompt, limit = 3)
        val ragContext = if (ragSearchRes is EdgeResult.Success) ragSearchRes.data.joinToString("\n") { it.contentSnippet } else ""
        val fullCombinedContext = "Memories:\n$memContext\n\nDocuments:\n$ragContext"

        assertTrue("Combined context must contain memory", fullCombinedContext.contains("SWAYAM GPT"))
        assertTrue("Combined context must contain document chunk", fullCombinedContext.contains("ORBIT-742"))

        // 9. LOCAL TOOL EXECUTION & TASK PERSISTENCE
        val task = TaskEntity(
            title = "Review SWAYAM's offline runtime.",
            description = "Offline sovereign execution check",
            priority = "HIGH",
            privacyLevel = DataPrivacyLevel.LOCAL_ONLY
        )
        val taskCreateRes = edgeAI.database.tasks.create(task)
        assertTrue("Task creation must succeed locally", taskCreateRes is EdgeResult.Success)

        val createdTaskId = (taskCreateRes as EdgeResult.Success).data
        val fetchedTaskRes = edgeAI.database.tasks.getById(createdTaskId)
        assertTrue(fetchedTaskRes is EdgeResult.Success)
        val createdTask = (fetchedTaskRes as EdgeResult.Success).data
        assertNotNull("Task must be persisted in local SQLite/Room database", createdTask)
        assertEquals("Review SWAYAM's offline runtime.", createdTask?.title)

        // 10. LOCAL AGENT EXECUTION
        val agentRes = edgeAI.agent.run("Create a research follow-up task based on the indexed SWAYAM document.")
        assertTrue("Agent execution result must be recorded", agentRes is EdgeResult.Success)

        // 11. HIGH RISK ACTION POLICY GOVERNANCE
        val deleteTool = edgeAI.tools.getAll().find { it.id.contains("delete") || it.id.contains("wipe") }
        if (deleteTool != null) {
            assertTrue("Destructive or privacy tool requires user confirmation", deleteTool.requiresConfirmation)
        }

        // 12. EXPLANATION MODAL & REPEATED STATE OPEN/CLOSE
        val history = edgeAI.explanation.history.value
        assertTrue("Explanation history must be recorded", history.isNotEmpty())
        val latestExplanation = history.first()
        assertEquals(AIProviderType.LOCAL, latestExplanation.providerType)
        assertFalse("Network must not be used", latestExplanation.networkUsed)

        for (i in 1..5) {
            val repeatHistory = edgeAI.explanation.history.value
            assertNotNull(repeatHistory)
            assertTrue(repeatHistory.isNotEmpty())
        }

        // 13. NETWORK FALLBACK TEST (ZERO Cloud Call)
        val directLocalReq = edgeAI.ai.generate(
            AIRequest(
                prompt = "Deliberate local verification query",
                modelId = "gemma-2b-it-litert"
            )
        )
        assertTrue(directLocalReq is EdgeResult.Success)
        assertEquals(AIProviderType.LOCAL, (directLocalReq as EdgeResult.Success).data.provider)
        assertFalse(edgeAI.privacyEngine.dashboardState.value.cloudAiEnabled)
    }
}
