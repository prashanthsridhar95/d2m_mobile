package com.d2m.app.ui.components

import androidx.compose.runtime.Composable

/**
 * Tiny, platform-neutral haptics surface. Keep it this simple on purpose:
 * three intensities cover every interaction in the app.
 *
 *  - [light]   : selection / tab switch / toggle
 *  - [medium]  : a committed action (swipe decision, send)
 *  - [success] : a positive outcome (match, saved)
 *
 * Android maps to View.performHapticFeedback (no VIBRATE permission needed);
 * iOS maps to UIImpactFeedbackGenerator / UINotificationFeedbackGenerator.
 * Calls are cheap and safe from any composition callback; they never throw.
 */
interface D2MHaptics {
    fun light()
    fun medium()
    fun success()
}

/** Remembered, stable haptics handle. Safe to capture in long-lived lambdas. */
@Composable
expect fun rememberHaptics(): D2MHaptics
