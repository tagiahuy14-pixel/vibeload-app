package com.vibeload.app.domain.repository

import com.vibeload.app.domain.model.MediaMetadata

data class EngineInfo(
    val ytDlpVersion: String,
    val pythonVersion: String,
    val platform: String
)

interface DownloadEngine {
    fun getEngineInfo(): EngineInfo
    suspend fun analyzeUrl(url: String): Result<MediaMetadata>
    suspend fun downloadStream(
        url: String,
        formatSpec: String,
        tempOutputPath: String,
        listener: DownloadProgressListener
    ): Result<String>
}
