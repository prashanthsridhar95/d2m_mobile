package com.d2m.app.messaging.crypto.archive

import com.d2m.app.messaging.protocol.ArchiveEnvelope
import com.d2m.app.messaging.protocol.ArchiveField
import com.d2m.app.messaging.protocol.ArchiveKeyBundle
import com.d2m.app.messaging.protocol.JsonWebKey

/**
 * Cross-device message-history recovery -- direct, function-for-function
 * port of d2m_web's `archiveCrypto.ts` onto ArchivePrimitives (pure Kotlin,
 * no platform code of its own). See that file's extensive doc comment for
 * the full design rationale; summarized here for this port:
 *
 * Signal Protocol's Double Ratchet gives forward secrecy -- each message
 * key is discarded the moment it's used, so a brand-new device never had
 * it and can never decrypt history it wasn't present for. This is correct
 * for the LIVE channel but means the server keeping messages around
 * (see messaging-framework's store.ts) doesn't by itself let a new device
 * ever read them.
 *
 * The fix, matching how WhatsApp/Signal's own backup systems work: every
 * ACCOUNT (not device) gets a second, persistent Archive Keypair (P-256
 * ECDH). The public half is published to the server like any other public
 * key. The private half never leaves a client in plaintext -- it's
 * AES-GCM-wrapped under a key derived (PBKDF2) from a recovery PIN the
 * user chooses, and only that wrapped blob reaches the server. Every
 * outgoing message additionally gets its plaintext sealed under a fresh
 * one-off AES-GCM content key, which is itself sealed (ephemeral ECDH --
 * textbook ECIES) to BOTH the sender's and recipient's Archive public key.
 * The server relays/stores all of this as opaque bytes -- it can decrypt
 * none of it without a PIN it never sees.
 */
object ArchiveCrypto {
    /** OWASP-recommended floor (2023 guidance) for PBKDF2-SHA256 -- same value as archiveCrypto.ts's PBKDF2_ITERATIONS. */
    const val PBKDF2_ITERATIONS = 250_000

    data class GeneratedKeypair(val privateKeyPkcs8: ByteArray, val publicKeyRawPoint: ByteArray, val publicKeyJwk: JsonWebKey)

    fun generateArchiveKeypair(primitives: ArchivePrimitives): GeneratedKeypair {
        val pair = primitives.ecP256GenerateKeyPair()
        return GeneratedKeypair(pair.privateKeyPkcs8, pair.publicKeyRawPoint, publicKeyToJwk(pair.publicKeyRawPoint))
    }

    fun publicKeyToJwk(rawPoint: ByteArray): JsonWebKey {
        val (x, y) = rawPointToXY(rawPoint)
        return JsonWebKey(x = x.toBase64Url(), y = y.toBase64Url())
    }

    /** Exports BOTH halves of a keypair to JWK, `d` included on the returned public-shaped JsonWebKey -- used only for ArchiveKeyStore's local same-device cache (never sent over the wire; the public JWK sent to the server/peers is built via publicKeyToJwk, which never has `d`). */
    fun privateKeyToJwk(primitives: ArchivePrimitives, privateKeyPkcs8: ByteArray, publicKeyRawPoint: ByteArray): JsonWebKey {
        val (x, y) = rawPointToXY(publicKeyRawPoint)
        val d = primitives.ecPrivateKeyScalar(privateKeyPkcs8)
        return JsonWebKey(x = x.toBase64Url(), y = y.toBase64Url(), d = d.toBase64Url())
    }

    /** Rebuilds a PKCS8 private key from a JWK that carries `d` (as produced by privateKeyToJwk / ArchiveKeyStore's local cache). */
    fun privateKeyFromJwk(primitives: ArchivePrimitives, jwk: JsonWebKey): ByteArray {
        val d = requireNotNull(jwk.d) { "privateKeyFromJwk requires a JWK with 'd' (a private-key export) -- got a public-only JWK" }
        return primitives.ecPrivateKeyFromScalar(d.fromBase64Url())
    }

    data class WrappedPrivateKey(val wrappedPrivateKey: String, val wrapIv: String, val kdfSalt: String, val kdfIterations: Int)

    /** Wraps a private key under a PIN -- what gets published to the server (as an ArchiveKeyBundle, alongside the public key) is entirely this function's output plus the public key; the server never sees `pin` or the unwrapped key. */
    fun wrapPrivateKeyWithPin(primitives: ArchivePrimitives, privateKeyPkcs8: ByteArray, pin: String): WrappedPrivateKey {
        val salt = primitives.randomBytes(16)
        val iv = primitives.randomBytes(12)
        val wrappingKey = primitives.pbkdf2Sha256(pin.encodeToByteArray(), salt, PBKDF2_ITERATIONS)
        val wrapped = primitives.aesGcmEncrypt(wrappingKey, iv, privateKeyPkcs8)
        return WrappedPrivateKey(wrapped.toB64(), iv.toB64(), salt.toB64(), PBKDF2_ITERATIONS)
    }

    /**
     * Reverses wrapPrivateKeyWithPin -- throws (AES-GCM tag check failure,
     * surfaced by ArchivePrimitives.aesGcmDecrypt) if `pin` is wrong. This is
     * the ONLY signal available; there is deliberately no separate "is this
     * PIN right" check, since that would just be an oracle for guessing it.
     * Returns the raw PKCS8 private key bytes -- caller wraps the result
     * back into a JsonWebKey (via privateKeyToJwk, needing the public point
     * too) if it needs to cache it locally.
     */
    fun unwrapPrivateKeyWithPin(primitives: ArchivePrimitives, bundle: ArchiveKeyBundle, pin: String): ByteArray {
        val salt = bundle.kdfSalt.fromB64()
        val iv = bundle.wrapIv.fromB64()
        val wrappingKey = primitives.pbkdf2Sha256(pin.encodeToByteArray(), salt, bundle.kdfIterations)
        return primitives.aesGcmDecrypt(wrappingKey, iv, bundle.wrappedPrivateKey.fromB64())
    }

    /**
     * Seals `contentKeyRaw` (a message's one-off content key, as raw bytes)
     * to `recipientPublicKeyJwk` -- generates a fresh ephemeral keypair per
     * call (never reused) so the server never sees a stable per-sender
     * identifier in this envelope, only in the ephemeral key itself, which
     * is thrown away right after this one seal.
     */
    private fun sealKeyTo(primitives: ArchivePrimitives, contentKeyRaw: ByteArray, recipientPublicKeyJwk: JsonWebKey): ArchiveEnvelope {
        val recipientPoint = xyToRawPoint(recipientPublicKeyJwk.x.fromBase64Url(), recipientPublicKeyJwk.y.fromBase64Url())
        val ephemeral = primitives.ecP256GenerateKeyPair()
        val sharedKey = primitives.ecdhSharedSecretAsAesKey(ephemeral.privateKeyPkcs8, recipientPoint)
        val iv = primitives.randomBytes(12)
        val wrapped = primitives.aesGcmEncrypt(sharedKey, iv, contentKeyRaw)
        return ArchiveEnvelope(publicKeyToJwk(ephemeral.publicKeyRawPoint), wrapped.toB64(), iv.toB64())
    }

    /** Reverses sealKeyTo using the OWNER's own (unwrapped) private key. */
    private fun unsealKeyWith(primitives: ArchivePrimitives, envelope: ArchiveEnvelope, myPrivateKeyPkcs8: ByteArray): ByteArray {
        val ephemeralPoint = xyToRawPoint(envelope.ephemeralPublicKey.x.fromBase64Url(), envelope.ephemeralPublicKey.y.fromBase64Url())
        val sharedKey = primitives.ecdhSharedSecretAsAesKey(myPrivateKeyPkcs8, ephemeralPoint)
        return primitives.aesGcmDecrypt(sharedKey, envelope.wrapIv.fromB64(), envelope.wrappedKey.fromB64())
    }

    /** Encrypts `plaintext` once under a fresh AES-GCM content key, then seals that key to both the sender's and recipient's Archive public key -- the shape ArchiveManager.buildArchiveField attaches to every outgoing MessageEnvelope as `archive`. */
    fun encryptForArchive(primitives: ArchivePrimitives, plaintext: String, senderPublicKeyJwk: JsonWebKey, recipientPublicKeyJwk: JsonWebKey): ArchiveField {
        val contentKey = primitives.randomBytes(32)
        val iv = primitives.randomBytes(12)
        val ciphertext = primitives.aesGcmEncrypt(contentKey, iv, plaintext.encodeToByteArray())
        val envelopeForSender = sealKeyTo(primitives, contentKey, senderPublicKeyJwk)
        val envelopeForRecipient = sealKeyTo(primitives, contentKey, recipientPublicKeyJwk)
        return ArchiveField(ciphertext.toB64(), iv.toB64(), envelopeForSender, envelopeForRecipient)
    }

    /** Decrypts an archived message using whichever envelope belongs to the caller (`myEnvelope` -- envelopeForSender if this was our own outgoing message, envelopeForRecipient if it was sent to us) plus our own unwrapped Archive private key. */
    fun decryptArchiveEntry(primitives: ArchivePrimitives, archive: ArchiveField, myEnvelope: ArchiveEnvelope, myPrivateKeyPkcs8: ByteArray): String {
        val rawKey = unsealKeyWith(primitives, myEnvelope, myPrivateKeyPkcs8)
        val plaintext = primitives.aesGcmDecrypt(rawKey, archive.iv.fromB64(), archive.body.fromB64())
        return plaintext.decodeToString()
    }
}
