package com.d2m.app.data.network

import com.d2m.app.data.model.*

/** Mirrors app/routers/consent.py -- the Consent Gateway. */
class ConsentApi(private val api: ApiClient) {

    suspend fun getConsentRequests(primaryId: String): List<ConsentRequestOut> =
        api.get("/primaries/$primaryId/consent-requests")

    suspend fun getCurrentProspect(sponsorId: String): ProspectCardOut =
        api.get("/sponsors/$sponsorId/prospect")

    suspend fun getProspectCard(sponsorId: String, prospectId: String): ProspectCardOut =
        api.get("/sponsors/$sponsorId/prospects/$prospectId/card")

    suspend fun requestAccess(sponsorId: String, prospectId: String): ConsentRequestOut =
        api.post("/sponsors/$sponsorId/prospects/$prospectId/request-access")

    suspend fun decide(primaryId: String, requestId: String, decision: String): ConsentRequestOut =
        api.post("/primaries/$primaryId/consent-requests/$requestId/decide", ConsentDecisionIn(decision))
}
