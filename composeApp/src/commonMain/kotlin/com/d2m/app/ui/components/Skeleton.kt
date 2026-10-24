package com.d2m.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
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
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "skeleton-progress",
    )
    val base = d2m.surfaceSunken
    val highlight = lerp(d2m.surfaceSunken, MaterialTheme.colorScheme.surface, 0.75f)
    var m = modifier.height(height)
    m = if (width != null) m.width(width) else m.fillMaxWidth()
    // The sweep position is read in the draw phase only: a running shimmer
    // never recomposes or re-lays-out, it just repaints this one rect.
    Box(
        modifier = m
            .clip(RoundedCornerShape(radius))
            .drawBehind {
                drawRect(base)
                val w = size.width
                val x = -w + progress.value * 3f * w
                drawRect(
                    Brush.horizontalGradient(
                        colors = listOf(highlight.copy(alpha = 0f), highlight, highlight.copy(alpha = 0f)),
                        startX = x,
                        endX = x + w,
                    ),
                )
            },
    )
}

/**
 * One list row placeholder (avatar + two text bars) with the same geometry
 * as the real conversation / notification rows so nothing reflows when data
 * arrives.
 */
@Composable
fun SkeletonListRow(modifier: Modifier = Modifier, avatarSize: Dp = 56.dp, circle: Boolean = true) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
    ) {
        D2MSkeleton(width = avatarSize, height = avatarSize, radius = if (circle) avatarSize / 2 else D2MRadius.lg)
        Column(Modifier.weight(1f)) {
            D2MSkeleton(width = 150.dp, height = 14.dp)
            Spacer(Modifier.height(8.dp))
            D2MSkeleton(height = 11.dp)
        }
    }
}

@Composable
fun SkeletonList(count: Int = 6, modifier: Modifier = Modifier, avatarSize: Dp = 56.dp, circle: Boolean = true) {
    Column(modifier = modifier) {
        repeat(count) { SkeletonListRow(avatarSize = avatarSize, circle = circle) }
    }
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
