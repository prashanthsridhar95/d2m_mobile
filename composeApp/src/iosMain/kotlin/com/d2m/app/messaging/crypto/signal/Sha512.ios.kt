package com.d2m.app.messaging.crypto.signal

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA512

@OptIn(ExperimentalForeignApi::class)
internal actual fun sha512(data: ByteArray): ByteArray {
    val out = UByteArray(64)
    out.usePinned { pinnedOut ->
        if (data.isEmpty()) {
            // Pinning a zero-length ByteArray throws in Kotlin/Native
            // (`addressOf(0)` on an empty array is out of bounds), so the
            // empty case passes a null pointer instead -- which is exactly
            // what CC_SHA512 expects for a zero-length input.
            CC_SHA512(null, 0u, pinnedOut.addressOf(0))
        } else {
            data.usePinned { pinnedIn ->
                CC_SHA512(pinnedIn.addressOf(0), data.size.toUInt(), pinnedOut.addressOf(0))
            }
        }
    }
    return out.toByteArray()
}
