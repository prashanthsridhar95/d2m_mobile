package com.d2m.app.messaging.crypto.signal

import com.d2m.app.data.network.ApiClient
import com.d2m.app.messaging.MessagingConfig
import com.d2m.app.messaging.crypto.CryptoProvider
import com.russhwolf.settings.Settings
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Real `CryptoProvider` -- see SignalProtocol.kt's top doc comment for the
 * full picture (research trail, wire-format precision, and the "needs real
 * device verification" caveat that applies to this whole `signal/`
 * package). This file is the thin orchestration layer: HTTP calls against
 * the relay server's existing `/keys/:username[...]` endpoints (confirmed
 * via research to already exist and already be what d2m_web's own api.ts
 * calls -- no server-side changes needed), per-peer serialization (mirrors
 * crypto.ts's own per-peer promise queue -- see that file's extensive doc
 * comment on why concurrent encrypt/decrypt/establishSession for the same
 * peer must be serialized, e.g. a burst of trickle-ICE call-signaling
 * messages), and translating between `CryptoProvider`'s (username, text)
 * interface and `SignalProtocol`'s session-record-based one.
 *
 * Bound as the real `CryptoProvider` implementation on Android only -- see
 * `createCryptoProvider.kt`. iOS keeps `StubUnencryptedCryptoProvider` for
 * now (no `CryptoPrimitives` actual exists for iOS -- see that interface's
 * doc comment).
 */
class SignalCryptoProvider(
    private val apiClient: ApiClient,
    private val primitives: CryptoPrimitives,
    private val settings: Settings,
) : CryptoProvider {
    override val isProductionGrade: Boolean = true

    private val wireJson = Json { ignoreUnknownKeys = true }
    private var username: String? = null
    private var identity: IdentityRecord? = null
    private val peerLocks = mutableMapOf<String, Mutex>()
    // Guards getOrPut on peerLocks itself -- `kotlin.synchronized` is a JVM-only
    // stdlib intrinsic (maps to monitorenter/monitorexit bytecode) with no
    // Kotlin/Native equivalent ("Unresolved reference 'synchronized'", confirmed
    // directly against the iOS simulator target). Every real caller of lockFor()
    // is already a suspend fun (encrypt/decrypt/resetSession below), so a second,
    // dedicated coroutine Mutex protecting just the map access is a natural fit --
    // no new dependency needed, unlike kotlinx-atomicfu's SynchronizedObject.
    private val peerLocksGuard = Mutex()

    private suspend fun lockFor(peer: String): Mutex = peerLocksGuard.withLock { peerLocks.getOrPut(peer) { Mutex() } }
    private fun store(): SignalSessionStore = SignalSessionStore(settings, username ?: error("SignalCryptoProvider.ensureIdentity was never called"))
    private fun requireIdentity(): IdentityRecord = identity ?: error("SignalCryptoProvider.ensureIdentity was never called")

    override suspend fun ensureIdentity(username: String) {
        this.username = username
        val store = store()
        val existing = store.getIdentity()
        if (existing == null) {
            val newIdentity = SignalProtocol.generateIdentity(primitives)
            store.saveIdentity(newIdentity)
            val signedPreKey = SignalProtocol.generateSignedPreKey(primitives, newIdentity, keyId = 1)
            store.addSignedPreKey(signedPreKey)
            val preKeys = SignalProtocol.generateOneTimePreKeys(primitives, startId = 1, count = SignalProtocol.NUM_ONE_TIME_PREKEYS)
            store.addOneTimePreKeys(preKeys)
            runCatching { uploadKeys(username, SignalProtocol.buildKeyBundleUpload(newIdentity, signedPreKey, preKeys)) }
            identity = newIdentity
            return
        }
        identity = existing
        // Mirrors crypto.ts's topUpPreKeys flow: if the server lost our bundle (e.g. its DB was reset) but we still hold our identity locally, re-publish a fresh signed prekey + one-time prekey batch so existing peer sessions keep working.
        val serverCount = runCatching { fetchKeyCount(username) }.getOrDefault(0)
        if (serverCount <= 0) {
            val nextSignedId = (store.getSignedPreKeys().maxOfOrNull { it.keyId } ?: 0) + 1
            val signedPreKey = SignalProtocol.generateSignedPreKey(primitives, existing, keyId = nextSignedId)
            store.addSignedPreKey(signedPreKey)
            val preKeys = SignalProtocol.generateOneTimePreKeys(primitives, startId = nextSignedId * 10_000, count = SignalProtocol.NUM_ONE_TIME_PREKEYS)
            store.addOneTimePreKeys(preKeys)
            runCatching { uploadKeys(username, SignalProtocol.buildKeyBundleUpload(existing, signedPreKey, preKeys)) }
        }
    }

    override suspend fun encrypt(toUsername: String, plaintext: String): Pair<String, String> = lockFor(toUsername).withLock {
        val myIdentity = requireIdentity()
        val store = store()
        var session = store.getSession(toUsername)
        if (session == null) {
            val bundle = fetchBundle(toUsername)
            session = SignalProtocol.establishSessionAsInitiator(primitives, myIdentity, bundle)
        }
        val (type, wireBytes) = SignalProtocol.encrypt(primitives, session, myIdentity.publicKey.fromB64(), myIdentity.registrationId, plaintext.encodeToByteArray())
        store.saveSession(toUsername, session)
        (if (type == 3) "prekey" else "signal") to wireBytes.toB64()
    }

    override suspend fun decrypt(fromUsername: String, ciphertextType: String, body: String): String = lockFor(fromUsername).withLock {
        val myIdentity = requireIdentity()
        val store = store()
        val wireBytes = body.fromB64()
        if (ciphertextType == "prekey") {
            val result = SignalProtocol.handleIncomingPreKeyMessage(primitives, myIdentity, store, wireBytes)
            store.saveSession(fromUsername, result.session)
            return@withLock result.plaintext.decodeToString()
        }
        val session = store.getSession(fromUsername) ?: error("no session with $fromUsername -- cannot decrypt a non-prekey message with no established session")
        val plaintext = SignalProtocol.decryptWhisperMessage(primitives, session, myIdentity.publicKey.fromB64(), wireBytes)
        store.saveSession(fromUsername, session)
        plaintext.decodeToString()
    }

    override suspend fun resetSession(peerUsername: String) = lockFor(peerUsername).withLock {
        store().clearSession(peerUsername)
    }

    // ---- Relay server HTTP calls -- mirrors d2m_web's api.ts (uploadKeys/fetchBundle/keyCount) exactly. ----
    // Deliberately manual JSON encode/decode (not ApiClient.client's automatic ContentNegotiation) -- that
    // client's JSON is configured with snake_case naming for d2m_core_engine's Pydantic backend (see
    // ApiClient.kt's doc comment); the messaging-framework relay server uses plain camelCase, same reason
    // MessagingRepository.kt's fetchIceServers() also bypasses it.

    private suspend fun uploadKeys(username: String, bundle: KeyBundleUpload) {
        apiClient.client.post("${MessagingConfig.httpBaseUrl}/keys/$username") {
            contentType(ContentType.Application.Json)
            setBody(wireJson.encodeToString(KeyBundleUpload.serializer(), bundle))
        }
    }

    private suspend fun fetchBundle(username: String): PreKeyBundleResponse {
        val response = apiClient.client.get("${MessagingConfig.httpBaseUrl}/keys/$username/bundle")
        return wireJson.decodeFromString(PreKeyBundleResponse.serializer(), response.bodyAsText())
    }

    private suspend fun fetchKeyCount(username: String): Int {
        val response = apiClient.client.get("${MessagingConfig.httpBaseUrl}/keys/$username/count")
        return wireJson.decodeFromString(KeyCountResponse.serializer(), response.bodyAsText()).oneTimePreKeys
    }
}
