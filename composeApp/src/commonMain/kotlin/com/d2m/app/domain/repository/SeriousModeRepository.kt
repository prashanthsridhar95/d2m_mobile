package com.d2m.app.domain.repository

import com.d2m.app.data.cache.ApiCache
import com.d2m.app.data.model.*
import com.d2m.app.data.network.SeriousModeApi

class SeriousModeRepository(
    private val api: SeriousModeApi,
    private val cache: ApiCache,
) {
    suspend fun getThreads(primaryId: String, forceRefresh: Boolean = false): List<ThreadOut> {
        val key = "threads:$primaryId"
        if (forceRefresh) cache.invalidateKey(key)
        return cache.get(key, ttlMillis = 15_000) { api.getThreads(primaryId) }.value
    }

    suspend fun requestSeriousMode(primaryId: String, threadId: String, requestedBy: String): SeriousModeRequestOut {
        val r = api.requestSeriousMode(threadId, requestedBy)
        cache.invalidateKey("threads:$primaryId")
        return r
    }

    suspend fun respond(primaryId: String, requestId: String, decision: String): SeriousModeRespondOut {
        val r = api.respondToSeriousMode(requestId, primaryId, decision)
        cache.invalidateKey("threads:$primaryId")
        return r
    }

    suspend fun revoke(primaryId: String, threadId: String): SeriousModeRevokeOut {
        val r = api.revokeSeriousMode(threadId, primaryId)
        cache.invalidateKey("threads:$primaryId")
        return r
    }

    suspend fun unmatch(primaryId: String, threadId: String): UnmatchOut {
        val r = api.unmatch(threadId, primaryId)
        cache.invalidateKey("threads:$primaryId")
        return r
    }

    suspend fun getSponsorStatus(primaryId: String): SponsorStatusOut =
        cache.get("sponsor-status:$primaryId", ttlMillis = 30_000) { api.getSponsorStatus(primaryId) }.value
}
