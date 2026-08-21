package com.d2m.app.messaging.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.d2m.app.messaging.d2mIdToMessagingUsername
import com.d2m.app.messaging.protocol.ReplyContext
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

private val QUICK_REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

// ----- Date/time formatting for bubbles + day dividers -- matches d2m_web's
// MessageBubble.jsx (fmtBubbleTime) and MessageList.jsx (dayLabel/sameDay)
// exactly: "Today"/"Yesterday"/"Month Day"(+", Year" if not this year) for
// day pills, "h:mm AM/PM" inside each bubble. -----

private fun localDateOf(epochMillis: Long): LocalDate =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun sameDay(a: Long, b: Long): Boolean = localDateOf(a) == localDateOf(b)

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

private fun dayLabel(epochMillis: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
    if (sameDay(epochMillis, now)) return "Today"
    if (sameDay(epochMillis, now - 86_400_000L)) return "Yesterday"
    val date = localDateOf(epochMillis)
    val monthDay = "${MONTH_NAMES[date.monthNumber - 1]} ${date.dayOfMonth}"
    return if (date.year == localDateOf(now).year) monthDay else "$monthDay, ${date.year}"
}

private fun formatBubbleTime(epochMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    val hour12 = when (val h = dt.hour % 12) { 0 -> 12; else -> h }
    val amPm = if (dt.hour < 12) "AM" else "PM"
    return "$hour12:${dt.minute.toString().padStart(2, '0')} $amPm"
}

/** Consecutive messages grouped into one entry per calendar day -- mirrors d2m_web's MessageList.jsx groupByDay exactly (same reasoning: one real bounding box per day is what lets a sticky pill let go once its own day scrolls out, instead of every day's pill stacking against one shared unbounded list). */
private fun groupByDay(messages: List<ChatMessage>): List<Pair<String, List<ChatMessage>>> {
    val groups = mutableListOf<Pair<String, MutableList<ChatMessage>>>()
    for (m in messages) {
        val last = groups.lastOrNull()
        if (last != null && sameDay(last.second.last().sentAt, m.sentAt)) {
            last.second.add(m)
        } else {
            groups.add(m.id to mutableListOf(m))
        }
    }
    return groups
}

/**
 * Real-time conversation pane -- mirrors screens/child/MatchesScreen.jsx's
 * MessageList + MessageComposer + MessageBubble, now at full parity with
 * useMessaging.js's feature set: reply, edit, delete-for-me/everyone,
 * reactions, typing indicator, delivered/read ticks. Media (image
 * attachments) render if received but there's no attach button yet on
 * either platform -- see messaging/README.md.
 *
 * Deliberately owns NO header/toolbar of its own anymore (back button,
 * avatar, name, call buttons, more-menu all used to be split across this
 * composable's own toolbar row and a second row in MatchesScreen.kt above
 * it -- reported directly as wrong: "back button, user thumbnail, Name,
 * audio call button, video call button, more option all in one line -
 * that's how things are in all the apps"). The caller now owns the entire
 * header as ONE row and renders this pane underneath it -- see
 * MatchesScreen.kt's ConversationHeader.
 *
 * Runs today on the explicitly non-production StubUnencryptedCryptoProvider
 * (see messaging/crypto/CryptoProvider.kt) -- no banner here (that was
 * reported as unwanted clutter); the caller shows a lock icon next to the
 * peer's name only once `isProductionGradeEncryption` is actually true.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatPane(
    peerId: String,
    peerName: String,
    modifier: Modifier = Modifier,
) {
    val identityStore: IdentityStore = koinInject()
    val messagingRepo: MessagingRepository = koinInject()
    val apiClient = koinInject<com.d2m.app.data.network.ApiClient>()
    val scope = rememberCoroutineScope()

    val peerUsername = remember(peerId) { d2mIdToMessagingUsername(peerId) }
    val messages by messagingRepo.messagesFor(peerUsername).collectAsState()
    val myUsername = remember(messages) { messagingRepo.currentUsername() }

    var draft by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var menuForMessageId by remember { mutableStateOf<String?>(null) }
    var reactionPickerFor by remember { mutableStateOf<String?>(null) }
    var typingJob by remember { mutableStateOf<Job?>(null) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(peerId) {
        scope.launch { messagingRepo.start(apiClient.client) }
        messagingRepo.setActivePeer(peerId)
        messagingRepo.rememberPeerName(peerId, peerName)
        onDispose { messagingRepo.setActivePeer(null) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // Don Norman "visibility of system status": a send/edit/delete/reaction
    // that failed used to just vanish with zero feedback -- now it surfaces
    // as a transient snackbar (MessagingRepository.sendErrors), same pattern
    // the rest of the OS uses for "this action didn't go through."
    LaunchedEffect(Unit) {
        messagingRepo.sendErrors.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No messages yet -- say hi 👋", style = MaterialTheme.typography.bodyMedium, color = mutedText(0.45f))
                    }
                } else {
                    val dayGroups = remember(messages) { groupByDay(messages) }
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for ((groupKey, groupMessages) in dayGroups) {
                            // Matches d2m_web's sticky day pill exactly (MessageList.jsx)
                            // -- one real stickyHeader per day, so it lets go the moment
                            // that day's own messages scroll out rather than every day's
                            // pill stacking against one shared unbounded list.
                            stickyHeader(key = "day-$groupKey") {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(
                                        dayLabel(groupMessages.first().sentAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = mutedText(0.6f),
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(D2MRadius.pill))
                                            .padding(horizontal = 14.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            items(groupMessages, key = { it.id }) { m ->
                                MessageRow(
                                    message = m,
                                    myUsername = myUsername,
                                    menuOpen = menuForMessageId == m.id,
                                    reactionPickerOpen = reactionPickerFor == m.id,
                                    // Automatic fade-in on arrival + smooth reflow when a bubble
                                    // above/below it changes size (edit, reaction, status tick) --
                                    // the single highest-value animation for a chat list, and Compose
                                    // provides it for free once items are keyed (they already are).
                                    modifier = Modifier.animateItem(),
                                    onOpenMenu = { menuForMessageId = m.id },
                                    onCloseMenu = { menuForMessageId = null },
                                    onOpenReactionPicker = { reactionPickerFor = m.id; menuForMessageId = null },
                                    onCloseReactionPicker = { reactionPickerFor = null },
                                    onReact = { emoji -> scope.launch { messagingRepo.toggleReaction(peerId, m, emoji) }; reactionPickerFor = null },
                                    onReply = { replyingTo = m; editingId = null; menuForMessageId = null },
                                    onEdit = { editingId = m.id; draft = m.text; replyingTo = null; menuForMessageId = null },
                                    onDeleteForMe = { messagingRepo.deleteForMe(peerId, m.id); menuForMessageId = null },
                                    onDeleteForEveryone = { scope.launch { messagingRepo.deleteForEveryone(peerId, m.id) }; menuForMessageId = null },
                                    onRetry = { scope.launch { messagingRepo.retryFailedMessage(peerId, m.id) } },
                                )
                            }
                        }
                    }
                }
                SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)) { data ->
                    Snackbar(snackbarData = data, containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            // AnimatedVisibility (expand/fade) instead of a plain `if` -- the
            // reply/edit context bar now slides in and out instead of
            // popping abruptly, and its appearance/disappearance itself IS
            // the feedback that "reply mode" was entered or left.
            AnimatedVisibility(
                visible = replyingTo != null || editingId != null,
                enter = expandVertically(tween(180)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(150)) + fadeOut(tween(120)),
            ) {
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
                // Animates instead of snapping so the button visibly "wakes up"
                // the moment there's something to send -- a small but real
                // affordance cue (Don Norman: the control's own appearance
                // should signal whether the action is currently available).
                val sendBg by animateColorAsState(if (canSend) MaterialTheme.colorScheme.primary else mutedText(0.12f), tween(150))
                Box(
                    modifier = Modifier.size(44.dp)
                        .clip(CircleShape)
                        .background(sendBg)
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
}

/**
 * Sent/delivered/read ticks -- WhatsApp's single-check/double-check/blue-
 * double-check convention, matching d2m_web's MessageBubble.jsx Ticks
 * exactly (down to reusing the theme's primary color for "read" rather than
 * a hardcoded blue, so it stays correct across all of this app's color
 * flows and in dark mode). No tick at all while SENDING (nothing to confirm
 * yet); FAILED is handled by the caller before this is ever reached.
 */
@Composable
private fun MessageTicks(status: MessageStatus, mine: Boolean) {
    if (status == MessageStatus.SENDING || status == MessageStatus.FAILED) return
    val color = if (status == MessageStatus.READ) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = if (mine) 0.75f else 0.5f)
    Icon(
        if (status == MessageStatus.SENT) Icons.Filled.Check else Icons.Filled.DoneAll,
        contentDescription = when (status) {
            MessageStatus.SENT -> "Sent"
            MessageStatus.DELIVERED -> "Delivered"
            MessageStatus.READ -> "Read"
            else -> null
        },
        tint = color,
        modifier = Modifier.size(13.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    message: ChatMessage,
    myUsername: String?,
    menuOpen: Boolean,
    reactionPickerOpen: Boolean,
    modifier: Modifier = Modifier,
    onOpenMenu: () -> Unit,
    onCloseMenu: () -> Unit,
    onOpenReactionPicker: () -> Unit,
    onCloseReactionPicker: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start) {
            Box {
                // Bubble shape + fill matches d2m_web's MessageBubble.jsx exactly:
                // mine = solid accent fill, "tail" corner bottom-right (sharp 4dp
                // vs 16dp everywhere else); theirs = card surface + a subtle
                // border, tail bottom-left. Text/icon color inside is provided via
                // LocalContentColor below so every unstyled Text/Icon in this
                // bubble (message text, reply quote, call-log label, timestamp,
                // ticks) automatically contrasts against whichever fill is
                // actually behind it, instead of each needing its own color logic.
                val bubbleShape = RoundedCornerShape(
                    topStart = D2MRadius.md,
                    topEnd = D2MRadius.md,
                    bottomEnd = if (message.isMine) 4.dp else D2MRadius.md,
                    bottomStart = if (message.isMine) D2MRadius.md else 4.dp,
                )
                val bubbleContentColor = if (message.isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .background(if (message.isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, bubbleShape)
                        .then(if (message.isMine) Modifier else Modifier.border(1.dp, mutedText(0.15f), bubbleShape))
                        // Tap now also does something: FAILED messages retry
                        // on tap (Don Norman "error recovery" -- a stuck
                        // "Failed" label with no action was a dead end before).
                        .combinedClickable(onClick = { if (message.status == MessageStatus.FAILED) onRetry() }, onLongClick = { if (!message.deleted) onOpenMenu() })
                        .padding(10.dp),
                ) {
                    CompositionLocalProvider(LocalContentColor provides bubbleContentColor) {
                        Column {
                            if (message.replyTo != null) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .background(
                                            if (message.isMine) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                            RoundedCornerShape(D2MRadius.sm),
                                        )
                                        .padding(6.dp),
                                ) {
                                    Text(message.replyTo.from, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Text(message.replyTo.preview, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                                }
                                Spacer(Modifier.padding(top = 4.dp))
                            }

                            when {
                                message.deleted -> Text("This message was deleted", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = LocalContentColor.current.copy(alpha = 0.7f))
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
                                        Text(message.text, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }

                            if (!message.deleted) {
                                // Timestamp (+ "edited" + read ticks for my own
                                // messages) sits INSIDE the bubble, bottom-right --
                                // matches d2m_web's MessageBubble.jsx layout exactly,
                                // rather than the timestamp living outside/below the
                                // bubble as a separate element.
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (message.status == MessageStatus.FAILED) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("Failed -- tap to retry", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    } else {
                                        if (message.edited) {
                                            Text("edited", style = MaterialTheme.typography.labelSmall, fontStyle = FontStyle.Italic, color = LocalContentColor.current.copy(alpha = 0.65f))
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        Text(formatBubbleTime(message.sentAt), style = MaterialTheme.typography.labelSmall, color = LocalContentColor.current.copy(alpha = 0.7f))
                                        if (message.isMine) {
                                            Spacer(Modifier.width(4.dp))
                                            MessageTicks(status = message.status, mine = message.isMine)
                                        }
                                    }
                                }
                            }
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
