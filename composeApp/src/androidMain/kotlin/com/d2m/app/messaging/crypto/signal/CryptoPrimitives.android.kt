package com.d2m.app.messaging.crypto.signal

import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Real primitives for the from-spec Signal Protocol port -- see
 * SignalProtocol.kt's top doc comment for the full picture and its
 * "needs real device verification" caveat, which applies to this file too.
 *
 * X25519 (ECDH) uses BouncyCastle (`org.bouncycastle:bcprov-jdk18on`,
 * MIT-style permissive license) because Android's built-in Conscrypt
 * provider only gained X25519 support in API 34 -- this app's minSdk is 26
 * (confirmed via research before implementing; see the license/library
 * research this pass started from). HMAC-SHA256 and AES-256-CBC use plain
 * `javax.crypto` (standard JCE algorithms, supported on every Android API
 * level this app targets) rather than BouncyCastle, to keep the dependency
 * surface as small as possible. XEdDSA sign/verify is
 * `Ed25519Math.kt`'s from-scratch BigInteger implementation -- see that
 * file's doc comment for why no library (BouncyCastle included) exposes a
 * usable API for it.
 *
 * Deliberately NOT Signal's own `libsignal` (AGPLv3) -- see git history /
 * gradle/libs.versions.toml's doc comment on bouncycastle-version for the
 * license research that led here.
 */
actual fun createCryptoPrimitives(): CryptoPrimitives = AndroidCryptoPrimitives()

private class AndroidCryptoPrimitives : CryptoPrimitives {
    private val secureRandom = SecureRandom()

    override fun randomBytes(length: Int): ByteArray {
        val out = ByteArray(length)
        secureRandom.nextBytes(out)
        return out
    }

    override fun x25519GenerateKeyPair(): Pair<ByteArray, ByteArray> {
        val generator = X25519KeyPairGenerator()
        generator.init(X25519KeyGenerationParameters(secureRandom))
        val pair = generator.generateKeyPair()
        val priv = (pair.private as X25519PrivateKeyParameters).encoded
        val pub = (pair.public as X25519PublicKeyParameters).encoded
        return priv to pub
    }

    override fun x25519Agree(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
        val agreement = X25519Agreement()
        agreement.init(X25519PrivateKeyParameters(privateKey, 0))
        val out = ByteArray(agreement.agreementSize)
        agreement.calculateAgreement(X25519PublicKeyParameters(publicKey, 0), out, 0)
        return out
    }

    override fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    override fun aesCbcEncrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray {
        // JCE's "PKCS5Padding" for a 128-bit-block cipher like AES IS PKCS7 padding -- a well-known JCE naming quirk (PKCS5 padding is technically defined only for 8-byte blocks; JCE reuses the name for the general PKCS7 scheme).
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(plaintext)
    }

    override fun aesCbcDecrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(ciphertext)
    }

    override fun xEdDSASign(privateKey: ByteArray, message: ByteArray): ByteArray =
        Ed25519Math.sign(privateKey, message, randomBytes(32))

    override fun xEdDSAVerify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean =
        Ed25519Math.verify(publicKey, message, signature)
}
