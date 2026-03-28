package com.codex.sleepmonitor.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.codex.sleepmonitor.data.SoothingSoundType
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WhiteNoisePlayer {
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private var currentVolume = 0.25f
    private var currentSoundType = SoothingSoundType.WHITE_NOISE

    fun start(
        scope: CoroutineScope,
        soundType: SoothingSoundType,
        volumePercent: Int
    ) {
        val normalizedVolume = (volumePercent / 100f).coerceIn(0.05f, 1f)
        if (playbackJob?.isActive == true && currentSoundType == soundType) {
            currentVolume = normalizedVolume
            audioTrack?.setVolume(currentVolume)
            return
        }

        stopImmediate()
        currentVolume = normalizedVolume
        currentSoundType = soundType

        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            minBuffer.coerceAtLeast(BUFFER_SAMPLES * 2),
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.setVolume(currentVolume)
        track.play()
        audioTrack = track

        playbackJob = scope.launch(Dispatchers.Default) {
            val random = Random(System.currentTimeMillis())
            val buffer = ShortArray(BUFFER_SAMPLES)
            var lowPassState = 0f
            var oceanPhase = 0.0
            var oceanPhase2 = 0.0
            var fanPhase = 0.0
            while (isActive) {
                when (currentSoundType) {
                    SoothingSoundType.WHITE_NOISE -> fillWhiteNoise(buffer, random)
                    SoothingSoundType.RAIN -> {
                        lowPassState = fillRain(buffer, random, lowPassState)
                    }
                    SoothingSoundType.OCEAN -> {
                        val updated = fillOcean(buffer, random, oceanPhase, oceanPhase2)
                        oceanPhase = updated.first
                        oceanPhase2 = updated.second
                    }
                    SoothingSoundType.FAN -> {
                        fanPhase = fillFan(buffer, random, fanPhase)
                    }
                }
                track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
            }
        }
    }

    suspend fun fadeOutAndStop(durationMillis: Long = 8_000L) {
        val track = audioTrack ?: return
        val steps = 16
        val initialVolume = currentVolume
        repeat(steps) { index ->
            val ratio = 1f - ((index + 1) / steps.toFloat())
            val target = (initialVolume * ratio).coerceAtLeast(0f)
            currentVolume = target
            track.setVolume(target)
            delay((durationMillis / steps).coerceAtLeast(30L))
        }
        stopImmediate()
    }

    fun stopImmediate() {
        playbackJob?.cancel()
        playbackJob = null
        audioTrack?.pause()
        audioTrack?.flush()
        audioTrack?.release()
        audioTrack = null
    }

    fun isPlaying(): Boolean = playbackJob?.isActive == true

    private fun fillWhiteNoise(buffer: ShortArray, random: Random) {
        for (index in buffer.indices) {
            buffer[index] = ((random.nextFloat() * 2f - 1f) * Short.MAX_VALUE * currentVolume * WHITE_GAIN)
                .toInt()
                .toShort()
        }
    }

    private fun fillRain(buffer: ShortArray, random: Random, state: Float): Float {
        var lowPass = state
        for (index in buffer.indices) {
            val noise = random.nextFloat() * 2f - 1f
            lowPass = lowPass * 0.95f + noise * 0.05f
            val droplet = if (random.nextFloat() < 0.0012f) {
                (random.nextFloat() * 2f - 1f) * 0.6f
            } else {
                0f
            }
            val sample = ((lowPass * 0.7f) + droplet) * currentVolume * Short.MAX_VALUE * RAIN_GAIN
            buffer[index] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return lowPass
    }

    private fun fillOcean(
        buffer: ShortArray,
        random: Random,
        phase1: Double,
        phase2: Double
    ): Pair<Double, Double> {
        var p1 = phase1
        var p2 = phase2
        for (index in buffer.indices) {
            val wave = sin(p1) * 0.55 + sin(p2) * 0.35
            val foam = (random.nextFloat() * 2f - 1f) * 0.15f
            val sample = ((wave + foam) * currentVolume * Short.MAX_VALUE * OCEAN_GAIN)
            buffer[index] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            p1 += 2 * PI * 0.22 / SAMPLE_RATE
            p2 += 2 * PI * 0.08 / SAMPLE_RATE
        }
        return p1 to p2
    }

    private fun fillFan(buffer: ShortArray, random: Random, phase: Double): Double {
        var p = phase
        var lowPass = 0f
        for (index in buffer.indices) {
            val hum = sin(p) * 0.65
            val noise = random.nextFloat() * 2f - 1f
            lowPass = lowPass * 0.88f + noise * 0.12f
            val sample = ((hum * 0.55f) + (lowPass * 0.20f)) * currentVolume * Short.MAX_VALUE * FAN_GAIN
            buffer[index] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            p += 2 * PI * 78 / SAMPLE_RATE
        }
        return p
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
        const val BUFFER_SAMPLES = 2048
        const val WHITE_GAIN = 0.30f
        const val RAIN_GAIN = 0.24f
        const val OCEAN_GAIN = 0.22f
        const val FAN_GAIN = 0.20f
    }
}
