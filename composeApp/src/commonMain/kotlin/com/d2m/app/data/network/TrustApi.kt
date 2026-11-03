package com.d2m.app.data.network

import com.d2m.app.data.model.*

private const val STEP_UP_HEADER = "X-Step-Up-Token"

/** Mirrors the /trust/... routes in app/routers/trust.py -- Round 1 slice
 *  only (vouches, trusted connections, endorsements, plus the shared
 *  step-up re-auth call). Every mutating call here takes a stepUpToken
 *  obtained from reauthenticate() moments earlier (see
 *  ui/components/StepUpConfirmDialog.kt) and forwards it as the
 *  X-Step-Up-Token header WedLock itself checks -- D2M's router doesn't
 *  validate it, only passes it through (see trust_bridge_service.py's doc
 *  comment: "this IS the feature, there's no D2M-side fallback").
 *
 *  Vouch/endorsement routes take D2M's own primary_id (matching every
 *  other /primaries/{id}/... route in this app); trusted-connection routes
 *  are account-level and take no primary_id at all. */
class TrustApi(private val api: ApiClient) {

    // -- Step-up re-auth --------------------------------------------------

    suspend fun reauthenticate(password: String): StepUpTokenOut =
        api.post("/trust/reauthenticate", ReauthenticateIn(password))

    // -- Vouches ------------------------------------------------------------

    suspend fun createVouch(primaryId: String, claimScopes: List<String>, visibility: String, stepUpToken: String): VouchOut =
        api.post(
            "/trust/primaries/$primaryId/vouches",
            VouchCreateIn(claimScopes, visibility),
            extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken),
        )

    suspend fun listVouches(primaryId: String): List<VouchOut> =
        api.get("/trust/primaries/$primaryId/vouches")

    suspend fun approveVouch(vouchId: String, stepUpToken: String): VouchOut =
        api.post("/trust/vouches/$vouchId/approve", extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken))

    suspend fun declineVouch(vouchId: String, stepUpToken: String): VouchOut =
        api.post("/trust/vouches/$vouchId/decline", extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken))

    suspend fun withdrawVouch(vouchId: String, stepUpToken: String): VouchOut =
        api.post("/trust/vouches/$vouchId/withdraw", extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken))

    // Account-scoped -- every vouch the caller's own account has given,
    // across every profile. The "Given" half of the Vouches tab's split.
    suspend fun listGivenVouches(): List<VouchOut> =
        api.get("/trust/vouches/given")

    // Viewer-agnostic candidate-card badge -- not require_owns_primary,
    // deliberately viewable on someone else's profile.
    suspend fun getPublicTrustSummary(primaryId: String): TrustSummaryOut =
        api.get("/trust/primaries/$primaryId/public-trust-summary")

    // The click-through from that badge: every approved, match-visible
    // vouch on this profile, each with a real voucherName.
    suspend fun listPublicVouches(primaryId: String): List<VouchOut> =
        api.get("/trust/primaries/$primaryId/vouches/public")

    // Takes the raw password, not a stepUpToken -- see
    // TrustRepository.unlinkFamily's own doc comment for why.
    suspend fun unlinkFamily(primaryId: String, password: String): MessageOut =
        api.delete("/trust/primaries/$primaryId/family-link", body = UnlinkFamilyIn(password))

    // -- Trusted connections --------------------------------------------------

    suspend fun createConnectionInvite(connectionType: String, expiresInHours: Int, stepUpToken: String): ConnectionOut =
        api.post(
            "/trust/connections/invites",
            ConnectionInviteCreateIn(connectionType, expiresInHours),
            extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken),
        )

    suspend fun acceptConnectionInvite(inviteToken: String, stepUpToken: String): ConnectionOut =
        api.post(
            "/trust/connections/accept",
            ConnectionAcceptIn(inviteToken),
            extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken),
        )

    suspend fun listConnections(): List<ConnectionOut> =
        api.get("/trust/connections")

    suspend fun revokeConnection(connectionId: String, stepUpToken: String): MessageOut =
        api.delete("/trust/connections/$connectionId", extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken))

    // -- Endorsements -----------------------------------------------------

    suspend fun createEndorsement(
        primaryId: String,
        trustedConnectionId: String,
        claimScope: String,
        visibility: String,
        stepUpToken: String,
    ): EndorsementOut =
        api.post(
            "/trust/primaries/$primaryId/endorsements",
            EndorsementCreateIn(trustedConnectionId, claimScope, visibility),
            extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken),
        )

    suspend fun listEndorsements(primaryId: String): List<EndorsementOut> =
        api.get("/trust/primaries/$primaryId/endorsements")

    suspend fun approveEndorsement(endorsementId: String, stepUpToken: String): EndorsementOut =
        api.post("/trust/endorsements/$endorsementId/approve", extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken))

    suspend fun declineEndorsement(endorsementId: String, stepUpToken: String): EndorsementOut =
        api.post("/trust/endorsements/$endorsementId/decline", extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken))

    suspend fun withdrawEndorsement(endorsementId: String, stepUpToken: String): EndorsementOut =
        api.post("/trust/endorsements/$endorsementId/withdraw", extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken))

    // -- Recommendations (Round 2) -------------------------------------------

    suspend fun requestRecommendation(primaryId: String, trustedConnectionId: String, visibility: String, stepUpToken: String): RecommendationOut =
        api.post(
            "/trust/primaries/$primaryId/recommendations/requests",
            RecommendationRequestCreateIn(trustedConnectionId, visibility),
            extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken),
        )

    suspend fun submitRecommendation(
        recommendationId: String,
        confirmedScopes: List<String>,
        recommendationText: String,
        knownSinceYear: Int?,
        stepUpToken: String,
    ): RecommendationOut =
        api.post(
            "/trust/recommendations/$recommendationId/submit",
            RecommendationSubmitIn(confirmedScopes, recommendationText, knownSinceYear),
            extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken),
        )

    suspend fun listRecommendations(primaryId: String): List<RecommendationOut> =
        api.get("/trust/primaries/$primaryId/recommendations")

    suspend fun approveRecommendation(recommendationId: String, stepUpToken: String): RecommendationOut =
        api.post("/trust/recommendations/$recommendationId/approve", extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken))

    suspend fun hideRecommendation(recommendationId: String, stepUpToken: String): RecommendationOut =
        api.post("/trust/recommendations/$recommendationId/hide", extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken))

    suspend fun withdrawRecommendation(recommendationId: String, stepUpToken: String): RecommendationOut =
        api.post("/trust/recommendations/$recommendationId/withdraw", extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken))

    // -- External references (Round 2, authenticated side only) --------------

    suspend fun createExternalReference(
        primaryId: String,
        contact: String,
        relationshipType: String,
        expiresInHours: Int,
        stepUpToken: String,
    ): ExternalReferenceOut =
        api.post(
            "/trust/primaries/$primaryId/external-references",
            ExternalReferenceCreateIn(contact, relationshipType, expiresInHours),
            extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken),
        )

    suspend fun listExternalReferences(primaryId: String): List<ExternalReferenceOut> =
        api.get("/trust/primaries/$primaryId/external-references")

    suspend fun approveExternalReference(referenceId: String, stepUpToken: String): ExternalReferenceOut =
        api.post("/trust/external-references/$referenceId/approve", extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken))

    suspend fun declineExternalReference(referenceId: String, stepUpToken: String): ExternalReferenceOut =
        api.post("/trust/external-references/$referenceId/decline", extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken))

    suspend fun hideExternalReference(referenceId: String, stepUpToken: String): ExternalReferenceOut =
        api.post("/trust/external-references/$referenceId/hide", extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken))

    suspend fun withdrawExternalReference(referenceId: String, stepUpToken: String): ExternalReferenceOut =
        api.post("/trust/external-references/$referenceId/withdraw", extraHeaders = mapOf(STEP_UP_HEADER to stepUpToken))
}
