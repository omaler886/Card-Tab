package com.codex.calorielens.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FoodEntryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CalorieLensDatabase : RoomDatabase() {
    abstract fun foodEntryDao(): FoodEntryDao

    companion object {
        @Volatile
        private var instance: CalorieLensDatabase? = null

        fun getInstance(context: Context): CalorieLensDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CalorieLensDatabase::class.java,
                    "calorie_lens_room.db"
                ).build().also { database ->
                    instance = database
                }
            }
        }
    }
}
