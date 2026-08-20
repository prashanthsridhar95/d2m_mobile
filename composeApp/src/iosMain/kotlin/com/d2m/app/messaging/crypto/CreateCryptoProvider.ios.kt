package com.d2m.app.messaging.crypto

import com.d2m.app.data.network.ApiClient
import com.russhwolf.settings.Settings

/** iOS keeps the non-production stub -- see this file's sibling doc comment (CreateCryptoProvider.kt) and messaging/crypto/signal/CryptoPrimitives.ios.kt for why. */
actual fun createCryptoProvider(apiClient: ApiClient, settings: Settings): CryptoProvider = StubUnencryptedCryptoProvider()
