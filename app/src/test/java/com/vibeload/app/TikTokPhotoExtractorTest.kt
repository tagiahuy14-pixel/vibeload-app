package com.vibeload.app

import com.vibeload.app.domain.model.DownloadStatus
import com.vibeload.app.domain.model.DownloadTask
import com.vibeload.app.domain.model.FormatOption
import com.vibeload.app.domain.model.MediaMetadata
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class TikTokPhotoExtractorTest {

    @Test
    fun parseTikTokPhotoAlbum_correctlyIdentifiesImagesAndOrder() {
        // Simulated TikTok photo album payload with 3 slides and music
        val jsonStr = """
        {
            "success": true,
            "id": "7478990063938833682",
            "url": "https://www.tiktok.com/@creator/photo/7478990063938833682",
            "title": "Bộ sưu tập ảnh hoàng hôn",
            "thumbnail": "https://p16-sign-sg.tiktokcdn.com/img1.jpeg",
            "uploader": "creator_vn",
            "duration": 0,
            "platform": "tiktok",
            "video_options": [],
            "audio_options": [
                {
                    "format_id": "https://sf16-ies-music-sg.tiktokcdn.com/music.mp3",
                    "ext": "mp3",
                    "abr": 128,
                    "filesize": 1048576,
                    "acodec": "mp3"
                }
            ],
            "image_options": [
                {
                    "format_id": "https://p16-sign-sg.tiktokcdn.com/img1.jpeg",
                    "label": "Ảnh 1/3",
                    "ext": "jpg",
                    "resolution": "Ảnh 1",
                    "filesize": 524288,
                    "is_image": true,
                    "mime_type": "image/jpeg"
                },
                {
                    "format_id": "https://p16-sign-sg.tiktokcdn.com/img2.jpeg",
                    "label": "Ảnh 2/3",
                    "ext": "jpg",
                    "resolution": "Ảnh 2",
                    "filesize": 629145,
                    "is_image": true,
                    "mime_type": "image/jpeg"
                },
                {
                    "format_id": "https://p16-sign-sg.tiktokcdn.com/img3.jpeg",
                    "label": "Ảnh 3/3",
                    "ext": "jpg",
                    "resolution": "Ảnh 3",
                    "filesize": 734003,
                    "is_image": true,
                    "mime_type": "image/jpeg"
                }
            ],
            "is_image_only": true
        }
        """.trimIndent()

        val json = JSONObject(jsonStr)
        assertTrue(json.getBoolean("success"))
        assertEquals("7478990063938833682", json.getString("id"))
        assertEquals("tiktok", json.getString("platform"))
        assertTrue(json.getBoolean("is_image_only"))

        val imageArray = json.getJSONArray("image_options")
        assertEquals(3, imageArray.length())

        // Verify order and labels
        for (i in 0 until imageArray.length()) {
            val img = imageArray.getJSONObject(i)
            assertEquals("Ảnh ${i + 1}/3", img.getString("label"))
            assertEquals("jpg", img.getString("ext"))
            assertTrue(img.getString("format_id").startsWith("https://"))
        }

        // Verify audio option exists for background music
        val audioArray = json.getJSONArray("audio_options")
        assertEquals(1, audioArray.length())
        assertEquals("mp3", audioArray.getJSONObject(0).getString("ext"))
    }

    @Test
    fun downloadTask_formatsBatchProgressProperly() {
        val task = DownloadTask(
            id = "task-123",
            url = "https://www.tiktok.com/@creator/photo/123",
            title = "TikTok Photo Album",
            outputFilename = "tiktok_album",
            selectedFormat = FormatOption(
                formatId = "ALL_IMAGES",
                label = "Tải tất cả (8 ảnh)",
                isImage = true,
                isVideo = false
            ),
            status = DownloadStatus.DOWNLOADING,
            progress = 37.5f,
            currentItemIndex = 3,
            totalItemCount = 8
        )

        assertEquals("3/8 ảnh", task.getFormattedSizeProgress())
        assertEquals(3, task.currentItemIndex)
        assertEquals(8, task.totalItemCount)
    }

    @Test
    fun downloadTask_formatsSingleItemProperly() {
        val singleTask = DownloadTask(
            id = "task-456",
            url = "https://example.com/single.jpg",
            title = "Single Image",
            outputFilename = "single.jpg",
            selectedFormat = FormatOption(
                formatId = "img_jpg",
                label = "Ảnh JPG",
                isImage = true,
                isVideo = false
            ),
            status = DownloadStatus.DOWNLOADING,
            progress = 50f,
            downloadedBytes = 1024 * 1024L,
            totalBytes = 2 * 1024 * 1024L,
            currentItemIndex = 1,
            totalItemCount = 1
        )

        assertEquals("1.0 MB / 2.0 MB", singleTask.getFormattedSizeProgress())
    }

    @Test
    fun mediaMetadata_durationForImageOnlyIsZero() {
        val meta = MediaMetadata(
            url = "https://www.tiktok.com/@user/photo/123",
            title = "Photo Slide",
            thumbnail = "https://example.com/thumb.jpg",
            uploader = "User",
            durationSeconds = 0,
            platform = "tiktok",
            videoOptions = emptyList(),
            audioOptions = emptyList(),
            imageOptions = listOf(
                FormatOption(formatId = "1", label = "Ảnh 1", isImage = true)
            ),
            isImageOnly = true,
            postId = "123"
        )

        assertEquals("--:--", meta.getFormattedDuration())
        assertTrue(meta.isImageOnly)
        assertEquals("123", meta.postId)
    }

    @Test
    fun multiImageSelection_correctlyFiltersTargetUrls() {
        val allImages = listOf(
            FormatOption(formatId = "https://cdn.com/1.jpg", label = "Ảnh 1", isImage = true),
            FormatOption(formatId = "https://cdn.com/2.jpg", label = "Ảnh 2", isImage = true),
            FormatOption(formatId = "https://cdn.com/3.jpg", label = "Ảnh 3", isImage = true),
            FormatOption(formatId = "https://cdn.com/4.jpg", label = "Ảnh 4", isImage = true),
            FormatOption(formatId = "https://cdn.com/5.jpg", label = "Ảnh 5", isImage = true)
        )

        // User selects images 1, 3, 5 (indices 0, 2, 4)
        val selectedIndices = setOf(0, 2, 4)
        val selectedUrls = selectedIndices.mapNotNull { allImages.getOrNull(it)?.formatId }

        assertEquals(3, selectedUrls.size)
        assertEquals(listOf("https://cdn.com/1.jpg", "https://cdn.com/3.jpg", "https://cdn.com/5.jpg"), selectedUrls)

        val task = DownloadTask(
            id = "task_test",
            url = "https://www.tiktok.com/@creator/photo/123",
            title = "Test Album",
            outputFilename = "Test_Album",
            selectedFormat = FormatOption(formatId = "ALL_IMAGES", label = "Tải tất cả (5 ảnh)", isImage = true),
            selectedImageUrls = selectedUrls,
            totalItemCount = selectedUrls.size
        )

        assertEquals(3, task.selectedImageUrls.size)
        assertEquals(3, task.totalItemCount)
    }

    @Test
    fun audioOnlyCandidate_correctlyDetected() {
        val audioUrl = "https://www.tiktok.com/music/original-sound-7187900768910413098"
        val isAudioOnlyCandidate = audioUrl.lowercase().run {
            contains(".mp3") || contains(".wav") || contains(".m4a") || contains(".flac") || contains(".aac") ||
            contains("soundcloud.com") || contains("/music/") || contains("audio")
        }
        assertTrue(isAudioOnlyCandidate)
    }
}
