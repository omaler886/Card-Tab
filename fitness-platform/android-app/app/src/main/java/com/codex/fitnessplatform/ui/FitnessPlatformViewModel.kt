package com.codex.fitnessplatform.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codex.fitnessplatform.data.FitnessRepository
import com.codex.fitnessplatform.data.GoalMetric
import com.codex.fitnessplatform.data.MealType
import com.codex.fitnessplatform.data.UserProfile
import com.codex.fitnessplatform.data.WorkoutSessionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FitnessPlatformViewModel(
    private val repository: FitnessRepository
) : ViewModel() {
    val store = repository.store

    init {
        viewModelScope.launch {
            while (true) {
                delay(5 * 60_000L)
                repository.runAgentAudit()
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch { repository.register(name, email, password) }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch { repository.login(email, password) }
    }

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }

    fun clearNotice() {
        viewModelScope.launch { repository.clearNotice() }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch { repository.updateProfile(profile) }
    }

    fun addGoal(title: String, metric: GoalMetric, currentValue: Double, targetValue: Double, deadline: String) {
        viewModelScope.launch { repository.addGoal(title, metric, currentValue, targetValue, deadline) }
    }

    fun updateGoalCurrent(goalId: String, currentValue: Double) {
        viewModelScope.launch { repository.updateGoalCurrent(goalId, currentValue) }
    }

    fun addWorkoutLog(
        date: String,
        sessionType: WorkoutSessionType,
        durationMin: Int,
        intensityRpe: Int,
        caloriesBurned: Int,
        notes: String
    ) {
        viewModelScope.launch {
            repository.addWorkoutLog(date, sessionType, durationMin, intensityRpe, caloriesBurned, notes)
        }
    }

    fun toggleWorkoutLog(logId: String) {
        viewModelScope.launch { repository.toggleWorkoutLog(logId) }
    }

    fun addFoodLog(
        date: String,
        mealType: MealType,
        title: String,
        kcal: Int,
        proteinG: Int,
        carbsG: Int,
        fatG: Int,
        notes: String
    ) {
        viewModelScope.launch {
            repository.addFoodLog(date, mealType, title, kcal, proteinG, carbsG, fatG, notes)
        }
    }

    fun regeneratePlans() {
        viewModelScope.launch { repository.regeneratePlans() }
    }

    fun runAgentAudit() {
        viewModelScope.launch { repository.runAgentAudit() }
    }

    class Factory(
        private val repository: FitnessRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FitnessPlatformViewModel(repository) as T
        }
    }
}
