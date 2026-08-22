package com.d2m.app.ui.components

import androidx.compose.runtime.Composable

// No-op -- see BackHandlerCompat.kt's doc comment. iOS has no real target to
// verify a back-gesture story against yet (same reasoning as
// WebRtcEngine.ios.kt/CryptoProvider.ios.kt); this app doesn't own UIKit's
// swipe-back gesture at this layer.
@Composable
actual fun BackHandlerCompat(enabled: Boolean, onBack: () -> Unit) {
}
