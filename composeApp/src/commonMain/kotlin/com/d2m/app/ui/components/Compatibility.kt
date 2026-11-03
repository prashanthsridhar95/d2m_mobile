package com.d2m.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d2m.app.data.model.SubScoreBreakdown
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.strings.SharedComponentsStrings
import com.d2m.app.ui.theme.D2MStroke
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import com.d2m.app.ui.theme.d2m

/**
 * Direct Kotlin mirror of d2m_web's lib/compatibility.js -- turns the raw
 * SubScoreBreakdown numbers into the same qualitative labels the web app
 * shows instead of raw scores (Low/Medium/High per factor, Exceptional/
 * High/Medium/Low overall). See that file's doc comment for the full
 * rationale/threshold discussion -- kept in sync by hand, same as
 * Taxonomy.kt's relationship to taxonomy.py.
 */
private const val LOW_MEDIUM_CUTOFF = 40.0
private const val MEDIUM_HIGH_CUTOFF = 70.0

fun levelForScore(score: Double): String = when {
    score >= MEDIUM_HIGH_CUTOFF -> "High"
    score >= LOW_MEDIUM_CUTOFF -> "Medium"
    else -> "Low"
}

fun astrologicalCompatibility(scores: SubScoreBreakdown): String = levelForScore(scores.astrologyScore)

fun preferenceCompatibility(scores: SubScoreBreakdown): String {
    val avg = (scores.communityScore + scores.childScore + scores.parentScore) / 3.0
    val nudged = avg + scores.affinityAdjustment
    return levelForScore(nudged.coerceIn(0.0, 100.0))
}

private const val EXCEPTIONAL_CUTOFF = 80.0
private const val HIGH_CUTOFF = 60.0
private const val MEDIUM_CUTOFF = 40.0

fun overallRating(compositeScore: Double): String = when {
    compositeScore >= EXCEPTIONAL_CUTOFF -> "Exceptional"
    compositeScore >= HIGH_CUTOFF -> "High"
    compositeScore >= MEDIUM_CUTOFF -> "Medium"
    else -> "Low"
}

/** Maps levelForScore()/overallRating()'s internal English key (also what toneForLevel matches on) to its display label -- never shown directly, since D2MLevelPill/CompatibilityCard are the only render sites for a `level` string. */
fun levelLabel(level: String, strings: SharedComponentsStrings): String = when (level) {
    "Exceptional" -> strings.compatExceptional
    "High" -> strings.compatHigh
    "Medium" -> strings.compatMedium
    else -> strings.compatLow
}

private fun toneForLevel(level: String): D2MBadgeTone = when (level) {
    "Exceptional" -> D2MBadgeTone.SUCCESS
    "High" -> D2MBadgeTone.INFO
    "Medium" -> D2MBadgeTone.WARNING
    else -> D2MBadgeTone.NEUTRAL
}

/**
 * The level word's own colour -- the foreground half of whichever status
 * tone toneForLevel() maps it to. Exposed (not private) because MatchCard's
 * compact row uses the exact same colour for its left accent bar: a High
 * row's bar and a High row's pill should never disagree about what "High"
 * looks like.
 */
@Composable
fun levelColor(level: String): Color {
    val palette = LocalD2MStatusPalette.current
    return when (toneForLevel(level)) {
        D2MBadgeTone.SUCCESS -> palette.success.fg
        D2MBadgeTone.INFO -> palette.info.fg
        D2MBadgeTone.WARNING -> palette.warning.fg
        else -> palette.neutral.fg
    }
}

/** Mirrors components/LevelPill.jsx -- a level word (Low/Medium/High/Exceptional) rendered as a toned pill. */
@Composable
fun D2MLevelPill(level: String) {
    val strings = LocalStrings.current.sharedComponents
    D2MBadge(text = levelLabel(level, strings), tone = toneForLevel(level))
}

enum class CompatibilityCardSize { Large, Small }

/** A level word as a solid-tone chip (web's LevelPill): [large] is the card's Overall rating, the small one is each breakdown entry. */
@Composable
private fun LevelChip(level: String?, large: Boolean) {
    val strings = LocalStrings.current.sharedComponents
    val palette = LocalD2MStatusPalette.current
    val tone = level?.let { toneForLevel(it) } ?: D2MBadgeTone.NEUTRAL
    val colors = when (tone) {
        D2MBadgeTone.SUCCESS -> palette.success
        D2MBadgeTone.INFO -> palette.info
        D2MBadgeTone.WARNING -> palette.warning
        else -> palette.neutral
    }
    Text(
        text = (level?.let { levelLabel(it, strings) } ?: strings.notYetCompared).uppercase(),
        color = colors.fg,
        style = if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        modifier = Modifier
            .background(colors.bg, androidx.compose.foundation.shape.RoundedCornerShape(if (large) 8.dp else 6.dp))
            .padding(horizontal = if (large) 14.dp else 9.dp, vertical = if (large) 7.dp else 3.dp),
    )
}

/**
 * Mirrors web's CompatibilityCard.jsx in both of its shapes.
 *
 * Small (child Discover / child profile): a coloured 3dp edge on the left (the overall level's colour); on the left the
 * "Overall compatibility" label over a large level chip, a hairline rule, and on the right each breakdown entry as a
 * label over a small level chip, stacked.
 *
 * Large (parent profile): "Overall compatibility", the level as a 22sp display word in its colour, ONE hairline, then the
 * breakdown as equal columns split by vertical rules, each a 16sp word -- web dropped the old double rule as "one divider
 * too many".
 *
 * No title on the card itself; the label carries the identification.
 */
@Composable
fun CompatibilityCard(
    level: String?,
    breakdown: List<Pair<String, String?>> = emptyList(),
    size: CompatibilityCardSize = CompatibilityCardSize.Large,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current.sharedComponents
    if (level == null) {
        SectionCard(modifier = modifier) {
            Text(strings.notScoredYet, style = MaterialTheme.typography.bodyMedium, color = d2m.meta)
        }
        return
    }

    if (size == CompatibilityCardSize.Small) {
        D2MCard(modifier = modifier) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(Modifier.width(3.dp).fillMaxHeight().background(levelColor(level)))
                Row(Modifier.weight(1f).padding(18.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(
                        Modifier.weight(1f).padding(end = 20.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp),
                    ) {
                        LabelText(strings.overallCompatibility)
                        LevelChip(level, large = true)
                    }
                    if (breakdown.isNotEmpty()) {
                        Box(Modifier.width(D2MStroke.hairline).fillMaxHeight().background(d2m.borderSoft))
                        Column(
                            Modifier.weight(1f).padding(start = 20.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                        ) {
                            breakdown.forEach { (label, value) ->
                                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
                                    LabelText(label)
                                    LevelChip(value, large = false)
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    SectionCard(modifier = modifier, padding = 24.dp) {
        LabelText(strings.overallCompatibility)
        Text(
            levelLabel(level, strings),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, lineHeight = 25.sp),
            color = levelColor(level),
            modifier = Modifier.padding(top = 6.dp),
        )

        if (breakdown.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            D2MDivider(soft = false)
            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                breakdown.forEachIndexed { index, (label, value) ->
                    if (index > 0) {
                        Box(Modifier.fillMaxHeight().width(D2MStroke.hairline).background(d2m.borderSoft))
                        Spacer(Modifier.width(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        LabelText(label)
                        Text(
                            value?.let { levelLabel(it, strings) } ?: "—",
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 16.sp, lineHeight = 18.sp),
                            color = value?.let { levelColor(it) } ?: d2m.faint,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    if (index < breakdown.lastIndex) Spacer(Modifier.width(20.dp))
                }
            }
        }
    }
}
