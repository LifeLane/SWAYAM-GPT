package com.example.edgeaicore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.edgeaicore.core.agent.AgentProfile
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
class AgentRuntimeOrchestrationTest {

    private lateinit var context: Context
    private lateinit var edgeAI: EdgeAICore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        edgeAI = EdgeAICore.getInstance(context)
    }

    @Test
    fun testSimpleAgentIntentExecutesSuccessfully() = runBlocking {
        val result = edgeAI.agent.run(
            request = "What tasks do I have scheduled for today?",
            profile = AgentProfile.PRODUCTIVITY,
            userConsentGiven = true
        )

        assertTrue(result is EdgeResult.Success)
        val data = (result as EdgeResult.Success).data
        assertNotNull(data)
        assertTrue(data.steps.isNotEmpty())
        assertTrue(data.finalResponse.isNotBlank())
        assertTrue(data.tokensUsed > 0)
    }

    @Test
    fun testAgentProfileRestrictsCapabilities() = runBlocking {
        val profiles = edgeAI.agent.getProfiles()
        assertTrue(profiles.isNotEmpty())
        val memoryProfile = edgeAI.agent.getProfile("profile_memory")
        assertNotNull(memoryProfile)
        assertEquals("Memory Lens", memoryProfile?.name)
    }
}
