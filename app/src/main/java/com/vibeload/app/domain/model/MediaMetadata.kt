package com.vibeload.app.domain.model

data class MediaMetadata(
    val url: String,
    val title: String,
    val thumbnail: String,
    val uploader: String,
    val durationSeconds: Long,
    val platform: String,
    val videoOptions: List<FormatOption>,
    val audioOptions: List<FormatOption>
) {
    fun getFormattedDuration(): String {
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
}
