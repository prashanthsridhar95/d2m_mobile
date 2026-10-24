package com.d2m.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class SwipeDirection { Accept, Pass, Snooze }

/**
 * Hinge/Bumble-style card stack. The whole card (whatever [card] draws) is one unit that translates + rotates; a
 * horizontal drag decides, a vertical drag is left to the card's own scroll container (the standard Compose axis lock:
 * `draggable(Horizontal)` only claims a pointer once horizontal travel passes touch slop, and a child's `verticalScroll`
 * consumes first when the vertical travel wins, so the two never fight).
 *
 * Perf contract: nothing here reads the drag Animatables during composition. They are only read inside
 * `graphicsLayer {}` / `drawWithContent {}` lambdas, so a drag recomposes nothing.
 */
@Stable
class SwipeDeckState {
    internal val offsetX = Animatable(0f)
    internal val offsetY = Animatable(0f)
    internal var swiper: ((SwipeDirection) -> Unit)? = null

    /** True while a fly-off is animating; buttons use it to ignore double taps. */
    var busy by mutableStateOf(false)
        internal set

    /** Programmatic swipe (the action buttons); runs the identical fly-off a gesture would. */
    fun swipe(direction: SwipeDirection) { swiper?.invoke(direction) }
}

@Composable
fun rememberSwipeDeckState(): SwipeDeckState = remember { SwipeDeckState() }

private val FlyEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
private const val STACK_DEPTH = 3
private const val COMMIT_FRACTION = 0.30f
private const val FLING_DP_PER_SEC = 700f

/**
 * @param items the queue, top first. Only the first [STACK_DEPTH] are composed.
 * @param card draws one item; `isTop` is false for the peeking cards behind (render the cheap hero only).
 */
@Composable
fun <T : Any> SwipeDeck(
    items: List<T>,
    itemKey: (T) -> Any,
    state: SwipeDeckState,
    onDecision: (T, SwipeDirection) -> Unit,
    stampAccept: String,
    stampPass: String,
    stampSnooze: String,
    modifier: Modifier = Modifier,
    card: @Composable (item: T, isTop: Boolean) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentItems by rememberUpdatedState(items)
    val currentOnDecision by rememberUpdatedState(onDecision)

    BoxWithConstraints(modifier = modifier) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val flingPx = with(androidx.compose.ui.platform.LocalDensity.current) { FLING_DP_PER_SEC.dp.toPx() }

        // Latest size for the lambdas below without re-creating them (and the draggable state) on every layout.
        val size by rememberUpdatedState(wPx to hPx)

        val flyOff: (SwipeDirection, Float) -> Unit = remember(state) {
            { dir, velocityX ->
                if (!state.busy && currentItems.isNotEmpty()) {
                    state.busy = true
                    val top = currentItems.first()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        val (w, h) = size
                        val xTarget = when (dir) {
                            SwipeDirection.Accept -> w * 1.6f
                            SwipeDirection.Pass -> -w * 1.6f
                            SwipeDirection.Snooze -> state.offsetX.value
                        }
                        val yTarget = if (dir == SwipeDirection.Snooze) -h * 1.1f else state.offsetY.value
                        val spec = tween<Float>(durationMillis = 230, easing = FlyEasing)
                        val jobX = launch(start = CoroutineStart.UNDISPATCHED) {
                            state.offsetX.animateTo(xTarget, spec, initialVelocity = velocityX)
                        }
                        val jobY = launch(start = CoroutineStart.UNDISPATCHED) { state.offsetY.animateTo(yTarget, spec) }
                        jobX.join(); jobY.join()
                        // Same continuation: the queue shrink and the offset reset land in one frame, so the card that
                        // was peeking (already at full scale by now) simply becomes the top card with no jump.
                        currentOnDecision(top, dir)
                        state.offsetX.snapTo(0f)
                        state.offsetY.snapTo(0f)
                        state.busy = false
                    }
                }
            }
        }
        state.swiper = { dir -> flyOff(dir, 0f) }

        var crossed by remember { mutableStateOf(false) }
        val draggable = rememberDraggableState { delta ->
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                val next = state.offsetX.value + delta
                state.offsetX.snapTo(next)
                val past = abs(next) > size.first * COMMIT_FRACTION
                if (past != crossed) {
                    crossed = past
                    if (past) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        }

        val visible = items.take(STACK_DEPTH)
        // Back to front so the top card draws last. key() keeps each card's composition alive as it moves up a slot.
        for (slot in visible.indices.reversed()) {
            val item = visible[slot]
            key(itemKey(item)) {
                val isTop = slot == 0
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (isTop) {
                                val x = state.offsetX.value
                                translationX = x
                                translationY = state.offsetY.value
                                rotationZ = (x / wPx).coerceIn(-1.5f, 1.5f) * 12f
                                transformOrigin = TransformOrigin(0.5f, 0.92f)
                            } else {
                                val p = (maxOf(abs(state.offsetX.value) / (wPx * 0.5f), abs(state.offsetY.value) / (hPx * 0.4f)))
                                    .coerceIn(0f, 1f)
                                val rise = 14.dp.toPx()
                                val from = slot.toFloat()
                                val to = (slot - 1).toFloat()
                                val pos = from + (to - from) * p
                                val s = 1f - 0.045f * pos
                                scaleX = s; scaleY = s
                                translationY = rise * pos
                                transformOrigin = TransformOrigin(0.5f, 0f)
                            }
                        }
                        .then(
                            if (isTop) {
                                Modifier.draggable(
                                    state = draggable,
                                    orientation = Orientation.Horizontal,
                                    enabled = !state.busy,
                                    onDragStarted = { crossed = false },
                                    onDragStopped = { velocity ->
                                        val (w, _) = size
                                        val x = state.offsetX.value
                                        val dir = when {
                                            x > w * COMMIT_FRACTION || (velocity > flingPx && x > w * 0.04f) -> SwipeDirection.Accept
                                            x < -w * COMMIT_FRACTION || (velocity < -flingPx && x < -w * 0.04f) -> SwipeDirection.Pass
                                            else -> null
                                        }
                                        crossed = false
                                        if (dir != null) flyOff(dir, velocity) else scope.launch {
                                            // Natural spring back, carrying the release velocity so it feels flicked, not snapped.
                                            val spring = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)
                                            launch(start = CoroutineStart.UNDISPATCHED) { state.offsetX.animateTo(0f, spring, initialVelocity = velocity) }
                                            launch(start = CoroutineStart.UNDISPATCHED) { state.offsetY.animateTo(0f, spring) }
                                        }
                                    },
                                )
                            } else Modifier,
                        ),
                ) {
                    card(item, isTop)
                    if (isTop) Stamps(state, wPx, hPx, stampAccept, stampPass, stampSnooze)
                }
            }
        }
    }
}

@Composable
private fun Stamps(state: SwipeDeckState, wPx: Float, hPx: Float, accept: String, pass: String, snooze: String) {
    val palette = LocalD2MStatusPalette.current
    Box(Modifier.fillMaxSize()) {
        Stamp(accept, palette.success.fg, Alignment.TopStart, -14f) { (state.offsetX.value / (wPx * 0.25f)).coerceIn(0f, 1f) }
        Stamp(pass, palette.danger.fg, Alignment.TopEnd, 14f) { (-state.offsetX.value / (wPx * 0.25f)).coerceIn(0f, 1f) }
        Stamp(snooze, palette.info.fg, Alignment.TopCenter, 0f) { (-state.offsetY.value / (hPx * 0.2f)).coerceIn(0f, 1f) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Stamp(
    text: String,
    color: Color,
    align: Alignment,
    angle: Float,
    alpha: () -> Float,
) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 3.sp,
        modifier = Modifier
            .align(align)
            .padding(top = 96.dp, start = 28.dp, end = 28.dp)
            .graphicsLayer { this.alpha = alpha(); rotationZ = angle }
            .clip(RoundedCornerShape(8.dp))
            .border(3.dp, color, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}
