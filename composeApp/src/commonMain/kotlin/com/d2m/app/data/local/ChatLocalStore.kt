package com.d2m.app.data.local

import app.cash.sqldelight.db.SqlDriver
import com.d2m.app.messaging.CallLogInfo
import com.d2m.app.messaging.ChatMessage
import com.d2m.app.messaging.MessageStatus
import com.d2m.app.messaging.protocol.MediaMeta
import com.d2m.app.messaging.protocol.ReplyContext
import com.d2m.app.messaging.protocol.messagingProtocolJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * Local persistent chat cache -- see ChatDatabase.sq's doc comment for the
 * full "why" (tasks #56/#57, two directly-reported bugs: call-log bubbles
 * vanishing on restart, and "chats once retrieved should be stored locally").
 *
 * Wraps the generated [D2MDatabase] with a Kotlin-native API operating
 * directly on [ChatMessage] -- MessagingRepository.kt (the only caller)
 * shouldn't need to know this is backed by SQLite/SQLDelight at all. Nested
 * fields (media, replyTo, reactions, callLog) are stored as JSON text
 * columns rather than a normalized column-per-field or a hand-written
 * SQLDelight adapter per column -- with kotlinx.serialization already a
 * project dependency and these types already @Serializable (they're the
 * exact same classes the wire protocol serializes, see protocol/Types.kt),
 * this is far less code than adapters for four different nested shapes
 * would be, at the cost of not being able to SQL-query inside them -- fine,
 * nothing here ever needs to (every real query is by id or ownerUsername).
 *
 * Every method does real disk I/O (SQLite) -- all suspend, hopped onto
 * Dispatchers.Default (SQLDelight's driver calls are synchronous/blocking
 * per call, not natively suspending) so a caller on the main/UI dispatcher
 * never blocks a frame on a write. MessagingRepository fires persistence
 * calls on its own background `scope` (fire-and-forget from the caller's
 * perspective -- a message is already live in the in-memory StateFlow, and
 * therefore already visible/sendable, before its disk write finishes;
 * durability is a best-effort side effect, not something any UI action
 * blocks on).
 */
class ChatLocalStore(driver: SqlDriver) {
    private val db = D2MDatabase(driver)
    private val queries = db.chatDatabaseQueries

    suspend fun loadAll(owner: String): Map<String, List<ChatMessage>> = withContext(Dispatchers.Default) {
        queries.selectByOwner(owner).executeAsList()
            .map { row ->
                val message = row.toChatMessage()
                // A row that's still SENDING on disk means the process died
                // mid-send (a real ack always upserts a SENT/FAILED status
                // before this device would ever restart) -- surfacing it as
                // FAILED instead of a permanently-stuck spinner gives the
                // person a retry action (ChatPane's retryFailedMessage)
                // rather than false confidence that a send is still in
                // flight when nothing is actually sending anymore.
                val normalized = if (message.status == MessageStatus.SENDING) message.copy(status = MessageStatus.FAILED) else message
                row.peerUsername to normalized
            }
            .groupBy({ it.first }, { it.second })
    }

    suspend fun maxSentAt(owner: String): Long? = withContext(Dispatchers.Default) {
        queries.maxSentAtForOwner(owner).executeAsOneOrNull()?.maxSentAt
    }

    suspend fun upsert(owner: String, peerUsername: String, message: ChatMessage) = withContext(Dispatchers.Default) {
        queries.upsert(
            id = message.id,
            ownerUsername = owner,
            peerUsername = peerUsername,
            fromUsername = message.fromUsername,
            toUsername = message.toUsername,
            text = message.text,
            sentAt = message.sentAt,
            isMine = if (message.isMine) 1L else 0L,
            status = message.status.name,
            reactionsJson = messagingProtocolJson.encodeToString(message.reactions),
            replyToJson = message.replyTo?.let { messagingProtocolJson.encodeToString(it) },
            edited = if (message.edited) 1L else 0L,
            deleted = if (message.deleted) 1L else 0L,
            mediaJson = message.media?.let { messagingProtocolJson.encodeToString(it) },
            mediaUrl = message.mediaUrl,
            callLogJson = message.callLog?.let { messagingProtocolJson.encodeToString(it) },
        )
    }

    suspend fun delete(owner: String, messageId: String) = withContext(Dispatchers.Default) {
        queries.deleteById(owner, messageId)
    }

    /** Called if a peer/account context needs a full local wipe -- not currently wired to any UI action, kept for symmetry/future logout-cleanup use. */
    suspend fun clear(owner: String) = withContext(Dispatchers.Default) {
        queries.clearForOwner(owner)
    }

    private fun ChatMessageEntity.toChatMessage(): ChatMessage = ChatMessage(
        id = id,
        fromUsername = fromUsername,
        toUsername = toUsername,
        text = text,
        sentAt = sentAt,
        isMine = isMine != 0L,
        status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.SENT),
        reactions = runCatching { messagingProtocolJson.decodeFromString<Map<String, Set<String>>>(reactionsJson) }.getOrDefault(emptyMap()),
        replyTo = replyToJson?.let { runCatching { messagingProtocolJson.decodeFromString<ReplyContext>(it) }.getOrNull() },
        edited = edited != 0L,
        deleted = deleted != 0L,
        media = mediaJson?.let { runCatching { messagingProtocolJson.decodeFromString<MediaMeta>(it) }.getOrNull() },
        mediaUrl = mediaUrl,
        callLog = callLogJson?.let { runCatching { messagingProtocolJson.decodeFromString<CallLogInfo>(it) }.getOrNull() },
    )
}
