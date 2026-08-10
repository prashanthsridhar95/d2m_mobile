package com.d2m.app.messaging

import com.d2m.app.data.session.IdentityStore
import com.d2m.app.messaging.crypto.CryptoProvider
import com.d2m.app.messaging.protocol.ChatPayload
import com.d2m.app.messaging.protocol.ClientToServer
import com.d2m.app.messaging.protocol.MessageEnvelope
import com.d2m.app.messaging.protocol.ServerToClient
import com.d2m.app.messaging.protocol.messagingProtocolJson
import com.d2m.app.messaging.transport.MessagingWsClient
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Ties MessagingWsClient (transport) + CryptoProvider (encryption boundary,
 * see that file's doc comment on what's real vs. stubbed) + IdentityStore
 * (username derivation) together, exposing per-peer message state as Flows
 * for ChatPane to render. Mirrors the shape of d2m_web's useMessaging
 * hook/MessagingProvider: one connection, hoisted above individual screens
 * (owned by this singleton repository rather than per-Composable state), so
 * it survives navigation the same way the web version's shell-level
 * provider does.
 *
 * Config note: messaging-framework is a SEPARATE backend service from
 * d2m_core_engine (own Postgres/Redis, own port -- default :4000 in dev,
 * matching D2M_Messaging_Integration_Plan.md §4's VITE_MSG_SERVER_URL/
 * VITE_MSG_WS_URL env vars) -- see MessagingConfig below for the equivalent
 * mobile config point.
 */
data class ChatMessage(
    val id: String,
    val fromUsername: String,
    val toUsername: String,
    val text: String,
    val sentAt: Long,
    val isMine: Boolean,
)

object MessagingConfig {
    var httpBaseUrl: String = "http://127.0.0.1:4000"
    var wsBaseUrl: String = "ws://127.0.0.1:4000"
}

class MessagingRepository(
    private val wsClient: MessagingWsClient,
    private val cryptoProvider: CryptoProvider,
    private val identityStore: IdentityStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _messagesByPeer = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    private val _typingByPeer = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val _presenceByUser = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private var myUsername: String? = null
    private var started = false

    val isProductionGradeEncryption: Boolean get() = cryptoProvider.isProductionGrade

    fun messagesFor(peerUsername: String): StateFlow<List<ChatMessage>> {
        // Derived, read-only view -- simplest correct implementation for this
        // scaffold is to re-map the whole map on every emission; fine at this
        // message-volume scale, revisit if a real usage pattern shows otherwise.
        val flow = MutableStateFlow(_messagesByPeer.value[peerUsername].orEmpty())
        scope.launch {
            _messagesByPeer.collect { flow.value = it[peerUsername].orEmpty() }
        }
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

    suspend fun start(httpClient: HttpClient) {
        if (started) return
        val identity = identityStore.identity.value
        val myId = identity.primaryId ?: identity.sponsorId ?: return
        myUsername = d2mIdToMessagingUsername(myId)
        cryptoProvider.ensureIdentity(myUsername!!)

        wsClient.onEvent = { event -> handleEvent(event) }
        wsClient.connect(MessagingConfig.wsBaseUrl, myUsername!!, httpClient)
        started = true
    }

    fun stop() {
        wsClient.disconnect()
        started = false
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun sendText(peerD2mId: String, text: String) {
        val me = myUsername ?: return
        val peerUsername = d2mIdToMessagingUsername(peerD2mId)
        val payload = ChatPayload.Text(text)
        val payloadJson = messagingProtocolJson.encodeToString(ChatPayload.serializer(), payload)
        val (ciphertextType, body) = cryptoProvider.encrypt(peerUsername, payloadJson)

        val envelope = MessageEnvelope(
            id = Uuid.random().toString(),
            from = me,
            to = peerUsername,
            ciphertextType = ciphertextType,
            body = body,
            sentAt = Clock.System.now().toEpochMilliseconds(),
        )
        wsClient.send(ClientToServer.MessageSend(envelope))

        appendMessage(peerUsername, ChatMessage(envelope.id, me, peerUsername, text, envelope.sentAt, isMine = true))
    }

    suspend fun sendTyping(peerD2mId: String, isTyping: Boolean) {
        val peerUsername = d2mIdToMessagingUsername(peerD2mId)
        wsClient.send(if (isTyping) ClientToServer.TypingStart(peerUsername) else ClientToServer.TypingStop(peerUsername))
    }

    private fun handleEvent(event: ServerToClient) {
        when (event) {
            is ServerToClient.MessageNew -> scope.launch {
                val plaintext = runCatching {
                    cryptoProvider.decrypt(event.msg.from, event.msg.ciphertextType, event.msg.body)
                }.getOrNull() ?: return@launch
                val payload = runCatching {
                    messagingProtocolJson.decodeFromString(ChatPayload.serializer(), plaintext)
                }.getOrNull()
                val text = (payload as? ChatPayload.Text)?.text ?: plaintext
                appendMessage(event.msg.from, ChatMessage(event.msg.id, event.msg.from, event.msg.to, text, event.msg.sentAt, isMine = false))
                wsClient.send(ClientToServer.ReceiptDelivered(event.msg.id, event.msg.from))
            }
            is ServerToClient.Typing -> _typingByPeer.update { it + (event.from to (event.state == "start")) }
            is ServerToClient.PresenceUpdate -> _presenceByUser.update { it + (event.user to event.online) }
            is ServerToClient.SessionReset -> { /* Signal re-handshake trigger -- see crypto/CryptoProvider.kt; no-op for the stub provider */ }
            else -> Unit
        }
    }

    private fun appendMessage(peerUsername: String, message: ChatMessage) {
        _messagesByPeer.update { current ->
            val existing = current[peerUsername].orEmpty()
            current + (peerUsername to (existing + message))
        }
    }
}
