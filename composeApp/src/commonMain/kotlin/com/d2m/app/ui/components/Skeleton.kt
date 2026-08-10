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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shimmer loading placeholder -- mirrors components/Skeleton.jsx's "shimmer over spinner" convention. */
@Composable
fun D2MSkeleton(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 16.dp,
    radius: Dp = 8.dp,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "skeleton-alpha",
    )
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.15f)
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
        D2MSkeleton(height = 44.dp, radius = 10.dp)
    }
}

@Composable
fun CardSkeletonRow(count: Int = 3) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            Column {
                D2MSkeleton(width = 120.dp, height = 120.dp, radius = 14.dp)
                androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
                D2MSkeleton(width = 100.dp, height = 12.dp)
            }
        }
    }
}
