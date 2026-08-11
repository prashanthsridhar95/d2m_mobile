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
)
