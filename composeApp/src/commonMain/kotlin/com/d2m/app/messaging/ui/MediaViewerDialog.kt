package com.d2m.app.messaging.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage

/**
 * "always the media bubble size is a defined height & width. If required,
 * the user would open by tapping it & seeing the full view - provide a
 * image & video viewer - shouldnt be going outside the app." A full-screen
 * in-app viewer (Dialog with usePlatformDefaultWidth = false, same pattern
 * CallLayer.kt already uses for its own full-screen overlays) rather than an
 * external intent/browser -- images get pinch-to-zoom + pan, video gets a
 * real in-app player (VideoPlayerView.kt).
 */
sealed class MediaViewerTarget {
    data class Image(val url: String, val contentDescription: String?) : MediaViewerTarget()
    data class Video(val url: String) : MediaViewerTarget()
}

@Composable
fun MediaViewerDialog(target: MediaViewerTarget?, onDismiss: () -> Unit) {
    if (target == null) return
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            when (target) {
                is MediaViewerTarget.Image -> ZoomableImage(
                    url = target.url,
                    contentDescription = target.contentDescription,
                    modifier = Modifier.fillMaxSize(),
                )
                is MediaViewerTarget.Video -> VideoPlayerView(url = target.url, modifier = Modifier.fillMaxSize())
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .size(40.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

/** Pinch-to-zoom + pan (1x-5x, clamped) -- standard photo-viewer gesture, matches every native gallery app's own image viewer. */
@Composable
private fun ZoomableImage(url: String, contentDescription: String?, modifier: Modifier = Modifier) {
    var scale by remember(url) { mutableFloatStateOf(1f) }
    var offsetX by remember(url) { mutableFloatStateOf(0f) }
    var offsetY by remember(url) { mutableFloatStateOf(0f) }

    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .pointerInput(url) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale <= 1f) {
                        offsetX = 0f
                        offsetY = 0f
                    } else {
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY,
            ),
    )
}
