package com.codex.fitnessplatform.data

import android.app.Application
import com.codex.fitnessplatform.logic.auditAgentHealth
import com.codex.fitnessplatform.logic.createDefaultProfile
import com.codex.fitnessplatform.logic.defaultMasterAgent
import com.codex.fitnessplatform.logic.generateCoachFeedback
import com.codex.fitnessplatform.logic.generateNutritionPlan
import com.codex.fitnessplatform.logic.generateTrainingPlan
import com.codex.fitnessplatform.logic.getStandardApplications
import com.codex.fitnessplatform.logic.newId
import com.codex.fitnessplatform.logic.recordAgentActivity
import com.codex.fitnessplatform.logic.seedFoodLogs
import com.codex.fitnessplatform.logic.seedGoals
import com.codex.fitnessplatform.logic.seedWorkoutLogs
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FitnessRepository(application: Application) {
    private val mutex = Mutex()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val storeFile = File(application.filesDir, STORE_FILE_NAME)
    private val _store = MutableStateFlow(loadInitialStore())

    val store: StateFlow<FitnessStore> = _store.asStateFlow()

    suspend fun register(name: String, email: String, password: String) = mutex.withLock {
        val safeName = name.trim().ifBlank { "Athlete" }
        val safeEmail = email.trim().lowercase()
        if (safeEmail.isBlank() || !safeEmail.contains("@")) {
            publish(_store.value.copy(session = _store.value.session.copy(notice = "请输入有效邮箱。")))
            return@withLock
        }
        if (password.length < 6) {
            publish(_store.value.copy(session = _store.value.session.copy(notice = "密码至少 6 位。")))
            return@withLock
        }
        if (_store.value.accounts.any { it.email == safeEmail }) {
            publish(_store.value.copy(session = _store.value.session.copy(notice = "该邮箱已注册。")))
            return@withLock
        }

        val now = System.currentTimeMillis()
        val baseAccount = UserAccount(
            id = newId("account"),
            displayName = safeName,
            email = safeEmail,
            passwordHash = hashPassword(password),
            createdAtMillis = now,
            updatedAtMillis = now,
            profile = createDefaultProfile(safeName),
            goals = seedGoals(),
            workoutLogs = seedWorkoutLogs(),
            foodLogs = seedFoodLogs(),
            masterAgent = defaultMasterAgent(now)
        )
        val prepared = rebuildAccount(
            account = baseAccount,
            now = now,
            agentUpdates = listOf(
                AgentUpdate(AgentRole.AUTH, "Registration completed and user session established."),
                AgentUpdate(AgentRole.TRAINING, "Initial training plan generated."),
                AgentUpdate(AgentRole.NUTRITION, "Initial nutrition plan generated."),
                AgentUpdate(AgentRole.TRACKING, "Seed workout history loaded."),
                AgentUpdate(AgentRole.GOAL_COACH, "Initial coaching feedback created.")
            )
        )
        publish(
            _store.value.copy(
                accounts = listOf(prepared) + _store.value.accounts,
                session = SessionState(
                    currentAccountId = prepared.id,
                    notice = "注册成功，已生成你的首套训练和饮食方案。"
                )
            )
        )
    }

    suspend fun login(email: String, password: String) = mutex.withLock {
        val safeEmail = email.trim().lowercase()
        val account = _store.value.accounts.firstOrNull { it.email == safeEmail }
        if (account == null || account.passwordHash != hashPassword(password)) {
            publish(_store.value.copy(session = _store.value.session.copy(notice = "邮箱或密码不正确。")))
            return@withLock
        }

        val now = System.currentTimeMillis()
        val refreshed = rebuildAccount(
            account = account,
            now = now,
            agentUpdates = listOf(AgentUpdate(AgentRole.AUTH, "User login verified and session recovered."))
        )
        publish(
            _store.value.copy(
                accounts = replaceAccount(_store.value.accounts, refreshed),
                session = SessionState(currentAccountId = refreshed.id, notice = "已登录，欢迎回来。")
            )
        )
    }

    suspend fun logout() = mutex.withLock {
        publish(_store.value.copy(session = SessionState(notice = "已退出登录。")))
    }

    suspend fun clearNotice() = mutex.withLock {
        if (_store.value.session.notice != null) {
            publish(_store.value.copy(session = _store.value.session.copy(notice = null)))
        }
    }

    suspend fun updateProfile(profile: UserProfile) = mutateCurrentAccount(
        notice = "画像已更新，训练和营养计划已重算。",
        agentUpdates = listOf(
            AgentUpdate(AgentRole.TRAINING, "Training plan recalculated from latest profile."),
            AgentUpdate(AgentRole.NUTRITION, "Nutrition targets recalculated from latest profile.")
        )
    ) { account ->
        account.copy(profile = sanitizeProfile(profile, account.displayName))
    }

    suspend fun addGoal(
        title: String,
        metric: GoalMetric,
        currentValue: Double,
        targetValue: Double,
        deadline: String
    ) = mutateCurrentAccount(
        notice = "已新增目标并更新进度监控。",
        agentUpdates = listOf(AgentUpdate(AgentRole.GOAL_COACH, "Goal portfolio changed and progress monitoring refreshed."))
    ) { account ->
        if (title.isBlank()) return@mutateCurrentAccount account
        account.copy(
            goals = listOf(
                GoalItem(
                    id = newId("goal"),
                    title = title.trim(),
                    metric = metric,
                    currentValue = currentValue,
                    targetValue = targetValue,
                    unit = metricUnit(metric),
                    deadline = deadline.trim()
                )
            ) + account.goals
        )
    }

    suspend fun updateGoalCurrent(goalId: String, currentValue: Double) = mutateCurrentAccount(
        notice = "目标进度已更新。",
        agentUpdates = listOf(AgentUpdate(AgentRole.GOAL_COACH, "Goal progress value changed and feedback refreshed."))
    ) { account ->
        account.copy(
            goals = account.goals.map { goal ->
                if (goal.id == goalId) goal.copy(currentValue = currentValue) else goal
            }
        )
    }

    suspend fun addWorkoutLog(
        date: String,
        sessionType: WorkoutSessionType,
        durationMin: Int,
        intensityRpe: Int,
        caloriesBurned: Int,
        notes: String
    ) = mutateCurrentAccount(
        notice = "训练记录已写入，分析已更新。",
        agentUpdates = listOf(
            AgentUpdate(AgentRole.TRACKING, "Workout log appended and weekly analytics rebuilt."),
            AgentUpdate(AgentRole.GOAL_COACH, "New workout evidence incorporated into coaching feedback.")
        )
    ) { account ->
        account.copy(
            workoutLogs = listOf(
                WorkoutLog(
                    id = newId("workout"),
                    date = date.trim(),
                    sessionType = sessionType,
                    durationMin = durationMin.coerceIn(10, 240),
                    intensityRpe = intensityRpe.coerceIn(1, 10),
                    caloriesBurned = caloriesBurned.coerceAtLeast(0),
                    completed = true,
                    notes = notes.trim()
                )
            ) + account.workoutLogs
        )
    }

    suspend fun toggleWorkoutLog(logId: String) = mutateCurrentAccount(
        notice = "训练完成状态已切换。",
        agentUpdates = listOf(AgentUpdate(AgentRole.TRACKING, "Workout completion state changed and compliance metrics refreshed."))
    ) { account ->
        account.copy(
            workoutLogs = account.workoutLogs.map { log ->
                if (log.id == logId) log.copy(completed = !log.completed) else log
            }
        )
    }

    suspend fun addFoodLog(
        date: String,
        mealType: MealType,
        title: String,
        kcal: Int,
        proteinG: Int,
        carbsG: Int,
        fatG: Int,
        notes: String
    ) = mutateCurrentAccount(
        notice = "饮食记录已写入，营养建议已刷新。",
        agentUpdates = listOf(
            AgentUpdate(AgentRole.NUTRITION, "Meal log appended and dietary guidance refreshed."),
            AgentUpdate(AgentRole.GOAL_COACH, "Meal execution incorporated into personalized feedback.")
        )
    ) { account ->
        if (title.isBlank()) return@mutateCurrentAccount account
        account.copy(
            foodLogs = listOf(
                FoodLogEntry(
                    id = newId("meal"),
                    date = date.trim(),
                    mealType = mealType,
                    title = title.trim(),
                    proteinG = proteinG.coerceAtLeast(0),
                    carbsG = carbsG.coerceAtLeast(0),
                    fatG = fatG.coerceAtLeast(0),
                    kcal = kcal.coerceAtLeast(0),
                    notes = notes.trim()
                )
            ) + account.foodLogs
        )
    }

    suspend fun regeneratePlans() = mutateCurrentAccount(
        notice = "训练与营养计划已重新生成。",
        agentUpdates = listOf(
            AgentUpdate(AgentRole.TRAINING, "Manual training plan regeneration completed."),
            AgentUpdate(AgentRole.NUTRITION, "Manual nutrition plan regeneration completed.")
        )
    ) { it }

    suspend fun runAgentAudit() = mutateCurrentAccount(
        notice = "主代理完成了一次巡检。",
        agentUpdates = emptyList()
    ) { it }

    private suspend fun mutateCurrentAccount(
        notice: String,
        agentUpdates: List<AgentUpdate>,
        transform: (UserAccount) -> UserAccount
    ) = mutex.withLock {
        val current = _store.value.currentAccount() ?: return@withLock
        val now = System.currentTimeMillis()
        val mutated = transform(current)
        val rebuilt = rebuildAccount(mutated, now, agentUpdates)
        publish(
            _store.value.copy(
                accounts = replaceAccount(_store.value.accounts, rebuilt),
                session = _store.value.session.copy(notice = notice)
            )
        )
    }

    private fun rebuildAccount(
        account: UserAccount,
        now: Long,
        agentUpdates: List<AgentUpdate>
    ): UserAccount {
        val trainingPlan = generateTrainingPlan(account.profile)
        val nutritionPlan = generateNutritionPlan(account.profile, account.foodLogs)
        val standards = getStandardApplications(account.profile, trainingPlan, nutritionPlan)
        val feedback = generateCoachFeedback(account.profile, account.goals, account.workoutLogs, account.foodLogs, nutritionPlan)

        var masterAgent = account.masterAgent.takeIf { it.agents.isNotEmpty() } ?: defaultMasterAgent(now)
        agentUpdates.forEach { update ->
            masterAgent = recordAgentActivity(masterAgent, update.role, update.report, now)
        }
        if (feedback.isNotEmpty()) {
            masterAgent = recordAgentActivity(masterAgent, AgentRole.GOAL_COACH, feedback.first(), now)
        }
        masterAgent = auditAgentHealth(masterAgent, now)

        return account.copy(
            updatedAtMillis = now,
            trainingPlan = trainingPlan,
            nutritionPlan = nutritionPlan,
            standards = standards,
            coachFeedback = feedback,
            masterAgent = masterAgent
        )
    }

    private fun loadInitialStore(): FitnessStore {
        if (!storeFile.exists()) return FitnessStore()
        return runCatching {
            json.decodeFromString<FitnessStore>(storeFile.readText())
        }.getOrDefault(FitnessStore())
    }

    private fun publish(store: FitnessStore) {
        _store.value = store
        storeFile.parentFile?.mkdirs()
        val temp = File(storeFile.parentFile, "$STORE_FILE_NAME.tmp")
        temp.writeText(json.encodeToString(store))
        temp.copyTo(storeFile, overwrite = true)
        temp.delete()
    }

    private fun sanitizeProfile(profile: UserProfile, fallbackName: String): UserProfile = profile.copy(
        name = profile.name.ifBlank { fallbackName },
        age = profile.age.coerceIn(16, 90),
        heightCm = profile.heightCm.coerceIn(120, 230),
        weightKg = profile.weightKg.coerceIn(35, 250),
        bodyFatPct = profile.bodyFatPct.coerceIn(5, 55),
        daysPerWeek = profile.daysPerWeek.coerceIn(2, 7),
        sleepHours = profile.sleepHours.coerceIn(4.0, 12.0),
        stepsPerDay = profile.stepsPerDay.coerceIn(2000, 30000)
    )

    private fun replaceAccount(accounts: List<UserAccount>, updated: UserAccount): List<UserAccount> {
        return accounts.map { account ->
            if (account.id == updated.id) updated else account
        }
    }

    private fun metricUnit(metric: GoalMetric): String = when (metric) {
        GoalMetric.WEIGHT -> "kg"
        GoalMetric.BODY_FAT -> "%"
        GoalMetric.STRENGTH -> "kg"
        GoalMetric.DISTANCE -> "km"
        GoalMetric.CONSISTENCY -> "sessions"
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private data class AgentUpdate(val role: AgentRole, val report: String)

    private companion object {
        const val STORE_FILE_NAME = "fitness-platform-store.json"
    }
}
