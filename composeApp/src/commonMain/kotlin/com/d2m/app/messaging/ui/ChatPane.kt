package com.d2m.app.messaging.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.messaging.ChatMessage
import com.d2m.app.messaging.MessageStatus
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.call.CallManager
import com.d2m.app.messaging.d2mIdToMessagingUsername
import com.d2m.app.messaging.protocol.ReplyContext
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val QUICK_REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

/**
 * Real-time conversation pane -- mirrors screens/child/MatchesScreen.jsx's
 * MessageList + MessageComposer + MessageBubble, now at full parity with
 * useMessaging.js's feature set: reply, edit, delete-for-me/everyone,
 * reactions, typing indicator, delivered/read ticks, and audio/video call
 * buttons wired to call/CallManager.kt. Media (image attachments) render if
 * received but there's no attach button yet on either platform -- see
 * messaging/README.md. Accepts an optional `trailingActions` slot so a
 * caller (MatchesScreen.kt) can add its own icon buttons into the same
 * toolbar row as the call buttons, rather than rendering a second row above
 * this one -- see that screen's "more options" menu for the intended use.
 *
 * Runs today on the explicitly non-production StubUnencryptedCryptoProvider
 * (see messaging/crypto/CryptoProvider.kt) -- no banner here anymore (that
 * was reported as unwanted clutter); the caller is expected to show a lock
 * icon next to the peer's name only once `isProductionGradeEncryption` is
 * actually true, same as MatchesScreen.kt's ThreadHeader now does.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatPane(
    peerId: String,
    peerName: String,
    modifier: Modifier = Modifier,
    trailingActions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    val identityStore: IdentityStore = koinInject()
    val messagingRepo: MessagingRepository = koinInject()
    val callManager: CallManager = koinInject()
    val apiClient = koinInject<com.d2m.app.data.network.ApiClient>()
    val scope = rememberCoroutineScope()

    val peerUsername = remember(peerId) { d2mIdToMessagingUsername(peerId) }
    val messages by messagingRepo.messagesFor(peerUsername).collectAsState()
    val peerOnline by messagingRepo.isPeerOnline(peerUsername).collectAsState()
    val peerTyping by messagingRepo.isPeerTyping(peerUsername).collectAsState()
    val myUsername = remember(messages) { messagingRepo.currentUsername() }

    var draft by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var menuForMessageId by remember { mutableStateOf<String?>(null) }
    var reactionPickerFor by remember { mutableStateOf<String?>(null) }
    var typingJob by remember { mutableStateOf<Job?>(null) }
    val listState = rememberLazyListState()

    DisposableEffect(peerId) {
        scope.launch { messagingRepo.start(apiClient.client) }
        messagingRepo.setActivePeer(peerId)
        messagingRepo.rememberPeerName(peerId, peerName)
        onDispose { messagingRepo.setActivePeer(null) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = modifier) {
        // Toolbar -- one line: online/typing status on the left, then every
        // action (audio call, video call, and whatever the caller passes
        // via `trailingActions`, e.g. MatchesScreen's "more options" menu)
        // together on the right as outlined circular buttons, all in the
        // same row (previously call buttons and "more" lived in two
        // separate rows -- reported directly, fixed here). No encryption
        // banner text anymore -- see the lock icon next to the peer's name
        // in MatchesScreen.kt's ThreadHeader instead, which only appears
        // once `isProductionGradeEncryption` is actually true.
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val status = when {
                peerTyping -> "$peerName is typing…"
                peerOnline -> "Online"
                else -> ""
            }
            Text(status, style = MaterialTheme.typography.labelMedium, color = mutedText(0.55f), modifier = Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolbarIconButton(icon = Icons.Filled.Call, contentDescription = "Audio call") {
                    scope.launch { callManager.startCall(peerUsername, "audio") }
                }
                ToolbarIconButton(icon = Icons.Filled.Videocam, contentDescription = "Video call") {
                    scope.launch { callManager.startCall(peerUsername, "video") }
                }
                trailingActions()
            }
        }
        HorizontalDivider(color = mutedText(0.08f))

        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No messages yet -- say hi 👋", style = MaterialTheme.typography.bodyMedium, color = mutedText(0.45f))
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(messages, key = { it.id }) { m ->
                        MessageRow(
                            message = m,
                            myUsername = myUsername,
                            menuOpen = menuForMessageId == m.id,
                            reactionPickerOpen = reactionPickerFor == m.id,
                            onOpenMenu = { menuForMessageId = m.id },
                            onCloseMenu = { menuForMessageId = null },
                            onOpenReactionPicker = { reactionPickerFor = m.id; menuForMessageId = null },
                            onCloseReactionPicker = { reactionPickerFor = null },
                            onReact = { emoji -> scope.launch { messagingRepo.toggleReaction(peerId, m, emoji) }; reactionPickerFor = null },
                            onReply = { replyingTo = m; editingId = null; menuForMessageId = null },
                            onEdit = { editingId = m.id; draft = m.text; replyingTo = null; menuForMessageId = null },
                            onDeleteForMe = { messagingRepo.deleteForMe(peerId, m.id); menuForMessageId = null },
                            onDeleteForEveryone = { scope.launch { messagingRepo.deleteForEveryone(peerId, m.id) }; menuForMessageId = null },
                        )
                    }
                }
            }
        }

        if (replyingTo != null || editingId != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    .background(mutedText(0.06f), RoundedCornerShape(D2MRadius.sm)).padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    if (editingId != null) "Editing message" else "Replying to ${replyingTo?.let { if (it.isMine) "yourself" else peerName }}: ${replyingTo?.text.orEmpty().take(60)}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { replyingTo = null; editingId = null; draft = "" }, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { new ->
                    val wasEmpty = draft.isEmpty()
                    draft = new
                    if (editingId == null) {
                        if (new.isNotEmpty() && wasEmpty) scope.launch { messagingRepo.sendTyping(peerId, true) }
                        typingJob?.cancel()
                        typingJob = scope.launch {
                            delay(2_000)
                            messagingRepo.sendTyping(peerId, false)
                        }
                        if (new.isEmpty()) scope.launch { messagingRepo.sendTyping(peerId, false) }
                    }
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message…", color = mutedText(0.4f)) },
                shape = RoundedCornerShape(D2MRadius.pill),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = mutedText(0.15f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = mutedText(0.03f),
                    focusedContainerColor = mutedText(0.03f),
                ),
            )
            val canSend = draft.isNotBlank()
            Box(
                modifier = Modifier.size(44.dp)
                    .clip(CircleShape)
                    .background(if (canSend) MaterialTheme.colorScheme.primary else mutedText(0.12f))
                    .combinedClickable(enabled = canSend, onClick = {
                        val text = draft
                        draft = ""
                        val editing = editingId
                        val reply = replyingTo
                        editingId = null
                        replyingTo = null
                        scope.launch {
                            messagingRepo.sendTyping(peerId, false)
                            when {
                                editing != null -> messagingRepo.editMessage(peerId, editing, text)
                                else -> messagingRepo.sendText(peerId, text, reply?.let { ReplyContext(it.id, it.fromUsername, it.text.take(80)) })
                            }
                        }
                    }, onLongClick = {}),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (editingId != null) Icons.Filled.Check else Icons.Filled.Send,
                    contentDescription = if (editingId != null) "Save" else "Send",
                    tint = if (canSend) Color.White else mutedText(0.4f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolbarIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(38.dp)
            .clip(CircleShape)
            .border(BorderStroke(1.dp, mutedText(0.15f)), CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(17.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    message: ChatMessage,
    myUsername: String?,
    menuOpen: Boolean,
    reactionPickerOpen: Boolean,
    onOpenMenu: () -> Unit,
    onCloseMenu: () -> Unit,
    onOpenReactionPicker: () -> Unit,
    onCloseReactionPicker: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start) {
            Box {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .background(
                            if (message.isMine) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else mutedText(0.08f),
                            RoundedCornerShape(D2MRadius.md),
                        )
                        .combinedClickable(onClick = {}, onLongClick = { if (!message.deleted) onOpenMenu() })
                        .padding(10.dp),
                ) {
                    Column {
                        if (message.replyTo != null) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .background(mutedText(0.08f), RoundedCornerShape(D2MRadius.sm))
                                    .padding(6.dp),
                            ) {
                                Text(message.replyTo.from, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(message.replyTo.preview, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                            }
                            Spacer(Modifier.padding(top = 4.dp))
                        }

                        when {
                            message.deleted -> Text("This message was deleted", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = mutedText(0.55f))
                            message.callLog != null -> {
                                val info = message.callLog
                                val label = when (info.reason) {
                                    "ended" -> "${if (info.media == "video") "Video" else "Voice"} call · ${info.durationSec / 60}:${(info.durationSec % 60).toString().padStart(2, '0')}"
                                    "busy", "failed" -> "Call failed"
                                    "declined" -> "Call declined"
                                    else -> if (info.incoming) "Missed call" else "No answer"
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(if (info.media == "video") Icons.Filled.Videocam else Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            else -> {
                                if (message.media != null && message.mediaUrl != null) {
                                    AsyncImage(
                                        model = message.mediaUrl,
                                        contentDescription = message.media.name,
                                        modifier = Modifier.widthIn(max = 240.dp).clip(RoundedCornerShape(D2MRadius.sm)),
                                    )
                                }
                                if (message.uploading) {
                                    Column(modifier = Modifier.width(160.dp).padding(top = 4.dp)) {
                                        Text("Uploading…", style = MaterialTheme.typography.labelSmall)
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                                if (message.uploadFailed) Text("Attachment failed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                if (message.text.isNotBlank()) {
                                    Text(message.text + if (message.edited) "  (edited)" else "", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        if (message.isMine && !message.deleted) {
                            Text(
                                when (message.status) {
                                    MessageStatus.SENDING -> "Sending…"
                                    MessageStatus.SENT -> "Sent"
                                    MessageStatus.DELIVERED -> "Delivered"
                                    MessageStatus.READ -> "Read"
                                    MessageStatus.FAILED -> "Failed"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (message.status == MessageStatus.READ) MaterialTheme.colorScheme.primary else mutedText(0.5f),
                            )
                        }
                    }
                }

                DropdownMenu(expanded = menuOpen, onDismissRequest = onCloseMenu) {
                    DropdownMenuItem(text = { Text("React") }, onClick = onOpenReactionPicker)
                    DropdownMenuItem(text = { Text("Reply") }, onClick = onReply)
                    if (message.isMine && message.media == null) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = onEdit)
                    }
                    DropdownMenuItem(text = { Text("Delete for me") }, onClick = onDeleteForMe)
                    if (message.isMine) {
                        DropdownMenuItem(text = { Text("Delete for everyone") }, onClick = onDeleteForEveryone)
                    }
                }

                DropdownMenu(expanded = reactionPickerOpen, onDismissRequest = onCloseReactionPicker) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                        QUICK_REACTIONS.forEach { emoji ->
                            Text(
                                emoji,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(4.dp).combinedClickable(onClick = { onReact(emoji) }),
                            )
                        }
                    }
                }
            }
        }

        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    message.reactions.forEach { (emoji, users) ->
                        val mine = myUsername != null && users.contains(myUsername)
                        Box(
                            modifier = Modifier
                                .background(if (mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else mutedText(0.08f), RoundedCornerShape(D2MRadius.sm))
                                .combinedClickable(onClick = { onReact(emoji) })
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text("$emoji ${users.size}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
