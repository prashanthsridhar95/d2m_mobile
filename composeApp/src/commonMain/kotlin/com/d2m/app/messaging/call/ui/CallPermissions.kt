package com.d2m.app.messaging.call.ui

import androidx.compose.runtime.Composable

/**
 * Requests camera+mic (camera only matters for video calls, but requesting
 * both upfront avoids a second prompt mid-call on upgradeToVideo) before
 * starting/accepting a call. Android needs an explicit runtime permission
 * request (API 23+); iOS prompts automatically the first time AVAudioSession/
 * AVCaptureDevice is actually touched, so the iOS actual just calls back
 * immediately with granted=true -- and since calling isn't wired up on iOS
 * yet anyway (see WebRtcEngine.ios.kt), there's nothing there to gate.
 */
@Composable
expect fun rememberCallPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit
