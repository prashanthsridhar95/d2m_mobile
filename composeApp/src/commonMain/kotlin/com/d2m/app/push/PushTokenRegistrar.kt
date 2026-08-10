package com.d2m.app.push

import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.NotificationsRepository

/**
 * Client-side half of push (plan §7/§8 Phase 3): registers whatever
 * platform token is generated against POST /accounts/{id}/device-tokens.
 * The backend's notification_service.dispatch() only logs a push_result of
 * "stubbed" or "no_device_token" today -- there's no real FCM/APNs send
 * implemented server-side yet (explicit SWAP POINT comment in that file).
 * Registering early costs nothing and means delivery starts working the
 * moment that backend work lands, with zero mobile-side changes needed.
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

/** Platform entry point -- requests permission + kicks off token retrieval, wiring the result into PushTokenRegistrar.registerCurrentToken. */
expect class PlatformPushInitializer {
    fun initialize()
}
