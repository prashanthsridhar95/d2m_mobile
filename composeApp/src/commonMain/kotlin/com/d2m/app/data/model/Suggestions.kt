package com.d2m.app.data.model

import kotlinx.serialization.Serializable

/** Mirrors app/schemas.py's SuggestionOut -- the ranked-discovery-feed / detail card shape. */
@Serializable
data class SuggestionOut(
    val candidateId: String,
    val name: String,
    val age: Int? = null,
    val city: String? = null,
    val occupationTitle: String? = null,
    val gothram: String? = null,
    val sect: String? = null,
    val moonNakshatra: String? = null,
    val moonPada: Int? = null,
    val photoUrl: String? = null,
    val compositeScore: Double? = null,
    val astrologyScore: Double? = null,
    val communityScore: Double? = null,
    val childScore: Double? = null,
    val parentScore: Double? = null,
    val status: String? = null,
    val source: String? = null,
    val isShortlisted: Boolean = false,
    val nextWakeAt: String? = null,
)

/** Mirrors BrowseCandidateOut -- the unfiltered "All profiles" toggle's shape (superset-ish of SuggestionOut). */
@Serializable
data class BrowseCandidateOut(
    val candidateId: String,
    val name: String,
    val age: Int? = null,
    val city: String? = null,
    val occupationTitle: String? = null,
    val gothram: String? = null,
    val sect: String? = null,
    val moonNakshatra: String? = null,
    val photoUrl: String? = null,
    val heightCm: Int? = null,
    val employer: String? = null,
    val financialStatus: String? = null,
    val complexion: String? = null,
    val scores: ScoreBlock? = null,
)

@Serializable
data class ScoreBlock(
    val compositeScore: Double? = null,
    val astrologyScore: Double? = null,
    val communityScore: Double? = null,
    val childScore: Double? = null,
    val parentScore: Double? = null,
)

@Serializable
data class SuggestionActionRequest(
    val action: String, // "accept" | "reject" | "snooze"
)

@Serializable
data class SuggestionActionResponse(
    val candidateId: String,
    val status: String,
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
