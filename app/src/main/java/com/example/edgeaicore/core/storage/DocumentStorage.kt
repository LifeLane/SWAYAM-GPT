package com.example.edgeaicore.core.storage

import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.database.DataPrivacyLevel
import com.example.edgeaicore.core.database.DocumentEntity
import com.example.edgeaicore.core.database.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID

/**
 * DocumentStorage handles:
 * Ingestion and indexing of PDF, TXT, DOCX, CSV, MD, and image documents.
 */
class DocumentStorage(
    private val storageEngine: StorageEngine,
    private val documentRepository: DocumentRepository
) {
    suspend fun storeDocument(
        title: String,
        fileExtension: String,
        mimeType: String,
        inputStream: InputStream,
        privacyLevel: DataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY,
        textSummary: String = ""
    ): EdgeResult<DocumentEntity> = withContext(Dispatchers.IO) {
        try {
            val docId = UUID.randomUUID().toString()
            val storedFileName = "doc_${docId}.${fileExtension.lowercase().removePrefix(".")}"

            val saveResult = storageEngine.save(StorageDirectory.DOCUMENTS, storedFileName, inputStream)
            if (saveResult is EdgeResult.Failure) {
                return@withContext EdgeResult.Failure(saveResult.error)
            }

            val meta = (saveResult as EdgeResult.Success).data
            val entity = DocumentEntity(
                id = docId,
                title = title,
                fileExtension = fileExtension.lowercase().removePrefix("."),
                mimeType = mimeType,
                relativeStoragePath = meta.relativePath,
                sizeBytes = meta.sizeBytes,
                checksumSha256 = meta.checksumSha256,
                extractedTextSummary = textSummary,
                totalPages = 1,
                totalChunks = 0,
                isIndexed = false,
                privacyLevel = privacyLevel,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            documentRepository.create(entity)
            EdgeResult.Success(entity)
        } catch (e: Exception) {
            EdgeResult.Failure(e)
        }
    }

    suspend fun readDocumentContent(docId: String): EdgeResult<String> = withContext(Dispatchers.IO) {
        val docRes = documentRepository.getById(docId)
        if (docRes !is EdgeResult.Success || docRes.data == null) {
            return@withContext EdgeResult.Failure(IllegalArgumentException("Document not found"))
        }

        val doc = docRes.data
        val fileName = doc.relativeStoragePath.substringAfterLast("/")
        storageEngine.readString(StorageDirectory.DOCUMENTS, fileName)
    }
}
