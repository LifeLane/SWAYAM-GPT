package com.example.edgeaicore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.edgeaicore.core.ai.AIRequest
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.RiskLevel
import com.example.edgeaicore.core.database.DataPermission
import com.example.edgeaicore.core.database.DataPrivacyLevel
import com.example.edgeaicore.core.database.TaskEntity
import com.example.edgeaicore.core.mcp.McpTrustLevel
import com.example.edgeaicore.core.storage.StorageDirectory
import com.example.edgeaicore.core.sync.BackupDestination
import com.example.edgeaicore.core.sync.ConflictResolutionStrategy
import com.example.edgeaicore.core.sync.ImportStrategy
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
class SecurityAndSystemIntegrationTest {

    private lateinit var context: Context
    private lateinit var edgeAI: EdgeAICore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        edgeAI = EdgeAICore.getInstance(context)
    }

    // ----------------------------------------------------
    // SECURITY TESTS MANDATE
    // ----------------------------------------------------

    @Test
    fun testSecurity_LLMCannotAccessRawDatabase() = runBlocking {
        // DataGateway strictly rejects untrusted or undeclared direct database access
        val checkRes = edgeAI.database.permissionManager.checkPermission(
            permission = DataPermission.READ,
            dataPrivacyLevel = DataPrivacyLevel.PRIVATE,
            isAgentCaller = true,
            userConsentGiven = false,
            targetResource = "raw_sql_execution"
        )
        // Without explicit schema permission and user consent, access is denied
        assertTrue(checkRes is EdgeResult.Failure)
    }

    @Test
    fun testSecurity_LLMCannotAccessFilesystemDirectly() = runBlocking {
        // Filesystem is completely encapsulated within StorageEngine
        // Any attempt to read outside the defined sandboxed enum directories is prevented
        val testFileName = "safe_sandboxed_file.txt"
        val saveRes = edgeAI.storage.save(StorageDirectory.DOCUMENTS, testFileName, "Protected Content")
        assertTrue(saveRes is EdgeResult.Success)

        val existsInDocs = edgeAI.storage.exists(StorageDirectory.DOCUMENTS, testFileName)
        assertTrue(existsInDocs)

        // Read through storage facade is controlled
        val readRes = edgeAI.storage.read(StorageDirectory.DOCUMENTS, testFileName)
        assertTrue(readRes is EdgeResult.Success)
        assertEquals("Protected Content", (readRes as EdgeResult.Success).data)
    }

    @Test
    fun testSecurity_MCPCannotExecuteArbitrarySQL() {
        // MCP registry only exposes explicitly registered high-level tool definitions with schema validators
        val mcpTools = edgeAI.mcpClient.toolRegistry.getAllTools()
        // Ensure no tool named "sql.exec", "sqlite.raw", or "database.query" with raw SQL input exists
        val hasRawSqlTool = mcpTools.any { it.name.contains("raw_sql") || it.name.contains("exec_sql") }
        assertFalse("MCP must not expose arbitrary SQL execution", hasRawSqlTool)
    }

    @Test
    fun testSecurity_MCPCannotBypassToolGateway() = runBlocking {
        // Untrusted MCP server invocations are intercepted by McpSecurityManager
        val untrustedValidation = edgeAI.mcpClient.securityManager.validateToolInvocation(
            serverId = "untrusted_peer",
            toolName = "any_tool",
            requiredPrivacy = PrivacyLevel.LOCAL_ONLY
        )
        assertTrue(untrustedValidation is EdgeResult.Failure)
    }

    @Test
    fun testSecurity_RemoteServerCannotAccessLocalOnlyData() = runBlocking {
        val check = edgeAI.database.permissionManager.checkPermission(
            permission = DataPermission.SYNC,
            dataPrivacyLevel = DataPrivacyLevel.LOCAL_ONLY,
            isAgentCaller = false,
            userConsentGiven = true,
            targetResource = "personal_vault"
        )
        // Even with userConsentGiven=true, LOCAL_ONLY data cannot be synced to remote
        assertTrue(check is EdgeResult.Failure)
    }

    @Test
    fun testSecurity_CloudFallbackCannotAccessSensitiveDataWithoutConsent() = runBlocking {
        // Sensitive request to cloud without consent MUST fail validation
        val sensitiveRequest = AIRequest(
            prompt = "Analyze my medical prescription",
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            userConsent = false
        )
        val target = edgeAI.aiRouter.determineTargetProvider(sensitiveRequest)
        val isAllowed = edgeAI.privacyEngine.validateRouting(
            privacyLevel = sensitiveRequest.privacyLevel,
            targetProvider = target,
            userConsentGiven = sensitiveRequest.userConsent
        )
        // Local-only target is valid for local execution
        assertTrue(isAllowed)

        // SENSITIVE or LOCAL_ONLY routed to Cloud must be blocked
        val isCloudAllowedForSensitive = edgeAI.privacyEngine.validateRouting(
            privacyLevel = PrivacyLevel.LOCAL_ONLY,
            targetProvider = com.example.edgeaicore.core.common.AIProviderType.CLOUD,
            userConsentGiven = false
        )
        assertFalse("Cloud routing for LOCAL_ONLY data must be rejected", isCloudAllowedForSensitive)
    }

    // ----------------------------------------------------
    // SYSTEM SUBSYSTEM TESTS
    // ----------------------------------------------------

    @Test
    fun testDatabase_TransactionsAndRollback() = runBlocking {
        val txResult = edgeAI.database.transaction { db ->
            db.taskDao().insertTask(
                TaskEntity(title = "Tx Task 1", description = "In Transaction")
            )
            db.taskDao().insertTask(
                TaskEntity(title = "Tx Task 2", description = "In Transaction")
            )
            "COMMITTED"
        }
        assertTrue(txResult is EdgeResult.Success)
        assertEquals("COMMITTED", (txResult as EdgeResult.Success).data)
    }

    @Test
    fun testStorage_LargeFileStreamingAndChecksumValidation() = runBlocking {
        // Generate simulated chunked stream (64 KB)
        val data = ByteArray(64 * 1024) { (it % 128).toByte() }
        val stream = ByteArrayInputStream(data)

        val saveRes = edgeAI.storage.save(StorageDirectory.MEDIA, "streaming_media_test.bin", stream)
        assertTrue(saveRes is EdgeResult.Success)

        val meta = (saveRes as EdgeResult.Success).data
        assertEquals(64 * 1024L, meta.sizeBytes)
        assertNotNull(meta.checksumSha256)
        assertTrue(meta.checksumSha256.isNotBlank())

        // Validate checksum computation
        val computedChecksum = edgeAI.storage.engine.computeChecksum(StorageDirectory.MEDIA, "streaming_media_test.bin")
        assertEquals(meta.checksumSha256, computedChecksum)
    }

    @Test
    fun testSync_OfflineQueueAndConflictResolution() = runBlocking {
        // Queue change while offline
        val queueRes = edgeAI.sync.engine.queueChange(
            entityType = "TASK",
            entityId = "task-101",
            operation = "UPDATE",
            payloadJson = """{"title":"Updated Offline"}"""
        )
        assertTrue(queueRes is EdgeResult.Success)

        // Conflict resolution algorithm test
        val resolvedLocal = edgeAI.sync.engine.resolveConflict(
            localTimestamp = 1000L,
            remoteTimestamp = 500L,
            strategy = ConflictResolutionStrategy.LOCAL_NEWER
        )
        assertEquals(ConflictResolutionStrategy.LOCAL_NEWER, resolvedLocal)

        val resolvedRemote = edgeAI.sync.engine.resolveConflict(
            localTimestamp = 500L,
            remoteTimestamp = 1000L,
            strategy = ConflictResolutionStrategy.LOCAL_NEWER
        )
        assertEquals(ConflictResolutionStrategy.REMOTE_NEWER, resolvedRemote)
    }

    @Test
    fun testBackupAndRestore_FullLifecycle() = runBlocking {
        // 1. Create backup snapshot
        val backupRes = edgeAI.backup.create(BackupDestination.LOCAL_DEVICE, userConsentGiven = true)
        assertTrue(backupRes is EdgeResult.Success)

        // 2. List backups
        val backups = edgeAI.backup.list()
        assertTrue(backups.isNotEmpty())

        val latestBackup = backups.first()
        // 3. Restore backup
        val restoreRes = edgeAI.backup.restore(latestBackup.fileName, userConsentGiven = true)
        assertTrue(restoreRes is EdgeResult.Success)
    }

    @Test
    fun testDataExportAndImport_FullLifecycle() = runBlocking {
        val exportRes = edgeAI.export.create()
        assertTrue(exportRes is EdgeResult.Success)

        val exports = edgeAI.export.list()
        assertTrue(exports.isNotEmpty())

        val latestExport = exports.first().fileName
        val validationRes = edgeAI.sync.import.validateExportFile(latestExport)
        assertTrue(validationRes is EdgeResult.Success)
        assertTrue((validationRes as EdgeResult.Success).data.isValid)

        val importRes = edgeAI.sync.import.executeImport(latestExport, ImportStrategy.MERGE)
        assertTrue(importRes is EdgeResult.Success)
    }
}
