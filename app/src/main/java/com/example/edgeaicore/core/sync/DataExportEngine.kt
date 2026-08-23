package com.example.edgeaicore.core.sync

import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.database.EdgeDatabase
import com.example.edgeaicore.core.storage.StorageDirectory
import com.example.edgeaicore.core.storage.StorageEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

data class ExportManifest(
    val exportVersion: String = "1.0",
    val createdAt: Long = System.currentTimeMillis(),
    val databaseVersion: Int = 2,
    val totalMemoriesExported: Int,
    val totalTasksExported: Int,
    val totalKnowledgeItemsExported: Int,
    val checksumSha256: String
)

/**
 * DataExportEngine packages user data into structured JSON, CSV, or backup archives with cryptographic manifest.
 */
class DataExportEngine(
    private val database: EdgeDatabase,
    private val storageEngine: StorageEngine
) {
    suspend fun exportUserDataJson(): EdgeResult<String> = withContext(Dispatchers.IO) {
        try {
            val memories = database.memoryDao().getAllActiveMemoriesSync()
            val tasks = database.openHelper.readableDatabase.let { db ->
                val cursor = db.query("SELECT id, title, description, isCompleted FROM tasks WHERE lifecycleState != 'DELETED'")
                val list = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    list.add("""{"id":${cursor.getLong(0)},"title":"${cursor.getString(1).replace("\"", "\\\"")}","completed":${cursor.getInt(3) == 1}}""")
                }
                cursor.close()
                list
            }
            val knowledge = database.knowledgeDao().getAllKnowledgeSync()

            val jsonContent = buildString {
                appendLine("{")
                appendLine("  \"exportTimestamp\": ${System.currentTimeMillis()},")
                appendLine("  \"memories\": [")
                memories.forEachIndexed { i, m ->
                    val comma = if (i < memories.size - 1) "," else ""
                    appendLine("    {\"id\": ${m.id}, \"title\": \"${m.title.replace("\"", "\\\"")}\", \"summary\": \"${m.summary.replace("\"", "\\\"")}\"}$comma")
                }
                appendLine("  ],")
                appendLine("  \"tasks\": [")
                tasks.forEachIndexed { i, t ->
                    val comma = if (i < tasks.size - 1) "," else ""
                    appendLine("    $t$comma")
                }
                appendLine("  ],")
                appendLine("  \"knowledge\": [")
                knowledge.forEachIndexed { i, k ->
                    val comma = if (i < knowledge.size - 1) "," else ""
                    appendLine("    {\"id\": \"${k.id}\", \"title\": \"${k.title.replace("\"", "\\\"")}\", \"type\": \"${k.type}\"}$comma")
                }
                appendLine("  ]")
                appendLine("}")
            }

            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(jsonContent.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

            val fileName = "export_${System.currentTimeMillis()}.json"
            val saveResult = storageEngine.saveString(StorageDirectory.EXPORTS, fileName, jsonContent)

            if (saveResult is EdgeResult.Success) {
                val manifestName = "manifest_${System.currentTimeMillis()}.json"
                val manifestJson = """
                {
                  "exportVersion": "1.0",
                  "createdAt": ${System.currentTimeMillis()},
                  "databaseVersion": 2,
                  "totalMemoriesExported": ${memories.size},
                  "totalTasksExported": ${tasks.size},
                  "totalKnowledgeItemsExported": ${knowledge.size},
                  "checksumSha256": "$hash"
                }
                """.trimIndent()
                storageEngine.saveString(StorageDirectory.EXPORTS, manifestName, manifestJson)
                EdgeResult.Success("Export saved to exports/$fileName (Checksum: ${hash.take(12)}...)")
            } else {
                EdgeResult.Failure((saveResult as EdgeResult.Failure).error)
            }
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    suspend fun listExports(): List<com.example.edgeaicore.core.storage.StoredFileMetadata> =
        storageEngine.list(StorageDirectory.EXPORTS)
}
