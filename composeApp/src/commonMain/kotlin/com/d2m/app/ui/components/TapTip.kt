package com.d2m.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.d2m.app.ui.theme.d2m
import kotlinx.coroutines.delay

/**
 * "Tap to see what this means" -- the touch version of the web's hover tooltip (BubbleTooltip.jsx): a small solid bubble
 * with a pointer, floating above whatever was tapped (below it when there is no room above), kept inside the screen
 * edges, in the accent colour with cream text unless told otherwise (the tick/cross use their own green/red, as on web).
 * Opens on a plain tap, closes on a second tap, a tap elsewhere, or after [visibleFor] ms.
 *
 * The wrapped content keeps its own size: [minTarget] only enlarges the tap area when you ask for it (the default is no
 * enlargement, so a pill or a tick never grows just because it can be tapped).
 */
@Composable
fun TapTip(
    text: String,
    modifier: Modifier = Modifier,
    container: Color? = null,
    content: Color? = null,
    minTarget: Dp = 0.dp,
    /** The tappable area's shape: the press ripple is clipped to it, so a pill ripples as a pill, not a square. */
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.ui.graphics.RectangleShape,
    visibleFor: Long = 4_000L,
    inner: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(open) { if (open) { delay(visibleFor); open = false } }
    val bubble = container ?: d2m.accent
    val ink = content ?: d2m.accentOn

    Box(
        modifier
            .defaultMinSize(minWidth = minTarget, minHeight = minTarget)
            .clip(shape)
            .clickable(role = Role.Button) { open = !open }
            .semantics { contentDescription = text },
        contentAlignment = Alignment.Center,
    ) {
        inner()
        if (open) {
            val caretX = remember { mutableStateOf(0f) }
            val above = remember { mutableStateOf(true) }
            val gapPx = with(androidx.compose.ui.platform.LocalDensity.current) { 6.dp.roundToPx() }
            val edgePx = with(androidx.compose.ui.platform.LocalDensity.current) { 10.dp.roundToPx() }
            val provider = remember {
                object : PopupPositionProvider {
                    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                        val x = (anchorBounds.center.x - popupContentSize.width / 2)
                            .coerceIn(edgePx, (windowSize.width - popupContentSize.width - edgePx).coerceAtLeast(edgePx))
                        val fitsAbove = anchorBounds.top - popupContentSize.height - gapPx >= edgePx
                        above.value = fitsAbove
                        caretX.value = (anchorBounds.center.x - x).toFloat()
                        val y = if (fitsAbove) anchorBounds.top - popupContentSize.height - gapPx else anchorBounds.bottom + gapPx
                        return IntOffset(x, y)
                    }
                }
            }
            Popup(
                popupPositionProvider = provider,
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true, dismissOnClickOutside = true),
            ) {
                BubbleWithCaret(text, bubble, ink, caretX.value, above.value)
            }
        }
    }
}

@Composable
private fun BubbleWithCaret(text: String, bubble: Color, ink: Color, caretX: Float, above: Boolean) {
    val caretH = 7.dp
    // Width of the bubble itself (not the window): the caret row below fills THIS width, so the popup is only as wide as
    // its text and the position provider can centre it on the tapped pill.
    Column(Modifier.width(IntrinsicSize.Max)) {
        if (!above) Caret(bubble, caretX, up = true, height = caretH)
        Box(
            Modifier
                .widthIn(max = 260.dp)
                .background(bubble, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(text, style = MaterialTheme.typography.bodySmall, color = ink, textAlign = TextAlign.Start)
        }
        if (above) Caret(bubble, caretX, up = false, height = caretH)
    }
}

/** The pointer: a small triangle at [x] px from the bubble's left edge (clamped off the rounded corners). */
@Composable
private fun Caret(color: Color, x: Float, up: Boolean, height: Dp) {
    Canvas(Modifier.fillMaxWidth().height(height)) {
        val half = height.toPx()
        val cx = x.coerceIn(half + 8.dp.toPx(), (size.width - half - 8.dp.toPx()).coerceAtLeast(half + 8.dp.toPx()))
        val path = Path().apply {
            if (up) { moveTo(cx - half, size.height); lineTo(cx, 0f); lineTo(cx + half, size.height) }
            else { moveTo(cx - half, 0f); lineTo(cx, size.height); lineTo(cx + half, 0f) }
            close()
        }
        drawPath(path, color)
    }
}
