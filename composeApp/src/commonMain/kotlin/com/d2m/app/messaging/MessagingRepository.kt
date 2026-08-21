package com.d2m.app.messaging

import com.d2m.app.data.session.IdentityStore
import com.d2m.app.messaging.crypto.CryptoProvider
import com.d2m.app.messaging.protocol.CallSignal
import com.d2m.app.messaging.protocol.ChatPayload
import com.d2m.app.messaging.protocol.ClientToServer
import com.d2m.app.messaging.protocol.MediaMeta
import com.d2m.app.messaging.protocol.MessageEnvelope
import com.d2m.app.messaging.protocol.ReplyContext
import com.d2m.app.messaging.protocol.ServerToClient
import com.d2m.app.messaging.protocol.messagingProtocolJson
import com.d2m.app.messaging.transport.MessagingWsClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Ties MessagingWsClient (transport) + CryptoProvider (encryption boundary,
 * see that file's doc comment on what's real vs. stubbed) + IdentityStore
 * (username derivation) together, exposing per-peer message/typing/presence/
 * unread state as Flows for ChatPane (and the call layer) to render. Mirrors
 * d2m_web's useMessaging hook: one connection, hoisted above individual
 * screens (owned by this singleton repository), survives navigation the
 * same way the web version's shell-level MessagingProvider does.
 *
 * Full text-messaging parity with useMessaging.js as of this pass: send/
 * receive text + media, edit, delete-for-everyone, delete-for-me, reactions,
 * typing, presence, delivered/read receipts, unread counters, active-peer
 * tracking. Deliberately NOT ported: the Archive Keypair cross-device
 * history-recovery system (archiveCrypto.ts/archiveStore.ts) -- it only
 * makes sense once a real CryptoProvider exists (it's an extension of real
 * Signal Protocol identity, not a separate feature), and a `session.reset`
 * recovery loop for a REAL crypto desync (this build's stub never actually
 * desyncs, so `session.reset` here is wired for wire-compatibility and
 * best-effort resend, not real Double Ratchet re-handshake).
 *
 * Call signaling: `sendCallSignal`/`callSignalHandler` below are the seam
 * `call/CallManager.kt` plugs into -- see that file. Call signals ride this
 * exact same encrypted channel (tagged `ephemeral`), never touch
 * `_messagesByPeer`.
 */
enum class MessageStatus { SENDING, SENT, DELIVERED, READ, FAILED }

data class ChatMessage(
    val id: String,
    val fromUsername: String,
    val toUsername: String,
    val text: String,
    val sentAt: Long,
    val isMine: Boolean,
    val status: MessageStatus = MessageStatus.SENT,
    val reactions: Map<String, Set<String>> = emptyMap(), // emoji -> usernames who reacted
    val replyTo: ReplyContext? = null,
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val media: MediaMeta? = null,
    val mediaUrl: String? = null,
    val uploading: Boolean = false,
    val uploadPct: Int = 0,
    val uploadFailed: Boolean = false,
    val localBytesPreview: ByteArray? = null,
    val callLog: CallLogInfo? = null,
)

data class CallLogInfo(val media: String, val reason: String, val durationSec: Int, val incoming: Boolean)

/**
 * One event per incoming message from a peer whose chat ISN'T the one
 * currently open (same condition as the unread-badge increment right next
 * to where this is emitted) -- consumed by messaging/ui/InAppNotificationLayer.kt
 * to show an Instagram-style in-app banner (point 4 of the notification
 * request), and by push/LocalNotificationBridge.android.kt to post a real
 * system notification when the app is backgrounded (point 3/5). Rides the
 * live WebSocket stream directly, so it works with zero backend push
 * involvement.
 */
data class InboxNotification(val peerUsername: String, val preview: String, val messageId: String)

object MessagingConfig {
    var httpBaseUrl: String = "https://chat.prashanthsridhar.com"
    var wsBaseUrl: String = "wss://chat.prashanthsridhar.com"
}

@Serializable
private data class UploadMediaResponse(val id: String)

@Serializable
private data class IceServerDto(val urls: kotlinx.serialization.json.JsonElement, val username: String? = null, val credential: String? = null)

@Serializable
private data class TurnCredentialsResponse(val iceServers: List<IceServerDto> = emptyList())

/** Platform-agnostic ICE server description handed to WebRtcEngine.createPeerConnection. */
data class IceServer(val urls: List<String>, val username: String? = null, val credential: String? = null)

private const val TYPING_TIMEOUT_MS = 3_000L

class MessagingRepository(
    private val wsClient: MessagingWsClient,
    private val cryptoProvider: CryptoProvider,
    private val identityStore: IdentityStore,
) {
    /**
     * Safety net for every coroutine launched on [scope]. Without this, an
     * uncaught exception from any of them (most realistically
     * `cryptoProvider.encrypt/decrypt` -- see the send-path try/catches
     * below, added after a real "messages/calls aren't sent or received,
     * and the app crashes" bug report) propagates to the platform's default
     * uncaught-exception handler and kills the whole app. `SupervisorJob`
     * alone does NOT prevent this -- it only stops one child's failure from
     * cancelling its siblings, it doesn't swallow the exception itself.
     * This handler is the last line of defense; individual call sites below
     * still catch what they can locally so failures are recoverable
     * (message marked FAILED, a human-readable message on [sendErrors])
     * rather than just silently logged here.
     */
    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        println("MessagingRepository: uncaught coroutine exception (recovered, not fatal): $e")
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
    private val _messagesByPeer = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    private val _typingByPeer = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val _presenceByUser = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val _unreadByPeer = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _peerDisplayNames = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _inboxNotifications = MutableSharedFlow<InboxNotification>(extraBufferCapacity = 8)
    /** Human-readable, transient failures for ChatPane to surface as a snackbar (Don Norman "visibility of system status" -- a send failure used to just vanish with no feedback at all). Not for call signaling failures, which have their own CallManager.errors -- see that class. */
    private val _sendErrors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val sendErrors: SharedFlow<String> = _sendErrors.asSharedFlow()
    /** Set if `start()` itself fails (identity/key generation, WS connect) -- see that function's doc comment. Persistent (not a one-shot toast like [sendErrors]) since a failed startup means EVERYTHING is broken until the app is relaunched or this is retried; App.kt shows this as a standing banner with a retry action. */
    private val _startupError = MutableStateFlow<String?>(null)
    val startupError: StateFlow<String?> = _startupError.asStateFlow()
    private val typingClearJobs = mutableMapOf<String, kotlinx.coroutines.Job>()
    private val subscribedPeers = mutableSetOf<String>()
    private var activePeer: String? = null
    private var myUsername: String? = null
    private var started = false
    private var httpClient: HttpClient? = null

    /** Registered by call/CallManager.kt at DI-construction time -- routes incoming ChatPayload.Call signals here instead of into chat history. */
    var callSignalHandler: (suspend (fromUsername: String, signal: CallSignal) -> Unit)? = null

    /** Also registered by call/CallManager.kt -- lets a mid-ring invite that was lost to a session desync be resent once the peer's session.reset round-trip completes (mirrors resendInvite in useMessaging.js). Kept as a separate callback rather than a direct CallManager reference to avoid a dependency cycle (CallManager already depends on this repository). */
    var sessionResetHandler: ((fromUsername: String) -> Unit)? = null

    val isProductionGradeEncryption: Boolean get() = cryptoProvider.isProductionGrade
    val unreadByPeer: StateFlow<Map<String, Int>> get() = _unreadByPeer.asStateFlow()
    val inboxNotifications: SharedFlow<InboxNotification> = _inboxNotifications.asSharedFlow()

    fun currentUsername(): String? = myUsername

    /** Called by ChatPane whenever it composes with a known (peerId, peerName) pair -- lets the globally-mounted CallLayer show a real display name on an incoming-call toast instead of the raw username, for any peer whose chat has been opened this session. */
    fun rememberPeerName(peerD2mId: String, name: String) {
        _peerDisplayNames.update { it + (d2mIdToMessagingUsername(peerD2mId) to name) }
    }

    fun peerDisplayName(peerUsername: String): String = _peerDisplayNames.value[peerUsername] ?: peerUsername

    fun messagesFor(peerUsername: String): StateFlow<List<ChatMessage>> {
        val flow = MutableStateFlow(_messagesByPeer.value[peerUsername].orEmpty())
        scope.launch { _messagesByPeer.collect { flow.value = it[peerUsername].orEmpty() } }
        return flow.asStateFlow()
    }

    fun isPeerTyping(peerUsername: String): StateFlow<Boolean> {
        val flow = MutableStateFlow(_typingByPeer.value[peerUsername] ?: false)
        scope.launch { _typingByPeer.collect { flow.value = it[peerUsername] ?: false } }
        return flow.asStateFlow()
    }

    fun isPeerOnline(peerUsername: String): StateFlow<Boolean> {
        val flow = MutableStateFlow(_presenceByUser.value[peerUsername] ?: false)
        scope.launch { _presenceByUser.collect { flow.value = it[peerUsername] ?: false } }
        return flow.asStateFlow()
    }

    fun unreadFor(peerUsername: String): StateFlow<Int> {
        val flow = MutableStateFlow(_unreadByPeer.value[peerUsername] ?: 0)
        scope.launch { _unreadByPeer.collect { flow.value = it[peerUsername] ?: 0 } }
        return flow.asStateFlow()
    }

    /**
     * If this throws (or is swallowed by a caller's own runCatching, as
     * App.kt's cold-start LaunchedEffect does), messaging never starts at
     * all -- no WebSocket, no send, no receive, nothing -- with previously
     * ZERO visible indication why ("calls/messages not sending or
     * receiving... totally silent, no error shown" was reported after the
     * backend itself was confirmed healthy, which points squarely at this
     * function: `cryptoProvider.ensureIdentity()` generates a fresh Signal
     * identity + prekeys on first run via the from-spec crypto in
     * messaging/crypto/signal/ -- code that has explicitly never been
     * verified against a real device -- and had no error handling of its
     * own here). Now catches its own failure and publishes it on
     * [startupError] instead of leaving the caller to decide (and possibly
     * silently swallow, as `runCatching { messagingRepo.start(...) }` in
     * App.kt does) whether the whole subsystem came up.
     */
    suspend fun start(httpClient: HttpClient) {
        this.httpClient = httpClient
        if (started) return
        val identity = identityStore.identity.value
        val myId = identity.primaryId ?: identity.sponsorId ?: return
        myUsername = d2mIdToMessagingUsername(myId)
        try {
            cryptoProvider.ensureIdentity(myUsername!!)

            wsClient.onEvent = { event -> handleEvent(event) }
            wsClient.onOpen = {
                for (peer in subscribedPeers) scope.launch { wsClient.send(ClientToServer.PresenceSubscribe(peer)) }
            }
            wsClient.connect(MessagingConfig.wsBaseUrl, myUsername!!, httpClient)
            started = true
            _startupError.value = null
        } catch (e: Throwable) {
            val message = "Messaging couldn't start: ${e.message ?: e::class.simpleName}"
            println("MessagingRepository.start: $message")
            _startupError.value = message
        }
    }

    fun stop() {
        wsClient.disconnect()
        started = false
    }

    /** Marks `peerD2mId` as the open conversation: clears its unread badge and enables auto-read-receipts for future arrivals (mirrors useMessaging.js's setActivePeer). Pass null when leaving the chat screen entirely. */
    fun setActivePeer(peerD2mId: String?) {
        val peer = peerD2mId?.let { d2mIdToMessagingUsername(it) }
        activePeer = peer
        if (peer != null) {
            subscribe(peer)
            _unreadByPeer.update { if ((it[peer] ?: 0) != 0) it + (peer to 0) else it }
        }
    }

    /** For thread-list rows that aren't the open conversation -- presence only, doesn't affect read-receipt behavior. */
    fun subscribePresence(peerD2mId: String) = subscribe(d2mIdToMessagingUsername(peerD2mId))

    private fun subscribe(peerUsername: String) {
        if (!subscribedPeers.add(peerUsername)) return
        scope.launch { wsClient.send(ClientToServer.PresenceSubscribe(peerUsername)) }
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun sendText(peerD2mId: String, text: String, replyTo: ReplyContext? = null) {
        val me = myUsername ?: return
        val t = text.trim()
        if (t.isEmpty()) return
        val peerUsername = d2mIdToMessagingUsername(peerD2mId)
        val id = Uuid.random().toString()
        appendMessage(peerUsername, ChatMessage(id, me, peerUsername, t, Clock.System.now().toEpochMilliseconds(), isMine = true, status = MessageStatus.SENDING, replyTo = replyTo))
        sendTextInternal(id, me, peerUsername, t, replyTo)
    }

    private suspend fun sendTextInternal(id: String, me: String, peerUsername: String, t: String, replyTo: ReplyContext?) {
        try {
            val payload = ChatPayload.Text(t, replyTo)
            val payloadJson = messagingProtocolJson.encodeToString(ChatPayload.serializer(), payload)
            val (ciphertextType, body) = cryptoProvider.encrypt(peerUsername, payloadJson)
            wsClient.send(ClientToServer.MessageSend(MessageEnvelope(id, me, peerUsername, ciphertextType, body, Clock.System.now().toEpochMilliseconds())))
        } catch (e: Throwable) {
            updateMessage(id) { it.copy(status = MessageStatus.FAILED) }
            _sendErrors.tryEmit("Message couldn't be sent -- tap it to retry")
        }
    }

    /** Re-encrypts and re-sends a message that's sitting at MessageStatus.FAILED -- the tap-to-retry affordance on the failed-message bubble (Don Norman: an error state is only useful if there's an obvious, immediate way to recover from it). */
    suspend fun retryFailedMessage(peerD2mId: String, messageId: String) {
        val me = myUsername ?: return
        val peerUsername = d2mIdToMessagingUsername(peerD2mId)
        val message = _messagesByPeer.value[peerUsername]?.firstOrNull { it.id == messageId } ?: return
        if (message.status != MessageStatus.FAILED) return
        updateMessage(messageId) { it.copy(status = MessageStatus.SENDING) }
        sendTextInternal(messageId, me, peerUsername, message.text, message.replyTo)
    }

    /** Uploads `bytes` to the messaging server's blob store and sends a media chat message. See this class's doc comment: bytes travel un-encrypted today (stub crypto) -- `MediaMeta.key`/`.iv` are placeholders, not real AES-GCM material, until a real CryptoProvider lands. */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun sendMedia(peerD2mId: String, bytes: ByteArray, fileName: String, mime: String, kind: String, width: Int? = null, height: Int? = null, replyTo: ReplyContext? = null) {
        val me = myUsername ?: return
        val client = httpClient ?: return
        val peerUsername = d2mIdToMessagingUsername(peerD2mId)
        val id = Uuid.random().toString()
        appendMessage(
            peerUsername,
            ChatMessage(
                id, me, peerUsername, "", Clock.System.now().toEpochMilliseconds(), isMine = true,
                status = MessageStatus.SENDING, uploading = true, uploadPct = 0, localBytesPreview = bytes, replyTo = replyTo,
            ),
        )
        try {
            val response = client.post("${MessagingConfig.httpBaseUrl}/media") {
                contentType(ContentType.Application.OctetStream)
                setBody(bytes)
            }
            val blobId = messagingProtocolJson.decodeFromString(UploadMediaResponse.serializer(), response.bodyAsText()).id
            val media = MediaMeta(blobId = blobId, key = "", iv = "", mime = mime, name = fileName, size = bytes.size.toLong(), kind = kind, width = width, height = height)
            updateMessage(id) { it.copy(media = media, mediaUrl = mediaUrl(blobId), uploading = false, localBytesPreview = null) }

            val payload = ChatPayload.Media(media, replyTo = replyTo)
            val payloadJson = messagingProtocolJson.encodeToString(ChatPayload.serializer(), payload)
            val (ciphertextType, body) = cryptoProvider.encrypt(peerUsername, payloadJson)
            wsClient.send(ClientToServer.MessageSend(MessageEnvelope(id, me, peerUsername, ciphertextType, body, Clock.System.now().toEpochMilliseconds())))
        } catch (_: Throwable) {
            updateMessage(id) { it.copy(uploading = false, uploadFailed = true, status = MessageStatus.FAILED) }
        }
    }

    fun mediaUrl(blobId: String): String = "${MessagingConfig.httpBaseUrl}/media/${blobId}"

    @OptIn(ExperimentalUuidApi::class)
    suspend fun editMessage(peerD2mId: String, messageId: String, newText: String) {
        val me = myUsername ?: return
        val t = newText.trim()
        if (t.isEmpty()) return
        val peerUsername = d2mIdToMessagingUsername(peerD2mId)
        val previous = _messagesByPeer.value[peerUsername]?.firstOrNull { it.id == messageId }
        updateMessage(messageId) { it.copy(text = t, edited = true) }

        try {
            val payload = ChatPayload.Edit(messageId, t)
            val payloadJson = messagingProtocolJson.encodeToString(ChatPayload.serializer(), payload)
            val (ciphertextType, body) = cryptoProvider.encrypt(peerUsername, payloadJson)
            wsClient.send(ClientToServer.MessageSend(MessageEnvelope(Uuid.random().toString(), me, peerUsername, ciphertextType, body, Clock.System.now().toEpochMilliseconds())))
        } catch (e: Throwable) {
            // Revert the optimistic edit so the bubble doesn't silently show
            // text the peer never actually received (Don Norman: the visible
            // state must match reality, not what we merely attempted).
            if (previous != null) updateMessage(messageId) { previous }
            _sendErrors.tryEmit("Edit couldn't be sent")
        }
    }

    /** Delete-for-everyone: tombstones the bubble on both sides (keeps its slot, shows "deleted"). */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun deleteForEveryone(peerD2mId: String, messageId: String) {
        val me = myUsername ?: return
        val peerUsername = d2mIdToMessagingUsername(peerD2mId)
        val previous = _messagesByPeer.value[peerUsername]?.firstOrNull { it.id == messageId }
        updateMessage(messageId) { it.copy(deleted = true) }

        try {
            val payload = ChatPayload.Delete(messageId)
            val payloadJson = messagingProtocolJson.encodeToString(ChatPayload.serializer(), payload)
            val (ciphertextType, body) = cryptoProvider.encrypt(peerUsername, payloadJson)
            wsClient.send(ClientToServer.MessageSend(MessageEnvelope(Uuid.random().toString(), me, peerUsername, ciphertextType, body, Clock.System.now().toEpochMilliseconds())))
        } catch (e: Throwable) {
            if (previous != null) updateMessage(messageId) { previous }
            _sendErrors.tryEmit("Delete couldn't be sent")
        }
    }

    /** Local-only: removes the row from this device, nothing sent -- the peer's copy is untouched. */
    fun deleteForMe(peerD2mId: String, messageId: String) {
        val peerUsername = d2mIdToMessagingUsername(peerD2mId)
        _messagesByPeer.update { current ->
            val existing = current[peerUsername].orEmpty()
            current + (peerUsername to existing.filterNot { it.id == messageId })
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun toggleReaction(peerD2mId: String, message: ChatMessage, emoji: String) {
        val me = myUsername ?: return
        val peerUsername = d2mIdToMessagingUsername(peerD2mId)
        val alreadyReacted = message.reactions[emoji]?.contains(me) == true
        val action = if (alreadyReacted) "remove" else "add"
        updateMessage(message.id) { applyReaction(it, emoji, me, action) }

        try {
            val payload = ChatPayload.Reaction(message.id, emoji, action)
            val payloadJson = messagingProtocolJson.encodeToString(ChatPayload.serializer(), payload)
            val (ciphertextType, body) = cryptoProvider.encrypt(peerUsername, payloadJson)
            wsClient.send(ClientToServer.MessageSend(MessageEnvelope(Uuid.random().toString(), me, peerUsername, ciphertextType, body, Clock.System.now().toEpochMilliseconds())))
        } catch (e: Throwable) {
            // Revert the optimistic reaction toggle -- same inverse action undoes it.
            updateMessage(message.id) { applyReaction(it, emoji, me, if (action == "add") "remove" else "add") }
            _sendErrors.tryEmit("Reaction couldn't be sent")
        }
    }

    suspend fun sendTyping(peerD2mId: String, isTyping: Boolean) {
        val peerUsername = d2mIdToMessagingUsername(peerD2mId)
        wsClient.send(if (isTyping) ClientToServer.TypingStart(peerUsername) else ClientToServer.TypingStop(peerUsername))
    }

    /**
     * Sends a CallSignal.* over the same encrypted channel, tagged ephemeral
     * (never queued/stored server-side) and callInvite only for the invite
     * that starts a call (lets the server push "incoming call" without
     * seeing content). Called by call/CallManager.kt.
     *
     * Returns false (never throws) on any failure -- most call sites in
     * CallManager fire this from a bare `scope.launch { }` with no try/catch
     * of their own (hangup, decline, ICE candidates, mid-call mute/camera
     * state, mid-ring resend): call signaling is inherently best-effort,
     * same as a dropped UDP packet, so a crypto/network failure here should
     * degrade gracefully, not propagate an uncaught exception that crashes
     * the whole app on a background dispatcher thread. That crash was a real
     * bug: ending a call launched this on CallManager's scope with no
     * handler, so a failure here took the app down right as `teardown()` ran.
     * The two call sites that DO need to know about failure (starting a call,
     * accepting one) check this return value explicitly instead.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun sendCallSignal(peerUsername: String, signal: CallSignal): Boolean {
        val me = myUsername ?: return false
        return try {
            val payload = ChatPayload.Call(signal)
            val payloadJson = messagingProtocolJson.encodeToString(ChatPayload.serializer(), payload)
            val (ciphertextType, body) = cryptoProvider.encrypt(peerUsername, payloadJson)
            val envelope = MessageEnvelope(
                id = Uuid.random().toString(),
                from = me,
                to = peerUsername,
                ciphertextType = ciphertextType,
                body = body,
                sentAt = Clock.System.now().toEpochMilliseconds(),
                ephemeral = true,
                callInvite = signal is CallSignal.Invite,
            )
            wsClient.send(ClientToServer.MessageSend(envelope))
            true
        } catch (e: Throwable) {
            println("MessagingRepository.sendCallSignal: failed to send ${signal::class.simpleName} to $peerUsername: $e")
            false
        }
    }

    /** Appends a call-history bubble ("Missed call", "Video call · 3:12", ...) into chat -- called by CallManager.onEnded. Not a real ChatPayload send: call logs are local-only, same as web (each side independently logs its own view of how the call ended). */
    fun appendCallLogMessage(peerUsername: String, message: ChatMessage) = appendMessage(peerUsername, message)

    suspend fun fetchIceServers(): List<IceServer> {
        val client = httpClient ?: return emptyList()
        val me = myUsername ?: return emptyList()
        return try {
            val resp = client.get("${MessagingConfig.httpBaseUrl}/turn-credentials") { parameter("user", me) }
            val parsed = messagingProtocolJson.decodeFromString(TurnCredentialsResponse.serializer(), resp.bodyAsText())
            parsed.iceServers.map { dto ->
                val urls = when {
                    dto.urls is kotlinx.serialization.json.JsonArray -> dto.urls.map { it.toString().trim('"') }
                    else -> listOf(dto.urls.toString().trim('"'))
                }
                IceServer(urls, dto.username, dto.credential)
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun handleEvent(event: ServerToClient) {
        when (event) {
            is ServerToClient.MessageNew -> scope.launch {
                val plaintext = runCatching {
                    cryptoProvider.decrypt(event.msg.from, event.msg.ciphertextType, event.msg.body)
                }.getOrNull()
                if (plaintext == null) {
                    // Session desync (real once a real CryptoProvider lands) -- ask the peer to re-handshake and resend; matches web's session.reset flow.
                    wsClient.send(ClientToServer.SessionReset(event.msg.from))
                    return@launch
                }
                val payload = runCatching {
                    messagingProtocolJson.decodeFromString(ChatPayload.serializer(), plaintext)
                }.getOrNull()

                when (payload) {
                    is ChatPayload.Text -> {
                        appendMessage(event.msg.from, ChatMessage(event.msg.id, event.msg.from, event.msg.to, payload.text, event.msg.sentAt, isMine = false, status = MessageStatus.DELIVERED, replyTo = payload.replyTo))
                        markUnreadOrChime(event.msg.from, payload.text, event.msg.id)
                    }
                    is ChatPayload.Media -> {
                        appendMessage(
                            event.msg.from,
                            ChatMessage(event.msg.id, event.msg.from, event.msg.to, payload.caption.orEmpty(), event.msg.sentAt, isMine = false, status = MessageStatus.DELIVERED, media = payload.media, mediaUrl = mediaUrl(payload.media.blobId), replyTo = payload.replyTo),
                        )
                        markUnreadOrChime(event.msg.from, payload.caption?.takeIf { it.isNotBlank() } ?: "Sent a photo", event.msg.id)
                    }
                    is ChatPayload.Edit -> updateMessage(payload.targetId) { it.copy(text = payload.text, edited = true) }
                    is ChatPayload.Delete -> updateMessage(payload.targetId) { it.copy(deleted = true) }
                    is ChatPayload.Reaction -> updateMessage(payload.targetId) { applyReaction(it, payload.emoji, event.msg.from, payload.action) }
                    is ChatPayload.Call -> {
                        val isStaleInvite = payload.call is CallSignal.Invite && (Clock.System.now().toEpochMilliseconds() - event.msg.sentAt) > 60_000
                        if (!isStaleInvite) callSignalHandler?.invoke(event.msg.from, payload.call)
                    }
                    null -> Unit
                }

                wsClient.send(ClientToServer.ReceiptDelivered(event.msg.id, event.msg.from))
                if ((payload is ChatPayload.Text || payload is ChatPayload.Media) && activePeer == event.msg.from) {
                    wsClient.send(ClientToServer.ReceiptRead(event.msg.id, event.msg.from))
                }
            }
            is ServerToClient.MessageAck -> updateMessage(event.messageId) { if (it.status == MessageStatus.SENDING) it.copy(status = MessageStatus.SENT) else it }
            is ServerToClient.ReceiptUpdate -> updateMessage(event.messageId) {
                it.copy(status = if (event.status == "read" || it.status == MessageStatus.READ) MessageStatus.READ else MessageStatus.DELIVERED)
            }
            is ServerToClient.Typing -> {
                _typingByPeer.update { it + (event.from to (event.state == "start")) }
                typingClearJobs.remove(event.from)?.cancel()
                if (event.state == "start") {
                    typingClearJobs[event.from] = scope.launch {
                        delay(TYPING_TIMEOUT_MS)
                        _typingByPeer.update { it + (event.from to false) }
                    }
                }
            }
            is ServerToClient.PresenceUpdate -> _presenceByUser.update { it + (event.user to event.online) }
            is ServerToClient.SessionReset -> {
                // Peer couldn't decrypt us -- best-effort resend of anything still
                // SENDING to them (no real session to rebuild against the stub
                // provider; a real CryptoProvider would also rebuild its ratchet
                // session here, same as crypto.resetPeer in useMessaging.js).
                scope.launch { resendUndelivered(event.from) }
                sessionResetHandler?.invoke(event.from)
            }
            else -> Unit
        }
    }

    private fun markUnreadOrChime(fromUsername: String, preview: String, messageId: String) {
        if (activePeer != fromUsername) {
            _unreadByPeer.update { it + (fromUsername to ((it[fromUsername] ?: 0) + 1)) }
            _inboxNotifications.tryEmit(InboxNotification(fromUsername, preview, messageId))
        }
    }

    private suspend fun resendUndelivered(peerUsername: String) {
        val me = myUsername ?: return
        val pending = _messagesByPeer.value[peerUsername].orEmpty().filter { it.isMine && it.status == MessageStatus.SENDING }
        for (m in pending) {
            val payload: ChatPayload = if (m.media != null) ChatPayload.Media(m.media, m.text.takeIf { it.isNotBlank() }, m.replyTo) else ChatPayload.Text(m.text, m.replyTo)
            val payloadJson = messagingProtocolJson.encodeToString(ChatPayload.serializer(), payload)
            val (ciphertextType, body) = runCatching { cryptoProvider.encrypt(peerUsername, payloadJson) }.getOrNull() ?: continue
            wsClient.send(ClientToServer.MessageSend(MessageEnvelope(m.id, me, peerUsername, ciphertextType, body, m.sentAt)))
        }
    }

    private fun applyReaction(message: ChatMessage, emoji: String, byUsername: String, action: String): ChatMessage {
        val current = message.reactions[emoji].orEmpty()
        val updated = if (action == "add") current + byUsername else current - byUsername
        val reactions = if (updated.isEmpty()) message.reactions - emoji else message.reactions + (emoji to updated)
        return message.copy(reactions = reactions)
    }

    private fun appendMessage(peerUsername: String, message: ChatMessage) {
        _messagesByPeer.update { current ->
            val existing = current[peerUsername].orEmpty()
            if (existing.any { it.id == message.id }) return@update current // dedup (e.g. resend after re-handshake)
            current + (peerUsername to (existing + message))
        }
    }

    private fun updateMessage(id: String, transform: (ChatMessage) -> ChatMessage) {
        _messagesByPeer.update { all ->
            all.mapValues { (_, list) -> list.map { if (it.id == id) transform(it) else it } }
        }
    }
}
