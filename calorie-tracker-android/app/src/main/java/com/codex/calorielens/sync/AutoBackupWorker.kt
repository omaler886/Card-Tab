package com.codex.calorielens.sync

import android.content.Context
import android.provider.Settings
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.codex.calorielens.data.AppStorage
import com.codex.calorielens.data.BackupManager
import com.codex.calorielens.data.BackupTarget
import com.codex.calorielens.data.CalorieLensDatabase
import com.codex.calorielens.data.EntryRepository
import com.codex.calorielens.data.SyncBackendService
import kotlinx.coroutines.flow.first

class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val storage = AppStorage(applicationContext)
        val repository = EntryRepository(
            dao = CalorieLensDatabase.getInstance(applicationContext).foodEntryDao(),
            storage = storage
        )
        repository.initialize()
        val backupManager = BackupManager(applicationContext, storage, repository)
        val syncService = SyncBackendService()
        val preferences = storage.backupPreferencesFlow.first()
        val passphrase = preferences.passphrase
        if (!preferences.autoBackupSettings.enabled) {
            return Result.success()
        }
        if (passphrase.isBlank()) {
            return Result.failure()
        }

        return when (preferences.autoBackupSettings.target) {
            BackupTarget.WEBDAV -> {
                backupManager.uploadToWebDav(preferences.webDavSettings, passphrase)
                    .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
            }
            BackupTarget.BACKEND -> {
                val backendSettings = preferences.backendSettings
                val deviceId = backendSettings.deviceId.ifBlank {
                    Settings.Secure.getString(
                        applicationContext.contentResolver,
                        Settings.Secure.ANDROID_ID
                    ).orEmpty()
                }
                if ((backendSettings.accessToken.isBlank() && backendSettings.refreshToken.isBlank()) || deviceId.isBlank()) {
                    Result.failure()
                } else {
                    val refreshedSettings = if (backendSettings.refreshToken.isNotBlank()) {
                        syncService.refresh(backendSettings.baseUrl, backendSettings.refreshToken)
                            .getOrNull()
                            ?.let { auth ->
                                val updated = backendSettings.copy(
                                    accessToken = auth.accessToken,
                                    refreshToken = auth.refreshToken,
                                    emailVerified = auth.emailVerified,
                                    accessExpiresAt = auth.accessExpiresAt,
                                    refreshExpiresAt = auth.refreshExpiresAt
                                )
                                storage.saveBackupPreferences(
                                    preferences.copy(backendSettings = updated)
                                )
                                updated
                            } ?: backendSettings
                    } else {
                        backendSettings
                    }
                    val encryptedJson = backupManager.createEncryptedEnvelopeJson(passphrase)
                    syncService.pushBackup(
                        settings = refreshedSettings.copy(deviceId = deviceId),
                        encryptedBackupJson = encryptedJson,
                        revision = System.currentTimeMillis(),
                        baseRevision = refreshedSettings.lastSyncedRevision
                    ).fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
                }
            }
        }
    }
}
