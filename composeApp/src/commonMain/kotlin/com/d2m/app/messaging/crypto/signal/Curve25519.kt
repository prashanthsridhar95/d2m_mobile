package com.d2m.app.messaging.crypto.signal

/**
 * Pure-Kotlin Curve25519 (X25519 ECDH + XEdDSA sign/verify) with no
 * platform dependency, so BOTH targets can run byte-for-byte identical
 * curve math.
 *
 * WHY THIS EXISTS: Android's `CryptoPrimitives.android.kt` gets X25519 from
 * BouncyCastle and XEdDSA from `Ed25519Math.kt` (java.math.BigInteger).
 * Neither is reachable from Kotlin/Native, and Apple ships no C API for
 * Curve25519 that Kotlin/Native can bind to -- CryptoKit's Curve25519 types
 * are Swift-only, and CommonCrypto/Security cover hashes, HMAC, AES and
 * NIST curves but no Montgomery/Edwards curve at all. That gap is the
 * entire reason iOS was still on `StubUnencryptedCryptoProvider` (base64,
 * NOT encryption) while Android was on real Signal Protocol -- which meant
 * an Android<->iOS conversation could never work: each side handed the
 * other bytes it had no way to interpret.
 *
 * CORRECTNESS STRATEGY -- this file is deliberately NOT trusted on the
 * strength of having been written carefully. `CurveInteropTest.kt` pins it
 * against (a) the RFC 7748 §5.2/§6.1 X25519 test vectors, and (b) on the
 * Android target specifically, BouncyCastle's own X25519 and the existing
 * `Ed25519Math` BigInteger implementation, over randomised inputs. (b) is
 * the load-bearing one: it proves this code agrees with exactly the
 * implementation Android is already shipping, which is what makes
 * Android<->iOS interop a tested property rather than a hope.
 *
 * IMPLEMENTATION NOTES:
 *  - Field elements are 16 limbs of 16 bits (base 2^16, little-endian),
 *    kept fully reduced (< p) after every operation. 16-bit limbs are
 *    deliberate: a limb product stays under 2^32, so a whole column of
 *    partial products accumulates in a Long with no overflow risk, which
 *    removes the single most likely source of a silent wrong-bytes bug.
 *  - Reduction mod p = 2^255-19 folds the high half rather than dividing:
 *    2^256 = 2*2^255 = 38 (mod p), so `value = lo + 38*hi`. No general
 *    division appears anywhere in the field layer.
 *  - Edwards point arithmetic uses EXTENDED coordinates (X:Y:Z:T), not the
 *    affine coordinates `Ed25519Math.kt` uses. Affine needs a modular
 *    inversion per point addition; extended needs exactly one inversion at
 *    the very end. With a hand-written field that difference is ~500
 *    inversions vs 1 per scalar multiplication -- the difference between a
 *    visibly janky app and an unnoticeable one. Same results either way,
 *    which is what the cross-check test verifies.
 *  - Not constant-time, matching the precedent `Ed25519Math.kt` already
 *    set and for the same reason: these run a handful of times per session
 *    (identity/prekey generation, session setup), locally, never per
 *    message and never as a remote-timing oracle target.
 */
internal object Curve25519 {

    // ---------------------------------------------------------------
    // Variable-width unsigned big integers (base 2^16, little-endian).
    // Used ONLY for scalar arithmetic mod L, which happens a handful of
    // times per signature. The field layer below never touches these.
    // ---------------------------------------------------------------

    private fun bnFromBytesLE(bytes: ByteArray): IntArray {
        val limbs = IntArray((bytes.size + 1) / 2)
        for (i in bytes.indices) {
            val b = bytes[i].toInt() and 0xFF
            if (i % 2 == 0) limbs[i / 2] = limbs[i / 2] or b
            else limbs[i / 2] = limbs[i / 2] or (b shl 8)
        }
        return bnTrim(limbs)
    }

    private fun bnToBytesLE(a: IntArray, outLen: Int): ByteArray {
        val out = ByteArray(outLen)
        for (i in 0 until outLen) {
            val limb = if (i / 2 < a.size) a[i / 2] else 0
            out[i] = (if (i % 2 == 0) limb and 0xFF else (limb ushr 8) and 0xFF).toByte()
        }
        return out
    }

    private fun bnTrim(a: IntArray): IntArray {
        var n = a.size
        while (n > 1 && a[n - 1] == 0) n--
        return if (n == a.size) a else a.copyOf(n)
    }

    private fun bnCompare(a: IntArray, b: IntArray): Int {
        val n = maxOf(a.size, b.size)
        for (i in n - 1 downTo 0) {
            val av = if (i < a.size) a[i] else 0
            val bv = if (i < b.size) b[i] else 0
            if (av != bv) return if (av < bv) -1 else 1
        }
        return 0
    }

    /** a - b, requires a >= b. */
    private fun bnSub(a: IntArray, b: IntArray): IntArray {
        val out = IntArray(a.size)
        var borrow = 0
        for (i in a.indices) {
            var v = a[i] - borrow - (if (i < b.size) b[i] else 0)
            if (v < 0) { v += 0x10000; borrow = 1 } else borrow = 0
            out[i] = v
        }
        return bnTrim(out)
    }

    private fun bnMul(a: IntArray, b: IntArray): IntArray {
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
        return bnTrim(IntArray(out.size) { out[it].toInt() })
    }

    private fun bnBitLength(a: IntArray): Int {
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

    private fun bnBit(a: IntArray, index: Int): Boolean {
        val limb = index / 16
        if (limb >= a.size) return false
        return ((a[limb] ushr (index % 16)) and 1) == 1
    }

    /**
     * a mod m by plain restoring binary long division -- shift the
     * remainder left one bit at a time, pulling in the next bit of `a`, and
     * conditionally subtract `m`. Deliberately the most boring algorithm
     * available: it is called ~3 times per signature on a 512-bit input, so
     * there is nothing to gain from Barrett/Montgomery reduction and a
     * great deal to lose from getting one subtly wrong.
     */
    private fun bnMod(a: IntArray, m: IntArray): IntArray {
        if (bnCompare(a, m) < 0) return a
        val remLimbs = m.size + 1
        var rem = IntArray(remLimbs)
        for (i in bnBitLength(a) - 1 downTo 0) {
            // rem <<= 1
            var carry = 0
            for (k in 0 until remLimbs) {
                val v = (rem[k] shl 1) or carry
                rem[k] = v and 0xFFFF
                carry = (v ushr 16) and 1
            }
            if (bnBit(a, i)) rem[0] = rem[0] or 1
            if (bnCompare(rem, m) >= 0) {
                val sub = bnSub(rem, m)
                rem = IntArray(remLimbs) { if (it < sub.size) sub[it] else 0 }
            }
        }
        return bnTrim(rem)
    }

    /** Order of the Ed25519 base point, L = 2^252 + 27742317777372353535851937790883648493 (RFC 8032 §5.1). */
    private val L: IntArray = bnFromBytesLE(
        byteArrayOf(
            0xED.toByte(), 0xD3.toByte(), 0xF5.toByte(), 0x5C, 0x1A, 0x63, 0x12, 0x58,
            0xD6.toByte(), 0x9C.toByte(), 0xF7.toByte(), 0xA2.toByte(), 0xDE.toByte(), 0xF9.toByte(), 0xDE.toByte(), 0x14,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0x10,
        ),
    )

    // ---------------------------------------------------------------
    // Field arithmetic mod p = 2^255 - 19. Fixed 16 limbs, base 2^16.
    // Every returned element is fully reduced (< p).
    // ---------------------------------------------------------------

    private const val FE = 16

    private val P: IntArray = IntArray(FE).also {
        it[0] = 0xFFED
        for (i in 1..14) it[i] = 0xFFFF
        it[15] = 0x7FFF
    }

    private fun feZero() = IntArray(FE)

    private fun feFromInt(v: Int): IntArray = feZero().also { it[0] = v and 0xFFFF; it[1] = (v ushr 16) and 0xFFFF }

    /** Interprets 32 little-endian bytes as a field element, masking off bit 255 (per RFC 7748 §5) and reducing. */
    private fun feFromBytes(bytes: ByteArray): IntArray {
        val out = feZero()
        for (i in 0 until 32) {
            val b = bytes[i].toInt() and 0xFF
            if (i % 2 == 0) out[i / 2] = out[i / 2] or b else out[i / 2] = out[i / 2] or (b shl 8)
        }
        out[15] = out[15] and 0x7FFF
        return feCondSubP(out)
    }

    private fun feToBytes(a: IntArray): ByteArray {
        val out = ByteArray(32)
        for (i in 0 until 32) {
            out[i] = (if (i % 2 == 0) a[i / 2] and 0xFF else (a[i / 2] ushr 8) and 0xFF).toByte()
        }
        return out
    }

    private fun feIsZero(a: IntArray): Boolean {
        for (i in 0 until FE) if (a[i] != 0) return false
        return true
    }

    private fun feEquals(a: IntArray, b: IntArray): Boolean {
        for (i in 0 until FE) if (a[i] != b[i]) return false
        return true
    }

    private fun feCompareP(a: IntArray): Int {
        for (i in FE - 1 downTo 0) if (a[i] != P[i]) return if (a[i] < P[i]) -1 else 1
        return 0
    }

    /** Subtracts p while the value is >= p. At most a couple of iterations by construction. */
    private fun feCondSubP(a: IntArray): IntArray {
        var cur = a
        while (feCompareP(cur) >= 0) {
            val out = IntArray(FE)
            var borrow = 0
            for (i in 0 until FE) {
                var v = cur[i] - borrow - P[i]
                if (v < 0) { v += 0x10000; borrow = 1 } else borrow = 0
                out[i] = v
            }
            cur = out
        }
        return cur
    }

    private fun feAdd(a: IntArray, b: IntArray): IntArray {
        val out = IntArray(FE)
        var carry = 0
        for (i in 0 until FE) {
            val v = a[i] + b[i] + carry
            out[i] = v and 0xFFFF
            carry = v ushr 16
        }
        // a,b < p < 2^255 so the sum is < 2^256 and any carry out of limb 15
        // is a 2^256 term, which folds back in as 38 (mod p).
        if (carry != 0) {
            var c = carry.toLong() * 38
            for (i in 0 until FE) {
                val v = out[i] + c
                out[i] = (v and 0xFFFF).toInt()
                c = v ushr 16
            }
        }
        return feCondSubP(out)
    }

    private fun feSub(a: IntArray, b: IntArray): IntArray {
        val out = IntArray(FE)
        var borrow = 0
        for (i in 0 until FE) {
            var v = a[i] - borrow - b[i]
            if (v < 0) { v += 0x10000; borrow = 1 } else borrow = 0
            out[i] = v
        }
        if (borrow != 0) {
            // Result went negative: add p back once. Valid because both
            // inputs are already reduced, so a-b > -p.
            var carry = 0
            for (i in 0 until FE) {
                val v = out[i] + P[i] + carry
                out[i] = v and 0xFFFF
                carry = v ushr 16
            }
        }
        return feCondSubP(out)
    }

    private fun feMul(a: IntArray, b: IntArray): IntArray {
        val t = LongArray(2 * FE)
        for (i in 0 until FE) {
            val ai = a[i].toLong()
            if (ai == 0L) continue
            for (j in 0 until FE) t[i + j] += ai * b[j]
        }
        return feReduceWide(t)
    }

    private fun feSqr(a: IntArray): IntArray = feMul(a, a)

    /**
     * Folds a 32-limb (512-bit) product back into 16 limbs mod p using
     * 2^256 = 38 (mod p), then normalises. Two fold passes are enough: the
     * first leaves a value under 2^261-ish, the second under 2^256.
     */
    private fun feReduceWide(t: LongArray): IntArray {
        val r = LongArray(FE)
        for (i in 0 until FE) r[i] = t[i] + 38L * t[i + FE]

        var carry = 0L
        for (i in 0 until FE) {
            val v = r[i] + carry
            r[i] = v and 0xFFFF
            carry = v ushr 16
        }
        // Fold the overflow past 2^256 back in, twice if it re-overflows.
        var pass = 0
        while (carry != 0L && pass < 4) {
            var c = carry * 38
            for (i in 0 until FE) {
                val v = r[i] + c
                r[i] = v and 0xFFFF
                c = v ushr 16
            }
            carry = c
            pass++
        }
        return feCondSubP(IntArray(FE) { r[it].toInt() })
    }

    /** a^(p-2) mod p -- the modular inverse, via square-and-multiply over p-2's bit pattern. */
    private fun feInv(a: IntArray): IntArray {
        // p - 2 = 2^255 - 21
        val exp = run {
            val e = IntArray(FE)
            e[0] = 0xFFEB // 0xFFED - 2
            for (i in 1..14) e[i] = 0xFFFF
            e[15] = 0x7FFF
            e
        }
        return fePow(a, exp)
    }

    private fun fePow(base: IntArray, exp: IntArray): IntArray {
        var result = feFromInt(1)
        var acc = base.copyOf()
        for (i in 0 until FE * 16) {
            if (((exp[i / 16] ushr (i % 16)) and 1) == 1) result = feMul(result, acc)
            acc = feSqr(acc)
        }
        return result
    }

    /** Square root mod p using the p = 5 (mod 8) closed form; returns null when `a` is not a QR. */
    private fun feSqrt(a: IntArray): IntArray? {
        if (feIsZero(a)) return feZero()
        // exponent (p+3)/8
        val exp = run {
            // (p+3)/8 = (2^255 - 16)/8 = 2^252 - 2
            val e = IntArray(FE)
            e[0] = 0xFFFE
            for (i in 1..14) e[i] = 0xFFFF
            e[15] = 0x0FFF
            e
        }
        var candidate = fePow(a, exp)
        if (feEquals(feSqr(candidate), a)) return candidate
        // multiply by sqrt(-1) = 2^((p-1)/4)
        val expQ = run {
            // (p-1)/4 = (2^255 - 20)/4 = 2^253 - 5
            val e = IntArray(FE)
            e[0] = 0xFFFB
            for (i in 1..14) e[i] = 0xFFFF
            e[15] = 0x1FFF
            e
        }
        val sqrtMinus1 = fePow(feFromInt(2), expQ)
        candidate = feMul(candidate, sqrtMinus1)
        return if (feEquals(feSqr(candidate), a)) candidate else null
    }

    private fun feIsOdd(a: IntArray): Boolean = (a[0] and 1) == 1

    private fun feNegate(a: IntArray): IntArray = if (feIsZero(a)) a else feSub(P, a)

    /** Edwards curve constant d = -121665/121666 mod p. */
    private val D: IntArray by lazy {
        feMul(feNegate(feFromInt(121665)), feInv(feFromInt(121666)))
    }

    private val D2: IntArray by lazy { feAdd(D, D) }

    // ---------------------------------------------------------------
    // Ed25519 points in extended coordinates (X:Y:Z:T), x = X/Z, y = Y/Z,
    // T = X*Y/Z.
    // ---------------------------------------------------------------

    private class Pt(val x: IntArray, val y: IntArray, val z: IntArray, val t: IntArray)

    private val IDENTITY get() = Pt(feZero(), feFromInt(1), feFromInt(1), feZero())

    /**
     * "add-2008-hwcd-3" from the Explicit-Formulas Database -- the strongly
     * unified addition law for a = -1 twisted Edwards curves, so the same
     * routine correctly handles doubling (P == Q) and needs no special
     * cases. Matches the unified affine law `Ed25519Math.add` implements,
     * just projectively.
     */
    private fun ptAdd(p1: Pt, p2: Pt): Pt {
        val a = feMul(feSub(p1.y, p1.x), feSub(p2.y, p2.x))
        val b = feMul(feAdd(p1.y, p1.x), feAdd(p2.y, p2.x))
        val c = feMul(feMul(p1.t, D2), p2.t)
        val d = feMul(feAdd(p1.z, p1.z), p2.z)
        val e = feSub(b, a)
        val f = feSub(d, c)
        val g = feAdd(d, c)
        val h = feAdd(b, a)
        return Pt(feMul(e, f), feMul(g, h), feMul(f, g), feMul(e, h))
    }

    private fun ptScalarMult(scalarLE: IntArray, point: Pt): Pt {
        var result = IDENTITY
        var addend = point
        val bits = bnBitLength(scalarLE)
        for (i in 0 until bits) {
            if (bnBit(scalarLE, i)) result = ptAdd(result, addend)
            addend = ptAdd(addend, addend)
        }
        return result
    }

    private fun ptToAffine(p: Pt): Pair<IntArray, IntArray> {
        val zInv = feInv(p.z)
        return feMul(p.x, zInv) to feMul(p.y, zInv)
    }

    /** 32-byte little-endian y, with x's low bit stored in the top bit -- RFC 8032 §5.1.2. */
    private fun ptEncode(p: Pt): ByteArray {
        val (x, y) = ptToAffine(p)
        val out = feToBytes(y)
        if (feIsOdd(x)) out[31] = (out[31].toInt() or 0x80).toByte()
        return out
    }

    /** Recovers x from y via x^2 = (y^2-1)/(d*y^2+1), picking the root whose low bit matches `signBit`. */
    private fun ptFromY(y: IntArray, signBit: Boolean): Pt? {
        val y2 = feSqr(y)
        val u = feSub(y2, feFromInt(1))
        val v = feAdd(feMul(D, y2), feFromInt(1))
        if (feIsZero(v)) return null
        val xx = feMul(u, feInv(v))
        var x = feSqrt(xx) ?: return null
        if (feIsOdd(x) != signBit) x = feNegate(x)
        return Pt(x, y, feFromInt(1), feMul(x, y))
    }

    private fun ptDecode(encoded: ByteArray): Pt? {
        val signBit = (encoded[31].toInt() and 0x80) != 0
        val yBytes = encoded.copyOf()
        yBytes[31] = (yBytes[31].toInt() and 0x7F).toByte()
        return ptFromY(feFromBytes(yBytes), signBit)
    }

    private val BASE: Pt by lazy {
        // y = 4/5; the standard base point's x is even, so signBit = false.
        val y = feMul(feFromInt(4), feInv(feFromInt(5)))
        ptFromY(y, false) ?: error("Curve25519: base point construction failed")
    }

    // ---------------------------------------------------------------
    // X25519 (RFC 7748) -- Montgomery ladder on the u-coordinate.
    // ---------------------------------------------------------------

    /** RFC 7748 §5 scalar clamping. */
    private fun clamp(scalar: ByteArray): ByteArray {
        val k = scalar.copyOf()
        k[0] = (k[0].toInt() and 248).toByte()
        k[31] = ((k[31].toInt() and 127) or 64).toByte()
        return k
    }

    private val A24: IntArray by lazy { feFromInt(121665) }

    /**
     * X25519 scalar multiplication of a u-coordinate, per RFC 7748 §5's
     * reference ladder. Returns the resulting 32-byte u-coordinate.
     */
    fun x25519(scalar: ByteArray, uCoordinate: ByteArray): ByteArray {
        val k = clamp(scalar)
        val u = feFromBytes(uCoordinate)

        var x1 = u
        var x2 = feFromInt(1)
        var z2 = feZero()
        var x3 = u
        var z3 = feFromInt(1)
        var swap = 0

        for (t in 254 downTo 0) {
            val kt = (k[t / 8].toInt() ushr (t % 8)) and 1
            if ((swap xor kt) == 1) {
                val tx = x2; x2 = x3; x3 = tx
                val tz = z2; z2 = z3; z3 = tz
            }
            swap = kt

            val a = feAdd(x2, z2)
            val aa = feSqr(a)
            val b = feSub(x2, z2)
            val bb = feSqr(b)
            val e = feSub(aa, bb)
            val c = feAdd(x3, z3)
            val d = feSub(x3, z3)
            val da = feMul(d, a)
            val cb = feMul(c, b)
            x3 = feSqr(feAdd(da, cb))
            z3 = feMul(x1, feSqr(feSub(da, cb)))
            x2 = feMul(aa, bb)
            // RFC 7748 §5: z_2 = E * (AA + a24 * E). The AA here is easy to
            // mistype as BB (the two are used adjacently on the line above);
            // doing so still yields a self-consistent Diffie-Hellman -- both
            // parties derive the SAME shared secret, just not the RIGHT one --
            // so it survives a round-trip test and only the published RFC
            // vectors catch it. That is exactly how it was caught here.
            z2 = feMul(e, feAdd(aa, feMul(A24, e)))
        }
        if (swap == 1) {
            val tx = x2; x2 = x3; x3 = tx
            val tz = z2; z2 = z3; z3 = tz
        }
        return feToBytes(feMul(x2, feInv(z2)))
    }

    /** The RFC 7748 base point u = 9. */
    private val BASE_U: ByteArray = ByteArray(32).also { it[0] = 9 }

    fun x25519Base(scalar: ByteArray): ByteArray = x25519(scalar, BASE_U)

    /**
     * Generates an X25519 key pair from 32 random bytes. The private half
     * is returned CLAMPED, matching what BouncyCastle's
     * `X25519PrivateKeyParameters.encoded` hands back on Android (it clamps
     * on generation too), so a key generated on either platform behaves
     * identically everywhere.
     */
    fun generateKeyPair(random32: ByteArray): Pair<ByteArray, ByteArray> {
        val priv = clamp(random32)
        return priv to x25519Base(priv)
    }

    // ---------------------------------------------------------------
    // XEdDSA (https://signal.org/docs/specifications/xeddsa/)
    // ---------------------------------------------------------------

    /**
     * Byte-for-byte the same scheme `Ed25519Math.sign` implements, so a
     * signature produced here is indistinguishable from one Android's
     * BigInteger path would have produced for the same inputs:
     *   a  = scalar mod L                     (valid: B has order L)
     *   r  = SHA-512(scalar || random || m) mod L
     *   R  = rB,  h = SHA-512(R || A || m) mod L,  s = r + h*a mod L
     *   sig = R || s, with A's sign bit stored in the top bit of s.
     */
    fun xEdDSASign(privateScalar: ByteArray, message: ByteArray, random32: ByteArray): ByteArray {
        val a = bnMod(bnFromBytesLE(privateScalar), L)
        val encodedA = ptEncode(ptScalarMult(a, BASE))
        val signBit = (encodedA[31].toInt() and 0x80) != 0

        val r = bnMod(bnFromBytesLE(sha512(privateScalar + random32 + message)), L)
        val encodedR = ptEncode(ptScalarMult(r, BASE))
        val h = bnMod(bnFromBytesLE(sha512(encodedR + encodedA + message)), L)
        val s = bnMod(bnAddArr(bnMul(h, a), r), L)

        val sig = ByteArray(64)
        encodedR.copyInto(sig, 0)
        bnToBytesLE(s, 32).copyInto(sig, 32)
        if (signBit) sig[63] = (sig[63].toInt() or 0x80).toByte()
        return sig
    }

    private fun bnAddArr(a: IntArray, b: IntArray): IntArray {
        val n = maxOf(a.size, b.size) + 1
        val out = IntArray(n)
        var carry = 0
        for (i in 0 until n) {
            val v = (if (i < a.size) a[i] else 0) + (if (i < b.size) b[i] else 0) + carry
            out[i] = v and 0xFFFF
            carry = v ushr 16
        }
        return bnTrim(out)
    }

    /**
     * `montgomeryPublicKey` is the raw 32-byte X25519 public u-coordinate;
     * it is converted to the Edwards y via y = (u-1)/(u+1) before the
     * standard s*B == R + h*A check. Same conversion and sign-bit
     * convention as `Ed25519Math.verify`.
     */
    fun xEdDSAVerify(montgomeryPublicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        if (signature.size != 64) return false

        val u = feFromBytes(montgomeryPublicKey)
        val uPlus1 = feAdd(u, feFromInt(1))
        if (feIsZero(uPlus1)) return false
        val y = feMul(feSub(u, feFromInt(1)), feInv(uPlus1))

        val signBit = (signature[63].toInt() and 0x80) != 0
        val pointA = ptFromY(y, signBit) ?: return false
        val encodedA = ptEncode(pointA)

        val encodedR = signature.copyOfRange(0, 32)
        val sBytes = signature.copyOfRange(32, 64)
        sBytes[31] = (sBytes[31].toInt() and 0x7F).toByte()
        val s = bnFromBytesLE(sBytes)
        if (bnCompare(s, L) >= 0) return false

        val h = bnMod(bnFromBytesLE(sha512(encodedR + encodedA + message)), L)
        val pointR = ptDecode(encodedR) ?: return false

        val lhs = ptScalarMult(s, BASE)
        val rhs = ptAdd(pointR, ptScalarMult(h, pointA))

        // Compare in affine form -- projective coordinates are not unique.
        val (lx, ly) = ptToAffine(lhs)
        val (rx, ry) = ptToAffine(rhs)
        return feEquals(lx, rx) && feEquals(ly, ry)
    }
}
