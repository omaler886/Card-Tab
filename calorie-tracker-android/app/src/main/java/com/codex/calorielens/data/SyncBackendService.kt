package com.codex.calorielens.data

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAt: Long,
    val refreshExpiresAt: Long,
    val userId: String,
    val email: String,
    val emailVerified: Boolean
)

data class PullResponse(
    val revision: Long,
    val updatedAt: Long,
    val updatedByDevice: String,
    val encryptedBackupJson: String
)

data class SyncConflictInfo(
    val serverRevision: Long,
    val serverUpdatedAt: Long,
    val serverUpdatedByDevice: String,
    val clientBaseRevision: Long,
    override val message: String
) : IOException(message)

class SyncBackendService {
    private val client = OkHttpClient.Builder().build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun register(baseUrl: String, email: String, password: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = request(
                    url = "${baseUrl.trimEnd('/')}/api/auth/register",
                    method = "POST",
                    jsonBody = JSONObject()
                        .put("email", email.trim())
                        .put("password", password)
                        .toString(),
                    bearerToken = null
                )
                parseAuth(body)
            }
        }

    suspend fun login(baseUrl: String, email: String, password: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = request(
                    url = "${baseUrl.trimEnd('/')}/api/auth/login",
                    method = "POST",
                    jsonBody = JSONObject()
                        .put("email", email.trim())
                        .put("password", password)
                        .toString(),
                    bearerToken = null
                )
                parseAuth(body)
            }
        }

    suspend fun refresh(baseUrl: String, refreshToken: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = request(
                    url = "${baseUrl.trimEnd('/')}/api/auth/refresh",
                    method = "POST",
                    jsonBody = JSONObject()
                        .put("refresh_token", refreshToken)
                        .toString(),
                    bearerToken = null
                )
                parseAuth(body)
            }
        }

    suspend fun requestEmailVerification(baseUrl: String, email: String): Result<String> =
        simpleEmailAction("${baseUrl.trimEnd('/')}/api/auth/request-email-verification", email)

    suspend fun verifyEmail(baseUrl: String, token: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = request(
                    url = "${baseUrl.trimEnd('/')}/api/auth/verify-email",
                    method = "POST",
                    jsonBody = JSONObject().put("token", token).toString(),
                    bearerToken = null
                )
                parseStatusWithDebugToken(body)
            }
        }

    suspend fun requestPasswordReset(baseUrl: String, email: String): Result<String> =
        simpleEmailAction("${baseUrl.trimEnd('/')}/api/auth/request-password-reset", email)

    suspend fun resetPassword(baseUrl: String, token: String, newPassword: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = request(
                    url = "${baseUrl.trimEnd('/')}/api/auth/reset-password",
                    method = "POST",
                    jsonBody = JSONObject()
                        .put("token", token)
                        .put("new_password", newPassword)
                        .toString(),
                    bearerToken = null
                )
                parseStatusWithDebugToken(body)
            }
        }

    suspend fun pushBackup(
        settings: SyncBackendSettings,
        encryptedBackupJson: String,
        revision: Long,
        baseRevision: Long,
        force: Boolean = false
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            require(settings.accessToken.isNotBlank()) { "请先登录同步账号。" }
            val body = request(
                url = "${settings.baseUrl.trimEnd('/')}/api/sync/push",
                method = "POST",
                jsonBody = JSONObject()
                    .put("device_id", settings.deviceId)
                    .put("device_name", android.os.Build.MODEL ?: "Android")
                    .put("revision", revision)
                    .put("base_revision", baseRevision)
                    .put("force", force)
                    .put("backup_envelope", JSONObject(encryptedBackupJson))
                    .toString(),
                bearerToken = settings.accessToken
            )
            JSONObject(body).optLong("revision", revision)
        }
    }

    suspend fun pullBackup(settings: SyncBackendSettings): Result<PullResponse> = withContext(Dispatchers.IO) {
        runCatching {
            require(settings.accessToken.isNotBlank()) { "请先登录同步账号。" }
            val body = request(
                url = "${settings.baseUrl.trimEnd('/')}/api/sync/pull?device_id=${settings.deviceId}",
                method = "GET",
                jsonBody = null,
                bearerToken = settings.accessToken
            )
            val root = JSONObject(body)
            PullResponse(
                revision = root.optLong("revision", 0L),
                updatedAt = root.optLong("updated_at", 0L),
                updatedByDevice = root.optString("updated_by_device"),
                encryptedBackupJson = root.optJSONObject("backup_envelope")?.toString()
                    ?: throw IOException("云端没有可恢复的备份。")
            )
        }
    }

    private suspend fun simpleEmailAction(url: String, email: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = request(
                    url = url,
                    method = "POST",
                    jsonBody = JSONObject().put("email", email.trim()).toString(),
                    bearerToken = null
                )
                parseStatusWithDebugToken(body)
            }
        }

    private fun parseAuth(body: String): AuthResponse {
        val root = JSONObject(body)
        val user = root.optJSONObject("user") ?: JSONObject()
        return AuthResponse(
            accessToken = root.optString("access_token"),
            refreshToken = root.optString("refresh_token"),
            accessExpiresAt = root.optLong("access_expires_at", 0L),
            refreshExpiresAt = root.optLong("refresh_expires_at", 0L),
            userId = user.optString("id"),
            email = user.optString("email"),
            emailVerified = user.optBoolean("email_verified", false)
        )
    }

    private fun parseStatusWithDebugToken(body: String): String {
        val root = JSONObject(body)
        val status = root.optString("status", "ok")
        val token = root.optString("debug_token")
        return if (token.isNotBlank()) {
            "$status，测试令牌：$token"
        } else {
            status
        }
    }

    private fun request(
        url: String,
        method: String,
        jsonBody: String?,
        bearerToken: String?
    ): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
        if (!bearerToken.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $bearerToken")
        }
        when (method) {
            "POST" -> requestBuilder.post((jsonBody ?: "{}").toRequestBody(jsonMediaType))
            "GET" -> requestBuilder.get()
            else -> error("Unsupported method $method")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (response.code == 409) {
                val root = JSONObject(body)
                throw SyncConflictInfo(
                    serverRevision = root.optLong("server_revision", 0L),
                    serverUpdatedAt = root.optLong("server_updated_at", 0L),
                    serverUpdatedByDevice = root.optString("server_updated_by_device"),
                    clientBaseRevision = root.optLong("client_base_revision", 0L),
                    message = root.optString("message", "同步冲突")
                )
            }
            if (!response.isSuccessful) {
                throw IOException("同步服务错误 ${response.code}: $body")
            }
            return body
        }
    }
}
