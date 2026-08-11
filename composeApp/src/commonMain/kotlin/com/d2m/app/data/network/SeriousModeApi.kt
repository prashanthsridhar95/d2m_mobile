package com.d2m.app.data.network

import com.d2m.app.data.model.*

/** Mirrors app/routers/serious_mode.py -- threads / exclusivity state machine (not chat content). */
class SeriousModeApi(private val api: ApiClient) {

    suspend fun getThreads(primaryId: String): List<ThreadOut> =
        api.get("/primaries/$primaryId/threads")

    suspend fun requestSeriousMode(threadId: String, requestedBy: String): SeriousModeRequestOut =
        api.post("/threads/$threadId/serious-mode/request", SeriousModeRequestIn(requestedBy))

    // req.responder_id is required by the backend's respond_to_request call
    // (used for an authorization/symmetry check, same as revoke/unmatch's
    // primary_id below) -- previously never sent at all, so every real
    // Accept/Decline Serious Mode request 422'd against Pydantic validation.
    suspend fun respondToSeriousMode(requestId: String, responderId: String, decision: String): SeriousModeRespondOut =
        api.post("/serious-mode-requests/$requestId/respond", SeriousModeRespondIn(responderId, decision))

    // Same bug as respondToSeriousMode above -- SeriousModeRevokeIn/UnmatchIn
    // both require primary_id server-side (see routers/serious_mode.py's
    // revoke_serious_mode/unmatch, which pass req.primary_id straight into
    // the service call), not a free-text "reason" (which the backend schema
    // never had in the first place).
    suspend fun revokeSeriousMode(threadId: String, primaryId: String): SeriousModeRevokeOut =
        api.post("/threads/$threadId/serious-mode/revoke", SeriousModeRevokeIn(primaryId))

    suspend fun unmatch(threadId: String, primaryId: String): UnmatchOut =
        api.post("/threads/$threadId/unmatch", UnmatchIn(primaryId))

    suspend fun getSponsorStatus(primaryId: String): SponsorStatusOut =
        api.get("/primaries/$primaryId/sponsor-status")
}
