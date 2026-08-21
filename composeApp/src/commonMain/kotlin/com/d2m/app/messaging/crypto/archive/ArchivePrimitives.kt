package com.d2m.app.messaging.crypto.archive

/**
 * P-256 ECDH + AES-256-GCM + PBKDF2-HMAC-SHA256 primitives for the Archive
 * Keypair cross-device backup system -- see ArchiveCrypto.kt for the
 * pure-Kotlin orchestration built on this interface, ported function-for-
 * function from d2m_web's `archiveCrypto.ts`, which uses the browser's
 * native SubtleCrypto for these exact same primitives.
 *
 * A DELIBERATELY separate interface from `messaging.crypto.signal.CryptoPrimitives`
 * (Signal's Curve25519/AES-CBC primitives) even though both are "raw crypto
 * bindings" -- P-256 (NIST) and Curve25519 (DJB) are unrelated curves
 * needing unrelated key encodings (SEC1 point / PKCS8 vs. raw 32-byte
 * Montgomery u-coordinates), and AES-GCM vs AES-CBC are different cipher
 * modes with different primitive shapes (GCM carries its own 16-byte auth
 * tag / needs no manual padding). Android's built-in `java.security`/
 * `javax.crypto` providers (Conscrypt) support P-256 EC/ECDH and AES-GCM
 * natively on every API level this app targets (unlike X25519, which only
 * landed in Conscrypt at API 34 -- see CryptoPrimitives.kt's own doc
 * comment on why THAT interface needs BouncyCastle) -- so this interface's
 * Android actual needs no extra crypto library at all.
 *
 * Every method here works in raw bytes only -- no JWK, no base64. JWK
 * encode/decode (RFC 7515 base64url) and the wire-format base64 used
 * elsewhere in the Archive system live in ArchiveCodec.kt, a pure-Kotlin
 * layer on top of this one, exactly mirroring how SignalCodec.kt sits on
 * top of the Signal CryptoPrimitives interface.
 */
interface ArchivePrimitives {
    /** Cryptographically secure random bytes. */
    fun randomBytes(length: Int): ByteArray

    /**
     * Generates a P-256 (secp256r1) key pair. `publicKeyRawPoint` is the
     * 65-byte uncompressed SEC1 point (`0x04 || X(32) || Y(32)`);
     * `privateKeyPkcs8` is the PKCS#8-encoded private key -- opaque outside
     * this interface, only ever round-tripped through ecdhSharedSecretAsAesKey/
     * ecPrivateKeyScalar/ecPrivateKeyFromScalar below.
     */
    fun ecP256GenerateKeyPair(): EcKeyPair

    /**
     * ECDH agreement: `privateKeyPkcs8` (this side) x `publicKeyRawPoint`
     * (the other side) -> a 32-byte shared secret, used DIRECTLY as an
     * AES-256-GCM key -- no HKDF in between. This matches
     * `archiveCrypto.ts`'s `crypto.subtle.deriveKey({name:"ECDH", public},
     * myPrivateKey, {name:"AES-GCM", length:256}, ...)` exactly: per the
     * W3C Web Crypto spec, ECDH's `deriveBits` primitive returns the raw
     * x-coordinate of the computed point (NIST SP 800-56A section
     * 5.7.1.2's "ECDH primitive"), padded to the field size (32 bytes for
     * P-256) and used as-is for whatever algorithm `deriveKey` was asked
     * to produce -- there is deliberately no additional KDF step, so this
     * must byte-for-byte match `javax.crypto.KeyAgreement.getInstance("ECDH").generateSecret()`,
     * which returns the identical raw x-coordinate.
     */
    fun ecdhSharedSecretAsAesKey(privateKeyPkcs8: ByteArray, publicKeyRawPoint: ByteArray): ByteArray

    /** Extracts the raw 32-byte private scalar (`d`, JWK's private-key component) from a PKCS8-encoded private key -- needed once, when a freshly generated keypair must be JWK-exported for local caching (mirrors SubtleCrypto's `exportKey("jwk", privateKey)`). */
    fun ecPrivateKeyScalar(privateKeyPkcs8: ByteArray): ByteArray

    /** Rebuilds a PKCS8-encoded private key from a raw 32-byte scalar (`d`) -- the inverse of ecPrivateKeyScalar, needed when importing a private key that was cached locally as JWK (mirrors SubtleCrypto's `importKey("jwk", ...)`). The public X/Y aren't needed to reconstruct a usable EC private key -- only the scalar plus the curve parameters (fixed to P-256 here) are. */
    fun ecPrivateKeyFromScalar(d: ByteArray): ByteArray

    /** AES-256-GCM encrypt. `key` is 32 bytes, `iv` is 12 bytes (96-bit -- the only IV length SubtleCrypto's AES-GCM supports). Returns ciphertext with the 16-byte auth tag appended, matching SubtleCrypto's own AES-GCM output framing byte-for-byte. */
    fun aesGcmEncrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray

    /** AES-256-GCM decrypt (inverse of aesGcmEncrypt) -- throws if the appended auth tag doesn't verify (e.g. a wrong PIN feeding the wrong wrapping key). There is deliberately no separate "is this key right" check anywhere in this system -- this exception IS that check. */
    fun aesGcmDecrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray

    /** PBKDF2-HMAC-SHA256, deriving `keyLengthBytes` of key material directly (matches `archiveCrypto.ts`'s `pinToWrappingKey`: PBKDF2 -> `deriveKey({name:"AES-GCM", length:256})`, i.e. the derived bits ARE the AES key, no separate HKDF step). `password` is expected to be the UTF-8 (== ASCII for the numeric-only PINs this system's UI collects) bytes of the PIN. */
    fun pbkdf2Sha256(password: ByteArray, salt: ByteArray, iterations: Int, keyLengthBytes: Int = 32): ByteArray
}

data class EcKeyPair(val privateKeyPkcs8: ByteArray, val publicKeyRawPoint: ByteArray)

/** Platform factory -- see AppModule.kt for wiring. Android-only for now, same precedent as messaging.crypto.signal.CryptoPrimitives (no iOS actual exists yet). */
expect fun createArchivePrimitives(): ArchivePrimitives
