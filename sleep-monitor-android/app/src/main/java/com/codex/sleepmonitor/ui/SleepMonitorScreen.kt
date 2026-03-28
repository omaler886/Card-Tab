package com.codex.sleepmonitor.ui

import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.codex.sleepmonitor.data.AnomalyClip
import com.codex.sleepmonitor.data.SoothingSoundType
import com.codex.sleepmonitor.data.SleepStore
import kotlinx.coroutines.delay

@Composable
fun SleepMonitorScreen(
    store: SleepStore,
    onStartRequested: () -> Unit,
    onStopRequested: () -> Unit,
    onStartNapRequested: (Int) -> Unit,
    onCancelNapRequested: () -> Unit,
    onStartSmartWakeRequested: (Int, Int) -> Unit,
    onCancelSmartWakeRequested: () -> Unit,
    onStartWhiteNoiseRequested: (SoothingSoundType, Int, Int?) -> Unit,
    onStopWhiteNoiseRequested: () -> Unit,
    onStartAutoStopRequested: (Int) -> Unit,
    onCancelAutoStopRequested: () -> Unit,
    onSetBedtimePlanRequested: (Int, Int, Boolean) -> Unit,
    onClearBedtimePlanRequested: () -> Unit,
    onStopAlarmRequested: () -> Unit,
    onCaffeineChanged: (Int) -> Unit,
    onStressChanged: (Int) -> Unit,
    onExerciseChanged: (Int) -> Unit,
    onLateMealToggled: () -> Unit,
    onAlcoholToggled: () -> Unit,
    onRecalibrateRequested: () -> Unit,
    onShareReportRequested: () -> Unit,
    onExportWeeklyPdfRequested: () -> Unit,
    onExportWeeklyImageRequested: () -> Unit,
    onExportEncryptedBackupRequested: () -> Unit,
    onImportEncryptedBackupRequested: () -> Unit,
    onOpenSettings: (Intent) -> Unit
) {
    val now by rememberTicker()
    val active = store.activeSession
    val focusSession = active ?: store.sessions.firstOrNull()
    val insights = focusSession?.let { buildSessionInsights(it, now) }
    val events = focusSession?.let(::sessionEvents).orEmpty()
    val onsetPoints = buildOnsetBarPoints(active, store.sessions, now)
    val weeklyTrends = buildWeeklyTrendPoints(store.sessions, now)
    val correlations = buildCorrelationInsights(store.sessions, now)
    val calibrationSummary = buildCalibrationSummary(store.calibrationProfile)
    val alarmTriggered = store.activeNapPlan?.triggeredAtMillis != null ||
        store.activeSmartWakePlan?.triggeredAtMillis != null

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingClipId by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun playClip(clip: AnomalyClip) {
        if (playingClipId == clip.id) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            playingClipId = null
            return
        }

        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            MediaPlayer().apply {
                setDataSource(clip.filePath)
                prepare()
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    playingClipId = null
                }
                start()
            }
        }.onSuccess { player ->
            mediaPlayer = player
            playingClipId = clip.id
        }.onFailure {
            mediaPlayer = null
            playingClipId = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D162E),
                        Color(0xFF173758),
                        Color(0xFFF1F7FC)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderBlock()
            HeroCard(active, insights, now, onStartRequested, onStopRequested)
            SleepLogCard(store.draftSleepLog, onCaffeineChanged, onStressChanged, onExerciseChanged, onLateMealToggled, onAlcoholToggled)
            BedtimePlanCard(store.bedtimePlan, onSetBedtimePlanRequested, onClearBedtimePlanRequested)
            SmartWakeCard(store.activeSmartWakePlan, store.activeSmartWakePlan?.triggeredAtMillis != null, now, onStartSmartWakeRequested, onCancelSmartWakeRequested, onStopAlarmRequested)
            NapAlarmCard(store.activeNapPlan, now, onStartNapRequested, onCancelNapRequested, onStopAlarmRequested)
            WhiteNoiseCard(store.activeWhiteNoisePlan, now, onStartWhiteNoiseRequested, onStopWhiteNoiseRequested)
            AutoStopCard(store.activeAutoStopPlan, now, onStartAutoStopRequested, onCancelAutoStopRequested)
            LiveSignalsCard(active, focusSession, events)
            SleepAnalysisCard(insights, onsetPoints)
            WeeklyTrendCard(
                weeklyTrends,
                correlations,
                onShareReportRequested,
                onExportWeeklyPdfRequested,
                onExportWeeklyImageRequested
            )
            AnomalyTimelineCard(focusSession, events)
            ClipPlaybackCard(focusSession?.anomalyClips?.takeLast(5)?.reversed().orEmpty(), playingClipId, ::playClip)
            CalibrationCard(calibrationSummary, store.calibrationProfile.nightsAnalyzed, onRecalibrateRequested)
            BackupCard(onExportEncryptedBackupRequested, onImportEncryptedBackupRequested)
            SleepSuggestionsCard(insights)
            GuidanceCard(alarmTriggered, onStopAlarmRequested, onOpenSettings)
            SessionHistoryCard(store.sessions, now)
        }
    }
}

@Composable
private fun rememberTicker() = produceState(initialValue = System.currentTimeMillis()) {
    while (true) {
        delay(1_000)
        value = System.currentTimeMillis()
    }
}
