package com.codex.calorielens.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EntryRepository(
    private val dao: FoodEntryDao,
    private val storage: AppStorage
) {
    val entriesFlow: Flow<List<FoodEntry>> = dao.observeAll().map { entities ->
        entities.map(FoodEntryEntity::toModel)
    }

    suspend fun initialize() {
        if (storage.isLegacyEntriesMigrated()) {
            return
        }
        val roomCount = dao.count()
        if (roomCount == 0) {
            val legacyEntries = storage.readLegacyEntries()
            if (legacyEntries.isNotEmpty()) {
                dao.insertAll(legacyEntries.map(FoodEntry::toEntity))
            }
        }
        storage.markLegacyEntriesMigrated()
    }

    suspend fun appendEntry(entry: FoodEntry) {
        dao.insert(entry.toEntity())
    }

    suspend fun appendEntries(entries: List<FoodEntry>) {
        dao.insertAll(entries.map(FoodEntry::toEntity))
    }

    suspend fun removeEntry(entryId: String) {
        dao.deleteById(entryId)
    }

    suspend fun readAllEntries(): List<FoodEntry> {
        return dao.getAll().map(FoodEntryEntity::toModel)
    }

    suspend fun replaceAllEntries(entries: List<FoodEntry>) {
        dao.deleteAll()
        if (entries.isNotEmpty()) {
            dao.insertAll(entries.map(FoodEntry::toEntity))
        }
    }
}
