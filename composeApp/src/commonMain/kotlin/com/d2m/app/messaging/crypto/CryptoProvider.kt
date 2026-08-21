package com.d2m.app.messaging.crypto

/**
 * ============================================================================
 *  SECURITY BOUNDARY -- READ BEFORE CHANGING THE DEFAULT BINDING IN di/AppModule.kt
 * ============================================================================
 *
 * This interface is the seam where real Signal Protocol encryption (X3DH +
 * Double Ratchet, per D2M_Messaging_Integration_Plan.md and
 * messaging-framework/packages/client-web/src/crypto.ts) needs to be
 * implemented before this app carries any real user conversation content.
 *
 * `StubUnencryptedCryptoProvider` below is bound by default in di/AppModule.kt
 * and does NOT encrypt anything -- it base64-encodes plaintext and calls it
 * done. It exists so the rest of the messaging stack (WS transport, wire
 * protocol, chat UI, repository plumbing) can be built, wired, and used for
 * development against a real messaging-framework server today, without
 * blocking on a cryptographic implementation that deserves its own focused
 * effort and review.
 *
 * Why this wasn't implemented for real in this pass: X3DH key agreement and
 * the Double Ratchet are exactly the kind of primitive where a
 * fast, unreviewed implementation is worse than no implementation --
 * mistakes here are silent (still "looks encrypted") and high-consequence
 * (private conversations). The right path is one of:
 *   1. Signal's own `libsignal` (Rust core, JNI bindings) on Android --
 *      no equivalent maintained Kotlin/Native binding for iOS as of this
 *      writing (see the mobile plan's §6 Option A cost note).
 *   2. A vetted third-party KMP Signal Protocol implementation, if one
 *      reaches a maintained, audited state.
 *   3. Porting crypto.ts's logic by hand against a KMP crypto primitives
 *      library (e.g. a multiplatform libsodium binding) -- the highest-effort,
 *      highest-control option, and the one the integration plan's Phase 1
 *      effectively assumes for the web side too (it ports crypto.ts as TS,
 *      not rewrites it).
 *
 * Whichever path is chosen, the contract below is what the rest of this
 * module needs satisfied -- implement it, then flip the single Koin binding
 * in di/AppModule.kt. Nothing else in messaging/ needs to change.
 */
interface CryptoProvider {
    /** True only for a real, reviewed implementation -- MessagingRepository surfaces this in UI so a stub build can warn users visibly rather than silently. */
    val isProductionGrade: Boolean

    suspend fun ensureIdentity(username: String)

    /** Encrypts `plaintext` for `toUsername`, returning (ciphertextType, base64 body) to place in a MessageEnvelope. */
    suspend fun encrypt(toUsername: String, plaintext: String): Pair<String, String>

    /** Decrypts a MessageEnvelope.body from `fromUsername` back to plaintext. */
    suspend fun decrypt(fromUsername: String, ciphertextType: String, body: String): String

    /**
     * Forgets the local session with `peerUsername` so the next [encrypt] call
     * re-establishes fresh (a new X3DH handshake against a newly-fetched
     * bundle). Mirrors d2m_web's `crypto.resetPeer` -- MessagingRepository
     * calls this both when ITS OWN decrypt fails (our side is desynced too,
     * not just the peer's) and when a peer's `session.reset` request arrives
     * (they couldn't decrypt us). Skipping this step was a real bug: without
     * it, "reset" only re-sent undelivered messages over the SAME stale
     * session, which the other side -- having genuinely reset -- still
     * couldn't decrypt, producing a reset loop instead of actually
     * recovering.
     */
    suspend fun resetSession(peerUsername: String)
}

/**
 * NOT SECURE. NOT END-TO-END ENCRYPTED. See this file's top-of-file doc
 * comment. Base64 is not encryption -- this exists purely so the transport/
 * UI layers have something to run against during development.
 */
class StubUnencryptedCryptoProvider : CryptoProvider {
    override val isProductionGrade: Boolean = false

    override suspend fun ensureIdentity(username: String) {
        // No key material to generate -- nothing to do for the stub.
    }

    override suspend fun encrypt(toUsername: String, plaintext: String): Pair<String, String> {
        val encoded = plaintext.encodeToByteArray().base64Encode()
        return "stub" to encoded
    }

    override suspend fun decrypt(fromUsername: String, ciphertextType: String, body: String): String =
        body.base64Decode().decodeToString()

    override suspend fun resetSession(peerUsername: String) {
        // No session state exists for the stub -- nothing to forget.
    }
}

// Minimal, dependency-free base64 -- avoids pulling in a platform-specific
// codec just for the stub provider. A real CryptoProvider implementation
// will bring its own crypto library with proper encoding utilities anyway.
private val BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

private fun ByteArray.base64Encode(): String {
    val sb = StringBuilder()
    var i = 0
    while (i < size) {
        val b0 = this[i].toInt() and 0xFF
        val b1 = if (i + 1 < size) this[i + 1].toInt() and 0xFF else 0
        val b2 = if (i + 2 < size) this[i + 2].toInt() and 0xFF else 0
        sb.append(BASE64_ALPHABET[b0 shr 2])
        sb.append(BASE64_ALPHABET[((b0 and 0x03) shl 4) or (b1 shr 4)])
        sb.append(if (i + 1 < size) BASE64_ALPHABET[((b1 and 0x0F) shl 2) or (b2 shr 6)] else '=')
        sb.append(if (i + 2 < size) BASE64_ALPHABET[b2 and 0x3F] else '=')
        i += 3
    }
    return sb.toString()
}

private fun String.base64Decode(): ByteArray {
    val clean = trimEnd('=')
    val out = ArrayList<Byte>((clean.length * 3) / 4 + 3)
    var buffer = 0
    var bits = 0
    for (c in clean) {
        val v = BASE64_ALPHABET.indexOf(c)
        if (v < 0) continue
        buffer = (buffer shl 6) or v
        bits += 6
        if (bits >= 8) {
            bits -= 8
            out.add(((buffer shr bits) and 0xFF).toByte())
        }
    }
    return out.toByteArray()
}
