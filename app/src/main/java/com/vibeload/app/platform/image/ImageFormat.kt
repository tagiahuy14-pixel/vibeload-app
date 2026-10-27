package com.vibeload.app.platform.image

enum class ImageFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String
) {
    JPEG("jpg", "image/jpeg", "JPEG"),
    PNG("png", "image/png", "PNG"),
    WEBP("webp", "image/webp", "WebP"),
    GIF("gif", "image/gif", "GIF"),
    AVIF("avif", "image/avif", "AVIF"),
    BMP("bmp", "image/bmp", "BMP"),
    TIFF("tiff", "image/tiff", "TIFF"),
    HEIC("heic", "image/heic", "HEIC"),
    SVG("svg", "image/svg+xml", "SVG"),
    ICO("ico", "image/x-icon", "ICO"),
    UNKNOWN("bin", "application/octet-stream", "Không xác định");

    companion object {
        fun fromExtension(ext: String): ImageFormat? {
            val clean = ext.removePrefix(".").lowercase().trim()
            return when (clean) {
                "jpg", "jpeg" -> JPEG
                "png" -> PNG
                "webp" -> WEBP
                "gif" -> GIF
                "avif" -> AVIF
                "bmp" -> BMP
                "tiff", "tif" -> TIFF
                "heic", "heif" -> HEIC
                "svg" -> SVG
                "ico" -> ICO
                else -> null
            }
        }

        fun fromMimeType(mime: String): ImageFormat? {
            val clean = mime.lowercase().split(";")[0].trim()
            return when (clean) {
                "image/jpeg", "image/jpg", "image/pjpeg" -> JPEG
                "image/png", "image/x-png" -> PNG
                "image/webp" -> WEBP
                "image/gif" -> GIF
                "image/avif" -> AVIF
                "image/bmp", "image/x-ms-bmp" -> BMP
                "image/tiff", "image/x-tiff" -> TIFF
                "image/heic", "image/heif", "image/heic-sequence", "image/heif-sequence" -> HEIC
                "image/svg+xml", "image/svg" -> SVG
                "image/x-icon", "image/vnd.microsoft.icon", "image/ico" -> ICO
                else -> null
            }
        }
    }
}
