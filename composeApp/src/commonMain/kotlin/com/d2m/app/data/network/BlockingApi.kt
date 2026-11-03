package com.d2m.app.data.network

import com.d2m.app.data.model.MessageReportIn
import com.d2m.app.data.model.MessageReportOut

/**
 * Mirrors app/routers/blocking.py's /primaries/{id}/message-reports route
 * -- see data/model/Blocking.kt's doc comment for why this is its own
 * D2M-native call rather than going through TrustApi/WedLock's trust
 * subsystem. Same require_owns_primary auth as every other
 * /primaries/{id}/... route this app already calls (ApiClient's
 * Authorization bearer header, applied automatically).
 */
class BlockingApi(private val api: ApiClient) {

    suspend fun reportMessage(
        primaryId: String,
        reportedPrimaryId: String,
        contentType: String,
        contentSnippet: String?,
        reason: String,
        details: String?,
    ): MessageReportOut =
        api.post(
            "/primaries/$primaryId/message-reports",
            MessageReportIn(
                reportedPrimaryId = reportedPrimaryId,
                contentType = contentType,
                contentSnippet = contentSnippet,
                reason = reason,
                details = details,
            ),
        )
}
