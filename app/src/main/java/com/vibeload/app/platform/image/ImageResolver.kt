package com.vibeload.app.platform.image

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ResolvedImageInfo(
    val url: String,
    val title: String,
    val format: ImageFormat,
    val filename: String,
    val sizeBytes: Long,
    val mimeType: String,
    val width: Int = 0,
    val height: Int = 0
)

class ImageResolver(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    companion object {
        private const val DEFAULT_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }

    suspend fun resolve(rawUrl: String): Result<ResolvedImageInfo> = withContext(Dispatchers.IO) {
        val url = rawUrl.trim().trim('"', '\'')
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            return@withContext Result.failure(Exception("URL không hợp lệ (phải bắt đầu bằng http:// hoặc https://)"))
        }

        try {
            // Send ranged GET (0-1023 bytes) to inspect headers and read magic bytes safely
            var request = Request.Builder()
                .url(url)
                .header("User-Agent", DEFAULT_UA)
                .header("Range", "bytes=0-1023")
                .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .build()

            var response = client.newCall(request).execute()

            // If 416 (Range Not Satisfiable) or other range error, fallback to standard GET
            if (response.code == 416 || response.code == 400) {
                response.close()
                request = Request.Builder()
                    .url(url)
                    .header("User-Agent", DEFAULT_UA)
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .build()
                response = client.newCall(request).execute()
            }

            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                return@withContext Result.failure(Exception("Máy chủ từ chối yêu cầu (HTTP $code)"))
            }

            val finalUrl = response.request.url.toString()
            val contentType = response.header("Content-Type") ?: ""
            val contentDisposition = response.header("Content-Disposition") ?: ""
            val contentRange = response.header("Content-Range")
            val contentLength = if (contentRange != null && contentRange.contains("/")) {
                contentRange.substringAfterLast("/").trim().toLongOrNull() ?: 0L
            } else {
                response.header("Content-Length")?.toLongOrNull() ?: 0L
            }

            // Immediately reject HTML or JSON content-types
            if (contentType.contains("text/html", ignoreCase = true) ||
                contentType.contains("application/json", ignoreCase = true) ||
                contentType.contains("text/plain", ignoreCase = true) && !finalUrl.endsWith(".svg", ignoreCase = true)
            ) {
                response.close()
                return@withContext Result.failure(
                    Exception("Nội dung máy chủ trả về là tài liệu HTML/JSON/Text, không phải tệp ảnh hợp lệ.")
                )
            }

            // Read the initial sample bytes (up to 1024 bytes) to inspect binary magic bytes
            val sampleBytes = response.body?.byteStream()?.use { stream ->
                val buffer = ByteArray(1024)
                val read = stream.read(buffer)
                if (read > 0) buffer.copyOf(read) else ByteArray(0)
            } ?: ByteArray(0)
            response.close()

            // Reject if sample bytes binary pattern matches HTML or JSON error
            if (ImageFormatDetector.isTextOrHtmlOrJson(sampleBytes)) {
                return@withContext Result.failure(
                    Exception("Nội dung máy chủ trả về là tài liệu HTML/JSON, không phải tệp ảnh hợp lệ.")
                )
            }

            // Determine image format via Magic Bytes first, then Content-Type, then URL extension
            var detectedFormat = ImageFormatDetector.detectFormat(sampleBytes)

            if (detectedFormat == ImageFormat.UNKNOWN) {
                val mimeFormat = ImageFormat.fromMimeType(contentType)
                if (mimeFormat != null) {
                    detectedFormat = mimeFormat
                }
            }

            if (detectedFormat == ImageFormat.UNKNOWN) {
                val urlExt = extractExtensionFromUrl(finalUrl)
                val extFormat = ImageFormat.fromExtension(urlExt)
                if (extFormat != null && sampleBytes.isNotEmpty() && !ImageFormatDetector.isTextOrHtmlOrJson(sampleBytes)) {
                    detectedFormat = extFormat
                }
            }

            if (detectedFormat == ImageFormat.UNKNOWN) {
                return@withContext Result.failure(
                    Exception("Không nhận diện được định dạng ảnh hợp lệ từ liên kết này.")
                )
            }

            // Extract file name
            val filename = extractFilename(finalUrl, contentDisposition, detectedFormat)
            val title = filename.substringBeforeLast(".")

            Result.success(
                ResolvedImageInfo(
                    url = finalUrl,
                    title = title,
                    format = detectedFormat,
                    filename = filename,
                    sizeBytes = contentLength,
                    mimeType = detectedFormat.mimeType
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractFilename(url: String, contentDisposition: String, format: ImageFormat): String {
        // 1. Try Content-Disposition: filename="..."
        if (contentDisposition.isNotEmpty()) {
            val matcher = Pattern.compile("filename\\*?=['\"]?(?:UTF-8'')?([^'\";\\n]+)['\"]?", Pattern.CASE_INSENSITIVE)
                .matcher(contentDisposition)
            if (matcher.find()) {
                val name = matcher.group(1)?.trim() ?: ""
                if (name.isNotEmpty()) {
                    val cleanName = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    return if (cleanName.contains(".")) cleanName else "$cleanName.${format.extension}"
                }
            }
        }

        // 2. Try URL path segment
        try {
            val uri = URI(url)
            val path = uri.path ?: ""
            val lastSegment = path.substringAfterLast("/").trim()
            if (lastSegment.isNotEmpty() && !lastSegment.equals("image", ignoreCase = true)) {
                val clean = lastSegment.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                val ext = clean.substringAfterLast(".", "")
                return if (ImageFormat.fromExtension(ext) != null) {
                    clean
                } else {
                    "$clean.${format.extension}"
                }
            }
        } catch (_: Exception) { }

        // 3. Fallback timestamp
        return "VibeLoad_Image_${System.currentTimeMillis()}.${format.extension}"
    }

    private fun extractExtensionFromUrl(url: String): String {
        return try {
            val uri = URI(url)
            val path = uri.path ?: ""
            path.substringAfterLast(".", "").substringBefore("?").substringBefore("#")
        } catch (_: Exception) {
            ""
        }
    }
}
