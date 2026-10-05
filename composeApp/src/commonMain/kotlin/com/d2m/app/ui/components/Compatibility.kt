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

/**
 * Mirrors web's CompatibilityCard.jsx (its own doc comment there has the
 * full seven-round history) -- Overall as a big colour word, a double
 * hairline rule (deliberately heavier than the single rule D2MDivider
 * draws everywhere else, marking this as the one section-within-a-section
 * on the card), then the breakdown as N columns split by vertical rules,
 * each with the same word-not-pill treatment as Overall, just smaller.
 *
 * No title on the card itself -- "leave off the Compatibility title, make
 * it 'Overall compatibility'" (reported directly on web) -- the label
 * above the word carries the card's whole identification.
 *
 * `size` exists because this card is used two ways: full width, on its
 * own row (ProfileDetailScreen, DiscoveryScreen) wants Large; nothing on
 * mobile currently needs Small, but the param exists so a narrower future
 * placement doesn't need a second component, same reasoning as web's own
 * `size` prop.
 */
@Composable
fun CompatibilityCard(
    level: String?,
    breakdown: List<Pair<String, String?>> = emptyList(),
    size: CompatibilityCardSize = CompatibilityCardSize.Large,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current.sharedComponents
    SectionCard(modifier = modifier) {
        if (level == null) {
            Text(
                strings.notScoredYet,
                style = MaterialTheme.typography.bodyMedium,
                color = d2m.meta,
            )
            return@SectionCard
        }

        val overallStyle = if (size == CompatibilityCardSize.Large) {
            MaterialTheme.typography.headlineMedium
        } else {
            MaterialTheme.typography.titleLarge
        }

        LabelText(strings.overallCompatibility)
        Text(levelLabel(level, strings), style = overallStyle, color = levelColor(level), modifier = Modifier.padding(top = 4.dp))

        if (breakdown.isNotEmpty()) {
            Column(Modifier.padding(top = 16.dp)) {
                D2MDivider(soft = false)
                Spacer(Modifier.height(3.dp))
                D2MDivider(soft = false)
            }

            val subStyle = if (size == CompatibilityCardSize.Large) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.titleMedium
            }

            Row(Modifier.fillMaxWidth().padding(top = 16.dp).height(IntrinsicSize.Min)) {
                breakdown.forEachIndexed { index, (label, value) ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .width(D2MStroke.hairline)
                                .padding(vertical = 1.dp)
                                .background(d2m.borderSoft),
                        )
                        Spacer(Modifier.width(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        LabelText(label)
                        Text(
                            value?.let { levelLabel(it, strings) } ?: "—",
                            style = subStyle,
                            color = value?.let { levelColor(it) } ?: d2m.faint,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (index < breakdown.lastIndex) Spacer(Modifier.width(20.dp))
                }
            }
        }
    }
}
