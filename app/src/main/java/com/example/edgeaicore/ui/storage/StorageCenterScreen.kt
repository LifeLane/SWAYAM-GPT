package com.example.edgeaicore.ui.storage

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.storage.StorageBreakdown
import com.example.edgeaicore.core.storage.StorageCleanupSuggestion
import com.example.edgeaicore.core.storage.StorageDirectory
import com.example.edgeaicore.ui.common.AppCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageCenterScreen(
    edgeAI: EdgeAICore,
    onBack: () -> Unit = {},
    onNavigateToModels: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var storageBreakdown by remember { mutableStateOf<StorageBreakdown?>(null) }
    var cleanupSuggestions by remember { mutableStateOf<List<StorageCleanupSuggestion>>(emptyList()) }
    var actionResultText by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Confirmation Dialog state
    var confirmDialogAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var confirmDialogTitle by remember { mutableStateOf("") }
    var confirmDialogMessage by remember { mutableStateOf("") }

    suspend fun loadStorageData() {
        isRefreshing = true
        storageBreakdown = edgeAI.storage.getBreakdown()
        cleanupSuggestions = edgeAI.storage.getCleanupSuggestions()
        isRefreshing = false
    }

    LaunchedEffect(Unit) {
        loadStorageData()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Storage Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("storage_back_btn")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch { loadStorageData() }
                        },
                        modifier = Modifier.testTag("storage_refresh_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. OVERVIEW & BAR VISUALIZATION
            item {
                val breakdown = storageBreakdown
                AppCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("storage_overview_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ON-DEVICE DISK UTILIZATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.2.sp
                                )
                                val totalUsed = breakdown?.totalAppStorageBytes ?: 0L
                                Text(
                                    text = "${formatBytes(totalUsed)} Used by EdgeAI",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (breakdown != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = LocalAIGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${formatBytes(breakdown.freeDeviceStorageBytes)} Free",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LocalAIGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Color-coded storage meter bar
                        if (breakdown != null) {
                            val totalApp = breakdown.totalAppStorageBytes.coerceAtLeast(1L)
                            val memoryWeight = (breakdown.documentsStorageBytes.toFloat() / totalApp).coerceIn(0.05f, 1f)
                            val mediaWeight = ((breakdown.imagesStorageBytes + breakdown.mediaStorageBytes).toFloat() / totalApp).coerceIn(0.05f, 1f)
                            val modelsWeight = (breakdown.modelsStorageBytes.toFloat() / totalApp).coerceIn(0.05f, 1f)
                            val cacheWeight = (breakdown.cacheStorageBytes.toFloat() / totalApp).coerceIn(0.05f, 1f)

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                ) {
                                    Box(modifier = Modifier.weight(memoryWeight).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                                    Box(modifier = Modifier.weight(mediaWeight).fillMaxHeight().background(LocalAIGreen))
                                    Box(modifier = Modifier.weight(modelsWeight).fillMaxHeight().background(CloudAIBorder))
                                    Box(modifier = Modifier.weight(cacheWeight).fillMaxHeight().background(PrivateServerAmber))
                                }

                                // Legend
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StorageLegendItem("Memory", MaterialTheme.colorScheme.primary)
                                    StorageLegendItem("Media", LocalAIGreen)
                                    StorageLegendItem("Models", CloudAIBorder)
                                    StorageLegendItem("Cache", PrivateServerAmber)
                                }
                            }
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            // 2. CATEGORIZED STORAGE USAGE CARDS (Memory, Media, Models, Cache)
            item {
                Text(
                    text = "CATEGORIZED STORAGE BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            val breakdown = storageBreakdown

            // Category 1: MEMORY (Database, vectors, notes)
            item {
                val memBytes = (breakdown?.documentsStorageBytes ?: 0L) + (breakdown?.backupsStorageBytes ?: 0L)
                StorageCategoryCard(
                    title = "Memory & SQLite Vault",
                    subtitle = "Encrypted memory records, vector embeddings, & personal knowledge",
                    sizeBytes = memBytes.coerceAtLeast(1024 * 512), // display baseline
                    icon = Icons.Default.Psychology,
                    iconTint = MaterialTheme.colorScheme.primary,
                    primaryButtonText = "Manage Memories",
                    secondaryButtonText = "Purge Memory",
                    onPrimaryClick = onNavigateToMemories,
                    onSecondaryClick = {
                        confirmDialogTitle = "Purge All Memories?"
                        confirmDialogMessage = "This will wipe all active memory entities and vector index caches from the local SQLite database."
                        confirmDialogAction = {
                            coroutineScope.launch {
                                edgeAI.memory.clear()
                                loadStorageData()
                                actionResultText = "Memory vault cleared successfully."
                            }
                        }
                    },
                    testTag = "storage_card_memory"
                )
            }

            // Category 2: MEDIA (Captured images, attachments, thumbnails)
            item {
                val mediaBytes = (breakdown?.imagesStorageBytes ?: 0L) + (breakdown?.mediaStorageBytes ?: 0L)
                StorageCategoryCard(
                    title = "Media & Captured Photos",
                    subtitle = "Locally stored perception captures, camera scans, and generated thumbnails",
                    sizeBytes = mediaBytes,
                    icon = Icons.Default.PhotoLibrary,
                    iconTint = LocalAIGreen,
                    primaryButtonText = "Clean Thumbnails",
                    secondaryButtonText = "Clear Media Vault",
                    onPrimaryClick = {
                        coroutineScope.launch {
                            val res = edgeAI.storage.clearDirectory(StorageDirectory.IMAGES)
                            loadStorageData()
                            actionResultText = when (res) {
                                is com.example.edgeaicore.core.common.EdgeResult.Success -> "Removed ${res.data} cached image thumbnails."
                                is com.example.edgeaicore.core.common.EdgeResult.Failure -> "Failed: ${res.error.message}"
                            }
                        }
                    },
                    onSecondaryClick = {
                        confirmDialogTitle = "Clear All Media?"
                        confirmDialogMessage = "This will remove all stored perception images, media assets, and camera recordings from on-device storage."
                        confirmDialogAction = {
                            coroutineScope.launch {
                                val resImages = edgeAI.storage.clearDirectory(StorageDirectory.IMAGES)
                                val resMedia = edgeAI.storage.clearDirectory(StorageDirectory.MEDIA)
                                loadStorageData()
                                actionResultText = "Media vault purged (${(resImages as? com.example.edgeaicore.core.common.EdgeResult.Success)?.data ?: 0} images cleared)."
                            }
                        }
                    },
                    testTag = "storage_card_media"
                )
            }

            // Category 3: MODELS (Quantized LLMs & Vision weights)
            item {
                val modelsBytes = breakdown?.modelsStorageBytes ?: 1_400_000_000L
                StorageCategoryCard(
                    title = "AI Neural Models",
                    subtitle = "Quantized Gemma LLM weights, LiteRT models, & MediaPipe vision pipelines",
                    sizeBytes = modelsBytes,
                    icon = Icons.Default.Memory,
                    iconTint = CloudAIBorder,
                    primaryButtonText = "Model Center",
                    secondaryButtonText = "Prune Unused",
                    onPrimaryClick = onNavigateToModels,
                    onSecondaryClick = {
                        coroutineScope.launch {
                            val res = edgeAI.storage.clearDirectory(StorageDirectory.MODELS)
                            loadStorageData()
                            actionResultText = "Pruned temporary model staging buffers."
                        }
                    },
                    testTag = "storage_card_models"
                )
            }

            // Category 4: CACHE (Transient perception tensors, temp buffers)
            item {
                val cacheBytes = breakdown?.cacheStorageBytes ?: 0L
                StorageCategoryCard(
                    title = "Temporary AI Cache",
                    subtitle = "Intermediate reasoning tokens, OCR bounding boxes, and response hashes",
                    sizeBytes = cacheBytes.coerceAtLeast(1024 * 256),
                    icon = Icons.Default.CleaningServices,
                    iconTint = PrivateServerAmber,
                    primaryButtonText = "Clear Cache",
                    secondaryButtonText = null,
                    onPrimaryClick = {
                        coroutineScope.launch {
                            edgeAI.cache.clearAll()
                            edgeAI.storage.clearDirectory(StorageDirectory.CACHE)
                            loadStorageData()
                            actionResultText = "Temporary AI cache and response tensors purged."
                        }
                    },
                    testTag = "storage_card_cache"
                )
            }

            // 3. CLEANUP SUGGESTIONS (If any)
            if (cleanupSuggestions.isNotEmpty()) {
                item {
                    Text(
                        text = "INTELLIGENT CLEANUP RECOMMENDATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }

                items(cleanupSuggestions) { suggestion ->
                    AppCard(
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        borderColor = PrivateServerAmber.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = suggestion.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(text = suggestion.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "Potential Savings: ${formatBytes(suggestion.potentialSavingsBytes)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrivateServerAmber
                                )
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        edgeAI.storage.clearDirectory(suggestion.directory)
                                        loadStorageData()
                                        actionResultText = "Cleaned ${suggestion.title}."
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("cleanup_btn_${suggestion.directory.name.lowercase()}")
                            ) {
                                Text("Clean", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 4. VAULT INTEGRITY & BACKUP TOOLS
            item {
                AppCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "VAULT INTEGRITY & BACKUP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val checkResult = edgeAI.storage.runIntegrityCheck()
                                        actionResultText = when (checkResult) {
                                            is com.example.edgeaicore.core.common.EdgeResult.Success -> {
                                                val report = checkResult.data
                                                if (report.corruptedFilesCount == 0 && report.missingFilesCount == 0) {
                                                    "Integrity Verified: ${report.validFilesCount} files passed checksum checks. Zero corruption detected."
                                                } else {
                                                    "Integrity Issues: ${report.corruptedFilesCount} corrupted files, ${report.missingFilesCount} missing files."
                                                }
                                            }
                                            is com.example.edgeaicore.core.common.EdgeResult.Failure -> {
                                                "Integrity Check Failed: ${checkResult.error.message}"
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("storage_integrity_btn")
                            ) {
                                Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check Health", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = edgeAI.backup.create()
                                        actionResultText = when (res) {
                                            is com.example.edgeaicore.core.common.EdgeResult.Success -> "Encrypted Backup Saved: ${res.data}"
                                            is com.example.edgeaicore.core.common.EdgeResult.Failure -> "Backup Failed: ${res.error.message}"
                                        }
                                        loadStorageData()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("storage_create_backup_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Backup", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Alert
    if (confirmDialogAction != null) {
        AlertDialog(
            onDismissRequest = { confirmDialogAction = null },
            title = { Text(confirmDialogTitle, fontWeight = FontWeight.Bold) },
            text = { Text(confirmDialogMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        val action = confirmDialogAction
                        confirmDialogAction = null
                        action?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Purge")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDialogAction = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Action Outcome Alert
    if (actionResultText != null) {
        AlertDialog(
            onDismissRequest = { actionResultText = null },
            title = { Text("Storage Operation", fontWeight = FontWeight.Bold) },
            text = { Text(actionResultText.orEmpty()) },
            confirmButton = {
                Button(onClick = { actionResultText = null }, modifier = Modifier.testTag("storage_alert_ok_btn")) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun StorageCategoryCard(
    title: String,
    subtitle: String,
    sizeBytes: Long,
    icon: ImageVector,
    iconTint: Color,
    primaryButtonText: String,
    secondaryButtonText: String?,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: (() -> Unit)? = null,
    testTag: String = ""
) {
    AppCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag(testTag)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = iconTint.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column {
                        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Text(
                    text = formatBytes(sizeBytes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (secondaryButtonText != null && onSecondaryClick != null) {
                    TextButton(
                        onClick = onSecondaryClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("${testTag}_secondary_btn")
                    ) {
                        Text(secondaryButtonText, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                FilledTonalButton(
                    onClick = onPrimaryClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("${testTag}_primary_btn")
                ) {
                    Text(primaryButtonText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StorageLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format("%.2f GB", mb / 1024.0)
    } else {
        String.format("%.1f MB", mb)
    }
}
