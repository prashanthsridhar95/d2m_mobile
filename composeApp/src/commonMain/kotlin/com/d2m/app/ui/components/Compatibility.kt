package com.d2m.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.PI
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

/**
 * Child-facing, Hinge-style readout of the composite score (mirror of d2m_web's compatibilityTier): a friendly message
 * per percentage tier instead of a level word. [level] maps the tier onto the existing level colours.
 */
data class CompatibilityTier(val key: String, val level: String, val emoji: String)

private val EXCELLENT = CompatibilityTier("excellent", "Exceptional", "❤️")
private val VERY_GOOD = CompatibilityTier("very_good", "High", "✨")
private val GOOD = CompatibilityTier("good", "High", "😉")
private val MODERATE = CompatibilityTier("moderate", "Medium", "😊")
private val UNEXPECTED = CompatibilityTier("unexpected", "Low", "🌟")

fun compatibilityTier(score: Double): CompatibilityTier = when {
    score >= 90.0 -> EXCELLENT
    score >= 75.0 -> VERY_GOOD
    score >= 60.0 -> GOOD
    score >= 40.0 -> MODERATE
    else -> UNEXPECTED
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
    /** Child view: the raw composite score. With [CompatibilityCardSize.Small] it switches the card to the tier message. */
    score: Double? = null,
) {
    val strings = LocalStrings.current.sharedComponents
    if (level == null) {
        SectionCard(modifier = modifier) {
            Text(strings.notScoredYet, style = MaterialTheme.typography.bodyMedium, color = d2m.meta)
        }
        return
    }

    if (size == CompatibilityCardSize.Small && score != null) {
        CompatibilityTierCard(compatibilityTier(score), modifier)
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

/**
 * The child's compatibility card: one self-contained surface (no title, no label, no score) -- an emoji badge that floats
 * and pulses a halo in a loop, the tier's friendly message as the headline, and a bright segment that travels around the
 * card's outline. All tinted from the tier's level colour. Mirror of web's `.dh-compat`.
 */
@Composable
private fun CompatibilityTierCard(tier: CompatibilityTier, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current.sharedComponents
    val accent = levelColor(tier.level)
    val message = strings.tierMessage(tier.key)
    val shape = RoundedCornerShape(CARD_RADIUS)
    val transition = rememberInfiniteTransition(label = "compatTier")
    val orbit by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 4500, easing = LinearEasing)),
        label = "orbit",
    )
    val bob by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    Row(
        modifier = modifier
            .semantics { contentDescription = "${strings.tierName(tier.key)}: $message" }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent)))
            .border(D2MStroke.hairline, accent.copy(alpha = 0.32f), shape)
            .drawWithContent {
                drawContent()
                // The travelling segment: a dash as long as ~22% of the outline, the rest a gap, with the phase advancing.
                val sw = 1.5.dp.toPx()
                val w = size.width - sw
                val h = size.height - sw
                val r = CARD_RADIUS.toPx().coerceAtMost(minOf(w, h) / 2f)
                val perimeter = 2f * (w + h) - (8f - 2f * PI.toFloat()) * r
                val seg = perimeter * 0.22f
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(sw / 2f, sw / 2f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(
                        width = sw,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(seg, perimeter - seg), phase = -orbit * perimeter),
                    ),
                )
            }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(54.dp)
                .drawBehind {
                    // Halo: breathes with the float.
                    drawCircle(accent.copy(alpha = 0.08f + 0.06f * bob), radius = size.minDimension / 2f + (5.dp + 5.dp * bob).toPx())
                }
                .graphicsLayer {
                    translationY = -3.dp.toPx() * bob
                    scaleX = 1f + 0.06f * bob
                    scaleY = 1f + 0.06f * bob
                }
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f))
                .border(D2MStroke.hairline, accent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(tier.emoji, fontSize = 26.sp)
        }
        Spacer(Modifier.width(16.dp))
        Text(
            message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp, lineHeight = 25.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
        )
    }
}

private val CARD_RADIUS = 14.dp
