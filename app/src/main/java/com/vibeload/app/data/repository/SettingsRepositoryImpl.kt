package com.vibeload.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.vibeload.app.data.local.AppDatabase
import com.vibeload.app.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class SettingsRepositoryImpl(
    private val context: Context,
    private val database: AppDatabase
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vibeload_preferences", Context.MODE_PRIVATE)

    private val _themeModeFlow = MutableStateFlow(
        prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
    )
    override val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    override fun setThemeMode(mode: String) {
        val safeMode = when (mode) {
            "LIGHT", "DARK" -> mode
            else -> "SYSTEM"
        }
        prefs.edit().putString(KEY_THEME_MODE, safeMode).apply()
        _themeModeFlow.value = safeMode
    }

    override suspend fun clearHistory(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.historyDao().clearAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearCache(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            var freedBytes = 0L
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    if (!file.name.startsWith("vibeload_active_")) {
                        val len = file.length()
                        if (file.deleteRecursively()) {
                            freedBytes += len
                        }
                    }
                }
            }
            Result.success(freedBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearTempFiles(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            var freedBytes = 0L
            val tempDir = File(context.cacheDir, "vibeload_temp")
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { file ->
                    val len = file.length()
                    if (file.delete()) {
                        freedBytes += len
                    }
                }
            }
            Result.success(freedBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${pInfo.versionCode})"
        } catch (e: Exception) {
            "1.0.0 (1)"
        }
    }

    override fun getEngineVersion(): String {
        return "Chaquopy 16.1.0 + yt-dlp 2026.10.21 + FFmpeg n8.1.2"
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
    }
}
