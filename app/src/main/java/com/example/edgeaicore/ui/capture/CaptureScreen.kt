package com.example.edgeaicore.ui.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.edgeaicore.EdgeAICore
import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.mediapipe.VisionResult
import com.example.edgeaicore.core.memory.MemoryType
import com.example.edgeaicore.ui.common.AppCard
import com.example.edgeaicore.ui.common.CameraXView
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class CaptureMode(val label: String) {
    DOCUMENT("Document / OCR"),
    SCENE("Scene / Object"),
    POSE("Pose / Posture"),
    HAND("Hand Gesture"),
    FACE("Face Landmark")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    edgeAI: EdgeAICore,
    onNavigateBack: () -> Unit = {},
    onSavedToMemory: () -> Unit = {},
    onBack: () -> Unit = onNavigateBack,
    onMemorySaved: () -> Unit = onSavedToMemory,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedMode by remember { mutableStateOf(CaptureMode.DOCUMENT) }
    var isProcessing by remember { mutableStateOf(false) }
    var isLiveStreamActive by remember { mutableStateOf(true) }
    var visionResult by remember { mutableStateOf<VisionResult?>(null) }
    var capturedNote by remember { mutableStateOf("") }
    var activeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSavedSnackbar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val defaultSampleBitmap = remember {
        val bmp = Bitmap.createBitmap(480, 360, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply { color = android.graphics.Color.rgb(32, 40, 56) }
        canvas.drawRect(0f, 0f, 480f, 360f, paint)
        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            isAntiAlias = true
        }
        canvas.drawText("SWAYAM Sovereign OCR & Vision", 40f, 180f, textPaint)
        bmp
    }

    // Photo Capture Launcher (Real Camera)
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            activeBitmap = bitmap
            isProcessing = true
            coroutineScope.launch {
                val result = edgeAI.vision.detect(bitmap, selectedMode.name)
                visionResult = result
                capturedNote = result.toCompactSummary()
                isProcessing = false
            }
        }
    }

    // Gallery / Document Image Picker Launcher
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        activeBitmap = bitmap
                        isProcessing = true
                        coroutineScope.launch {
                            val result = edgeAI.vision.detect(bitmap, selectedMode.name)
                            visionResult = result
                            capturedNote = result.toCompactSummary()
                            isProcessing = false
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Capture & OCR Vision", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Live CameraX • On-Device OCR", style = MaterialTheme.typography.labelSmall, color = LocalAIGreen)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { pickImageLauncher.launch("image/*") },
                        modifier = Modifier.testTag("capture_gallery_top_btn")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Import Photo")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Selectors
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(CaptureMode.values()) { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = {
                            selectedMode = mode
                            activeBitmap?.let { bmp ->
                                isProcessing = true
                                coroutineScope.launch {
                                    val result = edgeAI.vision.detect(bmp, mode.name)
                                    visionResult = result
                                    capturedNote = result.toCompactSummary()
                                    isProcessing = false
                                }
                            }
                        },
                        label = { Text(mode.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Live CameraX Viewfinder or Frozen Snapshot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                val bmp = activeBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Frozen Frame",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Re-enable live stream button
                    FilledTonalButton(
                        onClick = {
                            activeBitmap = null
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Resume Live Camera", fontSize = 12.sp)
                    }
                } else {
                    CameraXView(
                        modifier = Modifier.fillMaxSize(),
                        targetFps = 8,
                        isStreamActive = isLiveStreamActive,
                        latestVisionResult = visionResult,
                        onFrameCaptured = { frameBmp ->
                            if (isLiveStreamActive && !isProcessing) {
                                isProcessing = true
                                coroutineScope.launch {
                                    val result = edgeAI.vision.detect(frameBmp, selectedMode.name)
                                    visionResult = result
                                    capturedNote = result.toCompactSummary()
                                    isProcessing = false
                                }
                            }
                        },
                        onManualSnapshot = { snapshotBmp ->
                            activeBitmap = snapshotBmp
                            isProcessing = true
                            coroutineScope.launch {
                                val result = edgeAI.vision.detect(snapshotBmp, selectedMode.name)
                                visionResult = result
                                capturedNote = result.toCompactSummary()
                                isProcessing = false
                            }
                        },
                        onToggleStream = {
                            isLiveStreamActive = !isLiveStreamActive
                        }
                    )
                }
            }

            // Analysis & Extracted OCR Information Card
            if (visionResult != null || capturedNote.isNotBlank()) {
                AppCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = LocalAIGreen, modifier = Modifier.size(18.dp))
                                Text(
                                    if (selectedMode == CaptureMode.DOCUMENT) "Extracted OCR Transcription" else "Vision Analysis",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${visionResult?.processingTimeMs ?: 12} ms • On-Device",
                                style = MaterialTheme.typography.labelSmall,
                                color = LocalAIGreen
                            )
                        }

                        OutlinedTextField(
                            value = capturedNote,
                            onValueChange = { capturedNote = it },
                            label = { Text("Extracted Content") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Action Buttons: Save to Memory
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val memType = if (selectedMode == CaptureMode.DOCUMENT) MemoryType.DOCUMENT else MemoryType.IMAGE
                                        val memTitle = if (selectedMode == CaptureMode.DOCUMENT) "OCR Document Extract" else "Captured ${selectedMode.label}"
                                        edgeAI.memory.create(
                                            title = memTitle,
                                            content = capturedNote,
                                            type = memType,
                                            tags = "capture, ocr, ${selectedMode.name.lowercase()}"
                                        )
                                        snackbarHostState.showSnackbar("Saved to personal encrypted memory!")
                                        onSavedToMemory()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_capture_to_memory_btn")
                            ) {
                                Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save to Personal Memory Vault")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

