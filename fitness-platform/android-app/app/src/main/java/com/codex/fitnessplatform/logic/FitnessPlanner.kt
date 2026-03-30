package com.codex.fitnessplatform.logic

import com.codex.fitnessplatform.data.AgentRole
import com.codex.fitnessplatform.data.AgentStatus
import com.codex.fitnessplatform.data.EquipmentAccess
import com.codex.fitnessplatform.data.ExercisePrescription
import com.codex.fitnessplatform.data.ExperienceLevel
import com.codex.fitnessplatform.data.FoodLogEntry
import com.codex.fitnessplatform.data.GoalItem
import com.codex.fitnessplatform.data.GoalMetric
import com.codex.fitnessplatform.data.GoalType
import com.codex.fitnessplatform.data.MasterAgentState
import com.codex.fitnessplatform.data.MasterHealth
import com.codex.fitnessplatform.data.MealItem
import com.codex.fitnessplatform.data.MealType
import com.codex.fitnessplatform.data.NutritionPlan
import com.codex.fitnessplatform.data.RecoveryLevel
import com.codex.fitnessplatform.data.StandardApplication
import com.codex.fitnessplatform.data.SubAgentState
import com.codex.fitnessplatform.data.Sex
import com.codex.fitnessplatform.data.TrainingPlan
import com.codex.fitnessplatform.data.UserProfile
import com.codex.fitnessplatform.data.WeeklyAnalytics
import com.codex.fitnessplatform.data.WeeklyBar
import com.codex.fitnessplatform.data.WorkoutDayPlan
import com.codex.fitnessplatform.data.WorkoutLog
import com.codex.fitnessplatform.data.WorkoutSessionType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val WEEK_DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private val exerciseLibrary: Map<EquipmentAccess, Map<GoalType, List<String>>> = mapOf(
    EquipmentAccess.BODYWEIGHT to mapOf(
        GoalType.FAT_LOSS to listOf("Bodyweight Squat", "Push-up", "Reverse Lunge", "Plank Shoulder Tap", "Mountain Climber", "Glute Bridge"),
        GoalType.MUSCLE_GAIN to listOf("Tempo Push-up", "Bulgarian Split Squat", "Pike Push-up", "Single-leg Hip Thrust", "Assisted Chin-up", "Slow Eccentric Squat"),
        GoalType.ENDURANCE to listOf("Step-up", "Scaled Burpee", "Walking Lunge", "Jump Rope", "High Knee March", "Bear Crawl"),
        GoalType.HEALTH to listOf("Squat to Box", "Wall Push-up", "Bird Dog", "Dead Bug", "Hip Hinge Drill", "Sit-to-Stand")
    ),
    EquipmentAccess.HOME_GYM to mapOf(
        GoalType.FAT_LOSS to listOf("Dumbbell Goblet Squat", "Dumbbell Bench Press", "One-arm Row", "Romanian Deadlift", "Kettlebell Swing", "Bike Intervals"),
        GoalType.MUSCLE_GAIN to listOf("Barbell Back Squat", "Barbell Bench Press", "Overhead Press", "Barbell Row", "Romanian Deadlift", "Pull-up"),
        GoalType.ENDURANCE to listOf("Dumbbell Thruster", "Kettlebell Swing", "Row Erg Sprint", "Farmer Carry", "Cycling Intervals", "Sled Push"),
        GoalType.HEALTH to listOf("Goblet Squat", "Machine Chest Press", "Cable Row", "Leg Curl", "Elliptical", "Mobility Flow")
    ),
    EquipmentAccess.FULL_GYM to mapOf(
        GoalType.FAT_LOSS to listOf("Front Squat", "Incline Dumbbell Press", "Lat Pulldown", "Romanian Deadlift", "Cable Woodchop", "Treadmill Incline Walk"),
        GoalType.MUSCLE_GAIN to listOf("Back Squat", "Bench Press", "Deadlift", "Weighted Pull-up", "Incline Press", "Seated Row"),
        GoalType.ENDURANCE to listOf("Row Erg", "Ski Erg", "Air Bike Intervals", "Walking Lunge", "Battle Rope", "Circuit Medley"),
        GoalType.HEALTH to listOf("Leg Press", "Machine Row", "Cable Press", "Hamstring Curl", "Bike Zone 2", "Mobility Circuit")
    )
)

private val goalCalorieAdjustment = mapOf(
    GoalType.FAT_LOSS to -420,
    GoalType.MUSCLE_GAIN to 270,
    GoalType.ENDURANCE to 120,
    GoalType.HEALTH to 0
)

private val goalCardioTarget = mapOf(
    GoalType.FAT_LOSS to 220,
    GoalType.MUSCLE_GAIN to 120,
    GoalType.ENDURANCE to 280,
    GoalType.HEALTH to 170
)

private val levelSetBase = mapOf(
    ExperienceLevel.BEGINNER to 10,
    ExperienceLevel.INTERMEDIATE to 14,
    ExperienceLevel.ADVANCED to 18
)

private val recoveryModifier = mapOf(
    RecoveryLevel.LOW to -3,
    RecoveryLevel.MEDIUM to 0,
    RecoveryLevel.HIGH to 2
)

private const val AGENT_TIMEOUT_MS = 5 * 60_000L

fun todayIsoDate(): String = LocalDate.now(ZoneId.systemDefault()).toString()

fun createDefaultProfile(name: String): UserProfile = UserProfile(name = name.ifBlank { "Alex" })

fun seedGoals(today: LocalDate = LocalDate.now()): List<GoalItem> = listOf(
    GoalItem("goal-bodyfat", "Body fat to 15%", GoalMetric.BODY_FAT, 15.0, 19.0, "%", today.plusMonths(4).toString()),
    GoalItem("goal-bench", "Bench press 100kg", GoalMetric.STRENGTH, 100.0, 85.0, "kg", today.plusMonths(5).toString())
)

fun seedWorkoutLogs(today: LocalDate = LocalDate.now()): List<WorkoutLog> = buildList {
    repeat(6) { index ->
        add(
            WorkoutLog(
                id = "seed-log-$index",
                date = today.minusDays(index.toLong()).toString(),
                sessionType = if (index % 2 == 0) WorkoutSessionType.STRENGTH else WorkoutSessionType.CARDIO,
                durationMin = if (index % 2 == 0) 58 else 40,
                intensityRpe = if (index % 2 == 0) 7 else 6,
                caloriesBurned = if (index % 2 == 0) 360 else 280,
                completed = true,
                notes = if (index % 2 == 0) "Main lifts felt stable." else "Intervals completed at target pace."
            )
        )
    }
}

fun seedFoodLogs(today: LocalDate = LocalDate.now()): List<FoodLogEntry> = listOf(
    FoodLogEntry("food-breakfast", today.toString(), MealType.BREAKFAST, "Greek yogurt oats bowl", 32, 48, 14, 450, "High-protein start before work."),
    FoodLogEntry("food-lunch", today.minusDays(1).toString(), MealType.LUNCH, "Chicken rice bowl", 42, 65, 16, 610, "Good recovery meal post training.")
)

fun generateTrainingPlan(profile: UserProfile): TrainingPlan {
    val baseSets = (levelSetBase[profile.level] ?: 10) + (recoveryModifier[profile.recovery] ?: 0)
    val weeklySetsPerMuscle = max(8, baseSets)
    val goalCardio = goalCardioTarget[profile.goalType] ?: 150
    val sleepPenalty = if (profile.sleepHours < 7.0) -20 else 0
    val weeklyModerateCardioMinutes = max(90, goalCardio + sleepPenalty)

    return TrainingPlan(
        split = getSplitName(profile.daysPerWeek),
        weeklyResistanceSetsPerMuscle = weeklySetsPerMuscle,
        weeklyModerateCardioMinutes = weeklyModerateCardioMinutes,
        progressionRule = "If average RPE <= 7 and technique is stable, increase load 2.5-5% or add 1 rep next week.",
        deloadRule = "Every 5th week reduce volume about 30%, shift emphasis to mobility and recovery.",
        generatedAt = todayIsoDate(),
        days = buildTrainingDays(profile, weeklySetsPerMuscle, weeklyModerateCardioMinutes)
    )
}

fun generateNutritionPlan(profile: UserProfile, foodLogs: List<FoodLogEntry> = emptyList()): NutritionPlan {
    val bmr = getBmr(profile)
    val tdee = bmr * getActivityMultiplier(profile.daysPerWeek, profile.stepsPerDay)
    val targetKcal = max(1400, round10(tdee + (goalCalorieAdjustment[profile.goalType] ?: 0).toDouble()).toInt())

    val proteinMultiplier = when (profile.goalType) {
        GoalType.MUSCLE_GAIN -> 2.0
        GoalType.FAT_LOSS -> 2.1
        else -> 1.7
    }
    val proteinG = round10(profile.weightKg * proteinMultiplier).toInt()
    val fatG = round10(profile.weightKg * 0.8).toInt()
    val remainingCalories = targetKcal - (proteinG * 4 + fatG * 9)
    val carbsG = round10(max(80.0, remainingCalories / 4.0)).toInt()

    val recentLogs = foodLogs.filter { isWithinLastDays(it.date, 3) }
    val dailyProteinAvg = if (recentLogs.isEmpty()) 0.0 else recentLogs.sumOf { it.proteinG }.toDouble() / 3.0
    val dailyCaloriesAvg = if (recentLogs.isEmpty()) 0.0 else recentLogs.sumOf { it.kcal }.toDouble() / 3.0

    val notes = buildList {
        add("Prioritize 25-40g protein per meal to support recovery and muscle retention.")
        add("Distribute carbs around training sessions for performance and glycogen replenishment.")
        add("Aim for 80% minimally processed foods and 20% flexible intake.")
        add("Keep sodium and potassium balanced, especially on high sweat days.")
        if (recentLogs.isNotEmpty()) {
            add("Recent intake average: ${dailyCaloriesAvg.roundToInt()} kcal/day and ${dailyProteinAvg.roundToInt()}g protein/day.")
        }
        if (recentLogs.isNotEmpty() && dailyProteinAvg < proteinG * 0.7) {
            add("Protein intake is below target. Add one high-protein snack or larger lean-protein portions.")
        }
    }

    return NutritionPlan(
        dailyKcal = targetKcal,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG,
        hydrationLiters = max(2.2, (profile.weightKg * 0.035).let { (it * 10).roundToInt() / 10.0 }),
        notes = notes,
        meals = listOf(
            MealItem("Breakfast", listOf("Oats + Greek yogurt + berries", "2 eggs or tofu scramble"), (proteinG * 0.3).roundToInt(), (carbsG * 0.3).roundToInt(), (fatG * 0.25).roundToInt(), (targetKcal * 0.3).roundToInt()),
            MealItem("Lunch", listOf("Lean protein bowl", "Rice or quinoa", "Mixed vegetables + olive oil"), (proteinG * 0.3).roundToInt(), (carbsG * 0.33).roundToInt(), (fatG * 0.3).roundToInt(), (targetKcal * 0.35).roundToInt()),
            MealItem("Dinner", listOf("Fish, chicken or legumes", "Potato or whole grain", "Large salad"), (proteinG * 0.3).roundToInt(), (carbsG * 0.27).roundToInt(), (fatG * 0.35).roundToInt(), (targetKcal * 0.3).roundToInt()),
            MealItem("Training Snack", listOf("Banana + whey or soy isolate"), max(15, (proteinG * 0.1).roundToInt()), max(20, (carbsG * 0.1).roundToInt()), max(5, (fatG * 0.1).roundToInt()), (targetKcal * 0.1).roundToInt())
        )
    )
}

fun getStandardApplications(profile: UserProfile, plan: TrainingPlan, nutrition: NutritionPlan): List<StandardApplication> = listOf(
    StandardApplication("WHO Physical Activity Guideline", "Adults should accumulate 150-300 min moderate or 75-150 min vigorous activity weekly.", "Cardio target is ${plan.weeklyModerateCardioMinutes} min/week based on goal, sleep and recovery."),
    StandardApplication("ACSM Resistance Training Position Stand", "Train major muscle groups 2-3+ days/week with progressive overload.", "${profile.daysPerWeek} sessions/week and ${plan.weeklyResistanceSetsPerMuscle} sets per muscle group are scheduled."),
    StandardApplication("NSCA Volume Guidance", "Training volume and intensity should scale with level and recovery capacity.", "Volume adapts to level ${profile.level.name.lowercase()} and recovery ${profile.recovery.name.lowercase()}."),
    StandardApplication("NASM Recovery Principles", "Include mobility, recovery planning and deload structure to reduce injury risk.", "Each session includes ${plan.days.firstOrNull()?.mobilityMinutes ?: 12} min mobility and every 5th week is reduced."),
    StandardApplication("USDA / Sports Nutrition Consensus", "Calories and macros should reflect body mass, training load and goal.", "${nutrition.dailyKcal} kcal with P/C/F ${nutrition.proteinG}/${nutrition.carbsG}/${nutrition.fatG}g.")
)

fun calculateGoalProgress(goal: GoalItem): Int {
    if (goal.targetValue == goal.currentValue) return 100
    val totalDelta = abs(goal.targetValue - goal.currentValue)
    if (totalDelta == 0.0) return 100
    val isDecreaseGoal = goal.metric == GoalMetric.WEIGHT || goal.metric == GoalMetric.BODY_FAT
    val syntheticCurrent = if (isDecreaseGoal) goal.currentValue - totalDelta * 0.45 else goal.currentValue + totalDelta * 0.45
    val done = abs(syntheticCurrent - goal.currentValue)
    return ((done / totalDelta) * 100).roundToInt().coerceIn(5, 95)
}

fun getWeeklyAnalytics(logs: List<WorkoutLog>, today: LocalDate = LocalDate.now()): WeeklyAnalytics {
    val last7Days = linkedMapOf<String, Int>()
    repeat(7) { index -> last7Days[today.minusDays(index.toLong()).toString()] = 0 }

    var completedCount = 0
    var intensitySum = 0
    var loggedIntensityCount = 0

    logs.forEach { log ->
        if (last7Days.containsKey(log.date)) {
            last7Days[log.date] = (last7Days[log.date] ?: 0) + log.durationMin
        }
        if (log.completed) completedCount += 1
        if (log.intensityRpe > 0) {
            intensitySum += log.intensityRpe
            loggedIntensityCount += 1
        }
    }

    val formatter = DateTimeFormatter.ofPattern("MM-dd")
    return WeeklyAnalytics(
        totalDuration = last7Days.values.sum(),
        completionRate = if (logs.isEmpty()) 0 else ((completedCount.toDouble() / logs.size) * 100).roundToInt(),
        avgRpe = if (loggedIntensityCount == 0) 0.0 else ((intensitySum.toDouble() / loggedIntensityCount) * 10).roundToInt() / 10.0,
        bars = last7Days.entries.sortedBy { it.key }.map { (date, duration) ->
            WeeklyBar(LocalDate.parse(date).format(formatter), duration, ((duration / 120.0) * 100).roundToInt().coerceAtMost(100))
        }
    )
}

fun generateCoachFeedback(profile: UserProfile, goals: List<GoalItem>, logs: List<WorkoutLog>, foodLogs: List<FoodLogEntry>, nutritionPlan: NutritionPlan): List<String> {
    val analytics = getWeeklyAnalytics(logs)
    val goalAverage = if (goals.isEmpty()) 0 else goals.map(::calculateGoalProgress).average().roundToInt()
    val recentFoodLogs = foodLogs.filter { isWithinLastDays(it.date, 3) }
    val dailyCaloriesAverage = if (recentFoodLogs.isEmpty()) 0 else recentFoodLogs.sumOf { it.kcal } / 3
    val dailyProteinAverage = if (recentFoodLogs.isEmpty()) 0 else recentFoodLogs.sumOf { it.proteinG } / 3

    return buildList {
        if (analytics.completionRate < 70) add("Consistency is the current bottleneck. Drop one session or reduce volume until completion rate moves above 70%.")
        if (analytics.totalDuration < profile.daysPerWeek * 35) add("Weekly training duration is low for your target. Add one short conditioning or mobility block this week.")
        if (profile.goalType == GoalType.FAT_LOSS && dailyCaloriesAverage > 0 && dailyCaloriesAverage > nutritionPlan.dailyKcal * 1.1) add("Recent calorie intake is above the fat-loss target. Tighten portion control at dinner or snacks.")
        if (dailyProteinAverage > 0 && dailyProteinAverage < nutritionPlan.proteinG * 0.75) add("Protein intake is trailing target. Add 25-30g protein to breakfast or a post-workout snack.")
        if (goalAverage >= 70) add("Average goal progress is strong. Keep current training load and review progression weekly.")
        if (none()) add("Current plan is balanced. Focus on sleep quality, hit your scheduled sessions and keep meal logging consistent.")
    }
}

fun defaultMasterAgent(now: Long = System.currentTimeMillis()): MasterAgentState {
    val agents = AgentRole.entries.map { role ->
        SubAgentState(
            id = "${role.name.lowercase()}-$now",
            role = role,
            assignedTask = defaultTaskFor(role),
            status = AgentStatus.RUNNING,
            lastActiveAtMillis = now,
            latestReport = "Waiting for first workload."
        )
    }
    return MasterAgentState(now, MasterHealth.WATCHING, "Master agent initialized ${agents.size} subagents.", agents)
}

fun ensureAgentTopology(masterAgent: MasterAgentState, now: Long = System.currentTimeMillis()): MasterAgentState {
    if (masterAgent.agents.size == AgentRole.entries.size) return masterAgent
    val existingByRole = masterAgent.agents.associateBy { it.role }
    val normalized = AgentRole.entries.map { role ->
        existingByRole[role] ?: SubAgentState("${role.name.lowercase()}-$now", role, defaultTaskFor(role), AgentStatus.RUNNING, now, latestReport = "Recovered missing agent slot.")
    }
    return masterAgent.copy(agents = normalized)
}

fun recordAgentActivity(masterAgent: MasterAgentState, role: AgentRole, report: String, now: Long = System.currentTimeMillis(), status: AgentStatus = AgentStatus.COMPLETED): MasterAgentState {
    val normalized = ensureAgentTopology(masterAgent, now)
    return normalized.copy(
        agents = normalized.agents.map { agent ->
            if (agent.role == role) agent.copy(status = status, lastActiveAtMillis = now, latestReport = report) else agent
        },
        summary = "${role.name.lowercase().replace('_', ' ')} agent reported: $report"
    )
}

fun auditAgentHealth(masterAgent: MasterAgentState, now: Long = System.currentTimeMillis()): MasterAgentState {
    val normalized = ensureAgentTopology(masterAgent, now)
    var restarted = 0
    val updatedAgents = normalized.agents.map { agent ->
        val stale = now - agent.lastActiveAtMillis >= AGENT_TIMEOUT_MS
        if (stale || agent.status == AgentStatus.FAILED) {
            restarted += 1
            agent.copy(
                id = "${agent.role.name.lowercase()}-$now-${agent.restartCount + 1}",
                status = AgentStatus.RUNNING,
                lastActiveAtMillis = now,
                restartCount = agent.restartCount + 1,
                latestReport = "Restarted by master agent after timeout or failure."
            )
        } else if (now - agent.lastActiveAtMillis >= AGENT_TIMEOUT_MS / 2 && agent.status == AgentStatus.RUNNING) {
            agent.copy(status = AgentStatus.STALLED)
        } else {
            agent
        }
    }

    val health = when {
        restarted > 0 -> MasterHealth.RECOVERING
        updatedAgents.any { it.status == AgentStatus.STALLED } -> MasterHealth.WATCHING
        else -> MasterHealth.HEALTHY
    }
    val summary = when {
        restarted > 0 -> "Master agent restarted $restarted subagent(s) during the latest audit."
        health == MasterHealth.WATCHING -> "Master agent is monitoring agents with low recent activity."
        else -> "All subagents are active and aligned with their current task."
    }
    return normalized.copy(lastAuditAtMillis = now, health = health, summary = summary, agents = updatedAgents)
}

private fun getActivityMultiplier(daysPerWeek: Int, stepsPerDay: Int): Double = when {
    daysPerWeek <= 2 && stepsPerDay < 6000 -> 1.3
    daysPerWeek <= 3 && stepsPerDay < 8000 -> 1.45
    daysPerWeek <= 5 && stepsPerDay < 11000 -> 1.6
    else -> 1.75
}

private fun getBmr(profile: UserProfile): Double {
    val base = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age
    return if (profile.sex == Sex.MALE) base + 5 else base - 161
}

private fun round10(value: Double): Double = (value / 10.0).roundToInt() * 10.0

private fun pickRepRange(goal: GoalType): String = when (goal) {
    GoalType.MUSCLE_GAIN -> "6-12"
    GoalType.ENDURANCE -> "12-20"
    GoalType.FAT_LOSS -> "8-15"
    GoalType.HEALTH -> "8-12"
}

private fun getSplitName(daysPerWeek: Int): String = when {
    daysPerWeek <= 3 -> "Full-body split"
    daysPerWeek <= 5 -> "Upper/Lower + Conditioning"
    else -> "Push/Pull/Legs + Athletic days"
}

private fun buildDayFocus(index: Int, daysPerWeek: Int, goalType: GoalType): String {
    if (daysPerWeek <= 3) return listOf("Full-body A", "Full-body B", "Full-body C").getOrElse(index) { "Full-body" }
    if (daysPerWeek <= 5) return listOf("Upper Strength", "Lower Strength", "Conditioning", "Upper Hypertrophy", "Lower + Core").getOrElse(index) { "Conditioning" }
    val labels = listOf("Push Strength", "Pull Strength", "Leg Strength", "Conditioning Power", "Push Hypertrophy", "Pull Hypertrophy", "Aerobic Base")
    val current = labels.getOrElse(index) { "Mixed" }
    return if (goalType == GoalType.ENDURANCE && current == "Conditioning Power") "Threshold Conditioning" else current
}

private fun buildExercises(equipment: EquipmentAccess, goalType: GoalType, daysPerWeek: Int, dayIndex: Int, weeklySetsPerMuscle: Int): List<ExercisePrescription> {
    val library = exerciseLibrary[equipment]?.get(goalType).orEmpty()
    val reps = pickRepRange(goalType)
    val setsPerExercise = max(2, (weeklySetsPerMuscle / (daysPerWeek * 1.8)).roundToInt())
    val shifted = library.drop(dayIndex) + library.take(dayIndex)
    return shifted.take(4).mapIndexed { index, name ->
        ExercisePrescription(
            name = name,
            sets = setsPerExercise + if (index == 0) 1 else 0,
            reps = reps,
            restSec = when (goalType) {
                GoalType.ENDURANCE -> 45
                GoalType.FAT_LOSS -> 60
                else -> 90
            },
            intensityCue = when (goalType) {
                GoalType.MUSCLE_GAIN -> "RPE 7-9, keep 1-3 reps in reserve"
                GoalType.ENDURANCE -> "RPE 6-8, sustainable pace"
                else -> "RPE 6-8 with clean technique"
            }
        )
    }
}

private fun buildTrainingDays(profile: UserProfile, weeklySetsPerMuscle: Int, weeklyCardioMinutes: Int): List<WorkoutDayPlan> {
    val cardioPerSession = (weeklyCardioMinutes / profile.daysPerWeek.toDouble()).roundToInt()
    return WEEK_DAYS.take(profile.daysPerWeek).mapIndexed { index, day ->
        val focus = buildDayFocus(index, profile.daysPerWeek, profile.goalType)
        WorkoutDayPlan(
            day = day,
            focus = focus,
            strengthMinutes = if (profile.goalType == GoalType.ENDURANCE) 35 else 50,
            cardioMinutes = cardioPerSession + if (focus.contains("Conditioning")) 12 else 0,
            mobilityMinutes = if (profile.recovery == RecoveryLevel.LOW) 18 else 12,
            exercises = buildExercises(profile.equipment, profile.goalType, profile.daysPerWeek, index, weeklySetsPerMuscle)
        )
    }
}

private fun defaultTaskFor(role: AgentRole): String = when (role) {
    AgentRole.AUTH -> "Validate registration and login flow."
    AgentRole.TRAINING -> "Generate and refresh the user's training plan."
    AgentRole.NUTRITION -> "Review meal logs and adjust nutrition guidance."
    AgentRole.TRACKING -> "Aggregate workout logs and recent analytics."
    AgentRole.GOAL_COACH -> "Assess goal progress and write personalized feedback."
}

private fun isWithinLastDays(date: String, days: Long): Boolean {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull() ?: return false
    return !parsed.isBefore(LocalDate.now().minusDays(days - 1))
}

fun newId(prefix: String): String = "$prefix-${UUID.randomUUID()}"
