package com.codex.sleepmonitor.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.codex.sleepmonitor.SleepMonitorApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = (context.applicationContext as SleepMonitorApp).repository
                val plan = repository.store.value.bedtimePlan ?: return@launch
                if (!plan.enabled) {
                    return@launch
                }

                val nextTrigger = BedtimeScheduler.computeNextTrigger(plan.hour, plan.minute)
                repository.updateBedtimePlan(plan.hour, plan.minute, plan.autoStart, nextTrigger)
                BedtimeScheduler.schedule(context, repository.store.value.bedtimePlan ?: return@launch)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
