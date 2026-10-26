package com.d2m.app.ui.screens.child

import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.SnackbarDuration
import com.d2m.app.data.session.createSettings
import com.d2m.app.domain.repository.SeriousModeRepository
import com.d2m.app.messaging.ChatUiState
import com.d2m.app.messaging.d2mIdToMessagingUsername
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.network.ApiError
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.DashboardRepository
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.domain.repository.SuggestionsRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.LikeSheet
import com.d2m.app.ui.components.LikeTarget
import com.d2m.app.ui.components.PassFab
import com.d2m.app.ui.components.ProfileCardContent
import com.d2m.app.ui.components.SwipeDeck
import com.d2m.app.ui.components.SwipeDeckSkeleton
import com.d2m.app.ui.components.SwipeDirection
import com.d2m.app.ui.components.rememberPhotoImageLoader
import com.d2m.app.ui.components.rememberSwipeDeckState
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val FabBottomPad = 18.dp

/** Room kept under the hero's name block and at the end of the scroll for the floating pass pill. */
private val BottomClearance = FabBottomPad + 48.dp + 16.dp

/** How long a pass can be undone before it is sent (web: UNDO_MS). */
private const val UNDO_MS = 6_000L
private const val MATCH_SNACK_MS = 10_000L
private const val LIKE_SNACK_MS = 5_000L
private const val COACH_KEY = "d2m.discover.passCoach.v1"

/** The one pass waiting out its Undo window. Plain holder (not state): only read from callbacks. */
private class PendingPass {
    var card: SuggestionOut? = null
    var job: Job? = null
    /** Kept current so the final flush on leaving the screen never sees a stale id. */
    var primaryId: String? = null
}

/**
 * Child Discover, Hinge-style (mirrors web's screens/child/DiscoveryScreen.jsx): one profile at a time as a single
 * card -- hero photo, then photos interleaved with About-me prompt answers, then the detail tabs.
 *
 * There is no Accept / Snooze / Pass row. Every photo and every prompt answer has its own heart; tapping it opens a
 * small sheet for an optional comment and sends the request tied to that exact item (liked_item_type/id). A swipe
 * right does the same for the profile as a whole (the card springs back and the sheet opens; sending flies it off).
 * The single floating "Not now" pill -- or a swipe left -- is "pass for now": it is never a rejection. The card
 * rotates to the back of the queue immediately, but the server call is held for [UNDO_MS] so Undo is real; the
 * pending pass is flushed on the next pass or when the screen leaves.
 *
 * See [SwipeDeck] for the gesture/physics contract and [ProfileCardContent] for the card.
 */
@Composable
fun DiscoveryScreen(onOpenMatches: () -> Unit = {}) {
    val identityStore = org.koin.compose.koinInject<IdentityStore>()
    val suggestionsRepo = org.koin.compose.koinInject<SuggestionsRepository>()
    val identityRepo = org.koin.compose.koinInject<IdentityRepository>()
    val dashboardRepo = org.koin.compose.koinInject<DashboardRepository>()
    val seriousRepo = org.koin.compose.koinInject<SeriousModeRepository>()
    val chatUiState = org.koin.compose.koinInject<ChatUiState>()
    val apiClient = org.koin.compose.koinInject<ApiClient>()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current.discovery
    val snackbar = remember { SnackbarHostState() }
    val deckState = rememberSwipeDeckState()
    val settings = remember { createSettings() }

    var queue by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableStateOf(0) }
    var forceNext by remember { mutableStateOf(false) }
    // Stable candidate order from the last load, so "was that the last one?" survives the pass rotation.
    var originalOrder by remember { mutableStateOf<List<String>>(emptyList()) }
    // null while unknown; Discover is paused while the child is in an exclusive Serious Mode relationship.
    var seriousStatus by remember { mutableStateOf<String?>(null) }
    var coach by remember { mutableStateOf(false) }

    var likeTarget by remember { mutableStateOf<LikeTarget?>(null) }
    var likeBusy by remember { mutableStateOf(false) }
    var likeError by remember { mutableStateOf<String?>(null) }
    var matchedId by remember { mutableStateOf<String?>(null) }

    val primaryId = identity.primaryId

    LaunchedEffect(primaryId) {
        if (primaryId == null) return@LaunchedEffect
        seriousStatus = runCatching { seriousRepo.getSponsorStatus(primaryId).status }.getOrDefault("no_active_threads") // fail open
    }

    LaunchedEffect(primaryId, reloadTick) {
        if (primaryId == null) return@LaunchedEffect
        loading = true
        error = null
        try {
            queue = suggestionsRepo.getSuggestions(primaryId, forceRefresh = forceNext)
            originalOrder = queue.map { it.candidateId }
            forceNext = false
        } catch (e: Exception) {
            error = friendlyError(e, strings.errLoadCandidates)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(queue.isNotEmpty()) {
        if (queue.isNotEmpty() && !runCatching { settings.getBoolean(COACH_KEY, false) }.getOrDefault(false)) coach = true
    }
    fun dismissCoach() {
        coach = false
        runCatching { settings.putBoolean(COACH_KEY, true) }
    }

    // A pass is fire-and-forget and must survive leaving the screen right after the swipe.
    val detachedScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // Which kind of snackbar is showing, so the Undo timer never dismisses an unrelated message.
    val snackSeq = remember { intArrayOf(0) }
    fun showSnack(message: String, actionLabel: String? = null, ms: Long? = 4_000L, onAction: (() -> Unit)? = null) {
        snackbar.currentSnackbarData?.dismiss()
        val token = ++snackSeq[0]
        scope.launch {
            val timer = ms?.let { launch { delay(it); if (snackSeq[0] == token) snackbar.currentSnackbarData?.dismiss() } }
            val r = snackbar.showSnackbar(message, actionLabel = actionLabel, duration = SnackbarDuration.Indefinite)
            timer?.cancel()
            if (r == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }

    val pending = remember { PendingPass() }
    pending.primaryId = primaryId

    // Send a pass to the server; on failure keep a message up until the person retries (the card is already gone).
    fun commitPass(card: SuggestionOut) {
        val pid = pending.primaryId ?: return
        detachedScope.launch {
            try {
                suggestionsRepo.act(pid, card.candidateId, "pass")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                showSnack(strings.passFailed(card.candidateName.substringBefore(" ")), strings.retry, ms = null) { commitPass(card) }
            }
        }
    }
    fun flushPass() {
        val card = pending.card ?: return
        pending.job?.cancel()
        pending.card = null
        pending.job = null
        commitPass(card)
    }
    // Leaving the screen is a final answer.
    DisposableEffect(Unit) { onDispose { flushPass() } }

    fun onDecision(card: SuggestionOut, dir: SwipeDirection) {
        val first = card.candidateName.substringBefore(" ")
        if (dir == SwipeDirection.Accept) {
            // The request was already sent by the like sheet before the card flew off.
            queue = queue.filterNot { it.candidateId == card.candidateId }
            if (matchedId == card.candidateId) {
                matchedId = null
                showSnack(strings.matchBanner(first), strings.sayHi, ms = MATCH_SNACK_MS) {
                    // Open Matches straight onto that conversation.
                    chatUiState.requestOpenPeer(d2mIdToMessagingUsername(card.candidateId))
                    onOpenMatches()
                }
            } else {
                showSnack(strings.likeSent(first), ms = LIKE_SNACK_MS)
            }
            return
        }
        // Pass for now: a previous pass is now final; this one is held for the Undo window.
        flushPass()
        if (coach) dismissCoach()
        val remaining = originalOrder.filter { id -> queue.any { it.candidateId == id } }
        val wraps = remaining.size > 1 && remaining.lastOrNull() == card.candidateId
        val message = when {
            remaining.size <= 1 -> strings.passedOnly(first)
            wraps -> strings.passedWrap(first)
            else -> strings.passedForNow(first)
        }
        // Rotated to the back so unmatched profiles keep cycling.
        queue = queue.filterNot { it.candidateId == card.candidateId } + card
        pending.card = card
        pending.job = scope.launch {
            delay(UNDO_MS)
            if (pending.card === card) { pending.card = null; pending.job = null; commitPass(card) }
        }
        showSnack(message, strings.undo, ms = UNDO_MS) {
            if (pending.card === card) {
                pending.job?.cancel()
                pending.card = null
                pending.job = null
                // Back on top, not at the back.
                queue = listOf(card) + queue.filterNot { it.candidateId == card.candidateId }
            }
        }
    }

    fun sendLike(comment: String?) {
        val pid = primaryId ?: return
        val target = likeTarget ?: return
        val card = queue.firstOrNull() ?: return
        scope.launch {
            likeBusy = true
            likeError = null
            try {
                val res = suggestionsRepo.act(pid, card.candidateId, "accept", comment, target.type, target.id)
                if (res.mutualMatch) matchedId = card.candidateId
                likeTarget = null
                deckState.swipe(SwipeDirection.Accept)
            } catch (e: Exception) {
                likeError = friendlyError(e, strings.likeFailed)
            } finally {
                likeBusy = false
            }
        }
    }

    // Warm everything the next cards need so promoting one shows no loading state: tab data (repository-cached),
    // photos and prompts for the feed, and the hero images beyond the three the stack already composes.
    val imageLoader = rememberPhotoImageLoader()
    val platformContext = LocalPlatformContext.current
    val upcomingIds = queue.drop(1).take(4).map { it.candidateId }
    LaunchedEffect(upcomingIds) {
        val upcoming = queue.drop(1).take(4)
        upcoming.drop(2).forEach { c ->
            c.photoUrl?.let { imageLoader.enqueue(ImageRequest.Builder(platformContext).data(apiClient.resolveMediaUrl(it)).build()) }
        }
        coroutineScope {
            upcoming.take(2).forEach { c ->
                launch { runCatching { identityRepo.getExtendedBio(c.candidateId) } }
                launch { runCatching { identityRepo.getAboutMe(c.candidateId) } }
                launch { runCatching { identityRepo.getPhotos(c.candidateId) } }
                launch { runCatching { dashboardRepo.getChart(c.candidateId) } }
            }
        }
    }

    D2MTheme(flow = D2MFlow.CHILD) {
        val density = LocalDensity.current
        val topInsetPx = WindowInsets.statusBars.getTop(density)
        val topInset = with(density) { topInsetPx.toDp() }

        // The Scaffold above pads content below the status bar; grow upward by exactly that inset so the hero photo
        // runs edge-to-edge underneath it. Layout height reported to the parent is unchanged.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layout { measurable, c ->
                    val extra = if (c.hasBoundedHeight) topInsetPx else 0
                    val h = if (c.hasBoundedHeight) c.maxHeight + extra else c.maxHeight
                    val p = measurable.measure(c.copy(minHeight = if (c.hasBoundedHeight) h else 0, maxHeight = h))
                    layout(p.width, p.height - extra) { p.place(0, -extra) }
                },
        ) {
            when {
                seriousStatus == "serious_exploration" -> Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(top = topInset),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        strings.seriousTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        strings.seriousBody,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
                    )
                    D2MButton(strings.goToMatches, onClick = onOpenMatches)
                }
                (loading && queue.isEmpty()) || seriousStatus == null -> SwipeDeckSkeleton(topInset, BottomClearance)
                error != null && queue.isEmpty() -> Column(Modifier.padding(16.dp).padding(top = topInset)) {
                    D2MErrorBanner(error!!, onRetry = { reloadTick++ })
                }
                queue.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp).padding(top = topInset),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    D2MEmptyState(strings.caughtUpTitle, strings.caughtUpBody)
                    D2MButton(
                        strings.checkAgain,
                        variant = D2MButtonVariant.OUTLINE,
                        onClick = { forceNext = true; reloadTick++ },
                    )
                }
                else -> {
                    val top = queue.first()
                    SwipeDeck(
                        items = queue,
                        itemKey = { it.candidateId },
                        state = deckState,
                        onDecision = ::onDecision,
                        stampAccept = strings.stampAccept,
                        stampPass = strings.stampPass,
                        stampSnooze = strings.stampSnooze,
                        modifier = Modifier.fillMaxSize(),
                        onAcceptGesture = { likeError = null; likeTarget = LikeTarget("profile") },
                        // Screen-reader alternative to the swipe gestures.
                        topCardActions = listOf(
                            CustomAccessibilityAction(strings.likeProfileAction) { likeError = null; likeTarget = LikeTarget("profile"); true },
                            CustomAccessibilityAction(strings.passFab) { if (!deckState.busy) deckState.swipe(SwipeDirection.Pass); true },
                        ),
                    ) { card, isTop ->
                        ProfileCardContent(
                            card = card,
                            isTop = isTop,
                            viewerPrimaryId = primaryId,
                            topInset = topInset,
                            bottomClearance = BottomClearance,
                            onLike = { likeError = null; likeTarget = it },
                        )
                    }
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = FabBottomPad),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (coach) PassCoach(strings.coachTitle, strings.coachBody, strings.coachOk, onDismiss = ::dismissCoach)
                        PassFab(
                            label = strings.passLabel,
                            description = strings.passCd(top.candidateName.substringBefore(" ")),
                            enabled = !deckState.busy,
                            onClick = { deckState.swipe(SwipeDirection.Pass) },
                        )
                    }
                }
            }
            SnackbarHost(
                hostState = snackbar,
                // Top: at the bottom it would cover the card's name block.
                modifier = Modifier.align(Alignment.TopCenter).padding(top = topInset + 12.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        val target = likeTarget
        val current = queue.firstOrNull()
        if (target != null && current != null) {
            LikeSheet(
                target = target,
                name = current.candidateName,
                busy = likeBusy,
                error = likeError,
                onCancel = { likeTarget = null },
                onSend = ::sendLike,
            )
        }
    }
}

/** First-run hint above the pass pill (web: .dh-coach). Shown once; dismissing or the first pass records it. */
@Composable
private fun PassCoach(title: String, body: String, ok: String, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.widthIn(max = 300.dp).semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onDismiss) { Text(ok) }
        }
    }
}
