package com.vibeload.app.domain.model

data class FormatOption(
    val formatId: String,
    val label: String,
    val resolution: String = "",
    val height: Int = 0,
    val width: Int = 0,
    val ext: String = "mp4",
    val filesize: Long = 0L,
    val isVideo: Boolean = true,
    val needsMerge: Boolean = false,
    val bitrateKbps: Int = 0,
    val isOriginalAudio: Boolean = false,
    val isImage: Boolean = false,
    val mimeType: String = ""
) {
    fun getFormattedSize(): String {
        if (filesize <= 0L) return ""
        val mb = filesize / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format("%.2f GB", mb / 1024.0)
        } else {
            String.format("%.1f MB", mb)
        }
    }

    fun getEstimatedSize(durationSeconds: Long): String {
        if (filesize > 0L) return getFormattedSize()
        if (durationSeconds <= 0L) return ""
        val estKbps = when {
            !isVideo && !isImage -> (if (bitrateKbps > 0) bitrateKbps else 128)
            height >= 2160 -> 12000 // 4K ~ 12 Mbps
            height >= 1440 -> 6000  // 2K ~ 6 Mbps
            height >= 1080 -> 2800  // 1080p ~ 2.8 Mbps
            height >= 720 -> 1400   // 720p ~ 1.4 Mbps
            height >= 480 -> 750    // 480p ~ 750 kbps
            else -> 500
        }
        val estBytes = (estKbps * 1000L / 8) * durationSeconds
        val mb = estBytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format("~%.1f GB", mb / 1024.0)
        } else {
            String.format("~%.1f MB", mb)
        }
    }
}
