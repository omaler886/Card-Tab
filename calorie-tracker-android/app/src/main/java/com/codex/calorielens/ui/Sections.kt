package com.codex.calorielens.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codex.calorielens.data.ActivityLevel
import com.codex.calorielens.data.AiSettings
import com.codex.calorielens.data.BackupPreferences
import com.codex.calorielens.data.BackupTarget
import com.codex.calorielens.data.Cuisine
import com.codex.calorielens.data.FoodEntry
import com.codex.calorielens.data.FoodLookupItem
import com.codex.calorielens.data.FoodLookupState
import com.codex.calorielens.data.FoodRegion
import com.codex.calorielens.data.GoalType
import com.codex.calorielens.data.MainUiState
import com.codex.calorielens.data.PhotoAnalysisState
import com.codex.calorielens.data.Recipe
import com.codex.calorielens.data.Sex
import com.codex.calorielens.data.UserProfile
import com.codex.calorielens.data.BackupState
import com.codex.calorielens.data.SyncBackendSettings
import com.codex.calorielens.data.WebDavSettings
import com.codex.calorielens.logic.NutritionEngine
import java.util.Locale
import kotlin.math.min

@Composable
internal fun HeaderCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "CalorieLens",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "把手填记录、拍照识别、AI 分析和中美食谱判断放到一个页面里，先把每日热量算清楚。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("手填") })
                AssistChip(onClick = {}, label = { Text("拍照") })
                AssistChip(onClick = {}, label = { Text("中美食谱") })
            }
        }
    }
}

@Composable
internal fun SummaryCard(uiState: MainUiState) {
    val progress = if (uiState.targetCalories == 0) 0f else {
        min(uiState.consumedCalories.toFloat() / uiState.targetCalories.toFloat(), 1.4f)
    }
    SectionCard(title = "今日总览") {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricPill("目标", "${uiState.targetCalories} kcal")
            MetricPill("已摄入", "${uiState.consumedCalories} kcal")
            MetricPill("剩余", "${uiState.remainingCalories} kcal")
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
        )
        Text(
            text = uiState.statusText,
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
internal fun HistoryChartsCard(uiState: MainUiState) {
    val history = remember(uiState.entries) {
        NutritionEngine.historyPoints(uiState.entries, days = 7)
    }
    val breakdown = remember(uiState.entries) {
        NutritionEngine.cuisineBreakdown(uiState.entries, days = 30)
    }
    val average = remember(uiState.entries) {
        NutritionEngine.averageDailyCalories(uiState.entries, days = 7)
    }
    val maxCalories = maxOf(1, history.maxOfOrNull { it.calories } ?: 1, uiState.targetCalories)

    SectionCard(title = "历史统计图表") {
        Text(
            text = "最近 7 天平均 ${average} kcal / 天，帮助判断你是稳定达标还是偶尔波动。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            history.forEach { point ->
                val barHeight = ((point.calories.toFloat() / maxCalories.toFloat()) * 120f).coerceAtLeast(6f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(barHeight.dp)
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                            .background(
                                if (point.isToday) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)
                                }
                            )
                    )
                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = point.calories.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (breakdown.isNotEmpty()) {
            Text(
                text = "最近 30 天菜系占比",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            val total = breakdown.sumOf { it.value }.coerceAtLeast(1)
            breakdown.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.label, style = MaterialTheme.typography.bodyMedium)
                        Text("${item.value} kcal", style = MaterialTheme.typography.bodyMedium)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(item.value.toFloat() / total.toFloat())
                                .height(10.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(item.colorArgb))
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BarcodeDatabaseCard(
    lookupState: FoodLookupState,
    onSelectRegion: (FoodRegion) -> Unit,
    onScanBarcode: () -> Unit,
    onBarcodeDetected: (String) -> Unit,
    onSearch: (String) -> Unit,
    onAddItem: (FoodLookupItem) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showContinuousScanner by rememberSaveable { mutableStateOf(false) }

    SectionCard(title = "条码扫描 + 食品数据库") {
        Text(
            text = "中国侧优先查条码库和扩展本地菜品，美国侧会补充 USDA FoodData Central，适合查包装食品和品牌餐食。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ChipRow(
            title = "查询区域",
            options = FoodRegion.entries,
            selected = lookupState.region,
            label = { it.label },
            onSelect = onSelectRegion
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("食品名称搜索") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onScanBarcode, modifier = Modifier.weight(1f)) {
                Text("扫描条码")
            }
            Button(
                onClick = { onSearch(query) },
                modifier = Modifier.weight(1f)
            ) {
                Text("搜索数据库")
            }
        }
        Button(
            onClick = { showContinuousScanner = !showContinuousScanner },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showContinuousScanner) "关闭连续扫码" else "开启连续扫码")
        }
        if (showContinuousScanner) {
            ContinuousBarcodeScanner(
                modifier = Modifier.fillMaxWidth(),
                onBarcodeDetected = { code ->
                    query = code
                    onBarcodeDetected(code)
                }
            )
        }
        if (lookupState.lastBarcode.isNotBlank()) {
            InfoBlock("最近条码", lookupState.lastBarcode)
        }
        if (lookupState.isLoading) {
            LinearProgressIndicator(
                progress = { 0.4f },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "正在查询食品数据库...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        lookupState.error?.let { error ->
            Text(
                text = error,
                color = Color(0xFFB42318),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (lookupState.results.isNotEmpty()) {
            Text(
                text = "数据库结果",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            lookupState.results.forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${item.name} · ${item.calories} kcal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = listOfNotNull(
                                item.brand.takeIf { it.isNotBlank() },
                                item.servingLabel.takeIf { it.isNotBlank() },
                                item.sourceLabel
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "P${item.protein}/C${item.carbs}/F${item.fat} · ${item.cuisine.label}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (item.barcode.isNotBlank()) {
                            Text(
                                text = "条码 ${item.barcode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.note.isNotBlank()) {
                            Text(
                                text = item.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = { onAddItem(item) }) {
                            Text("加入今日")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProfileCard(profile: UserProfile, onSave: (UserProfile) -> Unit) {
    var ageText by remember(profile) { mutableStateOf(profile.age.toString()) }
    var heightText by remember(profile) { mutableStateOf(profile.heightCm.toString()) }
    var weightText by remember(profile) { mutableStateOf(String.format(Locale.US, "%.1f", profile.weightKg)) }
    var selectedSex by remember(profile) { mutableStateOf(profile.sex) }
    var selectedActivity by remember(profile) { mutableStateOf(profile.activityLevel) }
    var selectedGoal by remember(profile) { mutableStateOf(profile.goalType) }

    SectionCard(title = "每日目标计算") {
        Text(
            text = "基于 Mifflin-St Jeor 公式计算，并根据活动量和减脂/维持/增肌目标自动调整。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = ageText,
                onValueChange = { ageText = it },
                label = { Text("年龄") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = heightText,
                onValueChange = { heightText = it },
                label = { Text("身高 cm") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text("体重 kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        ChipRow("性别", Sex.entries, selectedSex, { it.label }) { selectedSex = it }
        ChipRow("活动量", ActivityLevel.entries, selectedActivity, { it.label }) { selectedActivity = it }
        ChipRow("目标", GoalType.entries, selectedGoal, { it.label }) { selectedGoal = it }
        Button(
            onClick = {
                onSave(
                    UserProfile(
                        age = ageText.toIntOrNull()?.coerceIn(12, 90) ?: profile.age,
                        heightCm = heightText.toIntOrNull()?.coerceIn(120, 220) ?: profile.heightCm,
                        weightKg = weightText.toDoubleOrNull()?.coerceIn(30.0, 250.0) ?: profile.weightKg,
                        sex = selectedSex,
                        activityLevel = selectedActivity,
                        goalType = selectedGoal
                    )
                )
            }
        ) {
            Text("保存目标设置")
        }
    }
}

@Composable
internal fun ManualEntryCard(
    onAdd: (String, String, String, Cuisine) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var caloriesText by rememberSaveable { mutableStateOf("") }
    var servingsText by rememberSaveable { mutableStateOf("1") }
    var cuisineRaw by rememberSaveable { mutableStateOf(Cuisine.CHINESE.name) }

    SectionCard(title = "手填食物") {
        Text(
            text = "如果不知道精确热量，可以只填名称，应用会先按常见食物库做估算。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("食物名称") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = servingsText,
                onValueChange = { servingsText = it },
                label = { Text("份数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = caloriesText,
                onValueChange = { caloriesText = it },
                label = { Text("总热量 kcal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        ChipRow(
            title = "菜系",
            options = listOf(Cuisine.CHINESE, Cuisine.AMERICAN, Cuisine.OTHER),
            selected = Cuisine.fromRaw(cuisineRaw),
            label = { it.label },
            onSelect = { cuisineRaw = it.name }
        )
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    onAdd(name, servingsText, caloriesText, Cuisine.fromRaw(cuisineRaw))
                    name = ""
                    caloriesText = ""
                    servingsText = "1"
                }
            }
        ) {
            Text("加入今日饮食")
        }
    }
}

@Composable
internal fun PhotoRecognitionCard(
    photoState: PhotoAnalysisState,
    aiSettings: AiSettings,
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
    onAnalyze: () -> Unit,
    onAddRecognized: () -> Unit
) {
    SectionCard(title = "拍照识别 + AI") {
        Text(
            text = "不配置 API Key 时，会先走 OCR 文本匹配；配置后可直接识别餐盘、便当和外卖照片。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onTakePhoto, modifier = Modifier.weight(1f)) { Text("拍照") }
            Button(onClick = onPickGallery, modifier = Modifier.weight(1f)) { Text("从相册选择") }
        }
        if (photoState.selectedImageUri != null) {
            AsyncImage(
                model = Uri.parse(photoState.selectedImageUri),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Text("这里会显示待识别的餐食照片")
            }
        }
        Text(
            text = if (aiSettings.apiKey.isBlank()) {
                "当前 AI 状态：未配置 Key，建议先保存 AI 接口后再识别餐盘。"
            } else {
                "当前 AI 状态：已配置 ${aiSettings.model}"
            },
            color = if (aiSettings.apiKey.isBlank()) Color(0xFFC0582B) else MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Button(
            onClick = onAnalyze,
            enabled = photoState.selectedImageUri != null && !photoState.isAnalyzing
        ) {
            Text(if (photoState.isAnalyzing) "识别中..." else "开始识别")
        }
        if (photoState.ocrText.isNotBlank()) {
            InfoBlock("OCR 文本", photoState.ocrText)
        }
        if (photoState.aiSummary.isNotBlank()) {
            InfoBlock("AI 总结", photoState.aiSummary)
        }
        photoState.error?.let { error ->
            Text(
                text = error,
                color = Color(0xFFB42318),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (photoState.items.isNotEmpty()) {
            Text(
                text = "识别结果",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            photoState.items.forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "${item.name} · ${item.calories} kcal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "份数 ${item.servings} · ${item.cuisine.label} · P${item.protein}/C${item.carbs}/F${item.fat}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = item.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Button(onClick = onAddRecognized) { Text("把识别结果加入今日") }
        }
    }
}

@Composable
internal fun RecipeSection(
    uiState: MainUiState,
    recipesForCuisine: (Cuisine) -> List<Recipe>,
    recipeImpact: (Recipe) -> String,
    onAddRecipe: (Recipe) -> Unit
) {
    var selectedCuisine by rememberSaveable { mutableStateOf(Cuisine.CHINESE.name) }
    val cuisine = Cuisine.fromRaw(selectedCuisine)
    val recipes = recipesForCuisine(cuisine)

    SectionCard(title = "中美食谱达标判断") {
        Text(
            text = "按你今天已经吃掉的热量重新排序，优先把更接近目标的食谱放前面。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ChipRow(
            title = "食谱来源",
            options = listOf(Cuisine.CHINESE, Cuisine.AMERICAN),
            selected = cuisine,
            label = { it.label },
            onSelect = { selectedCuisine = it.name }
        )
        Text(
            text = "当前：已摄入 ${uiState.consumedCalories} kcal，目标 ${uiState.targetCalories} kcal",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        recipes.forEach { recipe ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${recipe.mealLabel} · ${recipe.calories} kcal · P${recipe.protein}/C${recipe.carbs}/F${recipe.fat}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = recipe.highlight,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = recipeImpact(recipe),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = recipe.ingredients.joinToString(" / "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { onAddRecipe(recipe) }) { Text("加入今日") }
                }
            }
        }
    }
}

@Composable
internal fun AiSettingsCard(
    settings: AiSettings,
    onSave: (AiSettings) -> Unit
) {
    var baseUrl by remember(settings) { mutableStateOf(settings.baseUrl) }
    var apiKey by remember(settings) { mutableStateOf(settings.apiKey) }
    var model by remember(settings) { mutableStateOf(settings.model) }

    SectionCard(title = "AI 接口设置") {
        Text(
            text = "默认使用 OpenAI 兼容的 chat/completions 图像输入格式。如果你接的是代理或兼容服务，请填完整接口地址。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("接口地址") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("模型名") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                onSave(
                    AiSettings(
                        baseUrl = baseUrl.trim(),
                        apiKey = apiKey.trim(),
                        model = model.trim()
                    )
                )
            }
        ) {
            Text("保存 AI 配置")
        }
    }
}

@Composable
internal fun BackupRestoreCard(
    backupState: BackupState,
    onSavePreferences: (BackupPreferences) -> Unit,
    onExportLocal: () -> Unit,
    onImportLocal: () -> Unit,
    onUploadWebDav: () -> Unit,
    onRestoreWebDav: () -> Unit,
    onRegisterAccount: (String, String, String) -> Unit,
    onLoginAccount: (String, String, String) -> Unit,
    onRequestEmailVerification: (String, String) -> Unit,
    onVerifyEmail: (String, String) -> Unit,
    onRequestPasswordReset: (String, String) -> Unit,
    onResetPassword: (String, String, String) -> Unit,
    onPushBackend: () -> Unit,
    onForcePushBackend: () -> Unit,
    onPullBackend: () -> Unit
) {
    val preferences = backupState.preferences
    var passphrase by remember(preferences.passphrase) { mutableStateOf(preferences.passphrase) }
    var webDavBaseUrl by remember(preferences.webDavSettings) { mutableStateOf(preferences.webDavSettings.baseUrl) }
    var webDavUsername by remember(preferences.webDavSettings) { mutableStateOf(preferences.webDavSettings.username) }
    var webDavPassword by remember(preferences.webDavSettings) { mutableStateOf(preferences.webDavSettings.password) }
    var webDavRemotePath by remember(preferences.webDavSettings) { mutableStateOf(preferences.webDavSettings.remotePath) }
    var backendBaseUrl by remember(preferences.backendSettings) { mutableStateOf(preferences.backendSettings.baseUrl) }
    var backendEmail by remember(preferences.backendSettings) { mutableStateOf(preferences.backendSettings.email) }
    var backendPassword by remember(preferences.backendSettings) { mutableStateOf(preferences.backendSettings.password) }
    var verificationToken by rememberSaveable { mutableStateOf("") }
    var resetToken by rememberSaveable { mutableStateOf("") }
    var resetNewPassword by rememberSaveable { mutableStateOf("") }
    var autoBackupEnabled by remember(preferences.autoBackupSettings) { mutableStateOf(preferences.autoBackupSettings.enabled) }
    var autoBackupInterval by remember(preferences.autoBackupSettings) { mutableStateOf(preferences.autoBackupSettings.intervalHours.toString()) }
    var autoBackupTarget by remember(preferences.autoBackupSettings) { mutableStateOf(preferences.autoBackupSettings.target) }

    SectionCard(title = "数据导出 / 备份恢复") {
        Text(
            text = "支持加密后的本地 JSON、WebDAV 云备份，以及账号云同步。当前备份会包含饮食记录、目标设置和 AI 设置。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "备份加密",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = passphrase,
            onValueChange = { passphrase = it },
            label = { Text("备份加密口令") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "这组口令用于 AES-GCM 加密备份文件和云端同步数据；恢复时需要同一口令。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onExportLocal, modifier = Modifier.weight(1f)) {
                Text("导出 JSON")
            }
            Button(onClick = onImportLocal, modifier = Modifier.weight(1f)) {
                Text("恢复 JSON")
            }
        }
        Text(
            text = "WebDAV 云端备份",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = webDavBaseUrl,
            onValueChange = { webDavBaseUrl = it },
            label = { Text("WebDAV 地址") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = webDavUsername,
                onValueChange = { webDavUsername = it },
                label = { Text("用户名") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = webDavPassword,
                onValueChange = { webDavPassword = it },
                label = { Text("密码") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = webDavRemotePath,
            onValueChange = { webDavRemotePath = it },
            label = { Text("远程路径") },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "示例：地址可填坚果云/Nextcloud/群晖的 DAV 根路径，远程路径可填 `CalorieLens/backup.json`。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = {
                onSavePreferences(
                    BackupPreferences(
                        passphrase = passphrase,
                        webDavSettings = WebDavSettings(
                            baseUrl = webDavBaseUrl.trim(),
                            username = webDavUsername.trim(),
                            password = webDavPassword,
                            remotePath = webDavRemotePath.trim()
                        ),
                        backendSettings = preferences.backendSettings.copy(
                            baseUrl = backendBaseUrl.trim(),
                            email = backendEmail.trim(),
                            password = backendPassword
                        ),
                        autoBackupSettings = preferences.autoBackupSettings.copy(
                            enabled = autoBackupEnabled,
                            intervalHours = autoBackupInterval.toIntOrNull()?.coerceAtLeast(1) ?: 24,
                            target = autoBackupTarget
                        )
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存备份配置")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onUploadWebDav,
                enabled = !backupState.isWorking,
                modifier = Modifier.weight(1f)
            ) {
                Text("上传 WebDAV")
            }
            Button(
                onClick = onRestoreWebDav,
                enabled = !backupState.isWorking,
                modifier = Modifier.weight(1f)
            ) {
                Text("WebDAV 恢复")
            }
        }
        Text(
            text = "账号云同步",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = backendBaseUrl,
            onValueChange = { backendBaseUrl = it },
            label = { Text("同步后端地址") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = backendEmail,
            onValueChange = { backendEmail = it },
            label = { Text("账号邮箱") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = backendPassword,
            onValueChange = { backendPassword = it },
            label = { Text("账号密码") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (preferences.backendSettings.accessToken.isNotBlank()) {
            InfoBlock(
                "当前同步账号",
                "${preferences.backendSettings.email.ifBlank { "已登录" }} · " +
                    (if (preferences.backendSettings.emailVerified) "已验证" else "未验证") +
                    " · rev ${preferences.backendSettings.lastSyncedRevision} · device ${preferences.backendSettings.deviceId.take(8)}"
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onRegisterAccount(backendBaseUrl, backendEmail, backendPassword) },
                enabled = !backupState.isWorking,
                modifier = Modifier.weight(1f)
            ) {
                Text("注册账号")
            }
            Button(
                onClick = { onLoginAccount(backendBaseUrl, backendEmail, backendPassword) },
                enabled = !backupState.isWorking,
                modifier = Modifier.weight(1f)
            ) {
                Text("登录账号")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onRequestEmailVerification(backendBaseUrl, backendEmail) },
                enabled = !backupState.isWorking,
                modifier = Modifier.weight(1f)
            ) {
                Text("发验证邮件")
            }
            Button(
                onClick = { onRequestPasswordReset(backendBaseUrl, backendEmail) },
                enabled = !backupState.isWorking,
                modifier = Modifier.weight(1f)
            ) {
                Text("找回密码")
            }
        }
        OutlinedTextField(
            value = verificationToken,
            onValueChange = { verificationToken = it },
            label = { Text("邮箱验证令牌") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onVerifyEmail(backendBaseUrl, verificationToken) },
            enabled = !backupState.isWorking,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("确认邮箱验证")
        }
        OutlinedTextField(
            value = resetToken,
            onValueChange = { resetToken = it },
            label = { Text("重置密码令牌") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = resetNewPassword,
            onValueChange = { resetNewPassword = it },
            label = { Text("新密码") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onResetPassword(backendBaseUrl, resetToken, resetNewPassword) },
            enabled = !backupState.isWorking,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("确认重置密码")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onPushBackend,
                enabled = !backupState.isWorking,
                modifier = Modifier.weight(1f)
            ) {
                Text("上传同步")
            }
            Button(
                onClick = onPullBackend,
                enabled = !backupState.isWorking,
                modifier = Modifier.weight(1f)
            ) {
                Text("拉取同步")
            }
        }
        Button(
            onClick = onForcePushBackend,
            enabled = !backupState.isWorking,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("强制覆盖云端")
        }
        Text(
            text = "自动定时备份",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        ChipRow(
            title = "自动备份开关",
            options = listOf(true, false),
            selected = autoBackupEnabled,
            label = { if (it) "开启" else "关闭" },
            onSelect = { autoBackupEnabled = it }
        )
        ChipRow(
            title = "目标",
            options = BackupTarget.entries,
            selected = autoBackupTarget,
            label = { it.label },
            onSelect = { autoBackupTarget = it }
        )
        OutlinedTextField(
            value = autoBackupInterval,
            onValueChange = { autoBackupInterval = it },
            label = { Text("间隔小时") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        if (backupState.isWorking) {
            LinearProgressIndicator(
                progress = { 0.55f },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (backupState.status.isNotBlank()) {
            InfoBlock("备份状态", backupState.status)
        }
        backupState.error?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB42318)
            )
        }
    }
}

@Composable
internal fun ReleaseReadyCard(
    onOpenPrivacy: () -> Unit
) {
    SectionCard(title = "上架准备") {
        Text(
            text = "应用已接入本地 Room 数据库、签名 release 包和隐私页入口，适合继续做商店物料与合规补充。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onOpenPrivacy, modifier = Modifier.weight(1f)) {
                Text("打开隐私页")
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Release", fontWeight = FontWeight.SemiBold)
                    Text("已启用压缩与签名", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
internal fun RecentEntriesCard(
    entries: List<FoodEntry>,
    onDelete: (String) -> Unit
) {
    SectionCard(title = "今日已记录") {
        if (entries.isEmpty()) {
            Text(
                text = "今天还没有饮食记录，先从手填、拍照或食谱加入任意一种开始。",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider()
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${entry.calories} kcal · ${entry.cuisine.label} · ${formatTime(entry.createdAt)} · ${entry.source.label}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (entry.note.isNotBlank()) {
                            Text(
                                text = entry.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(onClick = { onDelete(entry.id) }) { Text("删除") }
                }
            }
        }
    }
}
