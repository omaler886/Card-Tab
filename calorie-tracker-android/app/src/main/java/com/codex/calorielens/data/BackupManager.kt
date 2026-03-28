package com.codex.calorielens.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import java.io.IOException
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class BackupManager(
    private val context: Context,
    private val storage: AppStorage,
    private val entryRepository: EntryRepository
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder().build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val secureRandom = SecureRandom()

    suspend fun exportToUri(uri: Uri, passphrase: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(passphrase.isNotBlank()) { "请先设置备份加密口令。" }
            val envelopeJson = createEncryptedEnvelopeJson(passphrase)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(envelopeJson.toByteArray(Charsets.UTF_8))
            } ?: throw IOException("无法打开导出文件。")
            val count = entryRepository.readAllEntries().size
            "已导出并加密 ${count} 条记录。"
        }
    }

    suspend fun restoreFromUri(uri: Uri, passphrase: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: throw IOException("无法读取备份文件。")
            restoreFromJson(json, passphrase)
        }
    }

    suspend fun uploadToWebDav(settings: WebDavSettings, passphrase: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(passphrase.isNotBlank()) { "请先设置备份加密口令。" }
                validateWebDavSettings(settings)
                ensureRemoteFolder(settings)
                val encryptedJson = createEncryptedEnvelopeJson(passphrase)
                val request = Request.Builder()
                    .url(buildRemoteUrl(settings))
                    .put(encryptedJson.toRequestBody(jsonMediaType))
                    .addHeader("Authorization", Credentials.basic(settings.username.trim(), settings.password))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("云端备份失败 ${response.code}: ${response.body?.string().orEmpty()}")
                    }
                }
                "已加密上传到 WebDAV: ${settings.remotePath}"
            }
        }

    suspend fun restoreFromWebDav(settings: WebDavSettings, passphrase: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                validateWebDavSettings(settings)
                val request = Request.Builder()
                    .url(buildRemoteUrl(settings))
                    .get()
                    .addHeader("Authorization", Credentials.basic(settings.username.trim(), settings.password))
                    .build()

                val json = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("云端恢复失败 ${response.code}: ${response.body?.string().orEmpty()}")
                    }
                    response.body?.string().orEmpty()
                }
                restoreFromJson(json, passphrase)
            }
        }

    suspend fun createEncryptedEnvelopeJson(passphrase: String): String {
        val snapshotJson = gson.toJson(snapshot())
        return gson.toJson(encryptSnapshot(snapshotJson, passphrase))
    }

    suspend fun restoreFromJson(json: String, passphrase: String): String {
        val snapshot = parseSnapshot(json, passphrase)
        entryRepository.replaceAllEntries(snapshot.entries.sortedByDescending { it.createdAt })
        storage.saveProfile(snapshot.profile)
        storage.saveAiSettings(snapshot.aiSettings)
        return "已恢复 ${snapshot.entries.size} 条记录。"
    }

    private suspend fun snapshot(): BackupSnapshot {
        return BackupSnapshot(
            profile = storage.profileFlow.firstValue(),
            aiSettings = storage.aiSettingsFlow.firstValue(),
            entries = entryRepository.readAllEntries()
        )
    }

    private fun parseSnapshot(json: String, passphrase: String): BackupSnapshot {
        val trimmed = json.trim()
        val encryptedEnvelope = runCatching {
            gson.fromJson(trimmed, EncryptedBackupEnvelope::class.java)
        }.getOrNull()
        if (encryptedEnvelope != null &&
            encryptedEnvelope.encrypted &&
            encryptedEnvelope.cipherTextBase64.isNotBlank()
        ) {
            require(passphrase.isNotBlank()) { "恢复加密备份需要口令。" }
            val decryptedJson = decryptEnvelope(encryptedEnvelope, passphrase)
            return gson.fromJson(decryptedJson, BackupSnapshot::class.java)
                ?: throw IOException("解密后备份内容为空。")
        }

        return gson.fromJson(trimmed, BackupSnapshot::class.java)
            ?: throw IOException("备份文件格式不正确。")
    }

    private fun encryptSnapshot(snapshotJson: String, passphrase: String): EncryptedBackupEnvelope {
        val salt = ByteArray(16).also(secureRandom::nextBytes)
        val iv = ByteArray(12).also(secureRandom::nextBytes)
        val secretKey = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val cipherBytes = cipher.doFinal(snapshotJson.toByteArray(Charsets.UTF_8))

        return EncryptedBackupEnvelope(
            saltBase64 = Base64.getEncoder().encodeToString(salt),
            ivBase64 = Base64.getEncoder().encodeToString(iv),
            cipherTextBase64 = Base64.getEncoder().encodeToString(cipherBytes)
        )
    }

    private fun decryptEnvelope(envelope: EncryptedBackupEnvelope, passphrase: String): String {
        val salt = Base64.getDecoder().decode(envelope.saltBase64)
        val iv = Base64.getDecoder().decode(envelope.ivBase64)
        val cipherBytes = Base64.getDecoder().decode(envelope.cipherTextBase64)
        val secretKey = deriveKey(passphrase, salt, envelope.iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        return try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (error: Throwable) {
            throw IOException("备份口令错误或文件已损坏。", error)
        }
    }

    private fun deriveKey(passphrase: String, salt: ByteArray, iterations: Int = 120_000): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun validateWebDavSettings(settings: WebDavSettings) {
        require(settings.baseUrl.isNotBlank()) { "请先填写 WebDAV 地址。" }
        require(settings.username.isNotBlank()) { "请先填写 WebDAV 用户名。" }
        require(settings.password.isNotBlank()) { "请先填写 WebDAV 密码。" }
        require(settings.remotePath.isNotBlank()) { "请先填写远程备份路径。" }
    }

    private fun buildRemoteUrl(settings: WebDavSettings): String {
        val base = settings.baseUrl.trim().trimEnd('/')
        val path = settings.remotePath.trim().trimStart('/')
            .split('/')
            .filter { it.isNotBlank() }
            .joinToString("/") { segment ->
                URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
            }
        return "$base/$path"
    }

    private fun ensureRemoteFolder(settings: WebDavSettings) {
        val base = settings.baseUrl.trim().trimEnd('/')
        val segments = settings.remotePath.trim().trim('/').split('/').dropLast(1)
        if (segments.isEmpty()) {
            return
        }
        var current = base
        for (segment in segments) {
            val encodedSegment = URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
            current = "$current/$encodedSegment"
            val request = Request.Builder()
                .url(current)
                .method("MKCOL", ByteArray(0).toRequestBody(null))
                .addHeader("Authorization", Credentials.basic(settings.username.trim(), settings.password))
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code !in listOf(200, 201, 204, 301, 405)) {
                    throw IOException("无法创建云端目录 ${settings.remotePath}: ${response.code}")
                }
            }
        }
    }

    private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.firstValue(): T {
        return first()
    }
}
