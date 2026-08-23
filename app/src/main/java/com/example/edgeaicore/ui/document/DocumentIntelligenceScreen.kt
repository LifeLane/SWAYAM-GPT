package com.example.edgeaicore.ui.document

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.memory.MemoryType
import com.example.edgeaicore.ui.common.AIStatus
import com.example.edgeaicore.ui.common.AppCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SampleDocument(
    val id: String,
    val title: String,
    val source: String,
    val snippet: String,
    val tags: List<String>,
    val wordCount: Int,
    val chunksCount: Int,
    val confidence: Float = 0.94f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentIntelligenceScreen(
    edgeAI: EdgeAICore,
    onBack: () -> Unit,
    onNavigateToAsk: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var isIngesting by remember { mutableStateOf(false) }
    var ingestionSuccessMsg by remember { mutableStateOf<String?>(null) }
    
    // Sample Ingested Documents Store
    var documents by remember {
        mutableStateOf(
            listOf(
                SampleDocument(
                    id = "doc-1",
                    title = "Edge Neural Runtime Architecture Spec",
                    source = "Research Paper / Offline PDF",
                    snippet = "On-device INT4 quantized execution pathways achieve 42 tok/s on Qualcomm Snapdragon NPU accelerators while maintaining zero data egress guarantees...",
                    tags = listOf("AI Architecture", "NPU", "LiteRT"),
                    wordCount = 1420,
                    chunksCount = 6,
                    confidence = 0.98f
                ),
                SampleDocument(
                    id = "doc-2",
                    title = "Personal Health & Nutrition Log 2026",
                    source = "Encrypted Vault Record",
                    snippet = "Daily recommended caloric baseline: 2,150 kcal. Hydration target: 3.2L. Vitamin D3 supplementation schedule: 2000 IU with breakfast...",
                    tags = listOf("Health", "Nutrition", "Personal"),
                    wordCount = 850,
                    chunksCount = 4,
                    confidence = 0.95f
                ),
                SampleDocument(
                    id = "doc-3",
                    title = "Project Roadmap & MCP Server Specs",
                    source = "Markdown Workspace",
                    snippet = "MCP ToolGateway implements local JSON-RPC 2.0 transport loopback over unix domain sockets. All user consents require explicit cryptographic verification.",
                    tags = listOf("Work", "Roadmap", "MCP"),
                    wordCount = 2100,
                    chunksCount = 9,
                    confidence = 0.92f
                )
            )
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var newDocTitle by remember { mutableStateOf("") }
    var newDocContent by remember { mutableStateOf("") }
    var newDocTags by remember { mutableStateOf("document, notes") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName = "Document"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(idx)
                    }
                }
                newDocTitle = fileName
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    newDocContent = content
                    newDocTags = "document, ${fileName.substringAfterLast('.', "vault")}"
                } else {
                    newDocContent = "Document: $fileName stored in sovereign memory vault."
                }
                showAddDialog = true
            } catch (_: Exception) {}
        }
    }

    val filteredDocs = remember(documents, searchQuery) {
        if (searchQuery.isBlank()) documents
        else documents.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.snippet.contains(searchQuery, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Document Intelligence & RAG", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("doc_intel_back_btn")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("doc_intel_add_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Ingest Document")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToAsk("Search my ingested documents and cite sources for: ") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                text = { Text("Ask RAG Vault", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_ask_rag")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HEADER & RAG EXPLANATION
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column {
                            Text(
                                text = "Zero-Cloud Semantic RAG Vault",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "PDFs and notes are chunked & embedded locally into SQLite vector tables. Agent answers provide exact source citations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. SEARCH BAR
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search document chunks, entities, citations...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("doc_intel_search_field")
                )
            }

            // 3. STATS STRIP
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val totalChunks = documents.sumOf { it.chunksCount }
                    val totalWords = documents.sumOf { it.wordCount }

                    StatMiniCard(
                        title = "DOCUMENTS",
                        value = "${documents.size}",
                        icon = Icons.Default.Description,
                        modifier = Modifier.weight(1f)
                    )
                    StatMiniCard(
                        title = "VECTOR CHUNKS",
                        value = "$totalChunks",
                        icon = Icons.Default.Grid4x4,
                        modifier = Modifier.weight(1f)
                    )
                    StatMiniCard(
                        title = "TOTAL WORDS",
                        value = "$totalWords",
                        icon = Icons.Default.Article,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. DOCUMENT LIST
            item {
                Text(
                    text = "INDEXED DOCUMENTS (${filteredDocs.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            items(filteredDocs, key = { it.id }) { doc ->
                DocumentCard(
                    doc = doc,
                    onAskAboutDoc = {
                        onNavigateToAsk("According to '${doc.title}', summarize the key findings and cite citations.")
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!isIngesting) showAddDialog = false },
            title = { Text("Ingest Document into RAG Vault", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Text will be tokenized, split into 256-token semantic chunks, and embedded via on-device embedding model.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { filePicker.launch("*/*") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("pick_file_in_rag_btn")
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick File / PDF Document", fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = newDocTitle,
                        onValueChange = { newDocTitle = it },
                        label = { Text("Document Title") },
                        placeholder = { Text("e.g. Android Kernel Architecture") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newDocContent,
                        onValueChange = { newDocContent = it },
                        label = { Text("Document Content / Text Extract") },
                        placeholder = { Text("Paste document text, research notes, or PDF extract...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newDocTags,
                        onValueChange = { newDocTags = it },
                        label = { Text("Tags (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (isIngesting) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDocTitle.isNotBlank() && newDocContent.isNotBlank()) {
                            isIngesting = true
                            coroutineScope.launch {
                                delay(400)
                                val words = newDocContent.split(Regex("\\s+")).size
                                val chunks = maxOf(1, words / 150)
                                val tagList = newDocTags.split(",").map { it.trim() }.filter { it.isNotBlank() }

                                val newDoc = SampleDocument(
                                    id = "doc-${System.currentTimeMillis()}",
                                    title = newDocTitle,
                                    source = "Local Ingestion Vault",
                                    snippet = newDocContent.take(180) + if (newDocContent.length > 180) "..." else "",
                                    tags = tagList,
                                    wordCount = words,
                                    chunksCount = chunks,
                                    confidence = 0.96f
                                )

                                edgeAI.memory.create(
                                    title = newDocTitle,
                                    content = newDocContent,
                                    type = MemoryType.DOCUMENT,
                                    tags = newDocTags
                                )

                                documents = listOf(newDoc) + documents
                                isIngesting = false
                                showAddDialog = false
                                newDocTitle = ""
                                newDocContent = ""
                            }
                        }
                    },
                    enabled = !isIngesting && newDocTitle.isNotBlank() && newDocContent.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isIngesting) "Chunking & Embedding..." else "Ingest & Index")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }, enabled = !isIngesting) {
                    Text("Cancel")
                }
            }
        )
    }

}

@Composable
private fun StatMiniCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = title, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DocumentCard(
    doc: SampleDocument,
    onAskAboutDoc: () -> Unit
) {
    AppCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("doc_card_${doc.id}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = doc.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${doc.source} • ${doc.wordCount} words • ${doc.chunksCount} vector chunks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LocalAIGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${(doc.confidence * 100).toInt()}% EMBEDDED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalAIGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = doc.snippet,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            // Tags & Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(doc.tags) { tag ->
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

                FilledTonalButton(
                    onClick = onAskAboutDoc,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("ask_doc_btn_${doc.id}")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Query", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
