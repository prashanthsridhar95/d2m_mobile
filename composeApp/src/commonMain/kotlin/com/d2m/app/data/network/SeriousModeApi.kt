package com.d2m.app.data.network

import com.d2m.app.data.model.*

/** Mirrors app/routers/serious_mode.py -- threads / exclusivity state machine (not chat content). */
class SeriousModeApi(private val api: ApiClient) {

    suspend fun getThreads(primaryId: String): List<ThreadOut> =
        api.get("/primaries/$primaryId/threads")

    suspend fun requestSeriousMode(threadId: String, requestedBy: String): SeriousModeRequestOut =
        api.post("/threads/$threadId/serious-mode/request", SeriousModeRequestIn(requestedBy))

    suspend fun respondToSeriousMode(requestId: String, decision: String): SeriousModeRespondOut =
        api.post("/serious-mode-requests/$requestId/respond", SeriousModeRespondIn(decision))

    suspend fun revokeSeriousMode(threadId: String, reason: String? = null): SeriousModeRevokeOut =
        api.post("/threads/$threadId/serious-mode/revoke", SeriousModeRevokeIn(reason))

    suspend fun unmatch(threadId: String, reason: String? = null): UnmatchOut =
        api.post("/threads/$threadId/unmatch", UnmatchIn(reason))

    suspend fun getSponsorStatus(primaryId: String): SponsorStatusOut =
        api.get("/primaries/$primaryId/sponsor-status")
}
