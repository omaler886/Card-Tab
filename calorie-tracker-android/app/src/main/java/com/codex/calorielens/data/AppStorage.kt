package com.codex.calorielens.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.calorieLensStore by preferencesDataStore(name = "calorie_lens_store")

class AppStorage(private val context: Context) {
    private val gson = Gson()
    private val entriesKey = stringPreferencesKey("entries_json")
    private val entriesMigratedKey = booleanPreferencesKey("entries_migrated_to_room")
    private val profileKey = stringPreferencesKey("profile_json")
    private val aiSettingsKey = stringPreferencesKey("ai_settings_json")
    private val backupPreferencesKey = stringPreferencesKey("backup_preferences_json")
    private val entriesType = object : TypeToken<List<FoodEntry>>() {}.type

    private val preferencesFlow = context.calorieLensStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
    val legacyEntriesFlow: Flow<List<FoodEntry>> = preferencesFlow.map { preferences ->
        decodeEntries(preferences[entriesKey])
    }

    val profileFlow: Flow<UserProfile> = preferencesFlow
        .map { preferences ->
            preferences[profileKey]?.let(::decodeProfile) ?: UserProfile()
        }

    val aiSettingsFlow: Flow<AiSettings> = preferencesFlow
        .map { preferences ->
            preferences[aiSettingsKey]?.let(::decodeAiSettings) ?: AiSettings()
        }

    val backupPreferencesFlow: Flow<BackupPreferences> = preferencesFlow
        .map { preferences ->
            preferences[backupPreferencesKey]?.let(::decodeBackupPreferences) ?: BackupPreferences()
        }

    suspend fun readLegacyEntries(): List<FoodEntry> {
        return legacyEntriesFlow.first()
    }

    suspend fun isLegacyEntriesMigrated(): Boolean {
        return preferencesFlow.first()[entriesMigratedKey] ?: false
    }

    suspend fun markLegacyEntriesMigrated() {
        context.calorieLensStore.edit { preferences ->
            preferences[entriesMigratedKey] = true
            preferences.remove(entriesKey)
        }
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.calorieLensStore.edit { preferences ->
            preferences[profileKey] = gson.toJson(profile)
        }
    }

    suspend fun saveAiSettings(settings: AiSettings) {
        context.calorieLensStore.edit { preferences ->
            preferences[aiSettingsKey] = gson.toJson(settings)
        }
    }

    suspend fun saveBackupPreferences(settings: BackupPreferences) {
        context.calorieLensStore.edit { preferences ->
            preferences[backupPreferencesKey] = gson.toJson(settings)
        }
    }

    private fun decodeEntries(raw: String?): List<FoodEntry> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }
        return runCatching {
            gson.fromJson<List<FoodEntry>>(raw, entriesType) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    private fun decodeProfile(raw: String): UserProfile {
        return runCatching {
            gson.fromJson(raw, UserProfile::class.java)
        }.getOrDefault(UserProfile())
    }

    private fun decodeAiSettings(raw: String): AiSettings {
        return runCatching {
            gson.fromJson(raw, AiSettings::class.java)
        }.getOrDefault(AiSettings())
    }

    private fun decodeBackupPreferences(raw: String): BackupPreferences {
        return runCatching {
            gson.fromJson(raw, BackupPreferences::class.java)
        }.getOrDefault(BackupPreferences())
    }
}
