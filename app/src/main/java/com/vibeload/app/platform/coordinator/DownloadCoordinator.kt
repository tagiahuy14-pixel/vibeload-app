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
import com.vibeload.app.platform.image.ImageDownloader
import com.vibeload.app.platform.service.DownloadForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class DownloadCompletedEvent(
    val title: String,
    val filePath: String,
    val fileName: String,
    val mimeType: String,
    val isAudio: Boolean,
    val fileSizeFormatted: String,
    val historyEntity: HistoryEntity
)

class DownloadCoordinator(
    private val context: Context,
    private val downloadEngine: DownloadEngine,
    private val mediaProcessor: MediaProcessor,
    private val storageRepository: MediaStorageRepository,
    private val historyRepository: HistoryRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val imageDownloader = ImageDownloader()

    private val _currentTask = MutableStateFlow<DownloadTask?>(null)
    val currentTask: StateFlow<DownloadTask?> = _currentTask.asStateFlow()

    private val _completedEvent = MutableSharedFlow<DownloadCompletedEvent>(extraBufferCapacity = 1)
    val completedEvent: SharedFlow<DownloadCompletedEvent> = _completedEvent.asSharedFlow()

    @Volatile
    private var isCancelled = false

    @Volatile
    private var isPaused = false

    private var activeJob: Job? = null

    fun startDownload(
        url: String,
        metadata: MediaMetadata,
        selectedFormat: FormatOption,
        customFilename: String,
        selectedImageUrls: List<String> = emptyList()
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
            selectedFormat.isImage -> selectedFormat.ext.ifEmpty { "jpg" }
            selectedFormat.isVideo -> "mp4"
            selectedFormat.isOriginalAudio -> selectedFormat.ext
            else -> "mp3"
        }
        val safeName = storageRepository.sanitizeFilename(customFilename, extension)

        val isBatchAlbum = selectedFormat.isImage && (selectedImageUrls.isNotEmpty() || selectedFormat.formatId == "ALL_IMAGES" || selectedFormat.label.contains("Tải tất cả", ignoreCase = true))
        val totalCount = when {
            selectedImageUrls.isNotEmpty() -> selectedImageUrls.size
            isBatchAlbum -> metadata.imageOptions.count { it.formatId != "ALL_IMAGES" }
            else -> 1
        }

        val task = DownloadTask(
            id = taskId,
            url = url,
            title = metadata.title,
            outputFilename = safeName,
            selectedFormat = selectedFormat,
            status = DownloadStatus.RESOLVING,
            totalBytes = selectedFormat.filesize,
            currentItemIndex = 1,
            totalItemCount = totalCount,
            selectedImageUrls = selectedImageUrls
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
        val tempExt = when {
            task.selectedFormat.isImage -> task.selectedFormat.ext.ifEmpty { "jpg" }
            task.selectedFormat.isVideo -> task.selectedFormat.ext.ifEmpty { "mp4" }
            else -> "mp3"
        }
        val rawTempFile = File(tempDir, "temp_dl_${task.id}.$tempExt")
        val videoTempFile = File(tempDir, "temp_vid_${task.id}.${task.selectedFormat.ext.ifEmpty { "mp4" }}")
        val audioTempFile = File(tempDir, "temp_aud_${task.id}.m4a")
        val processedFile = File(tempDir, "temp_proc_${task.id}.${if (task.selectedFormat.isVideo) "mp4" else if (task.selectedFormat.isImage) tempExt else "mp3"}")

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

                val finalFileToPublish: File

                if (task.selectedFormat.isImage) {
                    val realImageOptions = metadata.imageOptions.filter { it.formatId != "ALL_IMAGES" }
                    val isBatchAlbum = task.selectedImageUrls.isNotEmpty() ||
                            task.selectedFormat.formatId == "ALL_IMAGES" ||
                            task.selectedFormat.label.contains("Tải tất cả", ignoreCase = true)

                    val imagesToDownload = when {
                        task.selectedImageUrls.isNotEmpty() -> {
                            realImageOptions.filter { it.formatId in task.selectedImageUrls }
                        }
                        isBatchAlbum -> {
                            if (realImageOptions.isNotEmpty()) realImageOptions else listOf(task.selectedFormat)
                        }
                        else -> {
                            listOf(task.selectedFormat)
                        }
                    }

                    val totalImages = imagesToDownload.size
                    val postId = metadata.postId.ifEmpty {
                        val match = Regex("/photo/(\\d+)").find(metadata.url)
                        match?.groupValues?.get(1) ?: task.id.take(8)
                    }
                    val subfolder = if (metadata.platform.equals("tiktok", ignoreCase = true)) {
                        "TikTok_$postId"
                    } else if (totalImages > 1) {
                        "Album_$postId"
                    } else {
                        null
                    }

                    for ((idx, imgOption) in imagesToDownload.withIndex()) {
                        if (isCancelled) {
                            handleCancellation(rawTempFile, videoTempFile, audioTempFile, processedFile)
                            return
                        }

                        val currentIdx = idx + 1
                        _currentTask.value = _currentTask.value?.copy(
                            status = DownloadStatus.DOWNLOADING,
                            currentItemIndex = currentIdx,
                            totalItemCount = totalImages,
                            statusMessage = if (totalImages > 1) "Đang tải $currentIdx/$totalImages ảnh..." else "Đang tải ảnh..."
                        )

                        val imgTempFile = File(tempDir, "temp_img_${task.id}_$currentIdx.${imgOption.ext.ifEmpty { "jpg" }}")
                        val imageListener = object : DownloadProgressListener {
                            override fun onProgress(
                                percent: Float,
                                speed: Float,
                                downloadedBytes: Long,
                                totalBytes: Long,
                                etaSeconds: Long
                            ) {
                                val overallPercent = if (totalImages > 1) {
                                    (((idx.toFloat() + (percent / 100f).coerceIn(0f, 1f)) / totalImages) * 100f).coerceIn(0f, 100f)
                                } else {
                                    percent
                                }
                                _currentTask.value = _currentTask.value?.copy(
                                    progress = overallPercent,
                                    speedBytesPerSec = speed,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = if (totalBytes > 0) totalBytes else task.totalBytes,
                                    etaSeconds = etaSeconds,
                                    currentItemIndex = currentIdx,
                                    totalItemCount = totalImages,
                                    statusMessage = if (totalImages > 1) "Đang tải $currentIdx/$totalImages ảnh (${overallPercent.toInt()}%)" else "Đang tải ảnh..."
                                )
                                DownloadForegroundService.updateProgress(
                                    context,
                                    task.title,
                                    overallPercent.toInt(),
                                    speed,
                                    false
                                )
                            }

                            override fun isCancelled(): Boolean = isCancelled || isPaused
                        }

                        val targetImageUrl = if (imgOption.formatId.startsWith("http")) {
                            imgOption.formatId
                        } else {
                            task.url
                        }

                        val imgResult = imageDownloader.downloadImage(
                            url = targetImageUrl,
                            targetTempFile = imgTempFile,
                            progressListener = imageListener
                        )

                        if (isCancelled) {
                            if (imgTempFile.exists()) imgTempFile.delete()
                            handleCancellation(rawTempFile, videoTempFile, audioTempFile, processedFile)
                            return
                        }

                        if (imgResult.isFailure) {
                            if (imgTempFile.exists()) imgTempFile.delete()
                            throw imgResult.exceptionOrNull() ?: Exception("Lỗi khi tải ảnh $currentIdx/$totalImages")
                        }

                        val detectedFormat = imgResult.getOrThrow()
                        val ext = detectedFormat.extension
                        val mimeType = detectedFormat.mimeType

                        val itemFilename = if (totalImages > 1) {
                            val baseName = storageRepository.sanitizeFilename(task.outputFilename, "").removeSuffix(".")
                            "${baseName}_$currentIdx.$ext"
                        } else {
                            task.outputFilename
                        }

                        val saveResult = storageRepository.saveImageToMediaStore(
                            tempFile = imgTempFile,
                            targetFilename = itemFilename,
                            mimeType = mimeType,
                            subfolder = subfolder
                        )

                        if (saveResult.isFailure) {
                            throw saveResult.exceptionOrNull() ?: Exception("Lỗi lưu ảnh $currentIdx vào thư viện")
                        }

                        val publishedUri = saveResult.getOrThrow()

                        val historyEntity = HistoryEntity(
                            id = if (totalImages > 1) "${task.id}_$currentIdx" else task.id,
                            title = if (totalImages > 1) "${task.title} (Ảnh $currentIdx/$totalImages)" else task.title,
                            originalUrl = task.url,
                            mediaUriString = publishedUri.toString(),
                            relativePath = if (!subfolder.isNullOrBlank()) "Pictures/VibeLoad/$subfolder" else "Pictures/VibeLoad",
                            fileName = itemFilename,
                            mimeType = mimeType,
                            isAudio = false,
                            fileSize = imgTempFile.length().coerceAtLeast(1L),
                            durationSeconds = 0,
                            thumbnailUri = publishedUri.toString()
                        )
                        historyRepository.addRecord(historyEntity)
                    }

                    // Complete batch image task
                    _currentTask.value = _currentTask.value?.copy(
                        status = DownloadStatus.COMPLETED,
                        progress = 100f,
                        currentItemIndex = totalImages,
                        totalItemCount = totalImages,
                        statusMessage = if (totalImages > 1) "Đã tải xong $totalImages/$totalImages ảnh" else "Đã tải xong ảnh"
                    )
                    val completeMsg = if (totalImages > 1) "Đã tải xong $totalImages ảnh vào Pictures/VibeLoad/${subfolder ?: ""}" else "Đã tải xong ảnh"
                    DownloadForegroundService.updateStatus(context, completeMsg)
                    return
                } else if (task.selectedFormat.isVideo) {
                    if (task.selectedFormat.needsMerge) {
                        // Two-stage download: Video track + Audio track, then merge via FFmpegKit (in-process JNI)
                        // Step 1: Download video track (0% -> 70%)
                        val videoListener = object : DownloadProgressListener {
                            override fun onProgress(
                                percent: Float,
                                speed: Float,
                                downloadedBytes: Long,
                                totalBytes: Long,
                                etaSeconds: Long
                            ) {
                                val scaledPercent = (percent * 0.70f).coerceIn(0f, 70f)
                                _currentTask.value = _currentTask.value?.copy(
                                    progress = scaledPercent,
                                    speedBytesPerSec = speed,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = if (totalBytes > 0) (totalBytes * 1.3f).toLong() else task.totalBytes,
                                    etaSeconds = etaSeconds
                                )
                                DownloadForegroundService.updateProgress(
                                    context,
                                    task.title,
                                    scaledPercent.toInt(),
                                    speed,
                                    false
                                )
                            }

                            override fun isCancelled(): Boolean = isCancelled || isPaused
                        }

                        val vidFormatSpec = if (task.selectedFormat.formatId.isNotEmpty()) task.selectedFormat.formatId else "bestvideo"
                        val vidResult = downloadEngine.downloadStream(
                            task.url,
                            vidFormatSpec,
                            videoTempFile.absolutePath,
                            videoListener
                        )

                        if (isCancelled) {
                            handleCancellation(rawTempFile, videoTempFile, audioTempFile, processedFile)
                            return
                        }

                        if (vidResult.isFailure) {
                            throw vidResult.exceptionOrNull() ?: Exception("Lỗi khi tải luồng hình ảnh")
                        }

                        // Step 2: Download audio track (70% -> 90%)
                        val audioListener = object : DownloadProgressListener {
                            override fun onProgress(
                                percent: Float,
                                speed: Float,
                                downloadedBytes: Long,
                                totalBytes: Long,
                                etaSeconds: Long
                            ) {
                                val scaledPercent = (70f + percent * 0.20f).coerceIn(70f, 90f)
                                _currentTask.value = _currentTask.value?.copy(
                                    progress = scaledPercent,
                                    speedBytesPerSec = speed,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = task.totalBytes,
                                    etaSeconds = etaSeconds
                                )
                                DownloadForegroundService.updateProgress(
                                    context,
                                    task.title,
                                    scaledPercent.toInt(),
                                    speed,
                                    false
                                )
                            }

                            override fun isCancelled(): Boolean = isCancelled || isPaused
                        }

                        val audFormatSpec = "bestaudio[ext=m4a]/bestaudio/best"
                        val audResult = downloadEngine.downloadStream(
                            task.url,
                            audFormatSpec,
                            audioTempFile.absolutePath,
                            audioListener
                        )

                        if (isCancelled) {
                            handleCancellation(rawTempFile, videoTempFile, audioTempFile, processedFile)
                            return
                        }

                        if (audResult.isFailure) {
                            throw audResult.exceptionOrNull() ?: Exception("Lỗi khi tải luồng âm thanh")
                        }

                        // Step 3: Merge video and audio with FFmpegKit (90% -> 98%)
                        _currentTask.value = _currentTask.value?.copy(status = DownloadStatus.PROCESSING, progress = 92f)
                        DownloadForegroundService.updateStatus(context, context.getString(R.string.notif_processing_title, task.title))

                        val mergeResult = mediaProcessor.mergeVideoAndAudio(
                            videoTempFile.absolutePath,
                            audioTempFile.absolutePath,
                            processedFile.absolutePath,
                            onProgress = { }
                        )

                        // Clean up raw component streams
                        videoTempFile.delete()
                        audioTempFile.delete()

                        if (mergeResult.isFailure) {
                            processedFile.delete()
                            throw mergeResult.exceptionOrNull() ?: Exception("Lỗi khi ghép hình ảnh và âm thanh.")
                        }

                        finalFileToPublish = processedFile
                    } else {
                        // Single combined video+audio stream (Facebook sd/hd, TikTok, YouTube 18/22)
                        val combinedListener = object : DownloadProgressListener {
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

                        val formatSpec = if (task.selectedFormat.formatId.isNotEmpty()) task.selectedFormat.formatId else "best"
                        val dlResult = downloadEngine.downloadStream(
                            task.url,
                            formatSpec,
                            rawTempFile.absolutePath,
                            combinedListener
                        )

                        if (isCancelled) {
                            handleCancellation(rawTempFile, videoTempFile, audioTempFile, processedFile)
                            return
                        }

                        if (dlResult.isFailure) {
                            throw dlResult.exceptionOrNull() ?: Exception("Lỗi khi tải video")
                        }

                        finalFileToPublish = rawTempFile
                    }
                } else {
                    // Audio stream
                    val audioListener = object : DownloadProgressListener {
                        override fun onProgress(
                            percent: Float,
                            speed: Float,
                            downloadedBytes: Long,
                            totalBytes: Long,
                            etaSeconds: Long
                        ) {
                            val scaledPercent = if (task.selectedFormat.isOriginalAudio) percent else (percent * 0.80f).coerceIn(0f, 80f)
                            _currentTask.value = _currentTask.value?.copy(
                                progress = scaledPercent,
                                speedBytesPerSec = speed,
                                downloadedBytes = downloadedBytes,
                                totalBytes = if (totalBytes > 0) totalBytes else task.totalBytes,
                                etaSeconds = etaSeconds
                            )
                            DownloadForegroundService.updateProgress(
                                context,
                                task.title,
                                scaledPercent.toInt(),
                                speed,
                                false
                            )
                        }

                        override fun isCancelled(): Boolean = isCancelled || isPaused
                    }

                    val formatSpec = if (task.selectedFormat.formatId.startsWith("http")) {
                        task.selectedFormat.formatId
                    } else if (task.selectedFormat.isOriginalAudio) {
                        if (task.selectedFormat.formatId.isNotEmpty()) task.selectedFormat.formatId else "bestaudio/best"
                    } else {
                        "bestaudio/best"
                    }

                    val dlResult = downloadEngine.downloadStream(
                        task.url,
                        formatSpec,
                        rawTempFile.absolutePath,
                        audioListener
                    )

                    if (isCancelled) {
                        handleCancellation(rawTempFile, videoTempFile, audioTempFile, processedFile)
                        return
                    }

                    if (dlResult.isFailure) {
                        throw dlResult.exceptionOrNull() ?: Exception("Lỗi khi tải luồng âm thanh")
                    }

                    if (!task.selectedFormat.isOriginalAudio) {
                        // Transcode to MP3 with chosen bitrate (128k, 192k, 256k, 320k)
                        _currentTask.value = _currentTask.value?.copy(status = DownloadStatus.PROCESSING, progress = 85f)
                        DownloadForegroundService.updateStatus(context, context.getString(R.string.notif_processing_title, task.title))

                        val transcodeResult = mediaProcessor.transcodeToMp3(
                            rawTempFile.absolutePath,
                            processedFile.absolutePath,
                            task.selectedFormat.bitrateKbps,
                            onProgress = { }
                        )

                        rawTempFile.delete()

                        if (transcodeResult.isFailure) {
                            processedFile.delete()
                            throw transcodeResult.exceptionOrNull() ?: Exception("Lỗi chuyển đổi MP3")
                        }
                        finalFileToPublish = processedFile
                    } else {
                        finalFileToPublish = rawTempFile
                    }
                }

                // Saving stage (MediaStore Scoped Storage)
                _currentTask.value = _currentTask.value?.copy(status = DownloadStatus.SAVING, progress = 99f)

                val mimeType = when {
                    task.selectedFormat.isImage -> task.selectedFormat.mimeType.ifEmpty { "image/jpeg" }
                    task.selectedFormat.isVideo -> "video/mp4"
                    task.selectedFormat.isOriginalAudio -> "audio/${task.selectedFormat.ext}"
                    else -> "audio/mpeg"
                }

                val finalFileSize = finalFileToPublish.length().coerceAtLeast(1L)

                val mediaStoreUriResult = if (task.selectedFormat.isImage) {
                    storageRepository.saveImageToMediaStore(
                        tempFile = finalFileToPublish,
                        targetFilename = task.outputFilename,
                        mimeType = mimeType
                    )
                } else {
                    storageRepository.saveToMediaStore(
                        tempFile = finalFileToPublish,
                        targetFilename = task.outputFilename,
                        mimeType = mimeType,
                        isAudio = !task.selectedFormat.isVideo
                    )
                }

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
                    relativePath = if (task.selectedFormat.isImage) "Pictures/VibeLoad" else if (task.selectedFormat.isVideo) "Movies/VibeLoad" else "Music/VibeLoad",
                    fileName = task.outputFilename,
                    mimeType = mimeType,
                    isAudio = !task.selectedFormat.isVideo && !task.selectedFormat.isImage,
                    fileSize = finalFileSize,
                    durationSeconds = metadata.durationSeconds,
                    thumbnailUri = if (task.selectedFormat.isImage) publishedUri.toString() else metadata.thumbnail
                )
                historyRepository.addRecord(historyEntity)

                // Clean up final temporary file if still existing
                if (finalFileToPublish.exists()) {
                    finalFileToPublish.delete()
                }

                _currentTask.value = _currentTask.value?.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 100f
                )

                val fileSizeFormatted = FormatOption(formatId = "", label = "", filesize = finalFileSize).getFormattedSize()
                DownloadForegroundService.notifyComplete(
                    context = context,
                    title = task.title,
                    isSuccess = true,
                    filePath = finalFileToPublish.absolutePath,
                    mimeType = mimeType,
                    fileSizeFormatted = fileSizeFormatted
                )
                _completedEvent.tryEmit(
                    DownloadCompletedEvent(
                        title = task.title,
                        filePath = publishedUri.toString(),
                        fileName = task.outputFilename,
                        mimeType = mimeType,
                        isAudio = !task.selectedFormat.isVideo && !task.selectedFormat.isImage,
                        fileSizeFormatted = fileSizeFormatted,
                        historyEntity = historyEntity
                    )
                )
                break // Success! Exit retry loop.

            } catch (e: Exception) {
                if (isCancelled) {
                    handleCancellation(rawTempFile, videoTempFile, audioTempFile, processedFile)
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
                    handleCancellation(rawTempFile, videoTempFile, audioTempFile, processedFile)
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
                    platform = if (task.selectedFormat.isImage) "image" else "media",
                    videoOptions = if (task.selectedFormat.isVideo) listOf(task.selectedFormat) else emptyList(),
                    audioOptions = if (!task.selectedFormat.isVideo && !task.selectedFormat.isImage) listOf(task.selectedFormat) else emptyList(),
                    imageOptions = if (task.selectedFormat.isImage) listOf(task.selectedFormat) else emptyList(),
                    isImageOnly = task.selectedFormat.isImage
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
