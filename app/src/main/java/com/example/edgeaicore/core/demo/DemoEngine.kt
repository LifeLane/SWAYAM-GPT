package com.example.edgeaicore.core.demo

import com.example.edgeaicore.core.common.PrivacyLevel
import com.example.edgeaicore.core.common.RiskLevel
import com.example.edgeaicore.core.mediapipe.DetectedObject
import com.example.edgeaicore.core.mediapipe.VisionResult
import com.example.edgeaicore.core.memory.MemoryEntity
import com.example.edgeaicore.core.memory.MemoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.graphics.RectF

data class DemoScenario(
    val id: String,
    val title: String,
    val description: String,
    val targetApp: String, // MemoryLens, PocketAgent, MirrorOS, BodyOS, etc.
    val sampleQuery: String,
    val sampleMemories: List<MemoryEntity>,
    val sampleVision: VisionResult
)

class DemoEngine {
    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    val demoScenarios: List<DemoScenario> = listOf(
        DemoScenario(
            id = "demo-memorylens",
            title = "MemoryLens: Visual Recall",
            description = "Simulates visual entity detection paired with local episodic memory recall.",
            targetApp = "MemoryLens",
            sampleQuery = "Where did I place my passport and travel documents?",
            sampleMemories = listOf(
                MemoryEntity(
                    id = 901,
                    title = "Travel Document Storage",
                    summary = "Passport placed inside bedroom top drawer safe.",
                    content = "[DEMO MEMORY] Passport and yellow immunization card stored in top oak drawer safe box.",
                    type = MemoryType.PLACE,
                    tags = "passport,travel,documents",
                    privacyLevel = PrivacyLevel.LOCAL_ONLY
                )
            ),
            sampleVision = VisionResult(
                timestamp = System.currentTimeMillis(),
                objects = listOf(
                    DetectedObject("[DEMO] Passport Envelope", 0.96f, RectF(0.2f, 0.2f, 0.7f, 0.8f))
                ),
                confidence = 0.96f,
                processingTimeMs = 12L
            )
        ),
        DemoScenario(
            id = "demo-pocketagent",
            title = "PocketAgent: Local Action Plan",
            description = "Simulates on-device context synthesis with zero-cloud action execution.",
            targetApp = "PocketAgent",
            sampleQuery = "Schedule focus block for writing EdgeAI Core documentation",
            sampleMemories = listOf(
                MemoryEntity(
                    id = 902,
                    title = "Work Preference",
                    summary = "Prefers morning 90-min deep work blocks.",
                    content = "[DEMO MEMORY] Deep focus blocks scheduled before noon with muted alerts.",
                    type = MemoryType.PREFERENCE,
                    tags = "calendar,focus,productivity",
                    privacyLevel = PrivacyLevel.LOCAL_ONLY
                )
            ),
            sampleVision = VisionResult()
        ),
        DemoScenario(
            id = "demo-bodyos",
            title = "BodyOS: Real-Time Biomechanical Pose",
            description = "33-keypoint on-device pose estimation tracking posture alignment at 30 FPS.",
            targetApp = "BodyOS",
            sampleQuery = "Analyze ergonomic cervical spine posture",
            sampleMemories = emptyList(),
            sampleVision = VisionResult(
                timestamp = System.currentTimeMillis(),
                classifications = listOf("[DEMO] Ergonomic Neutral Spine (Good Alignment)"),
                confidence = 0.94f,
                processingTimeMs = 8L
            )
        )
    )

    fun toggleDemoMode() {
        _isDemoMode.value = !_isDemoMode.value
    }
}
