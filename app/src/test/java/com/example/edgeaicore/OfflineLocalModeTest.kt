package com.example.edgeaicore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.edgeaicore.core.ai.AIRequest
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
class OfflineLocalModeTest {

    private lateinit var context: Context
    private lateinit var edgeAI: EdgeAICore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        edgeAI = EdgeAICore.getInstance(context)
    }

    @Test
    fun testOfflineLocalOnlyExecutionSucceeds() = runBlocking {
        // Ensure remote AI is disabled
        edgeAI.privacy.setCloudAllowed(false)
        edgeAI.privacy.setPrivateServerAllowed(false)

        // Memory creation and retrieval operates fully on device
        edgeAI.memory.create("Offline memo", "Explain local models", tags = "offline")
        val search = edgeAI.memory.search("local models")
        assertTrue(search.isNotEmpty())

        // Provision local model artifact for offline neural inference verification
        val modelFile = java.io.File(context.filesDir, "edge_models/gemma-2b-it-litert.bin").apply {
            parentFile?.mkdirs()
            writeBytes("TEST_GEMMA_LITERT_MODEL_BINARY_MOCK".toByteArray())
        }
        val installRes = edgeAI.models.install("gemma-2b-it-litert")
        assertTrue(installRes is EdgeResult.Success)

        // Local inference succeeds in offline mode
        val response = edgeAI.ai.generate(AIRequest(prompt = "Explain edge computing", modelId = "gemma-2b-it-litert"))
        assertTrue(response is EdgeResult.Success)
        val data = (response as EdgeResult.Success).data
        assertTrue(data.text.isNotBlank())
    }
}
