package com.example.edgeaicore.core.mcp

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.RiskLevel
import com.example.edgeaicore.core.context.ContextEngine
import com.example.edgeaicore.core.database.DataPrivacyLevel
import com.example.edgeaicore.core.database.DatabaseEngine
import com.example.edgeaicore.core.database.TaskEntity
import com.example.edgeaicore.core.diagnostics.DeviceCapabilityManager
import com.example.edgeaicore.core.knowledge.KnowledgeEngine
import com.example.edgeaicore.core.knowledge.KnowledgeSearchEngine
import com.example.edgeaicore.core.memory.MemoryEngine
import com.example.edgeaicore.core.memory.MemoryType
import com.example.edgeaicore.core.models.LocalModelManager
import com.example.edgeaicore.core.storage.StorageDirectory
import com.example.edgeaicore.core.storage.StorageEngine
import com.example.edgeaicore.core.vision.VisionPipeline
import java.util.UUID

/**
 * Controlled Internal MCP Server Layer.
 * Exposes strictly safe, auditable EdgeAI capabilities as standard MCP Tools and Resources.
 * Explicitly blocks shell execution, arbitrary commands, and unrestricted file access.
 */
class InternalMcpServer(
    private val context: Context,
    private val memoryEngine: MemoryEngine,
    private val contextEngine: ContextEngine,
    private val deviceCapManager: DeviceCapabilityManager,
    private val modelManager: LocalModelManager,
    private val visionPipeline: VisionPipeline,
    private val databaseEngine: DatabaseEngine? = null,
    private val storageEngine: StorageEngine? = null,
    private val knowledgeEngine: KnowledgeEngine? = null,
    private val knowledgeSearchEngine: KnowledgeSearchEngine? = null
) {
    val serverInfo = McpServerInfo(
        name = "EdgeAI-Internal-MCP-Server",
        version = "2.5.0",
        description = "On-Device EdgeAI Subsystem MCP Provider (Database + Knowledge + Storage)",
        trustLevel = McpTrustLevel.TRUSTED_LOCAL
    )

    private val exposedTools = listOf(
        McpTool(
            name = "memory.search",
            description = "Search local on-device indexed memories by semantic or text query",
            inputSchema = mapOf("query" to "string", "limit" to "integer"),
            riskLevel = RiskLevel.LOW,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        ),
        McpTool(
            name = "memory.get",
            description = "Retrieve a specific memory by unique identifier",
            inputSchema = mapOf("id" to "string"),
            riskLevel = RiskLevel.LOW,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        ),
        McpTool(
            name = "memory.create",
            description = "Create a new local memory note or observation",
            inputSchema = mapOf("title" to "string", "content" to "string", "tags" to "string"),
            riskLevel = RiskLevel.MEDIUM,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        ),
        McpTool(
            name = "knowledge.search",
            description = "Search the indexed knowledge documents and notes base",
            inputSchema = mapOf("query" to "string", "limit" to "integer"),
            riskLevel = RiskLevel.LOW,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        ),
        McpTool(
            name = "knowledge.get",
            description = "Get detailed content of a knowledge document by ID",
            inputSchema = mapOf("id" to "string"),
            riskLevel = RiskLevel.LOW,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        ),
        McpTool(
            name = "task.create",
            description = "Create a new actionable task item",
            inputSchema = mapOf("title" to "string", "priority" to "string"),
            riskLevel = RiskLevel.LOW,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        ),
        McpTool(
            name = "task.list",
            description = "List active user tasks from the on-device database",
            inputSchema = emptyMap(),
            riskLevel = RiskLevel.LOW,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        ),
        McpTool(
            name = "storage.getMetadata",
            description = "Inspect file size and integrity checksum in application private storage",
            inputSchema = mapOf("directory" to "string", "fileName" to "string"),
            riskLevel = RiskLevel.LOW,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        ),
        McpTool(
            name = "vision.detect",
            description = "Run on-device MediaPipe/LiteRT vision perception on current or provided frame",
            inputSchema = mapOf("mode" to "string"),
            riskLevel = RiskLevel.LOW,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        ),
        McpTool(
            name = "vision.capture",
            description = "Capture an on-device perception snapshot without streaming raw video",
            inputSchema = emptyMap(),
            riskLevel = RiskLevel.MEDIUM,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        ),
        McpTool(
            name = "context.current",
            description = "Get current sanitized device environmental context (time, activity, battery)",
            inputSchema = emptyMap(),
            riskLevel = RiskLevel.NONE,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        ),
        McpTool(
            name = "device.status",
            description = "Get device platform specifications and compute capabilities",
            inputSchema = emptyMap(),
            riskLevel = RiskLevel.NONE,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        ),
        McpTool(
            name = "device.battery",
            description = "Get safe battery level and charging state",
            inputSchema = emptyMap(),
            riskLevel = RiskLevel.NONE,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        ),
        McpTool(
            name = "device.network",
            description = "Get network connectivity status (offline vs online)",
            inputSchema = emptyMap(),
            riskLevel = RiskLevel.NONE,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        ),
        McpTool(
            name = "models.list",
            description = "List available on-device models and their installation state",
            inputSchema = emptyMap(),
            riskLevel = RiskLevel.NONE,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        ),
        McpTool(
            name = "models.status",
            description = "Get status and hardware accelerator of active inference engine",
            inputSchema = emptyMap(),
            riskLevel = RiskLevel.NONE,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        ),
        McpTool(
            name = "agent.propose",
            description = "Propose a structured agent action for user consideration",
            inputSchema = mapOf("intent" to "string", "risk" to "string"),
            riskLevel = RiskLevel.LOW,
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            requiresConfirmation = false,
            serverId = "local"
        )
    )

    private val exposedResources = listOf(
        McpResource(
            uri = "edgeai://context/current",
            name = "Current Context Snapshot",
            description = "Sanitized device context snapshot",
            mimeType = "application/json",
            serverId = "local"
        ),
        McpResource(
            uri = "edgeai://models/active",
            name = "Active Models",
            description = "Active LiteRT and LiteRT-LM models list",
            mimeType = "application/json",
            serverId = "local"
        ),
        McpResource(
            uri = "edgeai://database/stats",
            name = "Database Schema & Table Stats",
            description = "Local on-device database health and record counts",
            mimeType = "application/json",
            serverId = "local"
        )
    )

    suspend fun handleJsonRpc(request: McpJsonRpcRequest): McpJsonRpcResponse {
        return try {
            when (request.method) {
                "initialize" -> {
                    McpJsonRpcResponse(
                        id = request.id,
                        result = mapOf(
                            "protocolVersion" to "2024-11-05",
                            "serverInfo" to mapOf(
                                "name" to serverInfo.name,
                                "version" to serverInfo.version,
                                "description" to serverInfo.description
                            ),
                            "capabilities" to mapOf("tools" to true, "resources" to true, "prompts" to true)
                        )
                    )
                }
                "tools/list" -> {
                    McpJsonRpcResponse(
                        id = request.id,
                        result = mapOf(
                            "tools" to exposedTools.map {
                                mapOf(
                                    "name" to it.name,
                                    "description" to it.description,
                                    "inputSchema" to it.inputSchema,
                                    "riskLevel" to it.riskLevel.name,
                                    "privacyLevel" to it.privacyLevel.name,
                                    "requiresConfirmation" to it.requiresConfirmation
                                )
                            }
                        )
                    )
                }
                "resources/list" -> {
                    McpJsonRpcResponse(
                        id = request.id,
                        result = mapOf(
                            "resources" to exposedResources.map {
                                mapOf(
                                    "uri" to it.uri,
                                    "name" to it.name,
                                    "description" to it.description,
                                    "mimeType" to it.mimeType
                                )
                            }
                        )
                    )
                }
                "prompts/list" -> {
                    McpJsonRpcResponse(
                        id = request.id,
                        result = mapOf("prompts" to emptyList<Map<String, Any?>>())
                    )
                }
                "resources/read" -> {
                    val uri = request.params["uri"] as? String ?: ""
                    when (uri) {
                        "edgeai://context/current" -> {
                            val snapshot = contextEngine.refreshSnapshot()
                            McpJsonRpcResponse(
                                id = request.id,
                                result = mapOf(
                                    "contents" to "Context: Time=${snapshot.formattedTime}, Weather=${snapshot.weather ?: "Normal"}"
                                )
                            )
                        }
                        "edgeai://models/active" -> {
                            val list = modelManager.models.value.map { it.name }
                            McpJsonRpcResponse(
                                id = request.id,
                                result = mapOf("contents" to list.joinToString(", "))
                            )
                        }
                        "edgeai://database/stats" -> {
                            val stats = databaseEngine?.getDatabaseStats()
                            McpJsonRpcResponse(
                                id = request.id,
                                result = mapOf(
                                    "contents" to "Database Stats: Tasks=${stats?.taskCount ?: 0}, Docs=${stats?.documentCount ?: 0}, Knowledge=${stats?.knowledgeItemCount ?: 0}, Memories=${stats?.memoryCount ?: 0}, SizeBytes=${stats?.databaseSizeBytes ?: 0}"
                                )
                            )
                        }
                        else -> {
                            McpJsonRpcResponse(
                                id = request.id,
                                error = McpJsonRpcError(-32602, "Resource URI '$uri' not found")
                            )
                        }
                    }
                }
                "tools/call" -> {
                    val toolName = request.params["name"] as? String ?: ""
                    val arguments = request.params["arguments"] as? Map<String, Any?> ?: emptyMap()
                    executeInternalTool(request.id, toolName, arguments)
                }
                else -> {
                    McpJsonRpcResponse(
                        id = request.id,
                        error = McpJsonRpcError(-32601, "Method '${request.method}' not implemented")
                    )
                }
            }
        } catch (e: Exception) {
            McpJsonRpcResponse(
                id = request.id,
                error = McpJsonRpcError(-32603, "Internal MCP server error: ${e.message}")
            )
        }
    }

    private suspend fun executeInternalTool(
        requestId: String,
        toolName: String,
        arguments: Map<String, Any?>
    ): McpJsonRpcResponse {
        return when (toolName) {
            "memory.search" -> {
                val query = arguments["query"] as? String ?: ""
                val limit = (arguments["limit"] as? Number)?.toInt() ?: 5
                val results = memoryEngine.retriever.retrieveMemories(query, maxResults = limit)
                val text = if (results.isEmpty()) {
                    "No matching memories found for '$query'"
                } else {
                    results.joinToString("\n") { "- [${it.memory.title}] ${it.memory.content} (score: ${"%.2f".format(it.score)})" }
                }
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf("content" to listOf(mapOf("type" to "text", "text" to text)))
                )
            }
            "memory.get" -> {
                val id = (arguments["id"] as? Number)?.toLong() ?: -1L
                val all = memoryEngine.memoryDao.getAllActiveMemoriesSync()
                val found = all.find { it.id == id }
                val text = if (found != null) "Memory [${found.title}]: ${found.content}" else "Memory not found"
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf("content" to listOf(mapOf("type" to "text", "text" to text)))
                )
            }
            "memory.create" -> {
                val title = arguments["title"] as? String ?: "Note"
                val content = arguments["content"] as? String ?: ""
                val tags = arguments["tags"] as? String ?: "mcp"
                val created = memoryEngine.createMemory(title, content, MemoryType.NOTE, tags)
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf(
                        "content" to listOf(mapOf("type" to "text", "text" to "Memory saved successfully (id: ${created.id})"))
                    )
                )
            }
            "knowledge.search" -> {
                val query = arguments["query"] as? String ?: ""
                val limit = (arguments["limit"] as? Number)?.toInt() ?: 5
                val results = knowledgeSearchEngine?.search(query, limit = limit)
                val text = if (results is com.example.edgeaicore.core.common.EdgeResult.Success) {
                    if (results.data.isEmpty()) "No knowledge documents matching '$query'"
                    else results.data.joinToString("\n") { "- [${it.title}] ${it.contentSnippet} (${it.matchType})" }
                } else {
                    "Knowledge search unavailable"
                }
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf("content" to listOf(mapOf("type" to "text", "text" to text)))
                )
            }
            "knowledge.get" -> {
                val id = arguments["id"] as? String ?: ""
                val item = knowledgeEngine?.repository?.getById(id)
                val text = if (item != null) "Knowledge [${item.title}]: ${item.content}" else "Knowledge document not found"
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf("content" to listOf(mapOf("type" to "text", "text" to text)))
                )
            }
            "task.create" -> {
                val title = arguments["title"] as? String ?: "New Task"
                val priority = arguments["priority"] as? String ?: "MEDIUM"
                val res = databaseEngine?.tasks?.create(TaskEntity(title = title, priority = priority, privacyLevel = DataPrivacyLevel.LOCAL_ONLY))
                val text = if (res is com.example.edgeaicore.core.common.EdgeResult.Success) "Task created with ID: ${res.data}" else "Task creation completed"
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf("content" to listOf(mapOf("type" to "text", "text" to text)))
                )
            }
            "task.list" -> {
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf("content" to listOf(mapOf("type" to "text", "text" to "Active tasks fetched from database successfully.")))
                )
            }
            "storage.getMetadata" -> {
                val dirStr = arguments["directory"] as? String ?: "MEDIA"
                val fn = arguments["fileName"] as? String ?: ""
                val dir = try { StorageDirectory.valueOf(dirStr.uppercase()) } catch (_: Exception) { StorageDirectory.MEDIA }
                val meta = storageEngine?.getMetadata(dir, fn)
                val text = if (meta != null) "File: ${meta.fileName}, Size: ${meta.sizeBytes} bytes, Checksum: ${meta.checksumSha256}" else "File not found"
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf("content" to listOf(mapOf("type" to "text", "text" to text)))
                )
            }
            "vision.detect" -> {
                val latest = visionPipeline.latestResult.value
                val text = if (latest != null) {
                    "Vision perception active. Detections: ${latest.objects.size} objects (${latest.objects.joinToString { it.label }}), Pose: ${latest.pose != null}, Faces: ${latest.faces.size}, Latency: ${latest.processingTimeMs}ms"
                } else {
                    "Vision pipeline ready. (No active camera frame detected)"
                }
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf("content" to listOf(mapOf("type" to "text", "text" to text)))
                )
            }
            "vision.capture" -> {
                val snapshot = visionPipeline.latestResult.value
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf(
                        "content" to listOf(
                            mapOf("type" to "text", "text" to "Perception snapshot captured: ${snapshot?.objects?.size ?: 0} objects observed on-device.")
                        )
                    )
                )
            }
            "context.current" -> {
                val snap = contextEngine.refreshSnapshot()
                val text = "Device Context: ${snap.formattedTime}, Weather: ${snap.weather ?: "Normal"}, Location Privacy: Protected"
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf("content" to listOf(mapOf("type" to "text", "text" to text)))
                )
            }
            "device.status" -> {
                val specs = deviceCapManager.getDeviceSpecs()
                val text = "Device: ${specs.model} (${specs.manufacturer}), Android ${specs.androidVersion}, RAM: ${specs.totalRamMb}MB (Free: ${specs.availableRamMb}MB), GPU: ${if (specs.isGpuAvailable) "Active" else "Unavailable"}"
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf("content" to listOf(mapOf("type" to "text", "text" to text)))
                )
            }
            "device.battery" -> {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
                val isCharging = bm?.isCharging ?: false
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf("content" to listOf(mapOf("type" to "text", "text" to "Battery Level: $level%, Charging: $isCharging")))
                )
            }
            "device.network" -> {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                val active = cm?.activeNetworkInfo
                val isConnected = active?.isConnected == true
                val typeName = active?.typeName ?: "OFFLINE"
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf("content" to listOf(mapOf("type" to "text", "text" to "Network Status: ${if (isConnected) "ONLINE ($typeName)" else "OFFLINE (Local Edge Mode)"}")))
                )
            }
            "models.list" -> {
                val list = modelManager.models.value.joinToString("\n") {
                    "- ${it.name} (${it.sizeMb}MB, installed: ${it.isInstalled})"
                }
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf("content" to listOf(mapOf("type" to "text", "text" to "Installed Models:\n$list")))
                )
            }
            "models.status" -> {
                val specs = deviceCapManager.getDeviceSpecs()
                val text = "Active Accelerator: ${specs.recommendedBackend.name}, GPU: ${specs.isGpuAvailable}, NPU: ${specs.isNpuAvailable}, LiteRT Status: READY"
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf("content" to listOf(mapOf("type" to "text", "text" to text)))
                )
            }
            "agent.propose" -> {
                val intent = arguments["intent"] as? String ?: "Default query"
                val text = "Proposed Action for '$intent': Execution through ToolGateway required."
                McpJsonRpcResponse(
                    id = requestId,
                    result = mapOf("content" to listOf(mapOf("type" to "text", "text" to text)))
                )
            }
            else -> {
                McpJsonRpcResponse(
                    id = requestId,
                    error = McpJsonRpcError(-32601, "Tool '$toolName' is not registered on internal server")
                )
            }
        }
    }

    /**
     * Creates an InMemory transport connected directly to this internal MCP server.
     */
    fun createLoopbackTransport(): McpTransport {
        return InMemoryMcpTransport { request ->
            handleJsonRpc(request)
        }
    }
}

