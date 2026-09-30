package com.d2m.app.security

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationUserDidTakeScreenshotNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.UIKit.UIScreen
import platform.UIKit.UIScreenCapturedDidChangeNotification

actual fun isScreenBeingCaptured(): Boolean = UIScreen.mainScreen.captured

actual fun observeScreenCapture(onChanged: (Boolean) -> Unit): () -> Unit {
    val center = NSNotificationCenter.defaultCenter
    // Fire once immediately with the current state -- a recording that was
    // already in progress before this observer was registered (e.g. the
    // app cold-launched into an active AirPlay mirror) should still cover
    // the content right away, not wait for the NEXT captured-state change.
    onChanged(UIScreen.mainScreen.captured)
    val token = center.addObserverForName(
        name = UIScreenCapturedDidChangeNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { _ ->
        onChanged(UIScreen.mainScreen.captured)
    }
    return { center.removeObserver(token) }
}

actual fun observeScreenshotTaken(onScreenshot: () -> Unit): () -> Unit {
    val center = NSNotificationCenter.defaultCenter
    val token = center.addObserverForName(
        name = UIApplicationUserDidTakeScreenshotNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { _ ->
        onScreenshot()
    }
    return { center.removeObserver(token) }
}

actual fun observeAppBackgrounded(onChanged: (Boolean) -> Unit): () -> Unit {
    val center = NSNotificationCenter.defaultCenter
    // WillResignActive, not DidEnterBackground: the former fires BEFORE
    // the app-switcher snapshot is taken (also on transient interruptions
    // like an incoming call or Control Center, which is fine -- covering
    // content a little too eagerly is harmless, missing the real
    // backgrounding case is not). DidBecomeActive is its exact inverse.
    val resignToken = center.addObserverForName(
        name = UIApplicationWillResignActiveNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { _ ->
        onChanged(true)
    }
    val activeToken = center.addObserverForName(
        name = UIApplicationDidBecomeActiveNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { _ ->
        onChanged(false)
    }
    return {
        center.removeObserver(resignToken)
        center.removeObserver(activeToken)
    }
}
