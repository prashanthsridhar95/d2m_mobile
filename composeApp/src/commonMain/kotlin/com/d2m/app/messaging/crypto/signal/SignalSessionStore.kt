package com.d2m.app.messaging.crypto.signal

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromString
import kotlinx.serialization.json.encodeToString

/**
 * Local storage for identity/prekeys/sessions -- the Kotlin equivalent of
 * `libsignal-protocol-typescript`'s `signalStore.ts` (which persists to
 * `localStorage`, namespaced `d2m_signal:${username}`). Every field here is
 * stored as base64 (rather than modeling a separate "runtime" type with
 * real ByteArrays and a to/from-stored mapping layer) -- keeps this file
 * roughly half the size for a chat app's message-rate crypto workload,
 * where the extra base64 encode/decode per operation is immaterial.
 *
 * Deliberate simplification vs. the reference implementation, stated
 * plainly: this app keeps exactly ONE active session per peer (no
 * multi-generation `SessionRecord` with up to 40 archived past sessions --
 * see SignalProtocol.kt's doc comment for why establishSession always
 * resets-and-rebuilds anyway, same as the web client's trust-on-first-use
 * behavior) and caps retired-but-still-decryptable receiving chains at
 * `MAX_RETIRED_CHAINS` rather than the reference's `oldRatchetList`
 * bookkeeping (which, per direct source inspection, has a latent bug that
 * makes its own 10-entry cap a no-op in practice -- this cap is a real one).
 *
 * Persisted via `multiplatform-settings` (already a project dependency),
 * namespaced `signal:<username>:...` so this doesn't collide with
 * `IdentityStore`'s own keys in the same underlying SharedPreferences file
 * (see `createSettings()`'s doc comment -- one shared file today).
 */
private const val MAX_RETIRED_CHAINS = 5

@Serializable
internal data class ChainRecord(
    var counter: Int,
    var key: String?, // base64 chain key; null once retired (no further forward derivation, but messageKeys may still hold undelivered skipped keys)
    val type: String, // "SENDING" | "RECEIVING"
    val messageKeys: MutableMap<String, String> = mutableMapOf(), // counter.toString() -> base64 message-key seed
    var retiredAtMillis: Long? = null,
)

@Serializable
internal data class RatchetRecord(
    var rootKey: String,
    var ourPrivateKey: String,
    var ourPublicKey: String,
    var lastRemotePublicKey: String,
    var previousCounter: Int,
)

@Serializable
internal data class PendingPreKeyRecord(val preKeyId: Int?, val signedPreKeyId: Int, val baseKey: String)

@Serializable
internal data class SessionRecord(
    val remoteIdentityKey: String,
    val remoteRegistrationId: Int,
    val ratchet: RatchetRecord,
    val chains: MutableMap<String, ChainRecord> = mutableMapOf(), // key = base64(32-byte raw ephemeral pubkey)
    var pendingPreKey: PendingPreKeyRecord? = null,
)

@Serializable
internal data class IdentityRecord(val registrationId: Int, val privateKey: String, val publicKey: String)

@Serializable
internal data class SignedPreKeyRecord(val keyId: Int, val privateKey: String, val publicKey: String, val signature: String)

@Serializable
internal data class OneTimePreKeyRecord(val keyId: Int, val privateKey: String, val publicKey: String)

internal class SignalSessionStore(private val settings: Settings, private val username: String) {
    private val json = Json { ignoreUnknownKeys = true }
    private fun key(suffix: String) = "signal:$username:$suffix"

    fun getIdentity(): IdentityRecord? =
        settings.getStringOrNull(key("identity"))?.let { runCatching { json.decodeFromString<IdentityRecord>(it) }.getOrNull() }

    fun saveIdentity(record: IdentityRecord) {
        settings.putString(key("identity"), json.encodeToString(record))
    }

    fun getSignedPreKeys(): List<SignedPreKeyRecord> =
        settings.getStringOrNull(key("signedPreKeys"))?.let { runCatching { json.decodeFromString<List<SignedPreKeyRecord>>(it) }.getOrNull() } ?: emptyList()

    fun addSignedPreKey(record: SignedPreKeyRecord) {
        // Keep the newest 2 (current + previous) -- enough to cover a bundle
        // fetched just before a rotation, same rationale web keeps every
        // signed prekey it ever issued (it never prunes), but bounded here
        // since nothing needs more than the last couple in practice.
        val updated = (getSignedPreKeys() + record).takeLast(2)
        settings.putString(key("signedPreKeys"), json.encodeToString(updated))
    }

    fun findSignedPreKey(keyId: Int): SignedPreKeyRecord? = getSignedPreKeys().firstOrNull { it.keyId == keyId }

    fun getOneTimePreKeys(): MutableMap<Int, OneTimePreKeyRecord> {
        val stored = settings.getStringOrNull(key("oneTimePreKeys")) ?: return mutableMapOf()
        val list = runCatching { json.decodeFromString<List<OneTimePreKeyRecord>>(stored) }.getOrNull() ?: return mutableMapOf()
        return list.associateBy { it.keyId }.toMutableMap()
    }

    fun addOneTimePreKeys(records: List<OneTimePreKeyRecord>) {
        val all = getOneTimePreKeys()
        for (r in records) all[r.keyId] = r
        settings.putString(key("oneTimePreKeys"), json.encodeToString(all.values.toList()))
    }

    /** Single-use -- removes and returns the prekey so it's never reused (X3DH forward secrecy relies on this). */
    fun consumeOneTimePreKey(keyId: Int): OneTimePreKeyRecord? {
        val all = getOneTimePreKeys()
        val found = all.remove(keyId) ?: return null
        settings.putString(key("oneTimePreKeys"), json.encodeToString(all.values.toList()))
        return found
    }

    fun oneTimePreKeyCountLocal(): Int = getOneTimePreKeys().size

    fun getSession(peerUsername: String): SessionRecord? =
        settings.getStringOrNull(key("session:$peerUsername"))?.let { runCatching { json.decodeFromString<SessionRecord>(it) }.getOrNull() }

    fun saveSession(peerUsername: String, session: SessionRecord) {
        // Evict oldest retired RECEIVING chains beyond the cap -- see class doc comment.
        val retired = session.chains.entries.filter { it.value.type == "RECEIVING" && it.value.key == null }
        if (retired.size > MAX_RETIRED_CHAINS) {
            val toRemove = retired.sortedBy { it.value.retiredAtMillis ?: 0L }.take(retired.size - MAX_RETIRED_CHAINS)
            for (entry in toRemove) session.chains.remove(entry.key)
        }
        settings.putString(key("session:$peerUsername"), json.encodeToString(session))
    }

    fun clearSession(peerUsername: String) {
        settings.remove(key("session:$peerUsername"))
    }
}
