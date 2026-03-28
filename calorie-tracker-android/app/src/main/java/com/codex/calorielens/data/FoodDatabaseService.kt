package com.codex.calorielens.data

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

class FoodDatabaseService {
    private val client = OkHttpClient.Builder().build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun lookupBarcode(barcode: String, region: FoodRegion): Result<List<FoodLookupItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalizedBarcode = barcode.filter(Char::isDigit)
                require(normalizedBarcode.isNotBlank()) { "条码为空" }

                val results = buildList {
                    fetchOpenFoodFactsProduct(normalizedBarcode)?.let(::add)
                    if (region != FoodRegion.CHINA) {
                        addAll(fetchUsdaFoods(query = normalizedBarcode, barcode = normalizedBarcode, pageSize = 4))
                    }
                }.distinctBy { listOf(it.name, it.brand, it.barcode, it.sourceLabel).joinToString("|") }

                if (results.isEmpty()) {
                    throw IOException("暂时没有查到这个条码。")
                }
                results
            }
        }

    suspend fun searchFoods(query: String, region: FoodRegion): Result<List<FoodLookupItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val safeQuery = query.trim()
                require(safeQuery.isNotBlank()) { "请输入要搜索的食物名称" }

                val local = FoodCatalog.search(safeQuery, region).map(::toLocalLookupItem)
                val remote = if (region == FoodRegion.CHINA) {
                    emptyList()
                } else {
                    fetchUsdaFoods(query = safeQuery, barcode = null, pageSize = 8)
                }

                (local + remote)
                    .distinctBy { listOf(it.name, it.brand, it.sourceLabel).joinToString("|") }
                    .take(14)
            }
        }

    private fun fetchOpenFoodFactsProduct(barcode: String): FoodLookupItem? {
        val url = "https://world.openfoodfacts.org/api/v2/product/$barcode" +
            "?fields=code,product_name,product_name_en,brands,quantity,serving_size,serving_quantity,countries_tags,nutriments"
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "CalorieLens/1.1 (Codex Android App)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return null
            }
            val body = response.body?.string().orEmpty()
            val root = JSONObject(body)
            if (root.optInt("status") != 1) {
                return null
            }
            val product = root.optJSONObject("product") ?: return null
            val nutriments = product.optJSONObject("nutriments") ?: JSONObject()
            val calories = firstPositiveDouble(
                nutriments.optDouble("energy-kcal_serving", Double.NaN),
                nutriments.optDouble("energy-kcal", Double.NaN),
                nutriments.optDouble("energy-kcal_100g", Double.NaN)
            ).roundToInt()

            if (calories <= 0) {
                return null
            }

            val usingPerServing = nutriments.has("energy-kcal_serving") &&
                !nutriments.isNull("energy-kcal_serving")
            val protein = firstPositiveDouble(
                nutriments.optDouble(if (usingPerServing) "proteins_serving" else "proteins", Double.NaN),
                nutriments.optDouble("proteins_100g", Double.NaN),
                nutriments.optDouble("proteins", Double.NaN)
            ).roundToInt()
            val carbs = firstPositiveDouble(
                nutriments.optDouble(if (usingPerServing) "carbohydrates_serving" else "carbohydrates", Double.NaN),
                nutriments.optDouble("carbohydrates_100g", Double.NaN),
                nutriments.optDouble("carbohydrates", Double.NaN)
            ).roundToInt()
            val fat = firstPositiveDouble(
                nutriments.optDouble(if (usingPerServing) "fat_serving" else "fat", Double.NaN),
                nutriments.optDouble("fat_100g", Double.NaN),
                nutriments.optDouble("fat", Double.NaN)
            ).roundToInt()

            val servingLabel = when {
                usingPerServing && product.optString("serving_size").isNotBlank() -> product.optString("serving_size")
                product.optString("quantity").isNotBlank() -> product.optString("quantity")
                else -> "100g"
            }

            return FoodLookupItem(
                name = product.optString("product_name").ifBlank {
                    product.optString("product_name_en", "条码食品")
                },
                brand = product.optString("brands"),
                barcode = product.optString("code", barcode),
                calories = calories,
                servings = 1.0,
                servingLabel = servingLabel,
                cuisine = inferCuisine(product.optJSONArray("countries_tags")),
                protein = protein,
                carbs = carbs,
                fat = fat,
                sourceLabel = "Open Food Facts",
                note = if (usingPerServing) {
                    "按每份营养标签估算"
                } else {
                    "按 100g 或包装信息估算"
                }
            )
        }
    }

    private fun fetchUsdaFoods(
        query: String,
        barcode: String?,
        pageSize: Int
    ): List<FoodLookupItem> {
        val payload = JSONObject()
            .put("query", query)
            .put("pageSize", pageSize)
            .put("dataType", JSONArray().put("Branded"))

        val request = Request.Builder()
            .url("https://api.nal.usda.gov/fdc/v1/foods/search?api_key=DEMO_KEY")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .addHeader("Content-Type", jsonMediaType.toString())
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return emptyList()
            }
            val body = response.body?.string().orEmpty()
            val root = JSONObject(body)
            val foods = root.optJSONArray("foods") ?: JSONArray()
            return buildList {
                for (index in 0 until foods.length()) {
                    val food = foods.optJSONObject(index) ?: continue
                    val upc = food.optString("gtinUpc")
                    if (barcode != null && upc.isNotBlank() && upc != barcode) {
                        continue
                    }
                    val item = parseUsdaFood(food) ?: continue
                    add(item)
                }
            }
        }
    }

    private fun parseUsdaFood(food: JSONObject): FoodLookupItem? {
        val nutrients = food.optJSONArray("foodNutrients") ?: JSONArray()
        val calories = nutrientValue(nutrients, "1008", "Energy")
        if (calories <= 0) {
            return null
        }
        val protein = nutrientValue(nutrients, "1003", "Protein")
        val carbs = nutrientValue(nutrients, "1005", "Carbohydrate, by difference")
        val fat = nutrientValue(nutrients, "1004", "Total lipid (fat)")
        val servingLabel = food.optString("householdServingFullText").ifBlank {
            val servingSize = food.optDouble("servingSize", Double.NaN)
            val unit = food.optString("servingSizeUnit")
            if (servingSize.isNaN()) {
                "1 份"
            } else {
                "${servingSize.roundToInt()} $unit"
            }
        }

        return FoodLookupItem(
            name = food.optString("description", "USDA 食品"),
            brand = food.optString("brandName").ifBlank { food.optString("brandOwner") },
            barcode = food.optString("gtinUpc"),
            calories = calories,
            servingLabel = servingLabel,
            cuisine = Cuisine.AMERICAN,
            protein = protein,
            carbs = carbs,
            fat = fat,
            sourceLabel = "USDA FoodData Central",
            note = food.optString("marketCountry").ifBlank { "美国品牌食品" }
        )
    }

    private fun nutrientValue(nutrients: JSONArray, nutrientNumber: String, nutrientName: String): Int {
        for (index in 0 until nutrients.length()) {
            val nutrient = nutrients.optJSONObject(index) ?: continue
            val numberMatches = nutrient.optString("nutrientNumber") == nutrientNumber
            val nameMatches = nutrient.optString("nutrientName") == nutrientName
            if (numberMatches || nameMatches) {
                return nutrient.optDouble("value", 0.0).roundToInt()
            }
        }
        return 0
    }

    private fun inferCuisine(countries: JSONArray?): Cuisine {
        val tags = buildList {
            if (countries != null) {
                for (index in 0 until countries.length()) {
                    add(countries.optString(index))
                }
            }
        }
        return when {
            tags.any { it.contains("united-states") } -> Cuisine.AMERICAN
            tags.any { it.contains("china") } -> Cuisine.CHINESE
            else -> Cuisine.OTHER
        }
    }

    private fun toLocalLookupItem(reference: FoodReference): FoodLookupItem {
        return FoodLookupItem(
            name = reference.name,
            calories = reference.caloriesPerServing,
            servingLabel = reference.servingLabel,
            cuisine = reference.cuisine,
            protein = reference.protein,
            carbs = reference.carbs,
            fat = reference.fat,
            sourceLabel = "扩展本地库",
            note = "离线常见食物库"
        )
    }

    private fun firstPositiveDouble(vararg values: Double): Double {
        return values.firstOrNull { it.isFinite() && it > 0 } ?: 0.0
    }
}
