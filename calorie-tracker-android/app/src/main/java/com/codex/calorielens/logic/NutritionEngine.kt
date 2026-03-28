package com.codex.calorielens.logic

import com.codex.calorielens.data.Cuisine
import com.codex.calorielens.data.EntrySource
import com.codex.calorielens.data.BreakdownPoint
import com.codex.calorielens.data.FoodCatalog
import com.codex.calorielens.data.FoodEntry
import com.codex.calorielens.data.FoodLookupItem
import com.codex.calorielens.data.HistoryPoint
import com.codex.calorielens.data.RecognizedFood
import com.codex.calorielens.data.Recipe
import com.codex.calorielens.data.Sex
import com.codex.calorielens.data.UserProfile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object NutritionEngine {
    fun calculateTargetCalories(profile: UserProfile): Int {
        val weight = profile.weightKg
        val height = profile.heightCm.toDouble()
        val age = profile.age.toDouble()
        val bmr = when (profile.sex) {
            Sex.MALE -> 10 * weight + 6.25 * height - 5 * age + 5
            Sex.FEMALE -> 10 * weight + 6.25 * height - 5 * age - 161
        }
        return ((bmr * profile.activityLevel.factor) + profile.goalType.calorieDelta)
            .roundToInt()
            .coerceAtLeast(1200)
    }

    fun todayEntries(entries: List<FoodEntry>): List<FoodEntry> {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        return entries
            .filter { entry ->
                Instant.ofEpochMilli(entry.createdAt)
                    .atZone(zoneId)
                    .toLocalDate() == today
            }
            .sortedByDescending { it.createdAt }
    }

    fun createManualEntry(
        name: String,
        servings: Double,
        caloriesInput: Int?,
        cuisine: Cuisine
    ): FoodEntry {
        val reference = FoodCatalog.findBestMatch(name)
        val safeServings = servings.coerceAtLeast(0.1)
        val totalCalories = caloriesInput ?: ((reference?.caloriesPerServing ?: 180) * safeServings).roundToInt()
        return FoodEntry(
            name = if (name.isBlank()) reference?.name ?: "未命名食物" else name,
            calories = totalCalories,
            servings = safeServings,
            cuisine = if (cuisine != Cuisine.OTHER) cuisine else reference?.cuisine ?: Cuisine.OTHER,
            source = EntrySource.DATABASE,
            protein = ((reference?.protein ?: 0) * safeServings).roundToInt(),
            carbs = ((reference?.carbs ?: 0) * safeServings).roundToInt(),
            fat = ((reference?.fat ?: 0) * safeServings).roundToInt(),
            note = when {
                caloriesInput != null -> "手动输入热量"
                reference != null -> "根据常见食物库估算"
                else -> "使用默认估值"
            }
        )
    }

    fun createRecipeEntry(recipe: Recipe): FoodEntry {
        return FoodEntry(
            name = recipe.title,
            calories = recipe.calories,
            servings = 1.0,
            cuisine = recipe.cuisine,
            source = EntrySource.RECIPE,
            protein = recipe.protein,
            carbs = recipe.carbs,
            fat = recipe.fat,
            note = "${recipe.mealLabel}食谱"
        )
    }

    fun createPhotoEntry(food: RecognizedFood): FoodEntry {
        return FoodEntry(
            name = food.name,
            calories = food.calories,
            servings = food.servings,
            cuisine = food.cuisine,
            source = EntrySource.PHOTO,
            protein = food.protein,
            carbs = food.carbs,
            fat = food.fat,
            note = food.note
        )
    }

    fun createLookupEntry(item: FoodLookupItem): FoodEntry {
        return FoodEntry(
            name = item.name,
            calories = item.calories,
            servings = item.servings,
            cuisine = item.cuisine,
            source = EntrySource.MANUAL,
            protein = item.protein,
            carbs = item.carbs,
            fat = item.fat,
            note = buildString {
                append(item.sourceLabel)
                if (item.brand.isNotBlank()) {
                    append(" · ")
                    append(item.brand)
                }
                if (item.servingLabel.isNotBlank()) {
                    append(" · ")
                    append(item.servingLabel)
                }
                if (item.note.isNotBlank()) {
                    append(" · ")
                    append(item.note)
                }
            }
        )
    }

    fun inferRecognizedFoods(ocrText: String): List<RecognizedFood> {
        return FoodCatalog.extractMatches(ocrText)
            .take(5)
            .map { match ->
                RecognizedFood(
                    name = match.name,
                    calories = match.caloriesPerServing,
                    servings = 1.0,
                    cuisine = match.cuisine,
                    protein = match.protein,
                    carbs = match.carbs,
                    fat = match.fat,
                    note = "根据 OCR 文本匹配 ${match.servingLabel}",
                    confidence = 0.55
                )
            }
    }

    fun statusText(consumed: Int, target: Int): String {
        val (lower, upper) = targetRange(target)
        return when {
            consumed in lower..upper -> "今天已达标，保持这个节奏。"
            consumed < lower -> "距离目标还差 ${lower - consumed} kcal。"
            else -> "已超出建议上限 ${consumed - upper} kcal。"
        }
    }

    fun recipeImpactText(recipe: Recipe, consumed: Int, target: Int): String {
        val projected = consumed + recipe.calories
        val (lower, upper) = targetRange(target)
        return when {
            projected in lower..upper -> "加入后预计正好落在目标区间。"
            projected < lower -> "加入后仍可再补 ${lower - projected} kcal。"
            else -> "加入后会超出 ${projected - upper} kcal。"
        }
    }

    fun sortedRecipes(recipes: List<Recipe>, consumed: Int, target: Int): List<Recipe> {
        val remaining = target - consumed
        return recipes.sortedBy { recipe ->
            abs(remaining - recipe.calories)
        }
    }

    fun historyPoints(entries: List<FoodEntry>, days: Int = 7): List<HistoryPoint> {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val totalsByDay = entries.groupBy { entry ->
            Instant.ofEpochMilli(entry.createdAt).atZone(zoneId).toLocalDate()
        }.mapValues { (_, value) ->
            value.sumOf { it.calories }
        }

        return (days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            HistoryPoint(
                label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA),
                calories = totalsByDay[date] ?: 0,
                isToday = date == today
            )
        }
    }

    fun cuisineBreakdown(entries: List<FoodEntry>, days: Int = 30): List<BreakdownPoint> {
        val zoneId = ZoneId.systemDefault()
        val startDate = LocalDate.now(zoneId).minusDays((days - 1).toLong())
        val filtered = entries.filter { entry ->
            Instant.ofEpochMilli(entry.createdAt).atZone(zoneId).toLocalDate() >= startDate
        }
        val totals = filtered.groupBy { it.cuisine }
            .mapValues { (_, value) -> value.sumOf { it.calories } }
        return listOf(
            BreakdownPoint("中式", totals[Cuisine.CHINESE] ?: 0, 0xFFCB5C35),
            BreakdownPoint("美式", totals[Cuisine.AMERICAN] ?: 0, 0xFF4F7B53),
            BreakdownPoint("其他", totals[Cuisine.OTHER] ?: 0, 0xFFD7972B)
        ).filter { it.value > 0 }
    }

    fun averageDailyCalories(entries: List<FoodEntry>, days: Int = 7): Int {
        val points = historyPoints(entries, days)
        return if (points.isEmpty()) 0 else points.sumOf { it.calories } / points.size
    }

    private fun targetRange(target: Int): Pair<Int, Int> {
        val lower = (target * 0.9).roundToInt()
        val upper = (target * 1.1).roundToInt()
        return lower to upper
    }
}
