package com.d2m.app.data.model

import kotlinx.serialization.Serializable

/** Mirrors app/models/serious_mode.py's Thread -- the match state machine (NOT chat content, see MessagingModels.kt). */
@Serializable
data class ThreadOut(
    val threadId: String,
    val otherParticipantId: String,
    val otherParticipantName: String,
    val status: String, // active | exclusive | sunsetting | closed
    val pendingSeriousModeRequest: SeriousModeRequestOut? = null,
    val sunsetExpiresAt: String? = null,
)

@Serializable
data class SeriousModeRequestIn(val requestedBy: String)

@Serializable
data class SeriousModeRequestOut(
    val requestId: String,
    val threadId: String,
    val requestedBy: String,
    val status: String, // pending | accepted | declined
)

@Serializable
data class SeriousModeRespondIn(val decision: String) // accept | decline

@Serializable
data class SeriousModeRespondOut(
    val requestId: String,
    val status: String,
    val sunsetThreadIds: List<String> = emptyList(),
)

@Serializable
data class SeriousModeRevokeIn(val reason: String? = null)

@Serializable
data class SeriousModeRevokeOut(val threadId: String, val status: String)

@Serializable
data class UnmatchIn(val reason: String? = null)

@Serializable
data class UnmatchOut(val threadId: String, val status: String)

@Serializable
data class SponsorStatusOut(
    val status: String, // no_active_threads | matched | serious_exploration
)

/** Mirrors SponsorPreferences (hard filters set at parent onboarding). */
@Serializable
data class SponsorPreferencesOut(
    val sponsorId: String,
    val ownReligion: String? = null,
    val ownCasteCommunity: String? = null,
    val acceptReligions: List<String> = emptyList(),
    val hardCasteCommunity: Boolean = false,
    val minAge: Int? = null,
    val maxAge: Int? = null,
    val acceptLocations: List<String> = emptyList(),
    val locationIsHardFilter: Boolean = false,
    val maritalStatusFilter: List<String> = emptyList(),
    val rankedCommunityList: List<String> = emptyList(),
)

/** Mirrors ChildPreferences (soft filters set by the child, ChildPreferencesRequest above is the write shape). */
@Serializable
data class ChildPreferencesOut(
    val primaryId: String,
    val minAge: Int,
    val maxAge: Int,
    val minHeightCm: Int? = null,
    val maxHeightCm: Int? = null,
    val educationPref: List<String> = emptyList(),
    val professionPref: List<String> = emptyList(),
    val hobbies: List<String> = emptyList(),
    val hobbiesPref: List<String> = emptyList(),
    val lifestyleTags: List<String> = emptyList(),
    val relationshipGoal: String,
)
