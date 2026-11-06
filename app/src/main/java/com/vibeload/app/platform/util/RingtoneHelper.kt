package com.vibeload.app.platform.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings

sealed class RingtoneResult {
    data object Success : RingtoneResult()
    data class PermissionRequired(val intent: Intent) : RingtoneResult()
    data class Error(val message: String) : RingtoneResult()
}

object RingtoneHelper {

    fun setAsRingtone(
        context: Context,
        audioUri: Uri,
        type: Int = RingtoneManager.TYPE_RINGTONE
    ): RingtoneResult {
        return try {
            // Check write settings permission
            if (!Settings.System.canWrite(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return RingtoneResult.PermissionRequired(intent)
            }

            // Update MediaStore flag if it's a MediaStore URI
            try {
                val values = ContentValues().apply {
                    when (type) {
                        RingtoneManager.TYPE_ALARM -> put(MediaStore.Audio.Media.IS_ALARM, 1)
                        RingtoneManager.TYPE_NOTIFICATION -> put(MediaStore.Audio.Media.IS_NOTIFICATION, 1)
                        else -> put(MediaStore.Audio.Media.IS_RINGTONE, 1)
                    }
                }
                context.contentResolver.update(audioUri, values, null, null)
            } catch (_: Exception) {
                // Ignore if update fails (e.g. file provider or external)
            }

            // Set actual default ringtone
            RingtoneManager.setActualDefaultRingtoneUri(context, type, audioUri)
            RingtoneResult.Success
        } catch (e: Exception) {
            RingtoneResult.Error(e.message ?: "Không thể cài đặt nhạc chuông")
        }
    }
}
