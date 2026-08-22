package com.d2m.app.ui.screens.child

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.components.BackHandlerCompat
import com.d2m.app.data.model.ThreadOut
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.SeriousModeRepository
import com.d2m.app.messaging.ChatUiState
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.call.CallManager
import com.d2m.app.messaging.d2mIdToMessagingUsername
import com.d2m.app.messaging.ui.ArchivePinDialog
import com.d2m.app.messaging.ui.ChatPane
import com.d2m.app.messaging.ui.mediaLabel
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Same two-tone gradient d2m_web hardcodes for every match avatar (MatchesScreen.jsx's `linear-gradient(135deg,#DCEEEA,#FBEAD2)`) -- there's no real profile photo on a Thread on either platform (ThreadOut/app/schemas.py's ThreadOut has no photo field at all), so this gradient squircle IS the design, not a placeholder standing in for a missing photo. */
private val AvatarGradient = Brush.linearGradient(listOf(Color(0xFFDCEEEA), Color(0xFFFBEAD2)))

/**
 * Mirrors screens/child/MatchesScreen.jsx -- match inbox + chat. The
 * state-machine parts (thread list, status badges, Go Serious/Revoke/
 * Unmatch/Union banners) are fully wired against SeriousModeRepository here
 * (Phase 2 scope). The conversation pane is ChatPane from the messaging
 * module (Phase 6) -- see that file's doc comment: it renders today using
 * the explicitly-non-production StubUnencryptedCryptoProvider, so this
 * screen is wired end-to-end and will "just work" for real once a real
 * CryptoProvider is swapped into the DI module, rather than needing this
 * screen rebuilt later.
 *
 * Layout bug fix (reported with screenshots -- the detail pane's empty-state
 * text was rendering one word per line in a near-zero-width strip on the
 * far right): this used to be a permanent side-by-side Row(list.width(300dp),
 * detail.weight(1f)) -- a two-pane master-detail layout straight-ported from
 * web without noticing that web itself only shows that layout above a
 * 720px viewport (MatchesScreen.jsx's `isNarrow` check collapses to a
 * single pane below that, "same fix as ParentMessagesScreen.jsx's identical
 * fixed-320px-sidebar issue"). A phone screen is always narrower than that,
 * so the fixed 300dp list column left the weight(1f) detail column with
 * only tens of dp of remaining width -- not a rendering bug so much as the
 * tablet/desktop layout being asked to fit somewhere it structurally can't.
 * Now mirrors web's own narrow-mode branch unconditionally: the thread list
 * fills the screen until a match is selected, then the conversation replaces
 * it full-width with a back button, same single-pane-at-a-time pattern as
 * any phone messaging app (and as web itself falls back to below 720px).
 */
@Composable
fun MatchesScreen() {
    val identityStore = org.koin.compose.koinInject<IdentityStore>()
    val seriousModeRepo = org.koin.compose.koinInject<SeriousModeRepository>()
    val chatUiState = org.koin.compose.koinInject<ChatUiState>()
    val callManager = org.koin.compose.koinInject<CallManager>()
    val messagingRepo = org.koin.compose.koinInject<MessagingRepository>()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()

    var threads by remember { mutableStateOf<List<ThreadOut>>(emptyList()) }
    var selected by remember { mutableStateOf<ThreadOut?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }

    val primaryId = identity.primaryId

    // Was `threads = seriousModeRepo.getThreads(...)` with no guard -- every
    // caller below wraps its own action in runCatching but then calls this
    // afterwards unguarded, so a network hiccup on the refresh alone (not
    // just the Go Serious/Revoke/Unmatch/respond call itself) escaped
    // scope.launch as an uncaught ApiError and crashed the whole app. Same
    // friendlyError/actionError pattern as ChildHomeScreen.kt's handlers.
    suspend fun refresh() {
        if (primaryId == null) return
        try {
            threads = seriousModeRepo.getThreads(primaryId, forceRefresh = true)
            threads.forEach { messagingRepo.rememberPeerName(it.otherParticipantId, it.otherParticipantName) }
        } catch (e: Exception) {
            actionError = friendlyError(e, "Couldn't refresh your matches.")
        }
    }

    LaunchedEffect(primaryId) {
        if (primaryId == null) return@LaunchedEffect
        loading = true
        error = null
        try {
            threads = seriousModeRepo.getThreads(primaryId)
            // Reported directly: "When notification is received, I get the id
            // in the title of who is sending." messagingRepo.peerDisplayName()
            // (used by both the in-app banner and LocalNotificationBridge's
            // background notifications) falls back to the raw messaging
            // username whenever rememberPeerName() has never been called for
            // that peer -- previously that only happened once ChatPane itself
            // composed for a peer, i.e. only after this device had actually
            // opened that specific conversation THIS session. A message from
            // any other match -- realistic right after a fresh app launch --
            // showed a raw id as the notification title. Feeding the whole
            // thread list's names in as soon as it loads means every match's
            // real name is known before any notification for them can arrive.
            threads.forEach { messagingRepo.rememberPeerName(it.otherParticipantId, it.otherParticipantName) }
        } catch (e: Exception) {
            error = friendlyError(e, "Couldn't load your matches.")
        } finally {
            loading = false
        }
    }

    // Hide App.kt's bottom-tab bar while a conversation is open -- reported
    // directly: "Bottom nav bar is not required inside a person's chat".
    // The thread list and open conversation are both this same MATCHES nav
    // route (switched by `selected`, not a route change), so App.kt has no
    // way to know a chat is open without this shared flag -- see
    // ChatUiState.kt's doc comment for why this is a Koin singleton rather
    // than a nav-graph change.
    LaunchedEffect(selected) { chatUiState.setConversationOpen(selected != null) }
    DisposableEffect(Unit) { onDispose { chatUiState.setConversationOpen(false) } }

    // Reported directly: "Going into a chat & pressing back takes me home -
    // should take me to matches." See BackHandlerCompat.kt's doc comment --
    // without this, system back has nothing to intercept inside this single
    // route and falls through to the NavController, popping past the
    // conversation to Home instead of just closing it.
    BackHandlerCompat(enabled = selected != null) { selected = null }

    // A tapped in-app notification banner (messaging/ui/InAppNotificationLayer.kt)
    // records which peer to jump to -- once this screen's thread list is
    // loaded, find the matching thread and open it directly.
    val pendingOpenPeer by chatUiState.pendingOpenPeerUsername.collectAsState()
    LaunchedEffect(pendingOpenPeer, threads) {
        val pending = pendingOpenPeer ?: return@LaunchedEffect
        val match = threads.firstOrNull { d2mIdToMessagingUsername(it.otherParticipantId) == pending }
        if (match != null) {
            selected = match
            chatUiState.clearPendingOpenPeer()
        }
    }

    // Reported directly: "Why am I seeing closed chats? - we can remove
    // them from list." Web actually keeps closed threads visible (with a
    // "Closed" badge) -- this is a deliberate mobile-only divergence per
    // that direct ask, not a parity bug. `threads` itself stays
    // unfiltered (rememberPeerName/pendingOpenPeer above still need every
    // match, closed or not); only the rendered list hides them.
    val visibleThreads = remember(threads) { threads.filterNot { it.status == "closed" } }

    D2MTheme(flow = D2MFlow.CHILD) {
      Box(modifier = Modifier.fillMaxSize()) {
        val t = selected
        if (t == null) {
            // Thread list, full width -- default pane.
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Matches", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                when {
                    loading -> Text("Loading…", color = mutedText(0.55f), modifier = Modifier.padding(top = 12.dp))
                    error != null -> D2MErrorBanner(error!!, modifier = Modifier.padding(top = 12.dp))
                    visibleThreads.isEmpty() -> D2MEmptyState("No matches yet", "Once you and someone else both accept, they'll show up here.")
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 14.dp)) {
                        items(visibleThreads) { thread ->
                            ThreadRow(thread = thread, onClick = { selected = thread })
                        }
                    }
                }
            }
        } else {
            // Conversation, full width -- replaces the list entirely (back
            // button returns to it), same as web's isNarrow branch. Header
            // is ONE row -- back button, avatar, name, audio call, video
            // call, more -- reported directly: "back button, user
            // thumbnail, Name, audio call button, video call button, more
            // option all in one line - that's how things are in all the
            // apps." ChatPane below owns nothing but messages + composer.
            val peerUsername = remember(t.otherParticipantId) { d2mIdToMessagingUsername(t.otherParticipantId) }
            Column(modifier = Modifier.fillMaxSize()) {
                ConversationHeader(
                    thread = t,
                    onBack = { selected = null },
                    onStartAudioCall = { scope.launch { callManager.startCall(peerUsername, "audio") } },
                    onStartVideoCall = { scope.launch { callManager.startCall(peerUsername, "video") } },
                    menuContent = {
                        MoreOptionsMenu(
                            thread = t,
                            onGoSerious = {
                                val pid = primaryId ?: return@MoreOptionsMenu
                                scope.launch {
                                    try {
                                        seriousModeRepo.requestSeriousMode(pid, t.threadId, pid)
                                    } catch (e: Exception) {
                                        actionError = friendlyError(e, "Couldn't send that Serious Mode request.")
                                    }
                                    refresh()
                                }
                            },
                            onRevoke = {
                                val pid = primaryId ?: return@MoreOptionsMenu
                                scope.launch {
                                    try {
                                        seriousModeRepo.revoke(pid, t.threadId)
                                    } catch (e: Exception) {
                                        actionError = friendlyError(e, "Couldn't revoke Serious Mode.")
                                    }
                                    refresh()
                                }
                            },
                            onUnmatch = {
                                val pid = primaryId ?: return@MoreOptionsMenu
                                scope.launch {
                                    try {
                                        seriousModeRepo.unmatch(pid, t.threadId)
                                        selected = null
                                    } catch (e: Exception) {
                                        actionError = friendlyError(e, "Couldn't unmatch.")
                                    }
                                    refresh()
                                }
                            },
                            onAcceptSeriousRequest = {
                                val reqId = t.pendingSeriousModeRequestId ?: return@MoreOptionsMenu
                                val pid = primaryId ?: return@MoreOptionsMenu
                                scope.launch {
                                    try {
                                        seriousModeRepo.respond(pid, reqId, "accept")
                                    } catch (e: Exception) {
                                        actionError = friendlyError(e, "Couldn't accept that Serious Mode request.")
                                    }
                                    refresh()
                                }
                            },
                            onDeclineSeriousRequest = {
                                val reqId = t.pendingSeriousModeRequestId ?: return@MoreOptionsMenu
                                val pid = primaryId ?: return@MoreOptionsMenu
                                scope.launch {
                                    try {
                                        seriousModeRepo.respond(pid, reqId, "decline")
                                    } catch (e: Exception) {
                                        actionError = friendlyError(e, "Couldn't decline that Serious Mode request.")
                                    }
                                    refresh()
                                }
                            },
                        )
                    },
                )
                actionError?.let { D2MErrorBanner(it, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
                ChatPane(
                    peerId = t.otherParticipantId,
                    peerName = t.otherParticipantName,
                    // imePadding() scoped right here -- ONLY this weighted
                    // region (message list + composer) reacts to the
                    // keyboard; ConversationHeader above is a plain sibling
                    // in this Column and is untouched by it. See App.kt's
                    // Scaffold contentWindowInsets comment for the other
                    // half of this fix.
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp).imePadding(),
                )
            }
        }
        // Rendered unconditionally (mirrors ArchivePinModal.jsx being
        // mounted regardless of whether a thread is open) -- it internally
        // no-ops until messagingRepo.archivePrompt is set, see its own doc
        // comment. Sits on top of both branches above via this shared Box.
        ArchivePinDialog(messagingRepo)
      }
    }
}

/**
 * One row in the thread list -- a gradient avatar placeholder (same visual
 * idea as web's `linear-gradient(135deg,#DCEEEA,#FBEAD2)` circle, which this
 * platform had no equivalent of at all) plus name and status, in a tappable
 * row rather than a boxed Card so a long list doesn't turn into a stack of
 * competing card borders.
 *
 * Presence dot added here (previously only shown once a conversation was
 * already open, in ConversationHeader below -- reported directly as "online
 * availability status is not shown properly," and this row was the actual
 * gap: it never subscribed to or rendered presence at all). Subscribing here
 * is safe to call once per row mount -- MessagingRepository.subscribePresence
 * is idempotent (guarded by its own `subscribedPeers` set) and self-heals on
 * reconnect, so this is just "make sure we're subscribed" for every peer
 * actually visible in the list, same as WhatsApp/web showing online status
 * directly in the chat list rather than only after opening a thread.
 */
@Composable
private fun ThreadRow(thread: ThreadOut, onClick: () -> Unit) {
    val messagingRepo: MessagingRepository = koinInject()
    val peerUsername = remember(thread.otherParticipantId) { d2mIdToMessagingUsername(thread.otherParticipantId) }
    val peerOnline by messagingRepo.isPeerOnline(peerUsername).collectAsState()
    val peerTyping by messagingRepo.isPeerTyping(peerUsername).collectAsState()
    // "In matches view, last sent/received message is shown below the name
    // in web. in mobile - matched is shown. i want same as web." Web's
    // sidebar row (MatchesScreen.jsx's sortedThreads/_preview) derives this
    // from the same messagesByPeer state the open conversation reads, not a
    // separate fetch -- messagingRepo.messagesFor() is the mobile equivalent
    // (already loaded from local cache + live updates, see
    // MessagingRepository.kt's messagesFor doc comment), so this row just
    // needs to pick the newest message and format it the same way web does:
    // typing (highest priority) > last message preview > the old static
    // status label as a fallback for a fresh match with no messages yet.
    val messages by messagingRepo.messagesFor(peerUsername).collectAsState()

    LaunchedEffect(peerUsername) { messagingRepo.subscribePresence(thread.otherParticipantId) }

    val lastMessage = remember(messages) { messages.maxByOrNull { it.sentAt } }
    val previewText = remember(lastMessage) {
        when {
            lastMessage == null -> ""
            lastMessage.deleted -> "This message was deleted"
            lastMessage.media != null -> mediaLabel(lastMessage.media)
            else -> lastMessage.text
        }
    }
    val subtitleBase = if (peerTyping) "typing…" else previewText.ifBlank { threadStatusLabel(thread.status) }
    val subtitle = if (!peerTyping && thread.pendingSeriousModeRequestId != null) "$subtitleBase · Serious Mode pending" else subtitleBase

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            Box(modifier = Modifier.size(48.dp).background(AvatarGradient, RoundedCornerShape(14.dp)))
            PresenceDot(online = peerOnline, modifier = Modifier.align(Alignment.BottomEnd))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(thread.otherParticipantName, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (peerTyping) FontWeight.SemiBold else FontWeight.Normal,
                color = if (peerTyping) MaterialTheme.colorScheme.primary else mutedText(0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Full conversation header -- ONE row: back button, gradient squircle
 * avatar + presence dot, name (+ lock icon once real E2E encryption is on)
 * with a status subtitle underneath (typing > online > match-status, web's
 * own priority), then audio call / video call / more-options together on
 * the trailing edge. Previously split across two rows (a back+avatar+name
 * row in MatchesScreen() and a second toolbar row inside ChatPane) --
 * reported directly as wrong ("all in one line - that's how things are in
 * all the apps"), fixed by moving the whole header here and having
 * ChatPane render only messages + composer underneath it. A small lock icon
 * appears next to the name only once messaging is actually end-to-end
 * encrypted (`isProductionGradeEncryption`) -- today that's always false
 * (see messaging/crypto/CryptoProvider.kt), so no lock shows and no other
 * "not encrypted yet" text clutters the header; this is the one place that
 * state surfaces at all now, and only in the direction of "yes, this is
 * secure," never "no, it isn't."
 */
@Composable
private fun ConversationHeader(
    thread: ThreadOut,
    onBack: () -> Unit,
    onStartAudioCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    menuContent: @Composable () -> Unit,
) {
    val messagingRepo: MessagingRepository = koinInject()
    val peerUsername = remember(thread.otherParticipantId) { d2mIdToMessagingUsername(thread.otherParticipantId) }
    val peerOnline by messagingRepo.isPeerOnline(peerUsername).collectAsState()
    val peerTyping by messagingRepo.isPeerTyping(peerUsername).collectAsState()

    LaunchedEffect(peerUsername) { messagingRepo.subscribePresence(thread.otherParticipantId) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to matches")
        }
        Box(modifier = Modifier.size(40.dp)) {
            Box(modifier = Modifier.size(40.dp).background(AvatarGradient, RoundedCornerShape(12.dp)))
            PresenceDot(online = peerOnline, modifier = Modifier.align(Alignment.BottomEnd))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(thread.otherParticipantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (messagingRepo.isProductionGradeEncryption) {
                    Icon(Icons.Filled.Lock, contentDescription = "End-to-end encrypted", modifier = Modifier.size(14.dp), tint = mutedText(0.5f))
                }
            }
            val subtitle = when {
                peerTyping -> "typing…"
                peerOnline -> "Online"
                else -> threadStatusLabel(thread.status)
            }
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = mutedText(0.55f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderIconButton(icon = Icons.Filled.Call, contentDescription = "Audio call", onClick = onStartAudioCall)
            HeaderIconButton(icon = Icons.Filled.Videocam, contentDescription = "Video call", onClick = onStartVideoCall)
            menuContent()
        }
    }
}

/** Same 38dp outlined-circle visual as the call buttons used to be inside ChatPane's old toolbar -- kept identical now that they live here instead. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeaderIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
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

/** The "more options" (⋮) menu -- Go Serious/Revoke Serious Mode/Unmatch, plus a pending Serious Mode request's Accept/Decline -- rendered as part of ConversationHeader's single row, trailing the call buttons. */
@Composable
private fun MoreOptionsMenu(
    thread: ThreadOut,
    onGoSerious: () -> Unit,
    onRevoke: () -> Unit,
    onUnmatch: () -> Unit,
    onAcceptSeriousRequest: () -> Unit,
    onDeclineSeriousRequest: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            if (thread.pendingSeriousModeRequestId != null) {
                DropdownMenuItem(text = { Text("Accept Serious Mode") }, onClick = { menuOpen = false; onAcceptSeriousRequest() })
                DropdownMenuItem(text = { Text("Decline Serious Mode") }, onClick = { menuOpen = false; onDeclineSeriousRequest() })
            }
            if (thread.status == "active") {
                DropdownMenuItem(text = { Text("Go Serious") }, onClick = { menuOpen = false; onGoSerious() })
            }
            if (thread.status == "exclusive") {
                DropdownMenuItem(text = { Text("Revoke Serious Mode") }, onClick = { menuOpen = false; onRevoke() })
            }
            DropdownMenuItem(text = { Text("Unmatch") }, onClick = { menuOpen = false; onUnmatch() })
        }
    }
}

@Composable
private fun PresenceDot(online: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(11.dp)
            .background(if (online) Color(0xFF3DBE6C) else mutedText(0.3f), CircleShape)
            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
    )
}

private fun threadStatusLabel(status: String): String = when (status) {
    "active" -> "Matched"
    "exclusive" -> "Serious exploration"
    "sunsetting" -> "Sunsetting"
    "closed" -> "Closed"
    else -> status
}
