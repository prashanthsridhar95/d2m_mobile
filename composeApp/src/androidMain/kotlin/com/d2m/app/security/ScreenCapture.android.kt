package com.d2m.app.security

// Real screenshot/recording prevention on Android is FLAG_SECURE, set
// directly in MainActivity.onCreate (it needs the Activity's actual
// Window, which nothing in commonMain has a handle to) -- there's nothing
// left to observe here, so every function below is a permanent no-op.
// They exist purely so App.kt (commonMain) can call one shared set of
// function names on both platforms without an `if (platform == ios)`
// branch at every call site.

actual fun isScreenBeingCaptured(): Boolean = false

actual fun observeScreenCapture(onChanged: (Boolean) -> Unit): () -> Unit = {}

actual fun observeScreenshotTaken(onScreenshot: () -> Unit): () -> Unit = {}

actual fun observeAppBackgrounded(onChanged: (Boolean) -> Unit): () -> Unit = {}
