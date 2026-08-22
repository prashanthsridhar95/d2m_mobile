package com.d2m.app.messaging.ui

import coil3.ImageLoader
import coil3.PlatformContext

// No GIF-specific decoder registered -- coil-gif is Android-only (see
// GifImageLoader.kt's doc comment). A GIF renders its static first frame
// here rather than animating; same "no real iOS verification yet" scope
// boundary as this app's other iOS actuals (CryptoProvider.ios.kt,
// WebRtcEngine.ios.kt).
actual fun createGifImageLoader(): ImageLoader = ImageLoader.Builder(PlatformContext.INSTANCE).build()
