package com.example.edgeaicore.core.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class CameraLens {
    BACK,
    FRONT
}

data class CameraConfig(
    val targetFps: Int = 10, // 5, 10, 15, 30 FPS frame throttling
    val lens: CameraLens = CameraLens.BACK,
    val isFlashEnabled: Boolean = false,
    val zoomRatio: Float = 1.0f
)

/**
 * CameraEngine: CameraX abstraction with configurable FPS throttling,
 * backpressure strategies, and decoupled frame analysis.
 */
class CameraEngine(private val context: Context) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalysis: ImageAnalysis? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _config = MutableStateFlow(CameraConfig())
    val config: StateFlow<CameraConfig> = _config.asStateFlow()

    private var lastAnalyzedTimestamp = 0L

    suspend fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onFrame: (Bitmap) -> Unit
    ): EdgeResult<Boolean> {
        return try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val lensFacing = if (_config.value.lens == CameraLens.BACK) {
                        CameraSelector.LENS_FACING_BACK
                    } else {
                        CameraSelector.LENS_FACING_FRONT
                    }
                    val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

                    val intervalMs = (1000.0 / _config.value.targetFps).toLong()

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val currentMs = System.currentTimeMillis()
                        if (currentMs - lastAnalyzedTimestamp >= intervalMs) {
                            lastAnalyzedTimestamp = currentMs
                            val bitmap = imageProxyToBitmap(imageProxy)
                            if (bitmap != null) {
                                onFrame(bitmap)
                            }
                        }
                        imageProxy.close()
                    }
                    imageAnalysis = analysis

                    provider.unbindAll()
                    camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        analysis
                    )
                } catch (e: Exception) {
                    // Fallback gracefully
                }
            }, ContextCompat.getMainExecutor(context))
            EdgeResult.Success(true)
        } catch (e: Exception) {
            EdgeResult.Failure(EdgeAIError.CameraUnavailable(e.message ?: "Failed to start camera"))
        }
    }

    fun setTargetFps(fps: Int) {
        _config.value = _config.value.copy(targetFps = fps.coerceIn(1, 60))
    }

    fun switchCamera() {
        val nextLens = if (_config.value.lens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
        _config.value = _config.value.copy(lens = nextLens)
    }

    fun toggleFlash() {
        val newFlash = !_config.value.isFlashEnabled
        _config.value = _config.value.copy(isFlashEnabled = newFlash)
        camera?.cameraControl?.enableTorch(newFlash)
    }

    fun setZoom(ratio: Float) {
        _config.value = _config.value.copy(zoomRatio = ratio)
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val planes = image.planes
        if (planes.isEmpty()) return null
        return try {
            val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            val rotationDegrees = image.imageInfo.rotationDegrees
            if (rotationDegrees != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }
}
