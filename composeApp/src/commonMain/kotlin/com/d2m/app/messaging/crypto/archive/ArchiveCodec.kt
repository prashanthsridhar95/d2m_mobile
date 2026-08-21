package com.d2m.app.messaging.crypto.archive

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Pure-Kotlin encoding helpers for the Archive Keypair system -- no JCE, no
 * platform code, safe to unit-test/reuse from any target. Two separate
 * base64 alphabets are used on the wire, both confirmed against
 * `archiveCrypto.ts`/the server's `ArchiveKeyBundle`/`ArchiveEnvelope`
 * TypeScript interfaces (messaging-framework/packages/protocol/src/index.ts):
 *  - Standard base64 (WITH padding) for opaque ciphertext/IV/salt byte
 *    blobs (`wrappedKey`, `wrapIv`, `body`, `iv`, `kdfSalt`,
 *    `wrappedPrivateKey`) -- these are produced by `arrayBufferToBase64` on
 *    the web side, ordinary standard base64.
 *  - base64url WITHOUT padding (RFC 7515 section 2's "base64url encoding
 *    without padding", the JWK/JWS convention) for JsonWebKey's `x`/`y`/`d`
 *    coordinate fields specifically -- these come from
 *    `crypto.subtle.exportKey("jwk", ...)`, which always emits base64url
 *    per the Web Crypto / JOSE spec, NOT the standard alphabet used
 *    everywhere else in this system. Mixing the two up here would silently
 *    corrupt every public key exchanged with a peer or the server.
 */

@OptIn(ExperimentalEncodingApi::class)
internal fun ByteArray.toB64(): String = Base64.Default.encode(this)

@OptIn(ExperimentalEncodingApi::class)
internal fun String.fromB64(): ByteArray = Base64.Default.decode(this)

@OptIn(ExperimentalEncodingApi::class)
private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

@OptIn(ExperimentalEncodingApi::class)
internal fun ByteArray.toBase64Url(): String = base64Url.encode(this)

@OptIn(ExperimentalEncodingApi::class)
internal fun String.fromBase64Url(): ByteArray = base64Url.decode(this)

/** Splits a 65-byte uncompressed SEC1 EC point (`0x04||X||Y`, as produced by ArchivePrimitives.ecP256GenerateKeyPair) into its two raw 32-byte coordinates, for JWK `x`/`y` export. Pure byte slicing -- no curve math, no JCE. */
internal fun rawPointToXY(point: ByteArray): Pair<ByteArray, ByteArray> {
    require(point.size == 65 && point[0] == 0x04.toByte()) { "expected a 65-byte uncompressed EC point (0x04||X||Y), got ${point.size} bytes" }
    return point.copyOfRange(1, 33) to point.copyOfRange(33, 65)
}

/** The inverse of rawPointToXY -- rebuilds the 65-byte uncompressed point from two 32-byte coordinates (e.g. after decoding a peer's or the server's JWK). */
internal fun xyToRawPoint(x: ByteArray, y: ByteArray): ByteArray {
    require(x.size == 32 && y.size == 32) { "expected 32-byte EC coordinates, got x=${x.size} y=${y.size}" }
    val out = ByteArray(65)
    out[0] = 0x04
    x.copyInto(out, 1)
    y.copyInto(out, 33)
    return out
}
