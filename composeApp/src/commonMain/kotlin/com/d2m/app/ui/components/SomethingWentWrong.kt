package com.d2m.app.ui.components

import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
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
        // Assertive: a whole-screen failure should be announced the moment it replaces the content.
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Assertive }.fillMaxWidth().fillMaxHeight().heightIn(min = 420.dp).padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ErrorIllustration(Modifier.size(width = 150.dp, height = 150.dp).clearAndSetSemantics {})
        Text(
            strings.errorTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = d2m.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 36.dp).semantics { heading() },
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

/**
 * D2M's own picture for "something went wrong": the South Indian horoscope chart (the product's core object) with the
 * brand's gold diamond mark at its centre -- and one edge of the diamond left open, like a chart that didn't finish
 * drawing. Greys and gold come from the app's palette, so it follows light/dark.
 */
@Composable
private fun ErrorIllustration(modifier: Modifier = Modifier) {
    val grid = d2m.borderStrong
    val gridFaint = d2m.border
    val gold = d2m.gold
    val goldStrong = d2m.goldStrong
    val page = MaterialTheme.colorScheme.background
    Canvas(modifier) {
        val side = size.minDimension
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val cell = side / 4f
        val thin = 1.5.dp.toPx()
        val thick = 2.dp.toPx()

        // the 4x4 chart: outer frame, the four cells on each edge, and the merged 2x2 centre
        drawRect(gridFaint, Offset(left, top), Size(side, side), style = Stroke(thick))
        for (i in 1..3) {
            val x = left + i * cell
            val y = top + i * cell
            drawLine(grid, Offset(x, top), Offset(x, top + cell), thin)
            drawLine(grid, Offset(x, top + 3 * cell), Offset(x, top + side), thin)
            drawLine(grid, Offset(left, y), Offset(left + cell, y), thin)
            drawLine(grid, Offset(left + 3 * cell, y), Offset(left + side, y), thin)
        }
        drawRect(grid, Offset(left + cell, top + cell), Size(cell * 2, cell * 2), style = Stroke(thick))

        // the brand diamond, centred on the merged cell, over a page-coloured disc so the grid doesn't run through it
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = cell * 0.86f
        val top0 = Offset(c.x, c.y - r)
        val right0 = Offset(c.x + r, c.y)
        val bottom0 = Offset(c.x, c.y + r)
        val left0 = Offset(c.x - r, c.y)
        drawPath(Path().apply { moveTo(top0.x, top0.y); lineTo(right0.x, right0.y); lineTo(bottom0.x, bottom0.y); lineTo(left0.x, left0.y); close() }, page)
        val cap = StrokeCap.Round
        val w = 2.5.dp.toPx()
        drawLine(goldStrong, top0, right0, w, cap)
        drawLine(goldStrong, right0, bottom0, w, cap)
        drawLine(goldStrong, bottom0, left0, w, cap)
        // the open edge: only the two ends of left -> top are drawn
        fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        drawLine(goldStrong, left0, lerp(left0, top0, 0.28f), w, cap)
        drawLine(goldStrong, lerp(left0, top0, 0.78f), top0, w, cap)

        // inner diamond
        val ri = r * 0.5f
        drawPath(
            Path().apply { moveTo(c.x, c.y - ri); lineTo(c.x + ri, c.y); lineTo(c.x, c.y + ri); lineTo(c.x - ri, c.y); close() },
            gold,
            style = Stroke(w),
        )
    }
}
