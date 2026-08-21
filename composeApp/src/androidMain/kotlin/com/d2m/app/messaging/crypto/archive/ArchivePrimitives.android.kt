package com.d2m.app.messaging.crypto.archive

import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.math.BigInteger
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Real primitives for the Archive Keypair system -- see ArchivePrimitives.kt's
 * interface doc comment for the full picture. Unlike CryptoPrimitives.android.kt
 * (Signal's Curve25519 port, which needs BouncyCastle), everything here is
 * plain `java.security`/`javax.crypto` (Conscrypt, Android's built-in
 * provider) -- P-256 EC/ECDH and AES-GCM have been supported since very
 * early Android API levels, well below this app's minSdk 26.
 *
 * Curve name: `"secp256r1"` is the SEC/OpenSSL name for the exact same
 * curve NIST calls "P-256" and ANSI X9.62 calls "prime256v1" -- Java's EC
 * provider accepts any of the three aliases; `secp256r1` is used throughout
 * this file to match `ECGenParameterSpec`'s own most common convention.
 */
private const val CURVE_NAME = "secp256r1"

/** Field size in bytes for P-256 -- every raw coordinate (X, Y, or the private scalar D) is left-padded to exactly this length, matching how SubtleCrypto/JWK always represent P-256 coordinates as fixed 32-byte values (never variable-length BigInteger encoding). */
private const val FIELD_SIZE_BYTES = 32

actual fun createArchivePrimitives(): ArchivePrimitives = AndroidArchivePrimitives()

private class AndroidArchivePrimitives : ArchivePrimitives {
    private val secureRandom = SecureRandom()
    private val keyFactory = KeyFactory.getInstance("EC")

    /** The P-256 curve's domain parameters (a, b, field prime, base point, order, cofactor) -- needed to reconstruct a PublicKey/PrivateKey from raw coordinates, since Java's EC key specs require the full parameter set, not just the curve name. Computed once and reused (AlgorithmParameters lookups aren't free). */
    private val ecParams: ECParameterSpec by lazy {
        val params = AlgorithmParameters.getInstance("EC")
        params.init(ECGenParameterSpec(CURVE_NAME))
        params.getParameterSpec(ECParameterSpec::class.java)
    }

    override fun randomBytes(length: Int): ByteArray {
        val out = ByteArray(length)
        secureRandom.nextBytes(out)
        return out
    }

    override fun ecP256GenerateKeyPair(): EcKeyPair {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec(CURVE_NAME), secureRandom)
        val pair = generator.generateKeyPair()
        val publicPoint = pair.public as ECPublicKey
        val rawPoint = xyToRawPointBytes(publicPoint.w.affineX, publicPoint.w.affineY)
        return EcKeyPair(privateKeyPkcs8 = pair.private.encoded, publicKeyRawPoint = rawPoint)
    }

    override fun ecdhSharedSecretAsAesKey(privateKeyPkcs8: ByteArray, publicKeyRawPoint: ByteArray): ByteArray {
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyPkcs8))
        val publicKey = importPublicKey(publicKeyRawPoint)
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)
        // generateSecret() for "ECDH" returns exactly the raw x-coordinate of
        // the shared point, zero-padded to the field size -- see the
        // interface doc comment for why this is used directly as the AES key
        // with no further KDF, matching SubtleCrypto byte-for-byte.
        return agreement.generateSecret()
    }

    override fun ecPrivateKeyScalar(privateKeyPkcs8: ByteArray): ByteArray {
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyPkcs8)) as ECPrivateKey
        return privateKey.s.toFixedLengthBytes(FIELD_SIZE_BYTES)
    }

    override fun ecPrivateKeyFromScalar(d: ByteArray): ByteArray {
        val scalar = BigInteger(1, d)
        val spec = ECPrivateKeySpec(scalar, ecParams)
        return keyFactory.generatePrivate(spec).encoded
    }

    override fun aesGcmEncrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        // Cipher's own output already appends the 16-byte auth tag after the
        // ciphertext -- same framing SubtleCrypto's AES-GCM uses, so this is
        // byte-for-byte wire-compatible with d2m_web with no extra work.
        return cipher.doFinal(plaintext)
    }

    override fun aesGcmDecrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext) // throws AEADBadTagException on a bad key/tampered ciphertext -- the "wrong PIN" signal.
    }

    override fun pbkdf2Sha256(password: ByteArray, salt: ByteArray, iterations: Int, keyLengthBytes: Int): ByteArray {
        // PBEKeySpec takes a char[], not a byte[] -- JCE's PBKDF2WithHmacSHA256
        // converts each char to bytes internally. Decoding as UTF-8 first and
        // converting to chars keeps this correct for the numeric-only PINs
        // this system's UI actually collects (pure ASCII, so UTF-8 decode and
        // JCE's char->byte conversion agree byte-for-byte); see the interface
        // doc comment.
        val chars = password.decodeToString().toCharArray()
        val spec = PBEKeySpec(chars, salt, iterations, keyLengthBytes * 8)
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return secretKeyFactory.generateSecret(spec).encoded
    }

    private fun importPublicKey(rawPoint: ByteArray): java.security.PublicKey {
        val (x, y) = rawPointToXYBigIntegers(rawPoint)
        val spec = ECPublicKeySpec(ECPoint(x, y), ecParams)
        return keyFactory.generatePublic(spec)
    }
}

/** `0x04 || X(32) || Y(32)` uncompressed SEC1 point encoding, from two field-size-padded coordinates. */
private fun xyToRawPointBytes(x: BigInteger, y: BigInteger): ByteArray {
    val out = ByteArray(1 + 2 * FIELD_SIZE_BYTES)
    out[0] = 0x04
    x.toFixedLengthBytes(FIELD_SIZE_BYTES).copyInto(out, 1)
    y.toFixedLengthBytes(FIELD_SIZE_BYTES).copyInto(out, 1 + FIELD_SIZE_BYTES)
    return out
}

private fun rawPointToXYBigIntegers(rawPoint: ByteArray): Pair<BigInteger, BigInteger> {
    require(rawPoint.size == 1 + 2 * FIELD_SIZE_BYTES && rawPoint[0] == 0x04.toByte()) {
        "expected a 65-byte uncompressed EC point (0x04||X||Y), got ${rawPoint.size} bytes"
    }
    val x = BigInteger(1, rawPoint.copyOfRange(1, 1 + FIELD_SIZE_BYTES))
    val y = BigInteger(1, rawPoint.copyOfRange(1 + FIELD_SIZE_BYTES, 1 + 2 * FIELD_SIZE_BYTES))
    return x to y
}

/** BigInteger's own toByteArray() is variable-length and may include a leading zero sign byte -- neither is acceptable for a fixed-size field element (JWK/wire format both expect exactly `length` bytes, always). Handles both cases uniformly: an over-length encoding (sign byte, or just naturally >length -- shouldn't happen for valid P-256 field elements but handled defensively) keeps its trailing `length` bytes; an under-length one is left-padded with zeros. */
private fun BigInteger.toFixedLengthBytes(length: Int): ByteArray {
    val raw = this.toByteArray()
    val out = ByteArray(length)
    if (raw.size >= length) {
        raw.copyInto(out, 0, raw.size - length, raw.size)
    } else {
        raw.copyInto(out, length - raw.size)
    }
    return out
}
