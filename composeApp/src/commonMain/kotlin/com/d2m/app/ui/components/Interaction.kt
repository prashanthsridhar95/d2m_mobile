package com.d2m.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.d2m.app.platform.rememberReduceMotion
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/**
 * Press feedback driven by any [InteractionSource]: scales the content down
 * while pressed and springs back with a small overshoot. The scale is read
 * inside graphicsLayer's lambda, so a press animates in the draw phase only --
 * no recomposition and no re-layout per frame.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.96f,
): Modifier {
    val pressed = interactionSource.collectIsPressedAsState()
    val reduceMotion = rememberReduceMotion()
    val scale: State<Float> = animateFloatAsState(
        targetValue = if (pressed.value && !reduceMotion) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return this.graphicsLayer {
        val s = scale.value
        scaleX = s
        scaleY = s
    }
}

/**
 * The app-wide tappable: >= 48dp touch target, bounce on press, no ripple
 * (the scale IS the feedback, which reads closer to Bumble/Hinge than a
 * Material ripple does). Use for cards, rows, icon buttons and chips.
 */
@Composable
fun Modifier.bounceClickable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    enforceMinimumTarget: Boolean = true,
): Modifier {
    val source = remember { MutableInteractionSource() }
    return this
        .then(if (enforceMinimumTarget) Modifier.minimumInteractiveComponentSize() else Modifier)
        .pressScale(source, pressedScale)
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = onClick,
        )
}
