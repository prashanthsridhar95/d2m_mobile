package com.d2m.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.d2m

/**
 * Brand lockup -- the mark plus the wordmark, and optionally an eyebrow.
 *
 * The comps lead with a mark-plus-wordmark lockup rather than a bare word,
 * and the entry screen is the one place a first-time user sees the brand
 * at full size, so it should be the same mark that appears everywhere
 * after. Previously the product name rendered as one bold sans string.
 *
 * `tagline` is optional and unset by default: this doesn't invent copy for
 * a brand it doesn't own. Pass one when there is approved wording.
 */
@Composable
fun BrandMark(size: Dp = 30.dp, modifier: Modifier = Modifier) {
    val tint = d2m.gold
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.045f)
        fun diamond(inset: Float) = Path().apply {
            moveTo(w / 2f, h * inset)
            lineTo(w * (1f - inset), h / 2f)
            lineTo(w / 2f, h * (1f - inset))
            lineTo(w * inset, h / 2f)
            close()
        }
        drawPath(diamond(0.06f), tint, style = stroke)
        drawPath(diamond(0.31f), tint, style = stroke)
    }
}

@Composable
fun D2MBrand(
    name: String = "D2M",
    tagline: String? = null,
    markSize: Dp = 30.dp,
    onDark: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BrandMark(markSize)
        Column {
            SubHeading(name)
            if (tagline != null) EyebrowText(tagline)
        }
    }
}
