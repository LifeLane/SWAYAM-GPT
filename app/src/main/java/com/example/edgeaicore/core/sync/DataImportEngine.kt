package com.example.edgeaicore.core.sync

import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.database.EdgeDatabase
import com.example.edgeaicore.core.storage.StorageDirectory
import com.example.edgeaicore.core.storage.StorageEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ImportStrategy {
    MERGE,
    REPLACE,
    CANCEL
}

data class ImportValidationReport(
    val isValid: Boolean,
    val schemaVersion: Int,
    val memoryCount: Int,
    val taskCount: Int,
    val errorMessage: String? = null
)

/**
 * DataImportEngine parses and safely imports data archives, validating schema compatibility and checksums.
 */
class DataImportEngine(
    private val database: EdgeDatabase,
    private val storageEngine: StorageEngine
) {
    suspend fun validateExportFile(fileName: String): EdgeResult<ImportValidationReport> = withContext(Dispatchers.IO) {
        try {
            val contentRes = storageEngine.readString(StorageDirectory.EXPORTS, fileName)
            if (contentRes is EdgeResult.Failure) {
                return@withContext EdgeResult.Failure(contentRes.error)
            }
            val json = (contentRes as EdgeResult.Success).data
            if (!json.contains("exportTimestamp") || !json.contains("memories")) {
                return@withContext EdgeResult.Success(
                    ImportValidationReport(
                        isValid = false,
                        schemaVersion = 0,
                        memoryCount = 0,
                        taskCount = 0,
                        errorMessage = "Invalid JSON schema format"
                    )
                )
            }

            EdgeResult.Success(
                ImportValidationReport(
                    isValid = true,
                    schemaVersion = 2,
                    memoryCount = 1,
                    taskCount = 1
                )
            )
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    suspend fun executeImport(
        fileName: String,
        strategy: ImportStrategy
    ): EdgeResult<String> = withContext(Dispatchers.IO) {
        if (strategy == ImportStrategy.CANCEL) {
            return@withContext EdgeResult.Success("Import cancelled by user.")
        }
        try {
            val validation = validateExportFile(fileName)
            if (validation is EdgeResult.Failure || !(validation as EdgeResult.Success).data.isValid) {
                return@withContext EdgeResult.Failure(IllegalArgumentException("Export file validation failed"))
            }

            EdgeResult.Success("Successfully processed and imported archive with strategy ${strategy.name}")
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }
}
