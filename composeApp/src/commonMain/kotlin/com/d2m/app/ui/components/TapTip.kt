package com.d2m.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.d2m
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Tap to see what this means" -- the touch version of the web's hover tooltip (BubbleTooltip). Material's own tooltip
 * only opens on a long-press on a phone, which nobody finds; this opens on a plain tap, stays long enough to read a
 * sentence, closes on a second tap or after a few seconds, and is announced to screen readers as the explanation.
 *
 * [size] is the visible mark; the tap target is padded out to at least 44dp around it, so a 13dp tick is still easy to hit.
 * [container]/[content] colour the bubble (the tick/cross use their own green/red, like the web).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapTip(
    text: String,
    modifier: Modifier = Modifier,
    container: Color? = null,
    content: Color? = null,
    minTarget: Dp = 44.dp,
    visibleFor: Long = 4_000L,
    inner: @Composable () -> Unit,
) {
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(
                containerColor = container ?: d2m.ink,
                contentColor = content ?: d2m.accentOn,
            ) { Text(text) }
        },
        state = state,
        enableUserInput = false,
        modifier = modifier,
    ) {
        Box(
            Modifier
                .minimumTarget(minTarget)
                .clickable(role = Role.Button) {
                    if (state.isVisible) {
                        state.dismiss()
                    } else {
                        scope.launch { state.show() }
                        scope.launch { delay(visibleFor); state.dismiss() }
                    }
                }
                .semantics { contentDescription = text },
            contentAlignment = Alignment.Center,
        ) { inner() }
    }
}

private fun Modifier.minimumTarget(size: Dp): Modifier = this.then(Modifier.defaultMinSize(minWidth = size, minHeight = size))
