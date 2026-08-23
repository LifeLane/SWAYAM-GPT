package com.example.edgeaicore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.database.DataPermission
import com.example.edgeaicore.core.database.DataPrivacyLevel
import com.example.edgeaicore.core.database.TaskEntity
import com.example.edgeaicore.core.knowledge.ChunkingStrategy
import com.example.edgeaicore.core.storage.StorageDirectory
import com.example.edgeaicore.core.sync.BackupDestination
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseStorageKnowledgeTest {

    private lateinit var context: Context
    private lateinit var edgeAI: EdgeAICore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        edgeAI = EdgeAICore.getInstance(context)
    }

    @Test
    fun testTaskRepositoryCrud() = runBlocking {
        val task = TaskEntity(
            title = "Test Edge Task",
            description = "Verifying database infrastructure",
            priority = "HIGH",
            privacyLevel = DataPrivacyLevel.LOCAL_ONLY
        )

        val createRes = edgeAI.database.tasks.create(task)
        assertTrue(createRes is EdgeResult.Success)

        val createdId = (createRes as EdgeResult.Success).data
        val fetchedRes = edgeAI.database.tasks.getById(createdId)
        assertTrue(fetchedRes is EdgeResult.Success)
        val fetched = (fetchedRes as EdgeResult.Success).data
        assertNotNull(fetched)
        assertEquals("Test Edge Task", fetched?.title)
        assertEquals("HIGH", fetched?.priority)

        // Update
        val updated = fetched!!.copy(isCompleted = true)
        val updateRes = edgeAI.database.tasks.update(updated)
        assertTrue(updateRes is EdgeResult.Success)

        val completedRes = edgeAI.database.tasks.getById(createdId)
        assertTrue(completedRes is EdgeResult.Success)
        val completed = (completedRes as EdgeResult.Success).data
        assertTrue(completed?.isCompleted == true)
    }

    @Test
    fun testDataGatewaySecurityPolicy() = runBlocking {
        // 1. LOCAL_ONLY data cannot be synced externally
        val syncCheck = edgeAI.database.permissionManager.checkPermission(
            permission = DataPermission.SYNC,
            dataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY,
            isAgentCaller = false,
            userConsentGiven = true,
            targetResource = "local_notes"
        )
        assertTrue(syncCheck is EdgeResult.Failure)

        // 2. Agent caller trying to delete without consent fails
        val deleteCheck = edgeAI.database.permissionManager.checkPermission(
            permission = DataPermission.DELETE,
            dataPrivacyLevel = DataPrivacyLevel.PRIVATE,
            isAgentCaller = true,
            userConsentGiven = false,
            targetResource = "database_records"
        )
        assertTrue(deleteCheck is EdgeResult.Failure)

        // 3. Agent caller with consent succeeds
        val deleteWithConsent = edgeAI.database.permissionManager.checkPermission(
            permission = DataPermission.DELETE,
            dataPrivacyLevel = DataPrivacyLevel.PRIVATE,
            isAgentCaller = true,
            userConsentGiven = true,
            targetResource = "database_records"
        )
        assertTrue(deleteWithConsent is EdgeResult.Success)
    }

    @Test
    fun testStorageEngineSaveAndRead() = runBlocking {
        val testContent = "EdgeAI Core Storage Test Content 12345"
        val inputStream = ByteArrayInputStream(testContent.toByteArray(Charsets.UTF_8))

        val saveRes = edgeAI.storage.engine.save(
            directory = StorageDirectory.DOCUMENTS,
            fileName = "unit_test_doc.txt",
            inputStream = inputStream
        )
        assertTrue(saveRes is EdgeResult.Success)

        val meta = (saveRes as EdgeResult.Success).data
        assertEquals("unit_test_doc.txt", meta.fileName)
        assertTrue(meta.sizeBytes > 0)
        assertNotNull(meta.checksumSha256)

        // Read back
        val readStreamRes = edgeAI.storage.engine.readStream(StorageDirectory.DOCUMENTS, "unit_test_doc.txt")
        assertTrue(readStreamRes is EdgeResult.Success)
        val readStream = (readStreamRes as EdgeResult.Success).data
        val readText = readStream.bufferedReader().use { it.readText() }
        assertEquals(testContent, readText)

        // Metadata verification
        val retrievedMeta = edgeAI.storage.engine.getMetadata(StorageDirectory.DOCUMENTS, "unit_test_doc.txt")
        assertNotNull(retrievedMeta)
        assertEquals(meta.checksumSha256, retrievedMeta?.checksumSha256)
    }

    @Test
    fun testKnowledgeIngestionAndSearch() = runBlocking {
        val rawDoc = """
            # EdgeAI Agent Runtime
            The EdgeAI platform executes LLM reasoning on-device using LiteRT and MediaPipe.
            Privacy is enforced via strict hardware boundary gating and permission policies.
            
            # Local Database Tier
            The database subsystem uses Room SQLite with multi-tier synchronization.
            All database operations operate offline-first without requiring cloud connections.
        """.trimIndent()

        val ingestRes = edgeAI.knowledge.ingestion.ingestDocument(
            title = "EdgeAI Architecture Overview",
            rawText = rawDoc,
            source = "test_manual",
            chunkStrategy = ChunkingStrategy.PARAGRAPH
        )

        assertTrue(ingestRes is EdgeResult.Success)
        val resultData = (ingestRes as EdgeResult.Success).data
        assertTrue(resultData.totalChunksGenerated >= 2)

        // Search by keyword
        val searchRes = edgeAI.knowledge.search.search("LiteRT MediaPipe", limit = 5)
        assertTrue(searchRes is EdgeResult.Success)
        val results = (searchRes as EdgeResult.Success).data
        assertTrue(results.isNotEmpty())
        assertEquals("EdgeAI Architecture Overview", results[0].title)

        // Search formatted context for agent prompt
        val promptContext = edgeAI.knowledge.search.getFormattedAgentContext("SQLite database")
        assertTrue(promptContext.contains("EdgeAI Architecture Overview") || promptContext.contains("database"))
    }

    @Test
    fun testDataExportAndBackupIntegrity() = runBlocking {
        // Create sample task
        edgeAI.database.tasks.create(
            TaskEntity(
                title = "Backup Test Task",
                description = "Task to be exported and verified",
                priority = "MEDIUM"
            )
        )

        // Export archive
        val exportRes = edgeAI.sync.export.exportUserDataJson()
        assertTrue(exportRes is EdgeResult.Success)
        val exportMsg = (exportRes as EdgeResult.Success).data
        assertTrue(exportMsg.contains("Export saved"))

        // Local snapshot backup
        val backupRes = edgeAI.sync.backup.createBackup(BackupDestination.LOCAL_DEVICE, userConsentGiven = true)
        assertTrue(backupRes is EdgeResult.Success)
        val backupMsg = (backupRes as EdgeResult.Success).data
        assertTrue(backupMsg.contains("completed"))
    }
}

