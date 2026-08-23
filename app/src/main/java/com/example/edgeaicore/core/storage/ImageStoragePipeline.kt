package com.example.edgeaicore.core.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.database.DataPrivacyLevel
import com.example.edgeaicore.core.database.MediaMetadataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * ImageStoragePipeline handles:
 * Raw Input -> Validation -> Resize/Compression -> Thumbnail Generation -> Permanent Storage -> Database Metadata -> Indexing Job.
 */
class ImageStoragePipeline(
    private val storageEngine: StorageEngine,
    private val mediaRepository: MediaRepository
) {
    suspend fun processAndStoreImage(
        rawInputStream: InputStream,
        originalFileName: String = "photo.jpg",
        privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY,
        maxDimension: Int = 1920,
        compressionQuality: Int = 85
    ): EdgeResult<MediaMetadataEntity> = withContext(Dispatchers.IO) {
        try {
            val imageId = UUID.randomUUID().toString()
            val finalFileName = "img_${imageId}.jpg"
            val thumbFileName = "thumb_${imageId}.jpg"

            // 1. Decode and Validate
            val rawBytes = rawInputStream.readBytes()
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                return@withContext EdgeResult.Failure(IllegalArgumentException("Invalid image file format"))
            }

            // 2. Resize / Compression
            var sampleSize = 1
            while (boundsOptions.outWidth / sampleSize > maxDimension || boundsOptions.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decodedBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                ?: return@withContext EdgeResult.Failure(IllegalStateException("Failed to decode image bitmap"))

            val mainOutputStream = ByteArrayOutputStream()
            decodedBitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, mainOutputStream)
            val mainImageBytes = mainOutputStream.toByteArray()

            // 3. Thumbnail Generation
            var thumbSampleSize = 1
            while (decodedBitmap.width / thumbSampleSize > 256 || decodedBitmap.height / thumbSampleSize > 256) {
                thumbSampleSize *= 2
            }
            val thumbWidth = (decodedBitmap.width / thumbSampleSize).coerceAtLeast(1)
            val thumbHeight = (decodedBitmap.height / thumbSampleSize).coerceAtLeast(1)
            val thumbBitmap = Bitmap.createScaledBitmap(decodedBitmap, thumbWidth, thumbHeight, true)

            val thumbStream = ByteArrayOutputStream()
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 75, thumbStream)
            storageEngine.saveBytes(StorageDirectory.IMAGES, thumbFileName, thumbStream.toByteArray())

            // 4. Permanent Storage and DB Metadata
            val result = mediaRepository.storeMedia(
                fileName = finalFileName,
                mediaType = "IMAGE",
                mimeType = "image/jpeg",
                inputStream = ByteArrayInputStream(mainImageBytes),
                directory = StorageDirectory.IMAGES,
                privacyLevel = privacyLevel,
                width = decodedBitmap.width,
                height = decodedBitmap.height,
                thumbnailPath = "images/$thumbFileName",
                source = "camera_pipeline"
            )

            result
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }
}
