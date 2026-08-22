package com.d2m.app.messaging.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.d2m.app.AppForegroundState
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.messaging.ChatMessage
import com.d2m.app.messaging.MessageStatus
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.SoundEffects
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
import kotlin.math.cos
import kotlin.math.roundToInt

private const val TWO_PI = 6.283185307179586

private val QUICK_REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

// "read double tick is not properly visible in the green bubble" -- the
// theme's LocalD2MStatusPalette.info.fg (a fairly dark navy, #1A56B0 light /
// a desaturated light blue #8AB2F5 dark) is tuned for text/badges on a
// neutral surface, not for reading as a small icon against a saturated
// accent-filled bubble -- against the Child flow's teal/green
// (D2MAccents.ChildLight/ChildDark, see Color.kt) it still doesn't pop.
// This is WhatsApp's own actual read-tick blue (#34B7F1-family), a
// purpose-built bright cyan that's famously legible against exactly this
// kind of saturated green -- used directly here instead of a theme token so
// it stays visible regardless of which flow's accent the bubble is filled
// with, mine-bubble color needing to differ, or light/dark mode.
private val ReadTickColor = Color(0xFF4FC3F7)

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
    // "typing notification is different in web & mobile - i want like web."
    // The header's "typing…" subtitle (ConversationHeader in MatchesScreen.kt/
    // ParentMessagesScreen.kt) already matched web -- what was missing is
    // web's OTHER typing indicator: an animated three-dot bubble inside the
    // message list itself (MessageList.jsx renders `{typing && <TypingIndicator />}`
    // as the last list item), which mobile never had at all. Read directly
    // here rather than threaded in as a prop from both callers, since
    // ChatPane already has everything (messagingRepo, peerUsername) needed.
    val peerTyping by messagingRepo.isPeerTyping(peerUsername).collectAsState()

    var draft by remember { mutableStateOf(TextFieldValue("")) }
    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var menuForMessageId by remember { mutableStateOf<String?>(null) }
    var typingJob by remember { mutableStateOf<Job?>(null) }
    var emojiOpen by remember { mutableStateOf(false) }
    var attaching by remember { mutableStateOf(false) }
    // "provide a image & video viewer - shouldnt be going outside the app" --
    // tapping a media bubble sets this; MediaViewerDialog renders full-screen
    // over the whole pane whenever it's non-null (see the outer Box below).
    var viewerTarget by remember { mutableStateOf<MediaViewerTarget?>(null) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    // "When reply is pressed, focus should go to the chat box & keyboard
    // should open up" -- tapping Reply (in the long-press menu or a swipe)
    // only ever set replyingTo before; nothing moved focus to the composer,
    // so the reply-quote preview appeared above it but the keyboard stayed
    // wherever it already was (usually closed).
    val composerFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(replyingTo) {
        if (replyingTo != null) {
            composerFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    // "edit should also open the keyboard. also the cursor should be at the
    // end of the text" -- onEdit (below) already sets draft to a
    // TextFieldValue with its selection at the end of the pre-filled text;
    // this mirrors the replyingTo effect above so tapping Edit focuses the
    // composer + opens the keyboard the same way Reply already does, rather
    // than only updating the text and leaving focus wherever it was.
    LaunchedEffect(editingId) {
        if (editingId != null) {
            composerFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    fun replyContext() = replyingTo?.let { ReplyContext(it.id, it.fromUsername, it.text.take(80)) }

    // Attach (paperclip button below) -- any file type, matches web's plain
    // <input type="file"> with no `accept` restriction. Reported directly:
    // "I want to be able to attach media, send emojis, send gifs" --
    // MessagingRepository.sendMedia already existed and was fully wired
    // (used internally by the archive-restore path) but had ZERO UI call
    // sites; this is the first one.
    val mediaLauncher = rememberMediaAttachLauncher { picked ->
        attaching = true
        val reply = replyContext()
        scope.launch {
            runCatching { messagingRepo.sendMedia(peerId, picked.bytes, picked.fileName, picked.mime, picked.kind, replyTo = reply) }
            replyingTo = null
            attaching = false
        }
    }

    DisposableEffect(peerId) {
        scope.launch { messagingRepo.start(apiClient.client) }
        messagingRepo.setActivePeer(peerId)
        messagingRepo.rememberPeerName(peerId, peerName)
        onDispose { messagingRepo.setActivePeer(null) }
    }

    // "everytime i open the chat, there is a scroll to end that happens. i
    // shouldnt be seeing it. it should be opening with last message being
    // shown." The list always started at index 0 (top) and this effect used
    // an ANIMATED scroll down to the last item on every trigger, including
    // the very first composition -- so opening a chat visibly played a
    // scroll-down animation instead of simply already being at the bottom.
    // hasScrolledInitially (per peer) draws the line: the first time
    // messages appear for this conversation, jump there instantly (no
    // visible motion); only NEW messages arriving afterwards -- while the
    // person is actually looking at the conversation -- get the smooth
    // animated scroll. Keyed on peerTyping too (matches web's identical
    // effect dependency list) -- so the view also scrolls down when the
    // typing bubble itself appears, not just when a real message lands.
    var hasScrolledInitially by remember(peerId) { mutableStateOf(false) }
    LaunchedEffect(messages.size, peerTyping) {
        if (messages.isEmpty()) return@LaunchedEffect
        if (!hasScrolledInitially) {
            listState.scrollToItem(messages.size - 1)
            hasScrolledInitially = true
        } else {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // "opening & closing the keyboard moves the chat a bit - it should stay
    // as is. i should be able to see the exact offset before opening & after."
    // The message list already shrinks when the keyboard opens (imePadding()
    // is applied by the caller, MatchesScreen.kt/ParentMessagesScreen.kt),
    // which can leave the last message sitting wherever the shrunk viewport
    // left it (sometimes hidden behind the keyboard) -- this re-pins it to
    // the bottom. It used to do that with an ANIMATED scroll, which is
    // exactly the visible "moves a bit" motion being reported here -- a
    // plain instant scrollToItem re-snaps to the same relative position
    // (last message at the bottom) without any perceptible scrolling
    // gesture, reading as "it just stayed there" instead of "it jumped."
    // WindowInsets.Companion.isImeVisible isn't resolvable in this KMP
    // Foundation artifact (Android-only in some Compose Multiplatform
    // versions) -- reading WindowInsets.ime's own bottom inset directly is
    // the more fundamental, universally-available API imePadding() itself
    // is already built on (see this project's existing imePadding() calls),
    // and >0 means the same thing isImeVisible would have.
    val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
    val imeVisible = imeBottomPx > 0
    LaunchedEffect(imeVisible) {
        if (imeVisible && messages.isNotEmpty()) listState.scrollToItem(messages.size - 1)
    }

    // Don Norman "visibility of system status": a send/edit/delete/reaction
    // that failed used to just vanish with zero feedback -- now it surfaces
    // as a transient snackbar (MessagingRepository.sendErrors), same pattern
    // the rest of the OS uses for "this action didn't go through."
    LaunchedEffect(Unit) {
        messagingRepo.sendErrors.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    // "Why notification sounds are not working like how it's working on
    // web?" -- web's playInChatTone() (useMessaging.js's else-branch, see
    // lib/sound.js): a message arriving in the exact conversation you're
    // already looking at gets its own distinct chime, separate from the
    // other-chat notification tone (InAppNotificationLayer.kt). Filtered to
    // this peer since MessagingRepository.inChatMessageEvents is shared
    // across every open ChatPane instance's peer. Gated on isForeground for
    // the same reason as InAppNotificationLayer's collector -- this
    // LaunchedEffect keeps running even if the app is backgrounded mid-chat
    // (activePeer isn't cleared on backgrounding, only on leaving the
    // screen), and a silent-to-nobody tone playing in the background would
    // be a new, worse bug, not a fix.
    LaunchedEffect(peerUsername) {
        messagingRepo.inChatMessageEvents.collect { from ->
            if (from == peerUsername && AppForegroundState.isForeground.value) SoundEffects.playInChatTone()
        }
    }

    Box(modifier = modifier) {
        MediaViewerDialog(target = viewerTarget, onDismiss = { viewerTarget = null })
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty() && !peerTyping) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No messages yet -- say hi 👋", style = MaterialTheme.typography.bodyMedium, color = mutedText(0.45f))
                    }
                } else if (messages.isEmpty()) {
                    // No real messages yet, but the peer is already typing --
                    // matches web's `{sortedMessages.length === 0 && !peerTyping && (...)}`
                    // gate: the empty-state copy hides in favor of just the
                    // typing bubble, rather than showing both at once.
                    Box(modifier = Modifier.fillMaxSize()) {
                        TypingIndicatorBubble(modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                } else {
                    val dayGroups = remember(messages) { groupByDay(messages) }
                    // 6.dp, not 4 -- matches d2m_web's MessageList.jsx exactly
                    // (`.d2m-msg-row { marginBottom: 6 }`), part of the same
                    // "make the messaging screen match web" pass as the
                    // bubble-width fix above.
                    // "vertical space between bubbles can be a bit more" --
                    // bumped from web's own 6.dp match up to 12.dp, a
                    // deliberate mobile-only divergence per that direct ask
                    // rather than a parity fix.
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    peerDisplayName = peerName,
                                    menuOpen = menuForMessageId == m.id,
                                    // Automatic fade-in on arrival + smooth reflow when a bubble
                                    // above/below it changes size (edit, reaction, status tick) --
                                    // the single highest-value animation for a chat list, and Compose
                                    // provides it for free once items are keyed (they already are).
                                    modifier = Modifier.animateItem(),
                                    onOpenMenu = { menuForMessageId = m.id },
                                    onCloseMenu = { menuForMessageId = null },
                                    onReact = { emoji -> scope.launch { messagingRepo.toggleReaction(peerId, m, emoji) }; menuForMessageId = null },
                                    onReply = { replyingTo = m; editingId = null; menuForMessageId = null },
                                    // Cursor placed at the end of the pre-filled text (TextRange with
                                    // a single offset = a collapsed cursor there, not a selection) --
                                    // "the cursor should be at the end of the text."
                                    onEdit = { editingId = m.id; draft = TextFieldValue(text = m.text, selection = TextRange(m.text.length)); replyingTo = null; menuForMessageId = null },
                                    onDeleteForMe = { messagingRepo.deleteForMe(peerId, m.id); menuForMessageId = null },
                                    onDeleteForEveryone = { scope.launch { messagingRepo.deleteForEveryone(peerId, m.id) }; menuForMessageId = null },
                                    onRetry = { scope.launch { messagingRepo.retryFailedMessage(peerId, m.id) } },
                                    onOpenMedia = { viewerTarget = it },
                                )
                            }
                        }
                        if (peerTyping) {
                            item(key = "typing-indicator") {
                                TypingIndicatorBubble(modifier = Modifier.animateItem())
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
                    IconButton(onClick = { replyingTo = null; editingId = null; draft = TextFieldValue("") }, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Emoji/Stickers/GIFs panel -- grows the composer area upward
            // (message list shrinks to make room, since it's the only
            // weight(1f) element in this Column) rather than a true CSS-style
            // absolute overlay, which Compose has no direct equivalent for
            // without extra offset math -- simpler and just as usable.
            AnimatedVisibility(
                visible = emojiOpen && editingId == null,
                enter = expandVertically(tween(180)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(150)) + fadeOut(tween(120)),
            ) {
                EmojiGifPanel(
                    client = apiClient.client,
                    onPickEmoji = { emoji ->
                        val newText = draft.text + emoji
                        draft = TextFieldValue(text = newText, selection = TextRange(newText.length))
                    },
                    onSendSticker = { emoji ->
                        val reply = replyContext()
                        scope.launch { messagingRepo.sendText(peerId, emoji, reply) }
                        replyingTo = null
                        emojiOpen = false
                    },
                    onPickGif = { gif ->
                        emojiOpen = false
                        val reply = replyContext()
                        scope.launch {
                            // fetchGifBytes can fail (network) BEFORE sendMedia
                            // ever creates a message row -- unlike every other
                            // failure path in this screen, that would have
                            // nothing to show a "failed, tap to retry" bubble
                            // on, so it needs its own explicit feedback rather
                            // than silently doing nothing when tapped. sendMedia
                            // itself still handles its own failure (uploadFailed
                            // bubble) once bytes are actually in hand.
                            val bytes = runCatching { fetchGifBytes(apiClient.client, gif) }
                                .onFailure { snackbarHostState.showSnackbar("Couldn't load that GIF -- try again") }
                                .getOrNull()
                            if (bytes != null) {
                                messagingRepo.sendMedia(peerId, bytes, "gif.gif", "image/gif", "image", gif.width, gif.height, reply)
                            }
                            replyingTo = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).padding(bottom = 6.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (editingId == null) {
                    IconButton(onClick = { emojiOpen = false; mediaLauncher() }, enabled = !attaching, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.AttachFile, contentDescription = "Attach a file", tint = mutedText(0.45f))
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { new ->
                        val wasEmpty = draft.text.isEmpty()
                        draft = new
                        if (editingId == null) {
                            if (new.text.isNotEmpty() && wasEmpty) scope.launch { messagingRepo.sendTyping(peerId, true) }
                            typingJob?.cancel()
                            typingJob = scope.launch {
                                delay(2_000)
                                messagingRepo.sendTyping(peerId, false)
                            }
                            if (new.text.isEmpty()) scope.launch { messagingRepo.sendTyping(peerId, false) }
                        }
                    },
                    modifier = Modifier.weight(1f).focusRequester(composerFocusRequester),
                    placeholder = { Text("Message…", color = mutedText(0.4f)) },
                    shape = RoundedCornerShape(D2MRadius.pill),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    // "based on user keyboard settings, first letter
                    // capitalisation should be triggered" -- Sentences mode
                    // asks the IME to auto-capitalize the first letter of
                    // each sentence, same as every other messaging app's
                    // compose field (and the system's own default behavior
                    // for a plain sentence-style input, which this field was
                    // never actually opting into before).
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    trailingIcon = {
                        if (editingId == null) {
                            IconButton(onClick = { emojiOpen = !emojiOpen }) {
                                Icon(
                                    Icons.Filled.EmojiEmotions,
                                    contentDescription = "Emoji, stickers & GIFs",
                                    tint = if (emojiOpen) MaterialTheme.colorScheme.primary else mutedText(0.45f),
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = mutedText(0.15f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = mutedText(0.03f),
                        focusedContainerColor = mutedText(0.03f),
                    ),
                )
                val canSend = draft.text.isNotBlank()
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
                            val text = draft.text
                            draft = TextFieldValue("")
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
    // "sent & read double tick differentiation - i couldnt feel" -- the real
    // bug: ticks only ever render on MY OWN sent bubbles (mine == true at
    // every call site), which are filled with MaterialTheme.colorScheme.
    // primary -- and the "read" branch here used that EXACT SAME color for
    // the tick. A read receipt wasn't subtly different, it was invisible:
    // same color as the bubble it's sitting on. First fix used the theme's
    // info.fg token, which turned out to still be too low-contrast against
    // the Child flow's teal/green bubble fill ("read double tick is not
    // properly visible in the green bubble") -- ReadTickColor (declared
    // above) is a dedicated, purpose-built bright blue instead, chosen
    // specifically for legibility against a saturated fill, not a
    // general-purpose semantic token repurposed for this.
    val color = if (status == MessageStatus.READ) {
        ReadTickColor
    } else {
        LocalContentColor.current.copy(alpha = if (mine) 0.75f else 0.5f)
    }
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

/**
 * "typing notification is different in web & mobile - i want like web."
 * Direct port of d2m_web's TypingIndicator.jsx: left-aligned, shaped like a
 * received bubble (16/16/16/4 corner radii -- the sharp bottom-left corner
 * is the same "notch" every received bubble in this app uses), three 6.dp
 * dots pulsing in a staggered wave.
 *
 * Web drives this with a shared CSS keyframe (d2m-pulse: scale 1→1.4→1,
 * opacity 1→0.35→1, 1.2s ease-in-out infinite, each dot delayed by 0.15s).
 * Compose has no direct per-element animation-delay primitive, so this uses
 * one shared `rememberInfiniteTransition` progress value (0f..1f over
 * 1200ms, linear, restarting) and phase-shifts each dot's read of it by
 * 0.125 (150ms / 1200ms) -- same net staggered wave, driven by one ticker
 * instead of three independent ones.
 */
@Composable
private fun TypingIndicatorBubble(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "typing-progress",
    )
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
                .border(BorderStroke(1.dp, mutedText(0.15f)), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0..2) {
                // Symmetric bump (0 at phase edges, 1 at phase center) approximates
                // the CSS keyframe's 0%/50%/100% shape closely enough to read the
                // same -- doesn't need to be pixel-identical easing, just the same
                // "breathing dot" feel.
                val phase = (t + i * 0.125f) % 1f
                val bump = ((1 - cos(TWO_PI * phase)) / 2).toFloat()
                val dotScale = 1f + 0.4f * bump
                val dotAlpha = 1f - 0.65f * bump
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer(scaleX = dotScale, scaleY = dotScale, alpha = dotAlpha)
                        .background(mutedText(0.45f), CircleShape),
                )
            }
        }
    }
}

/**
 * "GIF sent/received should be autoplaying in the bubble atleast for 2-3
 * times. When tapped on the bubble, it should play again for 2-3 times."
 *
 * Two separate pieces had to be real for this to work at all:
 *  1. An actual GIF decoder registered with Coil (see GifImageLoader.kt --
 *     this app's Coil dependencies had none before this change, so a GIF
 *     rendered as a static first-frame image regardless of anything below).
 *  2. A capped repeat count (GIF_BUBBLE_REPEAT_COUNT, via
 *     applyGifRepeatCount) -- without it, a typical authored/looping GIF's
 *     own embedded loop metadata is usually infinite, which would autoplay
 *     forever rather than "2-3 times."
 *
 * Replay-on-tap: Coil caches the decoded animated-drawable instance by
 * request identity, so re-issuing the exact same request after it's
 * finished its capped loops would just reattach the same already-stopped
 * drawable, not restart it. Bumping `replayToken` into the request's
 * memoryCacheKey forces Coil to treat a tap as a distinct request, which
 * creates a fresh animated-drawable instance starting at frame 0.
 */
@Composable
private fun GifBubbleImage(url: String, contentDescription: String?) {
    val imageLoader: ImageLoader = koinInject()
    val platformContext = LocalPlatformContext.current
    var replayToken by remember(url) { mutableStateOf(0) }
    val request = remember(url, replayToken) {
        ImageRequest.Builder(platformContext)
            .data(url)
            .applyGifRepeatCount(GIF_BUBBLE_REPEAT_COUNT)
            .memoryCacheKey("$url#replay=$replayToken")
            .build()
    }
    AsyncImage(
        model = request,
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        modifier = Modifier.widthIn(max = 240.dp).clip(RoundedCornerShape(D2MRadius.sm))
            .clickable { replayToken++ },
    )
}

/**
 * Video attachment bubble -- no decoded thumbnail frame is available (see
 * MessageRow's own comment on this), so this is a fixed-size dark card with
 * a centered play affordance + duration, matching every chat app's fallback
 * treatment for a video it hasn't generated a poster frame for. Tapping
 * opens the real in-app player full-screen (MediaViewerDialog.kt).
 */
@Composable
private fun VideoBubblePlaceholder(durationSec: Int?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(D2MRadius.sm))
            .background(Color(0xFF1C1C1C))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play video", tint = Color.White, modifier = Modifier.size(28.dp))
        }
        if (durationSec != null && durationSec > 0) {
            Text(
                "${durationSec / 60}:${(durationSec % 60).toString().padStart(2, '0')}",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(D2MRadius.sm))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    message: ChatMessage,
    myUsername: String?,
    peerDisplayName: String,
    menuOpen: Boolean,
    modifier: Modifier = Modifier,
    onOpenMenu: () -> Unit,
    onCloseMenu: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onRetry: () -> Unit,
    onOpenMedia: (MediaViewerTarget) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // BoxWithConstraints (not a fixed dp cap) to match d2m_web's
        // MessageBubble.jsx exactly -- web caps a bubble at `max-width: 72%`
        // of the message-list container, a RELATIVE limit, not a fixed
        // pixel one. This used to be a flat `widthIn(max = 280.dp)`, which
        // reads completely differently depending on screen size: on a
        // narrow phone 280dp is most of the available width (bubbles look
        // "elongated," reported directly against exactly this screen), and
        // on a wide layout the same 280dp looks disproportionately narrow
        // next to web's 72%. Computing the cap from the actual available
        // width here reproduces web's relative sizing instead of a
        // one-size-that-fits-no-screens constant.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val maxBubbleWidth = maxWidth * 0.72f
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start) {
            Box {
                // "swipe right on a bubble to reply should be provided" --
                // WhatsApp-style: drag the bubble right, a reply icon fades in
                // underneath/behind it, release past replyThresholdPx to fire
                // onReply and spring the bubble back to rest. Horizontal drag
                // only (detectHorizontalDragGestures), so this never competes
                // with the LazyColumn's own vertical scroll gesture.
                val swipeScope = rememberCoroutineScope()
                val density = LocalDensity.current
                val replyThresholdPx = remember(density) { with(density) { 56.dp.toPx() } }
                val maxDragPx = remember(density) { with(density) { 96.dp.toPx() } }
                val dragAnim = remember(message.id) { Animatable(0f) }

                // "If a sticker is sent, the ui is different in web - without
                // bubbles & bigger in size." A sticker isn't its own message
                // type -- it's a plain text message containing one emoji (see
                // EmojiGifPanel.onSendSticker) -- so this mirrors d2m_web's
                // MessageBubble.jsx exactly: ANY short emoji-only text message
                // (not just ones sent via the sticker tab) drops the bubble
                // chrome and renders the emoji large, same as iMessage/
                // WhatsApp's convention. See EmojiData.kt's isEmojiOnly for the
                // detection port.
                val hasMedia = message.media != null && message.mediaUrl != null
                val emojiOnly = !hasMedia && !message.deleted && message.callLog == null && isEmojiOnly(message.text)

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
                // emojiOnly bubbles sit directly on the page (transparent, no
                // fill) so they always need the normal page-text color, not the
                // accent-contrast color that's tuned for a solid `mine` fill --
                // matches web's own comment on this exact case ("sent emoji-only
                // timestamp unreadable in dark mode").
                val bubbleContentColor = when {
                    emojiOnly -> MaterialTheme.colorScheme.onSurface
                    message.isMine -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurface
                }

                // Reply icon reveal -- sits underneath the bubble (declared
                // before it, so it draws first/behind) at this Box's start
                // edge; fades and scales in as dragAnim approaches the
                // reply threshold, same as WhatsApp's own affordance.
                if (dragAnim.value > 1f) {
                    val revealProgress = (dragAnim.value / replyThresholdPx).coerceIn(0f, 1f)
                    Icon(
                        Icons.Filled.Reply,
                        contentDescription = null,
                        tint = mutedText(0.5f),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(20.dp)
                            .graphicsLayer(alpha = revealProgress, scaleX = 0.6f + 0.4f * revealProgress, scaleY = 0.6f + 0.4f * revealProgress),
                    )
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(dragAnim.value.roundToInt(), 0) }
                        // "replying/reacting to call logs shouldnt be
                        // allowed" -- there's nothing meaningful to reply to
                        // on a call-log entry, so the swipe gesture itself
                        // is disabled for one rather than just no-oping
                        // onReply, which would otherwise still let the
                        // bubble visibly drag and reveal the reply icon.
                        .then(
                            if (message.callLog == null) {
                                Modifier.pointerInput(message.id) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            swipeScope.launch {
                                                if (dragAnim.value > replyThresholdPx) onReply()
                                                dragAnim.animateTo(0f, animationSpec = spring())
                                            }
                                        },
                                        onDragCancel = { swipeScope.launch { dragAnim.animateTo(0f, animationSpec = spring()) } },
                                    ) { change, dragAmount ->
                                        // Rightward-only (coerced at 0f) -- matches
                                        // WhatsApp, which never lets a bubble drag
                                        // left. change.consume() here (rather than
                                        // leaving it for combinedClickable below)
                                        // is what keeps this gesture from also
                                        // registering as a long-press once a real
                                        // drag is underway.
                                        change.consume()
                                        val next = (dragAnim.value + dragAmount).coerceIn(0f, maxDragPx)
                                        swipeScope.launch { dragAnim.snapTo(next) }
                                    }
                                }
                            } else Modifier,
                        )
                        .widthIn(max = maxBubbleWidth)
                        .then(
                            if (emojiOnly) Modifier
                            else Modifier.background(if (message.isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, bubbleShape),
                        )
                        .then(if (!emojiOnly && !message.isMine) Modifier.border(1.dp, mutedText(0.15f), bubbleShape) else Modifier)
                        // Tap now also does something: FAILED messages retry
                        // on tap (Don Norman "error recovery" -- a stuck
                        // "Failed" label with no action was a dead end before).
                        // "replying/reacting to call logs shouldnt be
                        // allowed" -- long-press (the react/reply/edit/
                        // delete menu) is disabled entirely for a call-log
                        // bubble, same reasoning as the swipe gesture above.
                        .combinedClickable(onClick = { if (message.status == MessageStatus.FAILED) onRetry() }, onLongClick = { if (!message.deleted && message.callLog == null) onOpenMenu() })
                        .padding(if (emojiOnly) PaddingValues(horizontal = 2.dp, vertical = 1.dp) else PaddingValues(10.dp)),
                ) {
                    CompositionLocalProvider(LocalContentColor provides bubbleContentColor) {
                        // width(IntrinsicSize.Max) is the fix for a real bug (reported
                        // with a screenshot: every bubble stretched out to the full
                        // widthIn(max=maxBubbleWidth) cap regardless of how short the text
                        // was, instead of hugging its content the way web's bubbles do).
                        // The outer Box only constrains the MAX width (maxBubbleWidth,
                        // computed above as 72% of the available row width) -- a plain
                        // Column has no opinion of its own on width beyond that, but
                        // several children below (the timestamp/ticks row, the reply-quote
                        // background, the upload progress bar) use fillMaxWidth() so their
                        // OWN background/alignment spans the bubble's full resolved width
                        // rather than just their own text's width. Without an explicit
                        // width on this Column, "fill max width" for those children meant
                        // literally the incoming maxBubbleWidth constraint, forcing the
                        // WHOLE bubble to that width for every message. IntrinsicSize.Max makes
                        // this Column measure itself to its widest child's natural
                        // (non-fillMaxWidth) size first -- i.e. the message text -- and
                        // THEN the fillMaxWidth() children below fill relative to that
                        // resolved width instead of the outer constraint, exactly matching
                        // MessageBubble.jsx's CSS (width: fit-content, max-width: ...).
                        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                            if (message.replyTo != null) {
                                // "id shown in reply bubble - name should be
                                // shown" -- message.replyTo.from is the raw
                                // messaging username (same wire-level id every
                                // other place in this app resolves before
                                // display), never something to show directly.
                                // Only two people are ever in this
                                // conversation, so it's either me or this
                                // chat's one peer -- matches web's own
                                // `message.replyTo.from === me ? "You" :
                                // peerName || "Them"` exactly.
                                val replyFromLabel = if (message.replyTo.from == myUsername) "You" else peerDisplayName
                                // "reply bubble ui doesnt sync with web" --
                                // web's version (MessageBubble.jsx) has a
                                // left accent stripe (borderLeft: 3px),
                                // asymmetric 3/7 padding, and a single-line
                                // ellipsized preview -- this was a plain
                                // uniform-padding tinted box with no stripe
                                // and a 2-line wrap, a visibly different shape.
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                                        .background(
                                            if (message.isMine) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                            RoundedCornerShape(4.dp),
                                        ),
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxHeight().width(3.dp)
                                            .background(
                                                if (message.isMine) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp),
                                            ),
                                    )
                                    Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)) {
                                        Text(replyFromLabel, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = LocalContentColor.current.copy(alpha = 0.9f))
                                        Text(
                                            message.replyTo.preview,
                                            fontSize = 12.5.sp,
                                            color = LocalContentColor.current.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Spacer(Modifier.padding(top = 4.dp))
                            }

                            when {
                                message.deleted -> Text("This message was deleted", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = LocalContentColor.current.copy(alpha = 0.7f))
                                message.callLog != null -> {
                                    val info = message.callLog
                                    // "No answer"/declined/busy/failed all read as
                                    // "nobody actually talked" -- matches web's own
                                    // CallLogContent noAnswer check exactly (same four
                                    // reasons), which is what decides the icon (phone-off
                                    // vs phone/video) AND its color (danger vs normal).
                                    val noAnswer = info.reason == "declined" || info.reason == "busy" || info.reason == "failed" ||
                                        (info.durationSec == 0 && info.incoming && info.reason != "ended")
                                    val label = when (info.reason) {
                                        "ended" -> "${if (info.media == "video") "Video" else "Voice"} call"
                                        "busy", "failed" -> "Call failed"
                                        "declined" -> "Call declined"
                                        else -> if (info.incoming) "Missed call" else "No answer"
                                    }
                                    val durationLabel = if (info.durationSec > 0) "${info.durationSec / 60}:${(info.durationSec % 60).toString().padStart(2, '0')}" else null
                                    // Circular icon badge, not a bare small icon -- matches
                                    // d2m_web's CallLogContent exactly (30dp circle, soft
                                    // accent fill, danger tint only for the no-answer
                                    // states). Reported directly ("the width of the bubble
                                    // for call history... doesn't match web"): the old bare
                                    // 16dp icon made a call-log bubble read as visibly
                                    // smaller/plainer than every other bubble type, not an
                                    // actual width bug in this bubble's own container (which
                                    // already shares the same widthIn/IntrinsicSize.Max
                                    // machinery as every other message).
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                        Box(
                                            modifier = Modifier.size(30.dp)
                                                .background(
                                                    if (message.isMine) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                    CircleShape,
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                when {
                                                    noAnswer -> Icons.Filled.CallEnd
                                                    info.media == "video" -> Icons.Filled.Videocam
                                                    else -> Icons.Filled.Call
                                                },
                                                contentDescription = null,
                                                tint = if (noAnswer) MaterialTheme.colorScheme.error else LocalContentColor.current,
                                                modifier = Modifier.size(15.dp),
                                            )
                                        }
                                        Column {
                                            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            durationLabel?.let {
                                                Text(it, style = MaterialTheme.typography.labelSmall, color = LocalContentColor.current.copy(alpha = 0.75f))
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    if (message.media != null && message.mediaUrl != null) {
                                        // "GIF sent/received should be autoplaying in the
                                        // bubble atleast for 2-3 times. When tapped on the
                                        // bubble, it should play again for 2-3 times."
                                        // GIFs sent via the composer's GIF tab (GifPicker.kt)
                                        // upload with kind="image" (same as a plain photo
                                        // attachment) but mime="image/gif" -- mime is the
                                        // only reliable signal here for "this needs the
                                        // special animated treatment", not kind. GIFs keep
                                        // their existing tap-to-replay behavior rather than
                                        // opening the new full-screen viewer -- that's
                                        // already the correct, previously-requested tap
                                        // affordance for this specific media type.
                                        when {
                                            message.media.mime == "image/gif" -> GifBubbleImage(url = message.mediaUrl, contentDescription = message.media.name)
                                            // "always the media bubble size is a defined
                                            // height & width... provide a image & video
                                            // viewer" -- video attachments previously fell
                                            // through to the plain AsyncImage branch below,
                                            // which can't decode a video file at all (silently
                                            // broken). No decoded thumbnail is available yet
                                            // (MediaMeta.thumb is never populated anywhere in
                                            // this app today), so this is a fixed dark
                                            // placeholder + play affordance -- tapping opens
                                            // the real in-app player full-screen.
                                            message.media.kind == "video" -> VideoBubblePlaceholder(
                                                durationSec = message.media.duration,
                                                onClick = { onOpenMedia(MediaViewerTarget.Video(message.mediaUrl)) },
                                            )
                                            else -> AsyncImage(
                                                model = message.mediaUrl,
                                                contentDescription = message.media.name,
                                                modifier = Modifier.widthIn(max = 240.dp).clip(RoundedCornerShape(D2MRadius.sm))
                                                    .clickable { onOpenMedia(MediaViewerTarget.Image(message.mediaUrl, message.media.name)) },
                                            )
                                        }
                                    }
                                    if (message.uploading) {
                                        Column(modifier = Modifier.width(160.dp).padding(top = 4.dp)) {
                                            Text("Uploading…", style = MaterialTheme.typography.labelSmall)
                                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                        }
                                    }
                                    if (message.uploadFailed) Text("Attachment failed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    if (message.text.isNotBlank()) {
                                        if (emojiOnly) {
                                            // Matches web's emojiOnly text styling exactly: fontSize
                                            // 38 / lineHeight 1.1, no bubble chrome around it (handled
                                            // above by the outer Box's background/border/padding).
                                            Text(message.text, fontSize = 38.sp, lineHeight = 42.sp)
                                        } else {
                                            // "Text size can be a bit bigger" -- bumped from web's
                                            // own 14.5sp match (MaterialTheme.typography.bodyMedium)
                                            // up to 16sp, a deliberate mobile-only divergence per
                                            // that direct ask, not a parity fix.
                                            Text(message.text, fontSize = 16.sp, lineHeight = 23.sp)
                                        }
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

                // "long press a bubble - show emojis for react & show a
                // dropdown with other options - like how whatsapp does" --
                // previously a two-step interaction (long-press -> "React"
                // menu item -> a SECOND popup with the actual emoji row).
                // One long press now surfaces both at once: a quick-reaction
                // row at the top of the same popup, then the option list
                // below it, exactly like WhatsApp's combined long-press sheet.
                // "the long press menu we receive for bubble can be rounded
                // corner" -- Material3's DropdownMenu defaults to a small
                // (4dp) corner radius; bumped to match this app's own
                // generally-rounder D2MRadius.lg language used elsewhere
                // (bubbles, banners).
                DropdownMenu(expanded = menuOpen, onDismissRequest = onCloseMenu, shape = RoundedCornerShape(D2MRadius.lg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        QUICK_REACTIONS.forEach { emoji ->
                            Text(
                                emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .combinedClickable(onClick = { onReact(emoji) })
                                    .padding(4.dp),
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(text = { Text("Reply") }, onClick = onReply)
                    if (message.isMine && message.media == null) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = onEdit)
                    }
                    DropdownMenuItem(text = { Text("Delete for me") }, onClick = onDeleteForMe)
                    if (message.isMine) {
                        DropdownMenuItem(text = { Text("Delete for everyone") }, onClick = onDeleteForEveryone)
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
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    message.reactions.forEach { (emoji, users) ->
                        val mine = myUsername != null && users.contains(myUsername)
                        Box(
                            modifier = Modifier
                                .background(if (mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else mutedText(0.08f), RoundedCornerShape(D2MRadius.sm))
                                .combinedClickable(onClick = { onReact(emoji) })
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            // "reaction display below the bubbles also should be a
                            // bit bigger - looks too tiny now" -- bumped from
                            // labelSmall (11sp) to 14sp, up from 6/2dp to 8/4dp
                            // padding to match.
                            Text("$emoji ${users.size}", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
