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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.d2m.app.ui.components.D2MErrorState
import com.d2m.app.ui.components.SkeletonList
import com.d2m.app.ui.components.bounceClickable
import com.d2m.app.ui.components.rememberHaptics
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
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.strings.MatchesStrings
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
private fun fmtThreadListTime(epochMillis: Long, yesterdayLabel: String): String {
    if (epochMillis <= 0) return ""
    val now = Clock.System.now().toEpochMilliseconds()
    val date = localDateOf(epochMillis)
    if (date == localDateOf(now)) {
        val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
        val hour12 = when (val h = dt.hour % 12) { 0 -> 12; else -> h }
        val amPm = if (dt.hour < 12) "AM" else "PM"
        return "$hour12:${dt.minute.toString().padStart(2, '0')} $amPm"
    }
    if (date == localDateOf(now - 86_400_000L)) return yesterdayLabel
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
@OptIn(ExperimentalMaterial3Api::class)
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
    val haptics = rememberHaptics()
    var pulling by remember { mutableStateOf(false) }
    val unreadByPeer by messagingRepo.unreadByPeer.collectAsState()

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
    val strings = LocalStrings.current.matches

    // getReceivedRequests/getSentRequests can lag a beat behind an actual
    // mutual match -- same staleness web's HomeScreen.jsx/MatchesScreen.jsx
    // already work around via their own matchedIds/receivedFiltered.
    // Cross-checked against threads (the authoritative "are we matched"
    // source this screen already loads) so an already-matched candidate
    // never lingers in Received/Sent with live Accept/Decline buttons.
    // Reported directly: "After accepting a request, in message screen,
    // request is being shown."
    val matchedIds = remember(threads) {
        threads.filter { it.status != "closed" }.map { it.otherParticipantId }.toSet()
    }
    val receivedFiltered = remember(receivedRequests, matchedIds) {
        receivedRequests?.filter { it.candidateId !in matchedIds } ?: emptyList()
    }
    val sentFiltered = remember(sentRequests, matchedIds) {
        sentRequests?.filter { it.candidateId !in matchedIds } ?: emptyList()
    }

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
                unionError = friendlyError(e, strings.errConfirmUnion)
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
                unionError = friendlyError(e, strings.errUpdateGalleryOptin)
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
            actionError = friendlyError(e, strings.errRefreshMatches)
        }
    }

    // "Serious mode/Union/We met request not getting updated in real time
    // inside chat," reported directly -- messaging-framework pushes a
    // thread.updated event the moment a Serious Mode milestone changes on
    // either side (see d2m_core_engine's serious_mode_service.py). Just
    // calls the same refresh() this screen already runs on load/manual
    // pull -- no new fetch logic, just no longer waiting for one of those
    // to happen on its own.
    LaunchedEffect(Unit) {
        messagingRepo.threadUpdatedEvents.collect { refresh() }
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
            error = friendlyError(e, strings.errLoadMatches)
        } finally {
            loading = false
        }
    }

    suspend fun refreshReceived() {
        if (primaryId == null) return
        try {
            receivedRequests = suggestionsRepo.getReceivedRequests(primaryId)
        } catch (e: Exception) {
            requestsError = friendlyError(e, strings.errLoadReceivedRequests)
        }
    }

    LaunchedEffect(primaryId) {
        if (primaryId == null) return@LaunchedEffect
        refreshReceived()
        try {
            sentRequests = suggestionsRepo.getSentRequests(primaryId)
        } catch (e: Exception) {
            requestsError = friendlyError(e, strings.errLoadSentRequests)
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
    // Most recent conversation first; brand-new matches (no messages yet) pin
    // to the top so they get a first hello. Re-sorts when a message arrives
    // (unreadByPeer changes), not on every frame.
    val sortedThreads = remember(visibleThreads, unreadByPeer) {
        visibleThreads.sortedByDescending {
            messagingRepo.lastMessageAt(d2mIdToMessagingUsername(it.otherParticipantId)) ?: Long.MAX_VALUE
        }
    }

    D2MTheme(flow = D2MFlow.CHILD) {
      Box(modifier = Modifier.fillMaxSize()) {
        val t = selected
        if (t == null) {
            // Thread list, full width -- default pane.
            Column(modifier = Modifier.fillMaxSize()) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                    PageTitle(strings.pageTitle)
                    D2MTabs(
                        titles = listOf(strings.tabMatches, strings.tabReceived, strings.tabSent),
                        selectedIndex = matchesTab,
                        onSelect = { if (it != matchesTab) { haptics.light(); matchesTab = it } },
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                PullToRefreshBox(
                    isRefreshing = pulling,
                    onRefresh = {
                        scope.launch {
                            pulling = true
                            refresh()
                            refreshReceived()
                            runCatching { sentRequests = suggestionsRepo.getSentRequests(primaryId ?: return@runCatching) }
                            pulling = false
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                    ) {
                        when (matchesTab) {
                            0 -> when {
                                loading -> item(key = "sk0", contentType = "skeleton") { SkeletonList(count = 7) }
                                error != null -> item(key = "err0", contentType = "state") {
                                    D2MErrorState(error!!, onRetry = {
                                        scope.launch { loading = true; error = null; runCatching { refresh() }; loading = false }
                                    })
                                }
                                sortedThreads.isEmpty() -> item(key = "empty0", contentType = "state") {
                                    D2MEmptyState(strings.noMatchesTitle, strings.noMatchesBody)
                                }
                                else -> items(sortedThreads, key = { it.threadId }, contentType = { "thread" }) { thread ->
                                    ThreadRow(
                                        thread = thread,
                                        unread = unreadByPeer[d2mIdToMessagingUsername(thread.otherParticipantId)] ?: 0,
                                        onClick = { chatUiState.setActiveThreadId(thread.threadId) },
                                    )
                                }
                            }
                            1 -> {
                                requestsError?.let { msg -> item(key = "reqErr", contentType = "banner") { D2MErrorBanner(msg, modifier = Modifier.padding(vertical = 8.dp)) } }
                                when {
                                    receivedRequests == null -> item(key = "sk1", contentType = "skeleton") { SkeletonList(count = 5) }
                                    receivedFiltered.isEmpty() -> item(key = "empty1", contentType = "state") {
                                        D2MEmptyState(strings.noRequestsWaitingTitle, strings.noRequestsWaitingBody)
                                    }
                                    else -> items(receivedFiltered, key = { it.candidateId }, contentType = { "received" }) { r ->
                                        val busy = requestBusyId == r.candidateId
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .bounceClickable(onClick = { onOpenProfile(r.candidateId) }, pressedScale = 0.985f, enforceMinimumTarget = false)
                                                .padding(vertical = 10.dp),
                                        ) {
                                            InitialAvatar(r.candidateName, 56.dp)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(r.candidateName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(strings.wantsToMatch, style = MaterialTheme.typography.bodySmall, color = mutedText(0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            D2MButton(
                                                text = strings.accept, size = D2MButtonSize.SM, enabled = !busy,
                                                onClick = {
                                                    val pid = primaryId ?: return@D2MButton
                                                    requestBusyId = r.candidateId
                                                    scope.launch {
                                                        try {
                                                            suggestionsRepo.act(pid, r.candidateId, "accept")
                                                            haptics.success()
                                                            refreshReceived()
                                                            refresh()
                                                        } catch (e: Exception) {
                                                            requestsError = friendlyError(e, strings.errAcceptRequest)
                                                        } finally {
                                                            requestBusyId = null
                                                        }
                                                    }
                                                },
                                            )
                                            D2MButton(
                                                text = strings.decline, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM, enabled = !busy,
                                                onClick = {
                                                    val pid = primaryId ?: return@D2MButton
                                                    requestBusyId = r.candidateId
                                                    scope.launch {
                                                        try {
                                                            suggestionsRepo.act(pid, r.candidateId, "reject")
                                                            haptics.medium()
                                                            refreshReceived()
                                                        } catch (e: Exception) {
                                                            requestsError = friendlyError(e, strings.errDeclineRequest)
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
                            else -> when {
                                sentRequests == null -> item(key = "sk2", contentType = "skeleton") { SkeletonList(count = 5) }
                                sentFiltered.isEmpty() -> item(key = "empty2", contentType = "state") {
                                    D2MEmptyState(strings.noRequestsSentTitle, strings.noRequestsSentBody)
                                }
                                else -> items(sentFiltered, key = { it.candidateId }, contentType = { "sent" }) { r ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bounceClickable(onClick = { onOpenProfile(r.candidateId) }, pressedScale = 0.985f, enforceMinimumTarget = false)
                                            .padding(vertical = 10.dp),
                                    ) {
                                        InitialAvatar(r.candidateName, 56.dp)
                                        Column {
                                            Text(r.candidateName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(strings.waitingForReply, style = MaterialTheme.typography.bodySmall, color = mutedText(0.55f))
                                        }
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
                                        actionError = friendlyError(e, strings.errSendSeriousModeRequest)
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
                                        actionError = friendlyError(e, strings.errRevokeSeriousMode)
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
                                        actionError = friendlyError(e, strings.errUnmatch)
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
                                        actionError = friendlyError(e, strings.errAcceptSeriousModeRequest)
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
                                        actionError = friendlyError(e, strings.errDeclineSeriousModeRequest)
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
                                Text(strings.waitingOnThemToConfirm(t.otherParticipantName), style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text(strings.themWantsToConfirm(t.otherParticipantName), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                D2MButton(text = strings.confirmUnion, size = D2MButtonSize.SM, enabled = !unionBusy, onClick = { confirmUnion() })
                            }
                        }
                    }
                }

                if (t.status == "exclusive" && unionStatus?.status == "confirmed") {
                    D2MCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(strings.unionConfirmedTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                strings.unionConfirmedBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = mutedText(0.55f),
                                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
                            )
                            D2MCheckboxRow(
                                label = strings.optInSuccessGallery,
                                checked = myGalleryConsent,
                                enabled = !unionBusy,
                                onCheckedChange = { toggleGalleryOptIn(it) },
                            )
                            D2MButton(
                                text = strings.viewSuccessGallery,
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
private fun ThreadRow(thread: ThreadOut, unread: Int, onClick: () -> Unit) {
    val messagingRepo: MessagingRepository = koinInject()
    val strings = LocalStrings.current.matches
    val peerUsername = remember(thread.otherParticipantId) { d2mIdToMessagingUsername(thread.otherParticipantId) }
    // remember{}: MessagingRepository.messagesFor() launches a never-cancelled
    // collector on every call, so calling it bare in the body (as this row
    // used to) leaked one per recomposition while scrolling.
    val messagesFlow = remember(peerUsername) { messagingRepo.messagesFor(peerUsername) }
    val peerOnline by messagingRepo.isPeerOnline(peerUsername).collectAsState()
    val peerTyping by messagingRepo.isPeerTyping(peerUsername).collectAsState()
    val messages by messagesFlow.collectAsState()

    LaunchedEffect(peerUsername) { messagingRepo.subscribePresence(thread.otherParticipantId) }

    val lastMessage = remember(messages) { messages.maxByOrNull { it.sentAt } }
    val previewText = remember(lastMessage) {
        when {
            lastMessage == null -> ""
            lastMessage.deleted -> strings.messageDeleted
            lastMessage.media != null -> mediaLabel(lastMessage.media)
            else -> lastMessage.text
        }
    }
    val previewWithPrefix = if (lastMessage?.isMine == true && previewText.isNotBlank()) strings.youPrefix + previewText else previewText
    val subtitleBase = if (peerTyping) strings.typing else previewWithPrefix.ifBlank { if (lastMessage == null && thread.status == "active") strings.sayHi else threadStatusLabel(thread.status, strings) }
    val subtitle = if (!peerTyping && thread.pendingSeriousModeRequestId != null) strings.seriousModePendingSuffix(subtitleBase) else subtitleBase
    val hasUnread = unread > 0
    val timeLabel = remember(lastMessage?.sentAt) { lastMessage?.let { fmtThreadListTime(it.sentAt, strings.yesterday) } }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bounceClickable(onClick = onClick, pressedScale = 0.985f, enforceMinimumTarget = false)
            .padding(vertical = 10.dp)
            .semantics { if (hasUnread) stateDescription = strings.unreadDesc(unread) },
    ) {
        Box(modifier = Modifier.size(60.dp)) {
            InitialAvatar(thread.otherParticipantName, 60.dp)
            PresenceDot(online = peerOnline, modifier = Modifier.align(Alignment.BottomEnd))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                thread.otherParticipantName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (peerTyping || hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                color = if (peerTyping) MaterialTheme.colorScheme.primary else if (hasUnread) MaterialTheme.colorScheme.onSurface else mutedText(0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (timeLabel != null) {
                Text(
                    timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasUnread) d2m.accent else mutedText(0.55f),
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                )
            }
            if (hasUnread) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                        .background(d2m.accent, CircleShape)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (unread > 99) "99+" else unread.toString(), color = d2m.accentOn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Circle avatar showing the person's initial on the warm sunken surface (ThreadOut carries no photo URL). */
@Composable
private fun InitialAvatar(name: String, size: Dp) {
    Box(
        modifier = Modifier.size(size).background(avatarPlaceholder(), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            (name.trim().firstOrNull() ?: '?').uppercaseChar().toString(),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp,
            color = d2m.label,
        )
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
    val strings = LocalStrings.current.matches
    val peerUsername = remember(thread.otherParticipantId) { d2mIdToMessagingUsername(thread.otherParticipantId) }
    val peerOnline by messagingRepo.isPeerOnline(peerUsername).collectAsState()
    val peerTyping by messagingRepo.isPeerTyping(peerUsername).collectAsState()

    LaunchedEffect(peerUsername) { messagingRepo.subscribePresence(thread.otherParticipantId) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backToMatches)
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
                        Icon(Icons.Filled.Lock, contentDescription = strings.endToEndEncrypted, modifier = Modifier.size(14.dp), tint = mutedText(0.5f))
                    }
                }
                val subtitle = when {
                    peerTyping -> strings.typing
                    peerOnline -> strings.online
                    else -> threadStatusLabel(thread.status, strings)
                }
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = mutedText(0.55f))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderIconButton(icon = Icons.Filled.Call, contentDescription = strings.audioCall, onClick = onStartAudioCall)
            HeaderIconButton(icon = Icons.Filled.Videocam, contentDescription = strings.videoCall, onClick = onStartVideoCall)
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
    val strings = LocalStrings.current.matches
    Box {
        // "more menu button on the toolbar also should have rounded
        // outline" -- was a bare IconButton (no outline at all), visibly
        // inconsistent with the two call buttons right next to it
        // (HeaderIconButton's 38dp outlined circle). Same visual now.
        HeaderIconButton(icon = Icons.Filled.MoreVert, contentDescription = strings.moreOptions, onClick = { menuOpen = true })
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, shape = RoundedCornerShape(D2MRadius.lg)) {
            if (thread.pendingSeriousModeRequestId != null) {
                DropdownMenuItem(text = { Text(strings.acceptSeriousMode) }, onClick = { menuOpen = false; onAcceptSeriousRequest() })
                DropdownMenuItem(text = { Text(strings.declineSeriousMode) }, onClick = { menuOpen = false; onDeclineSeriousRequest() })
            }
            if (thread.status == "active") {
                DropdownMenuItem(text = { Text(strings.goSerious) }, onClick = { menuOpen = false; onGoSerious() })
            }
            if (thread.status == "exclusive") {
                DropdownMenuItem(text = { Text(strings.revokeSeriousMode) }, onClick = { menuOpen = false; onRevoke() })
            }
            if (showConfirmUnion) {
                DropdownMenuItem(text = { Text(strings.confirmUnion) }, onClick = { menuOpen = false; onConfirmUnion() })
            }
            DropdownMenuItem(text = { Text(strings.unmatch) }, onClick = { menuOpen = false; onUnmatch() })
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

private fun threadStatusLabel(status: String, strings: MatchesStrings): String = when (status) {
    "active" -> strings.statusMatched
    "exclusive" -> strings.statusSeriousExploration
    "sunsetting" -> strings.statusSunsetting
    "closed" -> strings.statusClosed
    else -> status
}
