package com.vibeload.app.platform.image

import java.io.File
import java.io.FileInputStream

object ImageFormatDetector {

    /**
     * Inspect raw bytes (at least 12-512 bytes recommended) and determine true image format.
     */
    fun detectFormat(bytes: ByteArray): ImageFormat {
        if (bytes.size < 2) return ImageFormat.UNKNOWN

        // 1. JPEG: starts with FF D8 FF
        if (bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()
        ) {
            return ImageFormat.JPEG
        }

        // 2. PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() && // 'P'
            bytes[2] == 0x4E.toByte() && // 'N'
            bytes[3] == 0x47.toByte() && // 'G'
            bytes[4] == 0x0D.toByte() &&
            bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() &&
            bytes[7] == 0x0A.toByte()
        ) {
            return ImageFormat.PNG
        }

        // 3. GIF: GIF87a or GIF89a
        if (bytes.size >= 6 &&
            bytes[0] == 'G'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == '8'.code.toByte() &&
            (bytes[4] == '7'.code.toByte() || bytes[4] == '9'.code.toByte()) &&
            bytes[5] == 'a'.code.toByte()
        ) {
            return ImageFormat.GIF
        }

        // 4. WebP: RIFF....WEBP (12 bytes)
        if (bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() &&
            bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() &&
            bytes[11] == 'P'.code.toByte()
        ) {
            return ImageFormat.WEBP
        }

        // 5. BMP: 'B' 'M'
        if (bytes.size >= 2 &&
            bytes[0] == 'B'.code.toByte() &&
            bytes[1] == 'M'.code.toByte()
        ) {
            return ImageFormat.BMP
        }

        // 6. TIFF: "II*\0" (0x49 0x49 0x2A 0x00) or "MM\0*" (0x4D 0x4D 0x00 0x2A)
        if (bytes.size >= 4) {
            if (bytes[0] == 0x49.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x2A.toByte() && bytes[3] == 0x00.toByte()) {
                return ImageFormat.TIFF
            }
            if (bytes[0] == 0x4D.toByte() && bytes[1] == 0x4D.toByte() && bytes[2] == 0x00.toByte() && bytes[3] == 0x2A.toByte()) {
                return ImageFormat.TIFF
            }
        }

        // 7. ICO: 00 00 01 00 (icon) or 00 00 02 00 (cursor)
        if (bytes.size >= 4 &&
            bytes[0] == 0x00.toByte() &&
            bytes[1] == 0x00.toByte() &&
            (bytes[2] == 0x01.toByte() || bytes[2] == 0x02.toByte()) &&
            bytes[3] == 0x00.toByte()
        ) {
            return ImageFormat.ICO
        }

        // 8. AVIF / HEIC (ISO Base Media File Format: starts at offset 4 with "ftyp")
        if (bytes.size >= 12 &&
            bytes[4] == 'f'.code.toByte() &&
            bytes[5] == 't'.code.toByte() &&
            bytes[6] == 'y'.code.toByte() &&
            bytes[7] == 'p'.code.toByte()
        ) {
            val brand = String(bytes, 8, 4, Charsets.US_ASCII).lowercase()
            if (brand == "avif" || brand == "avis") {
                return ImageFormat.AVIF
            }
            if (brand in setOf("heic", "heix", "hevc", "heim", "heis", "mif1", "msf1")) {
                return ImageFormat.HEIC
            }
        }

        // 9. SVG: text-based XML containing <svg
        val sampleLen = minOf(bytes.size, 1024)
        val headStr = String(bytes, 0, sampleLen, Charsets.UTF_8).lowercase().trim()
        if (headStr.startsWith("<?xml") || headStr.startsWith("<svg") || headStr.contains("<svg")) {
            // Verify it's not an HTML webpage containing SVG icons
            if (!headStr.contains("<!doctype html") && !headStr.contains("<html") && !headStr.contains("<body")) {
                return ImageFormat.SVG
            }
        }

        return ImageFormat.UNKNOWN
    }

    /**
     * Check if a file contains valid image magic bytes.
     */
    fun detectFormat(file: File): ImageFormat {
        if (!file.exists() || file.length() == 0L) return ImageFormat.UNKNOWN
        val buffer = ByteArray(1024)
        val read = FileInputStream(file).use { it.read(buffer) }
        if (read <= 0) return ImageFormat.UNKNOWN
        return detectFormat(buffer.copyOf(read))
    }

    /**
     * Detect if data is text/HTML/JSON error rather than binary/vector image data.
     */
    fun isTextOrHtmlOrJson(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val sampleLen = minOf(bytes.size, 512)
        val head = String(bytes, 0, sampleLen, Charsets.UTF_8).lowercase().trim()

        // HTML, XML or JSON markers
        if (head.startsWith("<!doctype html") ||
            head.startsWith("<html") ||
            head.startsWith("{\"") ||
            head.startsWith("{\n") ||
            head.startsWith("{ \"") ||
            head.startsWith("[{") ||
            head.startsWith("error") ||
            (head.contains("<head") && head.contains("<body")) ||
            head.contains("404 not found") ||
            head.contains("access denied") ||
            head.contains("internal server error")
        ) {
            return true
        }

        // SVG vector images are valid text-based XML
        if (head.contains("<svg") || head.startsWith("<?xml")) {
            return false
        }

        // Check if sample is plain text (ASCII printable characters) rather than binary image data
        if (sampleLen >= 16) {
            val printableCount = bytes.take(sampleLen).count {
                (it in 32..126) || it == 10.toByte() || it == 13.toByte() || it == 9.toByte()
            }
            if ((printableCount.toFloat() / sampleLen) > 0.95f) {
                return true
            }
        }

        return false
    }
}
