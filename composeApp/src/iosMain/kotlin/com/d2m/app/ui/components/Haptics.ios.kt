package com.d2m.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

@OptIn(ExperimentalForeignApi::class)
private class IosHaptics : D2MHaptics {
    private val lightGen = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val mediumGen = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val notifyGen = UINotificationFeedbackGenerator()

    override fun light() {
        lightGen.impactOccurred()
        lightGen.prepare()
    }

    override fun medium() {
        mediumGen.impactOccurred()
        mediumGen.prepare()
    }

    override fun success() {
        notifyGen.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
        notifyGen.prepare()
    }
}

@Composable
actual fun rememberHaptics(): D2MHaptics = remember { IosHaptics() }
