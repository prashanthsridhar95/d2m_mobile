package com.d2m.app.messaging.crypto.signal

import com.d2m.app.data.network.ApiClient
import com.d2m.app.messaging.MessagingConfig
import com.d2m.app.messaging.crypto.CryptoProvider
import com.russhwolf.settings.Settings
import com.d2m.app.messaging.DEFAULT_DEVICE
import com.d2m.app.messaging.addrKey
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.time.Clock

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
    private var myDeviceId: Int? = null
    override val deviceId: Int? get() = myDeviceId

    // One lock per Signal ADDRESS ("<user>.<device>"): each device pair has its own session record, and encrypt/decrypt/
    // reset against the same record are a read-modify-write that must not interleave (a burst of trickle-ICE signals, a
    // fan-out racing an incoming message). Different devices of the same peer proceed independently.
    private val addrLocks = mutableMapOf<String, Mutex>()
    // Guards getOrPut on addrLocks itself -- `kotlin.synchronized` is a JVM-only
    // stdlib intrinsic (maps to monitorenter/monitorexit bytecode) with no
    // Kotlin/Native equivalent ("Unresolved reference 'synchronized'", confirmed
    // directly against the iOS simulator target). Every real caller of lockFor()
    // is already a suspend fun (encrypt/decrypt/resetSession below), so a second,
    // dedicated coroutine Mutex protecting just the map access is a natural fit --
    // no new dependency needed, unlike kotlinx-atomicfu's SynchronizedObject.
    private val addrLocksGuard = Mutex()

    // ensureIdentity may be reached from start() and from a push-woken service at the same time; two concurrent runs
    // must not both register a device.
    private val ensureMutex = Mutex()
    private var ensured = false

    // Failure backoff for session establishment (address -> retry-not-before epoch ms): a failed bundle fetch (denied,
    // offline, or the server's own 429 key-request limit) used to be retried by every following signal/message, which
    // burned the per-pair bundle quota in seconds and turned one hiccup into a minute-long lockout.
    private val establishFailedUntil = mutableMapOf<String, Pair<Long, String>>()

    private suspend fun lockFor(peer: String, device: Int): Mutex = addrLocksGuard.withLock { addrLocks.getOrPut(addrKey(peer, device)) { Mutex() } }
    private fun store(): SignalSessionStore = SignalSessionStore(settings, username ?: error("SignalCryptoProvider.ensureIdentity was never called"))
    private fun requireIdentity(): IdentityRecord = identity ?: error("SignalCryptoProvider.ensureIdentity was never called")
    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    /** The identity key as published to / reported by the server (33-byte 0x05-prefixed, base64). */
    private fun publishedIdentityKey(id: IdentityRecord): String = prefix05(id.publicKey.fromB64()).toB64()

    override suspend fun ensureIdentity(username: String, force: Boolean): Unit = ensureMutex.withLock {
        if (!force && ensured && this.username == username && identity != null && myDeviceId != null) return@withLock
        this.username = username
        val store = store()
        val existing = store.getIdentity()
        val known = store.getDeviceId()

        if (existing == null) {
            // Fresh install: allocate a device id, then generate and publish its keys. The id is saved BEFORE the upload so
            // a failed upload retries against the same device instead of leaking an id per attempt.
            val id = registerDevice()
            store.saveDeviceId(id)
            myDeviceId = id
            val newIdentity = SignalProtocol.generateIdentity(primitives)
            store.saveIdentity(newIdentity)
            val signedPreKey = SignalProtocol.generateSignedPreKey(primitives, newIdentity, keyId = 1)
            store.addSignedPreKey(signedPreKey)
            val preKeys = SignalProtocol.generateOneTimePreKeys(primitives, startId = 1, count = SignalProtocol.NUM_ONE_TIME_PREKEYS)
            store.addOneTimePreKeys(preKeys)
            identity = newIdentity
            uploadKeys(username, SignalProtocol.buildKeyBundleUpload(newIdentity, signedPreKey, preKeys), id)
            ensured = true
            return@withLock
        }

        identity = existing
        if (known == null) {
            // An identity from before multi-device. It is device 1 if the server still holds exactly this identity there;
            // otherwise (another install published over it, or the server forgot it) it becomes a NEW device that keeps
            // its identity, so peers' existing sessions with it stay valid.
            val info = runCatching { fetchKeyInfo(username, DEFAULT_DEVICE) }.getOrNull()
            if (info?.identityKey != null && info.identityKey == publishedIdentityKey(existing)) {
                store.saveDeviceId(DEFAULT_DEVICE)
                myDeviceId = DEFAULT_DEVICE
                if (info.oneTimePreKeys <= 0) topUpPreKeys(username, DEFAULT_DEVICE)
            } else {
                val id = registerDevice()
                store.saveDeviceId(id)
                myDeviceId = id
                topUpPreKeys(username, id)
            }
            ensured = true
            return@withLock
        }

        myDeviceId = known
        // Registered device: ask the server what it holds for it. NOTE there is deliberately no "the server's identity
        // differs from ours, so overwrite it" repair any more -- with several devices per account that is last-writer-wins
        // and would silently knock another device's keys out.
        val info = try {
            fetchKeyInfo(username, known)
        } catch (e: MessagingHttpException) {
            if (e.status == 404) KeyCountResponse(0, null) else null
        } catch (_: Throwable) {
            null
        }
        if (info == null) {
            // Couldn't ask (offline): carry on with what we have; the socket retries.
            ensured = true
            return@withLock
        }
        if (info.identityKey == null) {
            // The server doesn't know this device's keys: removed, evicted, or its data was reset. Publish to the same id
            // first (it may be registered but never got its keys); only if the id is gone register a new one.
            try {
                topUpPreKeys(username, known)
            } catch (e: MessagingHttpException) {
                if (!e.isDeviceGone) throw e
                val id = registerDevice()
                store.saveDeviceId(id)
                myDeviceId = id
                topUpPreKeys(username, id)
            }
        } else if (info.oneTimePreKeys <= 0) {
            topUpPreKeys(username, known)
        }
        ensured = true
    }

    /** Rotates the signed prekey, issues a fresh batch of one-time prekeys and publishes them for [device], WITHOUT changing the identity. */
    private suspend fun topUpPreKeys(username: String, device: Int) {
        val store = store()
        val id = requireIdentity()
        val nextSignedId = (store.getSignedPreKeys().maxOfOrNull { it.keyId } ?: 0) + 1
        val signedPreKey = SignalProtocol.generateSignedPreKey(primitives, id, keyId = nextSignedId)
        store.addSignedPreKey(signedPreKey)
        val preKeys = SignalProtocol.generateOneTimePreKeys(primitives, startId = nextSignedId * 10_000, count = SignalProtocol.NUM_ONE_TIME_PREKEYS)
        store.addOneTimePreKeys(preKeys)
        uploadKeys(username, SignalProtocol.buildKeyBundleUpload(id, signedPreKey, preKeys), device)
    }

    override suspend fun listDevices(username: String): List<Int> {
        val response = apiClient.client.get("${MessagingConfig.httpBaseUrl}/keys/$username/devices")
        return wireJson.decodeFromString(DeviceListResponse.serializer(), response.checkedBody()).devices.map { it.deviceId }.sorted()
    }

    override suspend fun encrypt(toUsername: String, toDevice: Int, plaintext: String): Pair<String, String> = lockFor(toUsername, toDevice).withLock {
        val myIdentity = requireIdentity()
        val store = store()
        var session = store.getSession(toUsername, toDevice)
        if (session == null) {
            val key = addrKey(toUsername, toDevice)
            establishFailedUntil[key]?.let { (until, message) -> if (nowMs() < until) error("cannot start session with $key: $message") }
            val bundle = try {
                fetchBundle(toUsername, toDevice)
            } catch (e: Throwable) {
                val status = (e as? MessagingHttpException)?.status
                establishFailedUntil[key] = (nowMs() + if (status == 429) 65_000L else 15_000L) to (e.message ?: e::class.simpleName.orEmpty())
                throw e
            }
            establishFailedUntil.remove(key)
            session = SignalProtocol.establishSessionAsInitiator(primitives, myIdentity, bundle)
        }
        val (type, wireBytes) = SignalProtocol.encrypt(primitives, session, myIdentity.publicKey.fromB64(), myIdentity.registrationId, plaintext.encodeToByteArray())
        store.saveSession(toUsername, toDevice, session)
        (if (type == 3) "prekey" else "signal") to wireBytes.toB64()
    }

    override suspend fun decrypt(fromUsername: String, fromDevice: Int, ciphertextType: String, body: String): String = lockFor(fromUsername, fromDevice).withLock {
        val myIdentity = requireIdentity()
        val store = store()
        val wireBytes = body.fromB64()
        if (ciphertextType == "prekey") {
            val result = SignalProtocol.handleIncomingPreKeyMessage(primitives, myIdentity, store, wireBytes, store.getSession(fromUsername, fromDevice))
            store.saveSession(fromUsername, fromDevice, result.session)
            return@withLock result.plaintext.decodeToString()
        }
        val session = store.getSession(fromUsername, fromDevice) ?: error("no session with ${addrKey(fromUsername, fromDevice)} – cannot decrypt a non-prekey message with no established session")
        val plaintext = SignalProtocol.decryptWhisperMessage(primitives, session, myIdentity.publicKey.fromB64(), wireBytes)
        store.saveSession(fromUsername, fromDevice, session)
        plaintext.decodeToString()
    }

    override suspend fun resetSession(peerUsername: String, peerDevice: Int): Unit = lockFor(peerUsername, peerDevice).withLock {
        store().clearSession(peerUsername, peerDevice)
        establishFailedUntil.remove(addrKey(peerUsername, peerDevice))
    }

    override suspend fun resetAllSessions(peerUsername: String) {
        val store = store()
        // Every device we hold a session with, plus device 1 (a legacy peer we may be about to talk to).
        for (device in (store.sessionDevices(peerUsername) + DEFAULT_DEVICE).toSet()) resetSession(peerUsername, device)
    }

    override suspend fun deregisterDevice(username: String, timeoutMs: Long): Boolean = ensureMutex.withLock {
        val store = SignalSessionStore(settings, username)
        // A legacy identity (device 1, no stored id) or none: nothing of ours to remove.
        val id = store.getDeviceId() ?: return@withLock false
        try {
            withTimeout(timeoutMs) {
                val response = apiClient.client.delete("${MessagingConfig.httpBaseUrl}/devices/$id")
                response.checkedBody()
            }
        } catch (_: Throwable) {
            // Offline / session already expired / slow server: leave the device registered; a later sign-in reuses it, and a
            // device that is never used again is eventually evicted by the per-account cap.
            return@withLock false
        }
        store.clearAll()
        if (this.username == username) {
            identity = null
            myDeviceId = null
            ensured = false
        }
        true
    }

    // ---- Relay server HTTP calls -- mirrors d2m_web's api.ts exactly. ----
    // Deliberately manual JSON encode/decode (not ApiClient.client's automatic ContentNegotiation) -- that
    // client's JSON is configured with snake_case naming for d2m_core_engine's Pydantic backend (see
    // ApiClient.kt's doc comment); the messaging-framework relay server uses plain camelCase, same reason
    // MessagingRepository.kt's fetchIceServers() also bypasses it.

    private suspend fun registerDevice(): Int {
        val response = apiClient.client.post("${MessagingConfig.httpBaseUrl}/devices")
        return wireJson.decodeFromString(RegisterDeviceResponse.serializer(), response.checkedBody()).deviceId
    }

    private suspend fun uploadKeys(username: String, bundle: KeyBundleUpload, device: Int) {
        val response = apiClient.client.post("${MessagingConfig.httpBaseUrl}/keys/$username") {
            contentType(ContentType.Application.Json)
            setBody(wireJson.encodeToString(KeyBundleUpload.serializer(), bundle.copy(deviceId = device)))
        }
        response.checkedBody()
    }

    private suspend fun fetchBundle(username: String, device: Int): PreKeyBundleResponse {
        val response = apiClient.client.get("${MessagingConfig.httpBaseUrl}/keys/$username/bundle") { parameter("device", device) }
        return wireJson.decodeFromString(PreKeyBundleResponse.serializer(), response.checkedBody())
    }

    private suspend fun fetchKeyInfo(username: String, device: Int): KeyCountResponse {
        val response = apiClient.client.get("${MessagingConfig.httpBaseUrl}/keys/$username/count") { parameter("device", device) }
        return wireJson.decodeFromString(KeyCountResponse.serializer(), response.checkedBody())
    }

    private suspend fun HttpResponse.checkedBody(): String {
        val text = bodyAsText()
        if (!status.isSuccess()) throw MessagingHttpException(status.value, text)
        return text
    }
}

/** A non-2xx answer from the messaging relay. [status] is the HTTP status; the message carries the response body. */
class MessagingHttpException(val status: Int, body: String) : Exception("HTTP $status: ${body.take(200)}") {
    /** 409 `{code:'device_gone'}`: the targeted device id is not (or no longer) registered. */
    val isDeviceGone: Boolean get() = status == 409 && message?.contains("device_gone") == true
}
