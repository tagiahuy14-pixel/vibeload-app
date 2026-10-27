package com.vibeload.app.platform.image

import com.vibeload.app.domain.repository.DownloadProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ImageDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    companion object {
        private const val DEFAULT_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    suspend fun downloadImage(
        url: String,
        targetTempFile: File,
        progressListener: DownloadProgressListener? = null
    ): Result<ImageFormat> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", DEFAULT_UA)
            .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            .build()

        var outputStream: FileOutputStream? = null

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                return@withContext Result.failure(Exception("Tải ảnh thất bại: Máy chủ trả về mã lỗi HTTP $code"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Nội dung phản hồi rỗng từ máy chủ"))
            val totalBytes = body.contentLength()

            targetTempFile.parentFile?.mkdirs()
            outputStream = FileOutputStream(targetTempFile)

            val inputStream = body.byteStream()
            val buffer = ByteArray(8192)
            var downloadedBytes = 0L
            var lastTime = System.currentTimeMillis()
            var bytesSinceLastTime = 0L
            var currentSpeed = 0f

            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                if (progressListener?.isCancelled() == true) {
                    throw Exception("DOWNLOAD_CANCELLED")
                }

                outputStream.write(buffer, 0, read)
                downloadedBytes += read
                bytesSinceLastTime += read

                val now = System.currentTimeMillis()
                val elapsed = now - lastTime
                if (elapsed >= 300) {
                    currentSpeed = (bytesSinceLastTime * 1000f) / elapsed
                    lastTime = now
                    bytesSinceLastTime = 0
                }

                val percent = if (totalBytes > 0) {
                    (downloadedBytes.toFloat() / totalBytes) * 100f
                } else {
                    -1f // indeterminate
                }

                val eta = if (currentSpeed > 0 && totalBytes > downloadedBytes) {
                    ((totalBytes - downloadedBytes) / currentSpeed).toLong()
                } else {
                    0L
                }

                progressListener?.onProgress(
                    percent = if (percent >= 0f) percent.coerceIn(0f, 100f) else 50f,
                    speed = currentSpeed,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes.coerceAtLeast(downloadedBytes),
                    etaSeconds = eta
                )
            }

            outputStream.flush()
            outputStream.close()
            outputStream = null
            response.close()

            // Verify file magic bytes upon completion
            val detectedFormat = ImageFormatDetector.detectFormat(targetTempFile)
            if (detectedFormat == ImageFormat.UNKNOWN) {
                // Check if it's text/HTML/JSON error payload
                val sampleBytes = ByteArray(512)
                val readSample = targetTempFile.inputStream().use { it.read(sampleBytes) }
                val isText = if (readSample > 0) ImageFormatDetector.isTextOrHtmlOrJson(sampleBytes.copyOf(readSample)) else true
                
                targetTempFile.delete()
                return@withContext Result.failure(
                    Exception(
                        if (isText) "Dữ liệu tải về là tài liệu HTML/JSON, không phải tệp ảnh hợp lệ."
                        else "Định dạng tệp đã tải không được hỗ trợ hoặc bị lỗi dữ liệu."
                    )
                )
            }

            // Report 100% complete
            progressListener?.onProgress(
                percent = 100f,
                speed = 0f,
                downloadedBytes = targetTempFile.length(),
                totalBytes = targetTempFile.length(),
                etaSeconds = 0L
            )

            Result.success(detectedFormat)
        } catch (e: Exception) {
            outputStream?.close()
            if (targetTempFile.exists()) {
                targetTempFile.delete()
            }
            Result.failure(e)
        }
    }
}
