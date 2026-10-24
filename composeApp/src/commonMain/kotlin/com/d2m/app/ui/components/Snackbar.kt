package com.d2m.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import com.d2m.app.ui.theme.d2m
import kotlinx.coroutines.delay

enum class D2MSnackKind { Info, Success, Error }

/** One toast. [id] makes two identical messages in a row still re-trigger the host. */
@Stable
class D2MSnack internal constructor(
    val id: Long,
    val message: String,
    val kind: D2MSnackKind,
    val actionLabel: String?,
    val onAction: (() -> Unit)?,
)

/**
 * App-wide toast controller. Obtain with `LocalD2MSnackbar.current` and call
 * [show] from any callback (not suspending, safe from click handlers and
 * coroutines alike). The single [D2MSnackbarHost] in App.kt renders it above
 * the bottom bar, so individual screens never own a snackbar host.
 */
@Stable
class D2MSnackbarState {
    private var counter = 0L
    internal var current by mutableStateOf<D2MSnack?>(null)
        private set

    fun show(
        message: String,
        kind: D2MSnackKind = D2MSnackKind.Info,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
    ) {
        current = D2MSnack(++counter, message, kind, actionLabel, onAction)
    }

    fun showError(message: String) = show(message, D2MSnackKind.Error)
    fun showSuccess(message: String) = show(message, D2MSnackKind.Success)

    fun dismiss(id: Long) {
        if (current?.id == id) current = null
    }
}

val LocalD2MSnackbar = compositionLocalOf { D2MSnackbarState() }

@Composable
fun rememberD2MSnackbarState(): D2MSnackbarState = remember { D2MSnackbarState() }

@Composable
fun D2MSnackbarHost(state: D2MSnackbarState, modifier: Modifier = Modifier) {
    // Keep the last snack around while the exit animation plays.
    var shown by remember { mutableStateOf<D2MSnack?>(null) }
    val current = state.current
    if (current != null && current !== shown) shown = current

    LaunchedEffect(current?.id) {
        val c = current ?: return@LaunchedEffect
        delay(if (c.actionLabel != null) 6_000 else 3_500)
        state.dismiss(c.id)
    }

    AnimatedVisibility(
        visible = current != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        val snack = shown
        if (snack != null) SnackContent(snack) { state.dismiss(snack.id) }
    }
}

@Composable
private fun SnackContent(snack: D2MSnack, onDismiss: () -> Unit) {
    val status = LocalD2MStatusPalette.current
    val accent = when (snack.kind) {
        D2MSnackKind.Info -> d2m.accent
        D2MSnackKind.Success -> status.success.fg
        D2MSnackKind.Error -> status.danger.fg
    }
    val shape = RoundedCornerShape(D2MRadius.lg)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .background(d2m.panelBg, shape)
            .border(1.dp, accent.copy(alpha = 0.5f), shape)
            .padding(start = 16.dp, end = 8.dp)
            .padding(vertical = 4.dp),
    ) {
        Box(Modifier.padding(vertical = 10.dp).background(accent, RoundedCornerShape(2.dp)).padding(horizontal = 2.dp, vertical = 9.dp))
        Text(
            snack.message,
            color = d2m.panelFg,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
        )
        if (snack.actionLabel != null) {
            Text(
                snack.actionLabel,
                color = d2m.goldButton,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.bounceClickable(
                    onClick = { snack.onAction?.invoke(); onDismiss() },
                ).padding(horizontal = 8.dp),
            )
        }
    }
}
