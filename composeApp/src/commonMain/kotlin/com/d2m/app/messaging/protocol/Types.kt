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
 * types directly. Call-signaling types (CallSignal etc.) are ported for
 * shape-completeness but nothing in this build acts on them yet -- see
 * this package's README.md for what's real vs. scaffolded.
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

/** The PLAINTEXT payload carried inside an encrypted MessageEnvelope.body -- this is what CryptoProvider encrypts/decrypts. */
@Serializable
sealed class ChatPayload {
    @Serializable
    @SerialName("text")
    data class Text(val text: String, val replyToId: String? = null) : ChatPayload()

    @Serializable
    @SerialName("edit")
    data class Edit(val targetId: String, val text: String) : ChatPayload()

    @Serializable
    @SerialName("delete")
    data class Delete(val targetId: String) : ChatPayload()

    @Serializable
    @SerialName("reaction")
    data class Reaction(val targetId: String, val emoji: String, val action: String) : ChatPayload()
    // "media"/"call" payloads from the source protocol are intentionally not
    // ported yet -- media (Phase 2 of the integration plan) and calls
    // (Phase 3) both ride on top of this same envelope once text messaging
    // is proven out; adding them here now would be dead code with nothing
    // to exercise it.
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
            subclass(ChatPayload.Edit::class)
            subclass(ChatPayload.Delete::class)
            subclass(ChatPayload.Reaction::class)
        }
    }
}
