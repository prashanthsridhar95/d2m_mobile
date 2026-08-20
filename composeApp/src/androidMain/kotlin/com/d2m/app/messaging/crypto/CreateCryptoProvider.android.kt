package com.d2m.app.messaging.crypto

import com.d2m.app.data.network.ApiClient
import com.d2m.app.messaging.crypto.signal.SignalCryptoProvider
import com.d2m.app.messaging.crypto.signal.createCryptoPrimitives
import com.russhwolf.settings.Settings

actual fun createCryptoProvider(apiClient: ApiClient, settings: Settings): CryptoProvider =
    SignalCryptoProvider(apiClient, createCryptoPrimitives(), settings)
