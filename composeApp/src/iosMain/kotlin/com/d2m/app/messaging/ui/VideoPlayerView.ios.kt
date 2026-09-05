package com.d2m.app.messaging.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL

/**
 * Real in-app playback via AVPlayer, presented through AVKit's
 * `AVPlayerViewController` -- the direct counterpart of the Android
 * actual's ExoPlayer + `PlayerView` (AVPlayerViewController supplies the
 * same standard transport controls, scrubber and full-screen affordance
 * that PlayerView does, so neither platform has to hand-build a control
 * bar).
 *
 * Replaces a placeholder that rendered the literal text "Video playback
 * isn't available on iOS yet" inside the media viewer, which meant tapping
 * any video bubble on iOS produced a dead black box -- while the same tap
 * on Android opened a working player. Since the whole point of the viewer
 * is "shouldnt be going outside the app", falling back to opening the
 * system player was never an option either.
 *
 * Lifecycle mirrors the Android actual exactly: one player per `url`,
 * remember-scoped, explicitly paused on dispose. Pausing matters more here
 * than the ExoPlayer `release()` it mirrors -- an AVPlayer left playing
 * keeps pulling audio through the shared AVAudioSession, which would fight
 * with CallKit's session (WebRtcAudioSessionBridge.kt) if a call started
 * while a video had been left running.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayerView(url: String, modifier: Modifier, autoPlay: Boolean) {
    val player = remember(url) {
        NSURL.URLWithString(url)?.let { AVPlayer.playerWithURL(it) }
    }

    DisposableEffect(player) {
        if (autoPlay) player?.play()
        onDispose { player?.pause() }
    }

    UIKitViewController(
        modifier = modifier.fillMaxSize(),
        factory = {
            AVPlayerViewController().apply {
                this.player = player
                showsPlaybackControls = true
            }
        },
    )
}
