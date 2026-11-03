package com.d2m.app.messaging.crypto

import com.d2m.app.data.network.ApiClient
import com.russhwolf.settings.Settings

/**
 * Platform seam for which `CryptoProvider` gets bound in AppModule.kt --
 * same expect/actual factory-function shape as `createWebRtcEngine()`
 * (messaging/call/WebRtcEngine.kt). BOTH actuals now return a real
 * `SignalCryptoProvider` over the from-spec Signal Protocol implementation
 * in messaging/crypto/signal/ -- they differ only in which
 * `CryptoPrimitives` backs it.
 *
 * `StubUnencryptedCryptoProvider` (base64, NOT encryption) is no longer
 * bound anywhere. It is kept only as a clearly-labelled reference/testing
 * type; binding it on either platform would silently break messaging
 * against the other one, not merely weaken it.
 */
expect fun createCryptoProvider(apiClient: ApiClient, settings: Settings): CryptoProvider
