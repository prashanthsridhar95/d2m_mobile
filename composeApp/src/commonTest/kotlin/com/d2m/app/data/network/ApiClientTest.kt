package com.d2m.app.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Serializable
private data class Ping(val ok: Boolean)

/**
 * Covers the two ApiClient behaviors added/relied on in this hardening pass:
 * the 429 retry-once backoff (see ApiClient.withRateLimitRetry, added
 * against the backend's documented fixed-window rate limiter), and that a
 * genuine (non-429) error still surfaces as ApiError with its detail intact
 * -- unwrap() must not be accidentally swallowed by the new retry wrapper.
 */
class ApiClientTest {

    private fun clientWith(engine: MockEngine): ApiClient =
        ApiClient(engine = HttpClient(engine), baseUrl = "https://example.test")

    @Test
    fun get_on429_retriesOnce_andSucceeds() = runTest {
        var callCount = 0
        val engine = MockEngine { request ->
            callCount++
            if (callCount == 1) {
                respond(
                    content = """{"detail":"Rate limit exceeded"}""",
                    status = HttpStatusCode.TooManyRequests,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } else {
                respond(
                    content = """{"ok":true}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }

        val result: Ping = clientWith(engine).get("/ping")

        assertEquals(2, callCount)
        assertEquals(true, result.ok)
    }

    @Test
    fun get_on429Twice_surfacesApiErrorWithDetail() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"detail":"Rate limit exceeded"}""",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val error = assertFailsWith<ApiError> { clientWith(engine).get<Ping>("/ping") }
        assertEquals(429, error.status)
        assertEquals("Rate limit exceeded", error.detail)
    }

    @Test
    fun get_on404_doesNotRetry_andSurfacesDetail() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(
                content = """{"detail":"Not found"}""",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val error = assertFailsWith<ApiError> { clientWith(engine).get<Ping>("/missing") }
        assertEquals(1, callCount) // only 429s get the retry
        assertEquals(404, error.status)
        assertEquals("Not found", error.detail)
    }

    // ---- session expiry --------------------------------------------------------------------------------------------

    private fun unauthorizedEngine() = MockEngine {
        respond(
            content = """{"detail":"invalid or expired token"}""",
            status = HttpStatusCode.Unauthorized,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    @Test
    fun get_on401WithToken_signalsSessionExpired() = runTest {
        val store = com.d2m.app.data.session.IdentityStore(com.russhwolf.settings.MapSettings())
        store.setChild("child-1")
        store.setWedlockAccessToken("stale-token")
        val client = ApiClient(engine = HttpClient(unauthorizedEngine()), baseUrl = "https://example.test", identityStore = store)

        var signalled = false
        val job = launch(kotlinx.coroutines.Dispatchers.Unconfined) {
            client.sessionExpired.collect { signalled = true }
        }
        assertFailsWith<ApiError> { client.get<Ping>("/ping") }
        job.cancel()

        assertEquals(true, signalled)
    }

    @Test
    fun get_on401WithoutToken_doesNotSignal() = runTest {
        // e.g. a wrong password on the login screen: no token was sent, so it is an ordinary error, not an expiry.
        val store = com.d2m.app.data.session.IdentityStore(com.russhwolf.settings.MapSettings())
        val client = ApiClient(engine = HttpClient(unauthorizedEngine()), baseUrl = "https://example.test", identityStore = store)

        var signalled = false
        val job = launch(kotlinx.coroutines.Dispatchers.Unconfined) {
            client.sessionExpired.collect { signalled = true }
        }
        assertFailsWith<ApiError> { client.get<Ping>("/ping") }
        job.cancel()

        assertEquals(false, signalled)
    }

    // ---- error copy ------------------------------------------------------------------------------------------------

    @Test
    fun friendlyError_neverExposesServerDetailOrExceptionMessage() {
        ErrorCopy.somethingWentWrong = "Something went wrong"
        assertEquals("Something went wrong", friendlyError(ApiError(401, "invalid or expired token", "invalid or expired token")))
        assertEquals("Something went wrong", friendlyError(ApiError(500, null, "Request failed (500).")))
        assertEquals("Something went wrong", friendlyError(RuntimeException("java.net.UnknownHostException: api.example"), "Couldn't load candidates."))
    }
}
