package com.d2m.app.messaging.ui

// No BuildConfig-equivalent secret injection set up for iOS yet (not a real
// target for this build -- same precedent as CryptoProvider.ios.kt/
// WebRtcEngine.ios.kt). GIPHY's public "beta" key is intentionally public
// and rate-limited-but-shared, not a secret, so hardcoding it here (rather
// than throwing NotImplementedError like those other iOS stubs) is safe and
// keeps the GIF picker at least functional if this ever runs on iOS.
actual fun giphyApiKey(): String = "dc6zaTOxFJmzC"
