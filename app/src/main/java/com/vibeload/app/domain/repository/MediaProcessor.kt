package com.vibeload.app.domain.repository

interface MediaProcessor {
    suspend fun mergeVideoAndAudio(
        videoPath: String,
        audioPath: String,
        outputPath: String,
        onProgress: (Float) -> Unit
    ): Result<String>

    suspend fun transcodeToMp3(
        inputPath: String,
        outputPath: String,
        bitrateKbps: Int,
        title: String? = null,
        artist: String? = null,
        onProgress: (Float) -> Unit
    ): Result<String>

    fun cancelCurrentOperation()
}
