package com.example.edgeaicore.core.storage

import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.database.DataPrivacyLevel
import com.example.edgeaicore.core.database.MediaMetadataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.UUID

enum class AudioDeleteOption {
    RECORDING_ONLY,
    TRANSCRIPTION_ONLY,
    BOTH
}

data class AudioNoteRecord(
    val id: String,
    val title: String,
    val audioFileName: String?,
    val transcriptionText: String?,
    val durationMs: Long,
    val sizeBytes: Long,
    val createdAt: Long,
    val privacyLevel: DataPrivacyLevel
)

/**
 * AudioStorage manages voice memos, recordings, and transcription lifecycles.
 */
class AudioStorage(
    private val storageEngine: StorageEngine,
    private val mediaRepository: MediaRepository
) {
    suspend fun saveAudioNote(
        title: String,
        audioInputStream: InputStream?,
        transcriptionText: String?,
        durationMs: Long = 0L,
        privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY
    ): EdgeResult<AudioNoteRecord> = withContext(Dispatchers.IO) {
        try {
            val audioId = UUID.randomUUID().toString()
            var storedFileName: String? = null
            var sizeBytes = 0L

            if (audioInputStream != null) {
                storedFileName = "audio_${audioId}.m4a"
                val res = storageEngine.save(StorageDirectory.AUDIO, storedFileName, audioInputStream)
                if (res is EdgeResult.Success) {
                    sizeBytes = res.data.sizeBytes
                    mediaRepository.storeMedia(
                        fileName = storedFileName,
                        mediaType = "AUDIO",
                        mimeType = "audio/mp4",
                        inputStream = ByteArrayInputStream(ByteArray(0)), // placeholder metadata reference
                        directory = StorageDirectory.AUDIO,
                        privacyLevel = privacyLevel,
                        durationMs = durationMs,
                        source = "voice_recorder"
                    )
                }
            }

            val record = AudioNoteRecord(
                id = audioId,
                title = title,
                audioFileName = storedFileName,
                transcriptionText = transcriptionText,
                durationMs = durationMs,
                sizeBytes = sizeBytes,
                createdAt = System.currentTimeMillis(),
                privacyLevel = privacyLevel
            )

            EdgeResult.Success(record)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    suspend fun deleteAudioNote(
        note: AudioNoteRecord,
        deleteOption: AudioDeleteOption
    ): EdgeResult<Unit> = withContext(Dispatchers.IO) {
        try {
            when (deleteOption) {
                AudioDeleteOption.RECORDING_ONLY -> {
                    if (note.audioFileName != null) {
                        storageEngine.delete(StorageDirectory.AUDIO, note.audioFileName)
                    }
                }
                AudioDeleteOption.TRANSCRIPTION_ONLY -> {
                    // Handled at application state level
                }
                AudioDeleteOption.BOTH -> {
                    if (note.audioFileName != null) {
                        storageEngine.delete(StorageDirectory.AUDIO, note.audioFileName)
                    }
                }
            }
            EdgeResult.Success(Unit)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }
}
