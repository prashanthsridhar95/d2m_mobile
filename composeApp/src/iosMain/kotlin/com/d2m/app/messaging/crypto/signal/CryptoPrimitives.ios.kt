package com.d2m.app.messaging.crypto.signal

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCBlockSizeAES128
import platform.CoreCrypto.kCCDecrypt
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCHmacAlgSHA256
import platform.CoreCrypto.kCCOptionPKCS7Padding
import platform.CoreCrypto.kCCSuccess
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import platform.posix.size_tVar

/**
 * Real Signal Protocol primitives for iOS -- the actual that finally lets
 * `createCryptoProvider.ios.kt` bind a real `SignalCryptoProvider` instead
 * of `StubUnencryptedCryptoProvider`. Before this existed, iOS transmitted
 * base64 while Android transmitted real Signal ciphertext, so an
 * Android<->iOS conversation was not merely insecure, it was
 * non-functional: neither side could read the other at all.
 *
 * Split of responsibilities, and why:
 *  - RNG, HMAC-SHA256 and AES-256-CBC come from Apple's own CommonCrypto /
 *    Security frameworks. These are the parts where a vetted platform
 *    implementation exists and there is no interop ambiguity, so using one
 *    is strictly better than writing one.
 *  - X25519 and XEdDSA come from `Curve25519.kt` in commonMain, because no
 *    Apple C API exposes Curve25519 to Kotlin/Native at all (CryptoKit is
 *    Swift-only). That shared code is pinned against BouncyCastle's output
 *    on the Android target by `CurveInteropTest.kt`, which is what makes
 *    "the two platforms agree" a tested property.
 *
 * PKCS7 note: `kCCOptionPKCS7Padding` is the same padding scheme Android's
 * JCE calls "PKCS5Padding" for AES (a long-standing JCE naming quirk -- see
 * CryptoPrimitives.android.kt's own comment) and the same one the Web
 * Crypto API applies for AES-CBC, so all three of Android, iOS and d2m_web
 * pad identically.
 */
actual fun createCryptoPrimitives(): CryptoPrimitives = IosCryptoPrimitives()

@OptIn(ExperimentalForeignApi::class)
private class IosCryptoPrimitives : CryptoPrimitives {

    override fun randomBytes(length: Int): ByteArray {
        if (length == 0) return ByteArray(0)
        val out = ByteArray(length)
        val status = out.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, length.toULong(), pinned.addressOf(0))
        }
        check(status == 0) { "SecRandomCopyBytes failed with status $status" }
        return out
    }

    override fun x25519GenerateKeyPair(): Pair<ByteArray, ByteArray> =
        Curve25519.generateKeyPair(randomBytes(32))

    override fun x25519Agree(privateKey: ByteArray, publicKey: ByteArray): ByteArray =
        Curve25519.x25519(privateKey, publicKey)

    override fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val out = ByteArray(32)
        out.usePinned { pinnedOut ->
            // A zero-length key or message is legal for HMAC but cannot be
            // pinned, so those cases pass a null pointer with length 0.
            withOptionalPin(key) { keyPtr ->
                withOptionalPin(data) { dataPtr ->
                    CCHmac(
                        kCCHmacAlgSHA256,
                        keyPtr,
                        key.size.toULong(),
                        dataPtr,
                        data.size.toULong(),
                        pinnedOut.addressOf(0),
                    )
                }
            }
        }
        return out
    }

    override fun aesCbcEncrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray =
        aesCbc(kCCEncrypt, key, iv, plaintext)

    override fun aesCbcDecrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray =
        aesCbc(kCCDecrypt, key, iv, ciphertext)

    private fun aesCbc(operation: UInt, key: ByteArray, iv: ByteArray, input: ByteArray): ByteArray {
        require(iv.size == kCCBlockSizeAES128.toInt()) { "AES-CBC IV must be ${kCCBlockSizeAES128} bytes, was ${iv.size}" }
        // PKCS7 can grow the output by up to one full block on encrypt.
        val outCapacity = input.size + kCCBlockSizeAES128.toInt()
        val out = ByteArray(outCapacity)

        val moved = memScoped {
            val movedVar = alloc<size_tVar>()
            val status = out.usePinned { pinnedOut ->
                key.usePinned { pinnedKey ->
                    iv.usePinned { pinnedIv ->
                        withOptionalPin(input) { inPtr ->
                            CCCrypt(
                                operation,
                                kCCAlgorithmAES,
                                kCCOptionPKCS7Padding,
                                pinnedKey.addressOf(0),
                                key.size.toULong(),
                                pinnedIv.addressOf(0),
                                inPtr,
                                input.size.toULong(),
                                pinnedOut.addressOf(0),
                                outCapacity.toULong(),
                                movedVar.ptr,
                            )
                        }
                    }
                }
            }
            check(status == kCCSuccess) { "CCCrypt failed with status $status" }
            movedVar.value.toInt()
        }
        return out.copyOf(moved)
    }

    override fun xEdDSASign(privateKey: ByteArray, message: ByteArray): ByteArray =
        Curve25519.xEdDSASign(privateKey, message, randomBytes(32))

    override fun xEdDSAVerify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean =
        Curve25519.xEdDSAVerify(publicKey, message, signature)
}

/**
 * Runs [block] with a pinned pointer to [bytes], or with a null pointer
 * when the array is empty. Kotlin/Native's `usePinned` cannot pin a
 * zero-length array (`addressOf(0)` is an out-of-bounds index), and every
 * CommonCrypto entry point here treats a null pointer with length 0 as an
 * empty input, so this keeps the empty case correct instead of crashing.
 */
@OptIn(ExperimentalForeignApi::class)
private inline fun <R> withOptionalPin(bytes: ByteArray, block: (kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?) -> R): R =
    if (bytes.isEmpty()) block(null) else bytes.usePinned { block(it.addressOf(0)) }
