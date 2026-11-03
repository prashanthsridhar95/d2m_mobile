package com.d2m.app.ui.components

import com.d2m.app.platform.rememberReduceMotion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.toggleable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.d2m

/*
 * One pill-button system for parent mode.
 *
 *   PRIMARY    filled accent. One per region.
 *   SECONDARY  soft tonal fill + hairline. Clearly below primary.
 *   TERTIARY   no fill, accent text. Dismissive / low-stakes.
 *
 * All are >= 48dp (52dp by default), fully rounded, press-scale on touch,
 * single-line with shrink-to-fit so a long Hindi/Tamil label scales down
 * instead of being cut with an ellipsis.
 */
enum class D2MActionEmphasis { PRIMARY, SECONDARY, TERTIARY }

@Composable
fun D2MActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasis: D2MActionEmphasis = D2MActionEmphasis.PRIMARY,
    leadingIcon: ImageVector? = null,
    loading: Boolean = false,
    done: Boolean = false,
    enabled: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 52.dp,
) {
    val interactive = enabled && !loading && !done
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(D2MRadius.pill)
    val bg: Color
    val fg: Color
    val border: BorderStroke?
    when {
        done -> { bg = d2m.accentSoft; fg = d2m.accent; border = BorderStroke(1.dp, d2m.accentBorder) }
        !enabled -> { bg = if (emphasis == D2MActionEmphasis.TERTIARY) Color.Transparent else d2m.surfaceSunken; fg = d2m.faint; border = null }
        emphasis == D2MActionEmphasis.PRIMARY -> { bg = d2m.accent; fg = d2m.accentOn; border = null }
        emphasis == D2MActionEmphasis.SECONDARY -> { bg = d2m.surfaceSunken; fg = d2m.textPrimary; border = BorderStroke(1.dp, d2m.borderStrong) }
        else -> { bg = Color.Transparent; fg = d2m.accent; border = null }
    }
    Box(
        modifier = modifier
            .heightIn(min = height)
            .height(height)
            .pressScale(source)
            .clip(shape)
            .background(bg)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = interactive,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                loading -> CircularProgressIndicator(Modifier.size(18.dp), color = fg, strokeWidth = 2.dp)
                leadingIcon != null -> Icon(leadingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            }
            FitLabel(text, fg)
        }
    }
}

/** Single-line label that shrinks (down to 11sp) instead of truncating. */
@Composable
private fun FitLabel(text: String, color: Color) {
    val base = MaterialTheme.typography.labelLarge
    val baseSize = if (base.fontSize.isSpecified) base.fontSize.value else 14f
    var size by remember(text) { mutableStateOf(baseSize) }
    Text(
        text = text,
        style = base.copy(fontSize = size.sp),
        color = color,
        maxLines = 1,
        softWrap = false,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Clip,
        onTextLayout = { r -> if (r.hasVisualOverflow && size > 11f) size = (size - 1f).coerceAtLeast(11f) },
    )
}

private val androidx.compose.ui.unit.TextUnit.isSpecified get() = this != androidx.compose.ui.unit.TextUnit.Unspecified

/**
 * Round icon toggle (shortlist heart). Selected = filled accent icon on a soft
 * accent disc with accent rule; unselected = outlined meta icon on a neutral
 * disc. Springs on change and gives a haptic.
 */
@Composable
fun D2MToggleIconButton(
    selected: Boolean,
    onToggle: () -> Unit,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 52.dp,
    /** Spoken state ("Shortlisted" / "Not shortlisted"); falls back to the platform's On / Off. */
    stateDescription: String? = null,
) {
    val haptics = rememberHaptics()
    val reduceMotion = rememberReduceMotion()
    val source = remember { MutableInteractionSource() }
    val pop = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(selected) {
        if (first) { first = false; return@LaunchedEffect }
        if (reduceMotion) return@LaunchedEffect
        pop.snapTo(0.6f)
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
    }
    Box(
        modifier = modifier
            .size(size)
            .pressScale(source, 0.92f)
            .clip(CircleShape)
            .background(if (selected) d2m.accentSoft else d2m.surfaceSunken)
            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) d2m.accent else d2m.borderStrong), CircleShape)
            .toggleable(value = selected, interactionSource = source, indication = null, role = Role.Switch, onValueChange = {
                if (selected) haptics.light() else haptics.success()
                onToggle()
            })
            .then(if (stateDescription != null) Modifier.semantics { this.stateDescription = stateDescription } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (selected) selectedIcon else unselectedIcon,
            contentDescription = contentDescription,
            tint = if (selected) d2m.accent else d2m.meta,
            modifier = Modifier.size(24.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value },
        )
    }
}

/**
 * Bottom-docked action bar: soft top fade into a solid surface, content kept
 * clear of the home indicator via navigationBars insets. Place at
 * BottomCenter of a Box that extends to the real screen bottom.
 */
@Composable
fun D2MActionBar(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val surface = MaterialTheme.colorScheme.surface
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().height(20.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, surface.copy(alpha = 0.96f)))),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(surface)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}
