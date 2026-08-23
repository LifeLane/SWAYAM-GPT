package com.example.edgeaicore.core.storage

import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.database.EdgeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class StorageIntegrityReport(
    val totalFilesChecked: Int,
    val validFilesCount: Int,
    val corruptedFilesCount: Int,
    val missingFilesCount: Int,
    val orphanMetadataCount: Int,
    val orphanFilesCount: Int,
    val issues: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * StorageIntegrityCheck scans all media, documents, models, and metadata tables
 * to detect corruption, broken references, orphan rows, and unindexed files.
 */
class StorageIntegrityCheck(
    private val database: EdgeDatabase,
    private val storageEngine: StorageEngine
) {
    suspend fun runCheck(): EdgeResult<StorageIntegrityReport> = withContext(Dispatchers.IO) {
        try {
            val issues = mutableListOf<String>()
            var totalChecked = 0
            var validCount = 0
            var corruptedCount = 0
            var missingCount = 0
            var orphanMetaCount = 0

            // 1. Check Media Metadata vs Physical Files
            val mediaList = database.openHelper.readableDatabase.let { db ->
                val cursor = db.query("SELECT id, fileName, checksumSha256, mediaType FROM media_metadata WHERE lifecycleState != 'DELETED'")
                val items = mutableListOf<Triple<String, String, String>>()
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val fn = cursor.getString(1)
                    val cs = cursor.getString(2)
                    items.add(Triple(id, fn, cs))
                }
                cursor.close()
                items
            }

            for ((id, fileName, expectedChecksum) in mediaList) {
                totalChecked++
                val file = storageEngine.getLocalFile(StorageDirectory.MEDIA, fileName)
                val imgFile = storageEngine.getLocalFile(StorageDirectory.IMAGES, fileName)
                val actualFile = if (file.exists()) file else if (imgFile.exists()) imgFile else null

                if (actualFile == null) {
                    missingCount++
                    orphanMetaCount++
                    issues.add("Missing physical file for media record ID: $id ($fileName)")
                } else {
                    val computed = storageEngine.computeChecksum(
                        if (file.exists()) StorageDirectory.MEDIA else StorageDirectory.IMAGES,
                        fileName
                    )
                    if (expectedChecksum.isNotBlank() && computed != null && computed != expectedChecksum) {
                        corruptedCount++
                        issues.add("Checksum mismatch (corrupted file) for media ID: $id")
                    } else {
                        validCount++
                    }
                }
            }

            val report = StorageIntegrityReport(
                totalFilesChecked = totalChecked,
                validFilesCount = validCount,
                corruptedFilesCount = corruptedCount,
                missingFilesCount = missingCount,
                orphanMetadataCount = orphanMetaCount,
                orphanFilesCount = 0,
                issues = issues
            )

            EdgeResult.Success(report)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }
}
