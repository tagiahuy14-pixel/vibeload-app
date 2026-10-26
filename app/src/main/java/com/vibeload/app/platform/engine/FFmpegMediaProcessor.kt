package com.vibeload.app.platform.engine

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.vibeload.app.domain.repository.MediaProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class FFmpegMediaProcessor : MediaProcessor {

    @Volatile
    private var currentSessionId: Long? = null

    override suspend fun mergeVideoAndAudio(
        videoPath: String,
        audioPath: String,
        outputPath: String,
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val cmd = "-y -i \"$videoPath\" -i \"$audioPath\" -c:v copy -c:a aac -strict experimental \"$outputPath\""

            val session = FFmpegKit.executeAsync(
                cmd,
                { completedSession ->
                    currentSessionId = null
                    val returnCode = completedSession.returnCode
                    if (ReturnCode.isSuccess(returnCode)) {
                        continuation.resume(Result.success(outputPath))
                    } else if (ReturnCode.isCancel(returnCode)) {
                        continuation.resume(Result.failure(Exception("OPERATION_CANCELLED")))
                    } else {
                        val failStackTrace = completedSession.failStackTrace ?: "Lỗi FFmpeg không xác định"
                        continuation.resume(Result.failure(Exception("Lỗi ghép luồng: $failStackTrace")))
                    }
                },
                { log ->
                    // Optional logging
                },
                { statistics ->
                    if (statistics.time > 0) {
                        onProgress(statistics.time.toFloat())
                    }
                }
            )

            currentSessionId = session.sessionId

            continuation.invokeOnCancellation {
                session.cancel()
                currentSessionId = null
            }
        }
    }

    override suspend fun transcodeToMp3(
        inputPath: String,
        outputPath: String,
        bitrateKbps: Int,
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val safeBitrate = when (bitrateKbps) {
                128, 192, 256, 320 -> bitrateKbps
                else -> 192
            }
            val cmd = "-y -i \"$inputPath\" -vn -acodec libmp3lame -b:a ${safeBitrate}k \"$outputPath\""

            val session = FFmpegKit.executeAsync(
                cmd,
                { completedSession ->
                    currentSessionId = null
                    val returnCode = completedSession.returnCode
                    if (ReturnCode.isSuccess(returnCode)) {
                        continuation.resume(Result.success(outputPath))
                    } else if (ReturnCode.isCancel(returnCode)) {
                        continuation.resume(Result.failure(Exception("OPERATION_CANCELLED")))
                    } else {
                        val failStackTrace = completedSession.failStackTrace ?: "Lỗi FFmpeg không xác định"
                        continuation.resume(Result.failure(Exception("Lỗi chuyển đổi MP3: $failStackTrace")))
                    }
                },
                { log -> },
                { statistics ->
                    if (statistics.time > 0) {
                        onProgress(statistics.time.toFloat())
                    }
                }
            )

            currentSessionId = session.sessionId

            continuation.invokeOnCancellation {
                session.cancel()
                currentSessionId = null
            }
        }
    }

    override fun cancelCurrentOperation() {
        currentSessionId?.let {
            FFmpegKit.cancel(it)
            currentSessionId = null
        }
    }
}
