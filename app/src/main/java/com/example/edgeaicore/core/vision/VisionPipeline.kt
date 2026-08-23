package com.example.edgeaicore.core.vision

import android.content.Context
import android.graphics.Bitmap
import com.example.edgeaicore.core.litert.LiteRTEngine
import com.example.edgeaicore.core.mediapipe.MediaPipeEngine
import com.example.edgeaicore.core.mediapipe.VisionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Real-Time Vision Pipeline:
 * Transforms raw camera streams into structured perception representations (VisionResult)
 * through MediaPipe & LiteRT perception models, shielding LLMs from heavy raw frame arrays
 * and enforcing strict privacy boundaries.
 */
class VisionPipeline(
    private val context: Context,
    private val mediaPipeEngine: MediaPipeEngine,
    private val liteRTEngine: LiteRTEngine
) {
    private val _latestResult = MutableStateFlow(VisionResult())
    val latestResult: StateFlow<VisionResult> = _latestResult.asStateFlow()

    private var isPipelineActive = false

    suspend fun startPipeline(): Boolean = withContext(Dispatchers.IO) {
        mediaPipeEngine.loadObjectDetector()
        mediaPipeEngine.loadFaceLandmarker()
        mediaPipeEngine.loadHandLandmarker()
        mediaPipeEngine.loadPoseLandmarker()
        isPipelineActive = true
        true
    }

    suspend fun processFrame(bitmap: Bitmap, mode: String = "SCENE"): VisionResult = withContext(Dispatchers.Default) {
        if (!isPipelineActive) {
            startPipeline()
        }
        val result = mediaPipeEngine.processFrame(bitmap, mode)
        _latestResult.value = result
        result
    }

    suspend fun stopPipeline() = withContext(Dispatchers.IO) {
        isPipelineActive = false
        mediaPipeEngine.unloadAll()
    }
}
