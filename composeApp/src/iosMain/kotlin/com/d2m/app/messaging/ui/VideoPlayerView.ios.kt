package com.d2m.app.messaging.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * No iOS target actually runs this app yet -- same stub precedent as
 * WebRtcEngine.ios.kt/SoundEffects.ios.kt/GifImageLoader.kt. See
 * VideoPlayerView.kt's doc comment.
 */
@Composable
actual fun VideoPlayerView(url: String, modifier: Modifier, autoPlay: Boolean) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Text("Video playback isn't available on iOS yet", color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}
