package com.codex.calorielens.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_entries")
data class FoodEntryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val calories: Int,
    val servings: Double,
    val cuisine: String,
    val source: String,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val note: String,
    val createdAt: Long
)

fun FoodEntryEntity.toModel(): FoodEntry {
    return FoodEntry(
        id = id,
        name = name,
        calories = calories,
        servings = servings,
        cuisine = Cuisine.fromRaw(cuisine),
        source = EntrySource.entries.firstOrNull { it.name.equals(source, ignoreCase = true) }
            ?: EntrySource.MANUAL,
        protein = protein,
        carbs = carbs,
        fat = fat,
        note = note,
        createdAt = createdAt
    )
}

fun FoodEntry.toEntity(): FoodEntryEntity {
    return FoodEntryEntity(
        id = id,
        name = name,
        calories = calories,
        servings = servings,
        cuisine = cuisine.name,
        source = source.name,
        protein = protein,
        carbs = carbs,
        fat = fat,
        note = note,
        createdAt = createdAt
    )
}
