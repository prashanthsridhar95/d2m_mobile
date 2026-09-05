package com.d2m.app.messaging.crypto.signal

import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.SecureRandom
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/**
 * THE cross-platform interop test. `Curve25519Test` proves the shared
 * `Curve25519.kt` matches the RFCs on every target including iOS; this
 * proves the same code matches the implementation ANDROID IS ALREADY
 * SHIPPING -- BouncyCastle for X25519, `Ed25519Math` (java.math.BigInteger)
 * for XEdDSA.
 *
 * Together those two facts are what make Android<->iOS messaging a tested
 * property rather than an assumption: iOS runs `Curve25519.kt`, Android runs
 * BouncyCastle/Ed25519Math, and these assertions say the two produce
 * identical bytes for identical inputs. Without this, the two apps could
 * each be internally consistent and still be unable to talk to each other --
 * which is precisely the state iOS was in before (its base64
 * `StubUnencryptedCryptoProvider` was perfectly self-consistent too).
 *
 * Randomised over many iterations rather than a couple of fixed vectors on
 * purpose: the ladder bug this suite caught during development produced
 * self-consistent but wrong results, so the failure mode worth defending
 * against is "agrees with itself, disagrees with the other implementation"
 * across the whole input space, not at one hand-picked point.
 */
class CurveInteropAndroidTest {

    private val random = SecureRandom()

    private fun randomBytes(n: Int) = ByteArray(n).also { random.nextBytes(it) }

    private fun bouncyCastleAgree(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
        val agreement = X25519Agreement()
        agreement.init(X25519PrivateKeyParameters(privateKey, 0))
        val out = ByteArray(agreement.agreementSize)
        agreement.calculateAgreement(X25519PublicKeyParameters(publicKey, 0), out, 0)
        return out
    }

    /**
     * Note on clamping, which is subtle enough to be worth stating: BC's
     * `X25519PrivateKeyParameters(bytes, 0)` CONSTRUCTOR does not clamp, it
     * stores the bytes as given, while `X25519KeyPairGenerator` (the entry
     * point `CryptoPrimitives.android.kt` actually uses) DOES clamp via
     * `X25519.generatePrivateKey`. So Android's stored private scalars are
     * always clamped in production, matching what
     * `Curve25519.generateKeyPair` returns. Asserting against the raw
     * constructor instead would compare a clamped scalar to an unclamped
     * seed and fail for a reason that has nothing to do with interop.
     */
    @Test
    fun bouncyCastleKeyGenerator_producesClampedScalars_asSharedKeygenAssumes() {
        repeat(25) {
            val generator = X25519KeyPairGenerator()
            generator.init(X25519KeyGenerationParameters(random))
            val priv = (generator.generateKeyPair().private as X25519PrivateKeyParameters).encoded

            assertContentEquals(
                priv,
                Curve25519.generateKeyPair(priv).first,
                "BouncyCastle's generated scalar is not in the clamped form Curve25519.generateKeyPair produces",
            )
        }
    }

    @Test
    fun sharedX25519_publicKeyDerivation_matchesBouncyCastle() {
        repeat(25) {
            val seed = randomBytes(32)
            val (sharedPriv, sharedPub) = Curve25519.generateKeyPair(seed)

            // Derive from the SAME (clamped) scalar on both sides. BC clamps
            // again internally during scalar multiplication, and clamping is
            // idempotent, so this is an apples-to-apples comparison.
            val bcPub = X25519PrivateKeyParameters(sharedPriv, 0).generatePublicKey().encoded

            assertContentEquals(bcPub, sharedPub, "derived public key differs from BouncyCastle's")
        }
    }

    @Test
    fun sharedX25519_agreement_matchesBouncyCastle() {
        repeat(25) {
            val (aPriv, _) = Curve25519.generateKeyPair(randomBytes(32))
            val (_, bPub) = Curve25519.generateKeyPair(randomBytes(32))

            assertContentEquals(
                bouncyCastleAgree(aPriv, bPub),
                Curve25519.x25519(aPriv, bPub),
                "ECDH shared secret differs from BouncyCastle's",
            )
        }
    }

    /**
     * The strongest statement available for the signature layer: with the
     * per-signature nonce pinned to the same bytes, XEdDSA is fully
     * deterministic, so the shared implementation must reproduce
     * `Ed25519Math`'s output exactly -- not merely produce something
     * Ed25519Math would accept.
     */
    @Test
    fun sharedXEdDSA_signature_isByteIdenticalToEd25519Math() {
        repeat(15) {
            val (priv, _) = Curve25519.generateKeyPair(randomBytes(32))
            val message = randomBytes(64)
            val nonce = randomBytes(32)

            assertContentEquals(
                Ed25519Math.sign(priv, message, nonce),
                Curve25519.xEdDSASign(priv, message, nonce),
                "XEdDSA signature differs from Android's existing Ed25519Math output",
            )
        }
    }

    @Test
    fun eachImplementation_verifiesTheOthersSignatures() {
        repeat(15) {
            val (priv, pub) = Curve25519.generateKeyPair(randomBytes(32))
            val message = randomBytes(48)

            val signedByShared = Curve25519.xEdDSASign(priv, message, randomBytes(32))
            val signedByAndroid = Ed25519Math.sign(priv, message, randomBytes(32))

            assertTrue(Ed25519Math.verify(pub, message, signedByShared), "Ed25519Math rejected a Curve25519.kt signature")
            assertTrue(Curve25519.xEdDSAVerify(pub, message, signedByAndroid), "Curve25519.kt rejected an Ed25519Math signature")
        }
    }

    @Test
    fun sharedImplementation_rejectsSignaturesEd25519MathAlsoRejects() {
        repeat(15) {
            val (priv, pub) = Curve25519.generateKeyPair(randomBytes(32))
            val message = randomBytes(32)
            val sig = Curve25519.xEdDSASign(priv, message, randomBytes(32))
            sig[random.nextInt(64)] = (sig[random.nextInt(64)].toInt() xor 0x40).toByte()

            assertTrue(
                Ed25519Math.verify(pub, message, sig) == Curve25519.xEdDSAVerify(pub, message, sig),
                "the two implementations disagreed on whether a tampered signature is valid",
            )
        }
    }
}
