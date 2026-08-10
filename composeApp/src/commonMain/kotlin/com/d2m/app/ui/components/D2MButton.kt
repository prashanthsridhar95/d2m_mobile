package com.d2m.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button as M3Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Mirrors components/Button.jsx's 3 variants x 3 sizes, token-driven colors. */
enum class D2MButtonVariant { SOLID, OUTLINE, GHOST }
enum class D2MButtonSize { SM, MD, LG }

@Composable
fun D2MButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: D2MButtonVariant = D2MButtonVariant.SOLID,
    size: D2MButtonSize = D2MButtonSize.MD,
    enabled: Boolean = true,
) {
    val padding = when (size) {
        D2MButtonSize.SM -> PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        D2MButtonSize.MD -> PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        D2MButtonSize.LG -> PaddingValues(horizontal = 26.dp, vertical = 16.dp)
    }
    when (variant) {
        D2MButtonVariant.SOLID -> M3Button(onClick = onClick, modifier = modifier, enabled = enabled, contentPadding = padding) {
            Text(text)
        }
        D2MButtonVariant.OUTLINE -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            contentPadding = padding,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        ) { Text(text) }
        D2MButtonVariant.GHOST -> TextButton(onClick = onClick, modifier = modifier, enabled = enabled, contentPadding = padding) {
            Text(text)
        }
    }
}
