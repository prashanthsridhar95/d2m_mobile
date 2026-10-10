package com.d2m.app.messaging.protocol

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Direct Kotlin port of messaging-framework/packages/protocol/src/index.ts
 * -- the wire envelope the server routes, opaque past this shape (it never
 * inspects or decrypts `body`). See that file for the canonical source;
 * kept 1:1 field-for-field here rather than reshaped, so a future real
 * CryptoProvider implementation can be checked against the original TS
 * types directly.
 *
 * Now fully ported, including media (ChatPayload.Media/MediaMeta), replies
 * (ReplyContext) and call signaling (ChatPayload.Call/CallSignal) -- see
 * messaging/README.md for the current real-vs-scaffolded status of what
 * consumes these (MessagingRepository for chat, call/CallManager.kt for
 * signaling). `archive` on MessageEnvelope (JsonWebKey/ArchiveEnvelope/
 * ArchiveField/ArchiveKeyBundle below) is now real -- see
 * messaging/crypto/archive/ArchiveCrypto.kt and ArchiveManager.kt -- and is
 * populated on Android (matching this build's Signal crypto: real on
 * Android, stubbed on iOS).
 */

@Serializable
data class PreKeyPublic(val keyId: Int, val publicKey: String)

@Serializable
data class SignedPreKeyPublic(val keyId: Int, val publicKey: String, val signature: String)

@Serializable
data class KeyBundleUpload(
    val registrationId: Int,
    val identityKey: String,
    val signedPreKey: SignedPreKeyPublic,
    val preKeys: List<PreKeyPublic>,
)

@Serializable
data class PreKeyBundleResponse(
    val registrationId: Int,
    val identityKey: String,
    val signedPreKey: SignedPreKeyPublic,
    val preKey: PreKeyPublic? = null,
)

@Serializable
data class MessageEnvelope(
    val id: String,
    val from: String,
    val to: String,
    val ciphertextType: String, // "prekey" | "signal"
    val body: String, // base64 ciphertext, opaque to the server
    val sentAt: Long,
    val ephemeral: Boolean = false,
    // Set only on the one ephemeral envelope carrying a CallSignal.Invite --
    // lets the server trigger an "incoming call" push without ever seeing
    // call content. See callManager's `send` wiring.
    val callInvite: Boolean = false,
    // Long-term recoverable copy, additive to (never a replacement for) the
    // Signal ratchet ciphertext in `body` above -- absent on rows from a
    // device that hasn't set up an Archive Keypair yet, or on ephemeral
    // (call-signaling) envelopes, which are never archived. See
    // ArchiveManager.buildArchiveField.
    val archive: ArchiveField? = null,
)

/**
 * P-256 ECDH public key in JSON Web Key form -- the literal wire shape
 * `crypto.subtle.exportKey("jwk", ...)` produces on the web side (and what
 * this app's ArchivePrimitives/ArchiveCodec produce/consume on Android).
 * `x`/`y` (and `d`, private-key exports only, used for THIS DEVICE's own
 * local cache -- never sent to the server or a peer) are base64url WITHOUT
 * padding (RFC 7515), a DIFFERENT alphabet from the standard base64 used
 * for every other byte-blob field in this file -- see ArchiveCodec.kt's doc
 * comment.
 */
@Serializable
data class JsonWebKey(
    val kty: String = "EC",
    val crv: String = "P-256",
    val x: String,
    val y: String,
    val d: String? = null,
)

/**
 * One person's copy of a message's archive content-key, sealed via
 * ephemeral ECDH to their Archive Keypair's public key (see
 * ArchiveKeyBundle below). `ephemeralPublicKey` is the one-off keypair the
 * sender generated just for this seal -- the recipient redoes the same
 * ECDH using their own private key + this public key to re-derive the
 * wrapping key. Direct port of messaging-framework's `ArchiveEnvelope`.
 */
@Serializable
data class ArchiveEnvelope(
    val ephemeralPublicKey: JsonWebKey,
    val wrappedKey: String, // base64 AES-GCM ciphertext of the per-message content key
    val wrapIv: String, // base64
)

/** The `archive` field attached to an outgoing MessageEnvelope -- direct port of the inline type on messaging-framework's `MessageEnvelope.archive`. */
@Serializable
data class ArchiveField(
    val body: String, // base64 AES-GCM ciphertext (the plaintext ChatPayload, sealed independently of MessageEnvelope.body above)
    val iv: String, // base64
    val envelopeForSender: ArchiveEnvelope,
    val envelopeForRecipient: ArchiveEnvelope,
)

/**
 * What a client publishes to `PUT /archive/:username` -- server stores this
 * verbatim, opaque past the JWK shape (it never sees the PIN or the
 * unwrapped private key). Direct port of messaging-framework's
 * `ArchiveKeyBundle`.
 */
@Serializable
data class ArchiveKeyBundle(
    val publicKey: JsonWebKey, // P-256 ECDH public key
    val wrappedPrivateKey: String, // base64 AES-GCM ciphertext of the exported private key
    val wrapIv: String, // base64
    val kdfSalt: String, // base64, PBKDF2 salt
    val kdfIterations: Int,
)

/** `GET /archive/:username/public` response shape. */
@Serializable
data class ArchivePublicKeyResponse(val publicKey: JsonWebKey)

/** `GET /messages/history` response shape -- every message (own + received) this account has ever sent/received, for ArchiveManager.restoreFromArchive to decrypt. */
@Serializable
data class MessagesHistoryResponse(val messages: List<MessageEnvelope> = emptyList())

// ----- WebSocket events: client -> server -----
@Serializable
sealed class ClientToServer {
    @Serializable
    @SerialName("message.send")
    data class MessageSend(val msg: MessageEnvelope) : ClientToServer()

    @Serializable
    @SerialName("receipt.delivered")
    data class ReceiptDelivered(val messageId: String, val to: String) : ClientToServer()

    @Serializable
    @SerialName("receipt.read")
    data class ReceiptRead(val messageId: String, val to: String) : ClientToServer()

    @Serializable
    @SerialName("typing.start")
    data class TypingStart(val to: String) : ClientToServer()

    @Serializable
    @SerialName("typing.stop")
    data class TypingStop(val to: String) : ClientToServer()

    @Serializable
    @SerialName("presence.subscribe")
    data class PresenceSubscribe(val user: String) : ClientToServer()

    @Serializable
    @SerialName("presence.unsubscribe")
    data class PresenceUnsubscribe(val user: String) : ClientToServer()

    @Serializable
    @SerialName("session.reset")
    data class SessionReset(val to: String) : ClientToServer()

    @Serializable
    @SerialName("ping")
    object Ping : ClientToServer()
}

// ----- WebSocket events: server -> client -----
@Serializable
sealed class ServerToClient {
    @Serializable
    @SerialName("message.new")
    data class MessageNew(val msg: MessageEnvelope) : ServerToClient()

    @Serializable
    @SerialName("message.ack")
    data class MessageAck(val messageId: String) : ServerToClient()

    @Serializable
    @SerialName("receipt.update")
    data class ReceiptUpdate(val messageId: String, val status: String, val from: String) : ServerToClient()

    @Serializable
    @SerialName("typing")
    data class Typing(val from: String, val state: String) : ServerToClient()

    @Serializable
    @SerialName("presence.update")
    data class PresenceUpdate(val user: String, val online: Boolean, val lastSeen: Long? = null) : ServerToClient()

    @Serializable
    @SerialName("session.reset")
    data class SessionReset(val from: String) : ServerToClient()

    // Serious Mode/union/milestone state changed on the thread with `from`
    // -- "Serious mode/Union/We met request not getting updated in real
    // time inside chat," reported directly. Carries no state itself (this
    // server has no concept of Serious Mode at all, see
    // d2m_core_engine's serious_mode_service.py) -- just a "go refetch"
    // nudge, same reasoning as messaging-framework/packages/protocol's
    // matching addition.
    @Serializable
    @SerialName("thread.updated")
    data class ThreadUpdated(val from: String) : ServerToClient()

    @Serializable
    @SerialName("error")
    data class Error(val message: String) : ServerToClient()

    @Serializable
    @SerialName("pong")
    object Pong : ServerToClient()
}

/** Attachment description -- see MediaMeta in messaging-framework/packages/protocol. `key`/`iv` only carry real meaning once a real CryptoProvider lands; the stub provider leaves them as placeholder values (see StubUnencryptedCryptoProvider). */
@Serializable
data class MediaMeta(
    val blobId: String,
    val key: String,
    val iv: String,
    val mime: String,
    val name: String,
    val size: Long,
    val kind: String, // "image" | "audio" | "video" | "file"
    val duration: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val thumb: String? = null,
)

@Serializable
data class ReplyContext(val id: String, val from: String, val preview: String)

/**
 * The PLAINTEXT payload carried inside an encrypted MessageEnvelope.body --
 * this is what CryptoProvider encrypts/decrypts.
 *
 * `@JsonClassDiscriminator("type")` overrides messagingProtocolJson's global
 * `classDiscriminator = "t"` (below) for JUST this hierarchy -- confirmed via
 * a real cross-platform decrypt (mobile decrypting a live message from
 * d2m_web) that the crypto interop is byte-for-byte correct, but the
 * resulting plaintext JSON came back as `{"type":"text","text":"hello"}`,
 * not `{"t":"text",...}`, and failed to deserialize with "Class
 * discriminator was missing". Per
 * messaging-framework/packages/protocol/src/index.ts, ChatPayload's wire
 * discriminator key really is `type` -- it's ClientToServer/ServerToClient
 * (the outer WS envelope, a separate polymorphic hierarchy) that uses `t`.
 * Without this override every real message in both directions silently
 * failed to decode after a *successful* decrypt, which looked from the UI
 * like "messages not sent/received" despite the WebSocket, presence, and
 * encryption all working correctly.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class ChatPayload {
    @Serializable
    @SerialName("text")
    data class Text(val text: String, val replyTo: ReplyContext? = null) : ChatPayload()

    @Serializable
    @SerialName("media")
    data class Media(val media: MediaMeta, val caption: String? = null, val replyTo: ReplyContext? = null) : ChatPayload()

    @Serializable
    @SerialName("edit")
    data class Edit(val targetId: String, val text: String) : ChatPayload()

    @Serializable
    @SerialName("delete")
    data class Delete(val targetId: String) : ChatPayload()

    @Serializable
    @SerialName("reaction")
    data class Reaction(val targetId: String, val emoji: String, val action: String) : ChatPayload()

    @Serializable
    @SerialName("call")
    data class Call(val call: CallSignal) : ChatPayload()
}

// ----- Call signaling -- rides the same E2E channel as chat, routed to
// CallManager rather than rendered as a message. Direct port of CallSignal
// in messaging-framework/packages/protocol/src/index.ts. -----

@Serializable
data class SdpDescription(val type: String, val sdp: String? = null) // "offer" | "answer" | "pranswer" | "rollback"

@Serializable
data class IceCandidateData(
    val candidate: String,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
    val usernameFragment: String? = null,
)

// msid (local MediaStream id) -> role, plus which roles are actively sending.
@Serializable
data class VideoMeta(val roles: Map<String, String> = emptyMap(), val sending: List<String> = emptyList())

/** Same discriminator-override reasoning as ChatPayload above -- CallSignal's wire discriminator key is `kind`, per messaging-framework/packages/protocol/src/index.ts, distinct from both ChatPayload's `type` and the outer envelope's `t`. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("kind")
sealed class CallSignal {
    abstract val callId: String

    @Serializable
    @SerialName("invite")
    data class Invite(
        override val callId: String,
        val media: String, // "audio" | "video"
        val sdp: SdpDescription,
        val video: VideoMeta? = null,
        val mic: Boolean? = null,
    ) : CallSignal()

    @Serializable
    @SerialName("ringing")
    data class Ringing(override val callId: String) : CallSignal()

    @Serializable
    @SerialName("accept")
    data class Accept(
        override val callId: String,
        val sdp: SdpDescription,
        val video: VideoMeta? = null,
        val mic: Boolean? = null,
    ) : CallSignal()

    @Serializable
    @SerialName("decline")
    data class Decline(override val callId: String, val reason: String? = null) : CallSignal() // "busy" | "declined"

    @Serializable
    @SerialName("ice")
    data class Ice(override val callId: String, val candidate: IceCandidateData) : CallSignal()

    @Serializable
    @SerialName("offer")
    data class Offer(
        override val callId: String,
        val sdp: SdpDescription,
        val note: String? = null, // "upgrade" | "screen-on" | "screen-off"
        val video: VideoMeta? = null,
        val mic: Boolean? = null,
    ) : CallSignal()

    @Serializable
    @SerialName("answer")
    data class Answer(
        override val callId: String,
        val sdp: SdpDescription,
        val video: VideoMeta? = null,
        val mic: Boolean? = null,
    ) : CallSignal()

    @Serializable
    @SerialName("media-state")
    data class MediaState(override val callId: String, val video: VideoMeta, val mic: Boolean? = null) : CallSignal()

    @Serializable
    @SerialName("hangup")
    data class Hangup(override val callId: String) : CallSignal()
}

val messagingProtocolJson = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "t"
    serializersModule = SerializersModule {
        polymorphic(ClientToServer::class) {
            subclass(ClientToServer.MessageSend::class)
            subclass(ClientToServer.ReceiptDelivered::class)
            subclass(ClientToServer.ReceiptRead::class)
            subclass(ClientToServer.TypingStart::class)
            subclass(ClientToServer.TypingStop::class)
            subclass(ClientToServer.PresenceSubscribe::class)
            subclass(ClientToServer.PresenceUnsubscribe::class)
            subclass(ClientToServer.SessionReset::class)
            subclass(ClientToServer.Ping::class)
        }
        polymorphic(ServerToClient::class) {
            subclass(ServerToClient.MessageNew::class)
            subclass(ServerToClient.MessageAck::class)
            subclass(ServerToClient.ReceiptUpdate::class)
            subclass(ServerToClient.Typing::class)
            subclass(ServerToClient.PresenceUpdate::class)
            subclass(ServerToClient.SessionReset::class)
            subclass(ServerToClient.Error::class)
            subclass(ServerToClient.Pong::class)
        }
        polymorphic(ChatPayload::class) {
            subclass(ChatPayload.Text::class)
            subclass(ChatPayload.Media::class)
            subclass(ChatPayload.Edit::class)
            subclass(ChatPayload.Delete::class)
            subclass(ChatPayload.Reaction::class)
            subclass(ChatPayload.Call::class)
        }
        polymorphic(CallSignal::class) {
            subclass(CallSignal.Invite::class)
            subclass(CallSignal.Ringing::class)
            subclass(CallSignal.Accept::class)
            subclass(CallSignal.Decline::class)
            subclass(CallSignal.Ice::class)
            subclass(CallSignal.Offer::class)
            subclass(CallSignal.Answer::class)
            subclass(CallSignal.MediaState::class)
            subclass(CallSignal.Hangup::class)
        }
    }
}
