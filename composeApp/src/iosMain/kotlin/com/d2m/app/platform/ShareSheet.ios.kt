package com.d2m.app.platform

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual fun shareText(text: String) {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    var top = root
    while (top.presentedViewController != null) top = top.presentedViewController!!
    top.presentViewController(UIActivityViewController(activityItems = listOf(text), applicationActivities = null), animated = true, completion = null)
}
