package com.d2m.app.messaging.crypto.signal

import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals

/** In-memory Settings (the test artifact with MapSettings is not a dependency of this module). */
private class MemorySettings : Settings {
    private val map = mutableMapOf<String, Any>()
    override val keys: Set<String> get() = map.keys
    override val size: Int get() = map.size
    override fun clear() = map.clear()
    override fun remove(key: String) { map.remove(key) }
    override fun hasKey(key: String) = map.containsKey(key)
    override fun putInt(key: String, value: Int) { map[key] = value }
    override fun getInt(key: String, defaultValue: Int) = map[key] as? Int ?: defaultValue
    override fun getIntOrNull(key: String) = map[key] as? Int
    override fun putLong(key: String, value: Long) { map[key] = value }
    override fun getLong(key: String, defaultValue: Long) = map[key] as? Long ?: defaultValue
    override fun getLongOrNull(key: String) = map[key] as? Long
    override fun putString(key: String, value: String) { map[key] = value }
    override fun getString(key: String, defaultValue: String) = map[key] as? String ?: defaultValue
    override fun getStringOrNull(key: String) = map[key] as? String
    override fun putFloat(key: String, value: Float) { map[key] = value }
    override fun getFloat(key: String, defaultValue: Float) = map[key] as? Float ?: defaultValue
    override fun getFloatOrNull(key: String) = map[key] as? Float
    override fun putDouble(key: String, value: Double) { map[key] = value }
    override fun getDouble(key: String, defaultValue: Double) = map[key] as? Double ?: defaultValue
    override fun getDoubleOrNull(key: String) = map[key] as? Double
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean) = map[key] as? Boolean ?: defaultValue
    override fun getBooleanOrNull(key: String) = map[key] as? Boolean
}

/**
 * Regression: a sender keeps wrapping its messages in PreKey messages until it hears back, and every one repeats the same
 * base key. The responder must build the session from the FIRST one only - rebuilding consumed the one-time prekey twice
 * and threw the ratchet state away, so everything after the first message failed with "MAC verification failed".
 */
class RepeatedPreKeyMessageTest {
    @Test
    fun consecutiveMessagesBeforeAnyReplyAllDecrypt() {
        val primitives = createCryptoPrimitives()
        val alice = SignalProtocol.generateIdentity(primitives)
        val bob = SignalProtocol.generateIdentity(primitives)
        val bobSigned = SignalProtocol.generateSignedPreKey(primitives, bob, 1)
        val bobOneTime = SignalProtocol.generateOneTimePreKeys(primitives, 1, 5)
        val bobStore = SignalSessionStore(MemorySettings(), "bob").also {
            it.saveIdentity(bob)
            it.addSignedPreKey(bobSigned)
            it.addOneTimePreKeys(bobOneTime)
        }
        val upload = SignalProtocol.buildKeyBundleUpload(bob, bobSigned, bobOneTime)
        val bundle = PreKeyBundleResponse(upload.registrationId, upload.identityKey, upload.signedPreKey, upload.preKeys.first())

        val aliceSession = SignalProtocol.establishSessionAsInitiator(primitives, alice, bundle)
        val sent = (1..3).map { i ->
            SignalProtocol.encrypt(primitives, aliceSession, alice.publicKey.fromB64(), alice.registrationId, "message $i".encodeToByteArray())
        }
        assertEquals(listOf(3, 3, 3), sent.map { it.first }, "every message is still a PreKey message until Bob replies")

        var bobSession: SessionRecord? = null
        sent.forEachIndexed { index, (_, wire) ->
            val result = SignalProtocol.handleIncomingPreKeyMessage(primitives, bob, bobStore, wire, bobSession)
            bobSession = result.session
            assertEquals("message ${index + 1}", result.plaintext.decodeToString())
        }
    }
}
