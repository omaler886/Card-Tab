package com.codex.sleepmonitor.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.codex.sleepmonitor.MainActivity
import com.codex.sleepmonitor.R
import com.codex.sleepmonitor.SleepMonitorApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BedtimeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as SleepMonitorApp
                val repository = app.repository
                val plan = repository.store.value.bedtimePlan ?: return@launch
                if (!plan.enabled) {
                    return@launch
                }

                val nextTrigger = BedtimeScheduler.computeNextTrigger(plan.hour, plan.minute)
                repository.updateBedtimePlan(plan.hour, plan.minute, plan.autoStart, nextTrigger)
                BedtimeScheduler.schedule(context, repository.store.value.bedtimePlan ?: return@launch)

                val audioGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (plan.autoStart && audioGranted) {
                    SleepMonitoringService.start(context)
                    showReminderNotification(context, autoStarted = true)
                } else {
                    showReminderNotification(context, autoStarted = false)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showReminderNotification(context: Context, autoStarted: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "NightPulse Bedtime",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "睡前提醒和计划自动开始通知"
            }
        )

        val openIntent = PendingIntent.getActivity(
            context,
            6010,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val startNowIntent = PendingIntent.getService(
            context,
            6011,
            Intent(context, SleepMonitoringService::class.java).setAction("com.codex.sleepmonitor.action.START_AUDIO"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val content = if (autoStarted) {
            "已按你的计划自动开始今晚的睡眠监控。"
        } else {
            "到计划就寝时间了，建议准备入睡。"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_moon)
            .setContentTitle("NightPulse 睡前计划")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .addAction(0, "开始监测", startNowIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "sleep-monitor-bedtime-channel"
        const val NOTIFICATION_ID = 4050
    }
}
