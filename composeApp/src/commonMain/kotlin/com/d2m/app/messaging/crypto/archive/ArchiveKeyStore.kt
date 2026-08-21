package com.d2m.app.messaging.crypto.archive

import com.d2m.app.messaging.protocol.JsonWebKey
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Local persistence for the Archive Keypair -- direct port of d2m_web's
 * `archiveStore.ts`. Deliberately a SEPARATE Settings namespace
 * (`d2m_archive:<username>`) from `SignalSessionStore`'s `signal:<username>:...`
 * keys even though both live in the same underlying SharedPreferences file
 * (see `createSettings()`'s doc comment) -- keeping them apart means
 * wiping one (e.g. a future Signal identity reset flow) never accidentally
 * takes the archive keypair down with it, and vice versa, matching
 * archiveStore.ts's own stated reasoning for why it doesn't share
 * `signalStore.ts`'s namespace.
 *
 * This is the SAME-device fast path only: once a device has set up or
 * restored the archive keypair once, it keeps its own unwrapped copy here
 * so it never has to re-enter the PIN again locally. A genuinely NEW
 * device has nothing under this key and goes through
 * ArchiveCrypto.unwrapPrivateKeyWithPin instead, using the wrapped copy
 * fetched from the server (see ArchiveManager.checkAfterConnect).
 *
 * Stored as JWK (matching archiveStore.ts persisting exported JWK rather
 * than a live key object -- same "can't structured-clone a live key into
 * string storage" reasoning, here just "Settings only stores strings").
 */
@Serializable
private data class StoredArchiveKeypair(val publicKeyJwk: JsonWebKey, val privateKeyJwk: JsonWebKey)

class ArchiveKeyStore(private val settings: Settings) {
    private val json = Json { ignoreUnknownKeys = true }
    private fun key(username: String) = "d2m_archive:$username"

    fun load(username: String): Pair<JsonWebKey, JsonWebKey>? {
        val raw = settings.getStringOrNull(key(username)) ?: return null
        val stored = runCatching { json.decodeFromString<StoredArchiveKeypair>(raw) }.getOrNull() ?: return null
        return stored.publicKeyJwk to stored.privateKeyJwk
    }

    fun save(username: String, publicKeyJwk: JsonWebKey, privateKeyJwk: JsonWebKey) {
        settings.putString(key(username), json.encodeToString(StoredArchiveKeypair(publicKeyJwk, privateKeyJwk)))
    }

    fun has(username: String): Boolean = load(username) != null
}
