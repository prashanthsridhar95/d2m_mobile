package com.d2m.app.messaging

import com.d2m.app.data.local.ChatLocalStore
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.messaging.crypto.CryptoProvider
import com.d2m.app.messaging.crypto.archive.ArchiveManager
import com.d2m.app.messaging.crypto.archive.ArchivePrompt
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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
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
 * tracking, and (now) the Archive Keypair cross-device history-recovery
 * system -- see [archiveManager] (crypto/archive/ArchiveManager.kt) for the
 * state machine and crypto/archive/ArchiveCrypto.kt for the underlying
 * P-256 ECDH/AES-GCM/PBKDF2 crypto. Every send path below attaches a
 * best-effort `archive` field via [archiveManager]'s buildArchiveField;
 * [start] wires the post-connect local-cache/server-bundle check
 * (checkAfterConnect) and replays decrypted history back in via
 * [applyArchivedPayload].
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

// @Serializable -- persisted to the local chat cache as JSON (see
// data/local/ChatLocalStore.kt); needs no wire-protocol involvement (call
// signals are ephemeral and never touch ChatPayload, see appendCallLogMessage's
// doc comment below), this annotation is purely for that local disk format.
@Serializable
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
    private val archiveManager: ArchiveManager,
    private val localStore: ChatLocalStore,
) {
    init {
        // Wired once here rather than per-restore -- see ArchiveManager.restoreFromArchive's
        // doc comment: it can fire on every boot (already-set-up device) or
        // once after a PIN restore, and either way just needs somewhere to
        // hand decrypted history back to.
        archiveManager.onRestoredEntry = { peerUsername, payload, msg, isMine -> applyArchivedPayload(peerUsername, payload, msg, isMine) }
    }

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
        // Recovered-from doesn't mean "fine" -- this IS the last line of
        // defense catching something no individual call site's own
        // try/catch anticipated. println alone vanishes the moment nobody's
        // watching logcat; reportNonFatal surfaces it in Crashlytics so it's
        // still visible after the fact (see CrashReporter.kt).
        reportNonFatal(e, "MessagingRepository uncaught coroutine exception")
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
    private val _messagesByPeer = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    private val _typingByPeer = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val _presenceByUser = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val _unreadByPeer = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _peerDisplayNames = MutableStateFlow<Map<String, String>>(emptyMap())
    // replay = 1 (not just extraBufferCapacity): SharedFlow's extra buffer
    // only smooths delivery for collectors that are ALREADY subscribed when
    // an event is emitted -- a collector that attaches AFTER the emission
    // (e.g. InAppNotificationLayer.kt's LaunchedEffect(Unit), which races
    // cold-start identity/role resolution in App.kt) starts its cursor at
    // "now" and never sees anything buffered before it joined, silently
    // losing the notification with the app foregrounded and no system
    // notification either (see D2MFirebaseMessagingService's isForeground
    // check) -- nothing shown to the user at all. replay = 1 means a
    // late-attaching collector immediately receives the most recent inbox
    // event on subscribe, closing that window; InAppNotificationLayer is
    // effectively a permanent singleton (mounted once per CHILD session in
    // App.kt) so re-showing one stale banner on the rare remount is a
    // reasonable trade for never silently dropping a real one.
    private val _inboxNotifications = MutableSharedFlow<InboxNotification>(replay = 1, extraBufferCapacity = 8)
    /** "when i'm in a chat & i receive messages in that chat, i want this tune to be played" -- web's useMessaging.js has an explicit else-branch for exactly this (playInChatTone(), see lib/sound.js) that mobile never had at all; this is that same signal. Emits the sender's username whenever a text/media message arrives for the peer whose thread is ALREADY open (the complementary case to inboxNotifications above, which only fires when it ISN'T) -- see markUnreadOrChime below. No replay: a stale "someone messaged the chat you have open" chime after a fresh subscribe would be actively wrong, unlike inboxNotifications' banner. */
    private val _inChatMessageEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
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

    // Second, in-memory-only dedup layer for decryptAndPersistIncoming --
    // see that function's doc comment. The disk-backed _messagesByPeer check
    // there only protects Text/Media (the only payload types appendMessage
    // ever persists); a CallSignal.Invite never touches _messagesByPeer at
    // all, so without this a call invite decrypted directly from an FCM push
    // payload (D2MFirebaseMessagingService.kt's handleIncomingCallPush)
    // could be decrypted AGAIN moments later when that same handler's
    // ensureConnected() call brings the socket up and the server replays the
    // exact same invite from pending_call_invites (see messaging-framework's
    // store.ts) -- two concurrent decrypt attempts on the same ciphertext,
    // which would corrupt the Signal ratchet on whichever one loses the
    // race. Bounded (drops the oldest id past MAX_PROCESSED_ENVELOPE_IDS) --
    // this only needs to cover "two attempts on the same envelope within
    // roughly the same connect cycle," not a permanent record (that's what
    // the disk-backed check and the server's own single-use queue are for).
    private val processedEnvelopeIdsMutex = kotlinx.coroutines.sync.Mutex()
    private val processedEnvelopeIds = LinkedHashSet<String>()

    private suspend fun claimEnvelopeForProcessing(id: String): Boolean = processedEnvelopeIdsMutex.withLock {
        if (!processedEnvelopeIds.add(id)) return@withLock false
        if (processedEnvelopeIds.size > 500) {
            val oldest = processedEnvelopeIds.iterator()
            oldest.next()
            oldest.remove()
        }
        true
    }

    /** Registered by call/CallManager.kt at DI-construction time -- routes incoming ChatPayload.Call signals here instead of into chat history. */
    var callSignalHandler: (suspend (fromUsername: String, signal: CallSignal) -> Unit)? = null

    /** Also registered by call/CallManager.kt -- lets a mid-ring invite that was lost to a session desync be resent once the peer's session.reset round-trip completes (mirrors resendInvite in useMessaging.js). Kept as a separate callback rather than a direct CallManager reference to avoid a dependency cycle (CallManager already depends on this repository). */
    var sessionResetHandler: ((fromUsername: String) -> Unit)? = null

    val isProductionGradeEncryption: Boolean get() = cryptoProvider.isProductionGrade
    val unreadByPeer: StateFlow<Map<String, Int>> get() = _unreadByPeer.asStateFlow()
    val inboxNotifications: SharedFlow<InboxNotification> = _inboxNotifications.asSharedFlow()
    val inChatMessageEvents: SharedFlow<String> = _inChatMessageEvents.asSharedFlow()

    /** True only while wsClient actually has a live session -- see MessagingWsClient.isConnected's own doc comment. Exposed here mainly for [ensureConnected] below, but also useful directly (e.g. a "reconnecting…" indicator). */
    val isConnected: StateFlow<Boolean> get() = wsClient.isConnected

    /**
     * Ensures [start] has run and waits (bounded) for a live socket before
     * returning -- for callers OUTSIDE any Compose lifecycle that would
     * otherwise trigger [start] itself (App.kt's LaunchedEffect, ChatPane's
     * DisposableEffect): specifically MessageReplyReceiver.kt and
     * CallActionReceiver.kt, which can fire from a BroadcastReceiver in a
     * freshly-spawned process (the app was fully killed, nothing has called
     * [start] yet in this process at all) or one where the socket has gone
     * silently stale in the background without the app knowing (see
     * MessagingWsClient's own reconnect-loop doc comment). Without this, a
     * reply typed straight from a message notification would call
     * [sendText] against a dead/nonexistent connection and fail immediately
     * -- reported directly: "when i reply to it, it stops at the mobile
     * itself... shows as failed" until the app is opened by hand (which is
     * what finally calls [start] for real and lets the retry succeed).
     *
     * Safe to call when already connected (returns true immediately, no
     * extra work) and safe to call [start] again here even if it already
     * ran once in this process -- [start] itself no-ops past its first
     * successful call ([started] guard).
     */
    suspend fun ensureConnected(httpClient: HttpClient, timeoutMs: Long = 6_000): Boolean {
        if (wsClient.isConnected.value) return true
        start(httpClient)
        return withTimeoutOrNull(timeoutMs) {
            wsClient.isConnected.first { it }
        } ?: false
    }

    /**
     * "Mark as read" notification action support (MarkReadReceiver.kt) --
     * sends a real read receipt for one specific message (the same protocol
     * effect opening the chat and reading it has: turns the SENDER's tick
     * blue) and clears this peer's unread badge, without needing
     * [setActivePeer]/any UI to have ever run. Needs a live socket -- same
     * as every other wsClient.send call, silently no-ops if disconnected
     * (see MessagingWsClient.send's own doc comment) -- so callers
     * (MarkReadReceiver) call [ensureConnected] first, identical to
     * MessageReplyReceiver.kt's reply action.
     */
    suspend fun markMessageRead(peerUsername: String, messageId: String) {
        wsClient.send(ClientToServer.ReceiptRead(messageId, peerUsername))
        _unreadByPeer.update { if ((it[peerUsername] ?: 0) != 0) it + (peerUsername to 0) else it }
    }

    // ---- Archive Keypair (cross-device history restore) -- delegates straight to ArchiveManager; see that class + ArchivePinDialog.kt for the PIN UI this drives. ----
    val archivePrompt: StateFlow<ArchivePrompt?> get() = archiveManager.archivePrompt
    val archiveBusy: StateFlow<Boolean> get() = archiveManager.archiveBusy
    val archiveError: StateFlow<String> get() = archiveManager.archiveError
    suspend fun submitArchiveSetupPin(pin: String) = archiveManager.submitSetupPin(pin)
    suspend fun submitArchiveRestorePin(pin: String) = archiveManager.submitRestorePin(pin)
    fun dismissArchivePrompt() = archiveManager.dismissPrompt()

    fun currentUsername(): String? = myUsername

    /** Called by ChatPane whenever it composes with a known (peerId, peerName) pair -- lets the globally-mounted CallLayer show a real display name on an incoming-call toast instead of the raw username, for any peer whose chat has been opened this session. */
    fun rememberPeerName(peerD2mId: String, name: String) {
        _peerDisplayNames.update { it + (d2mIdToMessagingUsername(peerD2mId) to name) }
    }

    /**
     * Same cache as [rememberPeerName], keyed directly by messaging username
     * instead of a hyphenated d2m id -- for callers that only ever have the
     * username form and no reason to derive a d2m id, specifically
     * D2MFirebaseMessagingService: it already resolves a real display name
     * (via resolveDisplayName) for a push notification's `sender`, and on a
     * cold FCM-woken process [_peerDisplayNames] is otherwise completely
     * empty (nothing has composed a ChatPane yet) -- [peerDisplayName] would
     * silently fall back to the raw id, which is exactly what CallLayer.kt
     * showed for a call answered straight from a killed-app notification.
     * Seeding this here means that by the time the app actually opens (and
     * CallLayer/ChatPane compose), the name is already correct.
     */
    fun cachePeerDisplayName(peerUsername: String, name: String) {
        _peerDisplayNames.update { it + (peerUsername to name) }
    }

    fun peerDisplayName(peerUsername: String): String = _peerDisplayNames.value[peerUsername] ?: peerUsername

    /** Cheap synchronous snapshot (no Flow/collector created, unlike [messagesFor]) -- safe to call repeatedly, e.g. inside a sort comparator for ParentMessagesScreen.kt's thread list. */
    fun lastMessageAt(peerUsername: String): Long? = _messagesByPeer.value[peerUsername]?.maxOfOrNull { it.sentAt }

    fun messagesFor(peerUsername: String): StateFlow<List<ChatMessage>> {
        val flow = MutableStateFlow(_messagesByPeer.value[peerUsername].orEmpty())
        scope.launch { _messagesByPeer.collect { flow.value = it[peerUsername].orEmpty() } }
        return flow.asStateFlow()
    }

    // Callers (ConversationHeader, ThreadRow) call isPeerTyping/isPeerOnline
    // directly in @Composable bodies with no `remember` wrapping -- every
    // recomposition (ThreadRow's especially, scrolling a list) would
    // otherwise create ANOTHER fresh MutableStateFlow + launch ANOTHER
    // `scope.launch { collect }` on this repository's own long-lived scope,
    // none of which ever get cancelled (that scope outlives any composable).
    // Caching one derived flow per peer here, keyed on the SAME map both
    // functions already need, means repeat calls for the same peer just
    // return the existing flow instead of leaking a new collector forever.
    private val typingFlows = mutableMapOf<String, StateFlow<Boolean>>()
    private val onlineFlows = mutableMapOf<String, StateFlow<Boolean>>()

    fun isPeerTyping(peerUsername: String): StateFlow<Boolean> = typingFlows.getOrPut(peerUsername) {
        val flow = MutableStateFlow(_typingByPeer.value[peerUsername] ?: false)
        scope.launch { _typingByPeer.collect { flow.value = it[peerUsername] ?: false } }
        flow.asStateFlow()
    }

    fun isPeerOnline(peerUsername: String): StateFlow<Boolean> = onlineFlows.getOrPut(peerUsername) {
        val flow = MutableStateFlow(_presenceByUser.value[peerUsername] ?: false)
        scope.launch { _presenceByUser.collect { flow.value = it[peerUsername] ?: false } }
        flow.asStateFlow()
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
            // Reported directly: "chats once retrieved should be stored
            // locally, so that it can just get updated as & when
            // necessary" -- and separately, call-log bubbles (local-only,
            // never reach the server, see appendCallLogMessage's doc
            // comment) vanishing on restart because _messagesByPeer was
            // purely in-memory. Hydrate from disk BEFORE the socket connects
            // so the thread/chat UI has something to show immediately,
            // rather than a blank screen until the WS handshake and (on an
            // already-archive-set-up device) a full history re-fetch both
            // complete. appendMessage's existing id-based dedup makes this
            // safe to layer live/replayed messages on top of afterward.
            runCatching { _messagesByPeer.value = localStore.loadAll(myUsername!!) }
                .onFailure { e -> println("MessagingRepository.start: local chat hydration failed (non-fatal): ${e.message ?: e::class.simpleName}") }

            cryptoProvider.ensureIdentity(myUsername!!)

            wsClient.onEvent = { event -> handleEvent(event) }
            wsClient.onOpen = {
                for (peer in subscribedPeers) scope.launch { wsClient.send(ClientToServer.PresenceSubscribe(peer)) }
            }
            wsClient.connect(MessagingConfig.wsBaseUrl, myUsername!!, httpClient)
            started = true
            _startupError.value = null
            println("MessagingRepository.start: identity ready, socket connect initiated for $myUsername (see MessagingWsClient logs for actual connect result)")

            // Archive Keypair check -- deliberately fire-and-forget, not
            // awaited before start() returns: live messaging (what everyone
            // needs immediately) shouldn't wait on this, exactly like
            // useMessaging.js's own post-setReady IIFE. See
            // ArchiveManager.checkAfterConnect for what 'restore' vs 'setup'
            // mean.
            archiveManager.attach(httpClient, myUsername!!)
            scope.launch {
                // Passing the local cache's own latest sentAt turns the
                // steady-state case (an already-set-up device reconnecting,
                // which happens on every single start()) from "re-fetch and
                // re-decrypt this account's ENTIRE message history" into
                // "fetch only what's new since what's already on disk" --
                // see messaging-framework's /messages/history `since` param
                // and ChatDatabase.sq's doc comment for the full reasoning.
                val since = runCatching { localStore.maxSentAt(myUsername!!) }.getOrNull()
                runCatching { archiveManager.checkAfterConnect(since) }
                    .onFailure { e -> println("MessagingRepository.start: archive keypair check failed (non-fatal): ${e.message ?: e::class.simpleName}") }
            }
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
            val archiveField = archiveManager.buildArchiveField(peerUsername, payloadJson)
            val sent = wsClient.send(ClientToServer.MessageSend(MessageEnvelope(id, me, peerUsername, ciphertextType, body, Clock.System.now().toEpochMilliseconds(), archive = archiveField)))
            // wsClient.send() silently no-ops (never throws) if the socket
            // isn't connected -- without this check the message would sit at
            // SENDING forever with nothing to explain why (this was the
            // actual remaining cause of "messages not sent... totally
            // silent" after start() itself was confirmed to be succeeding).
            if (!sent) error("not connected to the server")
        } catch (e: Throwable) {
            println("MessagingRepository.sendTextInternal: failed to send $id to $peerUsername: ${e.message ?: e::class.simpleName}")
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
        // A failed media/attachment upload (sendMedia's own catch block --
        // see that function -- leaves `uploadFailed = true` with the ORIGINAL
        // bytes still sitting in `localBytesPreview`, but never stores the
        // fileName/mime/kind anywhere on the ChatMessage itself; those only
        // ever existed as sendMedia's own function parameters, gone the
        // moment that suspend call returned). Retrying via sendTextInternal
        // (the ONLY path this used to fall through to, unconditionally) sent
        // `message.text` -- which sendMedia always creates as an EMPTY
        // string -- as a brand-new blank text message, while the actually-
        // failed attachment stayed stuck with no way to retry it at all.
        // Guarded here rather than silently sending a stray blank message:
        // tell the person to re-attach instead of pretending a retry
        // happened. Full re-upload retry would need fileName/mime/kind
        // persisted on ChatMessage itself, which they aren't today.
        if (message.uploadFailed || (message.localBytesPreview != null && message.media == null)) {
            _sendErrors.tryEmit("Couldn't retry that attachment -- please attach it again")
            return
        }
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
            val archiveField = archiveManager.buildArchiveField(peerUsername, payloadJson)
            val sent = wsClient.send(ClientToServer.MessageSend(MessageEnvelope(id, me, peerUsername, ciphertextType, body, Clock.System.now().toEpochMilliseconds(), archive = archiveField)))
            if (!sent) error("not connected to the server")
        } catch (e: Throwable) {
            println("MessagingRepository.sendMedia: failed to send $id to $peerUsername: ${e.message ?: e::class.simpleName}")
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
            val archiveField = archiveManager.buildArchiveField(peerUsername, payloadJson)
            val sent = wsClient.send(ClientToServer.MessageSend(MessageEnvelope(Uuid.random().toString(), me, peerUsername, ciphertextType, body, Clock.System.now().toEpochMilliseconds(), archive = archiveField)))
            if (!sent) error("not connected to the server")
        } catch (e: Throwable) {
            println("MessagingRepository.editMessage: failed to send edit for $messageId to $peerUsername: ${e.message ?: e::class.simpleName}")
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
            val archiveField = archiveManager.buildArchiveField(peerUsername, payloadJson)
            val sent = wsClient.send(ClientToServer.MessageSend(MessageEnvelope(Uuid.random().toString(), me, peerUsername, ciphertextType, body, Clock.System.now().toEpochMilliseconds(), archive = archiveField)))
            if (!sent) error("not connected to the server")
        } catch (e: Throwable) {
            println("MessagingRepository.deleteForEveryone: failed to send delete for $messageId to $peerUsername: ${e.message ?: e::class.simpleName}")
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
        val owner = myUsername
        if (owner != null) {
            scope.launch {
                runCatching { localStore.delete(owner, messageId) }
                    .onFailure { e -> println("MessagingRepository.deleteForMe: failed to remove $messageId from local cache (non-fatal): ${e.message ?: e::class.simpleName}") }
            }
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
            val archiveField = archiveManager.buildArchiveField(peerUsername, payloadJson)
            val sent = wsClient.send(ClientToServer.MessageSend(MessageEnvelope(Uuid.random().toString(), me, peerUsername, ciphertextType, body, Clock.System.now().toEpochMilliseconds(), archive = archiveField)))
            if (!sent) error("not connected to the server")
        } catch (e: Throwable) {
            println("MessagingRepository.toggleReaction: failed to send $emoji ($action) for ${message.id} to $peerUsername: ${e.message ?: e::class.simpleName}")
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
                println("MessagingRepository.handleEvent: MessageNew ${event.msg.id} from ${event.msg.from}")
                decryptAndPersistIncoming(event.msg)
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
                // Peer couldn't decrypt us -- forget our side of the session FIRST
                // (mirrors useMessaging.js's `await crypto.resetPeer(ev.from)` before
                // resendUndelivered), so the resend below re-establishes a fresh X3DH
                // session instead of re-encrypting on the exact session the peer just
                // gave up on. Without this the resend was a no-op in practice: same
                // stale session in, same stale session out, so the peer's next decrypt
                // failed too and they'd send another session.reset right back.
                println("MessagingRepository.handleEvent: SessionReset from ${event.from} -- resetting our session and resending")
                scope.launch {
                    runCatching { cryptoProvider.resetSession(event.from) }
                    resendUndelivered(event.from)
                }
                sessionResetHandler?.invoke(event.from)
            }
            else -> Unit
        }
    }

    /**
     * Enough of [start] to safely call [decryptAndPersistIncoming] from a
     * process that never ran the real [start] -- specifically,
     * D2MFirebaseMessagingService, which can be woken cold by FCM (app fully
     * killed, no Composable has run, App.kt's LaunchedEffect that normally
     * calls start() never fired) with no httpClient/wsClient/archiveManager
     * available or needed for a one-off decrypt. Sets [myUsername] (from
     * IdentityStore, same derivation start() uses), ensures the local Signal
     * identity exists (idempotent -- safe even if start() already did this),
     * and hydrates [_messagesByPeer] from disk so the id-based dedup guard
     * in decryptAndPersistIncoming actually has something to check against.
     *
     * No-ops (just returns the existing value) if [myUsername] is already
     * set -- either a real start() already ran (warm background case), or
     * this was already called once earlier in this process's lifetime.
     * Conversely, a real start() called afterward is equally safe: same
     * myUsername gets recomputed identically, ensureIdentity/hydration both
     * just redo idempotent work, and connect/archive proceed as normal --
     * this function is a strict subset of start(), never a conflicting path.
     */
    suspend fun ensurePassiveIdentity(): String? {
        myUsername?.let { return it }
        val identity = identityStore.identity.value
        val myId = identity.primaryId ?: identity.sponsorId ?: return null
        val username = d2mIdToMessagingUsername(myId)
        runCatching { cryptoProvider.ensureIdentity(username) }
            .onFailure { e -> println("MessagingRepository.ensurePassiveIdentity: ensureIdentity failed: ${e.message ?: e::class.simpleName}") }
        runCatching { _messagesByPeer.value = localStore.loadAll(username) }
            .onFailure { e -> println("MessagingRepository.ensurePassiveIdentity: local chat hydration failed: ${e.message ?: e::class.simpleName}") }
        myUsername = username
        return username
    }

    /**
     * The one real decrypt+persist implementation for an incoming
     * MessageEnvelope -- shared by handleEvent's MessageNew branch (live
     * socket) and D2MFirebaseMessagingService (FCM data payload, no live
     * socket, see [ensurePassiveIdentity]). Previously this logic lived
     * inline in handleEvent only; pulling it out here is what makes it safe
     * for D2MFirebaseMessagingService to show a real decrypted preview in a
     * push notification, rather than reimplementing decrypt separately --
     * two independent decrypt call sites on the SAME ciphertext would each
     * try to consume the Signal ratchet's message key for that index, and
     * the second one to run would fail (the key is deleted after first use,
     * by design -- that's forward secrecy working correctly, not a bug),
     * likely triggering the SessionReset-loop failure path below for a
     * message that actually decrypted fine the first time.
     *
     * The id-based dedup check up front is what makes it safe to call this
     * on the same envelope from BOTH paths (e.g. FCM decrypts it while the
     * app is killed; the app is then opened, start() hydrates
     * _messagesByPeer from the local store FCM already wrote to, socket
     * connects, server redelivers the same envelope from its durable queue
     * since it was never acked while offline) -- whichever path gets here
     * first "wins" and actually decrypts; the other sees the id already
     * present and returns null without touching the ratchet at all.
     *
     * Returns the decoded ChatPayload on a fresh, successful decrypt (so a
     * caller building a notification preview has real content to show), or
     * null if this id was already processed, decrypt failed, or the
     * plaintext wasn't a valid ChatPayload.
     *
     * Two dedup layers, deliberately: the disk-backed _messagesByPeer check
     * covers Text/Media across a process restart (appendMessage persists
     * those); [claimEnvelopeForProcessing]'s in-memory set additionally
     * covers everything else (notably CallSignal.Invite, which never touches
     * _messagesByPeer at all) against a race WITHIN the same process -- see
     * that function's own doc comment for the concrete scenario this
     * prevents (a push-triggered decrypt racing the same envelope's replay
     * once ensureConnected() brings the socket up).
     */
    suspend fun decryptAndPersistIncoming(envelope: MessageEnvelope): ChatPayload? {
        if (_messagesByPeer.value[envelope.from]?.any { it.id == envelope.id } == true) {
            println("MessagingRepository.decryptAndPersistIncoming: ${envelope.id} from ${envelope.from} already processed, skipping decrypt")
            return null
        }
        if (!claimEnvelopeForProcessing(envelope.id)) {
            println("MessagingRepository.decryptAndPersistIncoming: ${envelope.id} already claimed by a concurrent call, skipping decrypt")
            return null
        }
        val plaintext = runCatching {
            cryptoProvider.decrypt(envelope.from, envelope.ciphertextType, envelope.body)
        }.onFailure { e ->
            println("MessagingRepository.decryptAndPersistIncoming: decrypt failed for ${envelope.id} from ${envelope.from}: ${e.message ?: e::class.simpleName}")
        }.getOrNull()
        if (plaintext == null) {
            // Session desync -- our side is just as broken as theirs (that's
            // WHY decrypt failed), so reset our own session before asking them
            // to re-handshake, exactly like d2m_web's useMessaging.js does
            // (`await crypto.resetPeer(msg.from)` before sending session.reset).
            // Skipping this was a real bug: without it, our NEXT encrypt() to
            // this peer kept reusing the same stale session that just failed
            // to decrypt, so their next decrypt failed too, and they'd send
            // another session.reset right back -- a reset loop that never
            // actually recovers instead of a real re-handshake.
            println("MessagingRepository.decryptAndPersistIncoming: null plaintext for ${envelope.id}, resetting our session with ${envelope.from} and requesting they reset too")
            runCatching { cryptoProvider.resetSession(envelope.from) }
            wsClient.send(ClientToServer.SessionReset(envelope.from))
            return null
        }
        val payload = runCatching {
            messagingProtocolJson.decodeFromString(ChatPayload.serializer(), plaintext)
        }.getOrElse { e ->
            println("MessagingRepository.decryptAndPersistIncoming: failed to decode ChatPayload for ${envelope.id}: ${e.message ?: e::class.simpleName}")
            null
        }

        when (payload) {
            is ChatPayload.Text -> {
                appendMessage(envelope.from, ChatMessage(envelope.id, envelope.from, envelope.to, payload.text, envelope.sentAt, isMine = false, status = MessageStatus.DELIVERED, replyTo = payload.replyTo))
                markUnreadOrChime(envelope.from, payload.text, envelope.id)
            }
            is ChatPayload.Media -> {
                appendMessage(
                    envelope.from,
                    ChatMessage(envelope.id, envelope.from, envelope.to, payload.caption.orEmpty(), envelope.sentAt, isMine = false, status = MessageStatus.DELIVERED, media = payload.media, mediaUrl = mediaUrl(payload.media.blobId), replyTo = payload.replyTo),
                )
                markUnreadOrChime(envelope.from, payload.caption?.takeIf { it.isNotBlank() } ?: "Sent a photo", envelope.id)
            }
            is ChatPayload.Edit -> updateMessage(payload.targetId) { it.copy(text = payload.text, edited = true) }
            is ChatPayload.Delete -> updateMessage(payload.targetId) { it.copy(deleted = true) }
            is ChatPayload.Reaction -> updateMessage(payload.targetId) { applyReaction(it, payload.emoji, envelope.from, payload.action) }
            is ChatPayload.Call -> {
                val isStaleInvite = payload.call is CallSignal.Invite && (Clock.System.now().toEpochMilliseconds() - envelope.sentAt) > 60_000
                if (!isStaleInvite) callSignalHandler?.invoke(envelope.from, payload.call)
            }
            null -> Unit
        }

        wsClient.send(ClientToServer.ReceiptDelivered(envelope.id, envelope.from))
        if ((payload is ChatPayload.Text || payload is ChatPayload.Media) && activePeer == envelope.from) {
            wsClient.send(ClientToServer.ReceiptRead(envelope.id, envelope.from))
        }
        return payload
    }

    private fun markUnreadOrChime(fromUsername: String, preview: String, messageId: String) {
        if (activePeer != fromUsername) {
            _unreadByPeer.update { it + (fromUsername to ((it[fromUsername] ?: 0) + 1)) }
            _inboxNotifications.tryEmit(InboxNotification(fromUsername, preview, messageId))
        } else {
            // Mirrors useMessaging.js's else-branch exactly (no unread badge/
            // toast -- the message just appends live into the already-open
            // conversation -- just the in-chat chime). See ChatPane.kt's
            // collector for where this actually plays.
            _inChatMessageEvents.tryEmit(fromUsername)
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

    /**
     * Replays one decrypted archive entry (ArchiveManager.restoreFromArchive's
     * callback) into chat history -- direct port of useMessaging.js's
     * top-level `applyArchivedPayload` helper, adapted to this class's
     * appendMessage/updateMessage/applyReaction primitives instead of
     * mutating a plain bucket object directly. Deliberately DOESN'T touch
     * unread counters, in-app notifications, delivered/read receipts, or
     * send acks -- those are live-arrival concerns (see handleEvent's
     * MessageNew branch, which this mirrors the payload-application half
     * of); a history replay is silent by design, exactly like the web
     * version's bucket rebuild. `ChatPayload.Call` is a no-op here: call
     * signaling is `ephemeral: true` and never reaches the server's message
     * log in the first place (see ArchiveManager.buildArchiveField's call
     * sites -- sendCallSignal never builds an archive field), so this
     * branch should never actually fire; kept only so the `when` stays
     * exhaustive.
     */
    private fun applyArchivedPayload(peerUsername: String, payload: ChatPayload, msg: MessageEnvelope, isMine: Boolean) {
        when (payload) {
            is ChatPayload.Text -> appendMessage(
                peerUsername,
                ChatMessage(msg.id, msg.from, msg.to, payload.text, msg.sentAt, isMine = isMine, status = if (isMine) MessageStatus.SENT else MessageStatus.DELIVERED, replyTo = payload.replyTo),
            )
            is ChatPayload.Media -> appendMessage(
                peerUsername,
                ChatMessage(
                    msg.id, msg.from, msg.to, payload.caption.orEmpty(), msg.sentAt, isMine = isMine,
                    status = if (isMine) MessageStatus.SENT else MessageStatus.DELIVERED, media = payload.media, mediaUrl = mediaUrl(payload.media.blobId), replyTo = payload.replyTo,
                ),
            )
            is ChatPayload.Edit -> updateMessage(payload.targetId) { it.copy(text = payload.text, edited = true) }
            is ChatPayload.Delete -> updateMessage(payload.targetId) { it.copy(deleted = true) }
            is ChatPayload.Reaction -> updateMessage(payload.targetId) { applyReaction(it, payload.emoji, msg.from, payload.action) }
            is ChatPayload.Call -> Unit
        }
    }

    private fun applyReaction(message: ChatMessage, emoji: String, byUsername: String, action: String): ChatMessage {
        val current = message.reactions[emoji].orEmpty()
        val updated = if (action == "add") current + byUsername else current - byUsername
        val reactions = if (updated.isEmpty()) message.reactions - emoji else message.reactions + (emoji to updated)
        return message.copy(reactions = reactions)
    }

    private fun appendMessage(peerUsername: String, message: ChatMessage) {
        var appended = false
        _messagesByPeer.update { current ->
            val existing = current[peerUsername].orEmpty()
            if (existing.any { it.id == message.id }) return@update current // dedup (e.g. resend after re-handshake)
            appended = true
            current + (peerUsername to (existing + message))
        }
        // Persist after the in-memory dedup check -- a message already on
        // disk (replay/resend hitting the same id) shouldn't re-write a row
        // that (for a call log, at least) may have since been locally
        // edited/deleted, and skipping the write entirely for a no-op is
        // also just less disk I/O for the common resend/replay case.
        if (appended) persistMessage(peerUsername, message)
    }

    private fun updateMessage(id: String, transform: (ChatMessage) -> ChatMessage) {
        var touchedPeer: String? = null
        var touchedMessage: ChatMessage? = null
        _messagesByPeer.update { all ->
            all.mapValues { (peer, list) ->
                list.map {
                    if (it.id == id) {
                        val updated = transform(it)
                        touchedPeer = peer
                        touchedMessage = updated
                        updated
                    } else it
                }
            }
        }
        val peer = touchedPeer
        val message = touchedMessage
        if (peer != null && message != null) persistMessage(peer, message)
    }

    /** Fire-and-forget disk write on [scope] -- see ChatLocalStore.kt's doc comment on why this never blocks the caller. No-op before [start] has set [myUsername] (shouldn't happen in practice -- every mutation path above only runs after a live/hydrated session exists -- but guarded rather than assumed). */
    private fun persistMessage(peerUsername: String, message: ChatMessage) {
        val owner = myUsername ?: return
        scope.launch {
            runCatching { localStore.upsert(owner, peerUsername, message) }
                .onFailure { e -> println("MessagingRepository.persistMessage: failed to persist ${message.id} (non-fatal, in-memory state unaffected): ${e.message ?: e::class.simpleName}") }
        }
    }
}
