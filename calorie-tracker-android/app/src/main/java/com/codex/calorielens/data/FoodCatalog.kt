package com.codex.calorielens.data

import java.util.Locale
import kotlin.math.max

data class FoodReference(
    val name: String,
    val aliases: List<String>,
    val caloriesPerServing: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val servingLabel: String,
    val cuisine: Cuisine
)

object FoodCatalog {
    val references: List<FoodReference> = listOf(
        FoodReference("鸡胸肉", listOf("鸡胸", "chicken breast"), 165, 31, 0, 4, "100g", Cuisine.CHINESE),
        FoodReference("糙米饭", listOf("糙米", "brown rice"), 180, 4, 38, 1, "1 碗", Cuisine.CHINESE),
        FoodReference("燕麦酸奶杯", listOf("燕麦", "oatmeal", "overnight oats"), 320, 17, 44, 8, "1 杯", Cuisine.OTHER),
        FoodReference("水煮蛋", listOf("鸡蛋", "egg", "boiled egg"), 78, 6, 1, 5, "1 个", Cuisine.OTHER),
        FoodReference("香蕉", listOf("banana"), 105, 1, 27, 0, "1 根", Cuisine.OTHER),
        FoodReference("苹果", listOf("apple"), 95, 0, 25, 0, "1 个", Cuisine.OTHER),
        FoodReference("希腊酸奶", listOf("greek yogurt", "酸奶"), 120, 17, 7, 4, "1 杯", Cuisine.AMERICAN),
        FoodReference("三文鱼藜麦碗", listOf("salmon bowl", "salmon quinoa", "三文鱼"), 530, 36, 42, 24, "1 份", Cuisine.AMERICAN),
        FoodReference("火鸡牛油果三明治", listOf("turkey sandwich", "avocado sandwich"), 510, 29, 41, 22, "1 份", Cuisine.AMERICAN),
        FoodReference("鸡肉沙拉", listOf("chicken salad", "caesar salad"), 420, 34, 18, 24, "1 份", Cuisine.AMERICAN),
        FoodReference("牛肉卷饭碗", listOf("burrito bowl", "beef bowl"), 590, 35, 52, 24, "1 份", Cuisine.AMERICAN),
        FoodReference("牛排", listOf("steak", "sirloin"), 270, 26, 0, 18, "150g", Cuisine.AMERICAN),
        FoodReference("披萨", listOf("pizza"), 285, 12, 36, 10, "1 片", Cuisine.AMERICAN),
        FoodReference("汉堡", listOf("burger"), 520, 28, 42, 27, "1 个", Cuisine.AMERICAN),
        FoodReference("薯条", listOf("fries", "french fries"), 365, 4, 48, 17, "1 份", Cuisine.AMERICAN),
        FoodReference("宫保鸡丁", listOf("kung pao chicken"), 430, 27, 18, 25, "1 份", Cuisine.CHINESE),
        FoodReference("番茄牛腩面", listOf("beef noodle", "牛肉面"), 520, 28, 57, 18, "1 碗", Cuisine.CHINESE),
        FoodReference("虾仁豆腐碗", listOf("虾仁豆腐", "shrimp tofu"), 430, 33, 27, 19, "1 份", Cuisine.CHINESE),
        FoodReference("西兰花", listOf("broccoli"), 55, 4, 11, 1, "1 碗", Cuisine.OTHER),
        FoodReference("米饭", listOf("white rice", "rice"), 232, 4, 51, 0, "1 碗", Cuisine.CHINESE),
        FoodReference("豆腐", listOf("tofu"), 95, 10, 2, 5, "100g", Cuisine.CHINESE),
        FoodReference("虾仁", listOf("shrimp"), 110, 23, 1, 1, "100g", Cuisine.CHINESE),
        FoodReference("蛋白奶昔", listOf("protein shake"), 210, 28, 10, 6, "1 杯", Cuisine.AMERICAN),
        FoodReference("牛油果吐司", listOf("avocado toast"), 340, 10, 34, 18, "1 份", Cuisine.AMERICAN),
        FoodReference("红烧牛肉饭", listOf("braised beef rice"), 620, 24, 68, 27, "1 份", Cuisine.CHINESE),
        FoodReference("麻婆豆腐", listOf("mapo tofu"), 410, 20, 14, 31, "1 份", Cuisine.CHINESE),
        FoodReference("番茄炒蛋", listOf("tomato egg"), 250, 15, 11, 16, "1 盘", Cuisine.CHINESE),
        FoodReference("清蒸鲈鱼", listOf("steamed fish"), 260, 32, 4, 11, "1 份", Cuisine.CHINESE),
        FoodReference("青椒牛肉", listOf("pepper beef"), 390, 25, 17, 24, "1 盘", Cuisine.CHINESE),
        FoodReference("酸辣土豆丝", listOf("shredded potato"), 190, 3, 28, 7, "1 盘", Cuisine.CHINESE),
        FoodReference("扬州炒饭", listOf("yangzhou fried rice", "fried rice"), 610, 18, 78, 24, "1 盘", Cuisine.CHINESE),
        FoodReference("鸡肉卷", listOf("chicken wrap"), 470, 26, 41, 21, "1 个", Cuisine.AMERICAN),
        FoodReference("金枪鱼三明治", listOf("tuna sandwich"), 430, 24, 35, 21, "1 份", Cuisine.AMERICAN),
        FoodReference("贝果奶油芝士", listOf("bagel", "cream cheese bagel"), 390, 11, 57, 13, "1 份", Cuisine.AMERICAN),
        FoodReference("蓝莓松饼", listOf("blueberry muffin", "muffin"), 410, 6, 57, 17, "1 个", Cuisine.AMERICAN),
        FoodReference("煎饼", listOf("pancake"), 180, 5, 27, 6, "2 片", Cuisine.AMERICAN),
        FoodReference("华夫饼", listOf("waffle"), 220, 5, 28, 9, "2 片", Cuisine.AMERICAN),
        FoodReference("牛肉塔可", listOf("beef taco", "taco"), 210, 11, 16, 11, "1 个", Cuisine.AMERICAN),
        FoodReference("芝士通心粉", listOf("mac and cheese"), 430, 14, 49, 19, "1 碗", Cuisine.AMERICAN),
        FoodReference("燕麦粥", listOf("porridge", "oatmeal bowl"), 250, 8, 41, 6, "1 碗", Cuisine.AMERICAN),
        FoodReference("低脂拿铁", listOf("latte"), 160, 10, 15, 5, "1 杯", Cuisine.AMERICAN),
        FoodReference("水果奶昔", listOf("smoothie"), 290, 8, 49, 7, "1 杯", Cuisine.AMERICAN),
        FoodReference("烤鸡胸沙拉", listOf("grilled chicken salad"), 360, 33, 14, 18, "1 份", Cuisine.AMERICAN),
        FoodReference("酸奶水果杯", listOf("yogurt parfait"), 280, 15, 34, 8, "1 杯", Cuisine.AMERICAN),
        FoodReference("藜麦鸡肉碗", listOf("quinoa chicken bowl"), 520, 34, 47, 19, "1 份", Cuisine.AMERICAN),
        FoodReference("豆浆", listOf("soy milk"), 105, 7, 8, 4, "1 杯", Cuisine.CHINESE),
        FoodReference("白粥", listOf("congee", "rice porridge"), 120, 2, 26, 0, "1 碗", Cuisine.CHINESE),
        FoodReference("小笼包", listOf("xiaolongbao", "soup dumpling"), 280, 11, 30, 12, "6 个", Cuisine.CHINESE),
        FoodReference("煎饺", listOf("potsticker", "dumpling"), 320, 12, 31, 16, "8 个", Cuisine.CHINESE),
        FoodReference("凉皮", listOf("cold noodles", "liangpi"), 330, 7, 53, 10, "1 份", Cuisine.CHINESE),
        FoodReference("手抓饼", listOf("scallion pancake", "pancake roll"), 410, 8, 48, 20, "1 张", Cuisine.CHINESE),
        FoodReference("馒头", listOf("steamed bun"), 223, 7, 46, 1, "2 个", Cuisine.CHINESE),
        FoodReference("烤红薯", listOf("sweet potato"), 180, 3, 41, 0, "1 个", Cuisine.CHINESE),
        FoodReference("卤牛肉", listOf("braised beef slices"), 250, 29, 5, 12, "120g", Cuisine.CHINESE),
        FoodReference("沙县蒸饺", listOf("steamed dumplings"), 300, 13, 34, 11, "10 个", Cuisine.CHINESE),
        FoodReference("鸡腿饭", listOf("chicken rice bowl"), 680, 27, 78, 28, "1 份", Cuisine.CHINESE),
        FoodReference("炒乌冬", listOf("udon noodles"), 540, 14, 71, 20, "1 盘", Cuisine.CHINESE),
        FoodReference("蛋炒饭", listOf("egg fried rice"), 560, 15, 71, 22, "1 盘", Cuisine.CHINESE),
        FoodReference("奶茶", listOf("milk tea", "bubble tea"), 360, 4, 58, 12, "1 杯", Cuisine.OTHER),
        FoodReference("protein bar", listOf("protein bar", "能量棒"), 210, 20, 24, 6, "1 条", Cuisine.AMERICAN),
        FoodReference("granola bar", listOf("谷物棒"), 160, 3, 25, 6, "1 条", Cuisine.AMERICAN)
    )

    fun findBestMatch(query: String): FoodReference? {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank()) {
            return null
        }
        return references
            .map { reference -> reference to score(reference, normalizedQuery) }
            .filter { (_, score) -> score > 0 }
            .maxByOrNull { (_, score) -> score }
            ?.first
    }

    fun search(query: String, region: FoodRegion): List<FoodReference> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank()) {
            return referencesForRegion(region).take(12)
        }
        return referencesForRegion(region)
            .map { reference -> reference to score(reference, normalizedQuery) }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { it.first }
            .take(12)
    }

    fun extractMatches(text: String): List<FoodReference> {
        val normalizedText = normalize(text)
        if (normalizedText.isBlank()) {
            return emptyList()
        }
        return references.filter { reference ->
            (listOf(reference.name) + reference.aliases).any { alias ->
                val normalizedAlias = normalize(alias)
                normalizedAlias.isNotBlank() && normalizedText.contains(normalizedAlias)
            }
        }
    }

    private fun referencesForRegion(region: FoodRegion): List<FoodReference> {
        return when (region) {
            FoodRegion.CHINA -> references.filter { it.cuisine != Cuisine.AMERICAN }
            FoodRegion.USA -> references.filter { it.cuisine != Cuisine.CHINESE }
            FoodRegion.GLOBAL -> references
        }
    }

    private fun score(reference: FoodReference, normalizedQuery: String): Int {
        var best = 0
        for (alias in listOf(reference.name) + reference.aliases) {
            val normalizedAlias = normalize(alias)
            if (normalizedAlias.isBlank()) {
                continue
            }
            if (normalizedQuery == normalizedAlias) {
                return 1000
            }
            if (normalizedQuery.contains(normalizedAlias) || normalizedAlias.contains(normalizedQuery)) {
                best = max(best, 200 + normalizedAlias.length)
            }
            if (normalizedQuery.split(" ").any { it == normalizedAlias }) {
                best = max(best, 120)
            }
        }
        return best
    }

    private fun normalize(text: String): String {
        return text
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\u4e00-\\u9fa5]+"), "")
    }
}
