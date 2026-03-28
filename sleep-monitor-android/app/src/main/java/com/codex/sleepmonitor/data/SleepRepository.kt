package com.codex.sleepmonitor.data

import android.app.Application
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SleepRepository(application: Application) {
    private val mutex = Mutex()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val secureStorage = SecureStorage()
    private val storeFile = File(application.filesDir, STORE_FILE_NAME)
    private val _store = MutableStateFlow(loadInitialStore())
    private var lastDiskPersistAtMillis = System.currentTimeMillis()

    val store: StateFlow<SleepStore> = _store.asStateFlow()

    suspend fun startSession(now: Long = System.currentTimeMillis()): SleepSession = mutex.withLock {
        _store.value.activeSession?.let { return it }

        val session = SleepSession(
            id = "session-$now",
            startedAtMillis = now,
            sleepLog = _store.value.draftSleepLog,
            lastHeartbeatAtMillis = now
        )
        val updated = _store.value.copy(activeSession = session)
        publish(updated, forceDisk = true)
        updated.activeSession!!
    }

    suspend fun updateLiveFrame(
        frame: LiveAnalysisFrame,
        now: Long = System.currentTimeMillis()
    ) = mutex.withLock {
        val active = _store.value.activeSession ?: return@withLock
        val latestEvent = frame.events.lastOrNull()
        val latestSnore = frame.events.lastOrNull { it.type == SleepEventType.SNORE }
        val updatedActive = active.copy(
            rollingAmplitudeDb = frame.amplitudeDb,
            peakAmplitudeDb = max(active.peakAmplitudeDb, frame.amplitudeDb),
            noiseFloorDb = frame.noiseFloorDb,
            latestConfidence = frame.confidence,
            latestDisturbance = frame.disturbanceScore,
            latestSnoreAtMillis = latestSnore?.timestampMillis ?: active.latestSnoreAtMillis,
            latestEventAtMillis = latestEvent?.timestampMillis ?: active.latestEventAtMillis,
            latestEventType = latestEvent?.type ?: active.latestEventType,
            soundBuckets = mergeFrameIntoBuckets(active.soundBuckets, frame, now),
            lastHeartbeatAtMillis = now
        )
        publish(
            _store.value.copy(activeSession = updatedActive),
            forceDisk = now - lastDiskPersistAtMillis >= LIVE_SYNC_INTERVAL_MS
        )
    }

    suspend fun registerEvents(
        events: List<SleepEvent>,
        now: Long = System.currentTimeMillis()
    ) = mutex.withLock {
        if (events.isEmpty()) {
            return@withLock
        }

        val active = _store.value.activeSession ?: return@withLock
        val latestEvent = events.maxByOrNull { it.timestampMillis }
        val updatedActive = active.copy(
            snoreEvents = active.snoreEvents + events
                .filter { it.type == SleepEventType.SNORE }
                .map {
                    SnoreEvent(
                        timestampMillis = it.timestampMillis,
                        durationMillis = it.durationMillis,
                        intensity = it.intensity,
                        peakDb = it.peakDb
                    )
                },
            sleepEvents = active.sleepEvents + events,
            soundBuckets = mergeEventsIntoBuckets(active.soundBuckets, events),
            peakAmplitudeDb = max(
                active.peakAmplitudeDb,
                events.maxOf { it.peakDb }
            ),
            latestSnoreAtMillis = events.lastOrNull { it.type == SleepEventType.SNORE }?.timestampMillis
                ?: active.latestSnoreAtMillis,
            latestEventAtMillis = latestEvent?.timestampMillis ?: active.latestEventAtMillis,
            latestEventType = latestEvent?.type ?: active.latestEventType,
            lastHeartbeatAtMillis = now
        )
        publish(_store.value.copy(activeSession = updatedActive), forceDisk = true)
    }

    suspend fun registerClip(clip: AnomalyClip) = mutex.withLock {
        val active = _store.value.activeSession ?: return@withLock
        val updated = _store.value.copy(
            activeSession = active.copy(
                anomalyClips = (active.anomalyClips + clip).takeLast(MAX_CLIPS_PER_SESSION)
            )
        )
        publish(updated, forceDisk = true)
    }

    suspend fun stopSession(now: Long = System.currentTimeMillis()) = mutex.withLock {
        val active = _store.value.activeSession ?: return@withLock
        val completed = normalizeSession(
            active.copy(
                endedAtMillis = now,
                lastHeartbeatAtMillis = now
            )
        )
        val updatedSessions = listOf(completed) + _store.value.sessions.take(MAX_HISTORY - 1)
        val updated = _store.value.copy(
            activeSession = null,
            sessions = updatedSessions,
            activeSmartWakePlan = null,
            activeAutoStopPlan = null
        )
        publish(
            updated.copy(
                calibrationProfile = deriveCalibration(updatedSessions)
            ),
            forceDisk = true
        )
    }

    suspend fun startNapPlan(
        durationMinutes: Int,
        now: Long = System.currentTimeMillis()
    ) = mutex.withLock {
        val updated = _store.value.copy(
            activeNapPlan = NapPlan(
                durationMinutes = durationMinutes,
                startedAtMillis = now,
                wakeAtMillis = now + durationMinutes * 60_000L
            )
        )
        publish(updated, forceDisk = true)
    }

    suspend fun triggerNap(now: Long = System.currentTimeMillis()) = mutex.withLock {
        val plan = _store.value.activeNapPlan ?: return@withLock
        if (plan.triggeredAtMillis != null) {
            return@withLock
        }
        publish(
            _store.value.copy(
                activeNapPlan = plan.copy(triggeredAtMillis = now)
            ),
            forceDisk = true
        )
    }

    suspend fun cancelNapPlan() = mutex.withLock {
        publish(_store.value.copy(activeNapPlan = null), forceDisk = true)
    }

    suspend fun startWhiteNoise(
        soundType: SoothingSoundType,
        volumePercent: Int,
        durationMinutes: Int?,
        now: Long = System.currentTimeMillis()
    ) = mutex.withLock {
        val safeVolume = volumePercent.coerceIn(10, 100)
        publish(
            _store.value.copy(
                activeWhiteNoisePlan = WhiteNoisePlan(
                    soundType = soundType,
                    volumePercent = safeVolume,
                    startedAtMillis = now,
                    stopAtMillis = durationMinutes?.let { now + it.coerceIn(5, 180) * 60_000L }
                )
            ),
            forceDisk = true
        )
    }

    suspend fun cancelWhiteNoise() = mutex.withLock {
        publish(_store.value.copy(activeWhiteNoisePlan = null), forceDisk = true)
    }

    suspend fun startSmartWakePlan(
        durationHours: Int,
        windowMinutes: Int,
        now: Long = System.currentTimeMillis()
    ) = mutex.withLock {
        val safeHours = durationHours.coerceIn(4, 12)
        val safeWindow = windowMinutes.coerceIn(10, 60)
        val updated = _store.value.copy(
            activeSmartWakePlan = SmartWakePlan(
                durationHours = safeHours,
                windowMinutes = safeWindow,
                startedAtMillis = now,
                targetWakeAtMillis = now + safeHours * 60L * 60L * 1000L
            )
        )
        publish(updated, forceDisk = true)
    }

    suspend fun triggerSmartWake(now: Long = System.currentTimeMillis()) = mutex.withLock {
        val plan = _store.value.activeSmartWakePlan ?: return@withLock
        if (plan.triggeredAtMillis != null) {
            return@withLock
        }
        val active = _store.value.activeSession
        publish(
            _store.value.copy(
                activeSmartWakePlan = plan.copy(triggeredAtMillis = now),
                activeSession = active?.copy(smartWakeTriggeredAtMillis = now)
            ),
            forceDisk = true
        )
    }

    suspend fun cancelSmartWakePlan() = mutex.withLock {
        publish(_store.value.copy(activeSmartWakePlan = null), forceDisk = true)
    }

    suspend fun startAutoStopPlan(
        durationHours: Int,
        now: Long = System.currentTimeMillis()
    ) = mutex.withLock {
        val safeHours = durationHours.coerceIn(1, 12)
        publish(
            _store.value.copy(
                activeAutoStopPlan = AutoStopPlan(
                    durationHours = safeHours,
                    startedAtMillis = now,
                    stopAtMillis = now + safeHours * 60L * 60L * 1000L
                )
            ),
            forceDisk = true
        )
    }

    suspend fun cancelAutoStopPlan() = mutex.withLock {
        publish(_store.value.copy(activeAutoStopPlan = null), forceDisk = true)
    }

    suspend fun updateBedtimePlan(
        hour: Int,
        minute: Int,
        autoStart: Boolean,
        nextTriggerAtMillis: Long
    ) = mutex.withLock {
        publish(
            _store.value.copy(
                bedtimePlan = BedtimePlan(
                    hour = hour.coerceIn(0, 23),
                    minute = minute.coerceIn(0, 59),
                    autoStart = autoStart,
                    nextTriggerAtMillis = nextTriggerAtMillis,
                    enabled = true
                )
            ),
            forceDisk = true
        )
    }

    suspend fun clearBedtimePlan() = mutex.withLock {
        publish(_store.value.copy(bedtimePlan = null), forceDisk = true)
    }

    suspend fun replaceStore(importedStore: SleepStore) = mutex.withLock {
        val normalizedSessions = importedStore.sessions
            .map(::normalizeSession)
            .sortedByDescending { it.startedAtMillis }
            .take(MAX_HISTORY)
        publish(
            importedStore.copy(
                activeSession = null,
                activeNapPlan = null,
                activeSmartWakePlan = null,
                activeWhiteNoisePlan = null,
                activeAutoStopPlan = null,
                sessions = normalizedSessions,
                calibrationProfile = deriveCalibration(normalizedSessions)
            ),
            forceDisk = true
        )
    }

    suspend fun updateDraftSleepLog(
        caffeineCups: Int? = null,
        stressLevel: Int? = null,
        exerciseMinutes: Int? = null,
        lateMeal: Boolean? = null,
        alcohol: Boolean? = null
    ) = mutex.withLock {
        val current = _store.value.draftSleepLog
        val updated = current.copy(
            caffeineCups = caffeineCups?.coerceIn(0, 6) ?: current.caffeineCups,
            stressLevel = stressLevel?.coerceIn(1, 5) ?: current.stressLevel,
            exerciseMinutes = exerciseMinutes?.coerceIn(0, 180) ?: current.exerciseMinutes,
            lateMeal = lateMeal ?: current.lateMeal,
            alcohol = alcohol ?: current.alcohol
        )
        publish(_store.value.copy(draftSleepLog = updated), forceDisk = true)
    }

    suspend fun recalibrate() = mutex.withLock {
        val updated = _store.value.copy(
            calibrationProfile = deriveCalibration(_store.value.sessions)
        )
        publish(updated, forceDisk = true)
    }

    private fun loadInitialStore(): SleepStore {
        if (!storeFile.exists()) {
            return SleepStore()
        }

        val loaded = runCatching {
            val raw = storeFile.readBytes()
            val plainBytes = if (secureStorage.isEncrypted(raw)) {
                secureStorage.decrypt(raw)
            } else {
                raw
            }
            json.decodeFromString<SleepStore>(plainBytes.decodeToString())
        }.getOrDefault(SleepStore())

        val staleClosedSession = loaded.activeSession
            ?.let(::normalizeSession)
            ?.let { activeSession ->
                if (System.currentTimeMillis() - activeSession.lastHeartbeatAtMillis > STALE_SESSION_TIMEOUT_MS) {
                    activeSession.copy(endedAtMillis = activeSession.lastHeartbeatAtMillis)
                } else {
                    null
                }
            }

        val normalizedSessions = loaded.sessions.map(::normalizeSession)
        val normalized = loaded.copy(
            activeSession = loaded.activeSession
                ?.let(::normalizeSession)
                ?.let(::closeStaleSessionIfNeeded),
            sessions = normalizedSessions,
            calibrationProfile = if (loaded.calibrationProfile.nightsAnalyzed == 0 && normalizedSessions.isNotEmpty()) {
                deriveCalibration(normalizedSessions)
            } else {
                loaded.calibrationProfile
            }
        )

        val finalStore = if (normalized.activeSession == null && staleClosedSession != null) {
            normalized.copy(
                sessions = listOf(staleClosedSession) + normalized.sessions
            )
        } else {
            normalized
        }

        if (finalStore != loaded) {
            writeToDisk(finalStore)
        }
        return finalStore
    }

    private fun normalizeSession(session: SleepSession): SleepSession {
        if (session.sleepEvents.isNotEmpty()) {
            return session
        }
        return session.copy(
            sleepEvents = session.snoreEvents.map {
                SleepEvent(
                    type = SleepEventType.SNORE,
                    timestampMillis = it.timestampMillis,
                    durationMillis = it.durationMillis,
                    intensity = it.intensity,
                    peakDb = it.peakDb
                )
            }
        )
    }

    private fun closeStaleSessionIfNeeded(session: SleepSession): SleepSession? {
        val isStale = System.currentTimeMillis() - session.lastHeartbeatAtMillis > STALE_SESSION_TIMEOUT_MS
        return if (isStale) null else session
    }

    private fun mergeFrameIntoBuckets(
        existing: List<SoundBucket>,
        frame: LiveAnalysisFrame,
        now: Long
    ): List<SoundBucket> {
        val bucketStart = now - (now % BUCKET_DURATION_MS)
        val updated = existing.toMutableList()
        val index = updated.indexOfLast { it.startMillis == bucketStart }

        if (index >= 0) {
            val bucket = updated[index]
            val frameCount = bucket.frameCount + 1
            updated[index] = bucket.copy(
                averageDb = ((bucket.averageDb * bucket.frameCount) + frame.amplitudeDb) / frameCount,
                peakDb = max(bucket.peakDb, frame.amplitudeDb),
                frameCount = frameCount,
                disturbanceScore = ((bucket.disturbanceScore * bucket.frameCount) + frame.disturbanceScore) / frameCount
            )
        } else {
            updated += SoundBucket(
                startMillis = bucketStart,
                averageDb = frame.amplitudeDb,
                peakDb = frame.amplitudeDb,
                frameCount = 1,
                disturbanceScore = frame.disturbanceScore
            )
        }

        return updated.takeLast(MAX_BUCKETS)
    }

    private fun mergeEventsIntoBuckets(
        existing: List<SoundBucket>,
        events: List<SleepEvent>
    ): List<SoundBucket> {
        val updated = existing.toMutableList()
        for (event in events) {
            val bucketStart = event.timestampMillis - (event.timestampMillis % BUCKET_DURATION_MS)
            val index = updated.indexOfLast { it.startMillis == bucketStart }
            val base = if (index >= 0) updated[index] else SoundBucket(startMillis = bucketStart)
            val next = when (event.type) {
                SleepEventType.SNORE -> base.copy(snoreCount = base.snoreCount + 1)
                SleepEventType.DREAM_TALK -> base.copy(talkCount = base.talkCount + 1)
                SleepEventType.TEETH_GRINDING -> base.copy(grindCount = base.grindCount + 1)
                SleepEventType.AMBIENT_ALERT -> base.copy(ambientCount = base.ambientCount + 1)
            }
            if (index >= 0) {
                updated[index] = next
            } else {
                updated += next
            }
        }
        return updated.sortedBy { it.startMillis }.takeLast(MAX_BUCKETS)
    }

    private fun deriveCalibration(sessions: List<SleepSession>): CalibrationProfile {
        val sample = sessions.take(5)
        if (sample.isEmpty()) {
            return CalibrationProfile()
        }

        val averageNoise = sample.map { it.noiseFloorDb }.average().toFloat()
        val talkRate = averageRate(sample, SleepEventType.DREAM_TALK)
        val snoreRate = averageRate(sample, SleepEventType.SNORE)
        val grindRate = averageRate(sample, SleepEventType.TEETH_GRINDING)
        val ambientRate = averageRate(sample, SleepEventType.AMBIENT_ALERT)

        return CalibrationProfile(
            nightsAnalyzed = sample.size,
            averageNoiseFloorDb = averageNoise,
            snoreThresholdOffset = when {
                snoreRate >= 4f -> 0.03f
                averageNoise < -60f -> -0.02f
                else -> 0f
            },
            talkThresholdOffset = when {
                talkRate >= 2f -> 0.02f
                averageNoise < -60f -> -0.01f
                else -> 0f
            },
            grindThresholdOffset = when {
                grindRate >= 1.5f -> 0.03f
                else -> 0f
            },
            ambientThresholdOffset = when {
                averageNoise > -50f || ambientRate >= 3f -> 0.05f
                averageNoise < -60f -> -0.01f
                else -> 0f
            }
        )
    }

    private fun averageRate(sessions: List<SleepSession>, type: SleepEventType): Float {
        return sessions.map { session ->
            val end = session.endedAtMillis ?: session.lastHeartbeatAtMillis
            val hours = ((end - session.startedAtMillis).coerceAtLeast(1L) / 3_600_000f).coerceAtLeast(0.5f)
            val count = session.sleepEvents.count { it.type == type }
            count / hours
        }.average().toFloat()
    }

    private fun publish(store: SleepStore, forceDisk: Boolean) {
        _store.value = store
        if (forceDisk) {
            writeToDisk(store)
        }
    }

    private fun writeToDisk(store: SleepStore) {
        storeFile.parentFile?.mkdirs()
        val tempFile = File(storeFile.parentFile, "${storeFile.name}.tmp")
        val plain = json.encodeToString(store).toByteArray()
        tempFile.writeBytes(secureStorage.encrypt(plain))
        tempFile.copyTo(storeFile, overwrite = true)
        tempFile.delete()
        lastDiskPersistAtMillis = System.currentTimeMillis()
    }

    private companion object {
        const val MAX_HISTORY = 14
        const val MAX_BUCKETS = 180
        const val MAX_CLIPS_PER_SESSION = 16
        const val BUCKET_DURATION_MS = 5 * 60_000L
        const val LIVE_SYNC_INTERVAL_MS = 15_000L
        const val STALE_SESSION_TIMEOUT_MS = 90_000L
        const val STORE_FILE_NAME = "sleep-monitor-store.json"
    }
}
