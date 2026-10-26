package com.vibeload.app

import com.vibeload.app.data.repository.MediaStoreStorageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageSanitizationTest {

    @Test
    fun forbiddenCharacters_areReplacedWithUnderscore() {
        val raw = "Video: Title / With * Forbidden ? \"Chars\" <And> | Pipes"
        val sanitized = MediaStoreStorageManager.sanitize(raw, "mp4")

        assertFalse(sanitized.contains(":"))
        assertFalse(sanitized.contains("/"))
        assertFalse(sanitized.contains("*"))
        assertFalse(sanitized.contains("?"))
        assertFalse(sanitized.contains("\""))
        assertFalse(sanitized.contains("<"))
        assertFalse(sanitized.contains(">"))
        assertFalse(sanitized.contains("|"))
        assertTrue(sanitized.endsWith(".mp4"))
    }

    @Test
    fun pathTraversalAttempts_areStripped() {
        val dangerous = "../../../../etc/passwd"
        val sanitized = MediaStoreStorageManager.sanitize(dangerous, "mp4")

        assertFalse(sanitized.contains(".."))
        assertFalse(sanitized.contains("/"))
        assertTrue(sanitized.endsWith(".mp4"))
    }

    @Test
    fun extensionAlreadyPresent_isNotDuplicated() {
        val nameWithExt = "My_Awesome_Song.mp3"
        val sanitized = MediaStoreStorageManager.sanitize(nameWithExt, "mp3")

        assertEquals("My_Awesome_Song.mp3", sanitized)
    }

    @Test
    fun emptyOrOnlySpecialChars_fallsBackToSafeName() {
        val emptyInput = "   "
        val sanitized = MediaStoreStorageManager.sanitize(emptyInput, "mp4")

        assertTrue(sanitized.startsWith("VibeLoad_Media_"))
        assertTrue(sanitized.endsWith(".mp4"))
    }

    @Test
    fun extremelyLongTitle_isTruncatedSafely() {
        val longTitle = "A".repeat(200)
        val sanitized = MediaStoreStorageManager.sanitize(longTitle, "mp4")

        assertTrue(sanitized.length <= 104) // 100 chars + .mp4
        assertTrue(sanitized.endsWith(".mp4"))
    }
}
