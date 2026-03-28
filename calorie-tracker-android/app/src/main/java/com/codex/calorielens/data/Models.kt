package com.codex.calorielens.data

import java.util.UUID

enum class Cuisine(val label: String) {
    CHINESE("中式"),
    AMERICAN("美式"),
    OTHER("其他");

    companion object {
        fun fromRaw(raw: String?): Cuisine {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: OTHER
        }
    }
}

enum class FoodRegion(val label: String) {
    CHINA("中国"),
    USA("美国"),
    GLOBAL("全球")
}

enum class EntrySource(val label: String) {
    MANUAL("手填"),
    DATABASE("数据库"),
    PHOTO("拍照"),
    RECIPE("食谱")
}

enum class Sex(val label: String) {
    MALE("男"),
    FEMALE("女")
}

enum class ActivityLevel(val label: String, val factor: Double) {
    SEDENTARY("久坐", 1.2),
    LIGHT("轻运动", 1.375),
    MODERATE("中等运动", 1.55),
    HIGH("高强度", 1.725)
}

enum class GoalType(val label: String, val calorieDelta: Int) {
    CUT("减脂", -350),
    MAINTAIN("维持", 0),
    GAIN("增肌", 250)
}

data class FoodEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val calories: Int,
    val servings: Double = 1.0,
    val cuisine: Cuisine = Cuisine.OTHER,
    val source: EntrySource = EntrySource.MANUAL,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class RecognizedFood(
    val name: String,
    val calories: Int,
    val servings: Double = 1.0,
    val cuisine: Cuisine = Cuisine.OTHER,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val note: String = "",
    val confidence: Double = 0.5
)

data class FoodLookupItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val brand: String = "",
    val barcode: String = "",
    val calories: Int,
    val servings: Double = 1.0,
    val servingLabel: String = "1 份",
    val cuisine: Cuisine = Cuisine.OTHER,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val sourceLabel: String = "本地库",
    val note: String = ""
)

data class Recipe(
    val id: String,
    val title: String,
    val mealLabel: String,
    val cuisine: Cuisine,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val highlight: String,
    val ingredients: List<String>
)

data class UserProfile(
    val age: Int = 28,
    val heightCm: Int = 170,
    val weightKg: Double = 65.0,
    val sex: Sex = Sex.MALE,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goalType: GoalType = GoalType.CUT
)

data class AiSettings(
    val baseUrl: String = "https://api.openai.com/v1/chat/completions",
    val apiKey: String = "",
    val model: String = "gpt-4.1-mini"
)

data class WebDavSettings(
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val remotePath: String = "CalorieLens/backup.json"
)

enum class BackupTarget(val label: String) {
    WEBDAV("WebDAV"),
    BACKEND("账号云同步")
}

data class SyncBackendSettings(
    val baseUrl: String = "http://10.0.2.2:8000",
    val email: String = "",
    val password: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val userId: String = "",
    val deviceId: String = "",
    val emailVerified: Boolean = false,
    val accessExpiresAt: Long = 0L,
    val refreshExpiresAt: Long = 0L,
    val lastSyncedRevision: Long = 0L
)

data class AutoBackupSettings(
    val enabled: Boolean = false,
    val intervalHours: Int = 24,
    val target: BackupTarget = BackupTarget.WEBDAV
)

data class BackupPreferences(
    val passphrase: String = "",
    val webDavSettings: WebDavSettings = WebDavSettings(),
    val backendSettings: SyncBackendSettings = SyncBackendSettings(),
    val autoBackupSettings: AutoBackupSettings = AutoBackupSettings()
)

data class BackupSnapshot(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val profile: UserProfile = UserProfile(),
    val aiSettings: AiSettings = AiSettings(),
    val entries: List<FoodEntry> = emptyList()
)

data class EncryptedBackupEnvelope(
    val schemaVersion: Int = 1,
    val encrypted: Boolean = true,
    val algorithm: String = "AES-256-GCM",
    val kdf: String = "PBKDF2WithHmacSHA256",
    val iterations: Int = 120_000,
    val saltBase64: String,
    val ivBase64: String,
    val cipherTextBase64: String
)

data class PhotoAnalysisState(
    val selectedImageUri: String? = null,
    val ocrText: String = "",
    val items: List<RecognizedFood> = emptyList(),
    val aiSummary: String = "",
    val isAnalyzing: Boolean = false,
    val error: String? = null
)

data class FoodLookupState(
    val region: FoodRegion = FoodRegion.CHINA,
    val lastBarcode: String = "",
    val isLoading: Boolean = false,
    val results: List<FoodLookupItem> = emptyList(),
    val error: String? = null
)

data class HistoryPoint(
    val label: String,
    val calories: Int,
    val isToday: Boolean
)

data class BreakdownPoint(
    val label: String,
    val value: Int,
    val colorArgb: Long
)

data class BackupState(
    val preferences: BackupPreferences = BackupPreferences(),
    val isWorking: Boolean = false,
    val status: String = "",
    val error: String? = null
)

data class MainUiState(
    val profile: UserProfile = UserProfile(),
    val settings: AiSettings = AiSettings(),
    val entries: List<FoodEntry> = emptyList(),
    val todayEntries: List<FoodEntry> = emptyList(),
    val targetCalories: Int = 0,
    val consumedCalories: Int = 0,
    val remainingCalories: Int = 0,
    val statusText: String = "",
    val lookup: FoodLookupState = FoodLookupState(),
    val backup: BackupState = BackupState(),
    val photo: PhotoAnalysisState = PhotoAnalysisState(),
    val notice: String? = null
)
