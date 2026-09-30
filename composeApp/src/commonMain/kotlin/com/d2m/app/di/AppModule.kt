package com.d2m.app.di

import com.d2m.app.data.cache.ApiCache
import com.d2m.app.data.local.ChatLocalStore
import com.d2m.app.data.local.createSqlDriver
import com.d2m.app.data.network.*
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.data.session.createSettings
import com.d2m.app.domain.repository.*
import com.d2m.app.messaging.ChatUiState
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.ParentContactsStore
import com.d2m.app.messaging.call.CallManager
import com.d2m.app.messaging.call.WebRtcEngine
import com.d2m.app.messaging.call.createWebRtcEngine
import com.d2m.app.messaging.crypto.CryptoProvider
import com.d2m.app.messaging.crypto.createCryptoProvider
import com.d2m.app.messaging.crypto.archive.ArchiveKeyStore
import com.d2m.app.messaging.crypto.archive.ArchiveManager
import com.d2m.app.messaging.crypto.archive.ArchivePrimitives
import com.d2m.app.messaging.crypto.archive.createArchivePrimitives
import com.d2m.app.messaging.transport.MessagingWsClient
import com.d2m.app.messaging.ui.createGifImageLoader
import com.d2m.app.push.PushTokenRegistrar
import org.koin.dsl.module

val appModule = module {
    single { ApiCache() }
    single { ApiClient(engine = httpEngine()) }

    single { IdentityApi(get()) }
    // Forensic watermark audit ping (data/network/AuditApi.kt) -- consumed
    // by ui/components/WatermarkOverlay.kt.
    single { AuditApi(get(), get()) }
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
    single { ShareLinksApi(get()) }

    single { IdentityRepository(get(), get()) }
    single { SuggestionsRepository(get(), get(), get()) }
    single { SeriousModeRepository(get(), get()) }
    single { ConsentRepository(get(), get()) }
    single { NotificationsRepository(get(), get()) }
    single { DashboardRepository(get(), get()) }
    single { ShareLinksRepository(get(), get()) }
    single { ShareLinkFieldsCache(createSettings()) }
    single { PanchangamRepository(get(), get()) }
    single { OffboardingRepository(get(), get()) }
    single { AdminRepository(get()) }

    single { IdentityStore(createSettings()) }
    single { com.d2m.app.ui.screens.onboarding.OnboardingResultHolder() }

    single { PushTokenRegistrar(get(), get()) }
    single { com.d2m.app.push.PlatformPushInitializer() }

    // Real Signal Protocol crypto on Android (messaging/crypto/signal/ --
    // from-spec X3DH + Double Ratchet, wire-compatible with d2m_web's
    // crypto.ts), non-production stub still on iOS -- see
    // createCryptoProvider.kt and messaging/crypto/signal/SignalProtocol.kt's
    // top doc comment (including its "needs real device verification"
    // caveat) for the full picture.
    single<CryptoProvider> { createCryptoProvider(get(), createSettings()) }

    // Cross-device message-history backup (messaging/crypto/archive/) --
    // P-256 ECDH/AES-GCM/PBKDF2, real on BOTH platforms (same
    // createSettings()-backed local-cache pattern as SignalSessionStore,
    // separate namespace -- see ArchiveKeyStore's doc comment). The iOS
    // actual became load-bearing the moment CryptoProvider above went real
    // there: an iOS device now has an actual Signal identity, so
    // ArchiveManager.checkAfterConnect reaches the PIN setup/restore path
    // that used to be unreachable.
    single<ArchivePrimitives> { createArchivePrimitives() }
    single { ArchiveKeyStore(createSettings()) }
    single { ArchiveManager(get(), get()) }

    // Local persistent chat cache (SQLDelight) -- see data/local/ChatDatabase.sq's
    // doc comment (tasks #56/#57: call-log bubbles vanishing on restart,
    // "chats once retrieved should be stored locally").
    single { ChatLocalStore(createSqlDriver()) }

    single { MessagingWsClient() }
    single { MessagingRepository(get(), get(), get(), get(), get()) }

    // Shared UI-chrome flag (hide bottom nav while a chat is open, jump to a
    // thread from a tapped in-app notification banner) -- see ChatUiState.kt.
    single { ChatUiState() }

    // Parent-to-parent contact registry (no backend "threads" concept exists
    // for this relationship) -- see ParentContactsStore.kt's doc comment.
    single { ParentContactsStore(createSettings()) }

    // Audio/video calling -- see messaging/call/CallManager.kt +
    // WebRtcEngine.android.kt (real) / WebRtcEngine.ios.kt (not implemented
    // yet). CallManager registers itself into MessagingRepository's
    // callSignalHandler/sessionResetHandler at construction time, so simply
    // resolving it once (CallLayer.kt does, at the App.kt shell level) is
    // enough to wire calling up end-to-end.
    single<WebRtcEngine> { createWebRtcEngine() }
    single { CallManager(get(), get()) }

    // Built once, not per-composition -- see messaging/ui/GifImageLoader.kt.
    // Reported directly: "GIF sent/received should be autoplaying."
    single { createGifImageLoader() }

    // Authenticated profile-photo loader -- see data/network/PhotoImageLoader.kt's
    // doc comment on why this is a separate ImageLoader/HttpClient from the
    // app-wide Coil default (GifPicker.kt's Giphy fetches, chat media) rather
    // than a global singleton swap.
    single { createPhotoImageLoader(get()) }
}
