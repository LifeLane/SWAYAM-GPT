package com.example.edgeaicore.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.edgeaicore.core.memory.MemoryDao
import com.example.edgeaicore.core.memory.MemoryEntity
import com.example.edgeaicore.core.memory.MemoryTypeConverters

/**
 * Unified EdgeAI Core Enterprise-Grade Room Database.
 * Contains all persistent stores for EdgeAI local intelligence.
 */
@Database(
    entities = [
        MemoryEntity::class,
        UserEntity::class,
        ProfileEntity::class,
        PreferenceEntity::class,
        TaskEntity::class,
        EventEntity::class,
        ActivityEntity::class,
        AIInteractionEntity::class,
        AgentSessionEntity::class,
        ToolCallEntity::class,
        McpServerEntity::class,
        McpToolEntity::class,
        AutomationEntity::class,
        NotificationEntity::class,
        ModelMetadataEntity::class,
        UsageMetricEntity::class,
        AuditRecordEntity::class,
        SubscriptionEntity::class,
        MediaMetadataEntity::class,
        DocumentEntity::class,
        EmbeddingEntity::class,
        KnowledgeItemEntity::class,
        KnowledgeChunkEntity::class,
        SyncQueueEntity::class,
        AgentLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(DatabaseTypeConverters::class, MemoryTypeConverters::class)
abstract class EdgeDatabase : RoomDatabase() {

    abstract fun memoryDao(): MemoryDao
    abstract fun userDao(): UserDao
    abstract fun profileDao(): ProfileDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun taskDao(): TaskDao
    abstract fun eventDao(): EventDao
    abstract fun activityDao(): ActivityDao
    abstract fun aiInteractionDao(): AIInteractionDao
    abstract fun agentSessionDao(): AgentSessionDao
    abstract fun toolCallDao(): ToolCallDao
    abstract fun mcpServerDao(): McpServerDao
    abstract fun mcpToolDao(): McpToolDao
    abstract fun automationDao(): AutomationDao
    abstract fun notificationDao(): NotificationDao
    abstract fun modelMetadataDao(): ModelMetadataDao
    abstract fun usageMetricDao(): UsageMetricDao
    abstract fun auditDao(): AuditDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun mediaMetadataDao(): MediaMetadataDao
    abstract fun documentDao(): DocumentDao
    abstract fun embeddingDao(): EmbeddingDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun knowledgeChunkDao(): KnowledgeChunkDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun agentLogDao(): AgentLogDao


    companion object {
        private const val DB_NAME = "edgeai_core_database.db"

        @Volatile
        private var INSTANCE: EdgeDatabase? = null

        // Non-destructive Migration v1 -> v2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Creates missing tables if coming from legacy Memory DB
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `users` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `email` TEXT,
                        `role` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isLocalOnly` INTEGER NOT NULL,
                        `lifecycleState` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `priority` TEXT NOT NULL,
                        `dueDate` INTEGER,
                        `category` TEXT NOT NULL,
                        `tags` TEXT NOT NULL,
                        `privacyLevel` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `lifecycleState` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `knowledge_items` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `sourceId` TEXT,
                        `tags` TEXT NOT NULL,
                        `privacyLevel` TEXT NOT NULL,
                        `embeddingReference` TEXT,
                        `checksumSha256` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `lifecycleState` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Non-destructive Migration v2 -> v3
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `knowledge_chunks` (
                        `chunkId` TEXT NOT NULL PRIMARY KEY,
                        `documentId` TEXT NOT NULL,
                        `chunkIndex` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `pageNumber` INTEGER NOT NULL,
                        `positionStart` INTEGER NOT NULL,
                        `positionEnd` INTEGER NOT NULL,
                        `embeddingReference` TEXT,
                        `metadataJson` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `embeddings` (
                        `embeddingId` TEXT NOT NULL PRIMARY KEY,
                        `sourceId` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `modelId` TEXT NOT NULL,
                        `dimension` INTEGER NOT NULL,
                        `vectorJson` TEXT NOT NULL,
                        `contentHash` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `agent_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` TEXT,
                        `agentName` TEXT NOT NULL,
                        `level` TEXT NOT NULL,
                        `tag` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `metadataJson` TEXT,
                        `latencyMs` INTEGER NOT NULL,
                        `tokenCount` INTEGER NOT NULL,
                        `privacyLevel` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): EdgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EdgeDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
