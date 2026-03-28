package com.codex.sleepmonitor

import android.app.Application
import com.codex.sleepmonitor.data.SleepRepository

class SleepMonitorApp : Application() {
    val repository: SleepRepository by lazy { SleepRepository(this) }
}
