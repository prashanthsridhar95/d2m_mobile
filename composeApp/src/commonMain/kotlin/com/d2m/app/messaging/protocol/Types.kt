package com.d2m.app.messaging.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
 * signaling). `archive`/`callInvite` on MessageEnvelope are ported for wire
 * shape-completeness (the server keys push-notification and archival
 * behavior off them) but this build never populates `archive` itself --
 * there is no real Archive Keypair layer here, matching the crypto stub.
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
)

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

/** The PLAINTEXT payload carried inside an encrypted MessageEnvelope.body -- this is what CryptoProvider encrypts/decrypts. */
@Serializable
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

@Serializable
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
