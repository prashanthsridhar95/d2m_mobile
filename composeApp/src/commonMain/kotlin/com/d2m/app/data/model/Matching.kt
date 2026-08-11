package com.d2m.app.data.model

import kotlinx.serialization.Serializable

/**
 * Mirrors app/models/serious_mode.py's Thread -- the match state machine
 * (NOT chat content, see MessagingModels.kt). Previously embedded a nested
 * `pendingSeriousModeRequest: SeriousModeRequestOut?` object, but the real
 * backend response has no such nested object -- it's two flat optional
 * strings (pending_serious_mode_request_id/pending_serious_mode_requested_by).
 * Since that field was nullable it never crashed decode, but it also could
 * never populate from a real response, so the Accept/Decline Serious Mode
 * banner in MatchesScreen.kt was permanently dead. Fixed field-for-field.
 */
@Serializable
data class ThreadOut(
    val threadId: String,
    val otherParticipantId: String,
    val otherParticipantName: String,
    val status: String, // active | exclusive | sunsetting | closed
    val createdAt: String? = null,
    val pendingSeriousModeRequestId: String? = null,
    val pendingSeriousModeRequestedBy: String? = null,
)

@Serializable
data class SeriousModeRequestIn(val requestedBy: String)

/** Mirrors app/schemas.py's SeriousModeRequestOut -- previously had an extra required `requestedBy` field the real response never sends (only the *request* body has that), which would have crashed the "Go Serious" action. */
@Serializable
data class SeriousModeRequestOut(
    val requestId: String,
    val threadId: String,
    val status: String, // pending | accepted | declined
)

/** Mirrors app/schemas.py's SeriousModeRespondIn -- previously missing responderId, so respond_to_serious_mode's actual FastAPI/Pydantic validation (which requires it) would 422 on every Accept/Decline. */
@Serializable
data class SeriousModeRespondIn(val responderId: String, val decision: String) // accept | decline

/** Mirrors app/schemas.py's SeriousModeRespondOut -- was missing required threadId and had `status` instead of the real key `threadStatus`/`thread_status`. */
@Serializable
data class SeriousModeRespondOut(
    val requestId: String,
    val threadId: String,
    val threadStatus: String,
    val sunsetThreadIds: List<String> = emptyList(),
)

@Serializable
data class SeriousModeRevokeIn(val primaryId: String)

/** `status` renamed to `threadStatus` -- matches the real `thread_status` key; the old name never matched anything so this always silently decoded null (nullable) rather than crashing, but the Revoke action's resulting status was always lost. */
@Serializable
data class SeriousModeRevokeOut(val threadId: String, val threadStatus: String)

@Serializable
data class UnmatchIn(val primaryId: String)

/** Same `status` -> `threadStatus` rename as SeriousModeRevokeOut, same reason. */
@Serializable
data class UnmatchOut(val threadId: String, val threadStatus: String)

/** Mirrors app/schemas.py's SponsorStatusOut -- added the required primaryId field (previously absent; harmless since it was an ignored extra key, not a crash, but incomplete). */
@Serializable
data class SponsorStatusOut(
    val primaryId: String,
    val status: String, // no_active_threads | matched | serious_exploration
)

/** Mirrors SponsorPreferences (hard filters set at parent onboarding). Not currently wired to a live GET endpoint response -- kept for future use, field set already matches app/models identity SponsorPreferences 1:1. */
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

/** Mirrors app/schemas.py's PrimaryPreferencesOut (ChildPreferencesRequest read back). Not currently wired to a live GET endpoint response on its own -- it's nested inside PrimaryProfileOut.preferences on the backend, which this build doesn't decode yet -- kept for future use. */
@Serializable
data class ChildPreferencesOut(
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
