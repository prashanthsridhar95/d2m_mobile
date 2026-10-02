package com.d2m.app.domain.repository

import com.d2m.app.data.cache.ApiCache
import com.d2m.app.data.model.ConnectionOut
import com.d2m.app.data.model.EndorsementOut
import com.d2m.app.data.model.ExternalReferenceOut
import com.d2m.app.data.model.MessageOut
import com.d2m.app.data.model.RecommendationOut
import com.d2m.app.data.model.TrustSummaryOut
import com.d2m.app.data.model.VouchOut
import com.d2m.app.data.network.TrustApi

/** Same cache-wrapper shape as ShareLinksRepository/ConsentRepository --
 *  short TTL (list state changes the moment someone approves/declines) with
 *  an explicit invalidate after every mutation. reauthenticate() itself is
 *  never cached (a step_up_token is single-purpose and short-lived). */
class TrustRepository(
    private val api: TrustApi,
    private val cache: ApiCache,
) {
    suspend fun reauthenticate(password: String): String = api.reauthenticate(password).stepUpToken

    // -- Vouches ------------------------------------------------------------

    suspend fun listVouches(primaryId: String, forceRefresh: Boolean = false): List<VouchOut> {
        val key = "trust-vouches:$primaryId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key, ttlMillis = 15_000) { api.listVouches(primaryId) }.value
    }

    suspend fun createVouch(primaryId: String, claimScopes: List<String>, visibility: String, stepUpToken: String): VouchOut {
        val r = api.createVouch(primaryId, claimScopes, visibility, stepUpToken)
        cache.invalidateKey("trust-vouches:$primaryId")
        return r
    }

    suspend fun approveVouch(primaryId: String, vouchId: String, stepUpToken: String): VouchOut {
        val r = api.approveVouch(vouchId, stepUpToken)
        cache.invalidateKey("trust-vouches:$primaryId")
        return r
    }

    suspend fun declineVouch(primaryId: String, vouchId: String, stepUpToken: String): VouchOut {
        val r = api.declineVouch(vouchId, stepUpToken)
        cache.invalidateKey("trust-vouches:$primaryId")
        return r
    }

    suspend fun withdrawVouch(primaryId: String, vouchId: String, stepUpToken: String): VouchOut {
        val r = api.withdrawVouch(vouchId, stepUpToken)
        cache.invalidateKey("trust-vouches:$primaryId")
        return r
    }

    // Account-scoped, not keyed by primaryId -- always the caller's own
    // given vouches, across every profile.
    suspend fun listGivenVouches(forceRefresh: Boolean = false): List<VouchOut> {
        val key = "trust-vouches-given"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key, ttlMillis = 15_000) { api.listGivenVouches() }.value
    }

    suspend fun getPublicTrustSummary(primaryId: String): TrustSummaryOut =
        cache.get("trust-summary:$primaryId", ttlMillis = 60_000) { api.getPublicTrustSummary(primaryId) }.value

    suspend fun listPublicVouches(primaryId: String): List<VouchOut> =
        api.listPublicVouches(primaryId)

    // Not cached, nothing to invalidate -- a destructive one-off action
    // (app/routers/trust.py::unlink_family), not a value read back
    // anywhere else. Takes the raw password, not a stepUpToken: WedLock
    // gates DELETE /links/{id} behind an action-bound reauthentication
    // proof that must be minted for the exact link_id, which D2M only
    // resolves server-side -- a pre-minted generic step-up token (the
    // reauthenticate() flow every other mutation here uses) can't satisfy
    // it. Confirmed by live testing against the real stack, not assumed
    // -- see app/services/trust_bridge_service.py::unlink_family's own
    // docstring on the d2m_core_engine side.
    suspend fun unlinkFamily(primaryId: String, password: String): MessageOut =
        api.unlinkFamily(primaryId, password)

    // -- Trusted connections --------------------------------------------------

    suspend fun listConnections(forceRefresh: Boolean = false): List<ConnectionOut> {
        val key = "trust-connections"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key, ttlMillis = 15_000) { api.listConnections() }.value
    }

    suspend fun createConnectionInvite(connectionType: String, expiresInHours: Int, stepUpToken: String): ConnectionOut {
        val r = api.createConnectionInvite(connectionType, expiresInHours, stepUpToken)
        cache.invalidateKey("trust-connections")
        return r
    }

    suspend fun acceptConnectionInvite(inviteToken: String, stepUpToken: String): ConnectionOut {
        val r = api.acceptConnectionInvite(inviteToken, stepUpToken)
        cache.invalidateKey("trust-connections")
        return r
    }

    suspend fun revokeConnection(connectionId: String, stepUpToken: String): MessageOut {
        val r = api.revokeConnection(connectionId, stepUpToken)
        cache.invalidateKey("trust-connections")
        return r
    }

    // -- Endorsements -----------------------------------------------------

    suspend fun listEndorsements(primaryId: String, forceRefresh: Boolean = false): List<EndorsementOut> {
        val key = "trust-endorsements:$primaryId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key, ttlMillis = 15_000) { api.listEndorsements(primaryId) }.value
    }

    suspend fun createEndorsement(
        primaryId: String,
        trustedConnectionId: String,
        claimScope: String,
        visibility: String,
        stepUpToken: String,
    ): EndorsementOut {
        val r = api.createEndorsement(primaryId, trustedConnectionId, claimScope, visibility, stepUpToken)
        cache.invalidateKey("trust-endorsements:$primaryId")
        return r
    }

    suspend fun approveEndorsement(primaryId: String, endorsementId: String, stepUpToken: String): EndorsementOut {
        val r = api.approveEndorsement(endorsementId, stepUpToken)
        cache.invalidateKey("trust-endorsements:$primaryId")
        return r
    }

    suspend fun declineEndorsement(primaryId: String, endorsementId: String, stepUpToken: String): EndorsementOut {
        val r = api.declineEndorsement(endorsementId, stepUpToken)
        cache.invalidateKey("trust-endorsements:$primaryId")
        return r
    }

    suspend fun withdrawEndorsement(primaryId: String, endorsementId: String, stepUpToken: String): EndorsementOut {
        val r = api.withdrawEndorsement(endorsementId, stepUpToken)
        cache.invalidateKey("trust-endorsements:$primaryId")
        return r
    }

    // -- Recommendations (Round 2) -------------------------------------------

    suspend fun listRecommendations(primaryId: String, forceRefresh: Boolean = false): List<RecommendationOut> {
        val key = "trust-recommendations:$primaryId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key, ttlMillis = 15_000) { api.listRecommendations(primaryId) }.value
    }

    suspend fun requestRecommendation(primaryId: String, trustedConnectionId: String, visibility: String, stepUpToken: String): RecommendationOut {
        val r = api.requestRecommendation(primaryId, trustedConnectionId, visibility, stepUpToken)
        cache.invalidateKey("trust-recommendations:$primaryId")
        return r
    }

    suspend fun submitRecommendation(
        primaryId: String,
        recommendationId: String,
        confirmedScopes: List<String>,
        recommendationText: String,
        knownSinceYear: Int?,
        stepUpToken: String,
    ): RecommendationOut {
        val r = api.submitRecommendation(recommendationId, confirmedScopes, recommendationText, knownSinceYear, stepUpToken)
        cache.invalidateKey("trust-recommendations:$primaryId")
        return r
    }

    suspend fun approveRecommendation(primaryId: String, recommendationId: String, stepUpToken: String): RecommendationOut {
        val r = api.approveRecommendation(recommendationId, stepUpToken)
        cache.invalidateKey("trust-recommendations:$primaryId")
        return r
    }

    suspend fun hideRecommendation(primaryId: String, recommendationId: String, stepUpToken: String): RecommendationOut {
        val r = api.hideRecommendation(recommendationId, stepUpToken)
        cache.invalidateKey("trust-recommendations:$primaryId")
        return r
    }

    suspend fun withdrawRecommendation(primaryId: String, recommendationId: String, stepUpToken: String): RecommendationOut {
        val r = api.withdrawRecommendation(recommendationId, stepUpToken)
        cache.invalidateKey("trust-recommendations:$primaryId")
        return r
    }

    // -- External references (Round 2, authenticated side only) --------------

    suspend fun listExternalReferences(primaryId: String, forceRefresh: Boolean = false): List<ExternalReferenceOut> {
        val key = "trust-external-references:$primaryId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key, ttlMillis = 15_000) { api.listExternalReferences(primaryId) }.value
    }

    suspend fun createExternalReference(
        primaryId: String,
        contact: String,
        relationshipType: String,
        expiresInHours: Int,
        stepUpToken: String,
    ): ExternalReferenceOut {
        val r = api.createExternalReference(primaryId, contact, relationshipType, expiresInHours, stepUpToken)
        cache.invalidateKey("trust-external-references:$primaryId")
        return r
    }

    suspend fun approveExternalReference(primaryId: String, referenceId: String, stepUpToken: String): ExternalReferenceOut {
        val r = api.approveExternalReference(referenceId, stepUpToken)
        cache.invalidateKey("trust-external-references:$primaryId")
        return r
    }

    suspend fun declineExternalReference(primaryId: String, referenceId: String, stepUpToken: String): ExternalReferenceOut {
        val r = api.declineExternalReference(referenceId, stepUpToken)
        cache.invalidateKey("trust-external-references:$primaryId")
        return r
    }

    suspend fun hideExternalReference(primaryId: String, referenceId: String, stepUpToken: String): ExternalReferenceOut {
        val r = api.hideExternalReference(referenceId, stepUpToken)
        cache.invalidateKey("trust-external-references:$primaryId")
        return r
    }

    suspend fun withdrawExternalReference(primaryId: String, referenceId: String, stepUpToken: String): ExternalReferenceOut {
        val r = api.withdrawExternalReference(referenceId, stepUpToken)
        cache.invalidateKey("trust-external-references:$primaryId")
        return r
    }
}
