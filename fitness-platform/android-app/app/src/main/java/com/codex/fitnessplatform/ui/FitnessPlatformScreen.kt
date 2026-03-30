package com.codex.fitnessplatform.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codex.fitnessplatform.data.AgentRole
import com.codex.fitnessplatform.data.EquipmentAccess
import com.codex.fitnessplatform.data.ExperienceLevel
import com.codex.fitnessplatform.data.FitnessStore
import com.codex.fitnessplatform.data.GoalItem
import com.codex.fitnessplatform.data.GoalMetric
import com.codex.fitnessplatform.data.GoalType
import com.codex.fitnessplatform.data.MealType
import com.codex.fitnessplatform.data.RecoveryLevel
import com.codex.fitnessplatform.data.UserAccount
import com.codex.fitnessplatform.data.UserProfile
import com.codex.fitnessplatform.data.WeeklyAnalytics
import com.codex.fitnessplatform.data.WorkoutSessionType
import com.codex.fitnessplatform.data.currentAccount
import com.codex.fitnessplatform.logic.calculateGoalProgress
import com.codex.fitnessplatform.logic.getWeeklyAnalytics
import com.codex.fitnessplatform.logic.todayIsoDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class HomeTab(val title: String) {
    DASHBOARD("概览"),
    PLAN("训练"),
    NUTRITION("饮食"),
    LOGS("记录"),
    AGENTS("代理")
}

@Composable
fun FitnessPlatformScreen(viewModel: FitnessPlatformViewModel) {
    val store by viewModel.store.collectAsStateWithLifecycle()
    val account = store.currentAccount()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(store.session.notice) {
        store.session.notice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.clearNotice()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (account == null) {
                AuthScreen(
                    onLogin = viewModel::login,
                    onRegister = viewModel::register
                )
            } else {
                HomeScreen(
                    store = store,
                    account = account,
                    onLogout = viewModel::logout,
                    onUpdateProfile = viewModel::updateProfile,
                    onRegeneratePlans = viewModel::regeneratePlans,
                    onAddGoal = viewModel::addGoal,
                    onUpdateGoalCurrent = viewModel::updateGoalCurrent,
                    onAddWorkoutLog = viewModel::addWorkoutLog,
                    onToggleWorkoutLog = viewModel::toggleWorkoutLog,
                    onAddFoodLog = viewModel::addFoodLog,
                    onRunAgentAudit = viewModel::runAgentAudit
                )
            }
        }
    }
}

@Composable
private fun AuthScreen(
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit
) {
    var isRegister by rememberSaveable { mutableStateOf(true) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("FitPilot", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "本地 Android 健身原型，支持注册登录、个性化计划、训练与饮食记录，以及主代理监督子代理。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isRegister, onClick = { isRegister = true }, label = { Text("注册") })
                    FilterChip(selected = !isRegister, onClick = { isRegister = false }, label = { Text("登录") })
                }
                if (isRegister) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("昵称") }
                    )
                }
                OutlinedTextField(value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(), label = { Text("邮箱") })
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation()
                )
                Button(
                    onClick = { if (isRegister) onRegister(name, email, password) else onLogin(email, password) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRegister) "创建账号并开始计划" else "登录并继续")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    store: FitnessStore,
    account: UserAccount,
    onLogout: () -> Unit,
    onUpdateProfile: (UserProfile) -> Unit,
    onRegeneratePlans: () -> Unit,
    onAddGoal: (String, GoalMetric, Double, Double, String) -> Unit,
    onUpdateGoalCurrent: (String, Double) -> Unit,
    onAddWorkoutLog: (String, WorkoutSessionType, Int, Int, Int, String) -> Unit,
    onToggleWorkoutLog: (String) -> Unit,
    onAddFoodLog: (String, MealType, String, Int, Int, Int, Int, String) -> Unit,
    onRunAgentAudit: () -> Unit
) {
    var selectedTab by rememberSaveable(account.id) { mutableStateOf(HomeTab.DASHBOARD) }
    val analytics = remember(account.workoutLogs) { getWeeklyAnalytics(account.workoutLogs) }
    val goalAverage = remember(account.goals) {
        if (account.goals.isEmpty()) 0 else account.goals.map(::calculateGoalProgress).average().toInt()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("${account.displayName} · ${selectedTab.title}") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = "退出")
                    }
                }
            )
        },
        bottomBar = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                NavItem(HomeTab.DASHBOARD, selectedTab, Icons.Outlined.Dashboard) { selectedTab = HomeTab.DASHBOARD }
                NavItem(HomeTab.PLAN, selectedTab, Icons.Outlined.FitnessCenter) { selectedTab = HomeTab.PLAN }
                NavItem(HomeTab.NUTRITION, selectedTab, Icons.Outlined.Restaurant) { selectedTab = HomeTab.NUTRITION }
                NavItem(HomeTab.LOGS, selectedTab, Icons.Outlined.AutoGraph) { selectedTab = HomeTab.LOGS }
                NavItem(HomeTab.AGENTS, selectedTab, Icons.Outlined.SmartToy) { selectedTab = HomeTab.AGENTS }
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            HomeTab.DASHBOARD -> DashboardTab(Modifier.padding(innerPadding), account, analytics, goalAverage, onUpdateProfile, onRegeneratePlans, onAddGoal, onUpdateGoalCurrent)
            HomeTab.PLAN -> PlanTab(Modifier.padding(innerPadding), account, analytics, goalAverage, onRegeneratePlans)
            HomeTab.NUTRITION -> NutritionTab(Modifier.padding(innerPadding), account, onAddFoodLog)
            HomeTab.LOGS -> LogsTab(Modifier.padding(innerPadding), account, analytics, onAddWorkoutLog, onToggleWorkoutLog)
            HomeTab.AGENTS -> AgentsTab(Modifier.padding(innerPadding), store, account, onRunAgentAudit)
        }
    }
}

@Composable
private fun NavItem(
    tab: HomeTab,
    selected: HomeTab,
    icon: ImageVector,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (tab == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = tab.title)
            Text(tab.title)
        }
    }
}

@Composable
private fun DashboardTab(
    modifier: Modifier,
    account: UserAccount,
    analyticsSummary: WeeklyAnalytics,
    goalAverage: Int,
    onUpdateProfile: (UserProfile) -> Unit,
    onRegeneratePlans: () -> Unit,
    onAddGoal: (String, GoalMetric, Double, Double, String) -> Unit,
    onUpdateGoalCurrent: (String, Double) -> Unit
) {
    var profileDraft by remember(account.updatedAtMillis) { mutableStateOf(account.profile) }
    var goalTitle by rememberSaveable { mutableStateOf("") }
    var goalMetric by rememberSaveable { mutableStateOf(GoalMetric.WEIGHT) }
    var goalCurrent by rememberSaveable { mutableStateOf("0") }
    var goalTarget by rememberSaveable { mutableStateOf("0") }
    var goalDeadline by rememberSaveable { mutableStateOf(todayIsoDate()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroCard(
                title = "计划驱动，数据执行",
                subtitle = "主代理会持续监督训练、营养、记录和目标反馈子代理。",
                items = listOf("${profileDraft.daysPerWeek} 次/周", "${analyticsSummary.totalDuration} 分钟/7天", "$goalAverage% 目标均值")
            )
        }
        item {
            MetricRow(
                listOf(
                    "训练完成率" to "${analyticsSummary.completionRate}%",
                    "平均 RPE" to analyticsSummary.avgRpe.toString(),
                    "体重" to "${profileDraft.weightKg} kg"
                )
            )
        }
        item {
            SectionCard("用户画像") {
                LabeledInput("昵称", profileDraft.name) { profileDraft = profileDraft.copy(name = it) }
                LabeledInput("年龄", profileDraft.age.toString()) { it.toIntOrNull()?.let { value -> profileDraft = profileDraft.copy(age = value) } }
                LabeledInput("身高(cm)", profileDraft.heightCm.toString()) { it.toIntOrNull()?.let { value -> profileDraft = profileDraft.copy(heightCm = value) } }
                LabeledInput("体重(kg)", profileDraft.weightKg.toString()) { it.toIntOrNull()?.let { value -> profileDraft = profileDraft.copy(weightKg = value) } }
                LabeledInput("体脂(%)", profileDraft.bodyFatPct.toString()) { it.toIntOrNull()?.let { value -> profileDraft = profileDraft.copy(bodyFatPct = value) } }
                LabeledInput("每周训练天数", profileDraft.daysPerWeek.toString()) { it.toIntOrNull()?.let { value -> profileDraft = profileDraft.copy(daysPerWeek = value) } }
                LabeledInput("睡眠时长", profileDraft.sleepHours.toString()) { it.toDoubleOrNull()?.let { value -> profileDraft = profileDraft.copy(sleepHours = value) } }
                LabeledInput("日步数", profileDraft.stepsPerDay.toString()) { it.toIntOrNull()?.let { value -> profileDraft = profileDraft.copy(stepsPerDay = value) } }
                LabeledInput("伤病备注", profileDraft.injuries) { profileDraft = profileDraft.copy(injuries = it) }

                ChipSelector("目标", GoalType.entries, profileDraft.goalType, { it.label() }) { profileDraft = profileDraft.copy(goalType = it) }
                ChipSelector("级别", ExperienceLevel.entries, profileDraft.level, { it.label() }) { profileDraft = profileDraft.copy(level = it) }
                ChipSelector("器械", EquipmentAccess.entries, profileDraft.equipment, { it.label() }) { profileDraft = profileDraft.copy(equipment = it) }
                ChipSelector("恢复", RecoveryLevel.entries, profileDraft.recovery, { it.label() }) { profileDraft = profileDraft.copy(recovery = it) }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { onUpdateProfile(profileDraft) }) { Text("保存画像") }
                    TextButton(onClick = onRegeneratePlans) { Text("立即重算计划") }
                }
            }
        }
        item {
            SectionCard("新增目标") {
                LabeledInput("目标标题", goalTitle) { goalTitle = it }
                ChipSelector("指标", GoalMetric.entries, goalMetric, { it.label() }) { goalMetric = it }
                LabeledInput("当前值", goalCurrent) { goalCurrent = it }
                LabeledInput("目标值", goalTarget) { goalTarget = it }
                LabeledInput("截止日期(YYYY-MM-DD)", goalDeadline) { goalDeadline = it }
                Button(
                    onClick = {
                        onAddGoal(goalTitle, goalMetric, goalCurrent.toDoubleOrNull() ?: 0.0, goalTarget.toDoubleOrNull() ?: 0.0, goalDeadline)
                        goalTitle = ""
                        goalCurrent = "0"
                        goalTarget = "0"
                    }
                ) {
                    Text("添加目标")
                }
            }
        }
        items(account.goals, key = { it.id }) { goal ->
            GoalCard(goal = goal, onUpdateGoalCurrent = onUpdateGoalCurrent)
        }
    }
}

@Composable
private fun GoalCard(
    goal: GoalItem,
    onUpdateGoalCurrent: (String, Double) -> Unit
) {
    var currentValueText by remember(goal.id, goal.currentValue) { mutableStateOf(goal.currentValue.toString()) }
    val progress = calculateGoalProgress(goal)

    SectionCard(goal.title) {
        Text("${goal.currentValue}${goal.unit} -> ${goal.targetValue}${goal.unit} · 截止 ${goal.deadline}")
        LinearProgressIndicator(progress = { (progress / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        Text("当前进度 $progress%")
        LabeledInput("更新当前值", currentValueText) { currentValueText = it }
        Button(onClick = { onUpdateGoalCurrent(goal.id, currentValueText.toDoubleOrNull() ?: goal.currentValue) }) {
            Text("保存进度")
        }
    }
}

@Composable
private fun PlanTab(
    modifier: Modifier,
    account: UserAccount,
    analyticsSummary: WeeklyAnalytics,
    goalAverage: Int,
    onRegeneratePlans: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroCard(
                title = account.trainingPlan.split,
                subtitle = "训练计划根据画像、器械、恢复能力和睡眠表现自动生成。",
                items = listOf(
                    "${account.trainingPlan.weeklyResistanceSetsPerMuscle} 组/肌群",
                    "${account.trainingPlan.weeklyModerateCardioMinutes} 分有氧",
                    "$goalAverage% 目标均值"
                )
            )
        }
        item {
            MetricRow(
                listOf(
                    "最近 7 天时长" to "${analyticsSummary.totalDuration} 分钟",
                    "完成率" to "${analyticsSummary.completionRate}%",
                    "减量规则" to "第 5 周"
                )
            )
        }
        item {
            SectionCard("计划规则") {
                Text(account.trainingPlan.progressionRule)
                Text(account.trainingPlan.deloadRule)
                TextButton(onClick = onRegeneratePlans) { Text("重新生成") }
            }
        }
        items(account.trainingPlan.days, key = { it.day }) { day ->
            SectionCard("${day.day} · ${day.focus}") {
                Text("力量 ${day.strengthMinutes}m · 有氧 ${day.cardioMinutes}m · 灵活性 ${day.mobilityMinutes}m")
                day.exercises.forEach { exercise ->
                    Text("• ${exercise.name} · ${exercise.sets} x ${exercise.reps} · 休息 ${exercise.restSec}s")
                }
            }
        }
        items(account.standards, key = { it.standard }) { standard ->
            SectionCard(standard.standard) {
                Text(standard.principle)
                Text(standard.appliedRule, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun NutritionTab(
    modifier: Modifier,
    account: UserAccount,
    onAddFoodLog: (String, MealType, String, Int, Int, Int, Int, String) -> Unit
) {
    var mealDate by rememberSaveable { mutableStateOf(todayIsoDate()) }
    var mealType by rememberSaveable { mutableStateOf(MealType.BREAKFAST) }
    var mealTitle by rememberSaveable { mutableStateOf("") }
    var mealKcal by rememberSaveable { mutableStateOf("0") }
    var mealProtein by rememberSaveable { mutableStateOf("0") }
    var mealCarbs by rememberSaveable { mutableStateOf("0") }
    var mealFat by rememberSaveable { mutableStateOf("0") }
    var mealNotes by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroCard(
                title = "每日 ${account.nutritionPlan.dailyKcal} kcal",
                subtitle = "蛋白、碳水、脂肪目标随训练目标和近期饮食执行自动调整。",
                items = listOf(
                    "P ${account.nutritionPlan.proteinG}g",
                    "C ${account.nutritionPlan.carbsG}g",
                    "F ${account.nutritionPlan.fatG}g"
                )
            )
        }
        item {
            MetricRow(
                listOf(
                    "饮水" to "${account.nutritionPlan.hydrationLiters} L",
                    "已记录餐次" to account.foodLogs.size.toString(),
                    "当前目标" to account.profile.goalType.label()
                )
            )
        }
        item {
            SectionCard("新增饮食记录") {
                LabeledInput("日期", mealDate) { mealDate = it }
                ChipSelector("餐次", MealType.entries, mealType, { it.label() }) { mealType = it }
                LabeledInput("餐名", mealTitle) { mealTitle = it }
                LabeledInput("热量", mealKcal) { mealKcal = it }
                LabeledInput("蛋白(g)", mealProtein) { mealProtein = it }
                LabeledInput("碳水(g)", mealCarbs) { mealCarbs = it }
                LabeledInput("脂肪(g)", mealFat) { mealFat = it }
                LabeledInput("备注", mealNotes) { mealNotes = it }
                Button(
                    onClick = {
                        onAddFoodLog(
                            mealDate,
                            mealType,
                            mealTitle,
                            mealKcal.toIntOrNull() ?: 0,
                            mealProtein.toIntOrNull() ?: 0,
                            mealCarbs.toIntOrNull() ?: 0,
                            mealFat.toIntOrNull() ?: 0,
                            mealNotes
                        )
                        mealTitle = ""
                        mealNotes = ""
                    }
                ) {
                    Text("保存饮食记录")
                }
            }
        }
        items(account.nutritionPlan.meals, key = { it.label }) { meal ->
            SectionCard(meal.label) {
                Text("${meal.kcal} kcal · P/C/F ${meal.proteinG}/${meal.carbsG}/${meal.fatG}g")
                Text(meal.foods.joinToString(" · "))
            }
        }
        item {
            SectionCard("饮食建议") {
                account.nutritionPlan.notes.forEach { note -> Text("• $note") }
            }
        }
        items(account.foodLogs, key = { it.id }) { log ->
            SectionCard("${log.date} · ${log.mealType.label()}") {
                Text("${log.title} · ${log.kcal} kcal")
                Text("P/C/F ${log.proteinG}/${log.carbsG}/${log.fatG}g")
                if (log.notes.isNotBlank()) Text(log.notes)
            }
        }
    }
}

@Composable
private fun LogsTab(
    modifier: Modifier,
    account: UserAccount,
    analyticsSummary: WeeklyAnalytics,
    onAddWorkoutLog: (String, WorkoutSessionType, Int, Int, Int, String) -> Unit,
    onToggleWorkoutLog: (String) -> Unit
) {
    var logDate by rememberSaveable { mutableStateOf(todayIsoDate()) }
    var sessionType by rememberSaveable { mutableStateOf(WorkoutSessionType.STRENGTH) }
    var duration by rememberSaveable { mutableStateOf("45") }
    var rpe by rememberSaveable { mutableStateOf("7") }
    var calories by rememberSaveable { mutableStateOf("320") }
    var notes by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroCard(
                title = "训练记录与跟踪",
                subtitle = "每次新增记录都会触发 Tracking Agent 重算最近 7 天分析。",
                items = listOf(
                    "${analyticsSummary.totalDuration} 分钟",
                    "${analyticsSummary.completionRate}% 完成率",
                    "RPE ${analyticsSummary.avgRpe}"
                )
            )
        }
        item {
            SectionCard("新增训练记录") {
                LabeledInput("日期", logDate) { logDate = it }
                ChipSelector("类型", WorkoutSessionType.entries, sessionType, { it.label() }) { sessionType = it }
                LabeledInput("时长(分钟)", duration) { duration = it }
                LabeledInput("RPE", rpe) { rpe = it }
                LabeledInput("消耗(kcal)", calories) { calories = it }
                LabeledInput("备注", notes) { notes = it }
                Button(
                    onClick = {
                        onAddWorkoutLog(
                            logDate,
                            sessionType,
                            duration.toIntOrNull() ?: 45,
                            rpe.toIntOrNull() ?: 7,
                            calories.toIntOrNull() ?: 0,
                            notes
                        )
                        notes = ""
                    }
                ) {
                    Text("保存训练记录")
                }
            }
        }
        item {
            SectionCard("最近 7 天分析") {
                analyticsSummary.bars.forEach { bar ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(bar.date)
                            Text("${bar.duration} 分钟")
                        }
                        LinearProgressIndicator(progress = { (bar.pct / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        items(account.workoutLogs, key = { it.id }) { log ->
            SectionCard("${log.date} · ${log.sessionType.label()}") {
                Text("${log.durationMin} 分钟 · RPE ${log.intensityRpe} · ${log.caloriesBurned} kcal")
                if (log.notes.isNotBlank()) Text(log.notes)
                AssistChip(
                    onClick = { onToggleWorkoutLog(log.id) },
                    label = { Text(if (log.completed) "已完成，点此切换" else "待完成，点此切换") }
                )
            }
        }
    }
}

@Composable
private fun AgentsTab(
    modifier: Modifier,
    store: FitnessStore,
    account: UserAccount,
    onRunAgentAudit: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroCard(
                title = "Master Agent",
                subtitle = account.masterAgent.summary,
                items = listOf(
                    account.masterAgent.health.name,
                    "审计 ${formatTime(account.masterAgent.lastAuditAtMillis)}",
                    "${account.masterAgent.agents.size} 个子代理"
                )
            )
        }
        item {
            MetricRow(
                listOf(
                    "账号总数" to store.accounts.size.toString(),
                    "当前用户" to account.displayName,
                    "最近更新" to formatTime(account.updatedAtMillis)
                )
            )
        }
        item {
            SectionCard("主代理控制台") {
                Text("主代理每 5 分钟执行一次巡检；如果子代理超时或失败，会自动重建并继续任务。")
                Button(onClick = onRunAgentAudit) { Text("立即巡检") }
            }
        }
        items(account.masterAgent.agents, key = { it.id }) { agent ->
            SectionCard(agent.role.label()) {
                Text("任务：${agent.assignedTask}")
                Text("状态：${agent.status.name} · 最近活动：${formatTime(agent.lastActiveAtMillis)}")
                Text("重启次数：${agent.restartCount}")
                if (agent.latestReport.isNotBlank()) Text(agent.latestReport, color = MaterialTheme.colorScheme.primary)
            }
        }
        item {
            SectionCard("个性化反馈") {
                account.coachFeedback.forEach { feedback -> Text("• $feedback") }
            }
        }
    }
}

@Composable
private fun HeroCard(
    title: String,
    subtitle: String,
    items: List<String>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { label ->
                    AssistChip(onClick = {}, label = { Text(label) })
                }
            }
        }
    }
}

@Composable
private fun MetricRow(metrics: List<Pair<String, String>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        metrics.forEach { (label, value) ->
            Card(modifier = Modifier.width(150.dp)) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun LabeledInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }
    )
}

@Composable
private fun <T> ChipSelector(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    label = { Text(label(option)) }
                )
            }
        }
    }
}

private fun GoalType.label(): String = when (this) {
    GoalType.FAT_LOSS -> "减脂"
    GoalType.MUSCLE_GAIN -> "增肌"
    GoalType.ENDURANCE -> "耐力"
    GoalType.HEALTH -> "健康"
}

private fun GoalMetric.label(): String = when (this) {
    GoalMetric.WEIGHT -> "体重"
    GoalMetric.BODY_FAT -> "体脂"
    GoalMetric.STRENGTH -> "力量"
    GoalMetric.DISTANCE -> "距离"
    GoalMetric.CONSISTENCY -> "一致性"
}

private fun ExperienceLevel.label(): String = when (this) {
    ExperienceLevel.BEGINNER -> "新手"
    ExperienceLevel.INTERMEDIATE -> "中级"
    ExperienceLevel.ADVANCED -> "高级"
}

private fun EquipmentAccess.label(): String = when (this) {
    EquipmentAccess.BODYWEIGHT -> "自重"
    EquipmentAccess.HOME_GYM -> "家用器械"
    EquipmentAccess.FULL_GYM -> "商业健身房"
}

private fun RecoveryLevel.label(): String = when (this) {
    RecoveryLevel.LOW -> "偏低"
    RecoveryLevel.MEDIUM -> "中等"
    RecoveryLevel.HIGH -> "较好"
}

private fun MealType.label(): String = when (this) {
    MealType.BREAKFAST -> "早餐"
    MealType.LUNCH -> "午餐"
    MealType.DINNER -> "晚餐"
    MealType.SNACK -> "加餐"
}

private fun WorkoutSessionType.label(): String = when (this) {
    WorkoutSessionType.STRENGTH -> "力量"
    WorkoutSessionType.CARDIO -> "有氧"
    WorkoutSessionType.MOBILITY -> "灵活性"
    WorkoutSessionType.MIXED -> "混合"
}

private fun AgentRole.label(): String = when (this) {
    AgentRole.AUTH -> "Auth Agent"
    AgentRole.TRAINING -> "Training Agent"
    AgentRole.NUTRITION -> "Nutrition Agent"
    AgentRole.TRACKING -> "Tracking Agent"
    AgentRole.GOAL_COACH -> "Goal Coach Agent"
}

private fun formatTime(millis: Long): String {
    if (millis <= 0L) return "未执行"
    val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(formatter)
}
