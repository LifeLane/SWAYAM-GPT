package com.example.edgeaicore.core.storage

import android.content.Context
import com.example.edgeaicore.core.common.EdgeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class StorageBreakdown(
    val totalAppStorageBytes: Long,
    val mediaStorageBytes: Long,
    val imagesStorageBytes: Long,
    val audioStorageBytes: Long,
    val videoStorageBytes: Long,
    val documentsStorageBytes: Long,
    val modelsStorageBytes: Long,
    val cacheStorageBytes: Long,
    val backupsStorageBytes: Long,
    val exportsStorageBytes: Long,
    val freeDeviceStorageBytes: Long,
    val totalDeviceStorageBytes: Long
)

data class StorageCleanupSuggestion(
    val title: String,
    val description: String,
    val potentialSavingsBytes: Long,
    val directory: StorageDirectory
)

/**
 * StorageManager monitors on-device disk quotas, visualizes storage distribution,
 * and provides safe recommendations without ever deleting user data automatically.
 */
class StorageManager(
    private val context: Context,
    private val storageEngine: StorageEngine
) {
    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        file.listFiles()?.forEach { child ->
            size += getFolderSize(child)
        }
        return size
    }

    suspend fun getStorageBreakdown(): StorageBreakdown = withContext(Dispatchers.IO) {
        val filesDir = context.filesDir
        val freeBytes = filesDir.freeSpace
        val totalDeviceBytes = filesDir.totalSpace

        fun sizeOf(dir: StorageDirectory): Long {
            return getFolderSize(File(filesDir, dir.folderName))
        }

        val media = sizeOf(StorageDirectory.MEDIA)
        val images = sizeOf(StorageDirectory.IMAGES)
        val audio = sizeOf(StorageDirectory.AUDIO)
        val video = sizeOf(StorageDirectory.VIDEO)
        val docs = sizeOf(StorageDirectory.DOCUMENTS)
        val models = sizeOf(StorageDirectory.MODELS)
        val cache = sizeOf(StorageDirectory.CACHE)
        val backups = sizeOf(StorageDirectory.BACKUPS)
        val exports = sizeOf(StorageDirectory.EXPORTS)

        val totalApp = media + images + audio + video + docs + models + cache + backups + exports

        StorageBreakdown(
            totalAppStorageBytes = totalApp,
            mediaStorageBytes = media,
            imagesStorageBytes = images,
            audioStorageBytes = audio,
            videoStorageBytes = video,
            documentsStorageBytes = docs,
            modelsStorageBytes = models,
            cacheStorageBytes = cache,
            backupsStorageBytes = backups,
            exportsStorageBytes = exports,
            freeDeviceStorageBytes = freeBytes,
            totalDeviceStorageBytes = totalDeviceBytes
        )
    }

    suspend fun getCleanupSuggestions(): List<StorageCleanupSuggestion> = withContext(Dispatchers.IO) {
        val suggestions = mutableListOf<StorageCleanupSuggestion>()
        val filesDir = context.filesDir

        val cacheSize = getFolderSize(File(filesDir, StorageDirectory.CACHE.folderName))
        if (cacheSize > 5 * 1024 * 1024) { // > 5MB
            suggestions.add(
                StorageCleanupSuggestion(
                    title = "Clear Transient AI & Image Cache",
                    description = "Remove cached responses and intermediate perception tensors.",
                    potentialSavingsBytes = cacheSize,
                    directory = StorageDirectory.CACHE
                )
            )
        }

        val exportsSize = getFolderSize(File(filesDir, StorageDirectory.EXPORTS.folderName))
        if (exportsSize > 10 * 1024 * 1024) {
            suggestions.add(
                StorageCleanupSuggestion(
                    title = "Prune Old Data Exports",
                    description = "Archive or remove previously downloaded backup exports.",
                    potentialSavingsBytes = exportsSize,
                    directory = StorageDirectory.EXPORTS
                )
            )
        }

        suggestions
    }

    suspend fun clearDirectory(directory: StorageDirectory): EdgeResult<Int> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, directory.folderName)
            var count = 0
            dir.listFiles()?.forEach { file ->
                if (file.delete()) count++
            }
            EdgeResult.Success(count)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }
}
