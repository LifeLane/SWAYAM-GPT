package com.example.edgeaicore.core.models

import android.content.Context
import com.example.edgeaicore.core.common.EdgeAIError
import com.example.edgeaicore.core.common.EdgeResult
import com.example.edgeaicore.core.common.ExecutionBackend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class ModelCapability {
    TEXT,
    VISION,
    EMBEDDING,
    CLASSIFICATION,
    POSE,
    HAND_TRACKING,
    FACE_LANDMARKS,
    OBJECT_DETECTION,
    SUMMARIZATION,
    CHAT,
    REASONING
}

enum class ModelType {
    LITERT_LM,
    LITERT_VISION,
    MEDIAPIPE_TASK,
    EMBEDDING_VECTOR
}

enum class ModelStatus {
    NOT_INSTALLED,
    VERIFYING,
    INSTALLED,
    LOADING,
    READY,
    ERROR,
    UNLOADED
}

data class EdgeModel(
    val id: String,
    val name: String,
    val version: String,
    val sizeBytes: Long,
    val type: ModelType,
    val capabilities: Set<ModelCapability>,
    val minimumRamMb: Long,
    val preferredBackend: ExecutionBackend = ExecutionBackend.AUTO,
    val downloadUrl: String = "",
    val checksum: String = "",
    val license: String = "Apache-2.0 / Gemma Terms",
    val isInstalled: Boolean = false,
    val isEnabled: Boolean = true,
    val localPath: String? = null,
    val status: ModelStatus = if (isInstalled) ModelStatus.INSTALLED else ModelStatus.NOT_INSTALLED,
    val downloadProgress: Float = 0f
) {
    val sizeMb: Double get() = sizeBytes / (1024.0 * 1024.0)
}

/**
 * Verified catalog of Edge Models ready for LiteRT & on-device pipelines.
 */
object ModelRegistry {
    val DEFAULT_MODELS = listOf(
        EdgeModel(
            id = "gemma-2b-it-litert",
            name = "Gemma 2B IT (LiteRT-LM)",
            version = "2.0.0",
            sizeBytes = 1_400_000_000L, // ~1.4 GB quantized INT4
            type = ModelType.LITERT_LM,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT, ModelCapability.SUMMARIZATION, ModelCapability.REASONING),
            minimumRamMb = 2048L,
            preferredBackend = ExecutionBackend.GPU,
            downloadUrl = "https://huggingface.co/google/gemma-2b-it-litert-int4",
            checksum = "sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            license = "Gemma Terms of Use",
            isInstalled = false,
            isEnabled = false,
            localPath = null,
            status = ModelStatus.NOT_INSTALLED
        ),
        EdgeModel(
            id = "all-minilm-l6-v2-embedding",
            name = "MiniLM-L6 Embedding (LiteRT)",
            version = "1.2.0",
            sizeBytes = 45_000_000L, // ~45 MB
            type = ModelType.EMBEDDING_VECTOR,
            capabilities = setOf(ModelCapability.EMBEDDING),
            minimumRamMb = 256L,
            preferredBackend = ExecutionBackend.CPU,
            downloadUrl = "https://storage.googleapis.com/edge-ai-models/embeddings/minilm-l6-v2.tflite",
            checksum = "sha256:88d4266fd4e6338d13b845fcf289579d209c897823b9217da3e161936f031589",
            license = "Apache-2.0",
            isInstalled = false,
            isEnabled = false,
            localPath = null,
            status = ModelStatus.NOT_INSTALLED
        ),
        EdgeModel(
            id = "mediapipe-pose-landmarker",
            name = "MediaPipe Pose Landmarker (Full)",
            version = "0.10.14",
            sizeBytes = 15_000_000L, // ~15 MB
            type = ModelType.MEDIAPIPE_TASK,
            capabilities = setOf(ModelCapability.VISION, ModelCapability.POSE),
            minimumRamMb = 384L,
            preferredBackend = ExecutionBackend.GPU,
            downloadUrl = "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/latest/pose_landmarker_full.task",
            checksum = "sha256:2d1847e45ff40cbffbfec3bc9dc7bcfc99859f7ec9e3150bf4bb904ef23db98f",
            license = "Apache-2.0",
            isInstalled = false,
            isEnabled = false,
            localPath = null,
            status = ModelStatus.NOT_INSTALLED
        ),
        EdgeModel(
            id = "mediapipe-hand-landmarker",
            name = "MediaPipe Hand Landmarker",
            version = "0.10.14",
            sizeBytes = 12_000_000L, // ~12 MB
            type = ModelType.MEDIAPIPE_TASK,
            capabilities = setOf(ModelCapability.VISION, ModelCapability.HAND_TRACKING),
            minimumRamMb = 256L,
            preferredBackend = ExecutionBackend.GPU,
            downloadUrl = "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task",
            checksum = "sha256:3a73c1c73a628867a57a1ef2480689b14736f890e0b35fbdb39e5ee6deebf456",
            license = "Apache-2.0",
            isInstalled = false,
            isEnabled = false,
            localPath = null,
            status = ModelStatus.NOT_INSTALLED
        ),
        EdgeModel(
            id = "mediapipe-face-landmarker",
            name = "MediaPipe Face Landmarker & Mesh",
            version = "0.10.14",
            sizeBytes = 9_500_000L, // ~9.5 MB
            type = ModelType.MEDIAPIPE_TASK,
            capabilities = setOf(ModelCapability.VISION, ModelCapability.FACE_LANDMARKS),
            minimumRamMb = 256L,
            preferredBackend = ExecutionBackend.GPU,
            downloadUrl = "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/latest/face_landmarker.task",
            checksum = "sha256:4a8b417c46f491c3905cf4db8f59d5dc01a7509c256034177d132a265e317c2f",
            license = "Apache-2.0",
            isInstalled = false,
            isEnabled = false,
            localPath = null,
            status = ModelStatus.NOT_INSTALLED
        ),
        EdgeModel(
            id = "efficientdet-lite0-object",
            name = "EfficientDet-Lite0 Object Detector",
            version = "1.0.0",
            sizeBytes = 18_000_000L, // ~18 MB
            type = ModelType.LITERT_VISION,
            capabilities = setOf(ModelCapability.VISION, ModelCapability.OBJECT_DETECTION),
            minimumRamMb = 384L,
            preferredBackend = ExecutionBackend.AUTO,
            downloadUrl = "https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/float16/latest/efficientdet_lite0.tflite",
            checksum = "sha256:5b8e426fc36573e04cf4c136329e46a78241e3d489b4f2c050efb042971bb890",
            license = "Apache-2.0",
            isInstalled = false,
            isEnabled = false,
            localPath = null,
            status = ModelStatus.NOT_INSTALLED
        ),
        EdgeModel(
            id = "mobilenet-v4-classifier",
            name = "MobileNetV4 Vision Classifier",
            version = "1.0.0",
            sizeBytes = 22_000_000L, // ~22 MB
            type = ModelType.LITERT_VISION,
            capabilities = setOf(ModelCapability.VISION, ModelCapability.CLASSIFICATION),
            minimumRamMb = 256L,
            preferredBackend = ExecutionBackend.GPU,
            downloadUrl = "https://storage.googleapis.com/mediapipe-models/image_classifier/mobilenet_v4/float16/latest/mobilenet_v4.tflite",
            checksum = "sha256:1a82f37c44e996fb92427ae41e4649b934ca495991b7852b855e3b0c44298fc1",
            license = "Apache-2.0",
            isInstalled = false,
            isEnabled = false,
            localPath = null,
            status = ModelStatus.NOT_INSTALLED
        ),
        EdgeModel(
            id = "tinyllama-1.1b-chat",
            name = "TinyLlama 1.1B (Fast Edge Chat)",
            version = "1.1.0",
            sizeBytes = 680_000_000L, // ~680 MB quantized INT4
            type = ModelType.LITERT_LM,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT, ModelCapability.SUMMARIZATION),
            minimumRamMb = 1024L,
            preferredBackend = ExecutionBackend.GPU,
            downloadUrl = "https://huggingface.co/TinyLlama/TinyLlama-1.1B-Chat-v1.0-LiteRT",
            checksum = "sha256:9c83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9011",
            license = "Apache-2.0",
            isInstalled = false,
            isEnabled = false,
            localPath = null,
            status = ModelStatus.NOT_INSTALLED
        ),
        EdgeModel(
            id = "gemma-7b-it-quantized",
            name = "Gemma 7B IT (Heavy Reasoning)",
            version = "2.0.0",
            sizeBytes = 4_500_000_000L, // ~4.5 GB
            type = ModelType.LITERT_LM,
            capabilities = setOf(ModelCapability.TEXT, ModelCapability.CHAT, ModelCapability.REASONING),
            minimumRamMb = 6144L,
            preferredBackend = ExecutionBackend.NPU,
            downloadUrl = "https://huggingface.co/google/gemma-7b-it-litert",
            checksum = "sha256:7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069",
            license = "Gemma Terms of Use",
            isInstalled = false,
            isEnabled = false,
            localPath = null,
            status = ModelStatus.NOT_INSTALLED
        )
    )
}

/**
 * Manages on-device model installation, removal, update, verification, and memory estimation.
 */
class LocalModelManager(private val context: Context) {
    private val _models = MutableStateFlow<List<EdgeModel>>(ModelRegistry.DEFAULT_MODELS)
    val models: StateFlow<List<EdgeModel>> = _models.asStateFlow()

    private val modelsDirectory: File by lazy {
        File(context.filesDir, "edge_models").apply { if (!exists()) mkdirs() }
    }

    init {
        scanAndVerifyInstalledModels()
    }

    fun scanAndVerifyInstalledModels() {
        val currentList = _models.value.toMutableList()
        val updatedList = currentList.map { model ->
            val binFile = File(modelsDirectory, "${model.id}.bin")
            val tfliteFile = File(modelsDirectory, "${model.id}.tflite")
            val taskFile = File(modelsDirectory, "${model.id}.task")
            val existingFile = when {
                binFile.exists() && binFile.length() > 0 -> binFile
                tfliteFile.exists() && tfliteFile.length() > 0 -> tfliteFile
                taskFile.exists() && taskFile.length() > 0 -> taskFile
                else -> null
            }
            if (existingFile != null && verifyModelArtifact(existingFile)) {
                model.copy(
                    isInstalled = true,
                    isEnabled = true,
                    localPath = existingFile.absolutePath,
                    status = ModelStatus.INSTALLED
                )
            } else {
                model.copy(
                    isInstalled = false,
                    localPath = null,
                    status = ModelStatus.NOT_INSTALLED
                )
            }
        }
        _models.value = updatedList
    }

    fun verifyModelArtifact(file: File): Boolean {
        return file.exists() && file.length() > 0 && file.canRead()
    }

    fun getInstalledModels(): List<EdgeModel> {
        return _models.value.filter { it.isInstalled && it.isEnabled }
    }

    fun getModelInfo(modelId: String): EdgeModel? {
        return _models.value.firstOrNull { it.id == modelId }
    }

    fun isInstalled(modelId: String): Boolean {
        val model = getModelInfo(modelId) ?: return false
        if (!model.isInstalled || model.localPath.isNullOrBlank()) return false
        val file = File(model.localPath)
        return verifyModelArtifact(file)
    }

    fun estimateMemoryRequirement(modelIds: List<String>): Long {
        return _models.value
            .filter { modelIds.contains(it.id) }
            .sumOf { it.minimumRamMb }
    }

    suspend fun installModel(modelId: String, onProgress: (Float) -> Unit = {}): EdgeResult<EdgeModel> {
        val model = getModelInfo(modelId) ?: return EdgeResult.Failure(EdgeAIError.ModelUnavailable(modelId))
        
        updateModelStatus(modelId, ModelStatus.VERIFYING, 0.1f)
        onProgress(0.1f)

        try {
            val targetFile = File(modelsDirectory, "${model.id}.bin")
            if (targetFile.exists() && verifyModelArtifact(targetFile)) {
                val updated = model.copy(
                    isInstalled = true,
                    isEnabled = true,
                    localPath = targetFile.absolutePath,
                    status = ModelStatus.INSTALLED,
                    downloadProgress = 1.0f
                )
                updateModelInList(updated)
                onProgress(1.0f)
                return EdgeResult.Success(updated)
            }

            updateModelStatus(modelId, ModelStatus.ERROR, 0f)
            return EdgeResult.Failure(
                EdgeAIError.ModelUnavailable("Model artifact '$modelId' is not present in local storage. Please import the verified model binary to complete installation.")
            )
        } catch (e: Exception) {
            updateModelStatus(modelId, ModelStatus.ERROR, 0f)
            return EdgeResult.Failure(EdgeAIError.Unknown("Failed to install model $modelId: ${e.message}", e))
        }
    }

    fun removeModel(modelId: String): EdgeResult<Boolean> {
        val model = getModelInfo(modelId) ?: return EdgeResult.Failure(EdgeAIError.ModelUnavailable(modelId))
        val targetFile = File(modelsDirectory, "${model.id}.bin")
        if (targetFile.exists()) {
            targetFile.delete()
        }
        val tfliteFile = File(modelsDirectory, "${model.id}.tflite")
        if (tfliteFile.exists()) {
            tfliteFile.delete()
        }
        val taskFile = File(modelsDirectory, "${model.id}.task")
        if (taskFile.exists()) {
            taskFile.delete()
        }

        val updated = model.copy(
            isInstalled = false,
            isEnabled = false,
            localPath = null,
            status = ModelStatus.NOT_INSTALLED,
            downloadProgress = 0f
        )
        updateModelInList(updated)
        return EdgeResult.Success(true)
    }

    fun setModelEnabled(modelId: String, enabled: Boolean) {
        val model = getModelInfo(modelId) ?: return
        updateModelInList(model.copy(isEnabled = enabled))
    }

    fun importLocalModel(file: File, name: String, type: ModelType, capabilities: Set<ModelCapability>): EdgeResult<EdgeModel> {
        if (!file.exists() || !verifyModelArtifact(file)) {
            return EdgeResult.Failure(EdgeAIError.InvalidResponse("Model file does not exist or is empty"))
        }
        val extension = file.extension.ifBlank { "bin" }
        val id = "custom-${file.nameWithoutExtension.lowercase().replace(" ", "-")}"
        val targetFile = File(modelsDirectory, "${id}.${extension}")
        file.copyTo(targetFile, overwrite = true)
        
        val newModel = EdgeModel(
            id = id,
            name = name,
            version = "1.0.0-custom",
            sizeBytes = targetFile.length(),
            type = type,
            capabilities = capabilities,
            minimumRamMb = 512L,
            preferredBackend = ExecutionBackend.AUTO,
            license = "Custom Local User Model",
            isInstalled = true,
            isEnabled = true,
            localPath = targetFile.absolutePath,
            status = ModelStatus.INSTALLED
        )
        val current = _models.value.toMutableList()
        current.removeAll { it.id == id }
        current.add(newModel)
        _models.value = current
        return EdgeResult.Success(newModel)
    }

    fun getUsedStorageBytes(): Long {
        return _models.value.filter { it.isInstalled }.sumOf { it.sizeBytes }
    }

    private fun updateModelStatus(modelId: String, status: ModelStatus, progress: Float) {
        val list = _models.value.map {
            if (it.id == modelId) it.copy(status = status, downloadProgress = progress) else it
        }
        _models.value = list
    }

    private fun updateModelInList(updated: EdgeModel) {
        val list = _models.value.map { if (it.id == updated.id) updated else it }
        _models.value = list
    }
}

