package com.vibeload.app.presentation.downloading

import androidx.lifecycle.ViewModel
import com.vibeload.app.domain.model.DownloadTask
import com.vibeload.app.platform.coordinator.DownloadCoordinator
import kotlinx.coroutines.flow.StateFlow

class DownloadingViewModel(
    private val downloadCoordinator: DownloadCoordinator
) : ViewModel() {

    val activeTask: StateFlow<DownloadTask?> = downloadCoordinator.currentTask

    fun pause() {
        downloadCoordinator.pauseDownload()
    }

    fun resume() {
        downloadCoordinator.resumeDownload()
    }

    fun cancel() {
        downloadCoordinator.cancelDownload()
    }

    fun retry() {
        downloadCoordinator.retryDownload()
    }
}
