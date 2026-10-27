package com.vibeload.app.data.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.vibeload.app.domain.repository.MediaStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MediaStoreStorageManager(
    private val context: Context
) : MediaStorageRepository {

    private val contentResolver: ContentResolver = context.contentResolver

    companion object {
        fun sanitize(rawTitle: String, extension: String): String {
            // Remove illegal characters on Android/Windows: \ / : * ? " < > |
            var sanitized = rawTitle
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .replace(Regex("\\.{2,}"), "") // prevent path traversal ..
                .trim()

            if (sanitized.isEmpty() || sanitized == "_") {
                sanitized = "VibeLoad_Media_${System.currentTimeMillis()}"
            }

            // Limit length to avoid filesystem max path issues
            if (sanitized.length > 100) {
                sanitized = sanitized.substring(0, 100).trim()
            }

            val cleanExt = extension.removePrefix(".")
            return if (sanitized.endsWith(".$cleanExt", ignoreCase = true)) {
                sanitized
            } else {
                "$sanitized.$cleanExt"
            }
        }
    }

    override fun sanitizeFilename(rawTitle: String, extension: String): String {
        return sanitize(rawTitle, extension)
    }

    override suspend fun saveToMediaStore(
        tempFile: File,
        targetFilename: String,
        mimeType: String,
        isAudio: Boolean
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (!tempFile.exists() || tempFile.length() == 0L) {
                return@withContext Result.failure(Exception("File nguồn tạm thời rỗng hoặc không tồn tại"))
            }

            val collection = if (isAudio) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            }

            val relativePath = if (isAudio) "Music/VibeLoad" else "Movies/VibeLoad"
            val uniqueDisplayName = resolveUniqueDisplayName(targetFilename, isAudio)

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, uniqueDisplayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val itemUri = contentResolver.insert(collection, values)
                ?: return@withContext Result.failure(Exception("Không thể tạo bản ghi MediaStore"))

            try {
                contentResolver.openOutputStream(itemUri)?.use { outStream ->
                    FileInputStream(tempFile).use { inStream ->
                        inStream.copyTo(outStream)
                        outStream.flush()
                    }
                } ?: throw Exception("Không thể mở luồng ghi MediaStore")

                // Mark file as complete (IS_PENDING = 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val completeValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    contentResolver.update(itemUri, completeValues, null, null)
                }

                // Delete temp file after successful publishing
                tempFile.delete()

                Result.success(itemUri)
            } catch (e: Exception) {
                contentResolver.delete(itemUri, null, null)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveImageToMediaStore(
        tempFile: File,
        targetFilename: String,
        mimeType: String,
        subfolder: String?
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (!tempFile.exists() || tempFile.length() == 0L) {
                return@withContext Result.failure(Exception("File ảnh nguồn tạm thời rỗng hoặc không tồn tại"))
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val relativePath = if (!subfolder.isNullOrBlank()) {
                "Pictures/VibeLoad/$subfolder"
            } else {
                "Pictures/VibeLoad"
            }
            val uniqueDisplayName = resolveUniqueDisplayNameForCollection(
                targetFilename,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "jpg"
            )

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, uniqueDisplayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val itemUri = contentResolver.insert(collection, values)
                ?: return@withContext Result.failure(Exception("Không thể tạo bản ghi MediaStore cho ảnh"))

            try {
                contentResolver.openOutputStream(itemUri)?.use { outStream ->
                    FileInputStream(tempFile).use { inStream ->
                        inStream.copyTo(outStream)
                        outStream.flush()
                    }
                } ?: throw Exception("Không thể mở luồng ghi MediaStore cho ảnh")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val completeValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    contentResolver.update(itemUri, completeValues, null, null)
                }

                tempFile.delete()
                Result.success(itemUri)
            } catch (e: Exception) {
                contentResolver.delete(itemUri, null, null)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolveUniqueDisplayName(baseName: String, isAudio: Boolean): String {
        val queryCollection = if (isAudio) {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val defaultExt = if (isAudio) "mp3" else "mp4"
        return resolveUniqueDisplayNameForCollection(baseName, queryCollection, defaultExt)
    }

    private fun resolveUniqueDisplayNameForCollection(
        baseName: String,
        collection: Uri,
        defaultExt: String
    ): String {
        val dotIndex = baseName.lastIndexOf('.')
        val nameWithoutExt = if (dotIndex > 0) baseName.substring(0, dotIndex) else baseName
        val ext = if (dotIndex > 0) baseName.substring(dotIndex) else ".$defaultExt"

        var candidate = "$nameWithoutExt$ext"
        var counter = 1

        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"

        while (true) {
            val cursor = contentResolver.query(collection, projection, selection, arrayOf(candidate), null)
            val exists = cursor?.use { it.count > 0 } ?: false
            if (!exists) {
                return candidate
            }
            candidate = "$nameWithoutExt ($counter)$ext"
            counter++
        }
    }

    override suspend fun deleteFile(uri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val rows = contentResolver.delete(uri, null, null)
            Result.success(rows > 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renameFile(uri: Uri, newName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
            }
            val rows = contentResolver.update(uri, values, null, null)
            Result.success(rows > 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAvailableStorageBytes(): Long {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            stat.availableBytes
        } catch (e: Exception) {
            1024L * 1024L * 1024L // 1 GB fallback
        }
    }
}
