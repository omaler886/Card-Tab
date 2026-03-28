package com.codex.sleepmonitor.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.codex.sleepmonitor.data.BedtimePlan
import java.time.LocalDateTime
import java.time.ZoneId

object BedtimeScheduler {
    fun computeNextTrigger(hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), zone)
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return next.atZone(zone).toInstant().toEpochMilli()
    }

    fun schedule(context: Context, plan: BedtimePlan) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            plan.nextTriggerAtMillis,
            bedtimeIntent(context)
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(bedtimeIntent(context))
    }

    private fun bedtimeIntent(context: Context): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            5010,
            Intent(context, BedtimeAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
