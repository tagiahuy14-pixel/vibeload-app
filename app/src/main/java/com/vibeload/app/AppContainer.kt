package com.vibeload.app

import android.content.Context
import com.vibeload.app.data.local.AppDatabase
import com.vibeload.app.data.repository.HistoryRepositoryImpl
import com.vibeload.app.data.repository.MediaStoreStorageManager
import com.vibeload.app.data.repository.SettingsRepositoryImpl
import com.vibeload.app.domain.repository.DownloadEngine
import com.vibeload.app.domain.repository.HistoryRepository
import com.vibeload.app.domain.repository.MediaProcessor
import com.vibeload.app.domain.repository.MediaStorageRepository
import com.vibeload.app.domain.repository.SettingsRepository
import com.vibeload.app.platform.coordinator.DownloadCoordinator
import com.vibeload.app.platform.engine.ChaquopyDownloadEngine
import com.vibeload.app.platform.engine.FFmpegMediaProcessor
import com.vibeload.app.platform.update.AppUpdateManager

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val historyRepository: HistoryRepository by lazy {
        HistoryRepositoryImpl(database)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(context, database)
    }

    val storageRepository: MediaStorageRepository by lazy {
        MediaStoreStorageManager(context)
    }

    val downloadEngine: DownloadEngine by lazy {
        ChaquopyDownloadEngine()
    }

    val mediaProcessor: MediaProcessor by lazy {
        FFmpegMediaProcessor()
    }

    val downloadCoordinator: DownloadCoordinator by lazy {
        DownloadCoordinator(
            context = context,
            downloadEngine = downloadEngine,
            mediaProcessor = mediaProcessor,
            storageRepository = storageRepository,
            historyRepository = historyRepository
        )
    }

    val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManager(context)
    }
}
