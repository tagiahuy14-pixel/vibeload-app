package com.vibeload.app.presentation.home

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeload.app.domain.model.FormatOption
import com.vibeload.app.domain.model.MediaMetadata
import com.vibeload.app.domain.repository.DownloadEngine
import com.vibeload.app.domain.repository.MediaStorageRepository
import com.vibeload.app.platform.coordinator.DownloadCoordinator
import com.vibeload.app.platform.image.ImageResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val urlInput: String = "",
    val isAnalyzing: Boolean = false,
    val metadata: MediaMetadata? = null,
    val selectedFormat: FormatOption? = null,
    val customFilename: String = "",
    val errorMessage: String? = null,
    val isStartingDownload: Boolean = false
)

class HomeViewModel(
    private val downloadEngine: DownloadEngine,
    private val downloadCoordinator: DownloadCoordinator,
    private val storageRepository: MediaStorageRepository,
    private val imageResolver: ImageResolver = ImageResolver()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onUrlChange(newUrl: String) {
        _uiState.update { it.copy(urlInput = newUrl, errorMessage = null) }
    }

    fun pasteFromClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipData = clipboard?.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text?.toString()?.trim() ?: ""
            val normalized = normalizeUrl(text)
            if (isValidUrl(normalized)) {
                _uiState.update { it.copy(urlInput = normalized, errorMessage = null) }
                analyzeUrl()
            } else {
                _uiState.update { it.copy(errorMessage = "Nội dung khay nhớ tạm không phải là URL hợp lệ") }
            }
        }
    }

    fun handleSharedUrl(sharedText: String) {
        val extractedUrl = extractUrlFromText(sharedText)
        if (extractedUrl.isNotEmpty()) {
            val normalized = normalizeUrl(extractedUrl)
            _uiState.update { it.copy(urlInput = normalized, errorMessage = null) }
            analyzeUrl()
        }
    }

    private fun isPossibleDirectImageUrl(url: String): Boolean {
        val lower = url.lowercase()
        val imageExtensions = listOf(
            ".jpg", ".jpeg", ".png", ".webp", ".gif", ".avif", ".bmp", ".tiff", ".tif", ".heic", ".heif", ".svg", ".ico"
        )
        val hasExt = imageExtensions.any { lower.contains(it) }
        val hasImagePath = lower.contains("/images/") || lower.contains("/img/") ||
                lower.contains("/photo/") || lower.contains("/photos/") ||
                lower.contains("unsplash.com") || lower.contains("pexels.com") ||
                lower.contains("pixabay.com") || lower.contains("giphy.com") ||
                lower.contains("tenor.com") || lower.contains("imgur.com") ||
                lower.contains("picsum.photos")
        return hasExt || hasImagePath
    }

    fun analyzeUrl() {
        val rawUrl = _uiState.value.urlInput.trim()
        val url = normalizeUrl(rawUrl)
        if (url != rawUrl) {
            _uiState.update { it.copy(urlInput = url) }
        }
        if (!isValidUrl(url)) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập đường dẫn URL hợp lệ (bắt đầu bằng http:// hoặc https://)") }
            return
        }

        // Check if playlist (FR-01)
        if (url.contains("list=", ignoreCase = true) && !url.contains("watch?v=", ignoreCase = true)) {
            _uiState.update { it.copy(errorMessage = "VibeLoad chỉ tải 1 video tại một thời điểm, không hỗ trợ playlist. Vui lòng chọn link video đơn lẻ.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }

            // Step 1: If URL is a direct image candidate, attempt image resolution first
            if (isPossibleDirectImageUrl(url)) {
                val imgResult = imageResolver.resolve(url)
                if (imgResult.isSuccess) {
                    val imgInfo = imgResult.getOrThrow()
                    val imageFormatOption = FormatOption(
                        formatId = "img_${imgInfo.format.extension}",
                        label = "Ảnh ${imgInfo.format.displayName}",
                        ext = imgInfo.format.extension,
                        resolution = if (imgInfo.width > 0 && imgInfo.height > 0) "${imgInfo.width}x${imgInfo.height}" else imgInfo.format.displayName,
                        filesize = imgInfo.sizeBytes,
                        isVideo = false,
                        width = imgInfo.width,
                        height = imgInfo.height,
                        isImage = true,
                        mimeType = imgInfo.mimeType
                    )
                    val meta = MediaMetadata(
                        url = imgInfo.url,
                        title = imgInfo.title,
                        thumbnail = imgInfo.url,
                        uploader = "VibeLoad Image",
                        durationSeconds = 0,
                        platform = "image",
                        videoOptions = emptyList(),
                        audioOptions = emptyList(),
                        imageOptions = listOf(imageFormatOption),
                        isImageOnly = true
                    )
                    val sanitizedName = storageRepository.sanitizeFilename(meta.title, imageFormatOption.ext)
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            metadata = meta,
                            selectedFormat = imageFormatOption,
                            customFilename = sanitizedName,
                            errorMessage = null
                        )
                    }
                    return@launch
                }
            }

            // Step 2: Attempt standard media extraction (video / audio from YouTube, TikTok, Facebook, etc.)
            val result = downloadEngine.analyzeUrl(url)
            if (result.isSuccess) {
                val meta = result.getOrThrow()
                val defaultFmt = meta.videoOptions.firstOrNull { it.height == 720 }
                    ?: meta.videoOptions.lastOrNull()
                    ?: meta.audioOptions.firstOrNull()

                val defaultExt = defaultFmt?.ext ?: "mp4"
                val sanitizedName = storageRepository.sanitizeFilename(meta.title, defaultExt)

                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        metadata = meta,
                        selectedFormat = defaultFmt,
                        customFilename = sanitizedName,
                        errorMessage = null
                    )
                }
            } else {
                // Step 3: Fallback check with image resolver (for extension-less image URLs or custom redirects)
                val fallbackImgResult = imageResolver.resolve(url)
                if (fallbackImgResult.isSuccess) {
                    val imgInfo = fallbackImgResult.getOrThrow()
                    val imageFormatOption = FormatOption(
                        formatId = "img_${imgInfo.format.extension}",
                        label = "Ảnh ${imgInfo.format.displayName}",
                        ext = imgInfo.format.extension,
                        resolution = if (imgInfo.width > 0 && imgInfo.height > 0) "${imgInfo.width}x${imgInfo.height}" else imgInfo.format.displayName,
                        filesize = imgInfo.sizeBytes,
                        isVideo = false,
                        width = imgInfo.width,
                        height = imgInfo.height,
                        isImage = true,
                        mimeType = imgInfo.mimeType
                    )
                    val meta = MediaMetadata(
                        url = imgInfo.url,
                        title = imgInfo.title,
                        thumbnail = imgInfo.url,
                        uploader = "VibeLoad Image",
                        durationSeconds = 0,
                        platform = "image",
                        videoOptions = emptyList(),
                        audioOptions = emptyList(),
                        imageOptions = listOf(imageFormatOption),
                        isImageOnly = true
                    )
                    val sanitizedName = storageRepository.sanitizeFilename(meta.title, imageFormatOption.ext)
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            metadata = meta,
                            selectedFormat = imageFormatOption,
                            customFilename = sanitizedName,
                            errorMessage = null
                        )
                    }
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Không thể phân tích liên kết này"
                    _uiState.update { it.copy(isAnalyzing = false, errorMessage = err) }
                }
            }
        }
    }

    fun selectFormat(format: FormatOption) {
        val meta = _uiState.value.metadata ?: return
        val ext = when {
            format.isImage -> format.ext.ifEmpty { "jpg" }
            format.isVideo -> "mp4"
            format.isOriginalAudio -> format.ext
            else -> "mp3"
        }
        val updatedName = storageRepository.sanitizeFilename(meta.title, ext)

        _uiState.update {
            it.copy(selectedFormat = format, customFilename = updatedName)
        }
    }

    fun onFilenameChange(newName: String) {
        _uiState.update { it.copy(customFilename = newName) }
    }

    fun startDownload(onSuccess: () -> Unit) {
        val state = _uiState.value
        val meta = state.metadata ?: return
        val format = state.selectedFormat ?: return
        val filename = state.customFilename.ifEmpty { meta.title }

        val result = downloadCoordinator.startDownload(
            url = meta.url,
            metadata = meta,
            selectedFormat = format,
            customFilename = filename
        )

        if (result.isSuccess) {
            onSuccess()
        } else {
            val err = result.exceptionOrNull()?.localizedMessage ?: "Không thể bắt đầu tác vụ tải"
            _uiState.update { it.copy(errorMessage = err) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun isValidUrl(url: String): Boolean {
            if (url.isEmpty()) return false
            val lower = url.lowercase()
            return (lower.startsWith("https://") || lower.startsWith("http://")) &&
                    !lower.startsWith("file://") &&
                    !lower.startsWith("content://") &&
                    !lower.contains("127.0.0.1") &&
                    !lower.contains("localhost")
        }

        fun normalizeUrl(raw: String): String {
            var url = raw.trim().trim('"', '\'')
            if (url.contains("threads.com", ignoreCase = true)) {
                url = url.replace("threads.com", "threads.net", ignoreCase = true)
            }
            return url
        }

        fun extractUrlFromText(text: String): String {
            val parts = text.split("\\s+".toRegex())
            for (part in parts) {
                val cleaned = part.trim().trim('"', '\'')
                if (isValidUrl(cleaned)) return normalizeUrl(cleaned)
            }
            return ""
        }
    }
}
