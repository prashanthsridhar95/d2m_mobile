package com.d2m.app.data.network

import com.d2m.app.data.model.SuggestionActionRequest
import com.d2m.app.data.model.SuggestionActionResponse

/** Mirrors app/routers/buffer.py -- Accept/Reject/Snooze on a candidate. */
class BufferApi(private val api: ApiClient) {
    suspend fun act(
        primaryId: String,
        candidateId: String,
        action: String,
        message: String? = null,
        likedItemType: String? = null,
        likedItemId: String? = null,
    ): SuggestionActionResponse =
        api.post(
            "/primaries/$primaryId/suggestions/$candidateId/action",
            SuggestionActionRequest(action, message = message, likedItemType = likedItemType, likedItemId = likedItemId),
        )
}
