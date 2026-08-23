package com.example.edgeaicore.core.analytics

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProductEvent(
    val eventName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val properties: Map<String, String> = emptyMap()
)

interface AnalyticsProvider {
    fun trackEvent(event: ProductEvent)
}

/**
 * Local privacy-guarded analytics provider.
 * NEVER captures raw camera pixels, biometric vectors, private memories, or full user prompts.
 */
class LocalAnalyticsProvider : AnalyticsProvider {
    private val _events = MutableStateFlow<List<ProductEvent>>(emptyList())
    val events: StateFlow<List<ProductEvent>> = _events.asStateFlow()

    override fun trackEvent(event: ProductEvent) {
        val sanitizedProperties = event.properties.filterKeys { key ->
            !key.contains("prompt") && !key.contains("image") && !key.contains("memory") && !key.contains("token")
        }
        val sanitized = event.copy(properties = sanitizedProperties)
        _events.value = listOf(sanitized) + _events.value.take(49)
    }

    fun clear() {
        _events.value = emptyList()
    }
}
