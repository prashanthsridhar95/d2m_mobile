package com.d2m.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.d2m

/**
 * The full-screen "something went wrong" state: a soft illustration, a bold headline, one sentence of help and a plain
 * "Try again" link -- no card, no box, no raw error text. Centred in whatever space the screen gives it (at least
 * 420dp tall, so it also works inside a scrolling list). Any screen whose content failed to load shows this instead of
 * leaving its own buttons and chrome on top of data that isn't there.
 */
@Composable
fun SomethingWentWrong(onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current.common
    Column(
        modifier = modifier.fillMaxWidth().fillMaxHeight().heightIn(min = 420.dp).padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ErrorIllustration(Modifier.size(width = 168.dp, height = 140.dp))
        Text(
            strings.errorTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = d2m.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 36.dp),
        )
        Text(
            strings.errorBody,
            style = MaterialTheme.typography.bodyLarge,
            color = d2m.meta,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp),
        )
        if (onRetry != null) {
            Text(
                strings.tryAgain,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = d2m.accent,
                modifier = Modifier
                    .padding(top = 26.dp)
                    .clickable(role = Role.Button, onClick = onRetry)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            )
        }
    }
}

/** Three overlapping soft blobs under a spotlight cone, in the app's own neutral tones (so it follows light/dark). */
@Composable
private fun ErrorIllustration(modifier: Modifier = Modifier) {
    // Greys with real contrast against the cream page (the sunken/border tones alone disappear on it).
    val cone = d2m.border.copy(alpha = 0.85f)
    val back = d2m.faint.copy(alpha = 0.75f)
    val front = d2m.borderStrong
    val deep = d2m.label
    val pool = d2m.surfaceSunken
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        // spotlight cone from the top-left corner down onto the blobs
        drawPath(
            Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.60f, h * 0.36f)
                lineTo(w * 0.16f, h * 0.70f)
                close()
            },
            color = cone,
        )
        // rear-left blob
        drawPath(blob(w * 0.30f, h * 0.30f, w * 0.34f, h * 0.52f), color = back)
        // rear-right, darker blob
        drawPath(blob(w * 0.52f, h * 0.36f, w * 0.34f, h * 0.44f), color = deep.copy(alpha = 0.85f))
        // front pooled shape
        drawPath(blob(w * 0.12f, h * 0.52f, w * 0.78f, h * 0.40f), color = front)
        drawPath(blob(w * 0.22f, h * 0.46f, w * 0.50f, h * 0.38f), color = pool)
    }
}

/** A rounded, slightly lopsided blob inside the given box. */
private fun blob(x: Float, y: Float, w: Float, h: Float): Path = Path().apply {
    moveTo(x + w * 0.10f, y + h * 0.55f)
    cubicTo(x - w * 0.02f, y + h * 0.20f, x + w * 0.30f, y - h * 0.02f, x + w * 0.55f, y + h * 0.10f)
    cubicTo(x + w * 0.85f, y + h * 0.02f, x + w * 1.05f, y + h * 0.40f, x + w * 0.92f, y + h * 0.68f)
    cubicTo(x + w * 1.00f, y + h * 0.98f, x + w * 0.60f, y + h * 1.02f, x + w * 0.45f, y + h * 0.95f)
    cubicTo(x + w * 0.20f, y + h * 1.02f, x - w * 0.02f, y + h * 0.85f, x + w * 0.10f, y + h * 0.55f)
    close()
}
