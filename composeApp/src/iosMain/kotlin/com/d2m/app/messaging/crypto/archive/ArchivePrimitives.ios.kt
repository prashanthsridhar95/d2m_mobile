package com.d2m.app.messaging.crypto.archive

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.CCKeyDerivationPBKDF
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCOptionECBMode
import platform.CoreCrypto.kCCPBKDF2
import platform.CoreCrypto.kCCPRFHmacAlgSHA256
import platform.CoreCrypto.kCCSuccess
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import platform.posix.size_tVar

/**
 * Real Archive Keypair primitives for iOS, replacing eight methods that all
 * threw `NotImplementedError`.
 *
 * Those throws were previously unreachable and documented as such: iOS ran
 * `StubUnencryptedCryptoProvider`, so there was no Signal identity for an
 * archive to attach to and `ArchiveManager` never got far enough to call
 * them. Giving iOS real Signal crypto (see CryptoPrimitives.ios.kt) removes
 * exactly that guarantee -- `checkAfterConnect` now finds a real identity,
 * and the PIN setup/restore path becomes live. Leaving these throwing would
 * therefore have turned a dormant stub into an actual crash on the first
 * archive prompt, so this file is a required part of that change rather
 * than an independent nice-to-have.
 *
 * Where each piece comes from, and why:
 *  - PBKDF2-HMAC-SHA256 and the AES block cipher: CommonCrypto, i.e. Apple's
 *    own vetted implementations.
 *  - AES-GCM: `AesGcm.kt` in commonMain, over the CommonCrypto AES-ECB block
 *    function below. Kotlin/Native's CommonCrypto bindings expose no GCM
 *    mode at all, so the mode itself has to be assembled here.
 *  - P-256 keygen/ECDH and the PKCS#8 encoding: `P256.kt` in commonMain.
 *    Security framework can do P-256 ECDH, but cannot build a private key
 *    from a bare scalar -- which is all Android's PKCS#8 carries. See
 *    P256.kt's doc comment for the full reasoning.
 *
 * All three shared pieces are pinned against `java.security`/`javax.crypto`
 * on the Android target by `ArchiveInteropAndroidTest.kt`, so
 * "an archive created on Android restores on iOS" is a tested property.
 */
actual fun createArchivePrimitives(): ArchivePrimitives = IosArchivePrimitives()

@OptIn(ExperimentalForeignApi::class)
private class IosArchivePrimitives : ArchivePrimitives {

    override fun randomBytes(length: Int): ByteArray {
        if (length == 0) return ByteArray(0)
        val out = ByteArray(length)
        val status = out.usePinned { SecRandomCopyBytes(kSecRandomDefault, length.toULong(), it.addressOf(0)) }
        check(status == 0) { "SecRandomCopyBytes failed with status $status" }
        return out
    }

    override fun ecP256GenerateKeyPair(): EcKeyPair {
        // Rejection-sample until the scalar is in [1, n-1]. The rejection
        // probability for P-256 is around 2^-32, so this effectively never
        // loops -- but sampling mod n instead would introduce a (tiny) bias,
        // and there is no reason to accept one.
        while (true) {
            val d = randomBytes(32)
            if (!P256.isValidScalar(P256.fromBytesBE(d))) continue
            return EcKeyPair(
                privateKeyPkcs8 = P256.encodePkcs8(d),
                publicKeyRawPoint = P256.publicPointFromScalar(d),
            )
        }
    }

    override fun ecdhSharedSecretAsAesKey(privateKeyPkcs8: ByteArray, publicKeyRawPoint: ByteArray): ByteArray =
        P256.ecdh(P256.decodePkcs8(privateKeyPkcs8), publicKeyRawPoint)

    override fun ecPrivateKeyScalar(privateKeyPkcs8: ByteArray): ByteArray =
        P256.decodePkcs8(privateKeyPkcs8)

    override fun ecPrivateKeyFromScalar(d: ByteArray): ByteArray =
        P256.encodePkcs8(d)

    override fun aesGcmEncrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray =
        AesGcm.encrypt({ block -> aesEcbEncryptBlock(key, block) }, iv, plaintext)

    override fun aesGcmDecrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray =
        AesGcm.decrypt({ block -> aesEcbEncryptBlock(key, block) }, iv, ciphertext)

    /**
     * Raw AES-ECB on exactly one block, with NO padding -- the block-cipher
     * primitive GCM is defined over. ECB is correct and safe here precisely
     * because it is never used as a mode: GCM only ever asks it to encrypt
     * counter blocks and the hash subkey.
     */
    private fun aesEcbEncryptBlock(key: ByteArray, block: ByteArray): ByteArray {
        require(block.size == 16) { "AES block must be 16 bytes, was ${block.size}" }
        val out = ByteArray(16)
        memScoped {
            val moved = alloc<size_tVar>()
            val status = out.usePinned { pinnedOut ->
                key.usePinned { pinnedKey ->
                    block.usePinned { pinnedIn ->
                        CCCrypt(
                            kCCEncrypt,
                            kCCAlgorithmAES,
                            kCCOptionECBMode, // no padding bit set -> exactly one block in, one block out
                            pinnedKey.addressOf(0),
                            key.size.toULong(),
                            null, // ECB takes no IV
                            pinnedIn.addressOf(0),
                            16uL,
                            pinnedOut.addressOf(0),
                            16uL,
                            moved.ptr,
                        )
                    }
                }
            }
            check(status == kCCSuccess) { "CCCrypt(AES-ECB) failed with status $status" }
        }
        return out
    }

    override fun pbkdf2Sha256(password: ByteArray, salt: ByteArray, iterations: Int, keyLengthBytes: Int): ByteArray {
        val out = UByteArray(keyLengthBytes)
        val status = out.usePinned { pinnedOut ->
            salt.usePinned { pinnedSalt ->
                CCKeyDerivationPBKDF(
                    kCCPBKDF2,
                    // The binding types `password` as a C string. Every caller
                    // is ArchiveCrypto passing a numeric PIN's UTF-8 bytes, so
                    // decoding back to a String here is lossless; passwordLen
                    // is still given explicitly in bytes, so this does not rely
                    // on NUL termination.
                    password.decodeToString(),
                    password.size.toULong(),
                    pinnedSalt.addressOf(0).reinterpret(),
                    salt.size.toULong(),
                    kCCPRFHmacAlgSHA256,
                    iterations.toUInt(),
                    pinnedOut.addressOf(0),
                    keyLengthBytes.toULong(),
                )
            }
        }
        check(status == kCCSuccess) { "CCKeyDerivationPBKDF failed with status $status" }
        return out.toByteArray()
    }
}
