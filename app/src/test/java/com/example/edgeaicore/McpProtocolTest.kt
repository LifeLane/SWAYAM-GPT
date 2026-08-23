package com.example.edgeaicore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.edgeaicore.core.mcp.McpJsonRpcRequest
import com.example.edgeaicore.core.mcp.McpTrustLevel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class McpProtocolTest {

    private lateinit var context: Context
    private lateinit var edgeAI: EdgeAICore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        edgeAI = EdgeAICore.getInstance(context)
    }

    @Test
    fun testLoopbackMcpServerHandlesJsonRpcRequests() = runBlocking {
        val server = edgeAI.internalMcpServer
        val response = server.handleJsonRpc(
            McpJsonRpcRequest(
                id = "req-test-1",
                method = "tools/list",
                params = emptyMap()
            )
        )

        assertNotNull(response)
        assertNull(response.error)
        assertNotNull(response.result)
        val resultMap = response.result as? Map<*, *>
        assertNotNull(resultMap)
        val toolsList = resultMap?.get("tools")
        assertNotNull(toolsList)
        val tools = toolsList as? List<*>
        assertTrue(tools != null && tools.isNotEmpty())
    }

    @Test
    fun testSecurityManagerEnforcesTrustLevels() {
        val securityManager = edgeAI.mcpClient.securityManager
        securityManager.authorizeServer("untrusted_ext", McpTrustLevel.UNTRUSTED)
        assertEquals(McpTrustLevel.UNTRUSTED, securityManager.getTrustLevel("untrusted_ext"))

        val validation = securityManager.validateToolInvocation(
            serverId = "untrusted_ext",
            toolName = "any_tool",
            requiredPrivacy = com.example.edgeaicore.core.common.PrivacyLevel.LOCAL_ONLY
        )
        assertTrue(validation is com.example.edgeaicore.core.common.EdgeResult.Failure)
    }
}
