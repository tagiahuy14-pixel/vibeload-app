package com.vibeload.app.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val themeModeFlow: StateFlow<String>
    fun setThemeMode(mode: String)

    val dynamicColorFlow: StateFlow<Boolean>
    fun setDynamicColor(enabled: Boolean)

    val notifySoundFlow: StateFlow<Boolean>
    fun setNotifySound(enabled: Boolean)

    val notifyVibrateFlow: StateFlow<Boolean>
    fun setNotifyVibrate(enabled: Boolean)

    suspend fun clearHistory(): Result<Unit>
    suspend fun clearCache(): Result<Long>
    suspend fun clearTempFiles(): Result<Long>
    fun getAppVersion(): String
    fun getEngineVersion(): String
}
