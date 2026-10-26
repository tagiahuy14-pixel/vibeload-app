package com.vibeload.app.domain.repository

import android.net.Uri
import java.io.File

interface MediaStorageRepository {
    suspend fun saveToMediaStore(
        tempFile: File,
        targetFilename: String,
        mimeType: String,
        isAudio: Boolean
    ): Result<Uri>

    fun sanitizeFilename(rawTitle: String, extension: String): String
    suspend fun deleteFile(uri: Uri): Result<Boolean>
    suspend fun renameFile(uri: Uri, newName: String): Result<Boolean>
    fun getAvailableStorageBytes(): Long
}
