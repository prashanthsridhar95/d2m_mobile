package com.d2m.app.ui.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Videocam
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
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.messaging.ChatUiState
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.ParentContact
import com.d2m.app.messaging.ParentContactsStore
import com.d2m.app.messaging.call.CallManager
import com.d2m.app.messaging.contactQualifier
import com.d2m.app.messaging.d2mIdToMessagingUsername
import com.d2m.app.messaging.ui.ArchivePinDialog
import com.d2m.app.messaging.ui.ChatPane
import com.d2m.app.ui.components.BackHandlerCompat
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import com.d2m.app.ui.theme.d2m
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/*
 * Retheme pass: the avatar placeholder was a fixed teal-to-cream gradient
 * (#DCEEEA -> #FBEAD2), left over from the old per-flow palettes and the
 * one place in this app with a colour baked in rather than read from the
 * theme. It's now the same warm sunken surface a missing photograph gets
 * everywhere else (see ProfilePhoto.kt), so an avatar with no photo and a
 * card with no photo read as the same material. A composable accessor
 * rather than a top-level val, since a CompositionLocal can only be read
 * inside composition.
 */
@Composable
private fun avatarPlaceholder() = d2m.surfaceSunken


/**
 * Mirrors screens/parent/ParentMessagesScreen.jsx -- real E2E chat (same
 * MessagingRepository/ChatPane machinery the Child flow uses, see
 * MatchesScreen.kt), not the "coming in a later release" placeholder this
 * screen used to be. Reported directly on web before this existed there
 * either: "messages framework is not integrated. it's just no improvement
 * there" -- ported here once the underlying machinery was mature enough to
 * reuse rather than rebuild.
 *
 * Only Parent-to-Parent messaging is wired up in this pass ("Message their
 * parent" from ProfileDetailScreen.kt, open to any candidate's Sponsor with
 * no request/accept gate -- matches web's own stated design, see that
 * screen's comment). Parent-to-Child messaging is deliberately NOT included
 * here yet: on web it only unlocks once a candidate reaches
 * `source === "consent_unlocked"`, which requires the granted-consent card
 * flow (ConsentRequestsScreen/DiscoverScreen's synthesized candidate) that
 * hasn't been ported to mobile at all (ConsentApi.kt exists at the data
 * layer, but there's no screen consuming it yet) -- wiring a "Message the
 * child" button without that real gate in place would let a parent message
 * a child before consent was ever granted, which is a safety regression,
 * not a shortcut. Revisit once the consent-request UI lands on mobile.
 *
 * No Serious-Mode-style action row (Go Serious/Revoke/Unmatch, see
 * MatchesScreen.kt's ConversationHeader) -- those are child-specific
 * concepts tied to d2m_core_engine's Thread model, which doesn't exist for
 * a parent-to-parent conversation (confirmed: no backend Sponsor-to-Sponsor
 * thread concept anywhere). A parent conversation is just open-ended chat
 * plus calling.
 */
@Composable
fun ParentMessagesScreen() {
    val identityStore: IdentityStore = koinInject()
    val messagingRepo: MessagingRepository = koinInject()
    val contactsStore: ParentContactsStore = koinInject()
    val chatUiState: ChatUiState = koinInject()
    val callManager: CallManager = koinInject()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()

    val sponsorId = identity.sponsorId

    D2MTheme(flow = D2MFlow.PARENT) {
        if (sponsorId == null) {
            // Mirrors web's `if (!identity?.sponsorId) return <Navigate to="/login" />` --
            // this route is only ever reachable once logged in as a Parent, so this is
            // a defensive empty state rather than a real navigation target.
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                D2MEmptyState("Not signed in", "Log in as a parent to see your messages.")
            }
            return@D2MTheme
        }

        val myUsername = remember(sponsorId) { d2mIdToMessagingUsername(sponsorId) }
        LaunchedEffect(myUsername) { contactsStore.attach(myUsername) }
        val contacts by contactsStore.contacts.collectAsState()

        // Same fix as MatchesScreen.kt: feed every known contact's real name
        // into messagingRepo as soon as the contacts list is available, so a
        // notification for a peer this device hasn't opened a chat with yet
        // (this session) still shows their real name instead of falling back
        // to the raw messaging username -- see rememberPeerName's doc comment.
        LaunchedEffect(contacts) {
            contacts.values.forEach { messagingRepo.rememberPeerName(it.d2mId, it.name) }
        }

        var selectedUsername by remember { mutableStateOf<String?>(null) }

        // Same "hide the bottom tab bar while a conversation is open" flag
        // the Child flow uses -- ChatUiState is a shared Koin singleton, not
        // role-specific, so this Just Works alongside MatchesScreen.kt's
        // identical wiring.
        LaunchedEffect(selectedUsername) { chatUiState.setConversationOpen(selectedUsername != null) }
        DisposableEffect(Unit) { onDispose { chatUiState.setConversationOpen(false) } }

        // Same system-back fix as MatchesScreen.kt -- see
        // BackHandlerCompat.kt's doc comment.
        BackHandlerCompat(enabled = selectedUsername != null) { selectedUsername = null }

        // A tapped in-app notification banner or a "Message their parent"
        // button records which peer to jump to -- opens it directly, same
        // as MatchesScreen.kt's identical pendingOpenPeer effect. Unlike
        // that screen, there's no backend thread list to match against: a
        // peerUsername is enough to open a conversation here (the header
        // falls back to the raw username if it isn't in `contacts` yet --
        // e.g. this Sponsor messaged first, before this device saved a
        // contact entry for them).
        val pendingOpenPeer by chatUiState.pendingOpenPeerUsername.collectAsState()
        LaunchedEffect(pendingOpenPeer) {
            val pending = pendingOpenPeer ?: return@LaunchedEffect
            selectedUsername = pending
            chatUiState.clearPendingOpenPeer()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val sel = selectedUsername
            if (sel == null) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    PageTitle("Messages")
                    if (contacts.isEmpty()) {
                        D2MEmptyState(
                            title = "No conversations yet",
                            subtitle = "Open a profile and use \"Message their parent\" to start one.",
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    } else {
                        // Most-recently-active first -- max of "when was this
                        // contact registered" and "when did the last message
                        // actually land", since (unlike the Child flow's
                        // backend-authoritative Thread list) there's no
                        // server-side thread concept here to sort by at all,
                        // see ParentContactsStore.kt's doc comment.
                        val sorted = remember(contacts) {
                            contacts.entries.sortedByDescending { (username, contact) ->
                                maxOf(contact.updatedAt, messagingRepo.lastMessageAt(username) ?: 0L)
                            }
                        }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 14.dp)) {
                            items(sorted, key = { it.key }) { entry ->
                                ParentContactRow(
                                    peerUsername = entry.key,
                                    contact = entry.value,
                                    onClick = { selectedUsername = entry.key },
                                )
                            }
                        }
                    }
                }
            } else {
                val contact = contacts[sel]
                // Idempotent-transform trick used throughout this codebase
                // (see MessageReplyReceiver.kt's doc comment): d2mIdToMessagingUsername
                // strips hyphens + lowercases, which is a no-op on a string
                // that's already in that form -- so passing `sel` straight
                // through as a stand-in "peerD2mId" resolves back to itself
                // whenever a real raw id isn't on hand (a contact we've only
                // ever received a message from, never explicitly registered).
                val peerD2mId = contact?.d2mId ?: sel
                val displayName = contact?.name ?: sel
                Column(modifier = Modifier.fillMaxSize()) {
                    ParentConversationHeader(
                        peerD2mId = peerD2mId,
                        name = displayName,
                        kind = contact?.kind,
                        childName = contact?.childName,
                        onBack = { selectedUsername = null },
                        onStartAudioCall = { scope.launch { callManager.startCall(sel, "audio") } },
                        onStartVideoCall = { scope.launch { callManager.startCall(sel, "video") } },
                    )
                    ChatPane(
                        peerId = peerD2mId,
                        peerName = displayName,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp).imePadding(),
                    )
                }
            }
            // Mounted unconditionally, same as MatchesScreen.kt's identical wiring --
            // no-ops until messagingRepo.archivePrompt is actually set.
            ArchivePinDialog(messagingRepo)
        }
    }
}

/** One row in the contact list -- gradient avatar + presence dot, name, qualifier ("Deepak's parent"), and a live last-message preview. */
@Composable
private fun ParentContactRow(peerUsername: String, contact: ParentContact, onClick: () -> Unit) {
    val messagingRepo: MessagingRepository = koinInject()
    val peerOnline by remember(peerUsername) { messagingRepo.isPeerOnline(peerUsername) }.collectAsState()
    val peerTyping by remember(peerUsername) { messagingRepo.isPeerTyping(peerUsername) }.collectAsState()
    val messages by remember(peerUsername) { messagingRepo.messagesFor(peerUsername) }.collectAsState()

    LaunchedEffect(peerUsername) { messagingRepo.subscribePresence(contact.d2mId) }

    val last = messages.maxByOrNull { it.sentAt }
    val preview = when {
        last == null -> "No messages yet"
        last.deleted -> "This message was deleted"
        last.media != null -> "Sent an attachment"
        else -> last.text
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            Box(modifier = Modifier.size(48.dp).background(avatarPlaceholder(), RoundedCornerShape(14.dp)))
            ParentPresenceDot(online = peerOnline, modifier = Modifier.align(Alignment.BottomEnd))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            contactQualifier(contact.kind, contact.childName)?.let {
                Text(it.replaceFirstChar { c -> c.uppercase() }, style = MaterialTheme.typography.labelSmall, color = mutedText(0.55f))
            }
            Text(
                if (peerTyping) "typing…" else preview,
                style = MaterialTheme.typography.bodySmall,
                color = if (peerTyping) MaterialTheme.colorScheme.primary else mutedText(0.55f),
                fontWeight = if (peerTyping) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Full conversation header -- back, avatar + presence, name/qualifier/status stack, then audio/video call buttons. Same one-row layout as MatchesScreen.kt's ConversationHeader, minus the Serious Mode "more options" menu (no thread state machine applies to a parent-to-parent conversation). */
@Composable
private fun ParentConversationHeader(
    peerD2mId: String,
    name: String,
    kind: String?,
    childName: String?,
    onBack: () -> Unit,
    onStartAudioCall: () -> Unit,
    onStartVideoCall: () -> Unit,
) {
    val messagingRepo: MessagingRepository = koinInject()
    val peerUsername = remember(peerD2mId) { d2mIdToMessagingUsername(peerD2mId) }
    val peerOnline by messagingRepo.isPeerOnline(peerUsername).collectAsState()
    val peerTyping by messagingRepo.isPeerTyping(peerUsername).collectAsState()

    LaunchedEffect(peerD2mId) { messagingRepo.subscribePresence(peerD2mId) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to messages")
        }
        Box(modifier = Modifier.size(40.dp)) {
            Box(modifier = Modifier.size(40.dp).background(avatarPlaceholder(), RoundedCornerShape(12.dp)))
            ParentPresenceDot(online = peerOnline, modifier = Modifier.align(Alignment.BottomEnd))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val qualifier = contactQualifier(kind, childName)
            val subtitle = when {
                peerTyping -> "typing…"
                peerOnline -> "Online"
                qualifier != null -> qualifier.replaceFirstChar { it.uppercase() }
                else -> "Offline"
            }
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = mutedText(0.55f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ParentHeaderIconButton(icon = Icons.Filled.Call, contentDescription = "Audio call", onClick = onStartAudioCall)
            ParentHeaderIconButton(icon = Icons.Filled.Videocam, contentDescription = "Video call", onClick = onStartVideoCall)
        }
    }
}

@Composable
private fun ParentHeaderIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(38.dp)
            .clip(CircleShape)
            .border(androidx.compose.foundation.BorderStroke(1.dp, mutedText(0.15f)), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun ParentPresenceDot(online: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(11.dp)
            .background(if (online) LocalD2MStatusPalette.current.success.fg else mutedText(0.3f), CircleShape)
            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
    )
}
