package com.d2m.app.messaging.crypto.archive

/**
 * AES-GCM built on top of a raw AES block-encrypt function.
 *
 * Needed because Kotlin/Native's CommonCrypto bindings expose no GCM entry
 * point at all -- `CCCryptorGCM*` is absent from the generated
 * `platform.CoreCrypto` package (only CBC/CTR/ECB/OFB/CFB modes are
 * surfaced), and CryptoKit's `AES.GCM` is Swift-only and unreachable from
 * Kotlin/Native. So iOS supplies AES-ECB (which CommonCrypto does expose)
 * and this builds the GCM construction over it: GCM is, by definition,
 * CTR-mode encryption plus a GHASH authenticator, both of which are
 * straightforward given a block cipher.
 *
 * Lives in commonMain rather than iosMain purely so it can be diffed
 * against `javax.crypto`'s AES/GCM/NoPadding on the Android target -- see
 * `ArchiveInteropAndroidTest.kt`. Android's own actual keeps using
 * javax.crypto directly; nothing here is on Android's production path.
 *
 * Output framing is ciphertext with the 16-byte tag appended, matching both
 * `javax.crypto`'s AES/GCM/NoPadding and SubtleCrypto's AES-GCM byte for
 * byte -- which is what lets an archive wrapped on one platform be
 * unwrapped on the other (see ArchiveCrypto.wrapPrivateKeyWithPin).
 *
 * Scope note: only the 96-bit IV case is implemented, with no additional
 * authenticated data. That is not a shortcut -- 96 bits is the only IV
 * length SubtleCrypto's AES-GCM supports, this system never passes AAD, and
 * supporting the general case would mean the extra GHASH-derived-J0 path
 * with no caller to exercise it.
 */
internal object AesGcm {

    private const val BLOCK = 16
    const val TAG_SIZE = 16

    /** [blockEncrypt] must be raw AES-ECB encryption of exactly one 16-byte block under the archive key. */
    fun encrypt(blockEncrypt: (ByteArray) -> ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray {
        require(iv.size == 12) { "AES-GCM here supports only a 96-bit IV, got ${iv.size} bytes" }
        val h = blockEncrypt(ByteArray(BLOCK))
        val j0 = ByteArray(BLOCK).also { iv.copyInto(it, 0); it[15] = 1 }

        val ciphertext = gctr(blockEncrypt, inc32(j0), plaintext)
        val tag = computeTag(blockEncrypt, h, j0, ciphertext)
        return ciphertext + tag
    }

    /** Inverse of [encrypt]; throws when the appended tag does not verify. */
    fun decrypt(blockEncrypt: (ByteArray) -> ByteArray, iv: ByteArray, input: ByteArray): ByteArray {
        require(iv.size == 12) { "AES-GCM here supports only a 96-bit IV, got ${iv.size} bytes" }
        require(input.size >= TAG_SIZE) { "AES-GCM ciphertext is shorter than its own auth tag" }

        val ciphertext = input.copyOfRange(0, input.size - TAG_SIZE)
        val providedTag = input.copyOfRange(input.size - TAG_SIZE, input.size)

        val h = blockEncrypt(ByteArray(BLOCK))
        val j0 = ByteArray(BLOCK).also { iv.copyInto(it, 0); it[15] = 1 }
        val expectedTag = computeTag(blockEncrypt, h, j0, ciphertext)

        // Constant-time compare. The tag check IS this system's "is the PIN
        // right" test (see ArchivePrimitives.aesGcmDecrypt's doc comment), so
        // it is worth not leaking where the first mismatching byte is.
        var diff = 0
        for (i in 0 until TAG_SIZE) diff = diff or (expectedTag[i].toInt() xor providedTag[i].toInt())
        if (diff != 0) throw IllegalArgumentException("AES-GCM authentication tag mismatch")

        return gctr(blockEncrypt, inc32(j0), ciphertext)
    }

    private fun computeTag(blockEncrypt: (ByteArray) -> ByteArray, h: ByteArray, j0: ByteArray, ciphertext: ByteArray): ByteArray {
        var s = ByteArray(BLOCK)
        // No AAD, so the GHASH input is just the padded ciphertext followed
        // by the 128-bit length block.
        var offset = 0
        while (offset < ciphertext.size) {
            val block = ByteArray(BLOCK)
            val n = minOf(BLOCK, ciphertext.size - offset)
            ciphertext.copyInto(block, 0, offset, offset + n)
            s = gfMul(xor(s, block), h)
            offset += BLOCK
        }
        val lengths = ByteArray(BLOCK)
        writeBitLengthBE(lengths, 0, 0L) // len(AAD) = 0
        writeBitLengthBE(lengths, 8, ciphertext.size.toLong() * 8)
        s = gfMul(xor(s, lengths), h)

        return xor(s, blockEncrypt(j0))
    }

    private fun writeBitLengthBE(out: ByteArray, at: Int, bits: Long) {
        for (i in 0 until 8) out[at + i] = ((bits ushr (56 - 8 * i)) and 0xFF).toByte()
    }

    /** CTR-mode keystream application starting from [counter]. */
    private fun gctr(blockEncrypt: (ByteArray) -> ByteArray, counter: ByteArray, input: ByteArray): ByteArray {
        if (input.isEmpty()) return ByteArray(0)
        val out = ByteArray(input.size)
        var ctr = counter
        var offset = 0
        while (offset < input.size) {
            val keystream = blockEncrypt(ctr)
            val n = minOf(BLOCK, input.size - offset)
            for (i in 0 until n) out[offset + i] = (input[offset + i].toInt() xor keystream[i].toInt()).toByte()
            ctr = inc32(ctr)
            offset += BLOCK
        }
        return out
    }

    /** Increments the rightmost 32 bits of a counter block, wrapping -- GCM's inc32. */
    private fun inc32(block: ByteArray): ByteArray {
        val out = block.copyOf()
        for (i in 15 downTo 12) {
            val v = (out[i].toInt() and 0xFF) + 1
            out[i] = (v and 0xFF).toByte()
            if (v <= 0xFF) break
        }
        return out
    }

    private fun xor(a: ByteArray, b: ByteArray): ByteArray =
        ByteArray(a.size) { (a[it].toInt() xor b[it].toInt()).toByte() }

    /**
     * Multiplication in GF(2^128) under GCM's reduction polynomial, using
     * the bitwise right-shift method from NIST SP 800-38D's Algorithm 1.
     * GCM treats the leftmost bit of byte 0 as the most significant, hence
     * the shift direction and the 0xE1 reduction constant applied at the
     * top byte.
     */
    private fun gfMul(x: ByteArray, y: ByteArray): ByteArray {
        val z = ByteArray(BLOCK)
        val v = y.copyOf()
        for (i in 0 until 128) {
            val bit = (x[i / 8].toInt() ushr (7 - (i % 8))) and 1
            if (bit == 1) {
                for (j in 0 until BLOCK) z[j] = (z[j].toInt() xor v[j].toInt()).toByte()
            }
            val lsbSet = (v[15].toInt() and 1) == 1
            // v >>= 1 across the whole 128-bit value
            for (j in 15 downTo 1) {
                v[j] = (((v[j].toInt() and 0xFF) ushr 1) or ((v[j - 1].toInt() and 1) shl 7)).toByte()
            }
            v[0] = ((v[0].toInt() and 0xFF) ushr 1).toByte()
            if (lsbSet) v[0] = (v[0].toInt() xor 0xE1).toByte()
        }
        return z
    }
}
