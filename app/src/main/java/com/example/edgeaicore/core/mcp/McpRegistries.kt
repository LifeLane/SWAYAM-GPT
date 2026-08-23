package com.example.edgeaicore.core.mcp

import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Registry storing discovered MCP Tools across all connected MCP sessions.
 */
class McpToolRegistry {
    private val _tools = MutableStateFlow<Map<String, McpTool>>(emptyMap())
    val tools: StateFlow<Map<String, McpTool>> = _tools.asStateFlow()

    fun registerTools(serverId: String, toolList: List<McpTool>) {
        val current = _tools.value.toMutableMap()
        toolList.forEach { tool ->
            current["${serverId}.${tool.name}"] = tool.copy(serverId = serverId)
        }
        _tools.value = current
    }

    fun unregisterServer(serverId: String) {
        val current = _tools.value.filterKeys { !it.startsWith("$serverId.") }
        _tools.value = current
    }

    fun getTool(fullToolId: String): McpTool? = _tools.value[fullToolId]

    fun getAllTools(): List<McpTool> = _tools.value.values.toList()

    fun clear() {
        _tools.value = emptyMap()
    }
}

/**
 * Registry storing discovered MCP Resources.
 */
class McpResourceRegistry {
    private val _resources = MutableStateFlow<Map<String, McpResource>>(emptyMap())
    val resources: StateFlow<Map<String, McpResource>> = _resources.asStateFlow()

    fun registerResources(serverId: String, resourceList: List<McpResource>) {
        val current = _resources.value.toMutableMap()
        resourceList.forEach { res ->
            current["${serverId}.${res.uri}"] = res.copy(serverId = serverId)
        }
        _resources.value = current
    }

    fun unregisterServer(serverId: String) {
        val current = _resources.value.filterKeys { !it.startsWith("$serverId.") }
        _resources.value = current
    }

    fun getResource(uri: String): McpResource? = _resources.value.values.find { it.uri == uri }

    fun getAllResources(): List<McpResource> = _resources.value.values.toList()

    fun clear() {
        _resources.value = emptyMap()
    }
}

/**
 * Registry storing discovered MCP Prompts.
 */
class McpPromptRegistry {
    private val _prompts = MutableStateFlow<Map<String, McpPrompt>>(emptyMap())
    val prompts: StateFlow<Map<String, McpPrompt>> = _prompts.asStateFlow()

    fun registerPrompts(serverId: String, promptList: List<McpPrompt>) {
        val current = _prompts.value.toMutableMap()
        promptList.forEach { prompt ->
            current["${serverId}.${prompt.name}"] = prompt.copy(serverId = serverId)
        }
        _prompts.value = current
    }

    fun unregisterServer(serverId: String) {
        val current = _prompts.value.filterKeys { !it.startsWith("$serverId.") }
        _prompts.value = current
    }

    fun getPrompt(name: String): McpPrompt? = _prompts.value.values.find { it.name == name }

    fun getAllPrompts(): List<McpPrompt> = _prompts.value.values.toList()

    fun clear() {
        _prompts.value = emptyMap()
    }
}
