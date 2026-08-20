package com.d2m.app.messaging.crypto.signal

import java.math.BigInteger
import java.security.MessageDigest

/**
 * XEdDSA (Signal's scheme for signing with a Curve25519 key directly --
 * https://signal.org/docs/specifications/xeddsa/, explicitly placed in the
 * public domain by Signal, same as the X3DH/Double Ratchet specs) --
 * implemented from scratch via `java.math.BigInteger` affine Edwards25519
 * arithmetic rather than any Ed25519 library's high-level sign/verify API.
 * Every mainstream Ed25519 implementation this project could otherwise use
 * (including BouncyCastle's `Ed25519Signer`) derives its scalar from a
 * 32-byte SEED via its own internal SHA-512 hash; XEdDSA instead needs to
 * sign with an ALREADY-clamped X25519 private scalar directly, and no
 * public API for that exists in this project's dependencies.
 *
 * Deliberately affine coordinates, not the optimized extended-coordinate
 * formulas real Ed25519 implementations use for speed -- this runs only a
 * handful of times per app session (identity/signed-prekey generation,
 * session establishment), never per-message, so clarity/auditability over
 * performance is the right trade here. Not constant-time either, for the
 * same reason (local, infrequent signing operations, not a network timing
 * oracle target).
 *
 * IMPORTANT security note on signing vs. verification: EdDSA VERIFICATION
 * depends only on the published equation (s*B == R + h*A) -- never on how
 * the signer derived its per-signature nonce `r`. So signatures produced
 * here interoperate correctly with any standard XEdDSA/Ed25519 verifier
 * (including `@privacyresearch/curve25519-typescript`, which d2m_web uses)
 * regardless of this file's exact nonce-derivation recipe, AS LONG AS the
 * verification equation and the Montgomery<->Edwards conversion + sign-bit
 * convention are correct -- both of which ARE implemented exactly per
 * libsignal's own C reference (`curve_sigs.c`, confirmed via direct source
 * inspection during research). The nonce derivation below
 * (SHA-512(scalar || random || message) mod L) is a safe, independently-
 * secure, standard choice -- not a byte-for-byte match of Signal's own
 * `crypto_sign_modified` internals (whose exact nonce formula could not be
 * retrieved during research), and does not need to be, for the reason above.
 */
internal object Ed25519Math {
    // BigInteger.TWO only exists from API 28 -- this app's minSdk is 26, so BigInteger.valueOf(2) throughout instead.
    private val TWO: BigInteger = BigInteger.valueOf(2)

    private val P: BigInteger = TWO.pow(255).subtract(BigInteger.valueOf(19))

    // RFC 8032 §5.1: order of the Ed25519 base point, L = 2^252 + 27742317777372353535851937790883648493.
    private val L: BigInteger = TWO.pow(252).add(BigInteger("27742317777372353535851937790883648493"))

    // d = -121665/121666 mod p (RFC 8032 §5.1's Edwards curve constant), computed rather than hardcoded as a hex literal.
    private val D: BigInteger = BigInteger.valueOf(-121665).mod(P).multiply(BigInteger.valueOf(121666).modInverse(P)).mod(P)

    // RFC 8032 §5.1's standard Ed25519 base point coordinates.
    private val BX: BigInteger = BigInteger("15112221349535400772501151409588531511454012693041857206046113283949847762202")
    private val BY: BigInteger = BigInteger("46316835694926478169428394003475163141307993866256225615783033603165251855960")

    private data class Point(val x: BigInteger, val y: BigInteger)

    private val IDENTITY = Point(BigInteger.ZERO, BigInteger.ONE)
    private val BASE = Point(BX.mod(P), BY.mod(P))

    /** Unified twisted-Edwards addition law for a=-1 (Ed25519) -- works for both point addition and doubling. */
    private fun add(p1: Point, p2: Point): Point {
        val x1y2 = p1.x.multiply(p2.y).mod(P)
        val y1x2 = p1.y.multiply(p2.x).mod(P)
        val y1y2 = p1.y.multiply(p2.y).mod(P)
        val x1x2 = p1.x.multiply(p2.x).mod(P)
        val dxxyy = D.multiply(x1x2).mod(P).multiply(y1y2).mod(P)
        val xNum = x1y2.add(y1x2).mod(P)
        val xDen = BigInteger.ONE.add(dxxyy).mod(P)
        val yNum = y1y2.add(x1x2).mod(P)
        val yDen = BigInteger.ONE.subtract(dxxyy).mod(P)
        val x3 = xNum.multiply(xDen.modInverse(P)).mod(P)
        val y3 = yNum.multiply(yDen.modInverse(P)).mod(P)
        return Point(x3, y3)
    }

    private fun scalarMult(k: BigInteger, point: Point): Point {
        var result = IDENTITY
        var addend = point
        var n = k
        while (n.signum() > 0) {
            if (n.testBit(0)) result = add(result, addend)
            addend = add(addend, addend)
            n = n.shiftRight(1)
        }
        return result
    }

    private fun scalarMultBase(k: BigInteger): Point = scalarMult(k, BASE)

    private fun encodePoint(point: Point): ByteArray {
        val out = point.y.mod(P).toLittleEndian32()
        if (point.x.testBit(0)) out[31] = (out[31].toInt() or 0x80).toByte()
        return out
    }

    /** Solves x^2 = (y^2-1)/(d*y^2+1) mod p, then picks the root matching `signBit`. */
    private fun decodePointFromY(y: BigInteger, signBit: Boolean): Point? {
        val y2 = y.multiply(y).mod(P)
        val u = y2.subtract(BigInteger.ONE).mod(P)
        val v = D.multiply(y2).mod(P).add(BigInteger.ONE).mod(P)
        val xx = u.multiply(v.modInverse(P)).mod(P)
        var x = sqrtMod(xx) ?: return null
        if (x.testBit(0) != signBit) x = P.subtract(x).mod(P)
        return Point(x, y)
    }

    private fun decodePointFull(encoded: ByteArray): Point? {
        val signBit = (encoded[31].toInt() and 0x80) != 0
        val yBytes = encoded.copyOf()
        yBytes[31] = (yBytes[31].toInt() and 0x7F).toByte()
        val y = fromLittleEndian(yBytes).mod(P)
        return decodePointFromY(y, signBit)
    }

    /** p ≡ 5 (mod 8) closed-form square root (Curve25519's field supports this -- Tonelli-Shanks isn't needed). */
    private fun sqrtMod(a: BigInteger): BigInteger? {
        if (a.mod(P).signum() == 0) return BigInteger.ZERO
        val exp = P.add(BigInteger.valueOf(3)).divide(BigInteger.valueOf(8))
        var candidate = a.modPow(exp, P)
        if (candidate.multiply(candidate).mod(P) == a.mod(P)) return candidate
        val sqrtMinus1 = TWO.modPow(P.subtract(BigInteger.ONE).divide(BigInteger.valueOf(4)), P)
        candidate = candidate.multiply(sqrtMinus1).mod(P)
        return if (candidate.multiply(candidate).mod(P) == a.mod(P)) candidate else null
    }

    private fun sha512(vararg parts: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-512")
        for (p in parts) md.update(p)
        return md.digest()
    }

    private fun hashToScalar(vararg parts: ByteArray): BigInteger = fromLittleEndian(sha512(*parts)).mod(L)

    /** Every call site passes an already-non-negative, already-reduced value (< P or < L, both well under 2^255) -- this only has to handle BigInteger.toByteArray()'s occasional extra leading 0x00 sign-guard byte, then reverse to little-endian and left-pad to 32 bytes. */
    private fun BigInteger.toLittleEndian32(): ByteArray {
        require(this.signum() >= 0) { "toLittleEndian32 called on a negative value -- caller must reduce mod P/L first" }
        val be = this.toByteArray()
        val unsigned = if (be.size > 1 && be[0] == 0.toByte()) be.copyOfRange(1, be.size) else be
        require(unsigned.size <= 32) { "value too large for 32 bytes" }
        val out = ByteArray(32)
        for (i in unsigned.indices) out[i] = unsigned[unsigned.size - 1 - i]
        return out
    }

    private fun fromLittleEndian(bytes: ByteArray): BigInteger {
        var result = BigInteger.ZERO
        for (i in bytes.indices.reversed()) {
            result = result.shiftLeft(8).or(BigInteger.valueOf((bytes[i].toInt() and 0xFF).toLong()))
        }
        return result
    }

    /**
     * `privateScalar` is the raw 32-byte X25519 private key, used directly
     * as the Ed25519 scalar (already clamped by X25519 key generation).
     * `message` is signed as-is -- the caller passes the exact bytes that
     * must match what the peer verifies against (the 33-byte 0x05-prefixed
     * public key form, per this package's wire-format convention -- see
     * SignalCodec.kt).
     */
    fun sign(privateScalar: ByteArray, message: ByteArray, random32: ByteArray): ByteArray {
        val a = fromLittleEndian(privateScalar).mod(L)
        val pointA = scalarMultBase(a)
        val encodedA = encodePoint(pointA)
        val signBit = (encodedA[31].toInt() and 0x80) != 0

        val r = hashToScalar(privateScalar, random32, message)
        val pointR = scalarMultBase(r)
        val encodedR = encodePoint(pointR)
        val h = hashToScalar(encodedR, encodedA, message)
        val s = r.add(h.multiply(a)).mod(L)

        val sig = ByteArray(64)
        encodedR.copyInto(sig, 0)
        s.toLittleEndian32().copyInto(sig, 32)
        if (signBit) sig[63] = (sig[63].toInt() or 0x80).toByte()
        return sig
    }

    /**
     * `montgomeryPublicKey` is the raw 32-byte X25519 public point
     * (u-coordinate) -- converted to the corresponding Edwards y-coordinate
     * (y = (u-1)/(u+1) mod p) before verifying.
     */
    fun verify(montgomeryPublicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        if (signature.size != 64) return false
        val u = fromLittleEndian(montgomeryPublicKey).mod(P)
        val uMinus1 = u.subtract(BigInteger.ONE).mod(P)
        val uPlus1 = u.add(BigInteger.ONE).mod(P)
        if (uPlus1.signum() == 0) return false
        val y = uMinus1.multiply(uPlus1.modInverse(P)).mod(P)

        val signBit = (signature[63].toInt() and 0x80) != 0
        val pointA = decodePointFromY(y, signBit) ?: return false
        val encodedA = encodePoint(pointA)

        val encodedR = signature.copyOfRange(0, 32)
        val sBytes = signature.copyOfRange(32, 64)
        sBytes[31] = (sBytes[31].toInt() and 0x7F).toByte()
        val s = fromLittleEndian(sBytes)
        if (s >= L) return false

        val h = hashToScalar(encodedR, encodedA, message)
        val pointR = decodePointFull(encodedR) ?: return false
        val sB = scalarMultBase(s)
        val hA = scalarMult(h, pointA)
        val rhs = add(pointR, hA)
        return sB.x == rhs.x && sB.y == rhs.y
    }
}
