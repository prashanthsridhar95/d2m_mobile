package com.d2m.app.data.network

import com.d2m.app.data.model.SuggestionActionRequest
import com.d2m.app.data.model.SuggestionActionResponse

/** Mirrors app/routers/buffer.py -- Accept/Reject/Snooze on a candidate. */
class BufferApi(private val api: ApiClient) {
    suspend fun act(primaryId: String, candidateId: String, action: String): SuggestionActionResponse =
        api.post("/primaries/$primaryId/suggestions/$candidateId/action", SuggestionActionRequest(action))
}
