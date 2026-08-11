package com.d2m.app.ui.components

import androidx.compose.runtime.Composable
import com.d2m.app.data.model.SubScoreBreakdown

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

private fun toneForLevel(level: String): D2MBadgeTone = when (level) {
    "Exceptional" -> D2MBadgeTone.SUCCESS
    "High" -> D2MBadgeTone.INFO
    "Medium" -> D2MBadgeTone.WARNING
    else -> D2MBadgeTone.NEUTRAL
}

/** Mirrors components/LevelPill.jsx -- a level word (Low/Medium/High/Exceptional) rendered as a toned pill. */
@Composable
fun D2MLevelPill(level: String) {
    D2MBadge(text = level, tone = toneForLevel(level))
}
