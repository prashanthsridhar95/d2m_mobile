package com.d2m.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.d2m
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shimmer loading placeholder -- "shimmer over spinner" wherever the
 * loading area has a known, stable shape.
 *
 * Retheme note: the shimmer was an onSurface alpha wash, which on the new
 * warm ground reads as a grey smear rather than as warm paper. It now
 * pulses between the sunken surface and the border tone -- the same two
 * tokens a real card uses -- so a loading list looks like the list it is
 * about to become. Default radius drops to D2MRadius.md with everything
 * else.
 */
@Composable
fun D2MSkeleton(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 16.dp,
    radius: Dp = D2MRadius.md,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "skeleton-progress",
    )
    val base = lerp(d2m.surfaceSunken, d2m.border, progress)
    var m = modifier.height(height).background(base, RoundedCornerShape(radius))
    if (width != null) m = m.width(width)
    androidx.compose.foundation.layout.Box(modifier = if (width != null) m else m.fillMaxWidth())
}

/** Mirrors the BioDataSkeleton pattern: label+field pairs grouped under section headings, previewing the real form's shape. */
@Composable
fun FieldSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        D2MSkeleton(width = 90.dp, height = 9.dp)
        androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
        D2MSkeleton(height = 46.dp)
    }
}

@Composable
fun CardSkeletonRow(count: Int = 3) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            Column {
                D2MSkeleton(width = 120.dp, height = 120.dp, radius = D2MRadius.lg)
                androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
                D2MSkeleton(width = 100.dp, height = 12.dp)
            }
        }
    }
}
