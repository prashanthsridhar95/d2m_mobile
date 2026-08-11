package com.d2m.app.data.model

import kotlinx.serialization.Serializable

/**
 * Mirrors app/schemas.py's SubScoreBreakdown exactly -- the nested scores
 * object every SuggestionOut carries and every BrowseCandidateOut optionally
 * carries. All six fields are required (non-Optional) on the backend when
 * this object is present at all; still given 0.0 defaults here so this type
 * can also be constructed client-side (e.g. ParentBrowseScreen.kt building a
 * lightweight SuggestionOut out of a BrowseCandidateOut) without needing to
 * fabricate values for fields that don't apply in that context.
 */
@Serializable
data class SubScoreBreakdown(
    val astrologyScore: Double = 0.0,
    val communityScore: Double = 0.0,
    val childScore: Double = 0.0,
    val parentScore: Double = 0.0,
    val affinityAdjustment: Double = 0.0,
    val compositeScore: Double = 0.0,
)

/**
 * Mirrors app/schemas.py's SuggestionOut exactly -- the ranked-discovery-feed
 * / detail card shape. Previous version of this file had drifted badly from
 * the real backend shape (wrong field name for the candidate's name, a flat
 * `name`/`status`/`nextWakeAt` that don't exist on the backend at all, and
 * five flat score fields where the backend actually nests them under one
 * required `scores: SubScoreBreakdown` object) -- every one of those was a
 * required-field mismatch that threw MissingFieldException on decode. Fixed
 * to match field-for-field; `rank`/`scores`/`doshaFlags`/`source` are
 * required on the backend but still given defaults here so this type stays
 * constructible client-side (see ParentBrowseScreen.kt, which builds one
 * from a BrowseCandidateOut to reuse MatchCard).
 */
@Serializable
data class SuggestionOut(
    val candidateId: String,
    val candidateName: String,
    val rank: Int = 0,
    val scores: SubScoreBreakdown = SubScoreBreakdown(),
    val doshaFlags: List<String> = emptyList(),
    val source: String = "", // "engine_suggested" | "sponsor_pushed"
    val photoUrl: String? = null,
    val age: Int? = null,
    val city: String? = null,
    val gothram: String? = null,
    val sect: String? = null,
    val nativity: String? = null,
    val occupationTitle: String? = null,
    val moonNakshatra: String? = null,
    val moonPada: Int? = null,
    val heightCm: Int? = null,
    val employer: String? = null,
    val financialStatus: String? = null,
    val complexion: String? = null,
    val isShortlisted: Boolean = false,
) {
    // Convenience flat accessors -- every UI call site (MatchCard,
    // DiscoveryScreen, ChildHomeScreen, ProfileDetailScreen, BrowseTable)
    // was originally written against the (wrong) flat shape this DTO used to
    // have. Kept as computed properties delegating to `scores` rather than
    // touching every call site, since the actual wire shape nests these.
    val compositeScore: Double? get() = scores.compositeScore
    val astrologyScore: Double? get() = scores.astrologyScore
    val communityScore: Double? get() = scores.communityScore
    val childScore: Double? get() = scores.childScore
    val parentScore: Double? get() = scores.parentScore
}

/**
 * Mirrors app/schemas.py's BrowseCandidateOut exactly -- the unfiltered "All
 * profiles" toggle's shape. Previously had `name` instead of the real
 * `candidate_name` key (required -- would crash), was missing `isMatching`
 * entirely (the field the backend uses to distinguish "in your ranked feed"
 * from "never scored" -- see that schema's own docstring), and had several
 * fields (heightCm etc.) that DO exist on the real backend response, so
 * those are kept.
 */
@Serializable
data class BrowseCandidateOut(
    val candidateId: String,
    val candidateName: String,
    val photoUrl: String? = null,
    val isMatching: Boolean = false,
    val scores: SubScoreBreakdown? = null,
    val doshaFlags: List<String> = emptyList(),
    val age: Int? = null,
    val city: String? = null,
    val gothram: String? = null,
    val sect: String? = null,
    val nativity: String? = null,
    val occupationTitle: String? = null,
    val moonNakshatra: String? = null,
    val moonPada: Int? = null,
    val heightCm: Int? = null,
    val employer: String? = null,
    val financialStatus: String? = null,
    val complexion: String? = null,
    val isShortlisted: Boolean = false,
)

@Serializable
data class SuggestionActionRequest(
    val action: String, // "accept" | "reject" | "snooze"
)

/**
 * Mirrors app/schemas.py's SuggestionActionResponse exactly -- was missing
 * three required fields (primaryId, snoozeCount, mutualMatch) entirely,
 * which would have crashed every Accept/Reject/Snooze action on
 * DiscoveryScreen the first time this response was actually decoded.
 */
@Serializable
data class SuggestionActionResponse(
    val primaryId: String,
    val candidateId: String,
    val status: String,
    val snoozeCount: Int,
    val mutualMatch: Boolean,
    val threadId: String? = null,
)

@Serializable
data class SuggestToChildIn(
    val candidateId: String,
)

@Serializable
data class SuggestToChildOut(
    val candidateId: String,
    val source: String,
)
