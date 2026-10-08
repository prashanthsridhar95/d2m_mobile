package com.d2m.app.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/**
 * Thin Ktor client for WedLock IAM (wedlock_iam/app/api/routes/auth.py in
 * the sibling WedLockIAM repo) -- a separate identity microservice from
 * d2m_core_engine, at its own base URL, so this deliberately builds its own
 * small HttpClient off the shared platform engine rather than routing
 * through ApiClient (which only ever talks to d2m_core_engine and attaches
 * the resulting access_token as a header -- see ApiClient.kt). Same
 * "engine reused, separate small wrapper for a separate base URL" pattern
 * as GeocodingApi.kt.
 *
 * The plain email+password+OTP surface is wired up here (otp/send,
 * otp/verify, register/parent, register/self, login), plus social auth
 * (socialAuth() below, backing LoginScreen.kt's native Google Sign-In
 * button) -- WedLock also exposes phone OTP, DPoP step-up, and password
 * reset, none of which this app's registration/login screens use.
 *
 * Registration has no token response (WedLock's /auth/register/parent and
 * /auth/register/self endpoints return a plain message) -- every register
 * call here must be followed by login()
 * to actually obtain an access_token, same as the equivalent web change.
 */
@Serializable
private data class WedLockSendOtpRequestDto(val emailOrPhone: String)

@Serializable
private data class WedLockVerifyOtpRequestDto(val emailOrPhone: String, val code: String)

@Serializable
private data class WedLockRegisterRequestDto(val email: String, val password: String)

@Serializable
private data class WedLockLoginRequestDto(val emailOrPhone: String, val password: String)

/**
 * Mirrors WedLockIAM's app/schemas/auth.py SocialAuthRequest. `provider` is
 * always "google" here (this app only builds the Google flow -- see
 * data/auth/GoogleSignInLauncher.kt's doc comment on Apple being explicitly
 * out of scope). `accountType` is only actually consulted server-side
 * (app/services/auth_service.py's social_auth()) the moment a brand-new
 * WedLock account is created from this call -- an existing account is
 * looked up by the Google token's own "sub" claim regardless of what's
 * sent here. `nonce`, if present, must match the "nonce" claim WedLock's
 * social_token_service.py decodes out of the id_token itself (replay
 * protection) -- left null since the native SDK calls below don't thread
 * one through yet.
 */
@Serializable
private data class WedLockSocialAuthRequestDto(
    val provider: String = "google",
    val idToken: String,
    val accountType: String? = null,
    val nonce: String? = null,
)

@Serializable
data class WedLockSendOtpResponse(val message: String, val method: String = "email", val otpCode: String? = null)

@Serializable
data class WedLockMessageResponse(val message: String)

@Serializable
data class WedLockTokenPair(val accessToken: String, val refreshToken: String? = null, val tokenType: String = "Bearer")

/** WedLockIAM's SocialAuthResponse -- TokenPair plus isNewUser, see POST /auth/social's response shape in this file's class doc comment. */
@Serializable
data class WedLockSocialAuthResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String = "Bearer",
    val isNewUser: Boolean = false,
)

class WedLockApi(
    engineHttpClient: HttpClient,
    private val baseUrl: String = resolveWedlockBaseUrl(),
) {
    // Separate Json instance from ApiClient's (same SnakeCase strategy,
    // configured independently since these two clients target different
    // services and shouldn't be coupled to one shared config object).
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    private val client = engineHttpClient.config {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
        }
    }

    private fun url(path: String) = baseUrl.trimEnd('/') + path

    private suspend inline fun <reified T> post(path: String, body: Any): T {
        val response = client.post(url(path)) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return unwrap(response)
    }

    suspend inline fun <reified T> unwrap(response: HttpResponse): T {
        if (!response.status.isSuccess()) {
            // Mirrors ApiClient.unwrap()'s ApiError shape -- friendlyError()
            // in ApiClient.kt already knows how to surface an ApiError's
            // detail regardless of which client raised it.
            val detail = runCatching { response.body<ApiErrorBodyDto>().detail }.getOrNull()
            throw ApiError(
                status = response.status.value,
                detail = detail,
                message = detail ?: "WedLock request failed (${response.status.value}).",
            )
        }
        return response.body()
    }

    /** Sends a registration OTP to an email address. Console-mode delivery logs to wedlock_email_otp.log locally. */
    suspend fun sendRegistrationOtp(email: String): WedLockSendOtpResponse =
        post("/auth/otp/send", WedLockSendOtpRequestDto(emailOrPhone = email))

    suspend fun verifyRegistrationOtp(email: String, code: String): WedLockMessageResponse =
        post("/auth/otp/verify", WedLockVerifyOtpRequestDto(emailOrPhone = email, code = code))

    /** Must immediately follow a verifyRegistrationOtp() for the same email -- see WedLock's register_parent(). */
    suspend fun registerParent(email: String, password: String): WedLockMessageResponse =
        post("/auth/register/parent", WedLockRegisterRequestDto(email = email, password = password))

    /** Must immediately follow a verifyRegistrationOtp() for the same email -- see WedLock's register_self(). */
    suspend fun registerSelf(email: String, password: String): WedLockMessageResponse =
        post("/auth/register/self", WedLockRegisterRequestDto(email = email, password = password))

    /** Registration doesn't return a token, so both the register-then-login and plain returning-user paths end here. */
    suspend fun login(emailOrPhone: String, password: String): WedLockTokenPair =
        post("/auth/login", WedLockLoginRequestDto(emailOrPhone = emailOrPhone, password = password))

    /**
     * WedLock's POST /auth/social -- exchanges a Google-issued ID token
     * (from data/auth/GoogleSignInLauncher.kt's native Credential
     * Manager/GoogleSignIn-iOS call) for a WedLock access_token, creating a
     * brand-new WedLock account on first sign-in the same way
     * register/parent + login() does for the password flow, or logging
     * into an existing one otherwise -- either way, one round trip, no
     * separate login() call needed afterward (unlike register above).
     *
     * In production, WedLock verifies the token's signature against
     * Google's real JWKS (RS256) and its `aud` claim against
     * GOOGLE_CLIENT_IDS -- see WedLockIAM's app/services/
     * social_token_service.py. With SOCIAL_TEST_MODE=true in WedLock's own
     * .env (local dev only), it instead accepts a locally HS256-signed
     * stand-in token, which is how this function's request/response
     * handling was verified end-to-end without any real Google
     * infrastructure -- see this app's own change notes for the exact
     * curl/python repro.
     */
    suspend fun socialAuth(idToken: String, accountType: String? = null, nonce: String? = null): WedLockSocialAuthResponse =
        post("/auth/social", WedLockSocialAuthRequestDto(provider = "google", idToken = idToken, accountType = accountType, nonce = nonce))
}

/**
 * WedLock IAM now has its own Cloudflare Tunnel (auth.prashanthsridhar.com),
 * same deployment pattern as d2m_core_engine -- see ApiConfig.DEFAULT_BASE_URL/
 * resolveDefaultBaseUrl(). Both platform actuals default there now, matching
 * the web app's own VITE_WEDLOCK_BASE_URL; see HttpEngine.android.kt /
 * HttpEngine.ios.kt for the local-dev loopback values to swap in instead
 * when working against a laptop-hosted wedlock_iam.
 */
expect fun resolveWedlockBaseUrl(): String
