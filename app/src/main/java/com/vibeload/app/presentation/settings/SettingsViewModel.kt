package com.vibeload.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeload.app.domain.repository.MediaStorageRepository
import com.vibeload.app.domain.repository.SettingsRepository
import com.vibeload.app.platform.update.AppUpdateManager
import com.vibeload.app.platform.update.UpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class SettingsUiState(
    val themeMode: String = "SYSTEM",
    val availableStorageFormatted: String = "Đang kiểm tra...",
    val appVersion: String = "1.0.0 (1)",
    val engineVersion: String = "",
    val isCheckingUpdate: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val message: String? = null,
    val showClearHistoryDialog: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val storageRepository: MediaStorageRepository,
    private val appUpdateManager: AppUpdateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = settingsRepository.themeModeFlow.value,
            appVersion = settingsRepository.getAppVersion(),
            engineVersion = settingsRepository.getEngineVersion()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.themeModeFlow.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        refreshStorageInfo()
    }

    fun refreshStorageInfo() {
        val freeBytes = storageRepository.getAvailableStorageBytes()
        val gb = freeBytes / (1024.0 * 1024.0 * 1024.0)
        _uiState.update {
            it.copy(availableStorageFormatted = String.format(Locale.US, "%.2f GB trống", gb))
        }
    }

    fun setThemeMode(mode: String) {
        settingsRepository.setThemeMode(mode)
    }

    fun clearCache() {
        viewModelScope.launch {
            val result = settingsRepository.clearCache()
            if (result.isSuccess) {
                val freed = result.getOrDefault(0L)
                val mb = freed / (1024.0 * 1024.0)
                _uiState.update {
                    it.copy(message = String.format(Locale.US, "Đã dọn dẹp bộ nhớ tạm: giải phóng %.1f MB", mb))
                }
                refreshStorageInfo()
            } else {
                _uiState.update { it.copy(message = "Lỗi khi xóa bộ nhớ tạm") }
            }
        }
    }

    fun requestClearHistory() {
        _uiState.update { it.copy(showClearHistoryDialog = true) }
    }

    fun dismissClearHistoryDialog() {
        _uiState.update { it.copy(showClearHistoryDialog = false) }
    }

    fun confirmClearHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(showClearHistoryDialog = false) }
            val result = settingsRepository.clearHistory()
            if (result.isSuccess) {
                _uiState.update { it.copy(message = "Đã xóa toàn bộ lịch sử tải về") }
            } else {
                _uiState.update { it.copy(message = "Lỗi khi xóa lịch sử") }
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true, message = null) }
            val res = appUpdateManager.checkForUpdates(isAuto = false, force = true)
            if (res.isSuccess) {
                val info = res.getOrThrow()
                _uiState.update {
                    it.copy(
                        isCheckingUpdate = false,
                        updateInfo = info,
                        message = if (info.hasUpdate) "Có bản cập nhật mới v${info.latestVersionName} (${info.latestVersionCode})" else "Bạn đang sử dụng phiên bản mới nhất (v${info.latestVersionName})"
                    )
                }
                if (info.hasUpdate) {
                    appUpdateManager.showManualUpdatePrompt(info)
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCheckingUpdate = false,
                        message = "Không thể kiểm tra bản cập nhật: Vui lòng kiểm tra kết nối mạng"
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
