package com.vibeload.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class HistoryEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val originalUrl: String,
    val mediaUriString: String,
    val relativePath: String,
    val fileName: String,
    val mimeType: String,
    val isAudio: Boolean,
    val fileSize: Long,
    val durationSeconds: Long,
    val thumbnailUri: String,
    val completedAt: Long = System.currentTimeMillis()
)
