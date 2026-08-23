package com.example.edgeaicore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.edgeaicore.core.server.PrivateAIClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrivateServerConnectivityTest {

    private lateinit var context: Context
    private lateinit var client: PrivateAIClient

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        client = PrivateAIClient(context)
    }

    @Test
    fun testValidConfigurationSavesAndRetrieves() = runBlocking {
        client.configureServer("https://192.168.1.100:8000", "test-token-12345", enabled = true)
        val cfg = client.config.value
        assertEquals("https://192.168.1.100:8000", cfg.baseUrl)
        assertTrue(cfg.enabled)
    }

    @Test
    fun testHealthCheckWithLoopbackEndpoint() = runBlocking {
        client.configureServer("http://127.0.0.1:8000", null, enabled = true)
        val health = client.checkHealth()
        assertNotNull(health)
    }
}
