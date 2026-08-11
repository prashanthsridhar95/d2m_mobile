package com.d2m.app.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
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

/**
 * Thin wrapper over Ktor mirroring d2m_web/src/lib/apiClient.js: same base-URL
 * config, same ApiError(status, detail) shape, same "no client-side caching
 * here, ApiCache owns that" split, same resolveMediaUrl for the backend's
 * relative /media/photos/... paths.
 *
 * Auth note: the backend has no session/token layer at all (see
 * session/IdentityStore.kt's doc comment) -- every call here is a plain,
 * unauthenticated request identified only by whatever sponsor_id/primary_id
 * is embedded in the path, exactly like the web app. There is no
 * Authorization header to attach. When real auth lands on the backend, this
 * is the one file that needs a bearer-token interceptor added.
 */
class ApiError(val status: Int, val detail: String?, message: String) : Exception(message)

class ApiClient(
    private val engine: HttpClient,
    var baseUrl: String = ApiConfig.DEFAULT_BASE_URL,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
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

    suspend inline fun <reified T> get(path: String, params: Map<String, String?> = emptyMap()): T =
        withRateLimitRetry {
            unwrap(client.get(url(path)) { params.forEach { (k, v) -> if (v != null) parameter(k, v) } })
        }

    suspend inline fun <reified T> post(path: String, body: Any? = null): T =
        withRateLimitRetry {
            unwrap(client.post(url(path)) {
                contentType(ContentType.Application.Json)
                if (body != null) setBody(body)
            })
        }

    suspend inline fun <reified T> put(path: String, body: Any? = null): T =
        withRateLimitRetry {
            unwrap(client.put(url(path)) {
                contentType(ContentType.Application.Json)
                if (body != null) setBody(body)
            })
        }

    suspend inline fun <reified T> delete(path: String): T =
        withRateLimitRetry { unwrap(client.delete(url(path))) }

    /** Multipart photo upload -- mirrors postForm() in apiClient.js. */
    suspend inline fun <reified T> postForm(path: String, fileName: String, contentType: String, bytes: ByteArray): T =
        withRateLimitRetry {
            unwrap(
                client.post(url(path)) {
                    setBody(MultiPartFormDataContent(formData {
                        append("file", bytes, Headers.build {
                            append(HttpHeaders.ContentType, contentType)
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        })
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
