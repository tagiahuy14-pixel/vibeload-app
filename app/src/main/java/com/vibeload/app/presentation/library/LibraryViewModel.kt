package com.vibeload.app.presentation.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeload.app.data.local.entity.HistoryEntity
import com.vibeload.app.data.local.entity.isImage
import com.vibeload.app.data.local.entity.isVideo
import com.vibeload.app.domain.repository.HistoryRepository
import com.vibeload.app.domain.repository.MediaStorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibrarySortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    NAME_AZ,
    FILE_SIZE_DESC
}

data class LibraryUiState(
    val selectedTab: Int = 0, // 0: All, 1: Video, 2: Audio
    val searchQuery: String = "",
    val sortOrder: LibrarySortOrder = LibrarySortOrder.NEWEST_FIRST,
    val renamingItem: HistoryEntity? = null,
    val deletingItem: HistoryEntity? = null,
    val infoMessage: String? = null
)

class LibraryViewModel(
    private val historyRepository: HistoryRepository,
    private val storageRepository: MediaStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    val items: StateFlow<List<HistoryEntity>> = combine(
        historyRepository.getAllHistory(),
        _uiState
    ) { allList, state ->
        // Filter by tab: 0: All, 1: Video, 2: Audio, 3: Image
        val tabFiltered = when (state.selectedTab) {
            1 -> allList.filter { it.isVideo }
            2 -> allList.filter { it.isAudio }
            3 -> allList.filter { it.isImage }
            else -> allList
        }

        // Filter by search query
        val queryFiltered = if (state.searchQuery.isBlank()) {
            tabFiltered
        } else {
            val query = state.searchQuery.trim().lowercase()
            tabFiltered.filter {
                it.title.lowercase().contains(query) || it.fileName.lowercase().contains(query)
            }
        }

        // Sort items
        when (state.sortOrder) {
            LibrarySortOrder.NEWEST_FIRST -> queryFiltered.sortedByDescending { it.completedAt }
            LibrarySortOrder.OLDEST_FIRST -> queryFiltered.sortedBy { it.completedAt }
            LibrarySortOrder.NAME_AZ -> queryFiltered.sortedBy { it.fileName.lowercase() }
            LibrarySortOrder.FILE_SIZE_DESC -> queryFiltered.sortedByDescending { it.fileSize }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSortOrderChange(order: LibrarySortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun requestRename(item: HistoryEntity) {
        _uiState.update { it.copy(renamingItem = item) }
    }

    fun cancelRename() {
        _uiState.update { it.copy(renamingItem = null) }
    }

    fun confirmRename(item: HistoryEntity, newName: String) {
        viewModelScope.launch {
            val defaultExt = when {
                item.isImage -> item.fileName.substringAfterLast(".", "jpg")
                item.isAudio -> "mp3"
                else -> "mp4"
            }
            val sanitized = storageRepository.sanitizeFilename(newName, defaultExt)
            try {
                val uri = Uri.parse(item.mediaUriString)
                val res = storageRepository.renameFile(uri, sanitized)
                if (res.isSuccess) {
                    historyRepository.updateFileName(item.id, sanitized)
                    _uiState.update { it.copy(renamingItem = null, infoMessage = "Đã đổi tên tệp thành công") }
                } else {
                    _uiState.update { it.copy(renamingItem = null, infoMessage = "Không thể đổi tên tệp trong MediaStore") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(renamingItem = null, infoMessage = "Lỗi đổi tên: ${e.localizedMessage}") }
            }
        }
    }

    fun requestDelete(item: HistoryEntity) {
        _uiState.update { it.copy(deletingItem = item) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(deletingItem = null) }
    }

    fun confirmDelete(item: HistoryEntity) {
        viewModelScope.launch {
            try {
                val uri = Uri.parse(item.mediaUriString)
                storageRepository.deleteFile(uri)
                historyRepository.deleteRecord(item.id)
                _uiState.update { it.copy(deletingItem = null, infoMessage = "Đã xóa tệp thành công") }
            } catch (e: Exception) {
                _uiState.update { it.copy(deletingItem = null, infoMessage = "Lỗi khi xóa tệp: ${e.localizedMessage}") }
            }
        }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun shareItem(context: Context, item: HistoryEntity) {
        try {
            val uri = Uri.parse(item.mediaUriString)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ ${item.fileName}"))
        } catch (e: Exception) {
            _uiState.update { it.copy(infoMessage = "Không thể chia sẻ tệp này: ${e.localizedMessage}") }
        }
    }

    fun openWithExternalApp(context: Context, item: HistoryEntity) {
        try {
            val uri = Uri.parse(item.mediaUriString)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(viewIntent, "Mở bằng ứng dụng"))
        } catch (e: Exception) {
            _uiState.update { it.copy(infoMessage = "Không tìm thấy ứng dụng phù hợp để mở tệp") }
        }
    }

    fun openContainingFolder(context: Context, item: HistoryEntity) {
        try {
            val uri = Uri.parse(item.mediaUriString)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Mở tệp / thư mục"))
        } catch (e: Exception) {
            _uiState.update { it.copy(infoMessage = "Không thể mở thư mục") }
        }
    }

    fun setAsRingtone(context: Context, item: HistoryEntity, type: Int = android.media.RingtoneManager.TYPE_RINGTONE) {
        val uri = Uri.parse(item.mediaUriString)
        val typeLabel = if (type == android.media.RingtoneManager.TYPE_ALARM) "chuông báo thức" else "nhạc chuông cuộc gọi"
        when (val result = com.vibeload.app.platform.util.RingtoneHelper.setAsRingtone(context, uri, type)) {
            is com.vibeload.app.platform.util.RingtoneResult.Success -> {
                _uiState.update { it.copy(infoMessage = "Đã cài làm $typeLabel thành công! 🔔") }
            }
            is com.vibeload.app.platform.util.RingtoneResult.PermissionRequired -> {
                _uiState.update { it.copy(infoMessage = "Vui lòng cho phép 'Thay đổi cài đặt hệ thống' để cài nhạc chuông") }
                context.startActivity(result.intent)
            }
            is com.vibeload.app.platform.util.RingtoneResult.Error -> {
                _uiState.update { it.copy(infoMessage = "Lỗi cài $typeLabel: ${result.message}") }
            }
        }
    }
}
