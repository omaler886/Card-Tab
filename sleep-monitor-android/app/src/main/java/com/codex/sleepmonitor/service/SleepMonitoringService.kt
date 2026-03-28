package com.codex.sleepmonitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.codex.sleepmonitor.MainActivity
import com.codex.sleepmonitor.R
import com.codex.sleepmonitor.SleepMonitorApp
import com.codex.sleepmonitor.data.AnomalyClip
import com.codex.sleepmonitor.data.AutoStopPlan
import com.codex.sleepmonitor.data.SleepEvent
import com.codex.sleepmonitor.data.SleepEventType
import com.codex.sleepmonitor.data.SleepSession
import com.codex.sleepmonitor.data.SoothingSoundType
import com.codex.sleepmonitor.data.SmartWakePlan
import com.codex.sleepmonitor.data.WhiteNoisePlan
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SleepMonitoringService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val analyzer = SleepAudioAnalyzer()

    private lateinit var wakeLock: PowerManager.WakeLock
    private var recorder: AudioRecord? = null
    private var rollingPcmBuffer = RollingPcmBuffer(SAMPLE_RATE * CLIP_BUFFER_SECONDS)
    private var monitorJob: Job? = null
    private var notificationJob: Job? = null
    private var napJob: Job? = null
    private var smartWakeJob: Job? = null
    private var autoStopJob: Job? = null
    private var whiteNoiseTimerJob: Job? = null
    private var alarmRingtone: Ringtone? = null
    private var activeAlarmType: AlarmType? = null
    private val whiteNoisePlayer = WhiteNoisePlayer()

    override fun onCreate() {
        super.onCreate()
        createChannels()
        wakeLock = (
            getSystemService(Context.POWER_SERVICE) as PowerManager
            ).newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "sleep-monitor:monitoring"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_AUDIO -> startAudioMonitoring()
            ACTION_STOP_AUDIO -> stopAudioMonitoring()
            ACTION_START_WHITE_NOISE -> startWhiteNoise(
                soundType = intent.getStringExtra(EXTRA_WHITE_NOISE_TYPE)
                    ?.let { runCatching { SoothingSoundType.valueOf(it) }.getOrNull() }
                    ?: SoothingSoundType.WHITE_NOISE,
                volumePercent = intent.getIntExtra(EXTRA_WHITE_NOISE_VOLUME, DEFAULT_WHITE_NOISE_VOLUME),
                durationMinutes = intent.getIntExtra(EXTRA_WHITE_NOISE_DURATION, -1).takeIf { it > 0 }
            )
            ACTION_STOP_WHITE_NOISE -> stopWhiteNoise()
            ACTION_START_NAP -> startNapTimer(
                intent.getIntExtra(EXTRA_NAP_DURATION_MINUTES, DEFAULT_NAP_MINUTES)
            )
            ACTION_CANCEL_NAP -> cancelNapTimer()
            ACTION_START_AUTO_STOP -> startAutoStopTimer(
                intent.getIntExtra(EXTRA_AUTO_STOP_HOURS, DEFAULT_AUTO_STOP_HOURS)
            )
            ACTION_CANCEL_AUTO_STOP -> cancelAutoStopTimer()
            ACTION_START_SMART_WAKE -> startSmartWakeTimer(
                durationHours = intent.getIntExtra(EXTRA_SMART_WAKE_HOURS, DEFAULT_SMART_WAKE_HOURS),
                windowMinutes = intent.getIntExtra(EXTRA_SMART_WAKE_WINDOW, DEFAULT_SMART_WAKE_WINDOW_MINUTES)
            )
            ACTION_CANCEL_SMART_WAKE -> cancelSmartWakeTimer()
            ACTION_STOP_ALARM -> dismissActiveAlarm()
            else -> restoreStateFromStore()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun restoreStateFromStore() {
        val store = (application as SleepMonitorApp).repository.store.value
        analyzer.setCalibration(store.calibrationProfile)
        if (store.activeSession != null) {
            startAudioMonitoring()
        }
        store.activeWhiteNoisePlan?.let(::syncWhiteNoisePlan)
        store.activeNapPlan?.let(::scheduleNapPlan)
        store.activeAutoStopPlan?.let(::scheduleAutoStopPlan)
        store.activeSmartWakePlan?.let(::scheduleSmartWakePlan)
        when {
            store.activeNapPlan?.triggeredAtMillis != null -> triggerAlarm(AlarmType.NAP)
            store.activeSmartWakePlan?.triggeredAtMillis != null -> triggerAlarm(AlarmType.SMART_WAKE)
        }
        ensureServiceState()
    }

    private fun startAudioMonitoring() {
        ensureForegroundNotification()
        if (monitorJob?.isActive == true) {
            return
        }

        acquireWakeLockIfNeeded()
        rollingPcmBuffer = RollingPcmBuffer(SAMPLE_RATE * CLIP_BUFFER_SECONDS)
        monitorJob = serviceScope.launch {
            val repository = (application as SleepMonitorApp).repository
            analyzer.setCalibration(repository.store.value.calibrationProfile)
            val session = repository.startSession()

            val minBuffer = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            if (minBuffer <= 0) {
                repository.stopSession()
                ensureServiceState()
                return@launch
            }

            val readBufferSize = max(minBuffer / 2, SAMPLE_RATE / 2)
            val readBuffer = ShortArray(readBufferSize)

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                max(minBuffer, readBufferSize * 2)
            )

            recorder = audioRecord
            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                repository.stopSession()
                ensureServiceState()
                return@launch
            }

            try {
                audioRecord.startRecording()
                while (isActive) {
                    val read = audioRecord.read(readBuffer, 0, readBuffer.size)
                    val now = System.currentTimeMillis()
                    if (read > 0) {
                        rollingPcmBuffer.append(readBuffer, read)
                        val frame = analyzer.analyze(readBuffer, read, now)
                        repository.updateLiveFrame(frame, now)
                        repository.registerEvents(frame.events, now)
                        frame.events.forEach { event ->
                            captureClipForEvent(sessionId = session.id, event = event, now = now)
                        }
                    } else {
                        delay(40)
                    }
                }
            } catch (_: SecurityException) {
                repository.stopSession()
            } finally {
                releaseRecorder()
                ensureServiceState()
            }
        }
    }

    private fun stopAudioMonitoring() {
        releaseRecorder()
        monitorJob?.cancel()
        smartWakeJob?.cancel()
        serviceScope.launch {
            val repository = (application as SleepMonitorApp).repository
            repository.cancelSmartWakePlan()
            repository.cancelAutoStopPlan()
            repository.stopSession()
            ensureServiceState()
        }
    }

    private fun startWhiteNoise(
        soundType: SoothingSoundType,
        volumePercent: Int,
        durationMinutes: Int?
    ) {
        ensureForegroundNotification()
        acquireWakeLockIfNeeded()
        serviceScope.launch {
            val repository = (application as SleepMonitorApp).repository
            repository.startWhiteNoise(soundType, volumePercent, durationMinutes)
            repository.store.value.activeWhiteNoisePlan?.let(::syncWhiteNoisePlan)
            ensureServiceState()
        }
    }

    private fun syncWhiteNoisePlan(plan: WhiteNoisePlan) {
        ensureForegroundNotification()
        acquireWakeLockIfNeeded()
        whiteNoisePlayer.start(serviceScope, plan.soundType, plan.volumePercent)
        whiteNoiseTimerJob?.cancel()
        whiteNoiseTimerJob = if (plan.stopAtMillis != null) {
            serviceScope.launch {
                while (isActive) {
                    val remaining = plan.stopAtMillis - System.currentTimeMillis()
                    if (remaining <= 0L) {
                        stopWhiteNoise()
                        break
                    }
                    if (remaining <= WHITE_NOISE_FADE_WINDOW_MS && whiteNoisePlayer.isPlaying()) {
                        whiteNoisePlayer.fadeOutAndStop(remaining.coerceAtLeast(1_000L))
                        (application as SleepMonitorApp).repository.cancelWhiteNoise()
                        ensureServiceState()
                        break
                    }
                    delay(minOf(1_000L, remaining))
                }
            }
        } else {
            null
        }
    }

    private fun stopWhiteNoise() {
        whiteNoiseTimerJob?.cancel()
        serviceScope.launch {
            whiteNoisePlayer.fadeOutAndStop(WHITE_NOISE_MANUAL_FADE_MS)
            (application as SleepMonitorApp).repository.cancelWhiteNoise()
            ensureServiceState()
        }
    }

    private fun startNapTimer(durationMinutes: Int) {
        val safeDuration = durationMinutes.coerceIn(10, 90)
        serviceScope.launch {
            val repository = (application as SleepMonitorApp).repository
            repository.startNapPlan(safeDuration)
            repository.store.value.activeNapPlan?.let(::scheduleNapPlan)
            ensureServiceState()
        }
    }

    private fun scheduleNapPlan(plan: com.codex.sleepmonitor.data.NapPlan) {
        ensureForegroundNotification()
        acquireWakeLockIfNeeded()
        napJob?.cancel()

        if (plan.triggeredAtMillis != null) {
            triggerAlarm(AlarmType.NAP)
            return
        }

        napJob = serviceScope.launch {
            while (isActive) {
                val remaining = plan.wakeAtMillis - System.currentTimeMillis()
                if (remaining <= 0L) {
                    (application as SleepMonitorApp).repository.triggerNap()
                    triggerAlarm(AlarmType.NAP)
                    break
                }
                delay(minOf(1_000L, remaining))
            }
        }
    }

    private fun cancelNapTimer() {
        napJob?.cancel()
        serviceScope.launch {
            (application as SleepMonitorApp).repository.cancelNapPlan()
            if (activeAlarmType == AlarmType.NAP) {
                dismissActiveAlarm()
            } else {
                ensureServiceState()
            }
        }
    }

    private fun startAutoStopTimer(durationHours: Int) {
        startAudioMonitoring()
        serviceScope.launch {
            val repository = (application as SleepMonitorApp).repository
            repository.startAutoStopPlan(durationHours)
            repository.store.value.activeAutoStopPlan?.let(::scheduleAutoStopPlan)
            ensureServiceState()
        }
    }

    private fun scheduleAutoStopPlan(plan: AutoStopPlan) {
        ensureForegroundNotification()
        acquireWakeLockIfNeeded()
        autoStopJob?.cancel()
        autoStopJob = serviceScope.launch {
            while (isActive) {
                val remaining = plan.stopAtMillis - System.currentTimeMillis()
                if (remaining <= 0L) {
                    NotificationManagerCompat.from(this@SleepMonitoringService)
                        .notify(
                            AUTO_STOP_NOTIFICATION_ID,
                            NotificationCompat.Builder(this@SleepMonitoringService, CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_notification_moon)
                                .setContentTitle("NightPulse 已自动结束监测")
                                .setContentText("达到预设时长，监测与白噪音已自动结束。")
                                .setAutoCancel(true)
                                .build()
                        )
                    stopWhiteNoise()
                    stopAudioMonitoring()
                    break
                }
                delay(minOf(1_000L, remaining))
            }
        }
    }

    private fun cancelAutoStopTimer() {
        autoStopJob?.cancel()
        serviceScope.launch {
            (application as SleepMonitorApp).repository.cancelAutoStopPlan()
            ensureServiceState()
        }
    }

    private fun startSmartWakeTimer(durationHours: Int, windowMinutes: Int) {
        val safeHours = durationHours.coerceIn(4, 12)
        val safeWindow = windowMinutes.coerceIn(10, 60)
        startAudioMonitoring()
        serviceScope.launch {
            val repository = (application as SleepMonitorApp).repository
            repository.startSmartWakePlan(safeHours, safeWindow)
            repository.store.value.activeSmartWakePlan?.let(::scheduleSmartWakePlan)
            ensureServiceState()
        }
    }

    private fun scheduleSmartWakePlan(plan: SmartWakePlan) {
        ensureForegroundNotification()
        acquireWakeLockIfNeeded()
        smartWakeJob?.cancel()

        if (plan.triggeredAtMillis != null) {
            triggerAlarm(AlarmType.SMART_WAKE)
            return
        }

        smartWakeJob = serviceScope.launch {
            while (isActive) {
                val repository = (application as SleepMonitorApp).repository
                val currentPlan = repository.store.value.activeSmartWakePlan ?: break
                val session = repository.store.value.activeSession
                val now = System.currentTimeMillis()

                if (now >= currentPlan.targetWakeAtMillis || shouldTriggerSmartWake(session, currentPlan, now)) {
                    repository.triggerSmartWake(now)
                    triggerAlarm(AlarmType.SMART_WAKE)
                    break
                }

                delay(SMART_WAKE_POLL_MS)
            }
        }
    }

    private fun cancelSmartWakeTimer() {
        smartWakeJob?.cancel()
        serviceScope.launch {
            (application as SleepMonitorApp).repository.cancelSmartWakePlan()
            if (activeAlarmType == AlarmType.SMART_WAKE) {
                dismissActiveAlarm()
            } else {
                ensureServiceState()
            }
        }
    }

    private fun shouldTriggerSmartWake(
        session: SleepSession?,
        plan: SmartWakePlan,
        now: Long
    ): Boolean {
        session ?: return false
        val windowStart = plan.targetWakeAtMillis - plan.windowMinutes * 60_000L
        if (now < windowStart) {
            return false
        }

        val recentBuckets = session.soundBuckets.takeLast(2)
        val recentEvents = session.sleepEvents.filter { now - it.timestampMillis <= 10 * 60_000L }
        val recentDisturbance = recentBuckets.map { it.disturbanceScore }.average().toFloat()
        val recentAmbient = recentEvents.any { it.type == SleepEventType.AMBIENT_ALERT || it.type == SleepEventType.DREAM_TALK }

        return recentDisturbance in 0.18f..0.60f || recentAmbient
    }

    private fun triggerAlarm(type: AlarmType) {
        ensureForegroundNotification()
        activeAlarmType = type
        NotificationManagerCompat.from(this).notify(ALARM_NOTIFICATION_ID, buildAlarmNotification(type))

        if (alarmRingtone?.isPlaying == true) {
            return
        }

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        alarmRingtone = RingtoneManager.getRingtone(this, uri)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isLooping = true
            }
            play()
        }
    }

    private fun dismissActiveAlarm() {
        stopAlarmSound()
        NotificationManagerCompat.from(this).cancel(ALARM_NOTIFICATION_ID)
        val currentAlarm = activeAlarmType
        activeAlarmType = null
        serviceScope.launch {
            val repository = (application as SleepMonitorApp).repository
            if (currentAlarm == AlarmType.NAP) {
                repository.cancelNapPlan()
            }
            if (currentAlarm == AlarmType.SMART_WAKE) {
                repository.cancelSmartWakePlan()
            }
            ensureServiceState()
        }
    }

    private fun stopAlarmSound() {
        alarmRingtone?.stop()
        alarmRingtone = null
    }

    private fun captureClipForEvent(sessionId: String, event: SleepEvent, now: Long) {
        serviceScope.launch(Dispatchers.IO) {
            val samples = rollingPcmBuffer.snapshot(SAMPLE_RATE * CLIP_EXPORT_SECONDS)
            if (samples.isEmpty()) {
                return@launch
            }

            val clipDir = File(filesDir, "clips/$sessionId")
            val fileName = "${event.type.name.lowercase(Locale.US)}-$now.wav"
            val file = File(clipDir, fileName)
            WavWriter.writePcm16Mono(file, SAMPLE_RATE, samples)

            val clip = AnomalyClip(
                id = "clip-$now-${event.type.name.lowercase(Locale.US)}",
                eventType = event.type,
                capturedAtMillis = event.timestampMillis,
                durationMillis = samples.size * 1000L / SAMPLE_RATE,
                filePath = file.absolutePath,
                peakDb = event.peakDb
            )
            (application as SleepMonitorApp).repository.registerClip(clip)
        }
    }

    private fun ensureServiceState() {
        val store = (application as SleepMonitorApp).repository.store.value
        val shouldStayAlive = store.activeSession != null ||
            store.activeNapPlan != null ||
            store.activeSmartWakePlan != null ||
            store.activeWhiteNoisePlan != null ||
            store.activeAutoStopPlan != null ||
            alarmRingtone?.isPlaying == true

        if (shouldStayAlive) {
            ensureForegroundNotification()
            acquireWakeLockIfNeeded()
        } else {
            notificationJob?.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
            stopSelf()
        }
    }

    private fun ensureForegroundNotification() {
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        if (notificationJob?.isActive == true) {
            return
        }
        notificationJob = serviceScope.launch {
            while (isActive) {
                NotificationManagerCompat.from(this@SleepMonitoringService)
                    .notify(NOTIFICATION_ID, buildForegroundNotification())
                delay(2_000)
            }
        }
    }

    private fun acquireWakeLockIfNeeded() {
        if (!wakeLock.isHeld) {
            wakeLock.acquire(WAKELOCK_TIMEOUT_MS)
        }
    }

    private fun releaseRecorder() {
        recorder?.runCatching { stop() }
        recorder?.release()
        recorder = null
    }

    private fun buildForegroundNotification(): Notification {
        val store = (application as SleepMonitorApp).repository.store.value
        val session = store.activeSession
        val napPlan = store.activeNapPlan
        val smartWakePlan = store.activeSmartWakePlan
        val whiteNoisePlan = store.activeWhiteNoisePlan
        val autoStopPlan = store.activeAutoStopPlan
        val eventCounts = session?.sleepEvents?.groupingBy { it.type }?.eachCount().orEmpty()

        val title = when {
            activeAlarmType == AlarmType.SMART_WAKE -> "NightPulse 智能唤醒触发"
            activeAlarmType == AlarmType.NAP -> "NightPulse 午睡时间到了"
            session != null && smartWakePlan != null -> "NightPulse 监测中 · 智能唤醒已开启"
            session != null && napPlan != null -> "NightPulse 监测中 · 午睡唤醒已开启"
            session != null && autoStopPlan != null -> "NightPulse 监测中 · 自动结束已设定"
            whiteNoisePlan != null && session == null -> "NightPulse 白噪音助眠运行中"
            session != null -> "NightPulse 正在监测睡眠"
            smartWakePlan != null -> "NightPulse 智能唤醒运行中"
            napPlan != null -> "NightPulse 午睡唤醒器运行中"
            else -> "NightPulse"
        }

        val contentText = when {
            activeAlarmType == AlarmType.SMART_WAKE -> "已进入更易醒的时刻，点击通知停止铃声。"
            activeAlarmType == AlarmType.NAP -> "午睡时间到了，点击通知停止铃声。"
            session != null && smartWakePlan != null -> {
                "已监控 ${formatDuration(System.currentTimeMillis() - session.startedAtMillis)} · " +
                    "目标 ${formatClock(smartWakePlan.targetWakeAtMillis)}"
            }
            session != null && napPlan != null -> {
                "已监控 ${formatDuration(System.currentTimeMillis() - session.startedAtMillis)} · " +
                    "午睡剩余 ${formatDuration(napPlan.wakeAtMillis - System.currentTimeMillis())}"
            }
            session != null && autoStopPlan != null -> {
                "已监控 ${formatDuration(System.currentTimeMillis() - session.startedAtMillis)} · " +
                    "自动结束 ${formatClock(autoStopPlan.stopAtMillis)}"
            }
            session != null -> {
                "鼾声 ${eventCounts[SleepEventType.SNORE] ?: 0} · 梦话 ${eventCounts[SleepEventType.DREAM_TALK] ?: 0} · " +
                    "磨牙 ${eventCounts[SleepEventType.TEETH_GRINDING] ?: 0} · 环境 ${eventCounts[SleepEventType.AMBIENT_ALERT] ?: 0}"
            }
            whiteNoisePlan != null -> {
                "${soundTypeLabel(whiteNoisePlan.soundType)} · 音量 ${whiteNoisePlan.volumePercent}% · " +
                    (whiteNoisePlan.stopAtMillis?.let { "停止 ${formatClock(it)}" } ?: "持续播放")
            }
            smartWakePlan != null -> {
                "目标 ${formatClock(smartWakePlan.targetWakeAtMillis)} · 窗口 ${smartWakePlan.windowMinutes} 分钟"
            }
            napPlan != null -> "剩余 ${formatDuration(napPlan.wakeAtMillis - System.currentTimeMillis())}"
            else -> "等待开始"
        }

        val secondaryActionLabel = when {
            smartWakePlan != null -> "取消唤醒"
            napPlan != null -> "取消午睡"
            else -> "智能唤醒"
        }
        val secondaryIntent = when {
            smartWakePlan != null -> cancelSmartWakeIntent()
            napPlan != null -> cancelNapIntent()
            else -> startSmartWakeIntent(DEFAULT_SMART_WAKE_HOURS, DEFAULT_SMART_WAKE_WINDOW_MINUTES)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_moon)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openMainIntent())
            .addAction(
                0,
                if (session != null) "结束监测" else "打开应用",
                if (session != null) stopAudioIntent() else openMainIntent()
            )
            .addAction(0, secondaryActionLabel, secondaryIntent)
            .build()
    }

    private fun buildAlarmNotification(type: AlarmType): Notification {
        val contentText = when (type) {
            AlarmType.NAP -> "时间到了，建议现在起身，避免午睡过长。"
            AlarmType.SMART_WAKE -> "已经进入更易醒的时间点，建议现在结束睡眠。"
        }

        return NotificationCompat.Builder(this, CHANNEL_ALARM_ID)
            .setSmallIcon(R.drawable.ic_notification_moon)
            .setContentTitle(
                when (type) {
                    AlarmType.NAP -> "NightPulse 午睡唤醒"
                    AlarmType.SMART_WAKE -> "NightPulse 智能唤醒"
                }
            )
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$contentText 点击“停止铃声”结束提醒。"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(openMainIntent())
            .addAction(0, "停止铃声", dismissAlarmIntent())
            .build()
    }

    private fun openMainIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun stopAudioIntent(): PendingIntent {
        return PendingIntent.getService(
            this,
            2,
            Intent(this, SleepMonitoringService::class.java).setAction(ACTION_STOP_AUDIO),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun startNapIntent(durationMinutes: Int): PendingIntent {
        return PendingIntent.getService(
            this,
            3 + durationMinutes,
            Intent(this, SleepMonitoringService::class.java)
                .setAction(ACTION_START_NAP)
                .putExtra(EXTRA_NAP_DURATION_MINUTES, durationMinutes),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelNapIntent(): PendingIntent {
        return PendingIntent.getService(
            this,
            98,
            Intent(this, SleepMonitoringService::class.java).setAction(ACTION_CANCEL_NAP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun startSmartWakeIntent(durationHours: Int, windowMinutes: Int): PendingIntent {
        return PendingIntent.getService(
            this,
            120 + durationHours,
            Intent(this, SleepMonitoringService::class.java)
                .setAction(ACTION_START_SMART_WAKE)
                .putExtra(EXTRA_SMART_WAKE_HOURS, durationHours)
                .putExtra(EXTRA_SMART_WAKE_WINDOW, windowMinutes),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelSmartWakeIntent(): PendingIntent {
        return PendingIntent.getService(
            this,
            140,
            Intent(this, SleepMonitoringService::class.java).setAction(ACTION_CANCEL_SMART_WAKE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun dismissAlarmIntent(): PendingIntent {
        return PendingIntent.getService(
            this,
            99,
            Intent(this, SleepMonitoringService::class.java).setAction(ACTION_STOP_ALARM),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannels() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "NightPulse Monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "睡眠监测、智能唤醒与午睡计时常驻通知"
            setShowBadge(false)
        }

        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM_ID,
            "NightPulse Alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "午睡与智能唤醒提醒"
            enableVibration(true)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(alarmChannel)
    }

    override fun onDestroy() {
        releaseRecorder()
        stopAlarmSound()
        whiteNoisePlayer.stopImmediate()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "sleep-monitor-channel"
        private const val CHANNEL_ALARM_ID = "sleep-monitor-alarm-channel"
        private const val NOTIFICATION_ID = 4040
        private const val ALARM_NOTIFICATION_ID = 4041
        private const val AUTO_STOP_NOTIFICATION_ID = 4042
        private const val WAKELOCK_TIMEOUT_MS = 12L * 60L * 60L * 1000L
        private const val SAMPLE_RATE = 16_000
        private const val CLIP_BUFFER_SECONDS = 8
        private const val CLIP_EXPORT_SECONDS = 6
        private const val DEFAULT_NAP_MINUTES = 20
        private const val DEFAULT_WHITE_NOISE_VOLUME = 35
        private const val DEFAULT_AUTO_STOP_HOURS = 8
        private const val WHITE_NOISE_FADE_WINDOW_MS = 60_000L
        private const val WHITE_NOISE_MANUAL_FADE_MS = 8_000L
        private const val DEFAULT_SMART_WAKE_HOURS = 8
        private const val DEFAULT_SMART_WAKE_WINDOW_MINUTES = 30
        private const val SMART_WAKE_POLL_MS = 30_000L
        private const val EXTRA_NAP_DURATION_MINUTES = "extra_nap_duration_minutes"
        private const val EXTRA_WHITE_NOISE_TYPE = "extra_white_noise_type"
        private const val EXTRA_WHITE_NOISE_VOLUME = "extra_white_noise_volume"
        private const val EXTRA_WHITE_NOISE_DURATION = "extra_white_noise_duration"
        private const val EXTRA_AUTO_STOP_HOURS = "extra_auto_stop_hours"
        private const val EXTRA_SMART_WAKE_HOURS = "extra_smart_wake_hours"
        private const val EXTRA_SMART_WAKE_WINDOW = "extra_smart_wake_window"
        private const val ACTION_START_AUDIO = "com.codex.sleepmonitor.action.START_AUDIO"
        private const val ACTION_STOP_AUDIO = "com.codex.sleepmonitor.action.STOP_AUDIO"
        private const val ACTION_START_WHITE_NOISE = "com.codex.sleepmonitor.action.START_WHITE_NOISE"
        private const val ACTION_STOP_WHITE_NOISE = "com.codex.sleepmonitor.action.STOP_WHITE_NOISE"
        private const val ACTION_START_NAP = "com.codex.sleepmonitor.action.START_NAP"
        private const val ACTION_CANCEL_NAP = "com.codex.sleepmonitor.action.CANCEL_NAP"
        private const val ACTION_START_AUTO_STOP = "com.codex.sleepmonitor.action.START_AUTO_STOP"
        private const val ACTION_CANCEL_AUTO_STOP = "com.codex.sleepmonitor.action.CANCEL_AUTO_STOP"
        private const val ACTION_START_SMART_WAKE = "com.codex.sleepmonitor.action.START_SMART_WAKE"
        private const val ACTION_CANCEL_SMART_WAKE = "com.codex.sleepmonitor.action.CANCEL_SMART_WAKE"
        private const val ACTION_STOP_ALARM = "com.codex.sleepmonitor.action.STOP_ALARM"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SleepMonitoringService::class.java).setAction(ACTION_START_AUDIO)
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, SleepMonitoringService::class.java).setAction(ACTION_STOP_AUDIO)
            )
        }

        fun startNap(context: Context, durationMinutes: Int) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SleepMonitoringService::class.java)
                    .setAction(ACTION_START_NAP)
                    .putExtra(EXTRA_NAP_DURATION_MINUTES, durationMinutes)
            )
        }

        fun startWhiteNoise(
            context: Context,
            soundType: SoothingSoundType,
            volumePercent: Int,
            durationMinutes: Int? = null
        ) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SleepMonitoringService::class.java)
                    .setAction(ACTION_START_WHITE_NOISE)
                    .putExtra(EXTRA_WHITE_NOISE_TYPE, soundType.name)
                    .putExtra(EXTRA_WHITE_NOISE_VOLUME, volumePercent)
                    .putExtra(EXTRA_WHITE_NOISE_DURATION, durationMinutes ?: -1)
            )
        }

        fun stopWhiteNoise(context: Context) {
            context.startService(
                Intent(context, SleepMonitoringService::class.java).setAction(ACTION_STOP_WHITE_NOISE)
            )
        }

        fun cancelNap(context: Context) {
            context.startService(
                Intent(context, SleepMonitoringService::class.java).setAction(ACTION_CANCEL_NAP)
            )
        }

        fun startAutoStop(context: Context, durationHours: Int) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SleepMonitoringService::class.java)
                    .setAction(ACTION_START_AUTO_STOP)
                    .putExtra(EXTRA_AUTO_STOP_HOURS, durationHours)
            )
        }

        fun cancelAutoStop(context: Context) {
            context.startService(
                Intent(context, SleepMonitoringService::class.java).setAction(ACTION_CANCEL_AUTO_STOP)
            )
        }

        fun startSmartWake(context: Context, durationHours: Int, windowMinutes: Int = DEFAULT_SMART_WAKE_WINDOW_MINUTES) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SleepMonitoringService::class.java)
                    .setAction(ACTION_START_SMART_WAKE)
                    .putExtra(EXTRA_SMART_WAKE_HOURS, durationHours)
                    .putExtra(EXTRA_SMART_WAKE_WINDOW, windowMinutes)
            )
        }

        fun cancelSmartWake(context: Context) {
            context.startService(
                Intent(context, SleepMonitoringService::class.java).setAction(ACTION_CANCEL_SMART_WAKE)
            )
        }

        fun stopAlarm(context: Context) {
            context.startService(
                Intent(context, SleepMonitoringService::class.java).setAction(ACTION_STOP_ALARM)
            )
        }

        private fun formatDuration(durationMillis: Long): String {
            val safe = durationMillis.coerceAtLeast(0L)
            val totalSeconds = safe / 1_000
            val hours = totalSeconds / 3_600
            val minutes = (totalSeconds % 3_600) / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        }

        private fun formatClock(timestampMillis: Long): String {
            return Instant.ofEpochMilli(timestampMillis)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        }

        private fun soundTypeLabel(type: SoothingSoundType): String = when (type) {
            SoothingSoundType.WHITE_NOISE -> "白噪音"
            SoothingSoundType.RAIN -> "雨声"
            SoothingSoundType.OCEAN -> "海浪"
            SoothingSoundType.FAN -> "风扇"
        }
    }
}

private enum class AlarmType {
    NAP,
    SMART_WAKE
}
