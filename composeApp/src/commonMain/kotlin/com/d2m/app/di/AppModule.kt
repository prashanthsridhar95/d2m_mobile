package com.d2m.app.di

import com.d2m.app.data.cache.ApiCache
import com.d2m.app.data.network.*
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.data.session.createSettings
import com.d2m.app.domain.repository.*
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.call.CallManager
import com.d2m.app.messaging.call.WebRtcEngine
import com.d2m.app.messaging.call.createWebRtcEngine
import com.d2m.app.messaging.crypto.CryptoProvider
import com.d2m.app.messaging.crypto.StubUnencryptedCryptoProvider
import com.d2m.app.messaging.transport.MessagingWsClient
import com.d2m.app.push.PushTokenRegistrar
import org.koin.dsl.module

val appModule = module {
    single { ApiCache() }
    single { ApiClient(engine = httpEngine()) }

    single { IdentityApi(get()) }
    single { SuggestionsApi(get()) }
    single { BufferApi(get()) }
    single { SeriousModeApi(get()) }
    single { ConsentApi(get()) }
    single { OffboardingApi(get()) }
    single { AdminApi(get()) }
    single { NotificationsApi(get()) }
    single { DashboardApi(get()) }
    single { PanchangamApi(get()) }
    single { GeocodingApi(httpEngine()) }

    single { IdentityRepository(get(), get()) }
    single { SuggestionsRepository(get(), get(), get()) }
    single { SeriousModeRepository(get(), get()) }
    single { ConsentRepository(get(), get()) }
    single { NotificationsRepository(get(), get()) }
    single { DashboardRepository(get(), get()) }
    single { PanchangamRepository(get(), get()) }
    single { OffboardingRepository(get(), get()) }
    single { AdminRepository(get()) }

    single { IdentityStore(createSettings()) }
    single { com.d2m.app.ui.screens.onboarding.OnboardingResultHolder() }

    single { PushTokenRegistrar(get(), get()) }
    single { com.d2m.app.push.PlatformPushInitializer() }

    // Phase 6 -- see messaging/crypto/CryptoProvider.kt's doc comment: this
    // binds the explicitly-non-production stub by default. Swap this single
    // binding for a real Signal Protocol-backed CryptoProvider implementation
    // before any of this carries real user content.
    single<CryptoProvider> { StubUnencryptedCryptoProvider() }
    single { MessagingWsClient() }
    single { MessagingRepository(get(), get(), get()) }

    // Audio/video calling -- see messaging/call/CallManager.kt +
    // WebRtcEngine.android.kt (real) / WebRtcEngine.ios.kt (not implemented
    // yet). CallManager registers itself into MessagingRepository's
    // callSignalHandler/sessionResetHandler at construction time, so simply
    // resolving it once (CallLayer.kt does, at the App.kt shell level) is
    // enough to wire calling up end-to-end.
    single<WebRtcEngine> { createWebRtcEngine() }
    single { CallManager(get(), get()) }
}
