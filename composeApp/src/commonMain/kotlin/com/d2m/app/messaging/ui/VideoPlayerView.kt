package com.d2m.app.messaging.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * In-app video playback surface for MediaViewerDialog.kt -- "provide a image
 * & video viewer - shouldnt be going outside the app." Both platforms have a real player: ExoPlayer + PlayerView on Android,
 * AVPlayer + AVKit's AVPlayerViewController on iOS. Both supply standard
 * transport controls, so neither actual hand-builds a control bar.
 *
 * `autoPlay` starts playback immediately (the viewer's whole reason for
 * existing -- tapping a video bubble should start watching it, not require
 * a second tap once the dialog opens).
 */
@Composable
expect fun VideoPlayerView(url: String, modifier: Modifier, autoPlay: Boolean = true)
