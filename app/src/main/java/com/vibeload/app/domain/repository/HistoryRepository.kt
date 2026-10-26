package com.vibeload.app.domain.repository

import com.vibeload.app.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getAllHistory(): Flow<List<HistoryEntity>>
    fun getVideos(): Flow<List<HistoryEntity>>
    fun getAudios(): Flow<List<HistoryEntity>>
    fun search(query: String): Flow<List<HistoryEntity>>
    suspend fun addRecord(entity: HistoryEntity)
    suspend fun deleteRecord(id: String)
    suspend fun updateFileName(id: String, newName: String)
    suspend fun clearHistory()
}
