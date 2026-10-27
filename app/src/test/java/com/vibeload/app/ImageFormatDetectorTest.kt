package com.vibeload.app

import com.vibeload.app.data.local.entity.HistoryEntity
import com.vibeload.app.data.local.entity.isImage
import com.vibeload.app.data.local.entity.isVideo
import com.vibeload.app.platform.image.ImageFormat
import com.vibeload.app.platform.image.ImageFormatDetector
import org.junit.Assert.*
import org.junit.Test

class ImageFormatDetectorTest {

    @Test
    fun detectFormat_detectsJpeg() {
        val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10)
        assertEquals(ImageFormat.JPEG, ImageFormatDetector.detectFormat(jpegHeader))
    }

    @Test
    fun detectFormat_detectsPng() {
        val pngHeader = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00)
        assertEquals(ImageFormat.PNG, ImageFormatDetector.detectFormat(pngHeader))
    }

    @Test
    fun detectFormat_detectsGif() {
        val gif89a = "GIF89a".toByteArray(Charsets.US_ASCII) + ByteArray(10)
        assertEquals(ImageFormat.GIF, ImageFormatDetector.detectFormat(gif89a))

        val gif87a = "GIF87a".toByteArray(Charsets.US_ASCII) + ByteArray(10)
        assertEquals(ImageFormat.GIF, ImageFormatDetector.detectFormat(gif87a))
    }

    @Test
    fun detectFormat_detectsWebp() {
        val webpHeader = "RIFF".toByteArray(Charsets.US_ASCII) + byteArrayOf(0x20, 0x00, 0x00, 0x00) + "WEBP".toByteArray(Charsets.US_ASCII)
        assertEquals(ImageFormat.WEBP, ImageFormatDetector.detectFormat(webpHeader))
    }

    @Test
    fun detectFormat_detectsAvif() {
        val avifHeader = byteArrayOf(0x00, 0x00, 0x00, 0x1C) + "ftypavif".toByteArray(Charsets.US_ASCII) + ByteArray(10)
        assertEquals(ImageFormat.AVIF, ImageFormatDetector.detectFormat(avifHeader))
    }

    @Test
    fun detectFormat_detectsBmp() {
        val bmpHeader = byteArrayOf(0x42, 0x4D, 0x36, 0x00, 0x0C, 0x00, 0x00, 0x00)
        assertEquals(ImageFormat.BMP, ImageFormatDetector.detectFormat(bmpHeader))
    }

    @Test
    fun detectFormat_detectsTiff() {
        val tiffLE = byteArrayOf(0x49, 0x49, 0x2A, 0x00, 0x08, 0x00, 0x00, 0x00)
        assertEquals(ImageFormat.TIFF, ImageFormatDetector.detectFormat(tiffLE))

        val tiffBE = byteArrayOf(0x4D, 0x4D, 0x00, 0x2A, 0x00, 0x00, 0x00, 0x08)
        assertEquals(ImageFormat.TIFF, ImageFormatDetector.detectFormat(tiffBE))
    }

    @Test
    fun detectFormat_detectsHeic() {
        val heicHeader = byteArrayOf(0x00, 0x00, 0x00, 0x18) + "ftypheic".toByteArray(Charsets.US_ASCII) + ByteArray(8)
        assertEquals(ImageFormat.HEIC, ImageFormatDetector.detectFormat(heicHeader))
    }

    @Test
    fun detectFormat_detectsSvg() {
        val svgStandard = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\"><circle/></svg>".toByteArray(Charsets.UTF_8)
        assertEquals(ImageFormat.SVG, ImageFormatDetector.detectFormat(svgStandard))

        val svgWithXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<svg viewBox=\"0 0 100 100\"></svg>".toByteArray(Charsets.UTF_8)
        assertEquals(ImageFormat.SVG, ImageFormatDetector.detectFormat(svgWithXml))
    }

    @Test
    fun detectFormat_detectsIco() {
        val icoHeader = byteArrayOf(0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x20, 0x20)
        assertEquals(ImageFormat.ICO, ImageFormatDetector.detectFormat(icoHeader))
    }

    @Test
    fun isTextOrHtmlOrJson_correctlyIdentifiesNonImages() {
        val htmlPage = "<!DOCTYPE html><html><body><h1>404 Not Found</h1></body></html>".toByteArray(Charsets.UTF_8)
        assertTrue(ImageFormatDetector.isTextOrHtmlOrJson(htmlPage))

        val jsonError = "{\"error\": \"Unauthorized\", \"message\": \"Access denied\", \"code\": 403}".toByteArray(Charsets.UTF_8)
        assertTrue(ImageFormatDetector.isTextOrHtmlOrJson(jsonError))

        val plainError = "Error 500: Internal server failure while retrieving object.".toByteArray(Charsets.UTF_8)
        assertTrue(ImageFormatDetector.isTextOrHtmlOrJson(plainError))

        val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        assertFalse(ImageFormatDetector.isTextOrHtmlOrJson(jpegHeader))
    }

    @Test
    fun fromExtension_resolvesAllSupportedExtensions() {
        assertEquals(ImageFormat.JPEG, ImageFormat.fromExtension("jpg"))
        assertEquals(ImageFormat.JPEG, ImageFormat.fromExtension("jpeg"))
        assertEquals(ImageFormat.PNG, ImageFormat.fromExtension("png"))
        assertEquals(ImageFormat.WEBP, ImageFormat.fromExtension("webp"))
        assertEquals(ImageFormat.GIF, ImageFormat.fromExtension("gif"))
        assertEquals(ImageFormat.AVIF, ImageFormat.fromExtension("avif"))
        assertEquals(ImageFormat.BMP, ImageFormat.fromExtension("bmp"))
        assertEquals(ImageFormat.TIFF, ImageFormat.fromExtension("tiff"))
        assertEquals(ImageFormat.TIFF, ImageFormat.fromExtension("tif"))
        assertEquals(ImageFormat.HEIC, ImageFormat.fromExtension("heic"))
        assertEquals(ImageFormat.HEIC, ImageFormat.fromExtension("heif"))
        assertEquals(ImageFormat.SVG, ImageFormat.fromExtension("svg"))
        assertEquals(ImageFormat.ICO, ImageFormat.fromExtension("ico"))
        assertNull(ImageFormat.fromExtension("exe"))
        assertNull(ImageFormat.fromExtension("mp4"))
    }

    @Test
    fun fromMimeType_resolvesCorrectFormats() {
        assertEquals(ImageFormat.JPEG, ImageFormat.fromMimeType("image/jpeg"))
        assertEquals(ImageFormat.PNG, ImageFormat.fromMimeType("image/png"))
        assertEquals(ImageFormat.WEBP, ImageFormat.fromMimeType("image/webp"))
        assertEquals(ImageFormat.GIF, ImageFormat.fromMimeType("image/gif"))
        assertEquals(ImageFormat.AVIF, ImageFormat.fromMimeType("image/avif"))
        assertEquals(ImageFormat.BMP, ImageFormat.fromMimeType("image/bmp"))
        assertEquals(ImageFormat.TIFF, ImageFormat.fromMimeType("image/tiff"))
        assertEquals(ImageFormat.HEIC, ImageFormat.fromMimeType("image/heic"))
        assertEquals(ImageFormat.SVG, ImageFormat.fromMimeType("image/svg+xml"))
        assertEquals(ImageFormat.ICO, ImageFormat.fromMimeType("image/x-icon"))
        assertNull(ImageFormat.fromMimeType("video/mp4"))
        assertNull(ImageFormat.fromMimeType("audio/mpeg"))
        assertNull(ImageFormat.fromMimeType("text/html"))
    }

    @Test
    fun historyEntity_extensionPropertiesRecognizeImageCorrectly() {
        val imageRecord = HistoryEntity(
            id = "test-img-1",
            title = "Sample Sunset",
            originalUrl = "https://example.com/sunset.jpg",
            mediaUriString = "content://media/external/images/media/100",
            relativePath = "Pictures/VibeLoad",
            fileName = "Sample Sunset.jpg",
            mimeType = "image/jpeg",
            isAudio = false,
            fileSize = 1024 * 500,
            durationSeconds = 0,
            thumbnailUri = "content://media/external/images/media/100"
        )
        assertTrue(imageRecord.isImage)
        assertFalse(imageRecord.isVideo)
        assertFalse(imageRecord.isAudio)

        val videoRecord = HistoryEntity(
            id = "test-vid-1",
            title = "Sample Video",
            originalUrl = "https://example.com/video.mp4",
            mediaUriString = "content://media/external/video/media/200",
            relativePath = "Movies/VibeLoad",
            fileName = "Sample Video.mp4",
            mimeType = "video/mp4",
            isAudio = false,
            fileSize = 1024 * 1024 * 10,
            durationSeconds = 120,
            thumbnailUri = "https://example.com/thumb.jpg"
        )
        assertFalse(videoRecord.isImage)
        assertTrue(videoRecord.isVideo)
        assertFalse(videoRecord.isAudio)

        val audioRecord = HistoryEntity(
            id = "test-aud-1",
            title = "Sample Song",
            originalUrl = "https://example.com/song.mp3",
            mediaUriString = "content://media/external/audio/media/300",
            relativePath = "Music/VibeLoad",
            fileName = "Sample Song.mp3",
            mimeType = "audio/mpeg",
            isAudio = true,
            fileSize = 1024 * 1024 * 3,
            durationSeconds = 180,
            thumbnailUri = "https://example.com/cover.jpg"
        )
        assertFalse(audioRecord.isImage)
        assertFalse(audioRecord.isVideo)
        assertTrue(audioRecord.isAudio)
    }
}
