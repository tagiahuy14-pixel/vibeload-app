package com.vibeload.app.data.repository

import com.vibeload.app.data.local.AppDatabase
import com.vibeload.app.data.local.entity.HistoryEntity
import com.vibeload.app.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow

class HistoryRepositoryImpl(
    private val database: AppDatabase
) : HistoryRepository {

    override fun getAllHistory(): Flow<List<HistoryEntity>> =
        database.historyDao().getAllHistoryFlow()

    override fun getVideos(): Flow<List<HistoryEntity>> =
        database.historyDao().getVideosFlow()

    override fun getAudios(): Flow<List<HistoryEntity>> =
        database.historyDao().getAudiosFlow()

    override fun search(query: String): Flow<List<HistoryEntity>> =
        database.historyDao().search(query)

    override suspend fun addRecord(entity: HistoryEntity) =
        database.historyDao().insert(entity)

    override suspend fun deleteRecord(id: String) =
        database.historyDao().deleteById(id)

    override suspend fun updateFileName(id: String, newName: String) =
        database.historyDao().updateFileName(id, newName)

    override suspend fun clearHistory() =
        database.historyDao().clearAll()
}
