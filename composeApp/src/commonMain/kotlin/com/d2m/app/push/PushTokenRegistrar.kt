package com.d2m.app.push

import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.NotificationsRepository

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
) {
    suspend fun registerCurrentToken(platform: String, token: String) {
        val identity = identityStore.identity.value
        val accountId = identity.sponsorId ?: identity.primaryId ?: return
        runCatching { notificationsRepo.registerDeviceToken(accountId, platform, token) }
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
