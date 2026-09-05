package com.d2m.app.messaging.crypto.archive

/**
 * Pure-Kotlin NIST P-256 (secp256r1) scalar multiplication and ECDH, plus
 * the PKCS#8 / SEC1 encodings the Archive Keypair system moves keys around
 * in.
 *
 * WHY THIS EXISTS, given `CryptoPrimitives.android.kt` gets all of this free
 * from `java.security`: the iOS actual cannot. Apple's Security framework
 * does expose P-256 ECDH (`SecKeyCopyKeyExchangeResult`), but
 * `SecKeyCreateWithData` will only build an EC private key from the full
 * ANSI X9.63 form `0x04 || X || Y || d` -- it cannot take a bare scalar. And
 * a bare scalar is exactly what this system has to work from: Android's
 * PKCS#8 encoding of a P-256 private key is 67 bytes carrying ONLY `d` (no
 * optional publicKey field -- confirmed by dumping a real
 * `KeyPairGenerator("EC")` key), and `wrapPrivateKeyWithPin` publishes those
 * very bytes to the server, so an iOS device restoring an archive created on
 * Android receives `d` and nothing else. Recovering X and Y from it means
 * computing d*G, which is the whole reason for the curve arithmetic below.
 *
 * Once d*G exists, doing the ECDH here too (rather than handing a
 * reconstructed key back to Security framework) is strictly simpler, and it
 * makes the result testable on every target instead of only on a device.
 * `ArchiveInteropAndroidTest.kt` pins all of it against `java.security`'s
 * own P-256 -- same strategy, and same reason, as
 * `Curve25519.kt`/`CurveInteropAndroidTest.kt`.
 *
 * PERFORMANCE: modular reduction is plain binary long division rather than
 * the Solinas fast reduction P-256's prime is specially shaped for, and
 * point arithmetic is Jacobian (one inversion per scalar multiplication
 * rather than one per addition). That combination lands a scalar
 * multiplication comfortably inside a few hundred milliseconds, which is the
 * right trade here: this runs on archive setup, archive restore, and once
 * per peer key exchange -- never per message, and never on a UI-blocking hot
 * path. Correctness and reviewability win over speed at that frequency.
 * Not constant-time, matching the precedent set by `Ed25519Math.kt`.
 */
internal object P256 {

    // ---------------------------------------------------------------
    // Minimal unsigned big-integer layer, base 2^16 little-endian.
    // ---------------------------------------------------------------

    private fun fromHex(hex: String): IntArray {
        val bytes = ByteArray(hex.length / 2) { ((hex[it * 2].digitToInt(16) shl 4) or hex[it * 2 + 1].digitToInt(16)).toByte() }
        return fromBytesBE(bytes)
    }

    fun fromBytesBE(bytes: ByteArray): IntArray {
        val limbs = IntArray((bytes.size + 1) / 2)
        for (i in bytes.indices) {
            val v = bytes[bytes.size - 1 - i].toInt() and 0xFF
            if (i % 2 == 0) limbs[i / 2] = limbs[i / 2] or v else limbs[i / 2] = limbs[i / 2] or (v shl 8)
        }
        return trim(limbs)
    }

    fun toBytesBE(a: IntArray, outLen: Int): ByteArray {
        val out = ByteArray(outLen)
        for (i in 0 until outLen) {
            val limb = if (i / 2 < a.size) a[i / 2] else 0
            val v = if (i % 2 == 0) limb and 0xFF else (limb ushr 8) and 0xFF
            out[outLen - 1 - i] = v.toByte()
        }
        return out
    }

    private fun trim(a: IntArray): IntArray {
        var n = a.size
        while (n > 1 && a[n - 1] == 0) n--
        return if (n == a.size) a else a.copyOf(n)
    }

    private fun isZero(a: IntArray): Boolean = a.all { it == 0 }

    private fun cmp(a: IntArray, b: IntArray): Int {
        val n = maxOf(a.size, b.size)
        for (i in n - 1 downTo 0) {
            val av = if (i < a.size) a[i] else 0
            val bv = if (i < b.size) b[i] else 0
            if (av != bv) return if (av < bv) -1 else 1
        }
        return 0
    }

    private fun add(a: IntArray, b: IntArray): IntArray {
        val n = maxOf(a.size, b.size) + 1
        val out = IntArray(n)
        var carry = 0
        for (i in 0 until n) {
            val v = (if (i < a.size) a[i] else 0) + (if (i < b.size) b[i] else 0) + carry
            out[i] = v and 0xFFFF
            carry = v ushr 16
        }
        return trim(out)
    }

    /** a - b, requires a >= b. */
    private fun sub(a: IntArray, b: IntArray): IntArray {
        val out = IntArray(a.size)
        var borrow = 0
        for (i in a.indices) {
            var v = a[i] - borrow - (if (i < b.size) b[i] else 0)
            if (v < 0) { v += 0x10000; borrow = 1 } else borrow = 0
            out[i] = v
        }
        return trim(out)
    }

    private fun mul(a: IntArray, b: IntArray): IntArray {
        val out = LongArray(a.size + b.size)
        for (i in a.indices) {
            if (a[i] == 0) continue
            var carry = 0L
            for (j in b.indices) {
                val cur = out[i + j] + a[i].toLong() * b[j].toLong() + carry
                out[i + j] = cur and 0xFFFF
                carry = cur ushr 16
            }
            var k = i + b.size
            while (carry != 0L) {
                val cur = out[k] + carry
                out[k] = cur and 0xFFFF
                carry = cur ushr 16
                k++
            }
        }
        return trim(IntArray(out.size) { out[it].toInt() })
    }

    private fun bitLength(a: IntArray): Int {
        for (i in a.size - 1 downTo 0) {
            if (a[i] != 0) {
                var bits = 0
                var v = a[i]
                while (v != 0) { bits++; v = v ushr 1 }
                return i * 16 + bits
            }
        }
        return 0
    }

    private fun bit(a: IntArray, index: Int): Boolean {
        val limb = index / 16
        if (limb >= a.size) return false
        return ((a[limb] ushr (index % 16)) and 1) == 1
    }

    /** a mod m by restoring binary long division -- see Curve25519.kt's identical helper for why the boring algorithm is the right one here. */
    private fun mod(a: IntArray, m: IntArray): IntArray {
        if (cmp(a, m) < 0) return a
        val width = m.size + 1
        var rem = IntArray(width)
        for (i in bitLength(a) - 1 downTo 0) {
            var carry = 0
            for (k in 0 until width) {
                val v = (rem[k] shl 1) or carry
                rem[k] = v and 0xFFFF
                carry = (v ushr 16) and 1
            }
            if (bit(a, i)) rem[0] = rem[0] or 1
            if (cmp(rem, m) >= 0) {
                val s = sub(rem, m)
                rem = IntArray(width) { if (it < s.size) s[it] else 0 }
            }
        }
        return trim(rem)
    }

    // ---- modular arithmetic over the field prime P ----

    private fun mAdd(a: IntArray, b: IntArray): IntArray {
        val s = add(a, b)
        return if (cmp(s, P) >= 0) sub(s, P) else s
    }

    private fun mSub(a: IntArray, b: IntArray): IntArray =
        if (cmp(a, b) >= 0) sub(a, b) else sub(add(a, P), b)

    private fun mMul(a: IntArray, b: IntArray): IntArray = mod(mul(a, b), P)

    private fun mSqr(a: IntArray): IntArray = mMul(a, a)

    private fun mPow(base: IntArray, exp: IntArray): IntArray {
        var result = ONE
        var acc = base
        for (i in 0 until bitLength(exp)) {
            if (bit(exp, i)) result = mMul(result, acc)
            acc = mSqr(acc)
        }
        return result
    }

    /** Fermat inverse: a^(p-2) mod p. P is prime, so this is well defined for every non-zero a. */
    private fun mInv(a: IntArray): IntArray = mPow(a, sub(P, TWO))

    // ---- curve parameters (SEC 2 / FIPS 186-4 P-256) ----

    private val ONE = intArrayOf(1)
    private val TWO = intArrayOf(2)

    private val P = fromHex("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF")
    private val B = fromHex("5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B")
    private val GX = fromHex("6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296")
    private val GY = fromHex("4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5")

    /** Order of the base point. */
    val N: IntArray = fromHex("FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551")

    /** Jacobian projective point: x = X/Z^2, y = Y/Z^3. Z == 0 is the point at infinity. */
    private class Jac(val x: IntArray, val y: IntArray, val z: IntArray)

    private val INFINITY get() = Jac(ONE, ONE, intArrayOf(0))

    private fun isInfinity(p: Jac) = isZero(p.z)

    /** "dbl-2001-b" -- the standard Jacobian doubling specialised for a = -3, which P-256 has. */
    private fun double(p: Jac): Jac {
        if (isInfinity(p) || isZero(p.y)) return INFINITY
        val delta = mSqr(p.z)
        val gamma = mSqr(p.y)
        val beta = mMul(p.x, gamma)
        val alpha = mMul(intArrayOf(3), mMul(mSub(p.x, delta), mAdd(p.x, delta)))
        val eightBeta = mMul(intArrayOf(8), beta)
        val x3 = mSub(mSqr(alpha), eightBeta)
        val z3 = mSub(mSub(mSqr(mAdd(p.y, p.z)), gamma), delta)
        val y3 = mSub(
            mMul(alpha, mSub(mMul(intArrayOf(4), beta), x3)),
            mMul(intArrayOf(8), mSqr(gamma)),
        )
        return Jac(x3, y3, z3)
    }

    private fun addPoints(p1: Jac, p2: Jac): Jac {
        if (isInfinity(p1)) return p2
        if (isInfinity(p2)) return p1

        val z1z1 = mSqr(p1.z)
        val z2z2 = mSqr(p2.z)
        val u1 = mMul(p1.x, z2z2)
        val u2 = mMul(p2.x, z1z1)
        val s1 = mMul(p1.y, mMul(z2z2, p2.z))
        val s2 = mMul(p2.y, mMul(z1z1, p1.z))

        if (cmp(u1, u2) == 0) {
            return if (cmp(s1, s2) == 0) double(p1) else INFINITY
        }

        val h = mSub(u2, u1)
        val r = mSub(s2, s1)
        val hh = mSqr(h)
        val hhh = mMul(h, hh)
        val u1hh = mMul(u1, hh)

        val x3 = mSub(mSub(mSqr(r), hhh), mMul(TWO, u1hh))
        val y3 = mSub(mMul(r, mSub(u1hh, x3)), mMul(s1, hhh))
        val z3 = mMul(mMul(p1.z, p2.z), h)
        return Jac(x3, y3, z3)
    }

    private fun scalarMult(k: IntArray, point: Jac): Jac {
        var result = INFINITY
        var addend = point
        for (i in 0 until bitLength(k)) {
            if (bit(k, i)) result = addPoints(result, addend)
            addend = double(addend)
        }
        return result
    }

    /** Jacobian -> affine (x, y), each as a 32-byte big-endian value. */
    private fun toAffine(p: Jac): Pair<IntArray, IntArray>? {
        if (isInfinity(p)) return null
        val zInv = mInv(p.z)
        val zInv2 = mSqr(zInv)
        return mMul(p.x, zInv2) to mMul(p.y, mMul(zInv2, zInv))
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /** True when `d` is a usable private scalar, i.e. 0 < d < n. */
    fun isValidScalar(d: IntArray): Boolean = !isZero(d) && cmp(d, N) < 0

    /** d*G as the 65-byte uncompressed SEC1 point `0x04 || X(32) || Y(32)`. */
    fun publicPointFromScalar(d: ByteArray): ByteArray {
        val k = fromBytesBE(d)
        require(isValidScalar(k)) { "P-256 private scalar out of range" }
        val (x, y) = toAffine(scalarMult(k, Jac(GX, GY, ONE)))
            ?: error("P-256 scalar multiplication produced the point at infinity")
        val out = ByteArray(65)
        out[0] = 0x04
        toBytesBE(x, 32).copyInto(out, 1)
        toBytesBE(y, 32).copyInto(out, 33)
        return out
    }

    /**
     * ECDH: returns the 32-byte big-endian X coordinate of d*peerPoint, with
     * no KDF applied. That raw X IS the agreed secret per NIST SP 800-56A
     * 5.7.1.2, and is exactly what both `javax.crypto`'s
     * `KeyAgreement("ECDH").generateSecret()` and SubtleCrypto's ECDH
     * `deriveBits` return -- see ArchivePrimitives.kt's own note on why there
     * is deliberately no HKDF step in this system.
     */
    fun ecdh(d: ByteArray, peerPoint: ByteArray): ByteArray {
        val peer = decodePoint(peerPoint)
        val k = fromBytesBE(d)
        require(isValidScalar(k)) { "P-256 private scalar out of range" }
        val (x, _) = toAffine(scalarMult(k, peer))
            ?: error("P-256 ECDH produced the point at infinity")
        return toBytesBE(x, 32)
    }

    /** Parses `0x04 || X || Y`, rejecting anything not actually on the curve. */
    private fun decodePoint(point: ByteArray): Jac {
        require(point.size == 65 && point[0] == 0x04.toByte()) {
            "expected a 65-byte uncompressed SEC1 point (0x04 || X || Y)"
        }
        val x = fromBytesBE(point.copyOfRange(1, 33))
        val y = fromBytesBE(point.copyOfRange(33, 65))
        require(cmp(x, P) < 0 && cmp(y, P) < 0) { "P-256 point coordinate out of field range" }

        // y^2 == x^3 - 3x + b. Skipping this check would make the ECDH below
        // an invalid-curve oracle: a peer could submit a point on a weaker
        // curve sharing this one's formulas and recover the private scalar
        // from the resulting shared secrets.
        val lhs = mSqr(y)
        val rhs = mAdd(mSub(mMul(x, mSqr(x)), mMul(intArrayOf(3), x)), B)
        require(cmp(lhs, rhs) == 0) { "P-256 point is not on the curve" }

        return Jac(x, y, ONE)
    }

    // ---------------------------------------------------------------
    // PKCS#8 / SEC1 encoding.
    //
    // The exact 67-byte shape Android's KeyPairGenerator("EC") emits for
    // P-256, reproduced byte-for-byte so a key generated on either platform
    // parses on the other. Everything except `d` is constant, so this is a
    // fixed prefix rather than a general DER writer:
    //   30 41                                  PrivateKeyInfo
    //      02 01 00                            version 0
    //      30 13                               AlgorithmIdentifier
    //         06 07 2A8648CE3D0201             OID ecPublicKey
    //         06 08 2A8648CE3D030107           OID prime256v1
    //      04 27                               privateKey OCTET STRING
    //         30 25                            ECPrivateKey
    //            02 01 01                      version 1
    //            04 20 <32 bytes of d>         the scalar
    // Note there is NO optional publicKey field -- see this object's top
    // doc comment for why that single fact drives the whole design.
    // ---------------------------------------------------------------

    private val PKCS8_PREFIX = byteArrayOf(
        0x30, 0x41, 0x02, 0x01, 0x00, 0x30, 0x13, 0x06, 0x07,
        0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x02, 0x01, 0x06, 0x08,
        0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x03, 0x01, 0x07,
        0x04, 0x27, 0x30, 0x25, 0x02, 0x01, 0x01, 0x04, 0x20,
    )

    fun encodePkcs8(d: ByteArray): ByteArray {
        require(d.size == 32) { "P-256 private scalar must be 32 bytes, was ${d.size}" }
        return PKCS8_PREFIX + d
    }

    /**
     * Extracts `d` from a PKCS#8 P-256 private key. Tolerates a trailing
     * optional publicKey field (some producers include one even though
     * Android's does not) by locating the inner ECPrivateKey's 32-byte
     * OCTET STRING rather than assuming a fixed total length.
     */
    fun decodePkcs8(pkcs8: ByteArray): ByteArray {
        if (pkcs8.size >= PKCS8_PREFIX.size + 32 &&
            PKCS8_PREFIX.indices.all { pkcs8[it] == PKCS8_PREFIX[it] }
        ) {
            return pkcs8.copyOfRange(PKCS8_PREFIX.size, PKCS8_PREFIX.size + 32)
        }
        // Fallback: scan for the ECPrivateKey header (30 25 02 01 01 04 20)
        // or its variable-length equivalent, then take the next 32 bytes.
        val marker = byteArrayOf(0x02, 0x01, 0x01, 0x04, 0x20)
        for (i in 0..pkcs8.size - marker.size - 32) {
            if (marker.indices.all { pkcs8[i + it] == marker[it] }) {
                return pkcs8.copyOfRange(i + marker.size, i + marker.size + 32)
            }
        }
        error("unrecognised PKCS#8 P-256 private key encoding (${pkcs8.size} bytes)")
    }
}
