package com.example.edgeaicore.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.edgeaicore.core.billing.SubscriptionTier
import com.example.edgeaicore.ui.common.AppCard
import com.example.edgeaicore.ui.common.GoogleRadioCard
import com.example.edgeaicore.ui.common.GoogleSectionHeader
import com.example.edgeaicore.ui.common.SwayamLogo
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    edgeAI: EdgeAICore,
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToModels: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onNavigateToServices: () -> Unit = {},
    onNavigateToDocumentIntel: () -> Unit = {},
    onNavigateToBenchmark: () -> Unit = {},
    onNavigateToAudioJournal: () -> Unit = {},
    onNavigateToRoutines: () -> Unit = {},
    onNavigateToToolPlayground: () -> Unit = {},
    onOpenDeveloperModal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isDemoMode by edgeAI.demo.isDemoMode.collectAsStateWithLifecycle()
    val currentTier by edgeAI.billing.currentTier.collectAsStateWithLifecycle()
    val specs = remember { edgeAI.diagnostics.specs() }
    val currentThemeMode = LocalThemeMode.current
    val updateThemeMode = LocalThemeUpdater.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. PROFILE HEADER
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Local User Vault", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LocalAIGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "TIER: ${currentTier.name}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalAIGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = currentThemeMode.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. THEME & DISPLAY PREFERENCES (Google Light / Googly Dark / System)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GoogleSectionHeader(
                    title = "Theme & Appearance",
                    subtitle = "Switch between signature Google Light and Googly Dark"
                )

                GoogleRadioCard(
                    selected = currentThemeMode == AppThemeMode.LIGHT,
                    onClick = { updateThemeMode(AppThemeMode.LIGHT) },
                    title = "Google Light Theme (Default)",
                    subtitle = "Clean #F8F9FA canvas, high-contrast typography & Google Blue accents",
                    icon = Icons.Default.LightMode,
                    badgeText = "Default",
                    badgeColor = GoogleBlue,
                    modifier = Modifier.testTag("theme_radio_light")
                )

                GoogleRadioCard(
                    selected = currentThemeMode == AppThemeMode.GOOGLY_DARK,
                    onClick = { updateThemeMode(AppThemeMode.GOOGLY_DARK) },
                    title = "Googly Dark Theme",
                    subtitle = "Authentic Google dark surfaces (#1F1F1F, #28292A, #8AB4F8)",
                    icon = Icons.Default.DarkMode,
                    badgeText = "Dark",
                    badgeColor = GoogleBlueDark,
                    modifier = Modifier.testTag("theme_radio_dark")
                )

                GoogleRadioCard(
                    selected = currentThemeMode == AppThemeMode.SYSTEM,
                    onClick = { updateThemeMode(AppThemeMode.SYSTEM) },
                    title = "System Default",
                    subtitle = "Automatically follows Android system dark/light mode",
                    icon = Icons.Default.SettingsBrightness,
                    modifier = Modifier.testTag("theme_radio_system")
                )
            }
        }

        // 3. SUBSCRIPTION & CAPABILITIES
        item {
            AppCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "EDITION & CAPABILITIES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    TierBenefitRow("On-Device LLM Reasoning", "Included (Lifetime)")
                    TierBenefitRow("Semantic Memory Embeddings", "Unlimited Local")
                    TierBenefitRow("Agent Multi-Tool Orchestrator", "Active")
                    TierBenefitRow("Self-Hosted Private AI Sync", if (currentTier == SubscriptionTier.FREE) "Upgrade to Pro" else "Enabled")
                }
            }
        }

        // 4. SYSTEM & HARDWARE ACCELERATION
        item {
            AppCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "HARDWARE TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    TierBenefitRow("Device Model", "${specs.manufacturer} ${specs.model}")
                    TierBenefitRow("NPU / GPU Acceleration", specs.recommendedBackend.name)
                    TierBenefitRow("Available RAM", "${specs.totalRamMb} MB")
                }
            }
        }

        // 5. DEMO SIMULATION MODE
        item {
            AppCard(backgroundColor = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Demo Simulation Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Injects sample multimodal perception memories and agent scenarios without hardware dependencies.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDemoMode,
                        onCheckedChange = { edgeAI.demo.toggleDemoMode() },
                        modifier = Modifier.testTag("switch_demo_mode")
                    )
                }
            }
        }


        // 5. NAVIGATION LINKS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SETTINGS & SYSTEM HUBS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                NavigationHubItem(
                    title = "Document Intelligence & RAG Vault",
                    subtitle = "Ingest PDFs, notes & semantic citation lookup",
                    icon = Icons.Default.MenuBook,
                    onClick = onNavigateToDocumentIntel
                )
                NavigationHubItem(
                    title = "Hardware AI Benchmarking",
                    subtitle = "Stress test NPU/GPU/CPU decode throughput",
                    icon = Icons.Default.Speed,
                    onClick = onNavigateToBenchmark
                )
                NavigationHubItem(
                    title = "Offline Speech & Audio Journal",
                    subtitle = "Voice transcription & speaker diarization",
                    icon = Icons.Default.Mic,
                    onClick = onNavigateToAudioJournal
                )
                NavigationHubItem(
                    title = "Autonomous Routines & Triggers",
                    subtitle = "Morning briefs, geofencing & eco maintenance",
                    icon = Icons.Default.Schedule,
                    onClick = onNavigateToRoutines
                )
                NavigationHubItem(
                    title = "MCP & Tool Playground",
                    subtitle = "Interactive JSON-RPC payload simulator",
                    icon = Icons.Default.Terminal,
                    onClick = onNavigateToToolPlayground
                )
                NavigationHubItem(
                    title = "Privacy & Safety Center",
                    subtitle = "Telemetry, cloud rules, and audit logs",
                    icon = Icons.Default.Shield,
                    onClick = onNavigateToPrivacy
                )
                NavigationHubItem(
                    title = "Model Management",
                    subtitle = "LiteRT, LiteRT-LM & MediaPipe neural files",
                    icon = Icons.Default.Memory,
                    onClick = onNavigateToModels
                )
                NavigationHubItem(
                    title = "Data & Storage Center",
                    subtitle = "Encrypted SQLite vault, backups, and exports",
                    icon = Icons.Default.Storage,
                    onClick = onNavigateToStorage
                )
                NavigationHubItem(
                    title = "Connected Services & MCP",
                    subtitle = "Model Context Protocol & Private AI server",
                    icon = Icons.Default.Hub,
                    onClick = onNavigateToServices
                )
                NavigationHubItem(
                    title = "Developer Diagnostics Hub",
                    subtitle = "Real-time engine telemetry, logs & benchmarks",
                    icon = Icons.Default.Terminal,
                    onClick = onOpenDeveloperModal
                )
            }
        }

        // 5. SWAYAM GPT ANCIENT INDIA THEMED IDENTITY CARD
        item {
            AppCard(
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = Color(0xFFFFB703).copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SwayamLogo(size = 56.dp)
                    Text(
                        text = "SWAYAM GPT v2.4.0",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFB703),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Sovereign On-Device AI Operating Core inspired by timeless Vedic intelligence and modern edge neural computing.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFB703).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "100% PRIVATE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB703),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "EDGE NEURAL CORE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TierBenefitRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NavigationHubItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
