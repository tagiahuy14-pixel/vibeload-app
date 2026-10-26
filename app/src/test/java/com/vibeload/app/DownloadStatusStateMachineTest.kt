package com.vibeload.app

import com.vibeload.app.domain.model.DownloadStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadStatusStateMachineTest {

    @Test
    fun isActive_returnsTrueOnlyForActiveExecutionStates() {
        assertTrue(DownloadStatus.RESOLVING.isActive())
        assertTrue(DownloadStatus.DOWNLOADING.isActive())
        assertTrue(DownloadStatus.PROCESSING.isActive())
        assertTrue(DownloadStatus.SAVING.isActive())
        assertTrue(DownloadStatus.RETRYING.isActive())

        assertFalse(DownloadStatus.IDLE.isActive())
        assertFalse(DownloadStatus.COMPLETED.isActive())
        assertFalse(DownloadStatus.FAILED.isActive())
        assertFalse(DownloadStatus.PAUSED.isActive())
        assertFalse(DownloadStatus.CANCELLED.isActive())
    }

    @Test
    fun isTerminal_returnsTrueOnlyForFinalStates() {
        assertTrue(DownloadStatus.COMPLETED.isTerminal())
        assertTrue(DownloadStatus.FAILED.isTerminal())
        assertTrue(DownloadStatus.CANCELLED.isTerminal())

        assertFalse(DownloadStatus.IDLE.isTerminal())
        assertFalse(DownloadStatus.RESOLVING.isTerminal())
        assertFalse(DownloadStatus.DOWNLOADING.isTerminal())
        assertFalse(DownloadStatus.PAUSED.isTerminal())
    }
}
