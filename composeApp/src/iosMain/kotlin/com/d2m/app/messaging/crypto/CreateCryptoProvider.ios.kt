package com.d2m.app.messaging.crypto

import com.d2m.app.data.network.ApiClient
import com.d2m.app.messaging.crypto.signal.SignalCryptoProvider
import com.d2m.app.messaging.crypto.signal.createCryptoPrimitives
import com.russhwolf.settings.Settings

/**
 * Identical to the Android actual now -- both platforms construct the same
 * real `SignalCryptoProvider` over the same `SignalProtocol` implementation
 * in commonMain, differing only in which `CryptoPrimitives` backs it
 * (BouncyCastle/JCE on Android, CommonCrypto + the shared `Curve25519.kt`
 * on iOS -- see CryptoPrimitives.ios.kt).
 *
 * This used to return `StubUnencryptedCryptoProvider` (base64, NOT
 * encryption). That was the single biggest functional gap between the two
 * apps: it did not just weaken iOS security, it made Android<->iOS
 * messaging impossible, because each side handed the other a payload it had
 * no way to interpret.
 */
actual fun createCryptoProvider(apiClient: ApiClient, settings: Settings): CryptoProvider =
    SignalCryptoProvider(apiClient, createCryptoPrimitives(), settings)
