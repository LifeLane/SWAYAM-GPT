package com.example.edgeaicore.core.storage

import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.database.DataPrivacyLevel
import com.example.edgeaicore.core.database.EdgeDatabase
import com.example.edgeaicore.core.database.MediaMetadataDao
import com.example.edgeaicore.core.database.MediaMetadataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID

/**
 * MediaRepository manages dual-layer media storage:
 * - Structured metadata is persisted in Room (MediaMetadataEntity).
 * - High-volume binary content is persisted securely through StorageEngine.
 */
class MediaRepository(
    private val database: EdgeDatabase,
    private val storageEngine: StorageEngine
) {
    private val dao: MediaMetadataDao = database.mediaMetadataDao()

    fun observeAllMedia(): Flow<List<MediaMetadataEntity>> = dao.getAllMedia()
    fun observeMediaByType(type: String): Flow<List<MediaMetadataEntity>> = dao.getMediaByType(type)
    fun getMediaCount(): Flow<Int> = dao.getCount()

    suspend fun getMediaById(id: String): MediaMetadataEntity? = withContext(Dispatchers.IO) {
        dao.getMediaById(id)
    }

    /**
     * Stores media binary into application-private storage and records its metadata in Room.
     */
    suspend fun storeMedia(
        fileName: String,
        mediaType: String,
        mimeType: String,
        inputStream: InputStream,
        directory: StorageDirectory = StorageDirectory.MEDIA,
        privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY,
        width: Int? = null,
        height: Int? = null,
        durationMs: Long? = null,
        thumbnailPath: String? = null,
        source: String = "app_input"
    ): EdgeResult<MediaMetadataEntity> = withContext(Dispatchers.IO) {
        try {
            val saveResult = storageEngine.save(directory, fileName, inputStream)
            if (saveResult is EdgeResult.Failure) {
                return@withContext EdgeResult.Failure(saveResult.error)
            }

            val meta = (saveResult as EdgeResult.Success).data
            val id = UUID.randomUUID().toString()
            val entity = MediaMetadataEntity(
                id = id,
                fileName = fileName,
                mediaType = mediaType,
                mimeType = mimeType,
                relativePath = meta.relativePath,
                sizeBytes = meta.sizeBytes,
                checksumSha256 = meta.checksumSha256,
                thumbnailPath = thumbnailPath,
                width = width,
                height = height,
                durationMs = durationMs,
                privacyLevel = privacyLevel,
                source = source,
                createdAt = System.currentTimeMillis()
            )

            dao.insertMedia(entity)
            EdgeResult.Success(entity)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    /**
     * Retrieves an input stream for the given media ID without loading the whole file into RAM.
     */
    suspend fun openMediaStream(mediaId: String): EdgeResult<InputStream> = withContext(Dispatchers.IO) {
        val entity = dao.getMediaById(mediaId)
            ?: return@withContext EdgeResult.Failure(IllegalArgumentException("Media record not found for id $mediaId"))

        val dir = when (entity.mediaType.uppercase()) {
            "IMAGE" -> StorageDirectory.IMAGES
            "AUDIO" -> StorageDirectory.AUDIO
            "VIDEO" -> StorageDirectory.VIDEO
            "DOCUMENT" -> StorageDirectory.DOCUMENTS
            else -> StorageDirectory.MEDIA
        }

        storageEngine.readStream(dir, entity.fileName)
    }

    /**
     * Deletes media metadata and deletes the physical binary file.
     */
    suspend fun deleteMedia(mediaId: String, purgeBinary: Boolean = true): EdgeResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = dao.getMediaById(mediaId)
            if (entity != null) {
                dao.purgeMedia(mediaId)
                if (purgeBinary) {
                    val dir = when (entity.mediaType.uppercase()) {
                        "IMAGE" -> StorageDirectory.IMAGES
                        "AUDIO" -> StorageDirectory.AUDIO
                        "VIDEO" -> StorageDirectory.VIDEO
                        "DOCUMENT" -> StorageDirectory.DOCUMENTS
                        else -> StorageDirectory.MEDIA
                    }
                    storageEngine.delete(dir, entity.fileName)
                    if (entity.thumbnailPath != null) {
                        val thumbName = entity.thumbnailPath.substringAfterLast("/")
                        storageEngine.delete(StorageDirectory.IMAGES, thumbName)
                    }
                }
            }
            EdgeResult.Success(Unit)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }
}
