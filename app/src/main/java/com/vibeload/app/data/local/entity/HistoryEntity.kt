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

val HistoryEntity.isImage: Boolean
    get() = mimeType.startsWith("image/", ignoreCase = true) ||
            relativePath.contains("Pictures", ignoreCase = true) ||
            fileName.endsWith(".jpg", ignoreCase = true) ||
            fileName.endsWith(".jpeg", ignoreCase = true) ||
            fileName.endsWith(".png", ignoreCase = true) ||
            fileName.endsWith(".webp", ignoreCase = true) ||
            fileName.endsWith(".gif", ignoreCase = true) ||
            fileName.endsWith(".avif", ignoreCase = true) ||
            fileName.endsWith(".bmp", ignoreCase = true) ||
            fileName.endsWith(".tiff", ignoreCase = true) ||
            fileName.endsWith(".tif", ignoreCase = true) ||
            fileName.endsWith(".heic", ignoreCase = true) ||
            fileName.endsWith(".heif", ignoreCase = true) ||
            fileName.endsWith(".svg", ignoreCase = true) ||
            fileName.endsWith(".ico", ignoreCase = true)

val HistoryEntity.isVideo: Boolean
    get() = !isAudio && !isImage

fun HistoryEntity.getFormattedDuration(): String {
    if (durationSeconds <= 0) return "--:--"
    val hours = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60
    val seconds = durationSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
