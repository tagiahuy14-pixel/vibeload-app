package com.vibeload.app.platform.coordinator

import android.content.Context
import android.net.Uri
import com.vibeload.app.R
import com.vibeload.app.data.local.entity.HistoryEntity
import com.vibeload.app.domain.model.DownloadStatus
import com.vibeload.app.domain.model.DownloadTask
import com.vibeload.app.domain.model.FormatOption
import com.vibeload.app.domain.model.MediaMetadata
import com.vibeload.app.domain.repository.DownloadEngine
import com.vibeload.app.domain.repository.DownloadProgressListener
import com.vibeload.app.domain.repository.HistoryRepository
import com.vibeload.app.domain.repository.MediaProcessor
import com.vibeload.app.domain.repository.MediaStorageRepository
import com.vibeload.app.platform.service.DownloadForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class DownloadCoordinator(
    private val context: Context,
    private val downloadEngine: DownloadEngine,
    private val mediaProcessor: MediaProcessor,
    private val storageRepository: MediaStorageRepository,
    private val historyRepository: HistoryRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _currentTask = MutableStateFlow<DownloadTask?>(null)
    val currentTask: StateFlow<DownloadTask?> = _currentTask.asStateFlow()

    @Volatile
    private var isCancelled = false

    @Volatile
    private var isPaused = false

    private var activeJob: Job? = null

    fun startDownload(
        url: String,
        metadata: MediaMetadata,
        selectedFormat: FormatOption,
        customFilename: String
    ): Result<Unit> {
        val active = _currentTask.value
        if (active != null && active.status.isActive()) {
            return Result.failure(Exception(context.getString(R.string.error_another_task_running)))
        }

        // Validate storage space (FR-21)
        val neededBytes = if (selectedFormat.filesize > 0) selectedFormat.filesize * 2 else 50 * 1024 * 1024L
        if (storageRepository.getAvailableStorageBytes() < neededBytes) {
            return Result.failure(Exception(context.getString(R.string.error_insufficient_storage)))
        }

        val taskId = UUID.randomUUID().toString()
        val extension = when {
            selectedFormat.isVideo -> "mp4"
            selectedFormat.isOriginalAudio -> selectedFormat.ext
            else -> "mp3"
        }
        val safeName = storageRepository.sanitizeFilename(customFilename, extension)

        val task = DownloadTask(
            id = taskId,
            url = url,
            title = metadata.title,
            outputFilename = safeName,
            selectedFormat = selectedFormat,
            status = DownloadStatus.RESOLVING,
            totalBytes = selectedFormat.filesize
        )

        _currentTask.value = task
        isCancelled = false
        isPaused = false

        // Start Foreground Service
        DownloadForegroundService.start(context, task.title)

        activeJob = scope.launch {
            executeDownloadPipeline(task, metadata)
        }

        return Result.success(Unit)
    }

    private suspend fun executeDownloadPipeline(task: DownloadTask, metadata: MediaMetadata) {
        val tempDir = File(context.cacheDir, "vibeload_temp").apply { mkdirs() }
        val rawTempFile = File(tempDir, "temp_dl_${task.id}.${task.selectedFormat.ext}")
        val processedFile = File(tempDir, "temp_proc_${task.id}.${if (task.selectedFormat.isVideo) "mp4" else "mp3"}")

        var attempt = 0
        val maxRetries = 3

        while (attempt <= maxRetries && !isCancelled) {
            try {
                if (isPaused) {
                    _currentTask.value = _currentTask.value?.copy(status = DownloadStatus.PAUSED)
                    DownloadForegroundService.updateProgress(context, _currentTask.value?.title ?: "", 0, 0f, true)
                    while (isPaused && !isCancelled) {
                        delay(500)
                    }
                    if (isCancelled) break
                }

                _currentTask.value = _currentTask.value?.copy(
                    status = DownloadStatus.DOWNLOADING,
                    retryCount = attempt
                )

                val progressListener = object : DownloadProgressListener {
                    override fun onProgress(
                        percent: Float,
                        speed: Float,
                        downloadedBytes: Long,
                        totalBytes: Long,
                        etaSeconds: Long
                    ) {
                        _currentTask.value = _currentTask.value?.copy(
                            progress = percent,
                            speedBytesPerSec = speed,
                            downloadedBytes = downloadedBytes,
                            totalBytes = if (totalBytes > 0) totalBytes else task.totalBytes,
                            etaSeconds = etaSeconds
                        )
                        DownloadForegroundService.updateProgress(
                            context,
                            task.title,
                            percent.toInt(),
                            speed,
                            false
                        )
                    }

                    override fun isCancelled(): Boolean = isCancelled || isPaused
                }

                val formatSpec = if (task.selectedFormat.isVideo) {
                    if (task.selectedFormat.formatId.isNotEmpty()) task.selectedFormat.formatId else "bestvideo+bestaudio/best"
                } else {
                    "bestaudio/best"
                }

                val dlResult = downloadEngine.downloadStream(
                    task.url,
                    formatSpec,
                    rawTempFile.absolutePath,
                    progressListener
                )

                if (isCancelled) {
                    handleCancellation(rawTempFile, processedFile)
                    return
                }

                if (dlResult.isFailure) {
                    throw dlResult.exceptionOrNull() ?: Exception("Lỗi khi tải stream")
                }

                // Processing stage (FFmpeg)
                val finalFileToPublish: File
                if (!task.selectedFormat.isVideo && !task.selectedFormat.isOriginalAudio) {
                    // Transcode to MP3 with chosen bitrate (128k, 192k, 256k, 320k)
                    _currentTask.value = _currentTask.value?.copy(status = DownloadStatus.PROCESSING)
                    DownloadForegroundService.updateStatus(context, context.getString(R.string.notif_processing_title, task.title))

                    val transcodeResult = mediaProcessor.transcodeToMp3(
                        rawTempFile.absolutePath,
                        processedFile.absolutePath,
                        task.selectedFormat.bitrateKbps,
                        onProgress = { }
                    )

                    if (transcodeResult.isFailure) {
                        throw transcodeResult.exceptionOrNull() ?: Exception("Lỗi chuyển đổi MP3")
                    }
                    finalFileToPublish = processedFile
                    rawTempFile.delete()
                } else if (task.selectedFormat.needsMerge) {
                    // Merging video & audio if separated
                    _currentTask.value = _currentTask.value?.copy(status = DownloadStatus.PROCESSING)
                    DownloadForegroundService.updateStatus(context, context.getString(R.string.notif_processing_title, task.title))

                    val mergeResult = mediaProcessor.mergeVideoAndAudio(
                        rawTempFile.absolutePath,
                        rawTempFile.absolutePath,
                        processedFile.absolutePath,
                        onProgress = { }
                    )
                    finalFileToPublish = if (mergeResult.isSuccess) processedFile else rawTempFile
                } else {
                    finalFileToPublish = rawTempFile
                }

                // Saving stage (MediaStore Scoped Storage)
                _currentTask.value = _currentTask.value?.copy(status = DownloadStatus.SAVING)

                val mimeType = when {
                    task.selectedFormat.isVideo -> "video/mp4"
                    task.selectedFormat.isOriginalAudio -> "audio/${task.selectedFormat.ext}"
                    else -> "audio/mpeg"
                }

                val mediaStoreUriResult = storageRepository.saveToMediaStore(
                    tempFile = finalFileToPublish,
                    targetFilename = task.outputFilename,
                    mimeType = mimeType,
                    isAudio = !task.selectedFormat.isVideo
                )

                if (mediaStoreUriResult.isFailure) {
                    throw mediaStoreUriResult.exceptionOrNull() ?: Exception("Lỗi lưu MediaStore")
                }

                val publishedUri = mediaStoreUriResult.getOrThrow()

                // Insert into History Database
                val historyEntity = HistoryEntity(
                    id = task.id,
                    title = task.title,
                    originalUrl = task.url,
                    mediaUriString = publishedUri.toString(),
                    relativePath = if (task.selectedFormat.isVideo) "Movies/VibeLoad" else "Music/VibeLoad",
                    fileName = task.outputFilename,
                    mimeType = mimeType,
                    isAudio = !task.selectedFormat.isVideo,
                    fileSize = finalFileToPublish.length(),
                    durationSeconds = metadata.durationSeconds,
                    thumbnailUri = metadata.thumbnail
                )
                historyRepository.addRecord(historyEntity)

                _currentTask.value = _currentTask.value?.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 100f
                )

                DownloadForegroundService.notifyComplete(context, task.title, true)
                break // Success! Exit retry loop.

            } catch (e: Exception) {
                if (isCancelled) {
                    handleCancellation(rawTempFile, processedFile)
                    return
                }

                attempt++
                if (attempt <= maxRetries) {
                    _currentTask.value = _currentTask.value?.copy(
                        status = DownloadStatus.RETRYING,
                        retryCount = attempt,
                        errorMessage = "Đang thử lại ($attempt/$maxRetries)..."
                    )
                    delay(2000L * attempt)
                } else {
                    _currentTask.value = _currentTask.value?.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = e.localizedMessage ?: "Tải thất bại"
                    )
                    DownloadForegroundService.notifyComplete(context, task.title, false)
                    rawTempFile.delete()
                    processedFile.delete()
                }
            }
        }
    }

    fun pauseDownload() {
        isPaused = true
        _currentTask.value = _currentTask.value?.copy(status = DownloadStatus.PAUSED)
    }

    fun resumeDownload() {
        if (isPaused) {
            isPaused = false
            _currentTask.value = _currentTask.value?.copy(status = DownloadStatus.DOWNLOADING)
        }
    }

    fun cancelDownload() {
        isCancelled = true
        isPaused = false
        mediaProcessor.cancelCurrentOperation()
        activeJob?.cancel()
        _currentTask.value = _currentTask.value?.copy(status = DownloadStatus.CANCELLED)
        DownloadForegroundService.stop(context)
    }

    fun retryDownload() {
        val task = _currentTask.value ?: return
        if (task.status == DownloadStatus.FAILED || task.status == DownloadStatus.CANCELLED) {
            isCancelled = false
            isPaused = false
            _currentTask.value = task.copy(status = DownloadStatus.RESOLVING, progress = 0f, errorMessage = null)
            DownloadForegroundService.start(context, task.title)
            activeJob = scope.launch {
                // Dummy metadata container for retry
                val dummyMeta = MediaMetadata(
                    url = task.url,
                    title = task.title,
                    thumbnail = "",
                    uploader = "",
                    durationSeconds = 0,
                    platform = "media",
                    videoOptions = listOf(task.selectedFormat),
                    audioOptions = emptyList()
                )
                executeDownloadPipeline(task, dummyMeta)
            }
        }
    }

    private fun handleCancellation(vararg files: File) {
        files.forEach { if (it.exists()) it.delete() }
        _currentTask.value = _currentTask.value?.copy(status = DownloadStatus.CANCELLED)
        DownloadForegroundService.stop(context)
    }
}
