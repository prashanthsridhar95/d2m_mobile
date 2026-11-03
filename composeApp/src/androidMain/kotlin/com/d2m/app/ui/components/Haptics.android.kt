package com.d2m.app.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

private class AndroidHaptics(private val view: View) : D2MHaptics {
    private fun perform(constant: Int) {
        runCatching { view.performHapticFeedback(constant) }
    }

    override fun light() = perform(
        if (Build.VERSION.SDK_INT >= 27) HapticFeedbackConstants.CLOCK_TICK else HapticFeedbackConstants.KEYBOARD_TAP,
    )

    override fun medium() = perform(
        if (Build.VERSION.SDK_INT >= 23) HapticFeedbackConstants.CONTEXT_CLICK else HapticFeedbackConstants.LONG_PRESS,
    )

    override fun success() = perform(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS,
    )
}

@Composable
actual fun rememberHaptics(): D2MHaptics {
    val view = LocalView.current
    return remember(view) { AndroidHaptics(view) }
}
