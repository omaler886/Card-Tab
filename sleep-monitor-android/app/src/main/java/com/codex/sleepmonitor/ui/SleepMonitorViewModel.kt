package com.codex.sleepmonitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codex.sleepmonitor.data.SleepRepository
import kotlinx.coroutines.launch

class SleepMonitorViewModel(
    private val repository: SleepRepository
) : ViewModel() {
    val store = repository.store

    fun updateCaffeine(cups: Int) {
        viewModelScope.launch {
            repository.updateDraftSleepLog(caffeineCups = cups)
        }
    }

    fun updateStress(level: Int) {
        viewModelScope.launch {
            repository.updateDraftSleepLog(stressLevel = level)
        }
    }

    fun updateExercise(minutes: Int) {
        viewModelScope.launch {
            repository.updateDraftSleepLog(exerciseMinutes = minutes)
        }
    }

    fun toggleLateMeal() {
        val current = repository.store.value.draftSleepLog.lateMeal
        viewModelScope.launch {
            repository.updateDraftSleepLog(lateMeal = !current)
        }
    }

    fun toggleAlcohol() {
        val current = repository.store.value.draftSleepLog.alcohol
        viewModelScope.launch {
            repository.updateDraftSleepLog(alcohol = !current)
        }
    }

    fun recalibrate() {
        viewModelScope.launch {
            repository.recalibrate()
        }
    }

    fun setBedtimePlan(hour: Int, minute: Int, autoStart: Boolean, nextTriggerAtMillis: Long) {
        viewModelScope.launch {
            repository.updateBedtimePlan(hour, minute, autoStart, nextTriggerAtMillis)
        }
    }

    fun clearBedtimePlan() {
        viewModelScope.launch {
            repository.clearBedtimePlan()
        }
    }

    class Factory(
        private val repository: SleepRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SleepMonitorViewModel(repository) as T
        }
    }
}
