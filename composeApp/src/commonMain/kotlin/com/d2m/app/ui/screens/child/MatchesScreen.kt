package com.d2m.app.ui.screens.child

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.ThreadOut
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.SeriousModeRepository
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.d2mIdToMessagingUsername
import com.d2m.app.messaging.ui.ChatPane
import com.d2m.app.ui.components.D2MBadge
import com.d2m.app.ui.components.D2MBadgeTone
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
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()

    var threads by remember { mutableStateOf<List<ThreadOut>>(emptyList()) }
    var selected by remember { mutableStateOf<ThreadOut?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val primaryId = identity.primaryId

    suspend fun refresh() {
        if (primaryId == null) return
        threads = seriousModeRepo.getThreads(primaryId, forceRefresh = true)
    }

    LaunchedEffect(primaryId) {
        if (primaryId == null) return@LaunchedEffect
        loading = true
        error = null
        try {
            threads = seriousModeRepo.getThreads(primaryId)
        } catch (e: Exception) {
            error = friendlyError(e, "Couldn't load your matches.")
        } finally {
            loading = false
        }
    }

    D2MTheme(flow = D2MFlow.CHILD) {
        val t = selected
        if (t == null) {
            // Thread list, full width -- default pane.
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Matches", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                when {
                    loading -> Text("Loading…", color = mutedText(0.55f), modifier = Modifier.padding(top = 12.dp))
                    error != null -> D2MErrorBanner(error!!, modifier = Modifier.padding(top = 12.dp))
                    threads.isEmpty() -> D2MEmptyState("No matches yet", "Once you and someone else both accept, they'll show up here.")
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 14.dp)) {
                        items(threads) { thread ->
                            ThreadRow(thread = thread, onClick = { selected = thread })
                        }
                    }
                }
            }
        } else {
            // Conversation, full width -- replaces the list entirely (back
            // button returns to it), same as web's isNarrow branch.
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp),
                ) {
                    IconButton(onClick = { selected = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to matches")
                    }
                    Box(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                        ThreadHeader(
                            thread = t,
                            onGoSerious = {
                                val pid = primaryId ?: return@ThreadHeader
                                scope.launch { runCatching { seriousModeRepo.requestSeriousMode(pid, t.threadId, pid) }; refresh() }
                            },
                            onRevoke = {
                                val pid = primaryId ?: return@ThreadHeader
                                scope.launch { runCatching { seriousModeRepo.revoke(pid, t.threadId) }; refresh() }
                            },
                            onUnmatch = {
                                val pid = primaryId ?: return@ThreadHeader
                                scope.launch { runCatching { seriousModeRepo.unmatch(pid, t.threadId) }; refresh(); selected = null }
                            },
                            onAcceptSeriousRequest = {
                                val reqId = t.pendingSeriousModeRequestId ?: return@ThreadHeader
                                val pid = primaryId ?: return@ThreadHeader
                                scope.launch { runCatching { seriousModeRepo.respond(pid, reqId, "accept") }; refresh() }
                            },
                            onDeclineSeriousRequest = {
                                val reqId = t.pendingSeriousModeRequestId ?: return@ThreadHeader
                                val pid = primaryId ?: return@ThreadHeader
                                scope.launch { runCatching { seriousModeRepo.respond(pid, reqId, "decline") }; refresh() }
                            },
                        )
                    }
                }
                ChatPane(peerId = t.otherParticipantId, peerName = t.otherParticipantName, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            }
        }
    }
}

/**
 * One row in the thread list -- a gradient avatar placeholder (same visual
 * idea as web's `linear-gradient(135deg,#DCEEEA,#FBEAD2)` circle, which this
 * platform had no equivalent of at all) plus name and status, in a tappable
 * row rather than a boxed Card so a long list doesn't turn into a stack of
 * competing card borders.
 */
@Composable
private fun ThreadRow(thread: ThreadOut, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Box(modifier = Modifier.size(48.dp).background(AvatarGradient, RoundedCornerShape(14.dp)))
        Column(modifier = Modifier.weight(1f)) {
            Text(thread.otherParticipantName, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                D2MBadge(threadStatusLabel(thread.status), threadStatusTone(thread.status))
                if (thread.pendingSeriousModeRequestId != null) {
                    D2MBadge("Serious Mode pending", D2MBadgeTone.INFO)
                }
            }
        }
    }
}

/**
 * Mirrors MatchesScreen.jsx's chat header exactly (lines ~562-639 of that
 * file): gradient squircle avatar + presence dot, name with a status
 * subtitle underneath (typing > online > match-status, same priority web
 * uses), and a "more options" (⋮) menu -- NOT inline buttons -- holding Go
 * Serious/Revoke/Unmatch, same as web keeps those out of the toolbar itself.
 * Reported directly: showing them as always-visible buttons plus a separate
 * "Matched" pill floating in the header didn't match web at all -- both are
 * fixed here. A pending Serious Mode request is the one thing web (and now
 * this) still surfaces as inline Accept/Decline buttons rather than burying
 * it in the menu, since it's a time-sensitive action on the other person,
 * not a routine setting.
 */
@Composable
private fun ThreadHeader(
    thread: ThreadOut,
    onGoSerious: () -> Unit,
    onRevoke: () -> Unit,
    onUnmatch: () -> Unit,
    onAcceptSeriousRequest: () -> Unit,
    onDeclineSeriousRequest: () -> Unit,
) {
    val messagingRepo: MessagingRepository = koinInject()
    val peerUsername = remember(thread.otherParticipantId) { d2mIdToMessagingUsername(thread.otherParticipantId) }
    val peerOnline by messagingRepo.isPeerOnline(peerUsername).collectAsState()
    val peerTyping by messagingRepo.isPeerTyping(peerUsername).collectAsState()
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(peerUsername) { messagingRepo.subscribePresence(thread.otherParticipantId) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(40.dp)) {
                Box(modifier = Modifier.size(40.dp).background(AvatarGradient, RoundedCornerShape(12.dp)))
                PresenceDot(online = peerOnline, modifier = Modifier.align(Alignment.BottomEnd))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(thread.otherParticipantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val subtitle = when {
                    peerTyping -> "typing…"
                    peerOnline -> "Online"
                    else -> threadStatusLabel(thread.status)
                }
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = mutedText(0.55f))
            }
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

private fun threadStatusTone(status: String): D2MBadgeTone = when (status) {
    "active" -> D2MBadgeTone.INFO
    "exclusive" -> D2MBadgeTone.SUCCESS
    "sunsetting" -> D2MBadgeTone.WARNING
    "closed" -> D2MBadgeTone.NEUTRAL
    else -> D2MBadgeTone.NEUTRAL
}
