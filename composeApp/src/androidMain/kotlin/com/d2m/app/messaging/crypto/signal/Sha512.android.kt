package com.d2m.app.messaging.crypto.signal

import java.security.MessageDigest

internal actual fun sha512(data: ByteArray): ByteArray =
    MessageDigest.getInstance("SHA-512").digest(data)
