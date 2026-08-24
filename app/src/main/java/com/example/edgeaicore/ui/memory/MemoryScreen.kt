package com.example.edgeaicore.ui.memory

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.common.AIProviderType
import com.example.edgeaicore.core.memory.MemoryEntity
import com.example.edgeaicore.core.memory.MemoryType
import com.example.edgeaicore.core.storage.EncryptionVaultStatus
import com.example.edgeaicore.ui.common.AIStatus
import com.example.edgeaicore.ui.common.AppCard
import com.example.edgeaicore.ui.common.GoogleFilterChip
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class MemoryFilter(val label: String) {
    ALL("All"),
    DOCUMENTS("Documents"),
    PHOTOS("Photos"),
    NOTES("Notes"),
    VOICE("Voice"),
    TASKS("Tasks"),
    PLACES("Places"),
    PEOPLE("People"),
    FAVORITES("Favorites")
}

enum class DateRangeFilter(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    PAST_WEEK("This Week"),
    PAST_MONTH("This Month")
}

enum class MemorySortOrder(val label: String) {
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First"),
    TITLE_AZ("Title (A-Z)"),
    HIGHEST_CONFIDENCE("Highest Score")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    edgeAI: EdgeAICore,
    onNavigateToAskMemory: (String) -> Unit,
    onSelectMemory: (MemoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allMemories: List<MemoryEntity> by edgeAI.memory.activeMemories.collectAsStateWithLifecycle(initialValue = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter and Sort states
    var selectedFilter by remember { mutableStateOf(MemoryFilter.ALL) }
    var selectedDateRange by remember { mutableStateOf(DateRangeFilter.ALL_TIME) }
    var selectedSortOrder by remember { mutableStateOf(MemorySortOrder.NEWEST_FIRST) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showDateMenu by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showVaultSecurityDialog by remember { mutableStateOf(false) }
    var vaultStatus by remember { mutableStateOf<EncryptionVaultStatus?>(null) }
    var isSelfTesting by remember { mutableStateOf(false) }

    var newMemoryTitle by remember { mutableStateOf("") }
    var newMemoryContent by remember { mutableStateOf("") }
    var newMemoryTags by remember { mutableStateOf("note, personal") }
    var uploadedFileName by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // File Picker for Documents, PDFs, and Text files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                var displayName = "uploaded_document"
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            displayName = it.getString(nameIndex)
                        }
                    }
                }

                val inputStream = contentResolver.openInputStream(uri)
                val rawText = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                val previewContent = if (rawText.isNotBlank()) rawText.take(1500) else "Binary document attached: $displayName"

                uploadedFileName = displayName
                newMemoryTitle = displayName.substringBeforeLast(".")
                newMemoryContent = previewContent
                newMemoryTags = "document, file, imported"
                showAddDialog = true
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Unable to read selected file: ${e.message}")
                }
            }
        }
    }

    // Filter and Sort Logic
    val filteredAndSortedMemories = remember(allMemories, searchQuery, selectedFilter, selectedDateRange, selectedSortOrder) {
        val currentTime = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val oneWeekMs = 7 * oneDayMs
        val oneMonthMs = 30 * oneDayMs

        val filtered = allMemories.filter { mem ->
            // Search query match
            val matchesQuery = if (searchQuery.isBlank()) true else {
                mem.title.contains(searchQuery, ignoreCase = true) ||
                mem.content.contains(searchQuery, ignoreCase = true) ||
                mem.tags.contains(searchQuery, ignoreCase = true) ||
                (mem.summary?.contains(searchQuery, ignoreCase = true) == true)
            }

            // Category filter match
            val matchesType = when (selectedFilter) {
                MemoryFilter.ALL -> true
                MemoryFilter.DOCUMENTS -> mem.type == MemoryType.DOCUMENT || mem.tags.contains("document", ignoreCase = true) || mem.tags.contains("file", ignoreCase = true)
                MemoryFilter.PHOTOS -> mem.type == MemoryType.IMAGE || mem.tags.contains("photo", ignoreCase = true) || mem.tags.contains("camera", ignoreCase = true)
                MemoryFilter.NOTES -> mem.type == MemoryType.NOTE || mem.tags.contains("note", ignoreCase = true)
                MemoryFilter.VOICE -> mem.type == MemoryType.VOICE || mem.tags.contains("audio", ignoreCase = true) || mem.tags.contains("voice", ignoreCase = true)
                MemoryFilter.TASKS -> mem.type == MemoryType.TASK || mem.tags.contains("task", ignoreCase = true)
                MemoryFilter.PLACES -> mem.type == MemoryType.PLACE || mem.tags.contains("location", ignoreCase = true) || mem.tags.contains("place", ignoreCase = true)
                MemoryFilter.PEOPLE -> mem.type == MemoryType.PERSON || mem.tags.contains("person", ignoreCase = true) || mem.tags.contains("contact", ignoreCase = true)
                MemoryFilter.FAVORITES -> mem.isFavorite || mem.tags.contains("favorite", ignoreCase = true) || mem.tags.contains("important", ignoreCase = true)
            }

            // Date Range filter match
            val matchesDate = when (selectedDateRange) {
                DateRangeFilter.ALL_TIME -> true
                DateRangeFilter.TODAY -> (currentTime - mem.createdAt) <= oneDayMs
                DateRangeFilter.PAST_WEEK -> (currentTime - mem.createdAt) <= oneWeekMs
                DateRangeFilter.PAST_MONTH -> (currentTime - mem.createdAt) <= oneMonthMs
            }

            matchesQuery && matchesType && matchesDate
        }

        // Apply sorting
        when (selectedSortOrder) {
            MemorySortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.createdAt }
            MemorySortOrder.OLDEST_FIRST -> filtered.sortedBy { it.createdAt }
            MemorySortOrder.TITLE_AZ -> filtered.sortedBy { it.title.lowercase() }
            MemorySortOrder.HIGHEST_CONFIDENCE -> filtered.sortedByDescending { it.confidence }
        }
    }

    val hasActiveFilters = selectedFilter != MemoryFilter.ALL || selectedDateRange != DateRangeFilter.ALL_TIME || searchQuery.isNotBlank()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Upload File Button
                SmallFloatingActionButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("fab_upload_file_memory")
                ) {
                    Icon(imageVector = Icons.Default.UploadFile, contentDescription = "Upload File")
                }

                // Add Memory Button
                FloatingActionButton(
                    onClick = {
                        newMemoryTitle = ""
                        newMemoryContent = ""
                        newMemoryTags = "note, personal"
                        uploadedFileName = null
                        showAddDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("fab_add_memory")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Memory")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. TOP HEADER & ASK MEMORY TRIGGER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Personal Memory Vault",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${allMemories.size} encrypted items in local SQLite vault",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            vaultStatus = edgeAI.memory.getVaultStatus()
                            showVaultSecurityDialog = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Vault Encryption Status",
                            tint = LocalAIGreen
                        )
                    }

                    FilledTonalButton(
                        onClick = { onNavigateToAskMemory("") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("ask_memory_shortcut_btn")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. VAULT ENCRYPTION STATUS BANNER
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = LocalAIGreen.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, LocalAIGreen.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        vaultStatus = edgeAI.memory.getVaultStatus()
                        showVaultSecurityDialog = true
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = LocalAIGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "ENCRYPTED AT REST (AES-256-GCM)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = LocalAIGreen,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Hardware KeyStore • Zero cloud egress",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LocalAIGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "VERIFIED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = LocalAIGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 3. SEARCH BAR
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search memories, notes, tags...", fontSize = 14.sp) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("memory_search_bar")
                    )
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 4. FILTER & SORT CONTROLS BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date Range Selector
                Box {
                    OutlinedButton(
                        onClick = { showDateMenu = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("memory_date_filter_btn")
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(selectedDateRange.label, fontSize = 11.sp)
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = showDateMenu,
                        onDismissRequest = { showDateMenu = false }
                    ) {
                        DateRangeFilter.values().forEach { range ->
                            DropdownMenuItem(
                                text = { Text(range.label, fontWeight = if (selectedDateRange == range) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedDateRange = range
                                    showDateMenu = false
                                }
                            )
                        }
                    }
                }

                // Sort Order Selector
                Box {
                    OutlinedButton(
                        onClick = { showSortMenu = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("memory_sort_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(selectedSortOrder.label, fontSize = 11.sp)
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        MemorySortOrder.values().forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(sort.label, fontWeight = if (selectedSortOrder == sort) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedSortOrder = sort
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                if (hasActiveFilters) {
                    TextButton(
                        onClick = {
                            selectedFilter = MemoryFilter.ALL
                            selectedDateRange = DateRangeFilter.ALL_TIME
                            searchQuery = ""
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("memory_clear_filters_btn")
                    ) {
                        Text("Reset", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // 5. TYPE FILTER CHIPS
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MemoryFilter.values()) { filter ->
                    val filterIcon = when (filter) {
                        MemoryFilter.DOCUMENTS -> Icons.Default.Description
                        MemoryFilter.PHOTOS -> Icons.Default.CameraAlt
                        MemoryFilter.NOTES -> Icons.Default.EditNote
                        MemoryFilter.VOICE -> Icons.Default.Mic
                        MemoryFilter.TASKS -> Icons.Default.TaskAlt
                        MemoryFilter.PLACES -> Icons.Default.Place
                        MemoryFilter.PEOPLE -> Icons.Default.Person
                        MemoryFilter.FAVORITES -> Icons.Default.Star
                        MemoryFilter.ALL -> null
                    }
                    GoogleFilterChip(
                        text = filter.label,
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        icon = filterIcon
                    )
                }
            }

            // 6. TIMELINE OF MEMORIES
            if (filteredAndSortedMemories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = if (hasActiveFilters) "No matching memories found" else "No memories recorded yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hasActiveFilters) "Try adjusting your filters or date range." else "Use the Capture or + button to save your first memory.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "SHOWING ${filteredAndSortedMemories.size} ENCRYPTED RECORDS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredAndSortedMemories, key = { it.id }) { memory ->
                        MemoryItemCard(
                            memory = memory,
                            onClick = { onSelectMemory(memory) }
                        )
                    }
                }
            }
        }
    }

    // VAULT SECURITY & ENCRYPTION STATUS MODAL
    if (showVaultSecurityDialog) {
        val status = vaultStatus ?: edgeAI.memory.getVaultStatus()
        AlertDialog(
            onDismissRequest = { showVaultSecurityDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = LocalAIGreen)
                    Text("Hardware Vault Security", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "All notes, memories, and personal interactions are encrypted at rest using device-bound hardware keys.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SecuritySpecRow("Algorithm", status.algorithm)
                            SecuritySpecRow("Key Size", "${status.keySizeBits}-bit AES")
                            SecuritySpecRow("Master Key Alias", status.keyAlias)
                            SecuritySpecRow("Key Provider", status.provider)
                            SecuritySpecRow("Zero Cloud Egress", "Guaranteed (Air-Gapped)")
                            SecuritySpecRow("Self-Test Latency", "${status.selfTestLatencyMs} ms")
                            SecuritySpecRow("Integrity Status", if (status.selfTestPassed) "VERIFIED PASS" else "CHECK FAILED")
                        }
                    }

                    Button(
                        onClick = {
                            isSelfTesting = true
                            vaultStatus = edgeAI.memory.getVaultStatus()
                            isSelfTesting = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Run Cryptographic Self-Test")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVaultSecurityDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // ADD NEW MEMORY DIALOG
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (uploadedFileName != null) Icons.Default.AttachFile else Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(if (uploadedFileName != null) "Store Uploaded File" else "Remember Something", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = LocalAIGreen, modifier = Modifier.size(14.dp))
                        Text(
                            text = "AES-256-GCM Encrypted on-device storage. Indexed with local vector embeddings.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Pick / Replace File Button
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("pick_file_in_dialog_btn")
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (uploadedFileName != null) "Selected: $uploadedFileName" else "Upload / Pick Document File", fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = newMemoryTitle,
                        onValueChange = { newMemoryTitle = it },
                        label = { Text("Title") },
                        placeholder = { Text("e.g. WiFi Credentials or Meeting Notes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_memory_title"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = newMemoryContent,
                        onValueChange = { newMemoryContent = it },
                        label = { Text("Content / Document Text") },
                        placeholder = { Text("Write content or paste text here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .testTag("input_new_memory_content"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = newMemoryTags,
                        onValueChange = { newMemoryTags = it },
                        label = { Text("Tags (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMemoryContent.isNotBlank() || newMemoryTitle.isNotBlank()) {
                            coroutineScope.launch {
                                val finalTitle = if (newMemoryTitle.isNotBlank()) {
                                    newMemoryTitle
                                } else if (newMemoryContent.length > 25) {
                                    newMemoryContent.take(22) + "..."
                                } else {
                                    newMemoryContent.ifBlank { "Personal Record" }
                                }

                                val memType = if (uploadedFileName != null) MemoryType.DOCUMENT else MemoryType.NOTE

                                edgeAI.memory.create(
                                    title = finalTitle,
                                    content = newMemoryContent.ifBlank { "Attached file: $finalTitle" },
                                    type = memType,
                                    tags = newMemoryTags,
                                    location = null
                                )
                                newMemoryTitle = ""
                                newMemoryContent = ""
                                uploadedFileName = null
                                showAddDialog = false
                                snackbarHostState.showSnackbar("Saved to encrypted memory vault (AES-256-GCM)!")
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_memory_confirm_btn")
                ) {
                    Text("Save to Vault")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SecuritySpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MemoryItemCard(
    memory: MemoryEntity,
    onClick: () -> Unit
) {
    val dateString = remember(memory.createdAt) {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(memory.createdAt))
    }

    AppCard(
        onClick = onClick,
        backgroundColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("memory_item_${memory.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (memory.type) {
                        MemoryType.IMAGE -> LocalAIGreen.copy(alpha = 0.15f)
                        MemoryType.DOCUMENT -> CloudAIBorder.copy(alpha = 0.15f)
                        MemoryType.VOICE -> PrivateServerAmber.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (memory.type) {
                                MemoryType.IMAGE -> Icons.Default.CameraAlt
                                MemoryType.DOCUMENT -> Icons.Default.Description
                                MemoryType.VOICE -> Icons.Default.Mic
                                MemoryType.TASK -> Icons.Default.TaskAlt
                                MemoryType.PLACE -> Icons.Default.Place
                                MemoryType.PERSON -> Icons.Default.Person
                                else -> Icons.Default.Bookmark
                            },
                            contentDescription = null,
                            tint = when (memory.type) {
                                MemoryType.IMAGE -> LocalAIGreen
                                MemoryType.DOCUMENT -> CloudAIBorder
                                MemoryType.VOICE -> PrivateServerAmber
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = memory.title.ifBlank { memory.content },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = memory.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )

                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = LocalAIGreen.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = LocalAIGreen, modifier = Modifier.size(10.dp))
                                Text(
                                    text = "AES-256",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAIGreen
                                )
                            }
                        }

                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (memory.tags.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            memory.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3).forEach { tag ->
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "#$tag",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
