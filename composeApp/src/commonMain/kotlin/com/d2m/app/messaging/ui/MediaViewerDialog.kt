package com.d2m.app.messaging.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import kotlin.math.roundToInt

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

private const val ZOOM_MIN = 1f
private const val ZOOM_MAX = 5f

@Composable
fun MediaViewerDialog(target: MediaViewerTarget?, onDismiss: () -> Unit) {
    if (target == null) return
    // Hoisted above ZoomableImage so the bottom slider ("I should be able
    // to see the full image & then should be able to zoom in using the
    // slider" -- reported directly) and the pinch gesture drive the exact
    // same scale, instead of two zoom mechanisms that could disagree.
    var scale by remember(target) { mutableFloatStateOf(ZOOM_MIN) }
    var offsetX by remember(target) { mutableFloatStateOf(0f) }
    var offsetY by remember(target) { mutableFloatStateOf(0f) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            when (target) {
                is MediaViewerTarget.Image -> {
                    ZoomableImage(
                        url = target.url,
                        contentDescription = target.contentDescription,
                        scale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        onTransform = { newScale, dx, dy ->
                            scale = newScale
                            offsetX = dx
                            offsetY = dy
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 20.dp, start = 20.dp, end = 20.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50)),
                    ) {
                        Slider(
                            value = scale,
                            onValueChange = { newScale ->
                                scale = newScale
                                // Re-center pan whenever the slider zooms back out to
                                // 100% -- same "zoomed view doesn't carry over
                                // nonsensically" rule the pinch gesture already follows.
                                if (newScale <= ZOOM_MIN) {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            },
                            valueRange = ZOOM_MIN..ZOOM_MAX,
                            modifier = Modifier.width(140.dp).padding(start = 14.dp),
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White),
                        )
                        Text(
                            "${(scale * 100).roundToInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 14.dp),
                        )
                    }
                }
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

/**
 * Pinch-to-zoom + pan (1x-5x, clamped), plus the caller's own slider driving the same hoisted scale/offset state -- standard photo-viewer gesture, matches every native gallery app's own image viewer. Starts at ZOOM_MIN (the full, un-zoomed image) every time a new target opens.
 *
 * "Why image is always filled to width? If it's smaller show only that
 * size. If required, i'll zoom in" (reported directly) -- ContentScale.Fit
 * scales an image UP to fill the available box regardless of its real
 * resolution, same "always filled" bug web's PhotoLightbox had.
 * ContentScale.Inside is Compose's built-in "never upscale" mode: scales
 * DOWN to fit if the image is bigger than the box, otherwise renders at
 * the image's own natural size -- exactly the new baseline before the
 * pinch/slider zoom above (1x-5x) scales up from it.
 */
@Composable
private fun ZoomableImage(
    url: String,
    contentDescription: String?,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onTransform: (scale: Float, offsetX: Float, offsetY: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Inside,
        modifier = modifier
            .pointerInput(url) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(ZOOM_MIN, ZOOM_MAX)
                    if (newScale <= ZOOM_MIN) {
                        onTransform(newScale, 0f, 0f)
                    } else {
                        onTransform(newScale, offsetX + pan.x, offsetY + pan.y)
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
