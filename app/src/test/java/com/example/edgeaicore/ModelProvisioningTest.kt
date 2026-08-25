package com.example.edgeaicore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.edgeaicore.core.models.ProvisioningStage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ModelProvisioningTest {

    private lateinit var context: Context
    private lateinit var edgeAI: EdgeAICore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        edgeAI = EdgeAICore.getInstance(context)
    }

    @Test
    fun testAutomaticProvisioningPipelineCompletesSuccessfully() = runBlocking {
        // Run direct provisioning pipeline
        val state = edgeAI.provisioning.runProvisioningDirect(forceRecheck = true)

        assertEquals(ProvisioningStage.READY, state.stage)
        assertTrue(state.selfTestPassed)
        assertTrue(state.activeModelId.isNotBlank())

        // Check file exists in edge_models
        val modelFile = File(context.filesDir, "edge_models/${state.activeModelId}.bin")
        assertTrue(modelFile.exists())
        assertTrue(modelFile.length() > 0)
    }
}
