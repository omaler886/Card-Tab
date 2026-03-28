package com.codex.calorielens.ai

import android.util.Base64
import com.codex.calorielens.data.AiSettings
import com.codex.calorielens.data.Cuisine
import com.codex.calorielens.data.RecognizedFood
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

data class AiMealResult(
    val items: List<RecognizedFood>,
    val summary: String,
    val rawResponse: String
)

class AiNutritionService {
    private val client = OkHttpClient.Builder().build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun analyzeMeal(
        settings: AiSettings,
        imageBytes: ByteArray,
        ocrText: String,
        targetCalories: Int,
        consumedCalories: Int
    ): Result<AiMealResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(settings.apiKey.isNotBlank()) { "AI Key 为空" }
            require(imageBytes.isNotEmpty()) { "图片为空" }

            val responseText = try {
                execute(
                    settings = settings,
                    body = buildRequestBody(
                        settings = settings,
                        imageBytes = imageBytes,
                        ocrText = ocrText,
                        targetCalories = targetCalories,
                        consumedCalories = consumedCalories,
                        includeResponseFormat = true
                    )
                )
            } catch (firstError: Throwable) {
                execute(
                    settings = settings,
                    body = buildRequestBody(
                        settings = settings,
                        imageBytes = imageBytes,
                        ocrText = ocrText,
                        targetCalories = targetCalories,
                        consumedCalories = consumedCalories,
                        includeResponseFormat = false
                    ),
                    previousError = firstError
                )
            }

            val parsed = JSONObject(responseText)
            val message = parsed.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?: throw IOException("AI 未返回 choices[0].message")
            val content = extractContent(message)
            val payload = JSONObject(sanitizeJson(content))
            AiMealResult(
                items = payload.optJSONArray("items").toRecognizedFoods(),
                summary = payload.optString("summary", "AI 已完成本次分析。"),
                rawResponse = payload.toString()
            )
        }
    }

    private fun execute(
        settings: AiSettings,
        body: JSONObject,
        previousError: Throwable? = null
    ): String {
        val request = Request.Builder()
            .url(settings.baseUrl.trim())
            .addHeader("Authorization", "Bearer ${settings.apiKey.trim()}")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val suffix = if (previousError != null) {
                    "；首次失败原因：${previousError.message}"
                } else {
                    ""
                }
                throw IOException("AI 接口错误 ${response.code}: $responseBody$suffix")
            }
            return responseBody
        }
    }

    private fun buildRequestBody(
        settings: AiSettings,
        imageBytes: ByteArray,
        ocrText: String,
        targetCalories: Int,
        consumedCalories: Int,
        includeResponseFormat: Boolean
    ): JSONObject {
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val prompt = """
            你是营养师兼卡路里分析助手。
            请识别图片中的主要食物，结合 OCR 文本估算份量与营养，并输出严格 JSON。
            输出格式：
            {
              "items": [
                {
                  "name": "食物名",
                  "calories": 320,
                  "servings": 1.0,
                  "protein": 20,
                  "carbs": 25,
                  "fat": 12,
                  "cuisine": "CHINESE|AMERICAN|OTHER",
                  "note": "估算依据"
                }
              ],
              "summary": "一句中文总结，说明今天是否接近目标"
            }
            约束：
            - calories/protein/carbs/fat 必须是数字
            - 不确定时也要给最合理估算
            - 可识别多个食物
            附加信息：
            - OCR 文本：${ocrText.ifBlank { "无" }}
            - 今日目标：$targetCalories kcal
            - 今日已摄入：$consumedCalories kcal
        """.trimIndent()

        val messages = JSONArray()
            .put(
                JSONObject()
                    .put("role", "system")
                    .put("content", "You estimate nutrition from meal photos and return concise JSON.")
            )
            .put(
                JSONObject()
                    .put("role", "user")
                    .put(
                        "content",
                        JSONArray()
                            .put(
                                JSONObject()
                                    .put("type", "text")
                                    .put("text", prompt)
                            )
                            .put(
                                JSONObject()
                                    .put("type", "image_url")
                                    .put(
                                        "image_url",
                                        JSONObject()
                                            .put("url", "data:image/jpeg;base64,$base64Image")
                                            .put("detail", "low")
                                    )
                            )
                    )
            )

        return JSONObject()
            .put("model", settings.model.trim())
            .put("messages", messages)
            .put("temperature", 0.2)
            .also { root ->
                if (includeResponseFormat) {
                    root.put("response_format", JSONObject().put("type", "json_object"))
                }
            }
    }

    private fun extractContent(message: JSONObject): String {
        return when (val content = message.opt("content")) {
            is String -> content
            is JSONArray -> buildString {
                for (index in 0 until content.length()) {
                    val part = content.optJSONObject(index) ?: continue
                    append(part.optString("text"))
                }
            }
            else -> ""
        }
    }

    private fun sanitizeJson(raw: String): String {
        val trimmed = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) {
            trimmed.substring(start, end + 1)
        } else {
            trimmed
        }
    }

    private fun JSONArray?.toRecognizedFoods(): List<RecognizedFood> {
        if (this == null) {
            return emptyList()
        }
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    RecognizedFood(
                        name = item.optString("name", "识别食物"),
                        calories = item.optDouble("calories", 0.0).roundToInt().coerceAtLeast(0),
                        servings = item.optDouble("servings", 1.0).coerceAtLeast(0.1),
                        cuisine = Cuisine.fromRaw(item.optString("cuisine", Cuisine.OTHER.name)),
                        protein = item.optDouble("protein", 0.0).roundToInt().coerceAtLeast(0),
                        carbs = item.optDouble("carbs", 0.0).roundToInt().coerceAtLeast(0),
                        fat = item.optDouble("fat", 0.0).roundToInt().coerceAtLeast(0),
                        note = item.optString("note", "AI 估算"),
                        confidence = 0.82
                    )
                )
            }
        }
    }
}
