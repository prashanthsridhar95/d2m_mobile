package com.d2m.app.data.network

import com.d2m.app.data.model.*

/** Mirrors app/routers/suggestions.py -- discovery feed / browse-all / requests / shortlist. */
class SuggestionsApi(private val api: ApiClient) {

    suspend fun getCandidate(primaryId: String, candidateId: String): SuggestionOut =
        api.get("/primaries/$primaryId/candidates/$candidateId")

    suspend fun getSuggestions(primaryId: String): List<SuggestionOut> =
        api.get("/primaries/$primaryId/suggestions")

    suspend fun getBrowseAll(primaryId: String): List<BrowseCandidateOut> =
        api.get("/primaries/$primaryId/browse-all")

    suspend fun getReceivedRequests(primaryId: String): List<SuggestionOut> =
        api.get("/primaries/$primaryId/requests/received")

    suspend fun getSentRequests(primaryId: String): List<SuggestionOut> =
        api.get("/primaries/$primaryId/requests/sent")

    suspend fun getShortlist(primaryId: String): List<SuggestionOut> =
        api.get("/primaries/$primaryId/shortlist")

    suspend fun addToShortlist(primaryId: String, candidateId: String): SuggestionOut =
        api.post("/primaries/$primaryId/shortlist/$candidateId")

    suspend fun removeFromShortlist(primaryId: String, candidateId: String) {
        api.delete<Map<String, Boolean>>("/primaries/$primaryId/shortlist/$candidateId")
    }

    suspend fun suggestToChild(sponsorId: String, candidateId: String): SuggestToChildOut =
        api.post("/sponsors/$sponsorId/suggest", SuggestToChildIn(candidateId))
}
