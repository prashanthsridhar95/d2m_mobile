package com.d2m.app.messaging.crypto.archive

import com.d2m.app.messaging.MessagingConfig
import com.d2m.app.messaging.protocol.ArchiveField
import com.d2m.app.messaging.protocol.ArchiveKeyBundle
import com.d2m.app.messaging.protocol.ArchivePublicKeyResponse
import com.d2m.app.messaging.protocol.ChatPayload
import com.d2m.app.messaging.protocol.JsonWebKey
import com.d2m.app.messaging.protocol.MessageEnvelope
import com.d2m.app.messaging.protocol.MessagesHistoryResponse
import com.d2m.app.messaging.protocol.messagingProtocolJson
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What ChatPane/MatchesScreen's PIN dialog renders off of -- mirrors useMessaging.js's `archivePrompt` shape (`{mode:'setup'}` / `{mode:'restore', bundle}`). */
sealed class ArchivePrompt {
    data object Setup : ArchivePrompt()
    data class Restore(val bundle: ArchiveKeyBundle) : ArchivePrompt()
}

/**
 * Orchestrates the Archive Keypair cross-device backup system -- the state
 * machine + HTTP-boundary half of ArchiveCrypto.kt's pure crypto, direct
 * port of useMessaging.js's archive-related state/callbacks
 * (`archiveKeypairRef`, `peerArchiveKeysRef`, `archivePrompt`/`archiveBusy`/
 * `archiveError`, `buildArchiveField`, `restoreFromArchive`,
 * `submitArchiveSetupPin`/`submitArchiveRestorePin`, `dismissArchivePrompt`,
 * and the boot-time local-cache/server-bundle check).
 *
 * Owned by MessagingRepository (constructed once via DI, same lifetime),
 * which wires [onRestoredEntry] to replay decrypted history into its own
 * `_messagesByPeer` and calls [checkAfterConnect] right after a successful
 * `start()` -- fire-and-forget, exactly like useMessaging.js's `void (async
 * () => {...})()` right after `setReady(true)`, since live messaging
 * shouldn't wait on this.
 */
class ArchiveManager(
    private val primitives: ArchivePrimitives,
    private val keyStore: ArchiveKeyStore,
) {
    private val wireJson = messagingProtocolJson

    private var httpClient: HttpClient? = null
    private var username: String? = null

    /** This device's own Archive Keypair, once loaded/restored/generated -- null until then, exactly like archiveKeypairRef.current on web. */
    private var myKeypair: ArchiveCrypto.GeneratedKeypair? = null

    /** Peer archive public keys, cached per-session -- these are stable, long-lived account keys (unlike Signal prekeys, nothing is "consumed" by fetching one), so there's no reason to hit the server again for a peer already looked up. A cached `null` means "looked up, this peer has no archive key" -- distinct from "never looked up", same as peerArchiveKeysRef's `undefined` vs. `null` distinction on web. */
    private val peerArchiveKeysCache = mutableMapOf<String, JsonWebKey?>()
    private val peerArchiveKeysLock = Mutex()

    private val _archivePrompt = MutableStateFlow<ArchivePrompt?>(null)
    val archivePrompt: StateFlow<ArchivePrompt?> = _archivePrompt.asStateFlow()
    private val _archiveBusy = MutableStateFlow(false)
    val archiveBusy: StateFlow<Boolean> = _archiveBusy.asStateFlow()
    private val _archiveError = MutableStateFlow("")
    val archiveError: StateFlow<String> = _archiveError.asStateFlow()

    /** Set by MessagingRepository right after construction -- lets restoreFromArchive hand decrypted history back without ArchiveManager needing to know ChatMessage's shape (keeps this file crypto/state-only, same split as ArchiveCrypto.kt not knowing about ChatPayload). `isMine` mirrors useMessaging.js's `msg.from === myUsername` check. */
    var onRestoredEntry: ((peerUsername: String, payload: ChatPayload, msg: MessageEnvelope, isMine: Boolean) -> Unit)? = null

    /** Called once from MessagingRepository.start(), right before the fire-and-forget checkAfterConnect(). */
    fun attach(httpClient: HttpClient, username: String) {
        this.httpClient = httpClient
        this.username = username
    }

    /**
     * The boot-time check, mirrors useMessaging.js's post-`setReady` IIFE:
     * if this device already has a local unwrapped copy, load it and
     * silently backfill (restoreFromArchive) anything that arrived
     * elsewhere since -- no PIN needed. Otherwise ask the server whether
     * this ACCOUNT has an archive at all: if yes, a different device set
     * one up already and this device needs the PIN to restore it
     * ([ArchivePrompt.Restore]); if no, this is the very first device ever
     * for this account and should set one up now ([ArchivePrompt.Setup]).
     *
     * `since`: see [fetchMessagesHistory]'s doc comment -- the already-set-up
     * (local keypair present) branch below is what runs on EVERY connect for
     * a normal returning user, so this is the steady-state case that
     * benefits from not re-pulling the entire history every time.
     */
    suspend fun checkAfterConnect(since: Long? = null) {
        val me = username ?: return
        val local = keyStore.load(me)
        if (local != null) {
            val (publicKeyJwk, privateKeyJwk) = local
            val privateKeyPkcs8 = runCatching { ArchiveCrypto.privateKeyFromJwk(primitives, privateKeyJwk) }.getOrNull()
            if (privateKeyPkcs8 == null) {
                // Corrupt local copy -- fall through to the server check below as if it were absent, same as web's catch-and-fall-through.
                println("ArchiveManager.checkAfterConnect: local archive keypair for $me failed to import, falling back to server bundle check")
            } else {
                val publicPoint = xyToRawPoint(publicKeyJwk.x.fromBase64Url(), publicKeyJwk.y.fromBase64Url())
                myKeypair = ArchiveCrypto.GeneratedKeypair(privateKeyPkcs8, publicPoint, publicKeyJwk)
                println("ArchiveManager.checkAfterConnect: loaded local archive keypair for $me, backfilling from history")
                restoreFromArchive(privateKeyPkcs8, since)
                return
            }
        }
        val serverBundle = fetchArchiveKeyBundle(me)
        _archivePrompt.value = if (serverBundle != null) ArchivePrompt.Restore(serverBundle) else ArchivePrompt.Setup
    }

    /**
     * Attaches an `archive` field to an outgoing message envelope if (a)
     * this device has an Archive Keypair set up and (b) the recipient has
     * published one too -- both best-effort; a peer who hasn't set up
     * backup yet just doesn't get an archived copy of THIS message (they
     * still get it live over the normal Signal channel; only cross-device
     * recovery is affected). Never throws -- a send should never fail
     * outright over the archive copy not working out, mirrors
     * buildArchiveField's try/catch-and-log-only behavior on web.
     */
    suspend fun buildArchiveField(peerUsername: String, payloadJson: String): ArchiveField? {
        val mine = myKeypair ?: return null
        return try {
            val recipientKey = peerArchiveKeysLock.withLock {
                peerArchiveKeysCache.getOrPut(peerUsername) { fetchArchivePublicKey(peerUsername) }
            } ?: return null
            ArchiveCrypto.encryptForArchive(primitives, payloadJson, mine.publicKeyJwk, recipientKey)
        } catch (e: Throwable) {
            println("ArchiveManager.buildArchiveField: couldn't attach an archive copy for $peerUsername: ${e.message ?: e::class.simpleName}")
            null
        }
    }

    /**
     * Pulls every message this account has ever sent/received from the
     * server's durable log and decrypts each one's archive copy with the
     * now-unwrapped Archive private key, replaying each into
     * [onRestoredEntry] -- this is what actually turns "the server kept
     * your messages" into "you can see them again." Safe to call
     * repeatedly (e.g. after every restore/setup, or every boot on an
     * already-set-up device) -- MessagingRepository's own appendMessage
     * dedup (by message id) makes replay idempotent.
     */
    private suspend fun restoreFromArchive(privateKeyPkcs8: ByteArray, since: Long? = null) {
        val me = username ?: return
        val history = fetchMessagesHistory(me, since) ?: return
        for (msg in history) {
            val archive = msg.archive ?: continue // sent from a device that never set up an archive key
            val isMine = msg.from == me
            val myEnvelope = if (isMine) archive.envelopeForSender else archive.envelopeForRecipient
            val peer = if (isMine) msg.to else msg.from
            try {
                val plaintext = ArchiveCrypto.decryptArchiveEntry(primitives, archive, myEnvelope, privateKeyPkcs8)
                val payload = wireJson.decodeFromString(ChatPayload.serializer(), plaintext)
                onRestoredEntry?.invoke(peer, payload, msg, isMine)
            } catch (e: Throwable) {
                // Wrong key half (shouldn't happen -- envelope selection above is
                // exact) or corrupt row -- skip rather than aborting the whole
                // restore over one bad message, same as web's bare catch here.
                println("ArchiveManager.restoreFromArchive: couldn't decrypt archived message ${msg.id}: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    /**
     * First-time setup: this account has no Archive Keypair anywhere yet.
     * Generates one, wraps the private half with the chosen PIN, publishes
     * the wrapped bundle (server never sees the PIN or the unwrapped key),
     * and keeps an unwrapped copy locally for this device's own immediate use.
     */
    suspend fun submitSetupPin(pin: String) {
        val me = username ?: return
        _archiveBusy.value = true
        _archiveError.value = ""
        try {
            val generated = ArchiveCrypto.generateArchiveKeypair(primitives)
            val wrapped = ArchiveCrypto.wrapPrivateKeyWithPin(primitives, generated.privateKeyPkcs8, pin)
            publishArchiveKeyBundle(
                me,
                ArchiveKeyBundle(generated.publicKeyJwk, wrapped.wrappedPrivateKey, wrapped.wrapIv, wrapped.kdfSalt, wrapped.kdfIterations),
            )
            val privateKeyJwk = ArchiveCrypto.privateKeyToJwk(primitives, generated.privateKeyPkcs8, generated.publicKeyRawPoint)
            keyStore.save(me, generated.publicKeyJwk, privateKeyJwk)
            myKeypair = generated
            _archivePrompt.value = null
        } catch (e: Throwable) {
            _archiveError.value = e.message ?: "couldn't set up backup right now"
        } finally {
            _archiveBusy.value = false
        }
    }

    /**
     * New-device restore: server already has an archive for this account (a
     * DIFFERENT device set it up) -- unwrap it with the PIN, keep a local
     * copy so this device doesn't need the PIN again, then pull + decrypt
     * history. Two separate try/catches, not one -- mirrors
     * submitArchiveRestorePin's own doc comment on web: unwrapPrivateKeyWithPin
     * failing (the AES-GCM tag check) is the only real "wrong PIN" signal
     * there is, but a correct PIN unwraps fine and any later step (a
     * network hiccup fetching history, say) can still fail for an unrelated
     * reason -- conflating the two would blame every failure on the PIN and
     * send someone hunting for a typo that isn't there.
     */
    suspend fun submitRestorePin(pin: String) {
        val me = username ?: return
        val prompt = _archivePrompt.value as? ArchivePrompt.Restore ?: return
        _archiveBusy.value = true
        _archiveError.value = ""

        val privateKeyPkcs8 = try {
            ArchiveCrypto.unwrapPrivateKeyWithPin(primitives, prompt.bundle, pin)
        } catch (e: Throwable) {
            _archiveError.value = "That PIN didn't work -- double check it and try again."
            _archiveBusy.value = false
            return
        }
        try {
            val publicKeyJwk = prompt.bundle.publicKey
            val publicPoint = xyToRawPoint(publicKeyJwk.x.fromBase64Url(), publicKeyJwk.y.fromBase64Url())
            val privateKeyJwk = ArchiveCrypto.privateKeyToJwk(primitives, privateKeyPkcs8, publicPoint)
            keyStore.save(me, publicKeyJwk, privateKeyJwk)
            myKeypair = ArchiveCrypto.GeneratedKeypair(privateKeyPkcs8, publicPoint, publicKeyJwk)
            _archivePrompt.value = null
            restoreFromArchive(privateKeyPkcs8)
        } catch (e: Throwable) {
            _archiveError.value = "Your PIN was right, but restoring your history hit a problem${e.message?.let { " ($it)" } ?: ""}. Try again in a moment."
        } finally {
            _archiveBusy.value = false
        }
    }

    /** "I don't have/remember my PIN" -- proceeds without cross-device history on this device. Doesn't touch the server's archive at all, so entering the correct PIN later (e.g. from the same prompt reappearing next login) still works. */
    fun dismissPrompt() {
        _archivePrompt.value = null
        _archiveError.value = ""
    }

    // ---- Relay server HTTP calls -- mirrors d2m_web's api.ts's Archive section (publishArchiveKeyBundle/fetchArchiveKeyBundle/fetchArchivePublicKey/messagesHistory) exactly. ----
    // Manual JSON encode/decode against `httpClient` directly (not ApiClient's
    // ContentNegotiation), same reason MessagingRepository's own media/
    // turn-credentials calls and SignalCryptoProvider's key calls do: that
    // client's JSON is snake_case-configured for d2m_core_engine's Pydantic
    // backend, while the messaging-framework relay server (this is a call to
    // IT, not the core engine) uses plain camelCase wire shapes.

    private suspend fun publishArchiveKeyBundle(username: String, bundle: ArchiveKeyBundle) {
        val client = httpClient ?: return
        client.put("${MessagingConfig.httpBaseUrl}/archive/$username") {
            contentType(ContentType.Application.Json)
            setBody(wireJson.encodeToString(ArchiveKeyBundle.serializer(), bundle))
        }
    }

    /** 404 (no archive set up for this account yet) is a normal, expected outcome -- Ktor's default expectSuccess throws for it same as any other non-2xx, so any failure here (a real 404, or a genuine network/server error) is treated the same way callers already treat a "no bundle" answer: fall through to first-time setup. */
    private suspend fun fetchArchiveKeyBundle(username: String): ArchiveKeyBundle? {
        val client = httpClient ?: return null
        return try {
            val response = client.get("${MessagingConfig.httpBaseUrl}/archive/$username")
            wireJson.decodeFromString(ArchiveKeyBundle.serializer(), response.bodyAsText())
        } catch (e: Throwable) {
            null
        }
    }

    private suspend fun fetchArchivePublicKey(username: String): JsonWebKey? {
        val client = httpClient ?: return null
        return try {
            val response = client.get("${MessagingConfig.httpBaseUrl}/archive/$username/public")
            wireJson.decodeFromString(ArchivePublicKeyResponse.serializer(), response.bodyAsText()).publicKey
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * `since` (exclusive, epoch millis) is optional -- when the caller
     * already has a local cache with a known latest timestamp (see
     * MessagingRepository.start()'s ChatLocalStore.maxSentAt call), passing
     * it here asks the relay server for only what's newer instead of this
     * account's entire message history, matching the `since` param
     * messaging-framework's /messages/history route now supports. Omitting
     * it (null) preserves the original full-history behavior exactly --
     * still used for a genuine new-device PIN restore (submitRestorePin),
     * which really does need everything.
     */
    private suspend fun fetchMessagesHistory(username: String, since: Long? = null): List<MessageEnvelope>? {
        val client = httpClient ?: return null
        return try {
            val response = client.get("${MessagingConfig.httpBaseUrl}/messages/history") {
                parameter("user", username)
                if (since != null) parameter("since", since)
            }
            wireJson.decodeFromString(MessagesHistoryResponse.serializer(), response.bodyAsText()).messages
        } catch (e: Throwable) {
            println("ArchiveManager.fetchMessagesHistory: failed for $username: ${e.message ?: e::class.simpleName}")
            null
        }
    }
}
