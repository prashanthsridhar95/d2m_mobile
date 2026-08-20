package com.d2m.app.messaging.crypto.signal

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Byte-format conventions confirmed against `libsignal-protocol-typescript`'s
 * actual source (`internal/curve.js`'s `processKeys()`/`validatePubKeyFormat()`)
 * -- NOT the generic X25519 spec, since this specific implementation's wire
 * format has a non-obvious detail that matters for byte-for-byte interop:
 * every public key it ever produces or transmits (identity key, signed
 * prekey, one-time prekeys, ratchet ephemeral keys -- in the JSON key-bundle
 * AND inside the WhisperMessage/PreKeyWhisperMessage protobuf fields) is 33
 * bytes: a `0x05` "DJB type" prefix byte followed by the raw 32-byte
 * Curve25519 point. `CryptoPrimitives` deals only in raw 32-byte keys (the
 * form the actual curve math needs); this file is the seam that adds/strips
 * the prefix at the wire boundary, exactly where the reference
 * implementation does it.
 */
private const val DJB_TYPE: Byte = 5

internal fun prefix05(raw32: ByteArray): ByteArray {
    require(raw32.size == 32) { "expected a 32-byte raw public key, got ${raw32.size}" }
    val out = ByteArray(33)
    out[0] = DJB_TYPE
    raw32.copyInto(out, 1)
    return out
}

/** Tolerant like the reference implementation's validatePubKeyFormat: accepts 33 bytes with a 0x05 prefix (the normal case) or a bare 32 bytes. */
internal fun strip05(key: ByteArray): ByteArray = when (key.size) {
    33 -> {
        require(key[0] == DJB_TYPE) { "unexpected public key type byte: ${key[0]}" }
        key.copyOfRange(1, 33)
    }
    32 -> key
    else -> error("invalid public key length: ${key.size}")
}

@OptIn(ExperimentalEncodingApi::class)
internal fun ByteArray.toB64(): String = Base64.Default.encode(this)

@OptIn(ExperimentalEncodingApi::class)
internal fun String.fromB64(): ByteArray = Base64.Default.decode(this)
