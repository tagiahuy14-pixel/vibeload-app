package com.vibeload.app.domain.model

data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val outputFilename: String,
    val selectedFormat: FormatOption,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val progress: Float = 0f,
    val speedBytesPerSec: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val etaSeconds: Long = 0L,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val currentItemIndex: Int = 0,
    val totalItemCount: Int = 0,
    val statusMessage: String? = null,
    val selectedImageUrls: List<String> = emptyList()
) {
    fun getFormattedSpeed(): String {
        if (speedBytesPerSec <= 0f) return "0 KB/s"
        val kbps = speedBytesPerSec / 1024.0
        return if (kbps >= 1024.0) {
            String.format("%.1f MB/s", kbps / 1024.0)
        } else {
            String.format("%.0f KB/s", kbps)
        }
    }

    fun getFormattedSizeProgress(): String {
        if (totalItemCount > 1) {
            return if (currentItemIndex > 0) "$currentItemIndex/$totalItemCount ảnh" else "$totalItemCount ảnh"
        }
        val dlMb = downloadedBytes / (1024.0 * 1024.0)
        return if (totalBytes > 0) {
            val totalMb = totalBytes / (1024.0 * 1024.0)
            String.format("%.1f MB / %.1f MB", dlMb, totalMb)
        } else {
            String.format("%.1f MB", dlMb)
        }
    }

    fun getFormattedEta(): String {
        if (etaSeconds <= 0L) return "--"
        val m = etaSeconds / 60
        val s = etaSeconds % 60
        return if (m > 0) "${m}p ${s}s" else "${s}s"
    }
}
