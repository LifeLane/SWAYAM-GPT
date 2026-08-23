package com.example.edgeaicore.ui.voice

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.memory.MemoryType
import com.example.edgeaicore.ui.common.AppCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class Utterance(
    val speaker: String,
    val text: String,
    val timestamp: String,
    val confidence: Float = 0.96f
)

data class AudioJournalEntry(
    val id: String,
    val title: String,
    val durationSeconds: Int,
    val createdAt: Long,
    val summary: String,
    val actionItems: List<String>,
    val utterances: List<Utterance>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioJournalScreen(
    edgeAI: EdgeAICore,
    onBack: () -> Unit,
    onNavigateToAsk: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    var isTranscribing by remember { mutableStateOf(false) }

    // Sample Recorded Audio Journals
    var audioEntries by remember {
        mutableStateOf(
            listOf(
                AudioJournalEntry(
                    id = "audio-1",
                    title = "Strategy Meeting & Edge Model Deployment",
                    durationSeconds = 48,
                    createdAt = System.currentTimeMillis() - 3600000L * 2,
                    summary = "Discussed migrating LiteRT delegates to INT4 quantization on Snapdragon NPU. Verified zero data egress security protocol.",
                    actionItems = listOf("Benchmark TTFT on NPU", "Update MCP ToolGateway schema", "Release APK build v1.2"),
                    utterances = listOf(
                        Utterance(speaker = "Speaker 1 (You)", text = "Let's review the on-device inference speed for the new 2B parameter model.", timestamp = "0:04"),
                        Utterance(speaker = "Speaker 2 (Alex)", text = "We tested INT4 quantization and achieved 44 tokens per second with 1.2GB RAM footprint.", timestamp = "0:18"),
                        Utterance(speaker = "Speaker 1 (You)", text = "Perfect, let's keep all memory embeddings 100% offline in SQLite.", timestamp = "0:36")
                    )
                ),
                AudioJournalEntry(
                    id = "audio-2",
                    title = "Morning Voice Thought: Architecture Ideas",
                    durationSeconds = 22,
                    createdAt = System.currentTimeMillis() - 86400000L,
                    summary = "Reflections on building proactive autonomous routines for morning briefings and battery-aware vector indexing.",
                    actionItems = listOf("Implement wake-up morning briefing pipeline", "Add battery level trigger"),
                    utterances = listOf(
                        Utterance(speaker = "Speaker 1 (You)", text = "Idea: What if the edge agent compiles a daily morning brief automatically while the phone is charging overnight?", timestamp = "0:02")
                    )
                )
            )
        )
    }

    // Recording Timer Loop
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0
            while (isRecording) {
                delay(1000)
                recordingDuration += 1
            }
        }
    }

    fun stopRecordingAndTranscribe() {
        isRecording = false
        isTranscribing = true

        coroutineScope.launch {
            delay(1200) // Pure on-device STT & Diarization simulation
            val newEntry = AudioJournalEntry(
                id = "audio-${System.currentTimeMillis()}",
                title = "Voice Memo #${audioEntries.size + 1}",
                durationSeconds = maxOf(3, recordingDuration),
                createdAt = System.currentTimeMillis(),
                summary = "Live transcribed on-device voice memo. Synthesized into actionable memory item.",
                actionItems = listOf("Review voice memo details", "Follow up on recorded points"),
                utterances = listOf(
                    Utterance(speaker = "Speaker 1 (You)", text = "Captured on-device audio stream transcribed strictly with local neural STT model.", timestamp = "0:01")
                )
            )

            // Also save to Memory Vault
            edgeAI.memory.create(
                title = newEntry.title,
                content = newEntry.summary + "\nAction Items: " + newEntry.actionItems.joinToString(", "),
                tags = "voice, audio, transcription",
                location = null
            )

            audioEntries = listOf(newEntry) + audioEntries
            isTranscribing = false
            recordingDuration = 0
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Offline Audio Journal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("audio_journal_back_btn")) {
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
            // 1. LIVE RECORDER CARD
            item {
                AppCard(
                    backgroundColor = if (isRecording) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                    borderColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.testTag("audio_recorder_card")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRecording) "RECORDING LIVE AUDIO" else "ON-DEVICE SPEECH-TO-TEXT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = LocalAIGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "100% OFFLINE STT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalAIGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Animated Waveform Display
                        if (isRecording) {
                            LiveWaveformVisualizer()
                            Text(
                                text = String.format("%02d:%02d", recordingDuration / 60, recordingDuration % 60),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (isTranscribing) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Text(
                                text = "Transcribing & Running Diarization on-device...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "Speak freely. Your microphone stream is processed locally with zero cloud transmission.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        // Record / Stop Button
                        Button(
                            onClick = {
                                if (isRecording) stopRecordingAndTranscribe()
                                else isRecording = true
                            },
                            enabled = !isTranscribing,
                            colors = if (isRecording) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_toggle_recording")
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRecording) "Stop & Save to Memory" else "Start Voice Recording",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. JOURNAL ENTRIES LIST
            item {
                Text(
                    text = "RECORDED JOURNALS & DIARIZATIONS (${audioEntries.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            items(audioEntries, key = { it.id }) { entry ->
                AudioJournalCard(
                    entry = entry,
                    onAskAboutMemo = {
                        onNavigateToAsk("Summarize key action items from voice memo '${entry.title}'")
                    }
                )
            }
        }
    }
}

@Composable
private fun LiveWaveformVisualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val anim1 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 48f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val anim2 by infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val anim3 by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 54f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar3"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val heights = listOf(anim1, anim2, anim3, anim1 * 0.8f, anim2 * 1.2f, anim3 * 0.6f, anim1 * 1.1f, anim2 * 0.9f)
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.error)
            )
        }
    }
}

@Composable
private fun AudioJournalCard(
    entry: AudioJournalEntry,
    onAskAboutMemo: () -> Unit
) {
    val dateStr = remember(entry.createdAt) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(entry.createdAt))
    }

    AppCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("audio_card_${entry.id}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(text = "$dateStr • ${entry.durationSeconds}s duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(
                    onClick = onAskAboutMemo,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Digest", fontSize = 11.sp)
                }
            }

            // Summary
            Text(text = entry.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)

            // Action Items
            if (entry.actionItems.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "EXTRACTED ACTION ITEMS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        entry.actionItems.forEach { item ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = LocalAIGreen, modifier = Modifier.size(14.dp))
                                Text(text = item, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // Speaker Diarization Snippets
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "SPEAKER DIARIZATION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                entry.utterances.forEach { u ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = u.speaker,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (u.speaker.contains("You")) MaterialTheme.colorScheme.primary else PrivateServerAmber,
                            modifier = Modifier.width(110.dp)
                        )
                        Text(text = u.text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
