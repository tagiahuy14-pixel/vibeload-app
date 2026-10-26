package com.vibeload.app.domain.model

enum class DownloadStatus {
    IDLE,
    RESOLVING,
    READY,
    DOWNLOADING,
    PROCESSING,
    SAVING,
    COMPLETED,
    PAUSED,
    RETRYING,
    FAILED,
    CANCELLED;

    fun isTerminal(): Boolean = this in listOf(COMPLETED, FAILED, CANCELLED)
    fun isActive(): Boolean = this in listOf(RESOLVING, DOWNLOADING, PROCESSING, SAVING, RETRYING)
}
