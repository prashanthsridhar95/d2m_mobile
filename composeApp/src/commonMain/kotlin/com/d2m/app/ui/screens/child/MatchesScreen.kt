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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
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
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.model.UnionStatusOut
import com.d2m.app.domain.repository.OffboardingRepository
import com.d2m.app.domain.repository.SuggestionsRepository
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.D2MCheckboxRow
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonSize
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MSkeleton
import com.d2m.app.ui.components.D2MTabs
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import com.d2m.app.ui.theme.d2m
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
// See ApiCache.kt's import comment -- deprecated kotlinx.datetime.Clock
// typealias, actually resolves to kotlin.time.Clock in the 0.7.1 that this
// project really compiles against.
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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

// Shared loading placeholder for this screen's three list tabs (Matches,
// Received, Sent) -- mirrors ThreadRow's own avatar-plus-two-lines shape
// so the list doesn't visibly reflow once real rows replace it.
@Composable
private fun MatchesListSkeleton(count: Int = 3) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 14.dp)) {
        repeat(count) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(48.dp).background(avatarPlaceholder(), RoundedCornerShape(14.dp)))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    D2MSkeleton(width = 140.dp, height = 13.dp)
                    D2MSkeleton(width = 90.dp, height = 11.dp)
                }
            }
        }
    }
}


private val SHORT_MONTH_NAMES = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private fun localDateOf(epochMillis: Long): LocalDate =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date

/**
 * "In matches view, timestamp is not shown - only last message is shown."
 * Direct port of web's MatchesScreen.jsx fmtThreadListTime: today shows a
 * time, yesterday says so, anything older shows a short date -- same
 * today/yesterday/date ladder as ChatPane.kt's own dayLabel/formatBubbleTime
 * (private to that file, so re-implemented here rather than exported across
 * an unrelated module boundary for one shared helper).
 */
private fun fmtThreadListTime(epochMillis: Long): String {
    if (epochMillis <= 0) return ""
    val now = Clock.System.now().toEpochMilliseconds()
    val date = localDateOf(epochMillis)
    if (date == localDateOf(now)) {
        val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
        val hour12 = when (val h = dt.hour % 12) { 0 -> 12; else -> h }
        val amPm = if (dt.hour < 12) "AM" else "PM"
        return "$hour12:${dt.minute.toString().padStart(2, '0')} $amPm"
    }
    if (date == localDateOf(now - 86_400_000L)) return "Yesterday"
    return "${SHORT_MONTH_NAMES[date.monthNumber - 1]} ${date.dayOfMonth}"
}

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
fun MatchesScreen(onOpenProfile: (String) -> Unit = {}, onOpenGallery: () -> Unit = {}) {
    val identityStore = org.koin.compose.koinInject<IdentityStore>()
    val seriousModeRepo = org.koin.compose.koinInject<SeriousModeRepository>()
    val chatUiState = org.koin.compose.koinInject<ChatUiState>()
    val callManager = org.koin.compose.koinInject<CallManager>()
    val messagingRepo = org.koin.compose.koinInject<MessagingRepository>()
    val suggestionsRepo = org.koin.compose.koinInject<SuggestionsRepository>()
    val offboardingRepo = org.koin.compose.koinInject<OffboardingRepository>()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()

    // "Integrate offboarding flow" (reported directly) -- the union-confirm
    // + Success Gallery opt-in banner, mirroring web's MatchesScreen.jsx
    // treatment exactly (see that file's own loadUnionStatus/
    // handleConfirmUnion/handleGalleryToggle). OffboardingRepository was
    // already fully wired on this platform with zero UI callers before
    // this -- this is that wiring.
    var unionStatus by remember { mutableStateOf<UnionStatusOut?>(null) }
    var unionBusy by remember { mutableStateOf(false) }
    var unionError by remember { mutableStateOf<String?>(null) }

    // "Include request sent/request received profiles sections in matches
    // tab" (reported directly) -- a Thread only exists once BOTH sides
    // have accepted, so a pending one-sided Accept has nowhere to show up
    // in this screen today. Same two endpoints HomeScreen already renders
    // inline on its own activity feed, surfaced here as their own tab
    // instead -- this screen's list is already a flat list of one kind of
    // thing (threads), and a request isn't a thread yet.
    var matchesTab by remember { mutableStateOf(0) } // 0 = matches, 1 = received, 2 = sent
    var receivedRequests by remember { mutableStateOf<List<SuggestionOut>?>(null) }
    var sentRequests by remember { mutableStateOf<List<SuggestionOut>?>(null) }
    var requestsError by remember { mutableStateOf<String?>(null) }
    var requestBusyId by remember { mutableStateOf<String?>(null) }

    var threads by remember { mutableStateOf<List<ThreadOut>>(emptyList()) }
    // "open profile & trigger system back - moves to matches page - should
    // go to chat only." `selected` used to be its own plain `remember` --
    // see ChatUiState.kt's activeThreadId doc comment for why that doesn't
    // survive navigating to the avatar-tapped profile and back. Deriving it
    // from the persisted id + the loaded thread list instead means this
    // screen re-finds the right thread on every recomposition, including
    // the fresh one Compose Navigation creates when this screen is
    // recomposed after being disposed. Every place that used to write
    // `selected = ...` now calls chatUiState.setActiveThreadId(...) instead.
    val activeThreadId by chatUiState.activeThreadId.collectAsState()
    val selected = remember(activeThreadId, threads) { threads.firstOrNull { it.threadId == activeThreadId } }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }

    val primaryId = identity.primaryId

    // Union status only means anything once a thread is exclusive, and
    // getUnionStatus is a read-only peek (never creates a row) -- safe to
    // refetch every time the active exclusive thread changes.
    suspend fun loadUnionStatus() {
        val pid = primaryId
        val t = selected
        if (pid == null || t == null) return
        try {
            unionStatus = offboardingRepo.getUnionStatus(pid, t.otherParticipantId)
        } catch (e: Exception) {
            // leave the previous value in place -- a stale-but-present
            // status is less confusing than the banner vanishing on a
            // transient error.
        }
    }

    LaunchedEffect(selected?.threadId, selected?.status, primaryId) {
        val t = selected
        if (t == null || t.status != "exclusive") {
            unionStatus = null
            return@LaunchedEffect
        }
        loadUnionStatus()
    }

    fun confirmUnion() {
        val pid = primaryId ?: return
        val t = selected ?: return
        unionBusy = true
        unionError = null
        scope.launch {
            try {
                offboardingRepo.confirmUnion(pid, t.otherParticipantId)
                loadUnionStatus()
            } catch (e: Exception) {
                unionError = friendlyError(e, "Couldn't confirm the union.")
            } finally {
                unionBusy = false
            }
        }
    }

    fun toggleGalleryOptIn(consent: Boolean) {
        val pid = primaryId ?: return
        val t = selected ?: return
        unionBusy = true
        unionError = null
        scope.launch {
            try {
                offboardingRepo.galleryOptIn(pid, t.otherParticipantId, consent)
                loadUnionStatus()
            } catch (e: Exception) {
                unionError = friendlyError(e, "Couldn't update gallery opt-in.")
            } finally {
                unionBusy = false
            }
        }
    }

    val iAmUnionA = unionStatus?.primaryAId == primaryId
    val myUnionConfirmed = unionStatus?.let { if (iAmUnionA) it.confirmedA else it.confirmedB } ?: false
    // Banner only ever appears once someone has actually asked -- see
    // web's MatchesScreen.jsx comment on the same gate: getUnionStatus
    // synthesizes a "pending" row (both sides false) for every exclusive
    // thread whether or not anyone's touched Confirm Union yet.
    val otherUnionConfirmed = unionStatus?.let { if (iAmUnionA) it.confirmedB else it.confirmedA } ?: false
    val myGalleryConsent = unionStatus?.let { if (iAmUnionA) it.galleryConsentA else it.galleryConsentB } ?: false

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

    suspend fun refreshReceived() {
        if (primaryId == null) return
        try {
            receivedRequests = suggestionsRepo.getReceivedRequests(primaryId)
        } catch (e: Exception) {
            requestsError = friendlyError(e, "Couldn't load received requests.")
        }
    }

    LaunchedEffect(primaryId) {
        if (primaryId == null) return@LaunchedEffect
        refreshReceived()
        try {
            sentRequests = suggestionsRepo.getSentRequests(primaryId)
        } catch (e: Exception) {
            requestsError = friendlyError(e, "Couldn't load sent requests.")
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
    //
    // "tapping on the user avatar & opening the profile & pressing back
    // button takes me to matches. but it should be taking me back to the
    // chat only." Root cause: this composable's back handler was gated only
    // on `selected != null`, which stays true the whole time a profile
    // pushed on top of it (via onOpenProfile -> navController.navigate(...))
    // is showing -- opening a profile never clears `selected`. Compose
    // Navigation does NOT dispose this screen's composition while it's
    // merely stopped underneath another destination, so this BackHandler
    // stays registered AND enabled the entire time ProfileDetailScreen is on
    // top. androidx's OnBackPressedDispatcher has no awareness of which
    // route is actually visible -- it just calls the most-recently-added
    // enabled callback -- so pressing system back while looking at the
    // profile was silently caught here instead: `selected = null` fired
    // (invisibly, since ProfileDetailScreen was still the current nav
    // destination), consuming that back press entirely. The SECOND back
    // press then had nothing left enabled here (selected was already null)
    // and fell through to NavController, which popped ProfileDetailScreen --
    // landing back on this screen, but by then `selected` had already been
    // reset, so it showed the bare thread list instead of the chat.
    // isResumed (via the current NavBackStackEntry's own lifecycle, which
    // Compose Navigation DOES correctly move between RESUMED/STARTED/CREATED
    // as the back stack changes) closes that gap: this handler only
    // intercepts back while this screen is actually the front-most,
    // resumed destination, exactly like every other BackHandler using this
    // check is expected to.
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val isResumed = lifecycleState == Lifecycle.State.RESUMED
    BackHandlerCompat(enabled = selected != null && isResumed) { chatUiState.setActiveThreadId(null) }

    // A tapped in-app notification banner (messaging/ui/InAppNotificationLayer.kt)
    // records which peer to jump to -- once this screen's thread list is
    // loaded, find the matching thread and open it directly.
    val pendingOpenPeer by chatUiState.pendingOpenPeerUsername.collectAsState()
    LaunchedEffect(pendingOpenPeer, threads) {
        val pending = pendingOpenPeer ?: return@LaunchedEffect
        val match = threads.firstOrNull { d2mIdToMessagingUsername(it.otherParticipantId) == pending }
        if (match != null) {
            chatUiState.setActiveThreadId(match.threadId)
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
                PageTitle("Matches")
                D2MTabs(
                    titles = listOf("Matches", "Received", "Sent"),
                    selectedIndex = matchesTab,
                    onSelect = { matchesTab = it },
                    modifier = Modifier.padding(top = 10.dp),
                )
                when (matchesTab) {
                    0 -> when {
                        loading -> MatchesListSkeleton()
                        error != null -> D2MErrorBanner(error!!, modifier = Modifier.padding(top = 12.dp))
                        visibleThreads.isEmpty() -> D2MEmptyState("No matches yet", "Once you and someone else both accept, they'll show up here.")
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 14.dp)) {
                            items(visibleThreads) { thread ->
                                ThreadRow(thread = thread, onClick = { chatUiState.setActiveThreadId(thread.threadId) })
                            }
                        }
                    }
                    1 -> {
                        requestsError?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 12.dp)) }
                        when {
                            receivedRequests == null -> MatchesListSkeleton()
                            receivedRequests!!.isEmpty() -> D2MEmptyState("No requests waiting", "When someone sends you a request, it shows up here.")
                            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 14.dp)) {
                                items(receivedRequests!!, key = { it.candidateId }) { r ->
                                    val busy = requestBusyId == r.candidateId
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Box(modifier = Modifier.size(48.dp).background(avatarPlaceholder(), RoundedCornerShape(14.dp)))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(r.candidateName, style = MaterialTheme.typography.titleSmall)
                                            Text("Wants to match with you", style = MaterialTheme.typography.bodySmall, color = mutedText(0.55f))
                                        }
                                        D2MButton(
                                            text = "Accept", size = D2MButtonSize.SM, enabled = !busy,
                                            onClick = {
                                                val pid = primaryId ?: return@D2MButton
                                                requestBusyId = r.candidateId
                                                scope.launch {
                                                    try {
                                                        suggestionsRepo.act(pid, r.candidateId, "accept")
                                                        refreshReceived()
                                                    } catch (e: Exception) {
                                                        requestsError = friendlyError(e, "Couldn't accept that request.")
                                                    } finally {
                                                        requestBusyId = null
                                                    }
                                                }
                                            },
                                        )
                                        D2MButton(
                                            text = "Decline", variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM, enabled = !busy,
                                            onClick = {
                                                val pid = primaryId ?: return@D2MButton
                                                requestBusyId = r.candidateId
                                                scope.launch {
                                                    try {
                                                        suggestionsRepo.act(pid, r.candidateId, "reject")
                                                        refreshReceived()
                                                    } catch (e: Exception) {
                                                        requestsError = friendlyError(e, "Couldn't decline that request.")
                                                    } finally {
                                                        requestBusyId = null
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> when {
                        sentRequests == null -> MatchesListSkeleton()
                        sentRequests!!.isEmpty() -> D2MEmptyState("No requests sent", "Accept a suggestion from Discover to send a request.")
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 14.dp)) {
                            items(sentRequests!!, key = { it.candidateId }) { r ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.size(48.dp).background(avatarPlaceholder(), RoundedCornerShape(14.dp)))
                                    Column {
                                        Text(r.candidateName, style = MaterialTheme.typography.titleSmall)
                                        Text("Waiting for a reply", style = MaterialTheme.typography.bodySmall, color = mutedText(0.55f))
                                    }
                                }
                            }
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
                    onBack = { chatUiState.setActiveThreadId(null) },
                    onOpenProfile = { onOpenProfile(t.otherParticipantId) },
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
                                        chatUiState.setActiveThreadId(null)
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
                            showConfirmUnion = t.status == "exclusive" && !myUnionConfirmed,
                            onConfirmUnion = { confirmUnion() },
                        )
                    },
                )
                actionError?.let { D2MErrorBanner(it, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
                unionError?.let { D2MErrorBanner(it, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }

                // Banner only ever appears once someone has actually asked
                // (myUnionConfirmed or otherUnionConfirmed) -- see the
                // comment where those are computed above for why gating on
                // unionStatus alone would nag both sides indefinitely just
                // for being exclusive. "More options > Confirm Union" is
                // still there for whoever wants to actually start it.
                if (t.status == "exclusive" && unionStatus?.status != "confirmed" && (myUnionConfirmed || otherUnionConfirmed)) {
                    D2MCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            if (myUnionConfirmed) {
                                Text("Waiting on ${t.otherParticipantName} to confirm your union.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("${t.otherParticipantName} wants to confirm your union.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                D2MButton(text = "Confirm Union", size = D2MButtonSize.SM, enabled = !unionBusy, onClick = { confirmUnion() })
                            }
                        }
                    }
                }

                if (t.status == "exclusive" && unionStatus?.status == "confirmed") {
                    D2MCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Union confirmed 🎉", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Share your story in the Success Gallery? Anonymized (5-year age buckets only) -- requires both sides to opt in, and you can revoke any time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = mutedText(0.55f),
                                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
                            )
                            D2MCheckboxRow(
                                label = "Opt in to the Success Gallery",
                                checked = myGalleryConsent,
                                enabled = !unionBusy,
                                onCheckedChange = { toggleGalleryOptIn(it) },
                            )
                            D2MButton(
                                text = "View the Success Gallery",
                                variant = D2MButtonVariant.OUTLINE,
                                size = D2MButtonSize.SM,
                                modifier = Modifier.padding(top = 10.dp),
                                onClick = onOpenGallery,
                            )
                        }
                    }
                }
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
            Box(modifier = Modifier.size(48.dp).background(avatarPlaceholder(), RoundedCornerShape(14.dp)))
            PresenceDot(online = peerOnline, modifier = Modifier.align(Alignment.BottomEnd))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(thread.otherParticipantName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (lastMessage != null) {
                    Text(
                        fmtThreadListTime(lastMessage.sentAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedText(0.55f),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
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
    onOpenProfile: () -> Unit,
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
        // "tapping the user avatar inside chat is not taking me to the
        // user's profile" -- matches web's own header (MatchesScreen.jsx:
        // the avatar+name block is a role="button" that navigates to
        // /child/profile/:id) -- this whole group had no tap handler at all
        // before, avatar or name.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).clickable(onClick = onOpenProfile),
        ) {
            Box(modifier = Modifier.size(40.dp)) {
                Box(modifier = Modifier.size(40.dp).background(avatarPlaceholder(), RoundedCornerShape(12.dp)))
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
    showConfirmUnion: Boolean = false,
    onConfirmUnion: () -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        // "more menu button on the toolbar also should have rounded
        // outline" -- was a bare IconButton (no outline at all), visibly
        // inconsistent with the two call buttons right next to it
        // (HeaderIconButton's 38dp outlined circle). Same visual now.
        HeaderIconButton(icon = Icons.Filled.MoreVert, contentDescription = "More options", onClick = { menuOpen = true })
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, shape = RoundedCornerShape(D2MRadius.lg)) {
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
            if (showConfirmUnion) {
                DropdownMenuItem(text = { Text("Confirm Union") }, onClick = { menuOpen = false; onConfirmUnion() })
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
            .background(if (online) LocalD2MStatusPalette.current.success.fg else mutedText(0.3f), CircleShape)
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
