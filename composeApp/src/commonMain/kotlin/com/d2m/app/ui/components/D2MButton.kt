package com.d2m.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button as M3Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.D2MStroke
import com.d2m.app.ui.theme.d2m

/*
 * Button, retuned to the reference comps.
 *
 * Four button *jobs* appear across the three comps, and the old three
 * variants couldn't express them:
 *
 *   SOLID           Register / Express interest / Continue -- maroon fill,
 *                   cream text. One per screen region.
 *   ACCENT_OUTLINE  View profile -- maroon rule, maroon text, transparent
 *                   fill. The repeated-many-times-per-screen action on a
 *                   browse list, where 20 maroon fills would turn the
 *                   screen into a wall of red.
 *   OUTLINE         Save to shortlist / Print bio-data -- warm neutral
 *                   rule, ink text. Genuinely secondary.
 *   GOLD            Log in to see full contact -- the CTA that lives on
 *                   the inverted "Contact the family" panel, where maroon
 *                   on espresso has almost no contrast and cream on
 *                   espresso reads as body text rather than a button.
 *
 * OUTLINE deliberately changes meaning from "maroon rule" (what the old
 * enum did) to "neutral rule", matching web's Button.jsx exactly, and
 * ACCENT_OUTLINE is the new name for the maroon one. Every existing call
 * site that wanted a quiet secondary keeps working and simply reads
 * neutral now, which is what it should have been.
 *
 * Shape comes from D2MRadius.md (4dp, was 13dp via Material's default) --
 * that single change is most of what makes a screen read as a printed
 * register rather than a consumer app.
 */
enum class D2MButtonVariant { SOLID, ACCENT_OUTLINE, OUTLINE, GOLD, GHOST }
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
        D2MButtonSize.LG -> PaddingValues(horizontal = 24.dp, vertical = 15.dp)
    }
    val shape = RoundedCornerShape(D2MRadius.md)

    // Sentence case, never uppercase: the uppercase treatment in this
    // design belongs to labels (see LabelText), and using it on buttons
    // too would blur the two. maxLines = 1 because a button label that
    // wraps reads as a broken control.
    val label: @Composable () -> Unit = {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    when (variant) {
        D2MButtonVariant.SOLID -> M3Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            contentPadding = padding,
            // Zero elevation everywhere -- nothing in this design floats.
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = d2m.accent,
                contentColor = d2m.accentOn,
                // Material's default disabled pair is a neutral grey on
                // grey, which on this warm ground reads as a different
                // material rather than as "not available yet".
                disabledContainerColor = d2m.surfaceSunken,
                disabledContentColor = d2m.faint,
            ),
        ) { label() }

        D2MButtonVariant.ACCENT_OUTLINE -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            contentPadding = padding,
            border = BorderStroke(D2MStroke.hairline, d2m.accentBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = d2m.accent,
                disabledContentColor = d2m.faint,
            ),
        ) { label() }

        D2MButtonVariant.OUTLINE -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            contentPadding = padding,
            border = BorderStroke(D2MStroke.hairline, d2m.borderStrong),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = d2m.textPrimary,
                disabledContentColor = d2m.faint,
            ),
        ) { label() }

        D2MButtonVariant.GOLD -> M3Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            contentPadding = padding,
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = d2m.goldButton,
                contentColor = d2m.goldButtonInk,
                disabledContainerColor = d2m.surfaceSunken,
                disabledContentColor = d2m.faint,
            ),
        ) { label() }

        D2MButtonVariant.GHOST -> TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            contentPadding = padding,
            colors = ButtonDefaults.textButtonColors(
                contentColor = d2m.accent,
                disabledContentColor = d2m.faint,
            ),
        ) { label() }
    }
}
