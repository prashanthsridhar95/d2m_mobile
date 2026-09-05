package com.d2m.app.messaging.crypto.archive

import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Pins the shared `P256.kt` and `AesGcm.kt` -- the two pieces iOS's
 * `ArchivePrimitives.ios.kt` is built from -- against the `java.security`/
 * `javax.crypto` implementations Android's own actual uses.
 *
 * This is what makes cross-device archive restore real rather than hoped
 * for. `ArchiveCrypto.wrapPrivateKeyWithPin` publishes a PKCS#8 private key,
 * AES-GCM-wrapped under a PBKDF2 key, to the server; the other device pulls
 * it down and must unwrap and parse it with a completely different crypto
 * stack. Every assertion below is one place that could silently diverge.
 */
class ArchiveInteropAndroidTest {

    private val random = SecureRandom()
    private fun randomBytes(n: Int) = ByteArray(n).also { random.nextBytes(it) }

    private val androidPrimitives = createArchivePrimitives()

    // ---- P-256 ----

    /**
     * The single most important assertion for cross-device restore: take a
     * keypair Android generated, and check the shared code recovers the same
     * public point from the PKCS#8 blob Android would have published.
     */
    @Test
    fun sharedP256_derivesSamePublicKey_fromAndroidGeneratedPkcs8() {
        repeat(5) {
            val pair = androidPrimitives.ecP256GenerateKeyPair()
            val d = P256.decodePkcs8(pair.privateKeyPkcs8)

            assertEquals(32, d.size, "decoded P-256 scalar should be 32 bytes")
            assertContentEquals(
                pair.publicKeyRawPoint,
                P256.publicPointFromScalar(d),
                "public point derived by P256.kt differs from the one java.security generated",
            )
        }
    }

    @Test
    fun sharedP256_pkcs8Encoding_isByteIdenticalToAndroids() {
        repeat(5) {
            val pair = androidPrimitives.ecP256GenerateKeyPair()
            val d = P256.decodePkcs8(pair.privateKeyPkcs8)

            assertContentEquals(
                pair.privateKeyPkcs8,
                P256.encodePkcs8(d),
                "re-encoded PKCS#8 differs from java.security's own encoding",
            )
        }
    }

    /** And the re-encoded form must still be loadable by java.security, not merely byte-equal by luck. */
    @Test
    fun pkcs8EncodedByShared_isAcceptedByJavaSecurity() {
        val pair = androidPrimitives.ecP256GenerateKeyPair()
        val reEncoded = P256.encodePkcs8(P256.decodePkcs8(pair.privateKeyPkcs8))

        val key = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(reEncoded))
        assertEquals("EC", key.algorithm)
    }

    @Test
    fun sharedP256_ecdh_matchesJavaSecurity() {
        repeat(5) {
            val a = androidPrimitives.ecP256GenerateKeyPair()
            val b = androidPrimitives.ecP256GenerateKeyPair()

            val viaJava = androidPrimitives.ecdhSharedSecretAsAesKey(a.privateKeyPkcs8, b.publicKeyRawPoint)
            val viaShared = P256.ecdh(P256.decodePkcs8(a.privateKeyPkcs8), b.publicKeyRawPoint)

            assertContentEquals(viaJava, viaShared, "ECDH shared secret differs from javax.crypto's")
            // And the agreement must be symmetric across the two stacks.
            assertContentEquals(
                viaShared,
                P256.ecdh(P256.decodePkcs8(b.privateKeyPkcs8), a.publicKeyRawPoint),
                "ECDH is not symmetric",
            )
        }
    }

    @Test
    fun sharedP256_rejectsPointNotOnCurve() {
        val pair = androidPrimitives.ecP256GenerateKeyPair()
        val bogus = pair.publicKeyRawPoint.copyOf().also { it[40] = (it[40].toInt() xor 0xFF).toByte() }

        assertFailsWith<IllegalArgumentException> {
            P256.ecdh(P256.decodePkcs8(pair.privateKeyPkcs8), bogus)
        }
    }

    // ---- AES-GCM ----

    private fun androidAesEcbBlock(key: ByteArray): (ByteArray) -> ByteArray = { block ->
        Cipher.getInstance("AES/ECB/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        }.doFinal(block)
    }

    private fun javaGcmEncrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        }.doFinal(plaintext)

    @Test
    fun sharedAesGcm_encrypt_matchesJavaxCrypto() {
        repeat(20) { i ->
            val key = randomBytes(32)
            val iv = randomBytes(12)
            // Include empty, sub-block, exact-block and multi-block sizes --
            // GCM's padding/length handling is where those differ.
            val plaintext = randomBytes(intArrayOf(0, 1, 15, 16, 17, 64, 67)[i % 7])

            assertContentEquals(
                javaGcmEncrypt(key, iv, plaintext),
                AesGcm.encrypt(androidAesEcbBlock(key), iv, plaintext),
                "AES-GCM ciphertext+tag differs from javax.crypto's",
            )
        }
    }

    @Test
    fun sharedAesGcm_decrypts_whatJavaxCryptoEncrypted() {
        repeat(10) {
            val key = randomBytes(32)
            val iv = randomBytes(12)
            val plaintext = randomBytes(67)

            assertContentEquals(
                plaintext,
                AesGcm.decrypt(androidAesEcbBlock(key), iv, javaGcmEncrypt(key, iv, plaintext)),
            )
        }
    }

    /**
     * The wrong-PIN path. There is deliberately no separate "is this key
     * right" check anywhere in the Archive system -- this failure IS that
     * check (see ArchivePrimitives.aesGcmDecrypt's doc comment), so it has
     * to actually fire.
     */
    @Test
    fun sharedAesGcm_rejectsWrongKey() {
        val iv = randomBytes(12)
        val wrapped = javaGcmEncrypt(randomBytes(32), iv, randomBytes(48))

        assertFailsWith<IllegalArgumentException> {
            AesGcm.decrypt(androidAesEcbBlock(randomBytes(32)), iv, wrapped)
        }
    }

    @Test
    fun sharedAesGcm_rejectsTamperedCiphertext() {
        val key = randomBytes(32)
        val iv = randomBytes(12)
        val wrapped = javaGcmEncrypt(key, iv, randomBytes(48)).also { it[3] = (it[3].toInt() xor 0x01).toByte() }

        assertFailsWith<IllegalArgumentException> { AesGcm.decrypt(androidAesEcbBlock(key), iv, wrapped) }
    }

    // ---- end-to-end ----

    /**
     * The actual user-visible scenario, end to end: a PIN-wrapped archive
     * produced by Android's primitives, unwrapped using only the shared code
     * iOS runs.
     */
    @Test
    fun archiveWrappedByAndroid_unwrapsWithSharedCryptoOnly() {
        val pair = androidPrimitives.ecP256GenerateKeyPair()
        val pin = "482915"
        val salt = randomBytes(16)
        val iv = randomBytes(12)

        val wrappingKey = androidPrimitives.pbkdf2Sha256(pin.encodeToByteArray(), salt, 250_000, 32)
        val wrapped = androidPrimitives.aesGcmEncrypt(wrappingKey, iv, pair.privateKeyPkcs8)

        // Now unwrap using only what iOS would run: the same PBKDF2 output,
        // then the shared GCM and P-256 code.
        val unwrapped = AesGcm.decrypt(androidAesEcbBlock(wrappingKey), iv, wrapped)

        assertContentEquals(pair.privateKeyPkcs8, unwrapped, "unwrapped PKCS#8 differs from the original")
        assertContentEquals(
            pair.publicKeyRawPoint,
            P256.publicPointFromScalar(P256.decodePkcs8(unwrapped)),
            "public key recovered from the restored archive differs from the original",
        )
        assertTrue(P256.isValidScalar(P256.fromBytesBE(P256.decodePkcs8(unwrapped))))
    }
}
