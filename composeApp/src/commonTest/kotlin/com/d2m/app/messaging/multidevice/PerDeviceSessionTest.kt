package com.d2m.app.messaging.multidevice

import com.d2m.app.messaging.crypto.signal.PreKeyBundleResponse
import com.d2m.app.messaging.crypto.signal.SessionRecord
import com.d2m.app.messaging.crypto.signal.SignalProtocol
import com.d2m.app.messaging.crypto.signal.SignalSessionStore
import com.d2m.app.messaging.crypto.signal.createCryptoPrimitives
import com.d2m.app.messaging.crypto.signal.fromB64
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class MemorySettings : Settings {
    private val map = mutableMapOf<String, Any>()
    override val keys: Set<String> get() = map.keys.toSet()
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

/** Sessions are keyed by (peer, peerDevice): resetting one device of a peer must leave the others intact. */
class PerDeviceSessionTest {
    private val primitives = createCryptoPrimitives()

    private class Device(val store: SignalSessionStore, val bundle: PreKeyBundleResponse)

    /** A peer device with its own identity, published bundle and local store. */
    private fun peerDevice(name: String): Device {
        val identity = SignalProtocol.generateIdentity(primitives)
        val signed = SignalProtocol.generateSignedPreKey(primitives, identity, 1)
        val oneTime = SignalProtocol.generateOneTimePreKeys(primitives, 1, 3)
        val store = SignalSessionStore(MemorySettings(), name).also {
            it.saveIdentity(identity)
            it.addSignedPreKey(signed)
            it.addOneTimePreKeys(oneTime)
        }
        val upload = SignalProtocol.buildKeyBundleUpload(identity, signed, oneTime)
        return Device(store, PreKeyBundleResponse(upload.registrationId, upload.identityKey, upload.signedPreKey, upload.preKeys.first()))
    }

    private fun session(bundle: PreKeyBundleResponse): SessionRecord {
        val alice = SignalProtocol.generateIdentity(primitives)
        return SignalProtocol.establishSessionAsInitiator(primitives, alice, bundle)
    }

    @Test
    fun sessionsAreStoredPerPeerDeviceAndResettingOneLeavesTheOthers() {
        val store = SignalSessionStore(MemorySettings(), "alice")
        val bob1 = peerDevice("bob1")
        val bob3 = peerDevice("bob3")
        val carol1 = peerDevice("carol1")
        store.saveSession("bob", 1, session(bob1.bundle))
        store.saveSession("bob", 3, session(bob3.bundle))
        store.saveSession("carol", 1, session(carol1.bundle))
        assertEquals(listOf(1, 3), store.sessionDevices("bob"))

        store.clearSession("bob", 3)

        assertNull(store.getSession("bob", 3))
        assertNotNull(store.getSession("bob", 1), "the peer's other device keeps its session")
        assertNotNull(store.getSession("carol", 1), "other peers are untouched")
        assertEquals(listOf(1), store.sessionDevices("bob"))
    }

    @Test
    fun aLegacyResetClearsEveryDeviceOfThePeerOnly() {
        val store = SignalSessionStore(MemorySettings(), "alice")
        store.saveSession("bob", 1, session(peerDevice("b1").bundle))
        store.saveSession("bob", 2, session(peerDevice("b2").bundle))
        store.saveSession("carol", 1, session(peerDevice("c1").bundle))
        store.clearAllSessions("bob")
        assertTrue(store.sessionDevices("bob").isEmpty())
        assertNotNull(store.getSession("carol", 1))
    }

    @Test
    fun aSessionSavedBeforeMultiDeviceIsTheDeviceOneSession() {
        val settings = MemorySettings()
        val store = SignalSessionStore(settings, "alice")
        store.saveSession("bob", 1, session(peerDevice("b1").bundle))
        // Move it back to where the pre-multi-device app stored it.
        val modern = "signal:alice:session:bob:1"
        val legacy = "signal:alice:session:bob"
        settings.putString(legacy, settings.getString(modern, ""))
        settings.remove(modern)

        assertNotNull(store.getSession("bob", 1), "read as the peer's device 1")
        assertNull(store.getSession("bob", 2), "never mistaken for another device")
        assertEquals(listOf(1), store.sessionDevices("bob"))

        store.saveSession("bob", 1, store.getSession("bob", 1)!!)
        assertTrue(!settings.hasKey(legacy), "rewritten under the device-qualified key")
        assertTrue(settings.hasKey(modern))
        store.clearSession("bob", 1)
        assertNull(store.getSession("bob", 1))
    }

    @Test
    fun deviceIdSurvivesIdentityRewritesAndIsWipedOnlyByClearAll() {
        val settings = MemorySettings()
        val store = SignalSessionStore(settings, "alice")
        assertNull(store.getDeviceId())
        val identity = SignalProtocol.generateIdentity(primitives)
        store.saveIdentity(identity)
        store.saveDeviceId(4)
        store.saveIdentity(identity)
        assertEquals(4, store.getDeviceId())
        store.saveSession("bob", 1, session(peerDevice("b1").bundle))
        // Another account on the same install is not touched by logging this one out.
        val other = SignalSessionStore(settings, "alicia").also { it.saveDeviceId(9) }
        store.clearAll()
        assertNull(store.getDeviceId())
        assertNull(store.getIdentity())
        assertNull(store.getSession("bob", 1))
        assertEquals(9, other.getDeviceId())
    }

    @Test
    fun eachPeerDeviceDecryptsItsOwnCopyAndSurvivesTheOthersReset() {
        // One logical message, encrypted once per target device: each device can read its copy, and a reset of one
        // device's session does not disturb the other's ratchet.
        val alice = SignalProtocol.generateIdentity(primitives)
        val aliceStore = SignalSessionStore(MemorySettings(), "alice")
        val bob1 = peerDevice("bob1")
        val bob2 = peerDevice("bob2")
        aliceStore.saveSession("bob", 1, SignalProtocol.establishSessionAsInitiator(primitives, alice, bob1.bundle))
        aliceStore.saveSession("bob", 2, SignalProtocol.establishSessionAsInitiator(primitives, alice, bob2.bundle))
        val aliceKey = alice.publicKey.fromB64()

        // Encrypt like SignalCryptoProvider.encrypt: load the device's session, advance it, save it back.
        fun encryptFor(device: Int, text: String): ByteArray {
            val session = aliceStore.getSession("bob", device)!!
            val wire = SignalProtocol.encrypt(primitives, session, aliceKey, alice.registrationId, text.encodeToByteArray()).second
            aliceStore.saveSession("bob", device, session)
            return wire
        }
        val copy1 = encryptFor(1, "hello")
        val copy2 = encryptFor(2, "hello")
        val identity1 = bob1.store.getIdentity()!!
        assertEquals("hello", SignalProtocol.handleIncomingPreKeyMessage(primitives, identity1, bob1.store, copy1, null).plaintext.decodeToString())

        // Forgetting device 1's session leaves device 2's ratchet exactly where it was.
        aliceStore.clearSession("bob", 1)
        val again = encryptFor(2, "second")
        val identity2 = bob2.store.getIdentity()!!
        var bobSession2: SessionRecord? = null
        val first = SignalProtocol.handleIncomingPreKeyMessage(primitives, identity2, bob2.store, copy2, bobSession2)
        bobSession2 = first.session
        val second = SignalProtocol.handleIncomingPreKeyMessage(primitives, identity2, bob2.store, again, bobSession2)
        assertEquals("hello", first.plaintext.decodeToString())
        assertEquals("second", second.plaintext.decodeToString())
    }
}
