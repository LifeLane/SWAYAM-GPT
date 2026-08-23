package com.example.edgeaicore.ui.models

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.common.ExecutionBackend
import com.example.edgeaicore.core.models.EdgeModel
import com.example.edgeaicore.core.models.ModelCapability
import com.example.edgeaicore.core.models.ModelStatus
import com.example.edgeaicore.core.models.ModelType
import com.example.edgeaicore.ui.common.AppCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ModelCategoryFilter(val label: String) {
    ALL("All Models"),
    LLM("Local LLMs"),
    VISION("Vision & Perception"),
    EMBEDDINGS("Embeddings"),
    INSTALLED("Installed Only")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelCenterScreen(
    edgeAI: EdgeAICore,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val modelsList by edgeAI.models.list.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ModelCategoryFilter.ALL) }
    
    // Per-model installation progress mapping
    var installingModelId by remember { mutableStateOf<String?>(null) }
    var installProgress by remember { mutableStateOf(0f) }
    var actionNotification by remember { mutableStateOf<String?>(null) }

    val filteredModels = remember(modelsList, searchQuery, selectedFilter) {
        modelsList.filter { model ->
            val matchesSearch = searchQuery.isBlank() ||
                    model.name.contains(searchQuery, ignoreCase = true) ||
                    model.capabilities.any { it.name.contains(searchQuery, ignoreCase = true) }
            val matchesCategory = when (selectedFilter) {
                ModelCategoryFilter.ALL -> true
                ModelCategoryFilter.LLM -> model.type == ModelType.LITERT_LM
                ModelCategoryFilter.VISION -> model.type == ModelType.LITERT_VISION || model.type == ModelType.MEDIAPIPE_TASK
                ModelCategoryFilter.EMBEDDINGS -> model.type == ModelType.EMBEDDING_VECTOR
                ModelCategoryFilter.INSTALLED -> model.isInstalled
            }
            matchesSearch && matchesCategory
        }
    }

    val totalInstalled = modelsList.count { it.isInstalled }
    val totalSizeMb = modelsList.filter { it.isInstalled }.sumOf { it.sizeMb }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Model Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("model_center_back_btn")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TOP METRICS HEADER
            item {
                AppCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("model_center_stats_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ON-DEVICE NEURAL REPOSITORY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "$totalInstalled Installed Models",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LocalAIGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${formatModelSize(totalSizeMb)} On-Disk",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAIGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ModelStatMetric(label = "Hardware Target", value = "GPU & NPU Accelerated")
                            ModelStatMetric(label = "Runtime Engine", value = "LiteRT + MediaPipe")
                            ModelStatMetric(label = "Privacy Guarantee", value = "100% Offline")
                        }
                    }
                }
            }

            // 2. SEARCH BAR
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search LLMs, vision, embeddings...", fontSize = 13.sp) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f).testTag("model_search_input")
                        )
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // 3. CATEGORY FILTERS
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ModelCategoryFilter.values()) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("model_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            // 4. MODEL LIST
            if (filteredModels.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                            Text("No models found matching criteria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                items(filteredModels, key = { it.id }) { model ->
                    val isCurrentlyInstalling = installingModelId == model.id
                    ModelItemCard(
                        model = model,
                        isInstalling = isCurrentlyInstalling,
                        installProgress = if (isCurrentlyInstalling) installProgress else model.downloadProgress,
                        onInstall = {
                            installingModelId = model.id
                            installProgress = 0.05f
                            coroutineScope.launch {
                                // Simulate smooth step-by-step progress
                                for (step in 1..10) {
                                    delay(120)
                                    installProgress = step / 10f
                                }
                                val result = edgeAI.models.install(model.id)
                                installingModelId = null
                                actionNotification = when (result) {
                                    is com.example.edgeaicore.core.common.EdgeResult.Success -> "Model '${model.name}' installed and ready for local inference."
                                    is com.example.edgeaicore.core.common.EdgeResult.Failure -> "Installation failed: ${result.error.message}"
                                }
                            }
                        },
                        onUninstall = {
                            edgeAI.models.remove(model.id)
                            actionNotification = "Removed '${model.name}' weights from storage."
                        },
                        onToggleEnabled = { enabled ->
                            edgeAI.models.setEnabled(model.id, enabled)
                        }
                    )
                }
            }
        }
    }

    if (actionNotification != null) {
        AlertDialog(
            onDismissRequest = { actionNotification = null },
            title = { Text("Model Manager", fontWeight = FontWeight.Bold) },
            text = { Text(actionNotification.orEmpty()) },
            confirmButton = {
                Button(onClick = { actionNotification = null }, modifier = Modifier.testTag("model_alert_ok_btn")) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ModelItemCard(
    model: EdgeModel,
    isInstalling: Boolean,
    installProgress: Float,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit
) {
    AppCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = if (model.isInstalled && model.isEnabled) LocalAIGreen.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.testTag("model_card_${model.id}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row
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
                        color = when (model.type) {
                            ModelType.LITERT_LM -> LocalAIGreen.copy(alpha = 0.15f)
                            ModelType.MEDIAPIPE_TASK -> CloudAIBorder.copy(alpha = 0.15f)
                            ModelType.LITERT_VISION -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ModelType.EMBEDDING_VECTOR -> PrivateServerAmber.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (model.type) {
                                    ModelType.LITERT_LM -> Icons.Default.Chat
                                    ModelType.MEDIAPIPE_TASK -> Icons.Default.Visibility
                                    ModelType.LITERT_VISION -> Icons.Default.CenterFocusWeak
                                    ModelType.EMBEDDING_VECTOR -> Icons.Default.Hub
                                },
                                contentDescription = null,
                                tint = when (model.type) {
                                    ModelType.LITERT_LM -> LocalAIGreen
                                    ModelType.MEDIAPIPE_TASK -> CloudAIBorder
                                    ModelType.LITERT_VISION -> MaterialTheme.colorScheme.primary
                                    ModelType.EMBEDDING_VECTOR -> PrivateServerAmber
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(text = model.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatModelSize(model.sizeMb),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "RAM: ${model.minimumRamMb} MB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = model.preferredBackend.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (model.isInstalled) {
                    Switch(
                        checked = model.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.testTag("switch_model_${model.id}")
                    )
                }
            }

            // Capabilities Tags
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                model.capabilities.take(4).forEach { cap ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = cap.name.lowercase().replace("_", " "),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Progress / Status / Action Buttons
            if (isInstalling) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { installProgress },
                        modifier = Modifier.fillMaxWidth().testTag("install_progress_${model.id}")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Verifying SHA-256 Checksum & Loading Weights...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${(installProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (model.isInstalled) LocalAIGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (model.isInstalled) "INSTALLED & READY" else "AVAILABLE TO DOWNLOAD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (model.isInstalled) LocalAIGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (model.isInstalled) {
                        TextButton(
                            onClick = onUninstall,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("uninstall_model_${model.id}")
                        ) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Uninstall", fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = onInstall,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("install_model_${model.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download & Install", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelStatMetric(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

private fun formatModelSize(mb: Double): String {
    return if (mb >= 1024.0) {
        String.format("%.2f GB", mb / 1024.0)
    } else {
        String.format("%.0f MB", mb)
    }
}
