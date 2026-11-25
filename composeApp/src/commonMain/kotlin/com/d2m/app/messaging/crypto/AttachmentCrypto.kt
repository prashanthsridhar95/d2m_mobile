package com.d2m.app.messaging.crypto

import com.d2m.app.messaging.crypto.archive.ArchivePrimitives
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class EncryptedAttachment(
    val ciphertext: ByteArray,
    val keyBase64: String,
    val ivBase64: String,
)

/** AES-256-GCM attachment encryption compatible with the web client's media.ts framing. */
class AttachmentCrypto(private val primitives: ArchivePrimitives) {
    @OptIn(ExperimentalEncodingApi::class)
    fun encrypt(plaintext: ByteArray): EncryptedAttachment {
        val key = primitives.randomBytes(32)
        val iv = primitives.randomBytes(12)
        return EncryptedAttachment(
            ciphertext = primitives.aesGcmEncrypt(key, iv, plaintext),
            keyBase64 = Base64.Default.encode(key),
            ivBase64 = Base64.Default.encode(iv),
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decrypt(ciphertext: ByteArray, keyBase64: String, ivBase64: String): ByteArray {
        val key = Base64.Default.decode(keyBase64)
        val iv = Base64.Default.decode(ivBase64)
        require(key.size == 32) { "attachment key must be 32 bytes" }
        require(iv.size == 12) { "attachment IV must be 12 bytes" }
        return primitives.aesGcmDecrypt(key, iv, ciphertext)
    }
}
