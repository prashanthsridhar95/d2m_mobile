package com.d2m.app.push

import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.NotificationsRepository
import com.d2m.app.messaging.crypto.CryptoProvider
import kotlinx.coroutines.delay

/**
 * Client-side half of push (plan §7/§8 Phase 3): registers whatever
 * platform token is generated against POST /accounts/{id}/device-tokens.
 * The backend's notification_service.dispatch() now does a real FCM send
 * (app/services/push_service.py, firebase_admin) for every registered "fcm"
 * token, gated behind D2M_FCM_SERVICE_ACCOUNT_JSON being configured on the
 * deployed server -- confirmed real via direct inspection, not a stub
 * (an "apns" token still registers cleanly but has no real send wired up
 * yet, no APNs SDK integration exists). This function itself never fires
 * for real on Android without ALSO having the google-services Gradle plugin
 * applied and a real google-services.json from your own Firebase project
 * (see composeApp/build.gradle.kts + README.md) -- without that,
 * `registerCurrentToken` is simply never called at all, since
 * FirebaseMessaging never mints a token in the first place. Registering
 * early costs nothing regardless, so this stays unconditional.
 *
 * `registerCurrentToken` is called from each platform's actual once a real
 * token is obtained (Firebase's onNewToken / APNs' didRegisterForRemote...)
 * -- see push/PushTokenRegistrar.android.kt and .ios.kt for those hooks.
 */
class PushTokenRegistrar(
    private val notificationsRepo: NotificationsRepository,
    private val identityStore: IdentityStore,
    private val cryptoProvider: CryptoProvider,
) {
    /**
     * Registers the token bound to this install's messaging device (`device_id`), so core pushes a message's ciphertext only
     * to the device whose Signal session can decrypt it (see d2m_core_engine docs/INTERNAL_CONTRACT.md, "Multi-device").
     * The device id only exists once messaging has registered the device, which can lag the token callback at app start, so
     * this waits (bounded) for it; if it never appears the token is registered unbound, exactly as before multi-device
     * (core then also sends it pushes meant for other devices -- MessagingRepository refuses to decrypt those).
     *
     * (Messaging derives the account from `primaryId ?: sponsorId`, this from `sponsorId ?: primaryId`. IdentityStore.setParent /
     * setChild clear the other id, so only one is ever set and both resolve to the same account.)
     */
    suspend fun registerCurrentToken(platform: String, token: String) {
        val identity = identityStore.identity.value
        val accountId = identity.sponsorId ?: identity.primaryId ?: return
        var deviceId = cryptoProvider.deviceId
        var waitedMs = 0L
        while (deviceId == null && waitedMs < DEVICE_WAIT_MS) {
            delay(DEVICE_POLL_MS)
            waitedMs += DEVICE_POLL_MS
            deviceId = cryptoProvider.deviceId
        }
        runCatching { notificationsRepo.registerDeviceToken(accountId, platform, token, deviceId) }
    }

    private companion object {
        const val DEVICE_WAIT_MS = 30_000L
        const val DEVICE_POLL_MS = 500L
    }
}

/**
 * Platform entry point -- requests permission + kicks off token retrieval, wiring the result into PushTokenRegistrar.registerCurrentToken.
 * Constructor is written explicitly (empty parens) rather than left implicit --
 * AppModule.kt's `single { PlatformPushInitializer() }` failed to compile
 * against an implicit expect constructor ("does not have default
 * constructor"), which this makes unambiguous for both actuals.
 */
expect class PlatformPushInitializer() {
    fun initialize()
}
