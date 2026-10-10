package com.d2m.app.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import com.d2m.app.data.session.IdentityStore

/**
 * Thin wrapper over Ktor mirroring d2m_web/src/lib/apiClient.js: same base-URL
 * config, same ApiError(status, detail) shape, same "no client-side caching
 * here, ApiCache owns that" split, same resolveMediaUrl for the backend's
 * relative /media/photos/... paths.
 *
 * Auth note: d2m_core_engine's /sponsors and /invites/redeem endpoints now
 * accept an optional `Authorization: Bearer <wedlock_jwt>` header (see
 * app/routers/identity.py's create_sponsor/redeem_invite) -- a token
 * obtained via WedLockApi.kt's login()/register flow and stashed in
 * session/IdentityStore.kt. This client attaches that header, when present,
 * to every request it makes (applyAuthHeader() below); most requests carry
 * no such token server-side requirement today, but sending it unconditionally
 * costs nothing since every other endpoint simply ignores an Authorization
 * header it doesn't ask for. identityStore is optional (defaults to null)
 * so anything constructing an ApiClient without a session context -- there
 * is none in this app today, but this keeps the constructor from forcing
 * one -- still works unauthenticated exactly as before.
 */
class ApiError(val status: Int, val detail: String?, message: String) : Exception(message)

class ApiClient(
    private val engine: HttpClient,
    var baseUrl: String = ApiConfig.DEFAULT_BASE_URL,
    @PublishedApi internal val identityStore: IdentityStore? = null,
) {
    // d2m_core_engine's Pydantic schemas (app/schemas.py) are plain BaseModel
    // subclasses with no alias_generator -- every field goes over the wire as
    // snake_case (primary_id, contact_info, seeking_gender, ...), while every
    // DTO in data/model/*.kt is deliberately idiomatic Kotlin camelCase with
    // no per-field @SerialName. namingStrategy handles the conversion in one
    // place instead of hand-annotating ~150 fields across every model file --
    // without it, every non-nullable field silently fails to decode
    // (MissingFieldException) because e.g. "primaryId" never matches the
    // "primary_id" key actually present in the response body.
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    val client: HttpClient = engine.config {
        install(ContentNegotiation) { json(json) }
        install(Logging) { level = LogLevel.INFO }
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
        }
        // Installed here (rather than a second client) so MessagingWsClient
        // (Phase 6) can reuse this same client instance -- one HTTP engine
        // per app, matching this build's "no reason to instantiate two"
        // simplicity bias elsewhere.
        install(WebSockets)
    }

    fun url(path: String) = baseUrl.trimEnd('/') + path

    /** Attaches the stored WedLock bearer token, if any -- see the class doc comment above. */
    @PublishedApi
    internal fun HttpRequestBuilder.applyAuthHeader() {
        identityStore?.identity?.value?.wedlockAccessToken?.let { token ->
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    /**
     * Backoff for 429s -- the backend's rate limiter (app/rate_limit.py) is a
     * simple 60s fixed window per IP, returning `Retry-After: 60` on
     * exceeding it (60/min for POST, 300/min for GET/PUT/DELETE by
     * default). This retries once after a short client-side delay rather
     * than surfacing the error immediately, since a single momentary burst
     * (e.g. a screen firing a few GETs on load) shouldn't need the user to
     * manually retry -- a second 429 still surfaces as a real error rather
     * than retrying indefinitely.
     */
    @PublishedApi
    internal suspend inline fun <T> withRateLimitRetry(block: () -> T): T =
        try {
            block()
        } catch (e: ApiError) {
            if (e.status == 429) {
                kotlinx.coroutines.delay(1500)
                block()
            } else {
                throw e
            }
        }

    /** Resolves the backend's relative /media/photos/... URLs against the configured base, same as resolveMediaUrl() on web. */
    fun resolveMediaUrl(path: String): String {
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("blob:") || path.startsWith("data:")) {
            return path
        }
        return baseUrl.trimEnd('/') + path
    }

    // extraHeaders defaults to empty, same additive shape as post/put/delete
    // below -- added for the public share-link viewer (PublicLinksApi.kt),
    // the one GET caller that needs a per-call X-D2M-User-Id/X-D2M-Lead-Key
    // header pair rather than the Authorization bearer token
    // applyAuthHeader() already attaches unconditionally.
    suspend inline fun <reified T> get(path: String, params: Map<String, String?> = emptyMap(), extraHeaders: Map<String, String> = emptyMap()): T =
        withRateLimitRetry {
            unwrap(client.get(url(path)) {
                applyAuthHeader()
                extraHeaders.forEach { (k, v) -> header(k, v) }
                params.forEach { (k, v) -> if (v != null) parameter(k, v) }
            })
        }

    // extraHeaders defaults to empty so every existing call site (none of
    // which needed a per-call header before the trust subsystem) is
    // unaffected. Added for the WedLock trust subsystem's step-up re-auth
    // flow (app/routers/trust.py): a mutating trust call must carry the
    // short-lived token from POST /trust/reauthenticate as
    // `X-Step-Up-Token`, and that header is specific to ONE call, not the
    // whole session -- unlike the Authorization bearer token, which
    // applyAuthHeader() already attaches to every request unconditionally.
    // See data/network/TrustApi.kt and ui/components/StepUpConfirmDialog.kt.
    suspend inline fun <reified T> post(path: String, body: Any? = null, extraHeaders: Map<String, String> = emptyMap()): T =
        withRateLimitRetry {
            unwrap(client.post(url(path)) {
                applyAuthHeader()
                contentType(ContentType.Application.Json)
                extraHeaders.forEach { (k, v) -> header(k, v) }
                if (body != null) setBody(body)
            })
        }

    // Like post() above, but hands back the raw HttpResponse instead of just
    // the decoded body -- for the one caller (PublicLinksApi.identify) that
    // needs a response HEADER (X-D2M-Lead-Key), not just the JSON body.
    // Still throws ApiError on a non-2xx status, same as unwrap()'s own
    // generic path -- callers only reach for this instead of post() when
    // they specifically need something unwrap() would otherwise discard.
    suspend fun postRaw(path: String, body: Any? = null, extraHeaders: Map<String, String> = emptyMap()): HttpResponse =
        withRateLimitRetry {
            val response = client.post(url(path)) {
                applyAuthHeader()
                contentType(ContentType.Application.Json)
                extraHeaders.forEach { (k, v) -> header(k, v) }
                if (body != null) setBody(body)
            }
            if (!response.status.isSuccess()) {
                val detail = runCatching { response.body<ApiErrorBodyDto>().detail }.getOrNull()
                throw ApiError(status = response.status.value, detail = detail, message = detail ?: friendlyStatusMessage(response.status))
            }
            response
        }

    suspend inline fun <reified T> put(path: String, body: Any? = null, extraHeaders: Map<String, String> = emptyMap()): T =
        withRateLimitRetry {
            unwrap(client.put(url(path)) {
                applyAuthHeader()
                contentType(ContentType.Application.Json)
                extraHeaders.forEach { (k, v) -> header(k, v) }
                if (body != null) setBody(body)
            })
        }

    // body defaults to null, same additive shape as post/put above -- every
    // existing DELETE call site is unaffected. Added for unlink_family
    // (TrustApi.kt), the one DELETE in this app that needs a JSON body
    // (the caller's password) rather than just headers.
    suspend inline fun <reified T> delete(path: String, body: Any? = null, extraHeaders: Map<String, String> = emptyMap()): T =
        withRateLimitRetry {
            unwrap(client.delete(url(path)) {
                applyAuthHeader()
                extraHeaders.forEach { (k, v) -> header(k, v) }
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            })
        }

    /** Multipart photo upload -- mirrors postForm() in apiClient.js. */
    suspend inline fun <reified T> postForm(path: String, fileName: String, contentType: String, bytes: ByteArray): T =
        withRateLimitRetry {
            unwrap(
                client.post(url(path)) {
                    applyAuthHeader()
                    setBody(MultiPartFormDataContent(formData {
                        append("file", bytes, Headers.build {
                            append(HttpHeaders.ContentType, contentType)
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        })
                    }))
                }
            )
        }

    /**
     * Generalized multipart upload -- postForm() above only ever sends one
     * file under a fixed "file" field name (every existing caller needed
     * exactly that). Identity-verification submission (Phase 3 of the
     * backlog this session is working) needs two named files (document,
     * selfie) plus a plain text field (document_type) in one request, same
     * as the backend's own Form(...) + two File(...) params.
     */
    data class MultipartFile(val fieldName: String, val fileName: String, val contentType: String, val bytes: ByteArray)

    suspend inline fun <reified T> postMultipart(path: String, fields: Map<String, String>, files: List<MultipartFile>): T =
        withRateLimitRetry {
            unwrap(
                client.post(url(path)) {
                    applyAuthHeader()
                    setBody(MultiPartFormDataContent(formData {
                        fields.forEach { (name, value) -> append(name, value) }
                        files.forEach { f ->
                            append(f.fieldName, f.bytes, Headers.build {
                                append(HttpHeaders.ContentType, f.contentType)
                                append(HttpHeaders.ContentDisposition, "filename=\"${f.fileName}\"")
                            })
                        }
                    }))
                }
            )
        }

    suspend inline fun <reified T> unwrap(response: HttpResponse): T {
        if (!response.status.isSuccess()) {
            val detail = runCatching { response.body<ApiErrorBodyDto>().detail }.getOrNull()
            throw ApiError(
                status = response.status.value,
                detail = detail,
                message = detail ?: friendlyStatusMessage(response.status),
            )
        }
        return response.body()
    }

    fun friendlyStatusMessage(status: HttpStatusCode): String = when (status.value) {
        404 -> "Not found."
        429 -> "Rate limit exceeded -- slow down and try again shortly."
        in 500..599 -> "Something went wrong on our end. Try again shortly."
        else -> "Request failed (${status.value})."
    }
}

@kotlinx.serialization.Serializable
data class ApiErrorBodyDto(val detail: String? = null)

object ApiConfig {
    // Defaults to the real d2m_core_engine deployment behind a Cloudflare
    // Tunnel (api.prashanthsridhar.com), same pattern as the web app's
    // app.prashanthsridhar.com. See resolveDefaultBaseUrl()'s per-platform
    // doc comment (HttpEngine.android.kt / HttpEngine.ios.kt) for the
    // local-laptop-dev override values, and plan §7 for the still-open
    // per-build-flavor/env-based config gap this will eventually replace.
    val DEFAULT_BASE_URL: String = resolveDefaultBaseUrl()
}

expect fun resolveDefaultBaseUrl(): String

/** Platform HTTP engine -- Android engine on Android, Darwin (NSURLSession-backed) on iOS. */
expect fun httpEngine(): HttpClient

/** friendlyError(e, fallback) equivalent -- the shared unwrap-to-display-string helper used in every screen's catch block. */
fun friendlyError(e: Throwable, fallback: String = "Something went wrong."): String =
    (e as? ApiError)?.detail ?: e.message ?: fallback
