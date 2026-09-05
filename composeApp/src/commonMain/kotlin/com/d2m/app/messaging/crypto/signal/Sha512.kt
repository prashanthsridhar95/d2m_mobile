package com.d2m.app.messaging.crypto.signal

/**
 * SHA-512, the one hash `Curve25519.kt`'s XEdDSA needs (nonce derivation
 * and the challenge scalar). Deliberately an expect/actual over each
 * platform's own vetted implementation -- `java.security.MessageDigest` on
 * Android, CommonCrypto's `CC_SHA512` on iOS -- rather than a hand-written
 * compression function. There is no interop risk in doing so: SHA-512 has
 * exactly one correct answer, unlike the curve encodings above it where
 * per-implementation convention differences are the real hazard.
 */
internal expect fun sha512(data: ByteArray): ByteArray
