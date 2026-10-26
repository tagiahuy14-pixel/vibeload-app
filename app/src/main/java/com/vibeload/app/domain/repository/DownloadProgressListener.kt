package com.vibeload.app.domain.repository

interface DownloadProgressListener {
    fun onProgress(
        percent: Float,
        speed: Float,
        downloadedBytes: Long,
        totalBytes: Long,
        etaSeconds: Long
    )
    fun isCancelled(): Boolean
}
