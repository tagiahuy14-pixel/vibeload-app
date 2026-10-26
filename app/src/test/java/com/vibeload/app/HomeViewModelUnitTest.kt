package com.vibeload.app

import com.vibeload.app.presentation.home.HomeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelUnitTest {

    @Test
    fun isValidUrl_acceptsValidHttpAndHttps() {
        assertTrue(HomeViewModel.isValidUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(HomeViewModel.isValidUrl("http://soundcloud.com/artist/track"))
        assertTrue(HomeViewModel.isValidUrl("https://vt.tiktok.com/ZSjRxxx/"))
    }

    @Test
    fun isValidUrl_rejectsInvalidOrMaliciousSchemes() {
        assertFalse(HomeViewModel.isValidUrl(""))
        assertFalse(HomeViewModel.isValidUrl("ftp://example.com/file.mp4"))
        assertFalse(HomeViewModel.isValidUrl("file:///sdcard/malicious.sh"))
        assertFalse(HomeViewModel.isValidUrl("content://media/external/123"))
        assertFalse(HomeViewModel.isValidUrl("http://127.0.0.1:8080/exploit"))
        assertFalse(HomeViewModel.isValidUrl("http://localhost:3000"))
        assertFalse(HomeViewModel.isValidUrl("random text without protocol"))
    }

    @Test
    fun extractUrlFromText_extractsUrlFromSharedClipboardText() {
        val sharedText = "Check out this song: https://soundcloud.com/sample/track it is awesome!"
        val extracted = HomeViewModel.extractUrlFromText(sharedText)
        assertEquals("https://soundcloud.com/sample/track", extracted)
    }

    @Test
    fun extractUrlFromText_returnsEmptyWhenNoUrlFound() {
        val plainText = "Hello this is a plain message without links"
        val extracted = HomeViewModel.extractUrlFromText(plainText)
        assertEquals("", extracted)
    }
}
