package com.example.edgeaicore.core.database

import androidx.room.withTransaction
import com.example.edgeaicore.core.common.EdgeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Generic repository interface supporting standard CRUD, pagination, filtering and search.
 */
interface DatabaseRepository<T, ID> {
    suspend fun create(item: T): EdgeResult<ID>
    suspend fun getById(id: ID): EdgeResult<T?>
    suspend fun update(item: T): EdgeResult<Unit>
    suspend fun delete(id: ID, softDelete: Boolean = true): EdgeResult<Unit>
    fun observeAll(): Flow<List<T>>
}

/**
 * Specialized Task Repository.
 */
class TaskRepository(private val database: EdgeDatabase) : DatabaseRepository<TaskEntity, Long> {
    private val dao = database.taskDao()

    override suspend fun create(item: TaskEntity): EdgeResult<Long> = withContext(Dispatchers.IO) {
        try {
            val id = dao.insertTask(item)
            EdgeResult.Success(id)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    override suspend fun getById(id: Long): EdgeResult<TaskEntity?> = withContext(Dispatchers.IO) {
        try {
            EdgeResult.Success(dao.getTaskById(id))
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    override suspend fun update(item: TaskEntity): EdgeResult<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.updateTask(item)
            EdgeResult.Success(Unit)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    override suspend fun delete(id: Long, softDelete: Boolean): EdgeResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (softDelete) dao.softDeleteTask(id) else dao.purgeTask(id)
            EdgeResult.Success(Unit)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    override fun observeAll(): Flow<List<TaskEntity>> = dao.getAllTasks()

    fun search(query: String): Flow<List<TaskEntity>> = dao.searchTasks(query)

    suspend fun setCompleted(id: Long, completed: Boolean): EdgeResult<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.setTaskCompleted(id, completed)
            EdgeResult.Success(Unit)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }
}

/**
 * Specialized Document Repository.
 */
class DocumentRepository(private val database: EdgeDatabase) : DatabaseRepository<DocumentEntity, String> {
    private val dao = database.documentDao()

    override suspend fun create(item: DocumentEntity): EdgeResult<String> = withContext(Dispatchers.IO) {
        try {
            dao.insertDocument(item)
            EdgeResult.Success(item.id)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    override suspend fun getById(id: String): EdgeResult<DocumentEntity?> = withContext(Dispatchers.IO) {
        try {
            EdgeResult.Success(dao.getDocumentById(id))
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    override suspend fun update(item: DocumentEntity): EdgeResult<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.updateDocument(item)
            EdgeResult.Success(Unit)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    override suspend fun delete(id: String, softDelete: Boolean): EdgeResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (softDelete) dao.softDeleteDocument(id) else dao.purgeDocument(id)
            EdgeResult.Success(Unit)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    override fun observeAll(): Flow<List<DocumentEntity>> = dao.getAllDocuments()
    fun search(query: String): Flow<List<DocumentEntity>> = dao.searchDocuments(query)
    fun getCount(): Flow<Int> = dao.getCount()
}

/**
 * Specialized Event Repository.
 */
class EventRepository(private val database: EdgeDatabase) : DatabaseRepository<EventEntity, Long> {
    private val dao = database.eventDao()

    override suspend fun create(item: EventEntity): EdgeResult<Long> = withContext(Dispatchers.IO) {
        try {
            val id = dao.insertEvent(item)
            EdgeResult.Success(id)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    override suspend fun getById(id: Long): EdgeResult<EventEntity?> = withContext(Dispatchers.IO) {
        try {
            EdgeResult.Success(dao.getEventById(id))
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    override suspend fun update(item: EventEntity): EdgeResult<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.updateEvent(item)
            EdgeResult.Success(Unit)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    override suspend fun delete(id: Long, softDelete: Boolean): EdgeResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (softDelete) dao.softDeleteEvent(id) else dao.purgeEvent(id)
            EdgeResult.Success(Unit)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    override fun observeAll(): Flow<List<EventEntity>> = dao.getAllEvents()
}

/**
 * Specialized Agent Session & Interaction Repository.
 */
class AgentRepository(private val database: EdgeDatabase) {
    private val sessionDao = database.agentSessionDao()
    private val interactionDao = database.aiInteractionDao()

    fun observeRecentSessions(limit: Int = 50): Flow<List<AgentSessionEntity>> = sessionDao.getRecentSessions(limit)
    suspend fun saveSession(session: AgentSessionEntity) = withContext(Dispatchers.IO) {
        sessionDao.insertSession(session)
    }

    suspend fun saveInteraction(interaction: AIInteractionEntity) = withContext(Dispatchers.IO) {
        interactionDao.insertInteraction(interaction)
    }
}

/**
 * Specialized Audit Repository.
 */
class AuditRepository(private val database: EdgeDatabase) {
    private val dao = database.auditDao()

    fun observeAudits(limit: Int = 100): Flow<List<AuditRecordEntity>> = dao.getRecentAudits(limit)

    suspend fun logAudit(record: AuditRecordEntity) = withContext(Dispatchers.IO) {
        try {
            dao.recordAudit(record)
        } catch (_: Exception) {}
    }

    suspend fun clearAudits() = withContext(Dispatchers.IO) {
        dao.clearAudits()
    }
}

/**
 * Specialized Usage Repository.
 */
class UsageRepository(private val database: EdgeDatabase) {
    private val dao = database.usageMetricDao()

    suspend fun recordMetric(metric: UsageMetricEntity) = withContext(Dispatchers.IO) {
        dao.recordMetric(metric)
    }

    suspend fun getTotalForCategory(category: String): Long = withContext(Dispatchers.IO) {
        dao.getTotalCountForCategory(category) ?: 0L
    }
}

/**
 * Specialized Model Repository.
 */
class ModelRepository(private val database: EdgeDatabase) {
    private val dao = database.modelMetadataDao()

    fun observeAllModels(): Flow<List<ModelMetadataEntity>> = dao.getAllModels()
    suspend fun getModel(modelId: String): ModelMetadataEntity? = withContext(Dispatchers.IO) {
        dao.getModelById(modelId)
    }
    suspend fun saveModel(model: ModelMetadataEntity) = withContext(Dispatchers.IO) {
        dao.insertModel(model)
    }
    suspend fun deleteModel(model: ModelMetadataEntity) = withContext(Dispatchers.IO) {
        dao.deleteModel(model)
    }
}

/**
 * Specialized MCP Repository.
 */
class McpRepository(private val database: EdgeDatabase) {
    private val serverDao = database.mcpServerDao()
    private val toolDao = database.mcpToolDao()

    fun observeServers(): Flow<List<McpServerEntity>> = serverDao.getAllServers()
    suspend fun saveServer(server: McpServerEntity) = withContext(Dispatchers.IO) {
        serverDao.insertServer(server)
    }
    suspend fun deleteServer(server: McpServerEntity) = withContext(Dispatchers.IO) {
        database.withTransaction {
            toolDao.deleteToolsForServer(server.serverId)
            serverDao.deleteServer(server)
        }
    }
    fun observeToolsForServer(serverId: String): Flow<List<McpToolEntity>> = toolDao.getToolsForServer(serverId)
}

/**
 * Specialized User & Profile Repository.
 */
class UserRepository(private val database: EdgeDatabase) {
    private val userDao = database.userDao()
    private val profileDao = database.profileDao()

    fun observeUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()
    suspend fun getUser(id: String): UserEntity? = withContext(Dispatchers.IO) { userDao.getUserById(id) }
    suspend fun saveUser(user: UserEntity) = withContext(Dispatchers.IO) { userDao.insertUser(user) }
    suspend fun saveProfile(profile: ProfileEntity) = withContext(Dispatchers.IO) { profileDao.insertProfile(profile) }
}

/**
 * Specialized Embedding Repository.
 */
class EmbeddingRepository(private val database: EdgeDatabase) {
    private val dao = database.embeddingDao()

    suspend fun saveEmbedding(embedding: EmbeddingEntity) = withContext(Dispatchers.IO) {
        dao.insertEmbedding(embedding)
    }

    suspend fun getEmbeddingsForSource(sourceId: String): List<EmbeddingEntity> = withContext(Dispatchers.IO) {
        dao.getEmbeddingsForSource(sourceId)
    }

    suspend fun getAllEmbeddings(): List<EmbeddingEntity> = withContext(Dispatchers.IO) {
        dao.getAllEmbeddingsSync()
    }

    fun getCount(): Flow<Int> = dao.getCount()
    suspend fun clear() = withContext(Dispatchers.IO) { dao.clearEmbeddings() }
}

/**
 * Specialized Agent Log Repository.
 */
class AgentLogRepository(private val database: EdgeDatabase) {
    private val dao = database.agentLogDao()

    fun observeAllLogs(): Flow<List<AgentLogEntity>> = dao.getAllLogs()

    fun observeRecentLogs(limit: Int = 100): Flow<List<AgentLogEntity>> = dao.getRecentLogs(limit)

    fun observeLogsForSession(sessionId: String): Flow<List<AgentLogEntity>> = dao.getLogsForSession(sessionId)

    fun observeLogsByLevel(level: String): Flow<List<AgentLogEntity>> = dao.getLogsByLevel(level)

    fun observeLogsByTag(tag: String): Flow<List<AgentLogEntity>> = dao.getLogsByTag(tag)

    suspend fun log(
        tag: String,
        message: String,
        level: String = "INFO",
        sessionId: String? = null,
        agentName: String = "SWAYAM Agent",
        metadataJson: String? = null,
        latencyMs: Long = 0L,
        tokenCount: Int = 0,
        privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY
    ): Long = withContext(Dispatchers.IO) {
        dao.insertLog(
            AgentLogEntity(
                sessionId = sessionId,
                agentName = agentName,
                level = level,
                tag = tag,
                message = message,
                metadataJson = metadataJson,
                latencyMs = latencyMs,
                tokenCount = tokenCount,
                privacyLevel = privacyLevel
            )
        )
    }

    suspend fun deleteLog(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteLogById(id)
    }

    suspend fun clearAllLogs() = withContext(Dispatchers.IO) {
        dao.clearAllLogs()
    }
}

