package com.d2m.app.messaging.crypto

import com.d2m.app.data.network.ApiClient
import com.russhwolf.settings.Settings

/**
 * Platform seam for which `CryptoProvider` gets bound in AppModule.kt --
 * same expect/actual factory-function shape as `createWebRtcEngine()`
 * (messaging/call/WebRtcEngine.kt). Android returns a real
 * `SignalCryptoProvider` (see messaging/crypto/signal/ for the from-spec
 * Signal Protocol implementation); iOS returns the non-production
 * `StubUnencryptedCryptoProvider` since no `CryptoPrimitives` actual exists
 * for iOS yet (see messaging/crypto/signal/CryptoPrimitives.ios.kt).
 */
expect fun createCryptoProvider(apiClient: ApiClient, settings: Settings): CryptoProvider
