package com.codex.sleepmonitor.service

import com.codex.sleepmonitor.data.CalibrationProfile
import com.codex.sleepmonitor.data.LiveAnalysisFrame
import com.codex.sleepmonitor.data.SleepEvent
import com.codex.sleepmonitor.data.SleepEventType
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

class SleepAudioAnalyzer {
    private var noiseFloorDb = -56f
    private var calibration = CalibrationProfile()
    private var candidateType: SleepEventType? = null
    private var streakStartMillis = 0L
    private var streakPeakDb = -80f
    private var streakConfidenceSum = 0f
    private var streakFrames = 0
    private var lastEventAtMillis = 0L
    private var lastAmplitudeDb = -60f

    fun setCalibration(profile: CalibrationProfile) {
        calibration = profile
        if (profile.nightsAnalyzed > 0) {
            noiseFloorDb = profile.averageNoiseFloorDb
        }
    }

    fun analyze(
        samples: ShortArray,
        readSize: Int,
        frameEndMillis: Long
    ): LiveAnalysisFrame {
        if (readSize <= 0) {
            return LiveAnalysisFrame(
                amplitudeDb = -90f,
                noiseFloorDb = noiseFloorDb,
                confidence = 0f,
                disturbanceScore = 0f
            )
        }

        var energy = 0.0
        var zeroCrossings = 0
        var peak = 1
        var sharpTransitions = 0
        for (index in 0 until readSize) {
            val sample = samples[index].toDouble()
            energy += sample * sample
            peak = maxOf(peak, abs(samples[index].toInt()))
            if (index > 0) {
                val previous = samples[index - 1]
                val current = samples[index]
                if ((previous >= 0) != (current >= 0)) {
                    zeroCrossings++
                }
                if (abs(current - previous) > 4_000) {
                    sharpTransitions++
                }
            }
        }

        val rms = sqrt(energy / readSize.toDouble()).coerceAtLeast(1.0)
        val amplitudeDb = (20.0 * ln(rms / MAX_PCM) / LN_10)
            .toFloat()
            .coerceAtLeast(-90f)
        val zeroCrossRate = zeroCrossings.toFloat() / readSize.toFloat()
        val crestFactor = peak.toFloat() / rms.toFloat()
        val transientRatio = sharpTransitions.toFloat() / readSize.toFloat()
        val amplitudeDelta = amplitudeDb - noiseFloorDb
        val suddenRise = amplitudeDb - lastAmplitudeDb

        val snoreScore = (
            scaled(amplitudeDelta, 4f, 18f) * 0.55f +
                closeness(zeroCrossRate, 0.08f, 0.08f) * 0.25f +
                closeness(crestFactor, 3.4f, 3.0f) * 0.20f
            ).coerceIn(0f, 1f)
        val talkScore = (
            scaled(amplitudeDelta, 3f, 18f) * 0.35f +
                closeness(zeroCrossRate, 0.16f, 0.12f) * 0.40f +
                closeness(crestFactor, 2.5f, 2.5f) * 0.15f +
                closeness(transientRatio, 0.08f, 0.10f) * 0.10f
            ).coerceIn(0f, 1f)
        val grindScore = (
            scaled(amplitudeDelta, 5f, 20f) * 0.25f +
                closeness(zeroCrossRate, 0.28f, 0.18f) * 0.25f +
                closeness(crestFactor, 4.8f, 3.5f) * 0.25f +
                closeness(transientRatio, 0.16f, 0.12f) * 0.25f
            ).coerceIn(0f, 1f)
        val ambientScore = (
            scaled(amplitudeDelta, 10f, 22f) * 0.45f +
                scaled(suddenRise, 3f, 12f) * 0.35f +
                closeness(transientRatio, 0.12f, 0.15f) * 0.20f
            ).coerceIn(0f, 1f)

        val scoreMap = linkedMapOf(
            SleepEventType.SNORE to snoreScore,
            SleepEventType.DREAM_TALK to talkScore,
            SleepEventType.TEETH_GRINDING to grindScore,
            SleepEventType.AMBIENT_ALERT to ambientScore
        )
        val topEntry = scoreMap.maxByOrNull { it.value }
        val disturbanceScore = scoreMap.values.maxOrNull() ?: 0f
        val events = mutableListOf<SleepEvent>()

        val candidateAllowed = topEntry != null &&
            disturbanceScore >= detectionThreshold(topEntry.key) &&
            amplitudeDelta >= minAmplitudeDelta(topEntry.key) &&
            frameEndMillis - lastEventAtMillis > GLOBAL_COOLDOWN_MS

        if (!candidateAllowed) {
            val noiseSample = amplitudeDb.coerceAtMost(noiseFloorDb + 4f)
            noiseFloorDb = (noiseFloorDb * 0.96f) + (noiseSample * 0.04f)
        }

        if (candidateType != null && (!candidateAllowed || topEntry?.key != candidateType)) {
            finalizeCandidate(frameEndMillis)?.let(events::add)
        }

        if (candidateAllowed && topEntry != null) {
            absorbCandidate(topEntry.key, amplitudeDb, topEntry.value, frameEndMillis)
        }

        candidateType?.let { type ->
            if (frameEndMillis - streakStartMillis >= maxDuration(type)) {
                finalizeCandidate(frameEndMillis)?.let(events::add)
            }
        }

        lastAmplitudeDb = amplitudeDb
        return LiveAnalysisFrame(
            amplitudeDb = amplitudeDb,
            noiseFloorDb = noiseFloorDb,
            confidence = disturbanceScore,
            disturbanceScore = disturbanceScore,
            events = events
        )
    }

    private fun absorbCandidate(
        type: SleepEventType,
        amplitudeDb: Float,
        confidence: Float,
        frameEndMillis: Long
    ) {
        if (candidateType != type || streakFrames == 0) {
            candidateType = type
            streakStartMillis = frameEndMillis - FRAME_DURATION_MS
            streakPeakDb = amplitudeDb
            streakConfidenceSum = 0f
            streakFrames = 0
        }

        streakFrames += 1
        streakConfidenceSum += confidence
        streakPeakDb = maxOf(streakPeakDb, amplitudeDb)
    }

    private fun finalizeCandidate(frameEndMillis: Long): SleepEvent? {
        val type = candidateType ?: return null
        if (streakFrames == 0) {
            resetCandidate()
            return null
        }

        val duration = frameEndMillis - streakStartMillis
        val averageConfidence = streakConfidenceSum / streakFrames.toFloat()
        val valid = duration in minDuration(type)..maxDuration(type) &&
            averageConfidence >= detectionThreshold(type)

        val event = if (valid) {
            lastEventAtMillis = frameEndMillis
            SleepEvent(
                type = type,
                timestampMillis = frameEndMillis,
                durationMillis = duration,
                intensity = ((streakPeakDb + 46f) / 30f).coerceIn(0f, 1f),
                peakDb = streakPeakDb
            )
        } else {
            null
        }

        resetCandidate()
        return event
    }

    private fun resetCandidate() {
        candidateType = null
        streakStartMillis = 0L
        streakPeakDb = -80f
        streakConfidenceSum = 0f
        streakFrames = 0
    }

    private fun detectionThreshold(type: SleepEventType): Float = when (type) {
        SleepEventType.SNORE -> 0.56f + calibration.snoreThresholdOffset
        SleepEventType.DREAM_TALK -> 0.54f + calibration.talkThresholdOffset
        SleepEventType.TEETH_GRINDING -> 0.58f + calibration.grindThresholdOffset
        SleepEventType.AMBIENT_ALERT -> 0.60f + calibration.ambientThresholdOffset
    }.coerceIn(0.45f, 0.78f)

    private fun minAmplitudeDelta(type: SleepEventType): Float = when (type) {
        SleepEventType.SNORE -> 4f
        SleepEventType.DREAM_TALK -> 3f
        SleepEventType.TEETH_GRINDING -> 4f
        SleepEventType.AMBIENT_ALERT -> 8f
    }

    private fun minDuration(type: SleepEventType): Long = when (type) {
        SleepEventType.SNORE -> 700L
        SleepEventType.DREAM_TALK -> 1_000L
        SleepEventType.TEETH_GRINDING -> 400L
        SleepEventType.AMBIENT_ALERT -> 250L
    }

    private fun maxDuration(type: SleepEventType): Long = when (type) {
        SleepEventType.SNORE -> 3_600L
        SleepEventType.DREAM_TALK -> 5_000L
        SleepEventType.TEETH_GRINDING -> 2_200L
        SleepEventType.AMBIENT_ALERT -> 2_500L
    }

    private fun closeness(value: Float, target: Float, tolerance: Float): Float {
        return (1f - abs(value - target) / tolerance).coerceIn(0f, 1f)
    }

    private fun scaled(value: Float, min: Float, range: Float): Float {
        return ((value - min) / range).coerceIn(0f, 1f)
    }

    private companion object {
        const val FRAME_DURATION_MS = 500L
        const val GLOBAL_COOLDOWN_MS = 1_500L
        const val MAX_PCM = 32768.0
        const val LN_10 = 2.302585093
    }
}
