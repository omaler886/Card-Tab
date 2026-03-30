package com.codex.fitnessplatform

import android.app.Application
import com.codex.fitnessplatform.data.FitnessRepository

class FitnessPlatformApp : Application() {
    val repository: FitnessRepository by lazy { FitnessRepository(this) }
}
