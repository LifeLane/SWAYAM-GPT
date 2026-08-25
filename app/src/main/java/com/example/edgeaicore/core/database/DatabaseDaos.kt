package com.example.edgeaicore.core.database

import androidx.room.*
import com.example.edgeaicore.core.common.RiskLevel
import kotlinx.coroutines.flow.Flow

class DatabaseTypeConverters {
    @TypeConverter
    fun fromLifecycle(state: DataLifecycleState): String = state.name

    @TypeConverter
    fun toLifecycle(value: String): DataLifecycleState = try {
        DataLifecycleState.valueOf(value)
    } catch (e: Exception) {
        DataLifecycleState.ACTIVE
    }

    @TypeConverter
    fun fromDataPrivacy(level: DataPrivacyLevel): String = level.name

    @TypeConverter
    fun toDataPrivacy(value: String): DataPrivacyLevel = try {
        DataPrivacyLevel.valueOf(value)
    } catch (e: Exception) {
        DataPrivacyLevel.LOCAL_ONLY
    }

    @TypeConverter
    fun fromRisk(level: RiskLevel): String = level.name

    @TypeConverter
    fun toRisk(value: String): RiskLevel = try {
        RiskLevel.valueOf(value)
    } catch (e: Exception) {
        RiskLevel.LOW
    }
}

// ---------------------------------------------------------------------------
// DAOs
// ---------------------------------------------------------------------------

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE lifecycleState != 'PURGED'")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET lifecycleState = 'DELETED', updatedAt = :time WHERE id = :id")
    suspend fun softDeleteUser(id: String, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun purgeUser(id: String)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    fun getProfilesForUser(userId: String): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)
}

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM user_preferences")
    fun getAllPreferences(): Flow<List<PreferenceEntity>>

    @Query("SELECT * FROM user_preferences WHERE key = :key LIMIT 1")
    suspend fun getPreference(key: String): PreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(pref: PreferenceEntity)

    @Query("DELETE FROM user_preferences WHERE key = :key")
    suspend fun deletePreference(key: String)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE lifecycleState != 'DELETED' AND lifecycleState != 'PURGED' ORDER BY isCompleted ASC, priority DESC, createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE lifecycleState != 'DELETED' AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :completed, updatedAt = :time WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, completed: Boolean, time: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET lifecycleState = 'DELETED', updatedAt = :time WHERE id = :id")
    suspend fun softDeleteTask(id: Long, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun purgeTask(id: Long)

    @Query("SELECT COUNT(*) FROM tasks WHERE lifecycleState != 'DELETED' AND lifecycleState != 'PURGED'")
    fun getCount(): Flow<Int>
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE lifecycleState != 'DELETED' AND lifecycleState != 'PURGED' ORDER BY startTime ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE startTime >= :start AND endTime <= :end AND lifecycleState != 'DELETED'")
    fun getEventsInRange(start: Long, end: Long): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Query("UPDATE events SET lifecycleState = 'DELETED' WHERE id = :id")
    suspend fun softDeleteEvent(id: Long)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun purgeEvent(id: Long)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentActivities(limit: Int = 50): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity): Long

    @Query("DELETE FROM activities WHERE timestamp < :cutoff")
    suspend fun pruneActivities(cutoff: Long)
}

@Dao
interface AIInteractionDao {
    @Query("SELECT * FROM ai_interactions WHERE lifecycleState != 'DELETED' ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentInteractions(limit: Int = 100): Flow<List<AIInteractionEntity>>

    @Query("SELECT * FROM ai_interactions WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getInteractionsForSession(sessionId: String): Flow<List<AIInteractionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: AIInteractionEntity): Long

    @Query("DELETE FROM ai_interactions WHERE timestamp < :cutoff")
    suspend fun pruneInteractions(cutoff: Long)
}

@Dao
interface AgentSessionDao {
    @Query("SELECT * FROM agent_sessions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 50): Flow<List<AgentSessionEntity>>

    @Query("SELECT * FROM agent_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): AgentSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AgentSessionEntity)

    @Update
    suspend fun updateSession(session: AgentSessionEntity)

    @Query("DELETE FROM agent_sessions")
    suspend fun clearSessions()
}

@Dao
interface ToolCallDao {
    @Query("SELECT * FROM tool_calls ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentToolCalls(limit: Int = 100): Flow<List<ToolCallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToolCall(call: ToolCallEntity): Long

    @Query("DELETE FROM tool_calls WHERE timestamp < :cutoff")
    suspend fun pruneToolCalls(cutoff: Long)
}

@Dao
interface McpServerDao {
    @Query("SELECT * FROM mcp_servers ORDER BY createdAt ASC")
    fun getAllServers(): Flow<List<McpServerEntity>>

    @Query("SELECT * FROM mcp_servers WHERE serverId = :serverId LIMIT 1")
    suspend fun getServerById(serverId: String): McpServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: McpServerEntity)

    @Delete
    suspend fun deleteServer(server: McpServerEntity)
}

@Dao
interface McpToolDao {
    @Query("SELECT * FROM mcp_tools WHERE serverId = :serverId")
    fun getToolsForServer(serverId: String): Flow<List<McpToolEntity>>

    @Query("SELECT * FROM mcp_tools WHERE isEnabled = 1")
    fun getAllEnabledTools(): Flow<List<McpToolEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTool(tool: McpToolEntity)

    @Query("DELETE FROM mcp_tools WHERE serverId = :serverId")
    suspend fun deleteToolsForServer(serverId: String)
}

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations ORDER BY createdAt DESC")
    fun getAllAutomations(): Flow<List<AutomationEntity>>

    @Query("SELECT * FROM automations WHERE id = :id LIMIT 1")
    suspend fun getAutomationById(id: String): AutomationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomation(automation: AutomationEntity)

    @Update
    suspend fun updateAutomation(automation: AutomationEntity)

    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun deleteAutomation(id: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentNotifications(limit: Int = 50): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}

@Dao
interface ModelMetadataDao {
    @Query("SELECT * FROM model_metadata ORDER BY name ASC")
    fun getAllModels(): Flow<List<ModelMetadataEntity>>

    @Query("SELECT * FROM model_metadata WHERE modelId = :modelId LIMIT 1")
    suspend fun getModelById(modelId: String): ModelMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: ModelMetadataEntity)

    @Update
    suspend fun updateModel(model: ModelMetadataEntity)

    @Delete
    suspend fun deleteModel(model: ModelMetadataEntity)
}

@Dao
interface UsageMetricDao {
    @Query("SELECT * FROM usage_metrics WHERE dateString = :date ORDER BY timestamp DESC")
    fun getUsageForDate(date: String): Flow<List<UsageMetricEntity>>

    @Query("SELECT SUM(countValue) FROM usage_metrics WHERE category = :category")
    suspend fun getTotalCountForCategory(category: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordMetric(metric: UsageMetricEntity): Long
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAudits(limit: Int = 100): Flow<List<AuditRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordAudit(record: AuditRecordEntity): Long

    @Query("DELETE FROM audit_records WHERE timestamp < :cutoff")
    suspend fun pruneAudits(cutoff: Long)

    @Query("DELETE FROM audit_records")
    suspend fun clearAudits()
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions WHERE isActive = 1 LIMIT 1")
    fun getActiveSubscription(): Flow<SubscriptionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSubscription(subscription: SubscriptionEntity)
}

@Dao
interface MediaMetadataDao {
    @Query("SELECT * FROM media_metadata WHERE lifecycleState != 'DELETED' AND lifecycleState != 'PURGED' ORDER BY createdAt DESC")
    fun getAllMedia(): Flow<List<MediaMetadataEntity>>

    @Query("SELECT * FROM media_metadata WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: String): MediaMetadataEntity?

    @Query("SELECT * FROM media_metadata WHERE mediaType = :type AND lifecycleState != 'DELETED' ORDER BY createdAt DESC")
    fun getMediaByType(type: String): Flow<List<MediaMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaMetadataEntity)

    @Update
    suspend fun updateMedia(media: MediaMetadataEntity)

    @Query("UPDATE media_metadata SET lifecycleState = 'DELETED' WHERE id = :id")
    suspend fun softDeleteMedia(id: String)

    @Query("DELETE FROM media_metadata WHERE id = :id")
    suspend fun purgeMedia(id: String)

    @Query("SELECT COUNT(*) FROM media_metadata WHERE lifecycleState != 'DELETED'")
    fun getCount(): Flow<Int>

    @Query("SELECT SUM(sizeBytes) FROM media_metadata WHERE lifecycleState != 'DELETED'")
    suspend fun getTotalStorageBytes(): Long?
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE lifecycleState != 'DELETED' AND lifecycleState != 'PURGED' ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE title LIKE '%' || :query || '%' OR extractedTextSummary LIKE '%' || :query || '%'")
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: DocumentEntity)

    @Update
    suspend fun updateDocument(doc: DocumentEntity)

    @Query("UPDATE documents SET lifecycleState = 'DELETED' WHERE id = :id")
    suspend fun softDeleteDocument(id: String)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun purgeDocument(id: String)

    @Query("SELECT COUNT(*) FROM documents WHERE lifecycleState != 'DELETED'")
    fun getCount(): Flow<Int>
}

@Dao
interface EmbeddingDao {
    @Query("SELECT * FROM embeddings WHERE sourceId = :sourceId")
    suspend fun getEmbeddingsForSource(sourceId: String): List<EmbeddingEntity>

    @Query("SELECT * FROM embeddings WHERE embeddingId = :embeddingId LIMIT 1")
    suspend fun getEmbeddingById(embeddingId: String): EmbeddingEntity?

    @Query("SELECT * FROM embeddings")
    suspend fun getAllEmbeddingsSync(): List<EmbeddingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: EmbeddingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(embeddings: List<EmbeddingEntity>)

    @Query("DELETE FROM embeddings WHERE sourceId = :sourceId")
    suspend fun deleteEmbeddingsForSource(sourceId: String)

    @Query("DELETE FROM embeddings")
    suspend fun clearEmbeddings()

    @Query("SELECT COUNT(*) FROM embeddings")
    fun getCount(): Flow<Int>
}

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_items WHERE lifecycleState != 'DELETED' AND lifecycleState != 'PURGED' ORDER BY updatedAt DESC")
    fun getAllKnowledgeItems(): Flow<List<KnowledgeItemEntity>>

    @Query("SELECT * FROM knowledge_items WHERE id = :id LIMIT 1")
    suspend fun getKnowledgeById(id: String): KnowledgeItemEntity?

    @Query("SELECT * FROM knowledge_items WHERE lifecycleState != 'DELETED' AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')")
    fun searchKnowledge(query: String): Flow<List<KnowledgeItemEntity>>

    @Query("SELECT * FROM knowledge_items WHERE lifecycleState != 'DELETED'")
    suspend fun getAllKnowledgeSync(): List<KnowledgeItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledge(item: KnowledgeItemEntity)

    @Update
    suspend fun updateKnowledge(item: KnowledgeItemEntity)

    @Query("UPDATE knowledge_items SET lifecycleState = 'DELETED', updatedAt = :time WHERE id = :id")
    suspend fun softDeleteKnowledge(id: String, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM knowledge_items WHERE id = :id")
    suspend fun purgeKnowledge(id: String)

    @Query("SELECT COUNT(*) FROM knowledge_items WHERE lifecycleState != 'DELETED'")
    fun getCount(): Flow<Int>
}

@Dao
interface KnowledgeChunkDao {
    @Query("SELECT * FROM knowledge_chunks WHERE documentId = :documentId ORDER BY chunkIndex ASC")
    suspend fun getChunksForDocument(documentId: String): List<KnowledgeChunkEntity>

    @Query("SELECT * FROM knowledge_chunks")
    suspend fun getAllChunksSync(): List<KnowledgeChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<KnowledgeChunkEntity>)

    @Query("DELETE FROM knowledge_chunks WHERE documentId = :documentId")
    suspend fun deleteChunksForDocument(documentId: String)

    @Query("DELETE FROM knowledge_chunks")
    suspend fun clearChunks()
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingSyncItems(limit: Int = 50): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Update
    suspend fun updateSyncItem(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun removeSyncItem(id: Long)

    @Query("DELETE FROM sync_queue WHERE status = 'COMPLETED'")
    suspend fun purgeCompleted()

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>
}

@Dao
interface AgentLogDao {
    @Query("SELECT * FROM agent_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AgentLogEntity>>

    @Query("SELECT * FROM agent_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getLogsForSession(sessionId: String): Flow<List<AgentLogEntity>>

    @Query("SELECT * FROM agent_logs WHERE level = :level ORDER BY timestamp DESC")
    fun getLogsByLevel(level: String): Flow<List<AgentLogEntity>>

    @Query("SELECT * FROM agent_logs WHERE tag = :tag ORDER BY timestamp DESC")
    fun getLogsByTag(tag: String): Flow<List<AgentLogEntity>>

    @Query("SELECT * FROM agent_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<AgentLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AgentLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<AgentLogEntity>)

    @Query("DELETE FROM agent_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM agent_logs")
    suspend fun clearAllLogs()
}

