package com.d2m.app.messaging.crypto.signal

/**
 * The only platform-specific seam in the from-spec Signal Protocol
 * implementation (see SignalProtocol.kt for the protocol logic itself,
 * which is pure Kotlin built entirely on this interface). Everything above
 * this layer -- X3DH, the Double Ratchet, protobuf framing, session storage
 * -- is ordinary cross-platform math over byte arrays and has no reason to
 * differ per platform; only the raw curve/cipher/MAC primitives need a real
 * crypto library binding.
 *
 * BOTH platforms implement this for real now.
 * `CryptoPrimitives.android.kt` uses BouncyCastle (permissively licensed --
 * see that file's doc comment for why Signal's own AGPLv3 `libsignal` was
 * deliberately not used) plus `Ed25519Math.kt`.
 * `CryptoPrimitives.ios.kt` uses Apple's CommonCrypto/Security for the
 * RNG/HMAC/AES half, and the pure-Kotlin `Curve25519.kt` (commonMain) for
 * the X25519/XEdDSA half, because Apple exposes no Curve25519 API that
 * Kotlin/Native can bind to.
 *
 * The two are pinned to each other by `CurveInteropAndroidTest.kt`, which
 * asserts they produce identical bytes -- see that file, since "each side is
 * self-consistent" is emphatically NOT enough for a protocol whose entire
 * job is letting the two platforms talk.
 *
 * All key/signature byte conventions here match
 * `@privacyresearch/libsignal-protocol-typescript` (d2m_web's crypto
 * library) exactly, confirmed against its actual source rather than the
 * general Signal Protocol spec, since small per-implementation choices
 * (public key prefix byte, IV derivation, MAC truncation length) differ
 * across implementations and must match byte-for-byte for interop:
 *  - X25519 key pairs: raw 32-byte private scalar, raw 32-byte public point
 *    (the 0x05 DJB-type prefix byte used on the wire is added/stripped by
 *    SignalCodec.kt, not by this interface -- keeps this layer a thin,
 *    literal wrapper over the underlying curve math).
 *  - XEdDSA signatures are 64 raw bytes, computed directly over the
 *    Curve25519 (X25519) private scalar with no separate Ed25519 identity
 *    -- see xEdDSASign's doc comment.
 */
interface CryptoPrimitives {
    /** Cryptographically secure random bytes. */
    fun randomBytes(length: Int): ByteArray

    /** Generates a Curve25519 (X25519) key pair. Returns (privateKey32, publicKey32), both raw, unprefixed. */
    fun x25519GenerateKeyPair(): Pair<ByteArray, ByteArray>

    /** X25519 Diffie-Hellman agreement. `privateKey`/`publicKey` are raw 32-byte values (unprefixed). Returns a 32-byte shared secret. */
    fun x25519Agree(privateKey: ByteArray, publicKey: ByteArray): ByteArray

    /** HMAC-SHA256. Returns 32 bytes. */
    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray

    /** AES-256-CBC encrypt with PKCS7 padding applied internally -- matches how the Web Crypto API's `AES-CBC` behaves (which is what libsignal-protocol-typescript relies on: it never pads/unpads by hand). `key` is 32 bytes, `iv` is 16 bytes. */
    fun aesCbcEncrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray

    /** AES-256-CBC decrypt with PKCS7 padding stripped internally (inverse of aesCbcEncrypt). */
    fun aesCbcDecrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray

    /**
     * XEdDSA signature (Signal's scheme for signing with a Curve25519/X25519
     * key directly, per https://signal.org/docs/specifications/xeddsa/ --
     * placed in the public domain by Signal, same as the X3DH/Double Ratchet
     * specs, see SignalProtocol.kt's doc comment). `privateKey` is the raw
     * 32-byte X25519 private scalar; `message` is signed as-is (the caller
     * is responsible for constructing the correct message bytes -- see
     * SignalProtocol.kt: this library signs the 33-byte 0x05-prefixed
     * public key form, not the raw 32-byte key, to match
     * libsignal-protocol-typescript exactly). Returns a 64-byte signature.
     */
    fun xEdDSASign(privateKey: ByteArray, message: ByteArray): ByteArray

    /** Verifies an XEdDSA signature produced by xEdDSASign (or any compatible XEdDSA implementation, e.g. libsignal-protocol-typescript's). `publicKey` is the raw 32-byte X25519 public point. */
    fun xEdDSAVerify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean
}

/** Platform factory -- see AppModule.kt/createCryptoProvider.kt for how this and StubUnencryptedCryptoProvider are chosen per platform. */
expect fun createCryptoPrimitives(): CryptoPrimitives
