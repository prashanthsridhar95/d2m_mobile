package com.d2m.app.data.model

import kotlinx.serialization.Serializable

/**
 * Round-1 slice of WedLock's trust subsystem (app/routers/trust.py):
 * vouches, trusted connections, endorsements, plus the step-up re-auth
 * envelope every mutating call in this subsystem needs. Every id below is
 * String -- these responses are WedLock's own (D2M's trust router is a
 * thin pass-through, see that file's doc comment), and this app already
 * treats every cross-service id as String regardless of the DB column type
 * on the other side (see ShareLinkOut.id, ConsentRequestOut.id, etc.) --
 * namingStrategy = SnakeCase (ApiClient.kt) handles claim_scopes ->
 * claimScopes etc without per-field @SerialName.
 */

/** POST /trust/reauthenticate body. */
@Serializable
data class ReauthenticateIn(val password: String)

/** POST /trust/reauthenticate response -- the step_up_token is short-lived
 *  (5 min) and must be attached as X-Step-Up-Token on the mutating trust
 *  call it was obtained for. See ui/components/StepUpConfirmDialog.kt. */
@Serializable
data class StepUpTokenOut(val stepUpToken: String)

/** WedLock's plain {"message": "..."} envelope -- DELETE
 *  /trust/connections/{id} returns this, not a ConnectionOut (confirmed
 *  against a live response: {"message": "Trusted connection revoked"},
 *  no id/connectionType/status at all). */
@Serializable
data class MessageOut(val message: String)

/** DELETE /trust/primaries/{id}/family-link body -- the raw password, not
 * a pre-minted step-up token (see TrustRepository.unlinkFamily's own
 * doc comment for why). */
@Serializable
data class UnlinkFamilyIn(val password: String)

// -- Vouches ------------------------------------------------------------

/** POST /trust/primaries/{id}/vouches body. claimScopes: any of
 *  IDENTITY | FAMILY_DETAILS | PROFILE_ACCURACY. visibility: PRIVATE | MATCHES_ONLY. */
@Serializable
data class VouchCreateIn(
    val claimScopes: List<String>,
    val visibility: String = "PRIVATE",
)

@Serializable
data class VouchOut(
    val id: String,
    val profileId: String? = null,
    val accountLinkId: String? = null,
    val voucherAccountId: String? = null,
    val subjectAccountId: String? = null,
    val claimScopes: List<String> = emptyList(),
    val status: String,
    val visibility: String? = null,
    val createdAt: String? = null,
    val approvedAt: String? = null,
    val withdrawnAt: String? = null,
    // GET /vouches/given only -- the D2M-resolved candidate this vouch
    // was given on (see trust_bridge_service.list_given_vouches's own
    // comment for why this needs resolving at all: WedLock's profile_id
    // is an internal int with no human-readable identity attached).
    val subjectPrimaryId: String? = null,
    val subjectName: String? = null,
    // GET /primaries/{id}/vouches/public only -- the D2M-resolved voucher
    // identity (trust_bridge_service.list_public_vouches).
    val voucherName: String? = null,
)

/** GET /primaries/{id}/public-trust-summary -- viewer-agnostic candidate-
 * card badge data. Counts/explanations only, never individual voucher
 * identities (see list_public_vouches, the click-through this feeds). */
@Serializable
data class TrustSummaryOut(
    val profileId: Int? = null,
    val verifiedLevels: List<String> = emptyList(),
    val familyVouched: Boolean = false,
    val endorsementScopes: List<String> = emptyList(),
    val recommendationCount: Int = 0,
    val matchSignalsVisible: Boolean = true,
    val badgeExplanations: List<TrustBadgeExplanation> = emptyList(),
)

@Serializable
data class TrustBadgeExplanation(
    val badge: String,
    val reason: String,
)

// -- Trusted connections --------------------------------------------------

/** POST /trust/connections/invites body. connectionType: SIBLING | RELATIVE |
 *  FRIEND | COLLEAGUE | COMMUNITY_REFERENCE. */
@Serializable
data class ConnectionInviteCreateIn(
    val connectionType: String,
    val expiresInHours: Int = 72,
)

@Serializable
data class ConnectionAcceptIn(val inviteToken: String)

@Serializable
data class ConnectionOut(
    val id: String,
    val requesterAccountId: String? = null,
    val recipientAccountId: String? = null,
    val connectionType: String,
    val status: String,
    val expiresAt: String? = null,
    val acceptedAt: String? = null,
    val revokedAt: String? = null,
    val createdAt: String? = null,
    // Only ever populated on the invite that was just created -- WedLock's
    // own one-time-share semantics (see the router's doc comment); absent
    // on every other list/accept/revoke response.
    val inviteToken: String? = null,
)

// -- Endorsements -----------------------------------------------------

/** POST /trust/primaries/{id}/endorsements body. claimScope: EDUCATION |
 *  OCCUPATION | PROFILE_ACCURACY | KNOWN_PERSONALLY. visibility: PRIVATE | MATCHES_ONLY. */
@Serializable
data class EndorsementCreateIn(
    val trustedConnectionId: String,
    val claimScope: String,
    val visibility: String = "PRIVATE",
)

@Serializable
data class EndorsementOut(
    val id: String,
    val profileId: String? = null,
    val trustedConnectionId: String? = null,
    val endorserAccountId: String? = null,
    val subjectAccountId: String? = null,
    val claimScope: String,
    val status: String,
    val visibility: String? = null,
    val createdAt: String? = null,
    val approvedAt: String? = null,
    val withdrawnAt: String? = null,
)

// -- Recommendations (Round 2) -------------------------------------------
// Two-sided flow, same "request -> author acts -> owner decides" shape as
// endorsements: the profile OWNER requests one against an existing ACTIVE
// trusted connection (POST .../recommendations/requests, DRAFT), the OTHER
// party on that connection submits the actual text/scopes (POST
// /recommendations/{id}/submit, -> PENDING_OWNER_APPROVAL), then the owner
// approves/hides/withdraws it. Every field below live-curl-verified against
// WedLock (127.0.0.1:8010) round-tripped through this same D2M proxy --
// request -> submit -> approve -> hide all returned the full
// RecommendationRead object every time (no bare-message surprise like
// Round 1's connection-revoke), so no separate MessageOut variant is
// needed here. Ids arrive as JSON integers on the wire (e.g.
// "trusted_connection_id":2) -- isLenient in ApiClient.kt's Json config
// decodes those into these String fields the same way it already does for
// VouchOut/ConnectionOut/EndorsementOut above, and WedLock's own Pydantic
// side coerces a numeric-string trustedConnectionId back the other way on
// the way out (confirmed live), so encoding these as String costs nothing.

/** POST /trust/primaries/{id}/recommendations/requests body. Requester is
 *  always the profile owner; trustedConnectionId must be an ACTIVE
 *  connection's id (from the Connections tab). */
@Serializable
data class RecommendationRequestCreateIn(
    val trustedConnectionId: String,
    val visibility: String = "PRIVATE",
)

/** POST /trust/recommendations/{id}/submit body -- called by the OTHER
 *  party on the connection (the author), not the requester. confirmedScopes:
 *  any of IDENTITY | EDUCATION | OCCUPATION | PROFILE_ACCURACY |
 *  KNOWN_PERSONALLY. recommendationText: 20-500 chars (WedLock-enforced). */
@Serializable
data class RecommendationSubmitIn(
    val confirmedScopes: List<String>,
    val recommendationText: String,
    val knownSinceYear: Int? = null,
)

@Serializable
data class RecommendationOut(
    val id: String,
    val profileId: String? = null,
    val trustedConnectionId: String? = null,
    val requestedByAccountId: String? = null,
    val authorAccountId: String? = null,
    val subjectAccountId: String? = null,
    val relationshipType: String? = null,
    val knownSinceYear: Int? = null,
    val confirmedScopes: List<String>? = null,
    val recommendationText: String? = null,
    val status: String,
    val visibility: String? = null,
    val createdAt: String? = null,
    val submittedAt: String? = null,
    val approvedAt: String? = null,
    val withdrawnAt: String? = null,
)

// -- External references (Round 2, authenticated side only) --------------
// The referee-facing OTP-verify/submit step is unauthenticated on WedLock's
// own side and is a web-only public page (d2m_web builds it) -- this app
// only ever creates/lists/approves/declines/hides/withdraws, never renders
// the referee's own form. inviteToken is populated ONLY on the create
// response (live-verified: the very next list call for the same reference
// came back with invite_token null) -- same one-time-share semantics as
// ConnectionOut.inviteToken above.

/** POST /trust/primaries/{id}/external-references body. contact: an email
 *  or phone for WedLock to send the referee's link to. relationshipType:
 *  free text, 2-32 chars (WedLock-enforced) -- not a closed enum like
 *  trusted-connection's connectionType. */
@Serializable
data class ExternalReferenceCreateIn(
    val contact: String,
    val relationshipType: String,
    val expiresInHours: Int = 72,
)

@Serializable
data class ExternalReferenceOut(
    val id: String,
    val profileId: String? = null,
    val relationshipType: String,
    val status: String,
    val expiresAt: String? = null,
    val knownSinceYear: Int? = null,
    val confirmedScopes: List<String>? = null,
    val recommendationText: String? = null,
    val createdAt: String? = null,
    val submittedAt: String? = null,
    val approvedAt: String? = null,
    val withdrawnAt: String? = null,
    // Only ever populated on the reference that was just created -- see
    // this section's doc comment above.
    val inviteToken: String? = null,
)
