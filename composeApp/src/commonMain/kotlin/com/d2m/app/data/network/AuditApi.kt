package com.d2m.app.data.network

import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
data class ScreenViewAuditRequestDto(val screen: String)

@Serializable
data class ScreenViewAuditResponseDto(val watermarkId: String, val serverTime: String)

/**
 * Client for the forensic-watermark audit trail (POST /audit/screen-view --
 * see d2m_core_engine's app/routers/audit.py). Consumed by
 * ui/components/WatermarkOverlay.kt, mirroring d2m_web's
 * src/lib/watermark.js.
 *
 * X-D2M-User-Id is app.auth.get_current_user's dev-mode fallback on the
 * backend -- there's no bearer token yet (see ApiClient.kt's own doc
 * comment). Attached per-request here (not via a shared defaultRequest
 * plugin the way PhotoImageLoader.kt does it) since this is the only call
 * site and reusing ApiClient's own client is simpler than standing up a
 * second one just for one endpoint.
 */
class AuditApi(private val apiClient: ApiClient, private val identityStore: IdentityStore) {
    suspend fun recordScreenView(screen: String): ScreenViewAuditResponseDto? {
        val identity = identityStore.identity.value
        val userId = if (identity.role == D2MRole.PARENT) identity.sponsorId else identity.primaryId
        if (userId == null) return null
        return try {
            apiClient.client.post(apiClient.url("/audit/screen-view")) {
                contentType(ContentType.Application.Json)
                header("X-D2M-User-Id", userId)
                setBody(ScreenViewAuditRequestDto(screen))
            }.body()
        } catch (e: Exception) {
            // Best-effort, same posture as d2m_web's watermark.js -- a
            // failed audit ping shouldn't block or crash the screen it's
            // decorating. The overlay just keeps showing its last stamp.
            null
        }
    }
}
