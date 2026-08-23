package com.example.edgeaicore

import android.content.Context
import android.graphics.Bitmap
import com.example.edgeaicore.core.agent.ActionProposal
import com.example.edgeaicore.core.agent.AgentEngine
import com.example.edgeaicore.core.agent.AgentExecutionResult
import com.example.edgeaicore.core.agent.AgentProfile
import com.example.edgeaicore.core.agent.AgentProfileRegistry
import com.example.edgeaicore.core.agent.AgentRuntime
import com.example.edgeaicore.core.agent.CapabilityRegistry
import com.example.edgeaicore.core.ai.*
import com.example.edgeaicore.core.analytics.LocalAnalyticsProvider
import com.example.edgeaicore.core.automation.AutomationEngine
import com.example.edgeaicore.core.automation.AutomationProposal
import com.example.edgeaicore.core.automation.AutomationRule
import com.example.edgeaicore.core.billing.BillingManager
import com.example.edgeaicore.core.cache.AIResponseCache
import com.example.edgeaicore.core.cache.CacheEngine
import com.example.edgeaicore.core.camera.CameraEngine
import com.example.edgeaicore.core.cloud.CloudFallbackProvider
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.context.ContextEngine
import com.example.edgeaicore.core.database.DataGateway
import com.example.edgeaicore.core.database.DataPermissionManager
import com.example.edgeaicore.core.database.DatabaseEngine
import com.example.edgeaicore.core.database.EdgeDatabase
import com.example.edgeaicore.core.demo.DemoEngine
import com.example.edgeaicore.core.diagnostics.DeviceCapabilityManager
import com.example.edgeaicore.core.diagnostics.DiagnosticsMetrics
import com.example.edgeaicore.core.diagnostics.PerformanceMonitor
import com.example.edgeaicore.core.embeddings.EmbeddingEngine
import com.example.edgeaicore.core.embeddings.LocalEmbeddingProvider
import com.example.edgeaicore.core.explanation.ExplanationEngine
import com.example.edgeaicore.core.gateway.ToolAuditRecord
import com.example.edgeaicore.core.gateway.ToolExecutionResult
import com.example.edgeaicore.core.gateway.ToolGateway
import com.example.edgeaicore.core.knowledge.ChunkingEngine
import com.example.edgeaicore.core.knowledge.KnowledgeEngine
import com.example.edgeaicore.core.knowledge.KnowledgeIngestionPipeline
import com.example.edgeaicore.core.knowledge.KnowledgeSearchEngine
import com.example.edgeaicore.core.litert.LiteRTEngine
import com.example.edgeaicore.core.litertlm.GenerationResponse
import com.example.edgeaicore.core.litertlm.LiteRTLMEngine
import com.example.edgeaicore.core.mcp.InternalMcpServer
import com.example.edgeaicore.core.mcp.McpClient
import com.example.edgeaicore.core.mcp.McpTrustLevel
import com.example.edgeaicore.core.mediapipe.MediaPipeEngine
import com.example.edgeaicore.core.mediapipe.VisionResult
import com.example.edgeaicore.core.memory.MemoryEngine
import com.example.edgeaicore.core.memory.RankedMemory
import com.example.edgeaicore.core.models.EdgeModel
import com.example.edgeaicore.core.models.LocalModelManager
import com.example.edgeaicore.core.notifications.LocalNotificationProvider
import com.example.edgeaicore.core.policy.ConfirmationManager
import com.example.edgeaicore.core.policy.PolicyEngine
import com.example.edgeaicore.core.policy.ToolActionProposal
import com.example.edgeaicore.core.preferences.PreferenceEngine
import com.example.edgeaicore.core.privacy.PrivacyEngine
import com.example.edgeaicore.core.server.PrivateAIClient
import com.example.edgeaicore.core.server.PrivateServerConfigManager
import com.example.edgeaicore.core.storage.AudioStorage
import com.example.edgeaicore.core.storage.DocumentStorage
import com.example.edgeaicore.core.storage.ImageStoragePipeline
import com.example.edgeaicore.core.storage.MediaRepository
import com.example.edgeaicore.core.storage.StorageDirectory
import com.example.edgeaicore.core.storage.StorageEngine
import com.example.edgeaicore.core.storage.StorageIntegrityCheck
import com.example.edgeaicore.core.storage.StorageManager
import com.example.edgeaicore.core.sync.BackupEngine
import com.example.edgeaicore.core.sync.DataExportEngine
import com.example.edgeaicore.core.sync.DataImportEngine
import com.example.edgeaicore.core.sync.SyncEngine
import com.example.edgeaicore.core.tools.DefaultTools
import com.example.edgeaicore.core.tools.Tool
import com.example.edgeaicore.core.tools.ToolRegistry
import com.example.edgeaicore.core.vision.VisionPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Unified EdgeAI Core & Agent Engine Platform Facade.
 *
 * Provides a modular, offline-first, MCP-compatible, and tool-governed execution environment
 * powering on-device intelligence applications with human-in-the-loop safety.
 */
class EdgeAICore private constructor(val context: Context) {

    // Internal Core Engines
    internal val deviceCapManager = DeviceCapabilityManager(context)
    internal val perfMonitor = PerformanceMonitor(context)
    internal val modelManager = LocalModelManager(context)
    internal val liteRTEngine = LiteRTEngine(context)
    internal val liteRTLMEngine = LiteRTLMEngine(context)
    internal val mediaPipeEngine = MediaPipeEngine(context)
    internal val visionPipeline = VisionPipeline(context, mediaPipeEngine, liteRTEngine)
    internal val embeddingEngine = EmbeddingEngine(context, LocalEmbeddingProvider())
    internal val memoryEngine = MemoryEngine(context, embeddingEngine)
    internal val contextEngine = ContextEngine(context, memoryEngine)
    internal val agentEngine = AgentEngine(context, contextEngine, memoryEngine)
    internal val privacyEngine = PrivacyEngine(context)
    internal val privateAIClient = PrivateAIClient(context)
    internal val privateServerConfigManager = PrivateServerConfigManager(context)
    internal val cloudFallbackProvider = CloudFallbackProvider(context)
    internal val aiCache = AIResponseCache()
    internal val cacheEngine = CacheEngine(aiCache)

    // Database & Storage Subsystems
    internal val edgeDatabase = EdgeDatabase.getInstance(context)
    internal val dataPermissionManager = DataPermissionManager(context)
    internal val databaseEngine = DatabaseEngine(context, edgeDatabase)
    internal val storageEngine = StorageEngine(context)
    internal val mediaRepository = MediaRepository(edgeDatabase, storageEngine)
    internal val imageStoragePipeline = ImageStoragePipeline(storageEngine, mediaRepository)
    internal val documentStorage = DocumentStorage(storageEngine, databaseEngine.documents)
    internal val audioStorage = AudioStorage(storageEngine, mediaRepository)
    internal val storageManager = StorageManager(context, storageEngine)
    internal val storageIntegrityCheck = StorageIntegrityCheck(edgeDatabase, storageEngine)
    internal val dataGateway = DataGateway(context, databaseEngine, storageEngine, dataPermissionManager)

    // Knowledge Subsystem
    internal val chunkingEngine = ChunkingEngine()
    internal val knowledgeEngine = KnowledgeEngine(edgeDatabase, chunking = chunkingEngine)
    internal val knowledgeIngestionPipeline = KnowledgeIngestionPipeline(edgeDatabase, knowledgeEngine, embeddingEngine)
    internal val knowledgeSearchEngine = KnowledgeSearchEngine(edgeDatabase, knowledgeEngine, embeddingEngine)

    // Sync & Backup Subsystems
    internal val syncEngine = SyncEngine(edgeDatabase)
    internal val dataExportEngine = DataExportEngine(edgeDatabase, storageEngine)
    internal val dataImportEngine = DataImportEngine(edgeDatabase, storageEngine)
    internal val backupEngine = BackupEngine(context, edgeDatabase, storageEngine)

    // MCP & Governance Engines
    internal val mcpClient = McpClient()
    internal val internalMcpServer = InternalMcpServer(
        context = context,
        memoryEngine = memoryEngine,
        contextEngine = contextEngine,
        deviceCapManager = deviceCapManager,
        modelManager = modelManager,
        visionPipeline = visionPipeline,
        databaseEngine = databaseEngine,
        storageEngine = storageEngine,
        knowledgeEngine = knowledgeEngine,
        knowledgeSearchEngine = knowledgeSearchEngine
    )

    internal val toolRegistry = ToolRegistry()
    internal val policyEngine = PolicyEngine(context, privacyEngine)
    internal val confirmationManager = ConfirmationManager()
    internal val toolGateway = ToolGateway(context, toolRegistry, policyEngine, confirmationManager)

    internal val capabilityRegistry = CapabilityRegistry(toolRegistry, mcpClient, policyEngine)
    internal val profileRegistry = AgentProfileRegistry()

    internal val agentRuntime = AgentRuntime(
        context = context,
        contextEngine = contextEngine,
        memoryEngine = memoryEngine,
        capabilityRegistry = capabilityRegistry,
        toolGateway = toolGateway,
        liteRTLMEngine = liteRTLMEngine,
        confirmationManager = confirmationManager,
        agentLogRepository = databaseEngine.agentLogs
    )

    internal val automationEngine = AutomationEngine(
        context = context,
        toolGateway = toolGateway,
        policyEngine = policyEngine,
        confirmationManager = confirmationManager
    )

    internal val localAIProvider = LocalAIProvider(liteRTLMEngine)
    internal val privateServerAIProvider = PrivateServerAIProvider(privateAIClient)
    internal val cloudAIProvider = CloudFallbackAIProvider(cloudFallbackProvider)

    internal val aiRouter = AIRouter(
        context = context,
        localProvider = localAIProvider,
        privateServerProvider = privateServerAIProvider,
        cloudProvider = cloudAIProvider,
        privacyEngine = privacyEngine,
        cache = aiCache,
        performanceMonitor = perfMonitor
    )

    // Subsystems
    val diagnostics = DiagnosticsSubsystem()
    val models = ModelsSubsystem()
    val vision = VisionSubsystem()
    val camera = CameraEngine(context)
    val memory = MemorySubsystem()
    val ai = AISubsystem()
    val agent = AgentSubsystem()
    val mcp = McpSubsystem()
    val tools = ToolsSubsystem()
    val gateway = GatewaySubsystem()
    val policy = PolicySubsystem()
    val automation = AutomationSubsystem()
    val privateServer = PrivateServerSubsystem()
    val privacy = PrivacySubsystem()
    val database = DatabaseSubsystem()
    val storage = StorageSubsystem()
    val knowledge = KnowledgeSubsystem()
    val sync = SyncSubsystem()
    val export = ExportSubsystem()
    val backup = BackupSubsystem()
    val cache = CacheSubsystem()
    val billing = BillingManager(context)
    val preferences = PreferenceEngine(context)
    val notifications = LocalNotificationProvider(context)
    val analytics = LocalAnalyticsProvider()
    val explanation = ExplanationEngine()
    val demo = DemoEngine()

    init {
        // 1. Register Default Native Tools
        DefaultTools.registerDefaults(
            context = context,
            registry = toolRegistry,
            memoryEngine = memoryEngine,
            contextEngine = contextEngine,
            deviceCapManager = deviceCapManager,
            visionPipeline = visionPipeline,
            notificationProvider = notifications,
            databaseEngine = databaseEngine,
            storageEngine = storageEngine,
            knowledgeEngine = knowledgeEngine,
            knowledgeSearchEngine = knowledgeSearchEngine
        )

        // 2. Connect Internal MCP Loopback Session asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                mcpClient.registerAndConnectSession(
                    serverId = "local",
                    transport = internalMcpServer.createLoopbackTransport(),
                    trustLevel = McpTrustLevel.TRUSTED_LOCAL
                )
            } catch (_: Exception) {}
        }
    }

    // Inner Subsystem Facades

    inner class DatabaseSubsystem {
        val engine: DatabaseEngine get() = databaseEngine
        val dataGateway: DataGateway get() = this@EdgeAICore.dataGateway
        val permissionManager: DataPermissionManager get() = dataPermissionManager
        val users get() = databaseEngine.users
        val tasks get() = databaseEngine.tasks
        val appDocuments get() = databaseEngine.documents
        val events get() = databaseEngine.events
        val agentLogs get() = databaseEngine.agentLogs
        suspend fun getStats() = databaseEngine.getDatabaseStats()
        suspend fun optimize() = databaseEngine.optimizeDatabase()

        suspend fun <R> transaction(block: suspend (EdgeDatabase) -> R): EdgeResult<R> =
            databaseEngine.runTransaction(block)
    }

    inner class StorageSubsystem {
        val engine: StorageEngine get() = storageEngine
        val media: MediaRepository get() = mediaRepository
        val images: ImageStoragePipeline get() = imageStoragePipeline
        val documents: DocumentStorage get() = documentStorage
        val audio: AudioStorage get() = audioStorage
        val manager: StorageManager get() = storageManager
        val integrity: StorageIntegrityCheck get() = storageIntegrityCheck
        suspend fun getBreakdown() = storageManager.getStorageBreakdown()
        suspend fun getCleanupSuggestions() = storageManager.getCleanupSuggestions()
        suspend fun clearDirectory(directory: StorageDirectory) = storageManager.clearDirectory(directory)
        suspend fun runIntegrityCheck() = storageIntegrityCheck.runCheck()

        suspend fun save(
            directory: StorageDirectory,
            fileName: String,
            inputStream: java.io.InputStream
        ) = storageEngine.save(directory, fileName, inputStream)

        suspend fun save(
            directory: StorageDirectory,
            fileName: String,
            bytes: ByteArray
        ) = storageEngine.saveBytes(directory, fileName, bytes)

        suspend fun save(
            directory: StorageDirectory,
            fileName: String,
            content: String
        ) = storageEngine.saveString(directory, fileName, content)

        suspend fun read(
            directory: StorageDirectory,
            fileName: String
        ): EdgeResult<String> = storageEngine.readString(directory, fileName)

        suspend fun readStream(
            directory: StorageDirectory,
            fileName: String
        ) = storageEngine.readStream(directory, fileName)

        suspend fun delete(
            directory: StorageDirectory,
            fileName: String
        ) = storageEngine.delete(directory, fileName)

        suspend fun exists(
            directory: StorageDirectory,
            fileName: String
        ) = storageEngine.exists(directory, fileName)
    }

    inner class KnowledgeSubsystem {
        val engine: KnowledgeEngine get() = knowledgeEngine
        val repository get() = knowledgeEngine.repository
        val ingestion: KnowledgeIngestionPipeline get() = knowledgeIngestionPipeline
        val search: KnowledgeSearchEngine get() = knowledgeSearchEngine
        val chunking: ChunkingEngine get() = chunkingEngine

        suspend fun search(query: String, limit: Int = 10) = knowledgeSearchEngine.search(query, limit)
    }

    inner class SyncSubsystem {
        val engine: SyncEngine get() = syncEngine
        val export: DataExportEngine get() = dataExportEngine
        val import: DataImportEngine get() = dataImportEngine
        val backup: BackupEngine get() = backupEngine

        suspend fun start(isOnline: Boolean = true, privateServerConfigured: Boolean = false): EdgeResult<Int> =
            syncEngine.processSyncQueue(isOnline, privateServerConfigured)
    }

    inner class ExportSubsystem {
        val engine: DataExportEngine get() = dataExportEngine
        suspend fun create(): EdgeResult<String> = dataExportEngine.exportUserDataJson()
        suspend fun list() = dataExportEngine.listExports()
    }

    inner class BackupSubsystem {
        val engine: BackupEngine get() = backupEngine
        val status get() = backupEngine.status
        suspend fun create(
            destination: com.example.edgeaicore.core.sync.BackupDestination = com.example.edgeaicore.core.sync.BackupDestination.LOCAL_DEVICE,
            userConsentGiven: Boolean = true
        ): EdgeResult<String> = backupEngine.createBackup(destination, userConsentGiven)
        suspend fun restore(backupFileName: String, userConsentGiven: Boolean = true): EdgeResult<String> =
            backupEngine.restoreBackup(backupFileName, userConsentGiven)
        suspend fun list() = backupEngine.listBackups()
    }

    inner class CacheSubsystem {
        val engine: CacheEngine get() = cacheEngine
        fun getStats() = cacheEngine.getStats()
        fun clearAll() = cacheEngine.clearAll()
    }

    inner class DiagnosticsSubsystem {
        fun metrics(): DiagnosticsMetrics = perfMonitor.metrics.value
        fun specs() = deviceCapManager.getDeviceSpecs()
        fun flow() = perfMonitor.metrics
    }

    inner class ModelsSubsystem {
        val list = modelManager.models
        suspend fun install(modelId: String, onProgress: (Float) -> Unit = {}): EdgeResult<EdgeModel> =
            modelManager.installModel(modelId, onProgress)
        fun remove(modelId: String) = modelManager.removeModel(modelId)
        fun setEnabled(modelId: String, enabled: Boolean) = modelManager.setModelEnabled(modelId, enabled)
    }

    inner class VisionSubsystem {
        val latestResult = visionPipeline.latestResult
        suspend fun detect(frame: Bitmap, mode: String = "SCENE"): VisionResult {
            val result = visionPipeline.processFrame(frame, mode)
            contextEngine.updateVisionContext(result)
            perfMonitor.updateCameraFps(if (result.processingTimeMs > 0) 1000.0 / result.processingTimeMs else 0.0)
            return result
        }
        suspend fun start() = visionPipeline.startPipeline()
        suspend fun stop() = visionPipeline.stopPipeline()
    }

    inner class MemorySubsystem {
        val activeMemories = memoryEngine.getAllActiveMemories()
        val count = memoryEngine.getMemoryCount()
        suspend fun create(
            title: String,
            content: String,
            type: com.example.edgeaicore.core.memory.MemoryType = com.example.edgeaicore.core.memory.MemoryType.NOTE,
            tags: String = "",
            privacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY,
            location: String? = null
        ) = memoryEngine.createMemory(title, content, type = type, tags = tags, privacyLevel = privacyLevel, location = location)
        suspend fun update(memory: com.example.edgeaicore.core.memory.MemoryEntity) = memoryEngine.updateMemory(memory)
        suspend fun updateMemory(memory: com.example.edgeaicore.core.memory.MemoryEntity) = memoryEngine.updateMemory(memory)
        suspend fun delete(memory: com.example.edgeaicore.core.memory.MemoryEntity) = memoryEngine.deleteMemory(memory)
        suspend fun deleteMemory(memory: com.example.edgeaicore.core.memory.MemoryEntity) = memoryEngine.deleteMemory(memory)
        suspend fun toggleFavorite(memory: com.example.edgeaicore.core.memory.MemoryEntity) = memoryEngine.toggleFavorite(memory)
        suspend fun archive(memory: com.example.edgeaicore.core.memory.MemoryEntity) = memoryEngine.archiveMemory(memory)
        suspend fun search(query: String): List<RankedMemory> =
            memoryEngine.retriever.retrieveMemories(query)
        suspend fun buildContext(query: String) =
            memoryEngine.contextBuilder.buildMemoryContext(query)
        suspend fun clear() = memoryEngine.clearAllMemories()
    }

    inner class AISubsystem {
        suspend fun generate(request: AIRequest): EdgeResult<GenerationResponse> =
            aiRouter.generate(request)
        suspend fun generate(prompt: String): EdgeResult<GenerationResponse> =
            aiRouter.generate(AIRequest(prompt = prompt))
        fun stream(request: AIRequest) = aiRouter.stream(request)
        fun clearCache() = aiCache.clear()
    }

    inner class AgentSubsystem {
        // Backwards compatibility with previous AgentEngine
        val proposals = agentEngine.recentProposals
        fun propose(intent: String): ActionProposal =
            agentEngine.proposeActionFromIntent(intent)
        suspend fun execute(proposal: ActionProposal) =
            agentEngine.executeConfirmedAction(proposal)

        // Agent Engine v2.4 Orchestrator
        val lastResult: StateFlow<AgentExecutionResult?> = agentRuntime.lastResult
        val isExecuting: StateFlow<Boolean> = agentRuntime.isExecuting
        val currentStateStep = agentRuntime.currentStateStep
        val confirmationManager: ConfirmationManager get() = this@EdgeAICore.confirmationManager

        suspend fun run(
            request: String,
            profile: AgentProfile = AgentProfile.ASSISTANT,
            userConsentGiven: Boolean = false
        ): EdgeResult<AgentExecutionResult> = agentRuntime.run(request, profile, userConsentGiven)

        fun getProfiles(): List<AgentProfile> = profileRegistry.getAll()
        fun getProfile(id: String): AgentProfile? = profileRegistry.get(id)
        fun registerProfile(profile: AgentProfile) = profileRegistry.register(profile)
    }

    inner class McpSubsystem {
        val client: McpClient get() = mcpClient
        val connectedServers = mcpClient.connectedServers
        val discoveredTools = mcpClient.toolRegistry.tools

        suspend fun discoverTools(): List<com.example.edgeaicore.core.mcp.McpTool> =
            mcpClient.toolRegistry.getAllTools()

        suspend fun invokeTool(
            toolName: String,
            arguments: Map<String, Any?>,
            declaredPrivacy: PrivacyLevel = PrivacyLevel.LOCAL_ONLY
        ): EdgeResult<com.example.edgeaicore.core.mcp.McpToolCallResult> {
            val tool = mcpClient.toolRegistry.getAllTools().find { it.name == toolName }
                ?: return EdgeResult.Failure(com.example.edgeaicore.core.common.EdgeAIError.McpProtocolError("client", "Tool '$toolName' not found"))
            return mcpClient.callTool(tool.serverId, toolName, arguments, declaredPrivacy)
        }

        suspend fun connectRemoteServer(serverId: String, endpoint: String, token: String? = null) {
            val transport = com.example.edgeaicore.core.mcp.HttpSseMcpTransport(endpoint, token)
            mcpClient.registerAndConnectSession(serverId, transport, McpTrustLevel.USER_APPROVED_REMOTE)
        }

        suspend fun disconnectServer(serverId: String) {
            mcpClient.disconnectSession(serverId)
        }
    }

    inner class ToolsSubsystem {
        val registry: ToolRegistry get() = toolRegistry
        fun register(tool: Tool) = toolRegistry.register(tool)
        fun unregister(toolId: String) = toolRegistry.unregister(toolId)
        fun getAll(): List<Tool> = toolRegistry.getAll()
        fun setEnabled(toolId: String, enabled: Boolean) = toolRegistry.setEnabled(toolId, enabled)

        suspend fun execute(
            toolId: String,
            arguments: Map<String, Any?>,
            userConsentGiven: Boolean = false,
            preConfirmedProposalId: String? = null
        ): EdgeResult<ToolExecutionResult> =
            toolGateway.executeTool(toolId, arguments, userConsentGiven, preConfirmedProposalId)
    }

    inner class GatewaySubsystem {
        val auditLogs: StateFlow<List<ToolAuditRecord>> = toolGateway.auditLogs
        suspend fun execute(
            toolId: String,
            arguments: Map<String, Any?>,
            userConsentGiven: Boolean = false,
            preConfirmedProposalId: String? = null
        ): EdgeResult<ToolExecutionResult> =
            toolGateway.executeTool(toolId, arguments, userConsentGiven, preConfirmedProposalId)

        fun clearAudit() = toolGateway.clearAuditLogs()
    }

    inner class PolicySubsystem {
        val engine: PolicyEngine get() = policyEngine
        val confirmationManager: ConfirmationManager get() = this@EdgeAICore.confirmationManager
        fun evaluate(tool: Tool, userConsentGiven: Boolean = false) =
            policyEngine.evaluateToolPolicy(tool, userConsentGiven)
        fun check(tool: Tool, userConsentGiven: Boolean = false) =
            policyEngine.evaluateToolPolicy(tool, userConsentGiven)
    }

    inner class AutomationSubsystem {
        val rules: StateFlow<List<AutomationRule>> = automationEngine.rules
        val proposals: StateFlow<List<AutomationProposal>> = automationEngine.proposals
        fun registerRule(rule: AutomationRule) = automationEngine.registerRule(rule)
        fun toggleRule(ruleId: String, enabled: Boolean) = automationEngine.toggleRule(ruleId, enabled)
        fun confirmProposal(proposalId: String) = automationEngine.confirmProposal(proposalId)
        fun rejectProposal(proposalId: String) = automationEngine.rejectProposal(proposalId)
    }

    inner class PrivateServerSubsystem {
        val client: PrivateAIClient get() = privateAIClient
        val config = privateAIClient.config
        fun configure(baseUrl: String, token: String?, enabled: Boolean = true) =
            privateAIClient.configureServer(baseUrl, token, enabled)
        suspend fun checkHealth() = privateAIClient.checkHealth()
    }

    inner class PrivacySubsystem {
        val state = privacyEngine.dashboardState
        val auditLogs = privacyEngine.auditLogs
        fun check(request: AIRequest): Boolean {
            val target = aiRouter.determineTargetProvider(request)
            return privacyEngine.validateRouting(request.privacyLevel, target, request.userConsent)
        }
        suspend fun setCloudAllowed(allowed: Boolean) = privacyEngine.setCloudAiAllowed(allowed)
        suspend fun setPrivateServerAllowed(allowed: Boolean) = privacyEngine.setPrivateServerAllowed(allowed)
        suspend fun setDataSharingAllowed(allowed: Boolean) = privacyEngine.setDataSharingAllowed(allowed)
        suspend fun setRemoteSyncAllowed(allowed: Boolean) = privacyEngine.setRemoteSyncAllowed(allowed)
        suspend fun setLocalVaultLocked(locked: Boolean) = privacyEngine.setLocalVaultLocked(locked)
        fun clearLogs() = privacyEngine.clearAuditLogs()
    }

    companion object {
        @Volatile
        private var INSTANCE: EdgeAICore? = null

        fun getInstance(context: Context): EdgeAICore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EdgeAICore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
