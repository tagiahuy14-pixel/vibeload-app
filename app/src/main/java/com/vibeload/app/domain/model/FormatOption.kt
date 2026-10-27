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
        if (filesize <= 0L) return "Không rõ dung lượng"
        val mb = filesize / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format("%.2f GB", mb / 1024.0)
        } else {
            String.format("%.1f MB", mb)
        }
    }
}
