package com.d2m.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SponsorChildOut(val primaryId: String)

/**
 * Mirrors app/schemas.py's DashboardCandidateOut -- a much lighter shape
 * than SuggestionOut (just enough for a dashboard preview tile). Previously
 * PrimaryDashboardOut/SponsorDashboardOut both typed their top_suggestions
 * field as List<SuggestionOut>, which doesn't match what the backend
 * actually returns there and would have crashed decoding every dashboard
 * load (SuggestionOut needs rank/scores/doshaFlags/source, none of which
 * DashboardCandidateOut carries).
 */
@Serializable
data class DashboardCandidateOut(
    val candidateId: String,
    val candidateName: String,
    val compositeScore: Double,
)

/** Mirrors app/schemas.py's PrimaryDashboardOut exactly. */
@Serializable
data class PrimaryDashboardOut(
    val profileCompleted: Boolean,
    val pendingSuggestionCount: Int,
    val topSuggestions: List<DashboardCandidateOut> = emptyList(),
    val threadStatus: String? = null,
    val unreadNotificationCount: Int,
    /** True once a union is confirmed: Discover/Browse give way to the wedding checklist. */
    val weddingMode: Boolean = false,
)

/**
 * Mirrors app/schemas.py's SponsorDashboardOut exactly -- previously had a
 * `topSuggestions: List<SuggestionOut>` field that doesn't exist on the real
 * backend response at all (the sponsor-facing dashboard only ever exposes a
 * count, `suggestionsSentCount`, never the actual suggestion objects -- see
 * that schema's own "sponsor-safe" comment), and was missing two required
 * fields (pendingConsentRequestCount, suggestionsSentCount) that would have
 * crashed decode. ParentHomeScreen.kt's "Suggested for you to review"
 * section now sources its cards from SuggestionsRepository.getSuggestions
 * directly instead of this type.
 */
@Serializable
data class SponsorDashboardOut(
    val childProfileCompleted: Boolean,
    // Mirrors dashboard.py's own "sponsor-safe" comment: never exposes thread
    // participants, message content, or scores -- just the state label.
    val childThreadStatus: String? = null,
    val pendingConsentRequestCount: Int,
    val unreadNotificationCount: Int,
    val suggestionsSentCount: Int,
    val weddingMode: Boolean = false,
)

/** Mirrors app/schemas.py's ParentMatchCheckIn -- POST /sponsors/{id}/match-check. Never stored server-side. */
@Serializable
data class ParentMatchCheckIn(
    val name: String?,
    val dob: String,
    val tob: String,
    val birthPlace: String,
    val birthLat: Double,
    val birthLon: Double,
    val birthTzOffsetHours: Double,
    val maritalStatus: String,
    val religion: String?,
    val casteCommunity: String?,
    val highestEducation: String?,
    val incomeBand: String?,
    val professionTier: String?,
    val tobIsExact: Boolean,
)

@Serializable
data class MatchCheckDosha(val name: String, val cancelled: Boolean = false)

@Serializable
data class MatchCheckAstrology(
    val score: Double,
    val tier: String,
    val confidence: String? = null,
    val dealbreakers: List<String> = emptyList(),
    val doshas: List<MatchCheckDosha> = emptyList(),
    val parts: Map<String, Double>? = null,
)

@Serializable
data class MatchCheckHardCheck(val key: String, val label: String, val ok: Boolean)

@Serializable
data class MatchCheckScores(val community: Double, val childPreferences: Double, val parentPreferences: Double)

@Serializable
data class MatchCheckPreferences(
    val hardChecks: List<MatchCheckHardCheck> = emptyList(),
    val hardRequirementsMet: Boolean,
    val scores: MatchCheckScores,
    val notProvided: List<String> = emptyList(),
)

/** Mirrors ParentMatchCheckOut. */
@Serializable
data class ParentMatchCheckOut(
    val childId: String,
    val candidateName: String? = null,
    val candidateAge: Int,
    val verdict: String,
    val compositeScore: Double,
    val astrology: MatchCheckAstrology,
    val preferences: MatchCheckPreferences,
    val stored: Boolean = false,
)
