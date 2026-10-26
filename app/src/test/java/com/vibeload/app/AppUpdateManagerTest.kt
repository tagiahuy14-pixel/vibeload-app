package com.vibeload.app

import com.vibeload.app.platform.update.AppUpdateManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun isNewerVersion_detectsMajorMinorPatchUpdates() {
        assertTrue(AppUpdateManager.isNewerVersion("1.0.1", "1.0.0"))
        assertTrue(AppUpdateManager.isNewerVersion("1.1.0", "1.0.9"))
        assertTrue(AppUpdateManager.isNewerVersion("2.0.0", "1.9.9"))
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0.1", "1.0.0"))
    }

    @Test
    fun isNewerVersion_returnsFalseForSameOrOlderVersion() {
        assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(AppUpdateManager.isNewerVersion("0.9.9", "1.0.0"))
        assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "1.0.1"))
    }
}
