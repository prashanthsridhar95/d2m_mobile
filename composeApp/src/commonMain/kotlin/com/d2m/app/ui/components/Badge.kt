package com.d2m.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.D2MStroke
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import com.d2m.app.ui.theme.d2m

/*
 * Badge, retuned to the comps' FEATURED tag.
 *
 * The old badge was a soft-tinted, fully-rounded pill at labelMedium. The
 * comps' equivalent is a near-rectangular tag with uppercase tracked type
 * -- the same typographic idea as LabelText, just with a fill behind it --
 * which is what makes a row of them read as catalogue metadata rather
 * than as UI chrome.
 *
 * GOLD is deliberately the only saturated fill available here, so it stays
 * scarce and keeps meaning "featured". The status tones (SUCCESS / INFO /
 * WARNING / DANGER / NEUTRAL) are kept because ~15 call sites map a
 * backend status onto one, but they now draw from the retuned warm status
 * palette rather than the old material-ish greens and blues.
 */
enum class D2MBadgeTone { SUCCESS, INFO, WARNING, DANGER, NEUTRAL, GOLD, ACCENT }

@Composable
fun D2MBadge(
    text: String,
    tone: D2MBadgeTone = D2MBadgeTone.NEUTRAL,
    modifier: Modifier = Modifier,
) {
    val palette = LocalD2MStatusPalette.current
    val bg: Color
    val fg: Color
    var borderColor: Color = Color.Transparent
    when (tone) {
        D2MBadgeTone.SUCCESS -> { bg = palette.success.bg; fg = palette.success.fg }
        D2MBadgeTone.INFO -> { bg = palette.info.bg; fg = palette.info.fg }
        D2MBadgeTone.WARNING -> { bg = palette.warning.bg; fg = palette.warning.fg }
        D2MBadgeTone.DANGER -> { bg = palette.danger.bg; fg = palette.danger.fg }
        D2MBadgeTone.NEUTRAL -> { bg = palette.neutral.bg; fg = palette.neutral.fg }
        D2MBadgeTone.GOLD -> { bg = d2m.gold; fg = Color(0xFFFFFDF8); borderColor = d2m.gold }
        D2MBadgeTone.ACCENT -> { bg = d2m.accent; fg = d2m.accentOn; borderColor = d2m.accent }
    }
    val shape = RoundedCornerShape(D2MRadius.sm)
    Text(
        text = text.uppercase(),
        color = fg,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(bg, shape)
            .border(D2MStroke.hairline, borderColor, shape)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

/**
 * The comps' FEATURED tag, named so call sites read as intent ("this
 * profile is featured") rather than as styling.
 */
@Composable
fun FeaturedBadge(text: String = "Featured", modifier: Modifier = Modifier) {
    D2MBadge(text, D2MBadgeTone.GOLD, modifier)
}

/**
 * Compatibility rating as a small squared tag.
 *
 * Deliberately different from D2MBadge in two ways -- sentence case, and a
 * numeric readout rather than a word. Those two differences are the whole
 * reason both can sit in the same card header row without reading as the
 * same thing: a badge is catalogue metadata, this is a computed rating.
 * The old version was a near-identical soft pill.
 */
@Composable
fun ScoreBadge(score: Double?, maxScore: Double = 10.0, modifier: Modifier = Modifier) {
    if (score == null) return
    val palette = LocalD2MStatusPalette.current
    val ratio = score / maxScore
    val colors = when {
        ratio >= 0.75 -> palette.success
        ratio >= 0.5 -> palette.info
        ratio >= 0.35 -> palette.warning
        else -> palette.neutral
    }
    val shape = RoundedCornerShape(D2MRadius.sm)
    Text(
        text = "${formatScore(score)}/${formatScore(maxScore)}",
        color = colors.fg,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .background(colors.bg, shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private fun formatScore(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else (kotlin.math.round(v * 10) / 10).toString()
