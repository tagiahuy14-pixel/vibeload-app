package com.vibeload.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vibeload.app.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity)

    @Query("SELECT * FROM download_history ORDER BY completedAt DESC")
    fun getAllHistoryFlow(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM download_history WHERE isAudio = 0 ORDER BY completedAt DESC")
    fun getVideosFlow(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM download_history WHERE isAudio = 1 ORDER BY completedAt DESC")
    fun getAudiosFlow(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM download_history WHERE title LIKE '%' || :query || '%' OR fileName LIKE '%' || :query || '%' ORDER BY completedAt DESC")
    fun search(query: String): Flow<List<HistoryEntity>>

    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM download_history WHERE mediaUriString = :uriString")
    suspend fun deleteByUri(uriString: String)

    @Query("UPDATE download_history SET fileName = :newName WHERE id = :id")
    suspend fun updateFileName(id: String, newName: String)

    @Query("DELETE FROM download_history")
    suspend fun clearAll()
}
