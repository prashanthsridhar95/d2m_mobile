package com.d2m.app.messaging.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * In-app video playback surface for MediaViewerDialog.kt -- "provide a image
 * & video viewer - shouldnt be going outside the app." Android gets a real
 * player (ExoPlayer + PlayerView, see the .android.kt actual); iOS is a
 * no-op stub for now, same established precedent as every other
 * platform-native feature in this app that has no iOS target to actually
 * run in yet (WebRtcEngine.ios.kt, SoundEffects.ios.kt, GifImageLoader.kt).
 *
 * `autoPlay` starts playback immediately (the viewer's whole reason for
 * existing -- tapping a video bubble should start watching it, not require
 * a second tap once the dialog opens).
 */
@Composable
expect fun VideoPlayerView(url: String, modifier: Modifier, autoPlay: Boolean = true)
