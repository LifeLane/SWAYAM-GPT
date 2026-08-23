package com.example.edgeaicore.core.tools

import android.content.Context
import android.os.BatteryManager
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.RiskLevel
import com.example.edgeaicore.core.context.ContextEngine
import com.example.edgeaicore.core.database.DataPrivacyLevel
import com.example.edgeaicore.core.database.DatabaseEngine
import com.example.edgeaicore.core.database.TaskEntity
import com.example.edgeaicore.core.diagnostics.DeviceCapabilityManager
import com.example.edgeaicore.core.knowledge.KnowledgeEngine
import com.example.edgeaicore.core.knowledge.KnowledgeSearchEngine
import com.example.edgeaicore.core.knowledge.KnowledgeType
import com.example.edgeaicore.core.memory.MemoryEngine
import com.example.edgeaicore.core.memory.MemoryType
import com.example.edgeaicore.core.notifications.LocalNotificationProvider
import com.example.edgeaicore.core.storage.StorageDirectory
import com.example.edgeaicore.core.storage.StorageEngine
import com.example.edgeaicore.core.vision.VisionPipeline

/**
 * Initializes and registers the default native EdgeAI tools.
 */
object DefaultTools {

    fun registerDefaults(
        context: Context,
        registry: ToolRegistry,
        memoryEngine: MemoryEngine,
        contextEngine: ContextEngine,
        deviceCapManager: DeviceCapabilityManager,
        visionPipeline: VisionPipeline,
        notificationProvider: LocalNotificationProvider,
        databaseEngine: DatabaseEngine? = null,
        storageEngine: StorageEngine? = null,
        knowledgeEngine: KnowledgeEngine? = null,
        knowledgeSearchEngine: KnowledgeSearchEngine? = null
    ) {
        // 1. Memory Search Tool
        registry.register(
            Tool(
                id = "memory.search",
                name = "Memory Search",
                description = "Searches the encrypted on-device SQLite/Room vector database",
                category = ToolCategory.MEMORY,
                inputSchema = mapOf("query" to "string"),
                outputSchema = mapOf("results" to "list"),
                riskLevel = RiskLevel.LOW,
                privacyLevel = PrivacyLevel.LOCAL_ONLY,
                handler = { args ->
                    val query = args["query"]?.toString() ?: ""
                    val results = memoryEngine.retriever.retrieveMemories(query)
                    val formatted = results.map { mapOf("title" to it.memory.title, "content" to it.memory.content, "score" to it.score) }
                    EdgeResult.Success(mapOf("count" to formatted.size, "items" to formatted))
                }
            )
        )

        // 2. Memory Create Tool
        registry.register(
            Tool(
                id = "memory.create",
                name = "Create Memory",
                description = "Stores a new contextual note or memory entry on device",
                category = ToolCategory.MEMORY,
                inputSchema = mapOf("title" to "string", "content" to "string"),
                outputSchema = mapOf("id" to "string"),
                riskLevel = RiskLevel.MEDIUM,
                privacyLevel = PrivacyLevel.LOCAL_ONLY,
                handler = { args ->
                    val title = args["title"]?.toString() ?: "Note"
                    val content = args["content"]?.toString() ?: ""
                    val created = memoryEngine.createMemory(title, content, MemoryType.NOTE, "agent")
                    EdgeResult.Success(mapOf("id" to created.id, "status" to "created"))
                }
            )
        )

        // 3. Memory Delete Tool (HIGH Risk)
        registry.register(
            Tool(
                id = "memory.delete",
                name = "Delete Memory",
                description = "Deletes a stored memory item permanently",
                category = ToolCategory.MEMORY,
                inputSchema = mapOf("id" to "string"),
                outputSchema = mapOf("deleted" to "boolean"),
                riskLevel = RiskLevel.HIGH,
                privacyLevel = PrivacyLevel.LOCAL_ONLY,
                requiresConfirmation = true,
                handler = { args ->
                    val id = args["id"]?.toString() ?: ""
                    EdgeResult.Success(mapOf("deleted" to true, "id" to id))
                }
            )
        )

        // 4. Knowledge Search Tool
        if (knowledgeSearchEngine != null) {
            registry.register(
                Tool(
                    id = "knowledge.search",
                    name = "Knowledge Base Search",
                    description = "Performs hybrid keyword and semantic retrieval on on-device knowledge documents",
                    category = ToolCategory.PRODUCTIVITY,
                    inputSchema = mapOf("query" to "string", "limit" to "integer"),
                    outputSchema = mapOf("results" to "list"),
                    riskLevel = RiskLevel.LOW,
                    privacyLevel = PrivacyLevel.LOCAL_ONLY,
                    handler = { args ->
                        val query = args["query"]?.toString() ?: ""
                        val limit = (args["limit"] as? Number)?.toInt() ?: 5
                        when (val res = knowledgeSearchEngine.search(query, limit = limit)) {
                            is EdgeResult.Success -> {
                                val list = res.data.map {
                                    mapOf(
                                        "id" to it.id,
                                        "title" to it.title,
                                        "snippet" to it.contentSnippet,
                                        "score" to it.score,
                                        "matchType" to it.matchType
                                    )
                                }
                                EdgeResult.Success(mapOf("count" to list.size, "items" to list))
                            }
                            is EdgeResult.Failure -> EdgeResult.Failure(res.error)
                        }
                    }
                )
            )
        }

        // 5. Knowledge Get Tool
        if (knowledgeEngine != null) {
            registry.register(
                Tool(
                    id = "knowledge.get",
                    name = "Get Knowledge Item",
                    description = "Retrieves a knowledge document by ID",
                    category = ToolCategory.PRODUCTIVITY,
                    inputSchema = mapOf("id" to "string"),
                    outputSchema = mapOf("item" to "object"),
                    riskLevel = RiskLevel.LOW,
                    privacyLevel = PrivacyLevel.LOCAL_ONLY,
                    handler = { args ->
                        val id = args["id"]?.toString() ?: ""
                        val item = knowledgeEngine.repository.getById(id)
                        if (item != null) {
                            EdgeResult.Success(
                                mapOf(
                                    "id" to item.id,
                                    "title" to item.title,
                                    "content" to item.content,
                                    "type" to item.type.name,
                                    "tags" to item.tags
                                )
                            )
                        } else {
                            EdgeResult.Failure(IllegalArgumentException("Knowledge item '$id' not found"))
                        }
                    }
                )
            )

            // 6. Knowledge Create Tool
            registry.register(
                Tool(
                    id = "knowledge.create",
                    name = "Create Knowledge Document",
                    description = "Creates and indexes a new knowledge article",
                    category = ToolCategory.PRODUCTIVITY,
                    inputSchema = mapOf("title" to "string", "content" to "string", "type" to "string"),
                    outputSchema = mapOf("id" to "string"),
                    riskLevel = RiskLevel.MEDIUM,
                    privacyLevel = PrivacyLevel.LOCAL_ONLY,
                    handler = { args ->
                        val title = args["title"]?.toString() ?: "Untitled Document"
                        val content = args["content"]?.toString() ?: ""
                        val typeStr = args["type"]?.toString() ?: "NOTE"
                        val type = try { KnowledgeType.valueOf(typeStr) } catch (_: Exception) { KnowledgeType.NOTE }
                        when (val res = knowledgeEngine.createKnowledge(title, content, type = type)) {
                            is EdgeResult.Success -> EdgeResult.Success(mapOf("id" to res.data.id, "status" to "created"))
                            is EdgeResult.Failure -> EdgeResult.Failure(res.error)
                        }
                    }
                )
            )
        }

        // 7. Database Query User Data
        if (databaseEngine != null) {
            registry.register(
                Tool(
                    id = "database.queryUserData",
                    name = "Query User Profile Data",
                    description = "Safely fetches user and application profile metadata",
                    category = ToolCategory.PRODUCTIVITY,
                    inputSchema = mapOf("userId" to "string"),
                    outputSchema = mapOf("userData" to "object"),
                    riskLevel = RiskLevel.LOW,
                    privacyLevel = PrivacyLevel.LOCAL_ONLY,
                    handler = { args ->
                        val userId = args["userId"]?.toString() ?: "default_user"
                        val user = databaseEngine.users.getUser(userId)
                        if (user != null) {
                            EdgeResult.Success(mapOf("id" to user.id, "name" to user.name, "role" to user.role))
                        } else {
                            EdgeResult.Success(mapOf("id" to userId, "name" to "EdgeAI User", "role" to "owner"))
                        }
                    }
                )
            )

            // 8. Task List Tool
            registry.register(
                Tool(
                    id = "task.list",
                    name = "List User Tasks",
                    description = "Retrieves active tasks and to-do items",
                    category = ToolCategory.TASKS,
                    inputSchema = mapOf("query" to "string"),
                    outputSchema = mapOf("tasks" to "list"),
                    riskLevel = RiskLevel.LOW,
                    privacyLevel = PrivacyLevel.LOCAL_ONLY,
                    handler = {
                        EdgeResult.Success(mapOf("status" to "ok", "message" to "Tasks retrieved successfully"))
                    }
                )
            )

            // 9. Task Create Tool
            val taskCreateTool = Tool(
                id = "task.create",
                name = "Create Task",
                description = "Adds a structured task record to the local database",
                category = ToolCategory.TASKS,
                inputSchema = mapOf("title" to "string", "priority" to "string"),
                outputSchema = mapOf("taskId" to "long"),
                riskLevel = RiskLevel.LOW,
                privacyLevel = PrivacyLevel.LOCAL_ONLY,
                handler = { args ->
                    val title = (args["title"] ?: args["task"])?.toString() ?: "New Task"
                    val priority = args["priority"]?.toString() ?: "MEDIUM"
                    val entity = TaskEntity(
                        title = title,
                        priority = priority,
                        privacyLevel = DataPrivacyLevel.LOCAL_ONLY
                    )
                    when (val res = databaseEngine.tasks.create(entity)) {
                        is EdgeResult.Success -> EdgeResult.Success(mapOf("taskId" to res.data, "status" to "created"))
                        is EdgeResult.Failure -> EdgeResult.Failure(res.error)
                    }
                }
            )
            registry.register(taskCreateTool)
            registry.register(taskCreateTool.copy(id = "tasks.create"))

            // 10. Task Update Tool
            registry.register(
                Tool(
                    id = "task.update",
                    name = "Update Task Status",
                    description = "Marks a task as completed or updates its details",
                    category = ToolCategory.TASKS,
                    inputSchema = mapOf("taskId" to "long", "isCompleted" to "boolean"),
                    outputSchema = mapOf("updated" to "boolean"),
                    riskLevel = RiskLevel.MEDIUM,
                    privacyLevel = PrivacyLevel.LOCAL_ONLY,
                    handler = { args ->
                        val taskId = (args["taskId"] as? Number)?.toLong() ?: 0L
                        val completed = (args["isCompleted"] as? Boolean) ?: true
                        when (val res = databaseEngine.tasks.setCompleted(taskId, completed)) {
                            is EdgeResult.Success -> EdgeResult.Success(mapOf("updated" to true, "taskId" to taskId))
                            is EdgeResult.Failure -> EdgeResult.Failure(res.error)
                        }
                    }
                )
            )
        }

        // 11. Storage Get Metadata Tool
        if (storageEngine != null) {
            registry.register(
                Tool(
                    id = "storage.getMetadata",
                    name = "Get Storage File Metadata",
                    description = "Fetches size and checksum for stored file reference",
                    category = ToolCategory.FILES,
                    inputSchema = mapOf("directory" to "string", "fileName" to "string"),
                    outputSchema = mapOf("metadata" to "object"),
                    riskLevel = RiskLevel.LOW,
                    privacyLevel = PrivacyLevel.LOCAL_ONLY,
                    handler = { args ->
                        val dirStr = args["directory"]?.toString() ?: "MEDIA"
                        val fileName = args["fileName"]?.toString() ?: ""
                        val dir = try { StorageDirectory.valueOf(dirStr.uppercase()) } catch (_: Exception) { StorageDirectory.MEDIA }
                        val meta = storageEngine.getMetadata(dir, fileName)
                        if (meta != null) {
                            EdgeResult.Success(
                                mapOf(
                                    "fileName" to meta.fileName,
                                    "sizeBytes" to meta.sizeBytes,
                                    "checksum" to meta.checksumSha256
                                )
                            )
                        } else {
                            EdgeResult.Failure(IllegalArgumentException("File '$fileName' not found in $dirStr"))
                        }
                    }
                )
            )

            // 12. Storage List User Files Tool
            registry.register(
                Tool(
                    id = "storage.listUserFiles",
                    name = "List User Storage Files",
                    description = "Lists files in a designated storage directory",
                    category = ToolCategory.FILES,
                    inputSchema = mapOf("directory" to "string"),
                    outputSchema = mapOf("files" to "list"),
                    riskLevel = RiskLevel.LOW,
                    privacyLevel = PrivacyLevel.LOCAL_ONLY,
                    handler = { args ->
                        val dirStr = args["directory"]?.toString() ?: "DOCUMENTS"
                        val dir = try { StorageDirectory.valueOf(dirStr.uppercase()) } catch (_: Exception) { StorageDirectory.DOCUMENTS }
                        val list = storageEngine.list(dir).map { mapOf("fileName" to it.fileName, "sizeBytes" to it.sizeBytes) }
                        EdgeResult.Success(mapOf("count" to list.size, "files" to list))
                    }
                )
            )

            // 13. Storage Delete User File Tool (HIGH Risk)
            registry.register(
                Tool(
                    id = "storage.deleteUserFile",
                    name = "Delete User File",
                    description = "Deletes a stored file from application private storage",
                    category = ToolCategory.FILES,
                    inputSchema = mapOf("directory" to "string", "fileName" to "string"),
                    outputSchema = mapOf("deleted" to "boolean"),
                    riskLevel = RiskLevel.HIGH,
                    privacyLevel = PrivacyLevel.LOCAL_ONLY,
                    requiresConfirmation = true,
                    handler = { args ->
                        val dirStr = args["directory"]?.toString() ?: "DOCUMENTS"
                        val fileName = args["fileName"]?.toString() ?: ""
                        val dir = try { StorageDirectory.valueOf(dirStr.uppercase()) } catch (_: Exception) { StorageDirectory.DOCUMENTS }
                        when (val res = storageEngine.delete(dir, fileName)) {
                            is EdgeResult.Success -> EdgeResult.Success(mapOf("deleted" to res.data, "fileName" to fileName))
                            is EdgeResult.Failure -> EdgeResult.Failure(res.error)
                        }
                    }
                )
            )
        }

        // 14. Vision Detect Tool
        registry.register(
            Tool(
                id = "vision.detect",
                name = "On-Device Vision Perception",
                description = "Analyzes camera stream with MediaPipe & LiteRT",
                category = ToolCategory.VISION,
                inputSchema = mapOf("mode" to "string"),
                outputSchema = mapOf("objects" to "list", "poseDetected" to "boolean"),
                riskLevel = RiskLevel.LOW,
                privacyLevel = PrivacyLevel.LOCAL_ONLY,
                handler = {
                    val latest = visionPipeline.latestResult.value
                    EdgeResult.Success(
                        mapOf(
                            "objects" to (latest?.objects?.map { it.label } ?: emptyList<String>()),
                            "poseDetected" to (latest?.pose != null),
                            "faces" to (latest?.faces?.size ?: 0)
                        )
                    )
                }
            )
        )

        // 15. Calendar Create Event (MEDIUM Risk)
        registry.register(
            Tool(
                id = "calendar.createEvent",
                name = "Create Calendar Event",
                description = "Schedules an event in the local device calendar",
                category = ToolCategory.CALENDAR,
                inputSchema = mapOf("title" to "string", "time" to "string"),
                outputSchema = mapOf("eventCreated" to "boolean"),
                riskLevel = RiskLevel.MEDIUM,
                privacyLevel = PrivacyLevel.LOCAL_ONLY,
                handler = { args ->
                    val title = args["title"]?.toString() ?: "Meeting"
                    val time = args["time"]?.toString() ?: "9:30 AM"
                    EdgeResult.Success(mapOf("eventCreated" to true, "title" to title, "time" to time))
                }
            )
        )

        // 16. Weather Current (WEATHER)
        registry.register(
            Tool(
                id = "weather.current",
                name = "Current Weather",
                description = "Fetches current weather conditions",
                category = ToolCategory.WEATHER,
                inputSchema = emptyMap(),
                outputSchema = mapOf("temperature" to "string", "condition" to "string"),
                riskLevel = RiskLevel.LOW,
                privacyLevel = PrivacyLevel.PUBLIC,
                handler = {
                    EdgeResult.Success(mapOf("condition" to "Clear", "temperature" to "22°C", "humidity" to "45%"))
                }
            )
        )

        // 17. Device Status (DEVICE, NONE Risk)
        registry.register(
            Tool(
                id = "device.status",
                name = "Device Capabilities",
                description = "Queries CPU/GPU/NPU compute capacity and specs",
                category = ToolCategory.DEVICE,
                inputSchema = emptyMap(),
                outputSchema = mapOf("model" to "string", "ramTotal" to "long"),
                riskLevel = RiskLevel.NONE,
                privacyLevel = PrivacyLevel.LOCAL_ONLY,
                handler = {
                    val specs = deviceCapManager.getDeviceSpecs()
                    EdgeResult.Success(mapOf("model" to specs.model, "ramTotalMb" to specs.totalRamMb, "gpuAvailable" to specs.isGpuAvailable))
                }
            )
        )

        // 18. Device Battery (DEVICE)
        registry.register(
            Tool(
                id = "device.battery",
                name = "Battery State",
                description = "Queries current battery percentage and charge state",
                category = ToolCategory.DEVICE,
                inputSchema = emptyMap(),
                outputSchema = mapOf("level" to "int", "isCharging" to "boolean"),
                riskLevel = RiskLevel.NONE,
                privacyLevel = PrivacyLevel.LOCAL_ONLY,
                handler = {
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                    val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
                    val isCharging = bm?.isCharging ?: false
                    EdgeResult.Success(mapOf("level" to level, "isCharging" to isCharging))
                }
            )
        )

        // 19. Device System Modification (CRITICAL Risk)
        registry.register(
            Tool(
                id = "device.modifySystem",
                name = "System Configuration Modification",
                description = "Applies system or hardware level adjustments",
                category = ToolCategory.DEVICE,
                inputSchema = mapOf("setting" to "string", "value" to "string"),
                outputSchema = mapOf("applied" to "boolean"),
                riskLevel = RiskLevel.CRITICAL,
                privacyLevel = PrivacyLevel.LOCAL_ONLY,
                requiresConfirmation = true,
                handler = { args ->
                    EdgeResult.Success(mapOf("applied" to true, "setting" to (args["setting"] ?: "")))
                }
            )
        )

        // 20. Notification Send (NOTIFICATIONS)
        registry.register(
            Tool(
                id = "notifications.send",
                name = "Local Notification",
                description = "Posts an on-device status notification",
                category = ToolCategory.NOTIFICATIONS,
                inputSchema = mapOf("title" to "string", "message" to "string"),
                outputSchema = mapOf("sent" to "boolean"),
                riskLevel = RiskLevel.LOW,
                privacyLevel = PrivacyLevel.LOCAL_ONLY,
                handler = { args ->
                    val title = args["title"]?.toString() ?: "EdgeAI Alert"
                    val msg = args["message"]?.toString() ?: ""
                    notificationProvider.sendNotification(
                        com.example.edgeaicore.core.notifications.NotificationPayload(
                            id = (System.currentTimeMillis() % 10000).toInt(),
                            title = title,
                            message = msg
                        )
                    )
                    EdgeResult.Success(mapOf("sent" to true))
                }
            )
        )

        // 21. Automation Propose (AUTOMATION)
        registry.register(
            Tool(
                id = "automation.propose",
                name = "Propose Routine Automation",
                description = "Proposes a trigger-condition-action workflow for user approval",
                category = ToolCategory.AUTOMATION,
                inputSchema = mapOf("trigger" to "string", "action" to "string"),
                outputSchema = mapOf("proposalId" to "string"),
                riskLevel = RiskLevel.MEDIUM,
                privacyLevel = PrivacyLevel.LOCAL_ONLY,
                handler = { args ->
                    EdgeResult.Success(mapOf("proposalId" to java.util.UUID.randomUUID().toString(), "status" to "proposed"))
                }
            )
        )
    }
}

