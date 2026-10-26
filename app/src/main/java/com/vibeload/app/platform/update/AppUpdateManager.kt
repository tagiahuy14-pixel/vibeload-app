package com.vibeload.app.platform.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val releaseNotesVi: String,
    val downloadUrl: String,
    val apkChecksum: String?
)

class AppUpdateManager(
    private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates(owner: String = "vibeload", repo: String = "vibeload-app"): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "VibeLoad-Android-Client")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                // If release repo doesn't exist yet, gracefully return no update
                return@withContext Result.success(
                    UpdateInfo(
                        hasUpdate = false,
                        latestVersionName = "1.0.0",
                        releaseNotesVi = "Bạn đang sử dụng phiên bản mới nhất.",
                        downloadUrl = "",
                        apkChecksum = null
                    )
                )
            }

            val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Phản hồi rỗng từ GitHub"))
            val json = JSONObject(bodyStr)
            val tagName = json.optString("tag_name", "v1.0.0").removePrefix("v")
            val releaseNotes = json.optString("body", "Không có ghi chú phát hành.")

            var apkUrl = ""
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            val currentVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
            } catch (e: Exception) {
                "1.0.0"
            }

            val hasUpdate = isNewerVersion(tagName, currentVersion) && apkUrl.isNotEmpty()

            Result.success(
                UpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersionName = tagName,
                    releaseNotesVi = releaseNotes,
                    downloadUrl = apkUrl,
                    apkChecksum = null
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val len = maxOf(rParts.size, cParts.size)
        for (i in 0 until len) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    suspend fun downloadAndInstallApk(downloadUrl: String, onProgress: (Int) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(downloadUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Không thể tải file APK: HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Dữ liệu APK rỗng"))
            val contentLength = body.contentLength()

            val apkFile = File(context.cacheDir, "vibeload_update.apk")
            if (apkFile.exists()) apkFile.delete()

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            onProgress((totalRead * 100 / contentLength).toInt())
                        }
                    }
                    output.flush()
                }
            }

            // Launch system PackageInstaller via FileProvider
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)

            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
