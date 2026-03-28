package com.codex.calorielens.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_entries ORDER BY createdAt DESC")
    suspend fun getAll(): List<FoodEntryEntity>

    @Query("SELECT COUNT(*) FROM food_entries")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: FoodEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<FoodEntryEntity>)

    @Query("DELETE FROM food_entries WHERE id = :entryId")
    suspend fun deleteById(entryId: String)

    @Query("DELETE FROM food_entries")
    suspend fun deleteAll()
}
