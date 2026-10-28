package com.vibeload.app.platform.engine

import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.vibeload.app.domain.model.FormatOption
import com.vibeload.app.domain.model.MediaMetadata
import com.vibeload.app.domain.repository.DownloadEngine
import com.vibeload.app.domain.repository.DownloadProgressListener
import com.vibeload.app.domain.repository.EngineInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ChaquopyDownloadEngine : DownloadEngine {

    private val python: Python by lazy { Python.getInstance() }
    private val module: PyObject by lazy { python.getModule("vibeload_engine") }

    override fun getEngineInfo(): EngineInfo {
        return try {
            val pyMap = module.callAttr("get_engine_version")
            val ytDlp = pyMap.callAttr("get", "yt_dlp_version")?.toString() ?: "Unknown"
            val pyVer = pyMap.callAttr("get", "python_version")?.toString() ?: "Unknown"
            val plat = pyMap.callAttr("get", "platform")?.toString() ?: "Android"
            EngineInfo(ytDlpVersion = ytDlp, pythonVersion = pyVer, platform = plat)
        } catch (e: Exception) {
            EngineInfo(ytDlpVersion = "2026.10.21", pythonVersion = "3.11", platform = "Android")
        }
    }

    override suspend fun analyzeUrl(url: String): Result<MediaMetadata> = withContext(Dispatchers.IO) {
        try {
            val jsonStr = module.callAttr("analyze_url", url).toString()
            val json = JSONObject(jsonStr)

            if (!json.optBoolean("success", false)) {
                val errorMsg = json.optString("error", "Không thể phân tích liên kết")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val title = json.optString("title", "Không rõ tiêu đề")
            val thumbnail = json.optString("thumbnail", "")
            val uploader = json.optString("uploader", "Không rõ tác giả")
            val duration = json.optLong("duration", 0L)
            val platform = json.optString("platform", "media")

            val videoOptions = mutableListOf<FormatOption>()
            val rawVideoArray = json.optJSONArray("video_options")
            if (rawVideoArray != null) {
                for (i in 0 until rawVideoArray.length()) {
                    val obj = rawVideoArray.getJSONObject(i)
                    videoOptions.add(
                        FormatOption(
                            formatId = obj.optString("format_id"),
                            label = obj.optString("resolution", "MP4"),
                            resolution = obj.optString("resolution", "720p"),
                            height = obj.optInt("height", 0),
                            width = obj.optInt("width", 0),
                            ext = obj.optString("ext", "mp4"),
                            filesize = obj.optLong("filesize", 0L),
                            isVideo = true,
                            needsMerge = obj.optBoolean("needs_merge", false)
                        )
                    )
                }
            }

            val audioOptions = mutableListOf<FormatOption>()
            val rawAudioArray = json.optJSONArray("audio_options")
            if (rawAudioArray != null) {
                for (i in 0 until rawAudioArray.length()) {
                    val obj = rawAudioArray.getJSONObject(i)
                    audioOptions.add(
                        FormatOption(
                            formatId = obj.optString("format_id"),
                            label = "Âm thanh gốc (${obj.optString("ext").uppercase()})",
                            resolution = "",
                            ext = obj.optString("ext", "m4a"),
                            filesize = obj.optLong("filesize", 0L),
                            isVideo = false,
                            needsMerge = false,
                            bitrateKbps = obj.optInt("abr", 128),
                            isOriginalAudio = true
                        )
                    )
                }
            }

            // Always add MP3 bitrate choices (128, 192, 256, 320 kbps as required by FR-14)
            val baseAudioFormatId = audioOptions.firstOrNull()?.formatId?.takeIf { it.startsWith("http") } ?: "bestaudio"
            val mp3Bitrates = listOf(128, 192, 256, 320)
            mp3Bitrates.forEach { br ->
                audioOptions.add(
                    FormatOption(
                        formatId = baseAudioFormatId,
                        label = "MP3 (${br} kbps)",
                        resolution = "",
                        ext = "mp3",
                        filesize = 0L,
                        isVideo = false,
                        needsMerge = false,
                        bitrateKbps = br,
                        isOriginalAudio = false
                    )
                )
            }

            // Parse image options if available (e.g. TikTok Photo Slides or platform images)
            val imageOptions = mutableListOf<FormatOption>()
            val rawImageArray = json.optJSONArray("image_options")
            if (rawImageArray != null) {
                for (i in 0 until rawImageArray.length()) {
                    val obj = rawImageArray.getJSONObject(i)
                    imageOptions.add(
                        FormatOption(
                            formatId = obj.optString("format_id"),
                            label = obj.optString("label", "Ảnh ${i + 1}"),
                            resolution = obj.optString("resolution", "Gốc"),
                            height = obj.optInt("height", 0),
                            width = obj.optInt("width", 0),
                            ext = obj.optString("ext", "jpg"),
                            filesize = obj.optLong("filesize", 0L),
                            isVideo = false,
                            isImage = true,
                            mimeType = obj.optString("mime_type", "image/jpeg")
                        )
                    )
                }
            }

            val isImageOnly = json.optBoolean("is_image_only", false) || (videoOptions.isEmpty() && imageOptions.isNotEmpty())
            val isAudioOnly = json.optBoolean("is_audio_only", false) || (videoOptions.isEmpty() && audioOptions.isNotEmpty() && !isImageOnly)
            val postId = json.optString("id", "")

            val metadata = MediaMetadata(
                url = url,
                title = title,
                thumbnail = thumbnail,
                uploader = uploader,
                durationSeconds = duration,
                platform = platform,
                videoOptions = videoOptions,
                audioOptions = audioOptions,
                imageOptions = imageOptions,
                isImageOnly = isImageOnly,
                isAudioOnly = isAudioOnly,
                postId = postId
            )

            Result.success(metadata)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadStream(
        url: String,
        formatSpec: String,
        tempOutputPath: String,
        listener: DownloadProgressListener
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonStr = module.callAttr(
                "download_stream",
                url,
                formatSpec,
                tempOutputPath,
                listener
            ).toString()

            val json = JSONObject(jsonStr)
            if (json.optBoolean("success", false)) {
                Result.success(json.optString("output_path", tempOutputPath))
            } else {
                val isCancelled = json.optBoolean("cancelled", false)
                val error = json.optString("error", "Lỗi tải dữ liệu")
                if (isCancelled) {
                    Result.failure(Exception("DOWNLOAD_CANCELLED"))
                } else {
                    Result.failure(Exception(error))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
