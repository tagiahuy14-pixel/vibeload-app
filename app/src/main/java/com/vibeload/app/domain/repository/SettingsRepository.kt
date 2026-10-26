package com.vibeload.app.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val themeModeFlow: StateFlow<String>
    fun setThemeMode(mode: String)
    suspend fun clearHistory(): Result<Unit>
    suspend fun clearCache(): Result<Long>
    suspend fun clearTempFiles(): Result<Long>
    fun getAppVersion(): String
    fun getEngineVersion(): String
}
