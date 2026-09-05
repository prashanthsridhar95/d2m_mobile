package com.d2m.app.messaging.crypto.signal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins `Curve25519.kt` against published, independently-authored test
 * vectors. This runs on EVERY target, so a green run on
 * `iosSimulatorArm64Test` is direct evidence the iOS build computes the
 * same curve results the RFC specifies -- and therefore the same ones
 * Android's BouncyCastle path computes. `CurveInteropAndroidTest.kt` closes
 * the loop from the other side by diffing this implementation against
 * BouncyCastle and `Ed25519Math` directly on the Android target.
 */
class Curve25519Test {

    private fun hex(s: String): ByteArray {
        val clean = s.replace(" ", "")
        return ByteArray(clean.length / 2) { ((clean[it * 2].digitToInt(16) shl 4) or clean[it * 2 + 1].digitToInt(16)).toByte() }
    }

    private fun ByteArray.hex(): String = joinToString("") { ((it.toInt() and 0xFF) + 0x100).toString(16).substring(1) }

    // ---- RFC 7748 §5.2, first X25519 test vector ----
    @Test
    fun rfc7748_scalarMult_vector1() {
        val scalar = hex("a546e36bf0527c9d3b16154b82465edd62144c0ac1fc5a18506a2244ba449ac4")
        val u = hex("e6db6867583030db3594c1a424b15f7c726624ec26b3353b10a903a6d0ab1c4c")
        val expected = "c3da55379de9c6908e94ea4df28d084f32eccf03491c71f754b4075577a28552"
        assertEquals(expected, Curve25519.x25519(scalar, u).hex())
    }

    // ---- RFC 7748 §5.2, second X25519 test vector ----
    @Test
    fun rfc7748_scalarMult_vector2() {
        val scalar = hex("4b66e9d4d1b4673c5ad22691957d6af5c11b6421e0ea01d42ca4169e7918ba0d")
        val u = hex("e5210f12786811d3f4b7959d0538ae2c31dbe7106fc03c3efc4cd549c715a493")
        val expected = "95cbde9476e8907d7aade45cb4b873f88b595a68799fa152e6f8f7647aac7957"
        assertEquals(expected, Curve25519.x25519(scalar, u).hex())
    }

    /**
     * RFC 7748 §6.1's full Diffie-Hellman example. This is the property the
     * whole messaging layer actually depends on -- both sides deriving the
     * same shared secret -- so it is worth asserting end to end rather than
     * only asserting the scalar multiplication underneath it.
     */
    @Test
    fun rfc7748_diffieHellman_producesMatchingSharedSecret() {
        val alicePriv = hex("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a")
        val alicePub = hex("8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a")
        val bobPriv = hex("5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb")
        val bobPub = hex("de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f")
        val expectedShared = "4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742"

        assertEquals(alicePub.hex(), Curve25519.x25519Base(alicePriv).hex())
        assertEquals(bobPub.hex(), Curve25519.x25519Base(bobPriv).hex())
        assertEquals(expectedShared, Curve25519.x25519(alicePriv, bobPub).hex())
        assertEquals(expectedShared, Curve25519.x25519(bobPriv, alicePub).hex())
    }

    @Test
    fun generatedKeyPair_agreesBothDirections() {
        val (aPriv, aPub) = Curve25519.generateKeyPair(ByteArray(32) { (it * 7 + 1).toByte() })
        val (bPriv, bPub) = Curve25519.generateKeyPair(ByteArray(32) { (it * 13 + 5).toByte() })
        assertEquals(
            Curve25519.x25519(aPriv, bPub).hex(),
            Curve25519.x25519(bPriv, aPub).hex(),
        )
    }

    @Test
    fun xEdDSA_signature_verifies() {
        val (priv, pub) = Curve25519.generateKeyPair(ByteArray(32) { (it * 3 + 9).toByte() })
        val message = "the quick brown fox".encodeToByteArray()
        val sig = Curve25519.xEdDSASign(priv, message, ByteArray(32) { (it + 42).toByte() })
        assertEquals(64, sig.size)
        assertTrue(Curve25519.xEdDSAVerify(pub, message, sig))
    }

    @Test
    fun xEdDSA_rejectsTamperedMessage() {
        val (priv, pub) = Curve25519.generateKeyPair(ByteArray(32) { (it * 5 + 2).toByte() })
        val sig = Curve25519.xEdDSASign(priv, "hello".encodeToByteArray(), ByteArray(32) { it.toByte() })
        assertFalse(Curve25519.xEdDSAVerify(pub, "hell0".encodeToByteArray(), sig))
    }

    @Test
    fun xEdDSA_rejectsTamperedSignature() {
        val (priv, pub) = Curve25519.generateKeyPair(ByteArray(32) { (it * 11 + 4).toByte() })
        val message = "hello".encodeToByteArray()
        val sig = Curve25519.xEdDSASign(priv, message, ByteArray(32) { it.toByte() })
        sig[5] = (sig[5].toInt() xor 0x01).toByte()
        assertFalse(Curve25519.xEdDSAVerify(pub, message, sig))
    }

    @Test
    fun xEdDSA_rejectsWrongSigner() {
        val (priv, _) = Curve25519.generateKeyPair(ByteArray(32) { (it + 1).toByte() })
        val (_, otherPub) = Curve25519.generateKeyPair(ByteArray(32) { (it + 90).toByte() })
        val message = "hello".encodeToByteArray()
        val sig = Curve25519.xEdDSASign(priv, message, ByteArray(32) { it.toByte() })
        assertFalse(Curve25519.xEdDSAVerify(otherPub, message, sig))
    }

    /** NIST FIPS 180-4's published SHA-512 example, checking the per-platform hash actual. */
    @Test
    fun sha512_matchesKnownDigest() {
        assertEquals(
            "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" +
                "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            sha512("abc".encodeToByteArray()).hex(),
        )
    }

    @Test
    fun sha512_handlesEmptyInput() {
        assertEquals(
            "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce" +
                "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
            sha512(ByteArray(0)).hex(),
        )
    }
}
