package com.d2m.app.security

/**
 * True while the screen is actively being recorded, mirrored (AirPlay), or
 * otherwise captured -- iOS's UIScreen.isCaptured. Always false on
 * Android: FLAG_SECURE (set in MainActivity.onCreate) blocks screen
 * capture at the OS level outright, so there's nothing to detect there --
 * Apple provides no equivalent blocking API for iOS, only this
 * after-the-fact/live signal. See App.kt's ScreenCaptureCover for the
 * live-recording response, and WatermarkOverlay.kt for this app's actual
 * cross-platform leak defense (baked into rendered pixels, survives any
 * capture method including one FLAG_SECURE/this can't touch: someone
 * photographing the screen with a second device).
 */
expect fun isScreenBeingCaptured(): Boolean

/**
 * Observes screen-recording start/stop. [onChanged] fires with the new
 * captured state, including once immediately with the current state.
 * Returns a function that removes the observer -- call it from a
 * DisposableEffect/LaunchedEffect's cleanup. A no-op registration on
 * Android (the callback never fires) for the same reason as
 * [isScreenBeingCaptured] above.
 */
expect fun observeScreenCapture(onChanged: (Boolean) -> Unit): () -> Unit

/**
 * Observes the OS screenshot event. Apple gives apps no way to block or
 * even intercept the screenshot itself -- only this notification, fired
 * immediately AFTER the OS already took and saved it to Photos. App.kt
 * uses this to log a forensic audit ping (AuditApi.kt), not to prevent
 * anything -- there's nothing left to prevent by the time this fires. A
 * no-op registration on Android, where FLAG_SECURE prevents the
 * screenshot from being captured at all -- there's nothing to react to.
 */
expect fun observeScreenshotTaken(onScreenshot: () -> Unit): () -> Unit

/**
 * Observes the app entering/leaving the background. [onChanged] fires
 * `true` the instant the app is about to resign active (before iOS takes
 * its app-switcher snapshot) and `false` once it's active again. App.kt
 * uses this to cover the content during that window -- unlike
 * [observeScreenshotTaken], this one genuinely prevents a leak rather than
 * just logging one after the fact: the app-switcher preview is a real,
 * commonly-exploited leak of sensitive content (the standard reason
 * banking/health apps blank themselves on backgrounding), and iOS gives
 * apps just enough notice to hide content before that snapshot is taken.
 * A no-op registration on Android, where FLAG_SECURE already blanks the
 * recents-list thumbnail automatically at the OS level -- there's nothing
 * left for this app to do there.
 */
expect fun observeAppBackgrounded(onChanged: (Boolean) -> Unit): () -> Unit
