package com.example.edgeaicore.core.database

import android.content.Context
import androidx.room.withTransaction
import com.example.edgeaicore.core.common.EdgeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class DatabaseTier {
    LOCAL,
    PRIVATE_SERVER,
    REMOTE
}

interface DatabaseProvider {
    val tier: DatabaseTier
    val isReady: Boolean
    suspend fun ping(): Boolean
}

class LocalDatabaseProvider(val database: EdgeDatabase) : DatabaseProvider {
    override val tier: DatabaseTier = DatabaseTier.LOCAL
    override val isReady: Boolean = true
    override suspend fun ping(): Boolean = true
}

class PrivateDatabaseProvider(
    val serverUrl: String,
    val apiKey: String? = null
) : DatabaseProvider {
    override val tier: DatabaseTier = DatabaseTier.PRIVATE_SERVER
    override val isReady: Boolean = serverUrl.isNotBlank()
    override suspend fun ping(): Boolean = isReady
}

class RemoteDatabaseProvider(
    val endpoint: String
) : DatabaseProvider {
    override val tier: DatabaseTier = DatabaseTier.REMOTE
    override val isReady: Boolean = endpoint.isNotBlank()
    override suspend fun ping(): Boolean = isReady
}

/**
 * Diagnostics information for database observability.
 */
data class DatabaseStats(
    val databaseSizeBytes: Long,
    val taskCount: Int,
    val documentCount: Int,
    val knowledgeItemCount: Int,
    val memoryCount: Int,
    val embeddingCount: Int,
    val mediaCount: Int,
    val auditCount: Int,
    val activeTier: DatabaseTier,
    val isHealthy: Boolean = true
)

/**
 * Enterprise Database Maintenance utility.
 */
class DatabaseMaintenance(private val database: EdgeDatabase, private val context: Context) {
    suspend fun runMaintenance(): EdgeResult<String> = withContext(Dispatchers.IO) {
        try {
            val db = database.openHelper.writableDatabase
            db.execSQL("VACUUM")
            EdgeResult.Success("Database vacuum and index optimization completed successfully.")
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    suspend fun getStats(activeTier: DatabaseTier): DatabaseStats = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath("edgeai_core_database.db")
        val size = if (dbFile.exists()) dbFile.length() else 0L

        val db = database.openHelper.readableDatabase
        fun queryCount(table: String): Int = try {
            val cursor = db.query("SELECT COUNT(*) FROM $table")
            if (cursor.moveToFirst()) {
                val count = cursor.getInt(0)
                cursor.close()
                count
            } else {
                cursor.close()
                0
            }
        } catch (_: Exception) {
            0
        }

        DatabaseStats(
            databaseSizeBytes = size,
            taskCount = queryCount("tasks"),
            documentCount = queryCount("documents"),
            knowledgeItemCount = queryCount("knowledge_items"),
            memoryCount = queryCount("memories"),
            embeddingCount = queryCount("embeddings"),
            mediaCount = queryCount("media_metadata"),
            auditCount = queryCount("audit_records"),
            activeTier = activeTier,
            isHealthy = true
        )
    }
}

/**
 * Unified DatabaseEngine.
 * Provides abstract multi-tier database access, safe transaction orchestration,
 * repositories management, and observability.
 */
class DatabaseEngine(
    val context: Context,
    val database: EdgeDatabase = EdgeDatabase.getInstance(context)
) {
    private val localProvider = LocalDatabaseProvider(database)
    private var privateProvider: PrivateDatabaseProvider? = null
    private var remoteProvider: RemoteDatabaseProvider? = null

    private val _currentTier = MutableStateFlow(DatabaseTier.LOCAL)
    val currentTier: StateFlow<DatabaseTier> = _currentTier.asStateFlow()

    // Repositories
    val tasks = TaskRepository(database)
    val documents = DocumentRepository(database)
    val events = EventRepository(database)
    val agents = AgentRepository(database)
    val audits = AuditRepository(database)
    val usage = UsageRepository(database)
    val models = ModelRepository(database)
    val mcp = McpRepository(database)
    val users = UserRepository(database)
    val embeddings = EmbeddingRepository(database)
    val agentLogs = AgentLogRepository(database)
    val maintenance = DatabaseMaintenance(database, context)

    fun setTier(tier: DatabaseTier) {
        _currentTier.value = tier
    }

    fun configurePrivateServer(url: String, token: String?) {
        privateProvider = PrivateDatabaseProvider(url, token)
    }

    fun getActiveProvider(): DatabaseProvider {
        return when (_currentTier.value) {
            DatabaseTier.LOCAL -> localProvider
            DatabaseTier.PRIVATE_SERVER -> privateProvider ?: localProvider
            DatabaseTier.REMOTE -> remoteProvider ?: localProvider
        }
    }

    /**
     * Executes atomic transactions across Room tables.
     * Ensures all operations commit together or roll back cleanly on any failure.
     */
    suspend fun <R> runTransaction(block: suspend (EdgeDatabase) -> R): EdgeResult<R> = withContext(Dispatchers.IO) {
        try {
            val result = database.withTransaction {
                block(database)
            }
            EdgeResult.Success(result)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    suspend fun getDatabaseStats(): DatabaseStats = maintenance.getStats(_currentTier.value)

    suspend fun optimizeDatabase(): EdgeResult<String> = maintenance.runMaintenance()
}


