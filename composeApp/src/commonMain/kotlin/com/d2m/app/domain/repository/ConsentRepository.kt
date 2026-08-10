package com.d2m.app.domain.repository

import com.d2m.app.data.cache.ApiCache
import com.d2m.app.data.model.*
import com.d2m.app.data.network.ConsentApi

class ConsentRepository(
    private val api: ConsentApi,
    private val cache: ApiCache,
) {
    suspend fun getConsentRequests(primaryId: String): List<ConsentRequestOut> =
        cache.get("consent-requests:$primaryId", ttlMillis = 30_000) { api.getConsentRequests(primaryId) }.value

    suspend fun getCurrentProspect(sponsorId: String): ProspectCardOut =
        api.getCurrentProspect(sponsorId)

    suspend fun getProspectCard(sponsorId: String, prospectId: String): ProspectCardOut =
        api.getProspectCard(sponsorId, prospectId)

    suspend fun requestAccess(sponsorId: String, prospectId: String): ConsentRequestOut =
        api.requestAccess(sponsorId, prospectId)

    suspend fun decide(primaryId: String, requestId: String, decision: String): ConsentRequestOut {
        val r = api.decide(primaryId, requestId, decision)
        cache.invalidateKey("consent-requests:$primaryId")
        return r
    }
}
