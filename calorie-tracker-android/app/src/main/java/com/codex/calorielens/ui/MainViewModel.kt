package com.codex.calorielens.ui

import android.app.Application
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codex.calorielens.ai.AiNutritionService
import com.codex.calorielens.ai.PhotoAnalyzer
import com.codex.calorielens.data.AiSettings
import com.codex.calorielens.data.AppStorage
import com.codex.calorielens.data.BackupPreferences
import com.codex.calorielens.data.BackupState
import com.codex.calorielens.data.CalorieLensDatabase
import com.codex.calorielens.data.Cuisine
import com.codex.calorielens.data.EntryRepository
import com.codex.calorielens.data.FoodDatabaseService
import com.codex.calorielens.data.FoodEntry
import com.codex.calorielens.data.FoodLookupItem
import com.codex.calorielens.data.FoodRegion
import com.codex.calorielens.data.MainUiState
import com.codex.calorielens.data.PhotoAnalysisState
import com.codex.calorielens.data.Recipe
import com.codex.calorielens.data.RecipeCatalog
import com.codex.calorielens.data.SyncConflictInfo
import com.codex.calorielens.data.SyncBackendSettings
import com.codex.calorielens.data.SyncBackendService
import com.codex.calorielens.data.UserProfile
import com.codex.calorielens.data.WebDavSettings
import com.codex.calorielens.data.BackupManager
import com.codex.calorielens.logic.NutritionEngine
import com.codex.calorielens.sync.AutoBackupScheduler
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = AppStorage(application)
    private val entryRepository = EntryRepository(
        dao = CalorieLensDatabase.getInstance(application).foodEntryDao(),
        storage = storage
    )
    private val backupManager = BackupManager(
        context = application,
        storage = storage,
        entryRepository = entryRepository
    )
    private val syncBackendService = SyncBackendService()
    private val photoAnalyzer = PhotoAnalyzer(application)
    private val aiService = AiNutritionService()
    private val foodDatabaseService = FoodDatabaseService()

    var uiState by mutableStateOf(MainUiState())
        private set

    init {
        viewModelScope.launch {
            entryRepository.initialize()
            ensureDeviceId()
        }
        viewModelScope.launch {
            combine(
                entryRepository.entriesFlow,
                storage.profileFlow,
                storage.aiSettingsFlow,
                storage.backupPreferencesFlow
            ) { entries, profile, settings, backupPreferences ->
                UiInputs(entries, profile, settings, backupPreferences)
            }.collect { inputs ->
                rebuildUi(
                    entries = inputs.entries,
                    profile = inputs.profile,
                    settings = inputs.aiSettings,
                    backupPreferences = inputs.backupPreferences
                )
                AutoBackupScheduler.update(getApplication(), inputs.backupPreferences.autoBackupSettings)
            }
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            storage.saveProfile(profile)
            pushNotice("已更新每日热量目标。")
        }
    }

    fun saveAiSettings(settings: AiSettings) {
        viewModelScope.launch {
            storage.saveAiSettings(settings)
            pushNotice("AI 配置已保存。")
        }
    }

    fun saveBackupPreferences(preferences: BackupPreferences, notice: String = "备份设置已保存。") {
        viewModelScope.launch {
            storage.saveBackupPreferences(withDeviceId(preferences))
            pushNotice(notice)
        }
    }

    fun addManualEntry(
        name: String,
        servingsText: String,
        caloriesText: String,
        cuisine: Cuisine
    ) {
        val safeName = name.trim()
        if (safeName.isBlank()) {
            pushNotice("请先填写食物名称。")
            return
        }
        val servings = servingsText.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0
        val calories = caloriesText.toIntOrNull()
        val entry = NutritionEngine.createManualEntry(safeName, servings, calories, cuisine)
        viewModelScope.launch {
            entryRepository.appendEntry(entry)
            pushNotice("${entry.name} 已加入今日记录。")
        }
    }

    fun addRecipe(recipe: Recipe) {
        viewModelScope.launch {
            entryRepository.appendEntry(NutritionEngine.createRecipeEntry(recipe))
            pushNotice("${recipe.title} 已加入今日饮食。")
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            entryRepository.removeEntry(entryId)
            pushNotice("已删除一条记录。")
        }
    }

    fun setLookupRegion(region: FoodRegion) {
        uiState = uiState.copy(
            lookup = uiState.lookup.copy(region = region, error = null)
        )
    }

    fun searchFoodDatabase(query: String) {
        val safeQuery = query.trim()
        if (safeQuery.isBlank()) {
            uiState = uiState.copy(
                lookup = uiState.lookup.copy(error = "请先输入食物名称。")
            )
            return
        }
        uiState = uiState.copy(
            lookup = uiState.lookup.copy(isLoading = true, error = null, results = emptyList())
        )
        viewModelScope.launch {
            val result = foodDatabaseService.searchFoods(safeQuery, uiState.lookup.region)
            uiState = uiState.copy(
                lookup = uiState.lookup.copy(
                    isLoading = false,
                    results = result.getOrDefault(emptyList()),
                    error = result.exceptionOrNull()?.message
                )
            )
        }
    }

    fun lookupBarcode(barcode: String) {
        val safeBarcode = barcode.filter(Char::isDigit)
        if (safeBarcode.isBlank()) {
            uiState = uiState.copy(
                lookup = uiState.lookup.copy(error = "没有识别到可用条码。")
            )
            return
        }
        uiState = uiState.copy(
            lookup = uiState.lookup.copy(
                lastBarcode = safeBarcode,
                isLoading = true,
                error = null,
                results = emptyList()
            )
        )
        viewModelScope.launch {
            val result = foodDatabaseService.lookupBarcode(safeBarcode, uiState.lookup.region)
            uiState = uiState.copy(
                lookup = uiState.lookup.copy(
                    isLoading = false,
                    results = result.getOrDefault(emptyList()),
                    error = result.exceptionOrNull()?.message
                )
            )
            if (result.isSuccess) {
                pushNotice("条码 $safeBarcode 已完成查询。")
            }
        }
    }

    fun setLookupError(message: String) {
        uiState = uiState.copy(
            lookup = uiState.lookup.copy(error = message, isLoading = false)
        )
    }

    fun addLookupItem(item: FoodLookupItem) {
        viewModelScope.launch {
            entryRepository.appendEntry(NutritionEngine.createLookupEntry(item))
            pushNotice("${item.name} 已加入今日记录。")
        }
    }

    fun exportBackup(uri: Uri?) {
        if (uri == null) {
            setBackupError("没有选择导出文件。")
            return
        }
        val passphrase = uiState.backup.preferences.passphrase
        startBackupWork("正在导出...") {
            backupManager.exportToUri(uri, passphrase)
        }
    }

    fun importBackup(uri: Uri?) {
        if (uri == null) {
            setBackupError("没有选择备份文件。")
            return
        }
        val passphrase = uiState.backup.preferences.passphrase
        startBackupWork("正在恢复...") {
            backupManager.restoreFromUri(uri, passphrase)
        }
    }

    fun uploadWebDavBackup() {
        val preferences = uiState.backup.preferences
        startBackupWork("正在上传 WebDAV...") {
            backupManager.uploadToWebDav(preferences.webDavSettings, preferences.passphrase)
        }
    }

    fun restoreWebDavBackup() {
        val preferences = uiState.backup.preferences
        startBackupWork("正在从 WebDAV 恢复...") {
            backupManager.restoreFromWebDav(preferences.webDavSettings, preferences.passphrase)
        }
    }

    fun registerSyncAccount(baseUrl: String, email: String, password: String) {
        startBackupWork("正在注册同步账号...") {
            syncBackendService.register(baseUrl, email, password).map { auth ->
                saveBackendSettingsInternal(
                    uiState.backup.preferences.backendSettings.copy(
                        baseUrl = baseUrl.trim(),
                        email = auth.email.ifBlank { email.trim() },
                        password = password,
                        accessToken = auth.accessToken,
                        refreshToken = auth.refreshToken,
                        userId = auth.userId,
                        emailVerified = auth.emailVerified,
                        accessExpiresAt = auth.accessExpiresAt,
                        refreshExpiresAt = auth.refreshExpiresAt
                    )
                )
                if (auth.emailVerified) {
                    "已注册并登录同步账号。"
                } else {
                    "已注册账号，请尽快完成邮箱验证。"
                }
            }
        }
    }

    fun loginSyncAccount(baseUrl: String, email: String, password: String) {
        startBackupWork("正在登录同步账号...") {
            syncBackendService.login(baseUrl, email, password).map { auth ->
                saveBackendSettingsInternal(
                    uiState.backup.preferences.backendSettings.copy(
                        baseUrl = baseUrl.trim(),
                        email = auth.email.ifBlank { email.trim() },
                        password = password,
                        accessToken = auth.accessToken,
                        refreshToken = auth.refreshToken,
                        userId = auth.userId,
                        emailVerified = auth.emailVerified,
                        accessExpiresAt = auth.accessExpiresAt,
                        refreshExpiresAt = auth.refreshExpiresAt
                    )
                )
                "已登录同步账号。"
            }
        }
    }

    fun requestEmailVerification(baseUrl: String, email: String) {
        startBackupWork("正在发送验证邮件...") {
            syncBackendService.requestEmailVerification(baseUrl, email)
        }
    }

    fun verifyEmail(baseUrl: String, token: String) {
        startBackupWork("正在验证邮箱...") {
            syncBackendService.verifyEmail(baseUrl, token).map { status ->
                val backend = uiState.backup.preferences.backendSettings.copy(emailVerified = true)
                saveBackendSettingsInternal(backend)
                status
            }
        }
    }

    fun requestPasswordReset(baseUrl: String, email: String) {
        startBackupWork("正在发送重置邮件...") {
            syncBackendService.requestPasswordReset(baseUrl, email)
        }
    }

    fun resetPassword(baseUrl: String, token: String, newPassword: String) {
        startBackupWork("正在重置密码...") {
            syncBackendService.resetPassword(baseUrl, token, newPassword).map { status ->
                val backend = uiState.backup.preferences.backendSettings.copy(password = newPassword)
                saveBackendSettingsInternal(backend)
                status
            }
        }
    }

    fun pushBackendSync() {
        pushBackendSync(force = false)
    }

    fun forcePushBackendSync() {
        pushBackendSync(force = true)
    }

    private fun pushBackendSync(force: Boolean) {
        val preferences = uiState.backup.preferences
        startBackupWork(if (force) "正在强制覆盖云端..." else "正在上传账号云同步...") {
            try {
                require(preferences.passphrase.isNotBlank()) { "请先设置备份加密口令。" }
                val refreshed = refreshBackendSessionIfPossible(preferences.backendSettings)
                val encryptedJson = backupManager.createEncryptedEnvelopeJson(preferences.passphrase)
                val revision = syncBackendService.pushBackup(
                    settings = refreshed,
                    encryptedBackupJson = encryptedJson,
                    revision = System.currentTimeMillis(),
                    baseRevision = refreshed.lastSyncedRevision,
                    force = force
                ).getOrThrow()
                saveBackendSettingsInternal(refreshed.copy(lastSyncedRevision = revision))
                Result.success(
                    if (force) {
                        "已强制覆盖云端，服务器版本 $revision"
                    } else {
                        "已上传到账号云同步，服务器版本 $revision"
                    }
                )
            } catch (error: Throwable) {
                if (error is SyncConflictInfo) {
                    Result.failure(
                        IllegalStateException(
                            "检测到同步冲突。服务器版本 ${error.serverRevision}，来自 ${error.serverUpdatedByDevice}。建议先拉取同步，确认无误后再选择强制覆盖。"
                        )
                    )
                } else {
                    Result.failure(error)
                }
            }
        }
    }

    fun pullBackendSync() {
        val preferences = uiState.backup.preferences
        startBackupWork("正在从账号云同步恢复...") {
            runCatching {
                require(preferences.passphrase.isNotBlank()) { "请先设置备份加密口令。" }
                val refreshed = refreshBackendSessionIfPossible(preferences.backendSettings)
                val response = syncBackendService.pullBackup(refreshed).getOrThrow()
                val message = backupManager.restoreFromJson(response.encryptedBackupJson, preferences.passphrase)
                saveBackendSettingsInternal(refreshed.copy(lastSyncedRevision = response.revision))
                "$message 已同步到服务器版本 ${response.revision}"
            }
        }
    }

    fun selectPhoto(uri: Uri?) {
        uiState = uiState.copy(
            photo = PhotoAnalysisState(selectedImageUri = uri?.toString())
        )
    }

    fun analyzeSelectedPhoto() {
        val selectedUri = uiState.photo.selectedImageUri ?: run {
            pushNotice("请先拍照或从相册选择图片。")
            return
        }

        uiState = uiState.copy(
            photo = uiState.photo.copy(
                isAnalyzing = true,
                error = null,
                items = emptyList(),
                aiSummary = "",
                ocrText = ""
            )
        )

        viewModelScope.launch {
            val uri = Uri.parse(selectedUri)
            val ocrText = runCatching { photoAnalyzer.extractText(uri) }.getOrDefault("")
            val localItems = NutritionEngine.inferRecognizedFoods(ocrText)
            val imageBytes = runCatching { photoAnalyzer.readBytes(uri) }.getOrDefault(ByteArray(0))
            val aiResult = if (uiState.settings.apiKey.isNotBlank() && imageBytes.isNotEmpty()) {
                aiService.analyzeMeal(
                    settings = uiState.settings,
                    imageBytes = imageBytes,
                    ocrText = ocrText,
                    targetCalories = uiState.targetCalories,
                    consumedCalories = uiState.consumedCalories
                ).getOrNull()
            } else {
                null
            }

            val chosenItems = when {
                !aiResult?.items.isNullOrEmpty() -> aiResult?.items.orEmpty()
                localItems.isNotEmpty() -> localItems
                else -> emptyList()
            }

            val errorText = when {
                chosenItems.isNotEmpty() -> null
                uiState.settings.apiKey.isBlank() ->
                    "当前未配置 AI Key，纯 OCR 没识别出有效食物。可先配置 AI 再识别餐盘照片。"
                else -> "AI 未返回可用结果，请换一张更清晰的照片再试。"
            }

            uiState = uiState.copy(
                photo = uiState.photo.copy(
                    ocrText = ocrText,
                    items = chosenItems,
                    aiSummary = aiResult?.summary ?: if (chosenItems.isNotEmpty()) {
                        "已根据 OCR 或本地食物库给出估算结果。"
                    } else {
                        ""
                    },
                    isAnalyzing = false,
                    error = errorText
                )
            )
        }
    }

    fun addRecognizedItemsToToday() {
        val items = uiState.photo.items
        if (items.isEmpty()) {
            pushNotice("当前没有可加入的识别结果。")
            return
        }
        viewModelScope.launch {
            entryRepository.appendEntries(items.map(NutritionEngine::createPhotoEntry))
            uiState = uiState.copy(
                photo = uiState.photo.copy(
                    items = emptyList(),
                    aiSummary = "已加入今日记录。",
                    error = null
                )
            )
            pushNotice("已加入 ${items.size} 条拍照记录。")
        }
    }

    fun clearNotice() {
        if (uiState.notice != null) {
            uiState = uiState.copy(notice = null)
        }
    }

    fun recipesFor(cuisine: Cuisine): List<Recipe> {
        return NutritionEngine.sortedRecipes(
            recipes = RecipeCatalog.recipes.filter { it.cuisine == cuisine },
            consumed = uiState.consumedCalories,
            target = uiState.targetCalories
        )
    }

    fun recipeImpactText(recipe: Recipe): String {
        return NutritionEngine.recipeImpactText(
            recipe = recipe,
            consumed = uiState.consumedCalories,
            target = uiState.targetCalories
        )
    }

    private fun rebuildUi(
        entries: List<FoodEntry>,
        profile: UserProfile,
        settings: AiSettings,
        backupPreferences: BackupPreferences
    ) {
        val todayEntries = NutritionEngine.todayEntries(entries)
        val consumed = todayEntries.sumOf { it.calories }
        val target = NutritionEngine.calculateTargetCalories(profile)
        val currentLookup = uiState.lookup
        val currentBackup = uiState.backup
        val currentPhoto = uiState.photo
        val currentNotice = uiState.notice
        uiState = MainUiState(
            profile = profile,
            settings = settings,
            entries = entries,
            todayEntries = todayEntries,
            targetCalories = target,
            consumedCalories = consumed,
            remainingCalories = target - consumed,
            statusText = NutritionEngine.statusText(consumed, target),
            lookup = currentLookup,
            backup = BackupState(
                preferences = backupPreferences,
                isWorking = currentBackup.isWorking,
                status = currentBackup.status,
                error = currentBackup.error
            ),
            photo = currentPhoto,
            notice = currentNotice
        )
    }

    private fun startBackupWork(workingStatus: String, block: suspend () -> Result<String>) {
        uiState = uiState.copy(
            backup = uiState.backup.copy(isWorking = true, error = null, status = workingStatus)
        )
        viewModelScope.launch {
            val result = block()
            updateBackupResult(result.getOrNull(), result.exceptionOrNull()?.message)
            if (result.isSuccess) {
                pushNotice(result.getOrNull().orEmpty())
            }
        }
    }

    private suspend fun saveBackendSettingsInternal(settings: SyncBackendSettings) {
        val updatedPreferences = uiState.backup.preferences.copy(
            backendSettings = withDeviceId(uiState.backup.preferences).backendSettings.copy(
                baseUrl = settings.baseUrl,
                email = settings.email,
                password = settings.password,
                accessToken = settings.accessToken,
                refreshToken = settings.refreshToken,
                userId = settings.userId,
                emailVerified = settings.emailVerified,
                accessExpiresAt = settings.accessExpiresAt,
                refreshExpiresAt = settings.refreshExpiresAt,
                lastSyncedRevision = settings.lastSyncedRevision
            )
        )
        storage.saveBackupPreferences(updatedPreferences)
    }

    private suspend fun ensureDeviceId() {
        val preferences = storage.backupPreferencesFlow.first()
        val updated = withDeviceId(preferences)
        if (updated != preferences) {
            storage.saveBackupPreferences(updated)
        }
    }

    private fun withDeviceId(preferences: BackupPreferences): BackupPreferences {
        val currentId = preferences.backendSettings.deviceId
        if (currentId.isNotBlank()) {
            return preferences
        }
        val androidId = Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty().ifBlank { "device-${System.currentTimeMillis()}" }
        return preferences.copy(
            backendSettings = preferences.backendSettings.copy(deviceId = androidId)
        )
    }

    private fun pushNotice(message: String) {
        uiState = uiState.copy(notice = message)
    }

    private suspend fun refreshBackendSessionIfPossible(settings: SyncBackendSettings): SyncBackendSettings {
        if (settings.refreshToken.isBlank()) {
            return settings
        }
        val refreshed = syncBackendService.refresh(settings.baseUrl, settings.refreshToken).getOrNull()
            ?: return settings
        val updated = settings.copy(
            accessToken = refreshed.accessToken,
            refreshToken = refreshed.refreshToken,
            emailVerified = refreshed.emailVerified,
            accessExpiresAt = refreshed.accessExpiresAt,
            refreshExpiresAt = refreshed.refreshExpiresAt
        )
        saveBackendSettingsInternal(updated)
        return updated
    }

    private fun setBackupError(message: String) {
        uiState = uiState.copy(
            backup = uiState.backup.copy(isWorking = false, error = message)
        )
    }

    private fun updateBackupResult(status: String?, error: String?) {
        uiState = uiState.copy(
            backup = uiState.backup.copy(
                isWorking = false,
                status = status.orEmpty(),
                error = error
            )
        )
    }
}

private data class UiInputs(
    val entries: List<FoodEntry>,
    val profile: UserProfile,
    val aiSettings: AiSettings,
    val backupPreferences: BackupPreferences
)

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
