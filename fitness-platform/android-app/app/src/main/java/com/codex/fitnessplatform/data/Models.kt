package com.codex.fitnessplatform.data

import kotlinx.serialization.Serializable

@Serializable
enum class GoalType {
    FAT_LOSS,
    MUSCLE_GAIN,
    ENDURANCE,
    HEALTH
}

@Serializable
enum class ExperienceLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

@Serializable
enum class EquipmentAccess {
    BODYWEIGHT,
    HOME_GYM,
    FULL_GYM
}

@Serializable
enum class RecoveryLevel {
    LOW,
    MEDIUM,
    HIGH
}

@Serializable
enum class Sex {
    MALE,
    FEMALE
}

@Serializable
enum class GoalMetric {
    WEIGHT,
    BODY_FAT,
    STRENGTH,
    DISTANCE,
    CONSISTENCY
}

@Serializable
data class UserProfile(
    val name: String = "Alex",
    val age: Int = 30,
    val sex: Sex = Sex.MALE,
    val heightCm: Int = 178,
    val weightKg: Int = 80,
    val bodyFatPct: Int = 19,
    val goalType: GoalType = GoalType.MUSCLE_GAIN,
    val level: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val equipment: EquipmentAccess = EquipmentAccess.FULL_GYM,
    val daysPerWeek: Int = 5,
    val recovery: RecoveryLevel = RecoveryLevel.MEDIUM,
    val sleepHours: Double = 7.2,
    val stepsPerDay: Int = 8000,
    val injuries: String = ""
)

@Serializable
data class GoalItem(
    val id: String,
    val title: String,
    val metric: GoalMetric,
    val targetValue: Double,
    val currentValue: Double,
    val unit: String,
    val deadline: String
)

@Serializable
data class ExercisePrescription(
    val name: String,
    val sets: Int,
    val reps: String,
    val restSec: Int,
    val intensityCue: String
)

@Serializable
data class WorkoutDayPlan(
    val day: String,
    val focus: String,
    val strengthMinutes: Int,
    val cardioMinutes: Int,
    val mobilityMinutes: Int,
    val exercises: List<ExercisePrescription> = emptyList()
)

@Serializable
data class TrainingPlan(
    val split: String = "",
    val weeklyResistanceSetsPerMuscle: Int = 0,
    val weeklyModerateCardioMinutes: Int = 0,
    val progressionRule: String = "",
    val deloadRule: String = "",
    val generatedAt: String = "",
    val days: List<WorkoutDayPlan> = emptyList()
)

@Serializable
data class MealItem(
    val label: String,
    val foods: List<String>,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val kcal: Int
)

@Serializable
data class NutritionPlan(
    val dailyKcal: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
    val hydrationLiters: Double = 0.0,
    val notes: List<String> = emptyList(),
    val meals: List<MealItem> = emptyList()
)

@Serializable
enum class WorkoutSessionType {
    STRENGTH,
    CARDIO,
    MOBILITY,
    MIXED
}

@Serializable
data class WorkoutLog(
    val id: String,
    val date: String,
    val sessionType: WorkoutSessionType,
    val durationMin: Int,
    val intensityRpe: Int,
    val caloriesBurned: Int,
    val completed: Boolean,
    val notes: String
)

@Serializable
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK
}

@Serializable
data class FoodLogEntry(
    val id: String,
    val date: String,
    val mealType: MealType,
    val title: String,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val kcal: Int,
    val notes: String = ""
)

@Serializable
data class StandardApplication(
    val standard: String,
    val principle: String,
    val appliedRule: String
)

data class WeeklyBar(
    val date: String,
    val duration: Int,
    val pct: Int
)

data class WeeklyAnalytics(
    val totalDuration: Int,
    val completionRate: Int,
    val avgRpe: Double,
    val bars: List<WeeklyBar> = emptyList()
)

@Serializable
enum class AgentRole {
    AUTH,
    TRAINING,
    NUTRITION,
    TRACKING,
    GOAL_COACH
}

@Serializable
enum class AgentStatus {
    RUNNING,
    COMPLETED,
    STALLED,
    FAILED
}

@Serializable
enum class MasterHealth {
    HEALTHY,
    WATCHING,
    RECOVERING
}

@Serializable
data class SubAgentState(
    val id: String,
    val role: AgentRole,
    val assignedTask: String,
    val status: AgentStatus,
    val lastActiveAtMillis: Long,
    val restartCount: Int = 0,
    val latestReport: String = ""
)

@Serializable
data class MasterAgentState(
    val lastAuditAtMillis: Long = 0L,
    val health: MasterHealth = MasterHealth.WATCHING,
    val summary: String = "Waiting for bootstrap.",
    val agents: List<SubAgentState> = emptyList()
)

@Serializable
data class UserAccount(
    val id: String,
    val displayName: String,
    val email: String,
    val passwordHash: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val profile: UserProfile = UserProfile(),
    val goals: List<GoalItem> = emptyList(),
    val workoutLogs: List<WorkoutLog> = emptyList(),
    val foodLogs: List<FoodLogEntry> = emptyList(),
    val trainingPlan: TrainingPlan = TrainingPlan(),
    val nutritionPlan: NutritionPlan = NutritionPlan(),
    val standards: List<StandardApplication> = emptyList(),
    val coachFeedback: List<String> = emptyList(),
    val masterAgent: MasterAgentState = MasterAgentState()
)

@Serializable
data class SessionState(
    val currentAccountId: String? = null,
    val notice: String? = null
)

@Serializable
data class FitnessStore(
    val accounts: List<UserAccount> = emptyList(),
    val session: SessionState = SessionState()
)

fun FitnessStore.currentAccount(): UserAccount? {
    val accountId = session.currentAccountId ?: return null
    return accounts.firstOrNull { it.id == accountId }
}
