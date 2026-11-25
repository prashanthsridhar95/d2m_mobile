package com.d2m.app.messaging.crypto

import com.d2m.app.messaging.crypto.archive.createArchivePrimitives
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class AttachmentCryptoTest {
    private val crypto = AttachmentCrypto(createArchivePrimitives())

    @Test
    fun roundTripProducesWebCompatibleKeyAndIvLengths() {
        val plaintext = ByteArray(4097) { (it % 251).toByte() }
        val encrypted = crypto.encrypt(plaintext)

        assertEquals(plaintext.size + 16, encrypted.ciphertext.size)
        assertContentEquals(plaintext, crypto.decrypt(encrypted.ciphertext, encrypted.keyBase64, encrypted.ivBase64))
    }

    @Test
    fun tamperingAndInvalidMetadataFailClosed() {
        val encrypted = crypto.encrypt("private attachment".encodeToByteArray())
        val tampered = encrypted.ciphertext.copyOf().also { it[it.lastIndex] = (it.last() + 1).toByte() }

        assertFails { crypto.decrypt(tampered, encrypted.keyBase64, encrypted.ivBase64) }
        assertFails { crypto.decrypt(encrypted.ciphertext, "AA==", encrypted.ivBase64) }
        assertFails { crypto.decrypt(encrypted.ciphertext, encrypted.keyBase64, "AA==") }
    }
}
