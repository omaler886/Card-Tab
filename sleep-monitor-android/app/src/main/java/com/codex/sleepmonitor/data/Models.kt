package com.codex.sleepmonitor.data

import kotlinx.serialization.Serializable

@Serializable
enum class SleepEventType {
    SNORE,
    DREAM_TALK,
    TEETH_GRINDING,
    AMBIENT_ALERT
}

@Serializable
data class SnoreEvent(
    val timestampMillis: Long,
    val durationMillis: Long,
    val intensity: Float,
    val peakDb: Float
)

@Serializable
data class SleepEvent(
    val type: SleepEventType,
    val timestampMillis: Long,
    val durationMillis: Long,
    val intensity: Float,
    val peakDb: Float
)

@Serializable
data class AnomalyClip(
    val id: String,
    val eventType: SleepEventType,
    val capturedAtMillis: Long,
    val durationMillis: Long,
    val filePath: String,
    val peakDb: Float
)

@Serializable
data class SoundBucket(
    val startMillis: Long,
    val averageDb: Float = -60f,
    val peakDb: Float = -60f,
    val frameCount: Int = 0,
    val disturbanceScore: Float = 0f,
    val snoreCount: Int = 0,
    val talkCount: Int = 0,
    val grindCount: Int = 0,
    val ambientCount: Int = 0
)

@Serializable
data class NapPlan(
    val durationMinutes: Int,
    val startedAtMillis: Long,
    val wakeAtMillis: Long,
    val triggeredAtMillis: Long? = null
)

@Serializable
data class SmartWakePlan(
    val durationHours: Int,
    val windowMinutes: Int,
    val startedAtMillis: Long,
    val targetWakeAtMillis: Long,
    val triggeredAtMillis: Long? = null
)

@Serializable
data class BedtimePlan(
    val hour: Int,
    val minute: Int,
    val autoStart: Boolean,
    val nextTriggerAtMillis: Long,
    val enabled: Boolean = true
)

@Serializable
enum class SoothingSoundType {
    WHITE_NOISE,
    RAIN,
    OCEAN,
    FAN
}

@Serializable
data class WhiteNoisePlan(
    val soundType: SoothingSoundType,
    val volumePercent: Int,
    val startedAtMillis: Long,
    val stopAtMillis: Long? = null
)

@Serializable
data class AutoStopPlan(
    val durationHours: Int,
    val startedAtMillis: Long,
    val stopAtMillis: Long
)

@Serializable
data class CalibrationProfile(
    val nightsAnalyzed: Int = 0,
    val averageNoiseFloorDb: Float = -56f,
    val snoreThresholdOffset: Float = 0f,
    val talkThresholdOffset: Float = 0f,
    val grindThresholdOffset: Float = 0f,
    val ambientThresholdOffset: Float = 0f
)

@Serializable
data class SleepLogEntry(
    val caffeineCups: Int = 0,
    val stressLevel: Int = 2,
    val exerciseMinutes: Int = 0,
    val lateMeal: Boolean = false,
    val alcohol: Boolean = false
)

@Serializable
data class SleepSession(
    val id: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val snoreEvents: List<SnoreEvent> = emptyList(),
    val sleepEvents: List<SleepEvent> = emptyList(),
    val anomalyClips: List<AnomalyClip> = emptyList(),
    val soundBuckets: List<SoundBucket> = emptyList(),
    val sleepLog: SleepLogEntry = SleepLogEntry(),
    val rollingAmplitudeDb: Float = -60f,
    val peakAmplitudeDb: Float = -60f,
    val noiseFloorDb: Float = -60f,
    val latestConfidence: Float = 0f,
    val latestDisturbance: Float = 0f,
    val latestSnoreAtMillis: Long? = null,
    val latestEventAtMillis: Long? = null,
    val latestEventType: SleepEventType? = null,
    val smartWakeTriggeredAtMillis: Long? = null,
    val lastHeartbeatAtMillis: Long = startedAtMillis
)

@Serializable
data class SleepStore(
    val activeSession: SleepSession? = null,
    val sessions: List<SleepSession> = emptyList(),
    val activeNapPlan: NapPlan? = null,
    val activeSmartWakePlan: SmartWakePlan? = null,
    val activeWhiteNoisePlan: WhiteNoisePlan? = null,
    val activeAutoStopPlan: AutoStopPlan? = null,
    val bedtimePlan: BedtimePlan? = null,
    val calibrationProfile: CalibrationProfile = CalibrationProfile(),
    val draftSleepLog: SleepLogEntry = SleepLogEntry()
)

data class LiveAnalysisFrame(
    val amplitudeDb: Float,
    val noiseFloorDb: Float,
    val confidence: Float,
    val disturbanceScore: Float,
    val events: List<SleepEvent> = emptyList()
)
