package com.d2m.app.messaging.crypto.archive

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

/**
 * Runs against `createArchivePrimitives()` -- the PLATFORM actual -- so this
 * exercises Apple's CommonCrypto bindings on the iOS target and
 * `javax.crypto` on Android, from one set of assertions.
 *
 * That split matters: `ArchiveInteropAndroidTest` proves the shared
 * `P256.kt`/`AesGcm.kt` agree with Android's stack, but it runs only on
 * Android and so never touches a single line of the Kotlin/Native cinterop
 * in `ArchivePrimitives.ios.kt`. A wrong `CCCrypt` flag or a mis-pinned
 * pointer there would sail straight past it. These vectors are the check
 * that the iOS bindings themselves are wired up correctly.
 */
class ArchivePrimitivesTest {

    private val primitives = createArchivePrimitives()

    private fun hex(s: String): ByteArray =
        ByteArray(s.length / 2) { ((s[it * 2].digitToInt(16) shl 4) or s[it * 2 + 1].digitToInt(16)).toByte() }

    private fun ByteArray.hex(): String = joinToString("") { ((it.toInt() and 0xFF) + 0x100).toString(16).substring(1) }

    // ---- AES-GCM, NIST SP 800-38D / GCM spec test cases 13 and 14 (256-bit key) ----

    @Test
    fun aesGcm_nistCase13_emptyPlaintext() {
        val key = ByteArray(32)
        val iv = ByteArray(12)
        // For an empty plaintext the whole output is just the 16-byte tag.
        assertEquals(
            "530f8afbc74536b9a963b4f1c4cb738b",
            primitives.aesGcmEncrypt(key, iv, ByteArray(0)).hex(),
        )
    }

    @Test
    fun aesGcm_nistCase14_singleBlock() {
        val key = ByteArray(32)
        val iv = ByteArray(12)
        assertEquals(
            "cea7403d4d606b6e074ec5d3baf39d18" + "d0d1c8a799996bf0265b98b5d48ab919",
            primitives.aesGcmEncrypt(key, iv, ByteArray(16)).hex(),
        )
    }

    @Test
    fun aesGcm_roundTrips_acrossBlockBoundaries() {
        val key = primitives.randomBytes(32)
        val iv = primitives.randomBytes(12)
        for (size in intArrayOf(0, 1, 15, 16, 17, 64, 67, 300)) {
            val plaintext = primitives.randomBytes(size)
            val encrypted = primitives.aesGcmEncrypt(key, iv, plaintext)
            assertEquals(size + 16, encrypted.size, "GCM output should be plaintext + a 16-byte tag")
            assertContentEquals(plaintext, primitives.aesGcmDecrypt(key, iv, encrypted), "round trip failed at size $size")
        }
    }

    @Test
    fun aesGcm_wrongKeyFails() {
        val iv = primitives.randomBytes(12)
        val encrypted = primitives.aesGcmEncrypt(primitives.randomBytes(32), iv, primitives.randomBytes(32))
        // Deliberately assertFails, not a specific type: the tag-check
        // failure surfaces as IllegalArgumentException from AesGcm.kt on iOS
        // and as javax.crypto's AEADBadTagException on Android. The
        // ArchivePrimitives contract promises only that it throws, and
        // ArchiveCrypto catches broadly, so pinning a type here would assert
        // something neither platform actually guarantees.
        assertFails {
            primitives.aesGcmDecrypt(primitives.randomBytes(32), iv, encrypted)
        }
    }

    // ---- PBKDF2-HMAC-SHA256, RFC-style published vectors ----

    @Test
    fun pbkdf2_knownVector_oneIteration() {
        assertEquals(
            "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b",
            primitives.pbkdf2Sha256("password".encodeToByteArray(), "salt".encodeToByteArray(), 1, 32).hex(),
        )
    }

    @Test
    fun pbkdf2_knownVector_manyIterations() {
        assertEquals(
            "c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a",
            primitives.pbkdf2Sha256("password".encodeToByteArray(), "salt".encodeToByteArray(), 4096, 32).hex(),
        )
    }

    // ---- P-256 ----

    @Test
    fun p256_scalarOne_yieldsTheBasePoint() {
        val d = ByteArray(32).also { it[31] = 1 }
        assertEquals(
            "04" +
                "6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296" +
                "4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5",
            P256.publicPointFromScalar(d).hex(),
        )
    }

    @Test
    fun p256_generatedKeyPair_isWellFormed() {
        val pair = primitives.ecP256GenerateKeyPair()
        assertEquals(65, pair.publicKeyRawPoint.size)
        assertEquals(0x04.toByte(), pair.publicKeyRawPoint[0])
        assertTrue(P256.isValidScalar(P256.fromBytesBE(primitives.ecPrivateKeyScalar(pair.privateKeyPkcs8))))
        assertContentEquals(
            pair.publicKeyRawPoint,
            P256.publicPointFromScalar(primitives.ecPrivateKeyScalar(pair.privateKeyPkcs8)),
            "generated public point does not match its own private scalar",
        )
    }

    @Test
    fun p256_ecdh_isSymmetric() {
        val a = primitives.ecP256GenerateKeyPair()
        val b = primitives.ecP256GenerateKeyPair()
        assertContentEquals(
            primitives.ecdhSharedSecretAsAesKey(a.privateKeyPkcs8, b.publicKeyRawPoint),
            primitives.ecdhSharedSecretAsAesKey(b.privateKeyPkcs8, a.publicKeyRawPoint),
        )
    }

    @Test
    fun ecPrivateKeyScalar_roundTripsThroughPkcs8() {
        val pair = primitives.ecP256GenerateKeyPair()
        val d = primitives.ecPrivateKeyScalar(pair.privateKeyPkcs8)
        assertEquals(32, d.size)
        assertContentEquals(pair.privateKeyPkcs8, primitives.ecPrivateKeyFromScalar(d))
    }

    /** The whole PIN wrap/unwrap cycle on one platform's own primitives. */
    @Test
    fun pinWrappedPrivateKey_roundTrips() {
        val pair = primitives.ecP256GenerateKeyPair()
        val salt = primitives.randomBytes(16)
        val iv = primitives.randomBytes(12)
        // A deliberately low iteration count -- this asserts the plumbing,
        // not the KDF's work factor, and 250k would slow the suite down for
        // no added coverage.
        val key = primitives.pbkdf2Sha256("135790".encodeToByteArray(), salt, 1000, 32)

        val wrapped = primitives.aesGcmEncrypt(key, iv, pair.privateKeyPkcs8)
        assertContentEquals(pair.privateKeyPkcs8, primitives.aesGcmDecrypt(key, iv, wrapped))

        val wrongKey = primitives.pbkdf2Sha256("135791".encodeToByteArray(), salt, 1000, 32)
        assertFails { primitives.aesGcmDecrypt(wrongKey, iv, wrapped) }
    }
}
