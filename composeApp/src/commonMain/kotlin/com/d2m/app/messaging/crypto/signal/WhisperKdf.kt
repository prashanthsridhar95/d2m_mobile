package com.d2m.app.messaging.crypto.signal

/**
 * The exact (non-standard-RFC5869-length) HKDF variant
 * `libsignal-protocol-typescript` uses internally -- confirmed against its
 * source (`internal/crypto.js`): always emits exactly 3x32-byte chunks
 * (T1/T2/T3), not an arbitrary requested length. Every KDF call site in
 * this app's Double Ratchet/X3DH port uses this exact shape, matching the
 * reference implementation's own `Internal.HKDF` byte-for-byte:
 *
 * ```
 * PRK = HMAC-SHA256(key=salt, data=input)
 * T1  = HMAC-SHA256(PRK, info || 0x01)
 * T2  = HMAC-SHA256(PRK, T1 || info || 0x02)
 * T3  = HMAC-SHA256(PRK, T2 || info || 0x03)
 * ```
 */
internal fun whisperHkdf(primitives: CryptoPrimitives, input: ByteArray, salt: ByteArray, info: String): Triple<ByteArray, ByteArray, ByteArray> {
    val infoBytes = info.encodeToByteArray()
    val prk = primitives.hmacSha256(salt, input)
    val t1 = primitives.hmacSha256(prk, infoBytes + byteArrayOf(1))
    val t2 = primitives.hmacSha256(prk, t1 + infoBytes + byteArrayOf(2))
    val t3 = primitives.hmacSha256(prk, t2 + infoBytes + byteArrayOf(3))
    return Triple(t1, t2, t3)
}

internal val ZERO_32 = ByteArray(32)

/** Signal's X3DH domain-separation prefix: 32 bytes of 0xFF prepended before the concatenated DH outputs. */
internal val X3DH_PREFIX_FF32 = ByteArray(32) { 0xFF.toByte() }

internal const val INFO_X3DH = "WhisperText"
internal const val INFO_RATCHET = "WhisperRatchet"
internal const val INFO_MESSAGE_KEYS = "WhisperMessageKeys"

/** Symmetric chain-key ratchet step (KDF_CK): HMAC-SHA256(chainKey, 0x01) -> message-key seed, HMAC-SHA256(chainKey, 0x02) -> next chain key. */
internal fun advanceChainKey(primitives: CryptoPrimitives, chainKey: ByteArray): Pair<ByteArray, ByteArray> {
    val seed = primitives.hmacSha256(chainKey, byteArrayOf(1))
    val next = primitives.hmacSha256(chainKey, byteArrayOf(2))
    return seed to next
}

/** Expands a message-key seed into (aesKey32, macKey32, iv16) -- iv is the first 16 bytes of the HKDF's 3rd output, NOT random (deliberately -- see CryptoPrimitives.kt's doc comment; confirmed against the reference implementation, which derives rather than randomizes the IV). */
internal fun deriveMessageKeys(primitives: CryptoPrimitives, seed: ByteArray): Triple<ByteArray, ByteArray, ByteArray> {
    val (aesKey, macKey, ivMaterial) = whisperHkdf(primitives, seed, ZERO_32, INFO_MESSAGE_KEYS)
    return Triple(aesKey, macKey, ivMaterial.copyOf(16))
}
