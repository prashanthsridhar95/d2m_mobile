package com.d2m.app.domain.repository

import com.d2m.app.data.cache.ApiCache
import com.d2m.app.data.model.*
import com.d2m.app.data.network.BufferApi
import com.d2m.app.data.network.SuggestionsApi

class SuggestionsRepository(
    private val api: SuggestionsApi,
    private val bufferApi: BufferApi,
    private val cache: ApiCache,
) {
    suspend fun getCandidate(primaryId: String, candidateId: String): SuggestionOut =
        api.getCandidate(primaryId, candidateId)

    suspend fun getSuggestions(primaryId: String, forceRefresh: Boolean = false): List<SuggestionOut> {
        val key = "suggestions:$primaryId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key, ttlMillis = 60_000) { api.getSuggestions(primaryId) }.value
    }

    suspend fun getBrowseAll(primaryId: String, forceRefresh: Boolean = false): List<BrowseCandidateOut> {
        val key = "browse-all:$primaryId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key) { api.getBrowseAll(primaryId) }.value
    }

    suspend fun getReceivedRequests(primaryId: String): List<SuggestionOut> =
        cache.get("requests-received:$primaryId", ttlMillis = 30_000) { api.getReceivedRequests(primaryId) }.value

    suspend fun getSentRequests(primaryId: String): List<SuggestionOut> =
        cache.get("requests-sent:$primaryId", ttlMillis = 30_000) { api.getSentRequests(primaryId) }.value

    suspend fun getShortlist(primaryId: String): List<SuggestionOut> =
        cache.get("shortlist:$primaryId") { api.getShortlist(primaryId) }.value

    suspend fun addToShortlist(primaryId: String, candidateId: String): SuggestionOut {
        val result = api.addToShortlist(primaryId, candidateId)
        cache.invalidateKey("shortlist:$primaryId")
        return result
    }

    suspend fun removeFromShortlist(primaryId: String, candidateId: String) {
        api.removeFromShortlist(primaryId, candidateId)
        cache.invalidateKey("shortlist:$primaryId")
    }

    suspend fun suggestToChild(sponsorId: String, candidateId: String): SuggestToChildOut =
        api.suggestToChild(sponsorId, candidateId)

    /** Accept/Reject/Snooze -- invalidates every list this action could change the shape of. */
    suspend fun act(primaryId: String, candidateId: String, action: String): SuggestionActionResponse {
        val result = bufferApi.act(primaryId, candidateId, action)
        cache.invalidate("suggestions:$primaryId")
        cache.invalidate("requests-received:$primaryId")
        cache.invalidate("requests-sent:$primaryId")
        cache.invalidate("dashboard:")
        return result
    }
}
