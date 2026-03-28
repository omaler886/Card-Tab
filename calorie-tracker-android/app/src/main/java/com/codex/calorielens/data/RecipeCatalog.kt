package com.codex.calorielens.data

object RecipeCatalog {
    val recipes: List<Recipe> = listOf(
        Recipe(
            id = "cn_chicken_rice",
            title = "鸡胸西兰花糙米饭",
            mealLabel = "午餐",
            cuisine = Cuisine.CHINESE,
            calories = 460,
            protein = 40,
            carbs = 42,
            fat = 12,
            highlight = "高蛋白、适合减脂工作餐",
            ingredients = listOf("鸡胸肉", "糙米饭", "西兰花", "蒜香橄榄油")
        ),
        Recipe(
            id = "cn_tomato_beef_noodle",
            title = "番茄牛腩全麦面",
            mealLabel = "午餐",
            cuisine = Cuisine.CHINESE,
            calories = 540,
            protein = 31,
            carbs = 56,
            fat = 18,
            highlight = "有饱腹感，适合中高活动量",
            ingredients = listOf("牛腩", "番茄", "全麦面", "胡萝卜")
        ),
        Recipe(
            id = "cn_shrimp_tofu",
            title = "虾仁豆腐藜麦碗",
            mealLabel = "晚餐",
            cuisine = Cuisine.CHINESE,
            calories = 430,
            protein = 35,
            carbs = 29,
            fat = 16,
            highlight = "低脂高蛋白，晚餐友好",
            ingredients = listOf("虾仁", "嫩豆腐", "藜麦", "菠菜")
        ),
        Recipe(
            id = "cn_salmon_soba",
            title = "三文鱼荞麦冷面",
            mealLabel = "晚餐",
            cuisine = Cuisine.CHINESE,
            calories = 495,
            protein = 33,
            carbs = 41,
            fat = 20,
            highlight = "碳水和脂肪更平衡",
            ingredients = listOf("三文鱼", "荞麦面", "黄瓜", "芝麻酱")
        ),
        Recipe(
            id = "cn_kungpao_lite",
            title = "低油宫保鸡丁饭",
            mealLabel = "午餐",
            cuisine = Cuisine.CHINESE,
            calories = 560,
            protein = 34,
            carbs = 47,
            fat = 22,
            highlight = "保留风味但控制油量",
            ingredients = listOf("鸡腿肉", "花生", "彩椒", "米饭")
        ),
        Recipe(
            id = "us_greek_oats",
            title = "Greek Yogurt Berry Oats",
            mealLabel = "早餐",
            cuisine = Cuisine.AMERICAN,
            calories = 360,
            protein = 24,
            carbs = 42,
            fat = 9,
            highlight = "早餐型配方，控卡轻松",
            ingredients = listOf("Greek yogurt", "oats", "blueberries", "chia seeds")
        ),
        Recipe(
            id = "us_caesar_lite",
            title = "Grilled Chicken Caesar Lite",
            mealLabel = "午餐",
            cuisine = Cuisine.AMERICAN,
            calories = 420,
            protein = 36,
            carbs = 18,
            fat = 22,
            highlight = "低碳午餐，适合减脂期",
            ingredients = listOf("grilled chicken", "romaine", "parmesan", "light caesar dressing")
        ),
        Recipe(
            id = "us_turkey_sandwich",
            title = "Turkey Avocado Sandwich",
            mealLabel = "午餐",
            cuisine = Cuisine.AMERICAN,
            calories = 510,
            protein = 31,
            carbs = 39,
            fat = 23,
            highlight = "办公室友好，准备快",
            ingredients = listOf("turkey breast", "avocado", "whole grain bread", "tomato")
        ),
        Recipe(
            id = "us_salmon_bowl",
            title = "Salmon Quinoa Bowl",
            mealLabel = "晚餐",
            cuisine = Cuisine.AMERICAN,
            calories = 530,
            protein = 37,
            carbs = 43,
            fat = 23,
            highlight = "Omega-3 足，适合维持和增肌",
            ingredients = listOf("salmon", "quinoa", "spinach", "corn salsa")
        ),
        Recipe(
            id = "us_burrito_bowl",
            title = "Lean Beef Burrito Bowl",
            mealLabel = "晚餐",
            cuisine = Cuisine.AMERICAN,
            calories = 590,
            protein = 38,
            carbs = 52,
            fat = 24,
            highlight = "训练后补能量更合适",
            ingredients = listOf("lean beef", "rice", "black beans", "salsa", "avocado")
        )
    )
}
