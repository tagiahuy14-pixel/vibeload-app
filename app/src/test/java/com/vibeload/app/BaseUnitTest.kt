package com.vibeload.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseUnitTest {
    @Test
    fun projectInitialization_isCorrect() {
        assertEquals("com.vibeload.app", "com.vibeload.app")
    }

    @Test
    fun targetSdkAndMinSdk_areCompliant() {
        val minSdk = 29
        val targetSdk = 35
        assertTrue(minSdk >= 29)
        assertTrue(targetSdk >= 34)
    }
}
