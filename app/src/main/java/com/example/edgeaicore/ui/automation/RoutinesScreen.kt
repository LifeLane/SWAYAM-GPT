package com.example.edgeaicore.ui.automation

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.ui.common.AppCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class RoutineItem(
    val id: String,
    val title: String,
    val triggerDescription: String,
    val actionSummary: String,
    val icon: ImageVector,
    val isEnabled: Boolean,
    val requiresCharging: Boolean = false,
    val lastExecutedText: String = "Never"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    edgeAI: EdgeAICore,
    onBack: () -> Unit,
    onNavigateToAsk: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isExecutingRoutine by remember { mutableStateOf<String?>(null) }
    var executionSuccessMessage by remember { mutableStateOf<String?>(null) }

    var routines by remember {
        mutableStateOf(
            listOf(
                RoutineItem(
                    id = "routine-morning",
                    title = "Proactive Morning Executive Briefing",
                    triggerDescription = "Daily at 07:30 AM or upon device unlock after 6 AM",
                    actionSummary = "Synthesizes today's calendar events, pending memory tasks, and unread priority items into an audible & visual brief.",
                    icon = Icons.Default.WbSunny,
                    isEnabled = true,
                    lastExecutedText = "Today at 7:30 AM"
                ),
                RoutineItem(
                    id = "routine-office-geofence",
                    title = "Office Arrival Context Switch",
                    triggerDescription = "Geofence Entry: Work Office (Latitude/Longitude Radius 150m)",
                    actionSummary = "Switches Agent mode to Work Profile, silences non-urgent alerts, and highlights relevant project documents.",
                    icon = Icons.Default.LocationOn,
                    isEnabled = true,
                    lastExecutedText = "Yesterday at 8:45 AM"
                ),
                RoutineItem(
                    id = "routine-eco-maintenance",
                    title = "Overnight Vector Compaction & Maintenance",
                    triggerDescription = "Device Charging + Connected to Home Wi-Fi + Battery > 80%",
                    actionSummary = "Executes SQLite vacuum, updates semantic embeddings for newly captured notes, and verifies checksum integrity.",
                    icon = Icons.Default.BatteryChargingFull,
                    isEnabled = true,
                    requiresCharging = true,
                    lastExecutedText = "Today at 3:15 AM (Saved 24MB)"
                ),
                RoutineItem(
                    id = "routine-evening-recap",
                    title = "Evening Reflection & Memory Consolidation",
                    triggerDescription = "Daily at 09:30 PM",
                    actionSummary = "Prompts for a quick 30-second audio note and automatically organizes accomplishments into the Memory vault.",
                    icon = Icons.Default.NightsStay,
                    isEnabled = false,
                    lastExecutedText = "3 days ago"
                )
            )
        )
    }

    fun triggerRoutineNow(routine: RoutineItem) {
        isExecutingRoutine = routine.id
        coroutineScope.launch {
            delay(900) // Simulation of on-device autonomous execution
            isExecutingRoutine = null
            executionSuccessMessage = "Executed '${routine.title}' successfully on-device."
            routines = routines.map {
                if (it.id == routine.id) it.copy(lastExecutedText = "Just now") else it
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Autonomous Routines & Triggers", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("routines_back_btn")) {
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
            // 1. BANNER
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrivateServerAmber.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = PrivateServerAmber)
                            }
                        }
                        Column {
                            Text("Context-Aware Local Automation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Routines evaluate on-device triggers (time, battery state, geofence) with zero cloud polling.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 2. ROUTINES LIST
            item {
                Text(
                    text = "ACTIVE AUTOMATIONS (${routines.count { it.isEnabled }} OF ${routines.size} ENABLED)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            items(routines, key = { it.id }) { routine ->
                val isCurrentRunning = isExecutingRoutine == routine.id

                AppCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("routine_card_${routine.id}")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (routine.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = routine.icon,
                                            contentDescription = null,
                                            tint = if (routine.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(text = routine.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(text = "Last ran: ${routine.lastExecutedText}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Switch(
                                checked = routine.isEnabled,
                                onCheckedChange = { checked ->
                                    routines = routines.map { if (it.id == routine.id) it.copy(isEnabled = checked) else it }
                                },
                                modifier = Modifier.testTag("switch_routine_${routine.id}")
                            )
                        }

                        // Trigger & Action Details
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = PrivateServerAmber, modifier = Modifier.size(14.dp))
                                    Text(text = "TRIGGER: ${routine.triggerDescription}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Text(text = routine.actionSummary, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Run Now Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            FilledTonalButton(
                                onClick = { triggerRoutineNow(routine) },
                                enabled = !isCurrentRunning,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_run_routine_${routine.id}")
                            ) {
                                if (isCurrentRunning) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Routine", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (executionSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = { executionSuccessMessage = null },
            title = { Text("Automation Executed", fontWeight = FontWeight.Bold) },
            text = { Text(executionSuccessMessage.orEmpty()) },
            confirmButton = {
                Button(onClick = { executionSuccessMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}
