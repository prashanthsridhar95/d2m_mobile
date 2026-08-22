package com.d2m.app.messaging.ui

/**
 * GIPHY API key for [GifPicker] -- Android reads the real per-deployment key
 * injected at build time from `local.properties` (gitignored, see
 * composeApp/build.gradle.kts's `giphyApiKey` doc comment); iOS (no real
 * target for this build yet, same precedent as CryptoProvider/WebRtcEngine)
 * falls back to GIPHY's own public "beta" key, which is intentionally public
 * and rate-limited-but-shared, not a secret -- same fallback d2m_web's own
 * GifPicker.jsx hardcodes.
 */
expect fun giphyApiKey(): String
