package com.d2m.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.LocalD2MStatusPalette

enum class D2MBadgeTone { SUCCESS, INFO, WARNING, DANGER, NEUTRAL }

/** Mirrors components/Badge.jsx's Badge/ScoreBadge/PreviewBadge family -- one pill component, tone-driven. */
@Composable
fun D2MBadge(text: String, tone: D2MBadgeTone = D2MBadgeTone.NEUTRAL) {
    val palette = LocalD2MStatusPalette.current
    val colors = when (tone) {
        D2MBadgeTone.SUCCESS -> palette.success
        D2MBadgeTone.INFO -> palette.info
        D2MBadgeTone.WARNING -> palette.warning
        D2MBadgeTone.DANGER -> palette.danger
        D2MBadgeTone.NEUTRAL -> palette.neutral
    }
    Text(
        text = text,
        color = colors.fg,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .background(colors.bg, RoundedCornerShape(D2MRadius.pill))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Compatibility-score chip, e.g. "8.2/10" -- mirrors ScoreBadge, tone derived from the score band. */
@Composable
fun ScoreBadge(score: Double?, maxScore: Double = 10.0) {
    if (score == null) return
    val ratio = score / maxScore
    val tone = when {
        ratio >= 0.75 -> D2MBadgeTone.SUCCESS
        ratio >= 0.5 -> D2MBadgeTone.INFO
        else -> D2MBadgeTone.WARNING
    }
    D2MBadge(text = "${formatScore(score)}/${formatScore(maxScore)}", tone = tone)
}

private fun formatScore(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else (kotlin.math.round(v * 10) / 10).toString()
