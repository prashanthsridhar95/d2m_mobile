package com.d2m.app.platform

import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

actual fun systemReduceMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()
