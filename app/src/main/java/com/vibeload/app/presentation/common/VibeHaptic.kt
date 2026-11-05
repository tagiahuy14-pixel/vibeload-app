package com.vibeload.app.presentation.common

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

object VibeHaptic {
    fun click(view: View) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}
    }

    fun tab(view: View) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (_: Exception) {}
    }

    fun success(view: View) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        } catch (_: Exception) {}
    }
}
