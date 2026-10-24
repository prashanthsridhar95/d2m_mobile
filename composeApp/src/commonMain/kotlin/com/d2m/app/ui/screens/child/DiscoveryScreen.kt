package com.d2m.app.ui.screens.child

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.DashboardRepository
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.domain.repository.SuggestionsRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.DeckProgress
import com.d2m.app.ui.components.ProfileCardContent
import com.d2m.app.ui.components.SwipeActionBar
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Window during which a decision can still be undone before it is sent to the server. */
private const val UNDO_WINDOW_MS = 4_000L
private val ActionBarBottomPad = 18.dp
private val BottomClearance = ActionBarBottomPad + 72.dp + 20.dp

private class PendingDecision(val card: SuggestionOut, val action: String, var job: Job? = null)

private fun SwipeDirection.wire() = when (this) {
    SwipeDirection.Accept -> "accept"
    SwipeDirection.Pass -> "reject"
    SwipeDirection.Snooze -> "snooze"
}

/**
 * Child Discover: a Hinge/Bumble-style swipe deck where the ENTIRE profile (hero photo, details, bio/chart tabs) is
 * one card -- see [SwipeDeck] for the gesture/physics contract and [ProfileCardContent] for the card.
 *
 * Decisions are optimistic: the card leaves immediately and the server call is delayed [UNDO_WINDOW_MS] so an Undo
 * snackbar can cancel it (SuggestionsRepository has no server-side undo). A failed send puts the card back at the top
 * with an error snackbar. A pending decision is flushed (not dropped) if the user leaves the screen.
 */
@Composable
fun DiscoveryScreen() {
    val identityStore = org.koin.compose.koinInject<IdentityStore>()
    val suggestionsRepo = org.koin.compose.koinInject<SuggestionsRepository>()
    val identityRepo = org.koin.compose.koinInject<IdentityRepository>()
    val dashboardRepo = org.koin.compose.koinInject<DashboardRepository>()
    val apiClient = org.koin.compose.koinInject<ApiClient>()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current.discovery
    val snackbar = remember { SnackbarHostState() }
    val deckState = rememberSwipeDeckState()

    var queue by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableStateOf(0) }
    var forceNext by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<PendingDecision?>(null) }

    val primaryId = identity.primaryId

    LaunchedEffect(primaryId, reloadTick) {
        if (primaryId == null) return@LaunchedEffect
        loading = true
        error = null
        try {
            queue = suggestionsRepo.getSuggestions(primaryId, forceRefresh = forceNext)
            forceNext = false
            // Seeded once per load so the progress counts down within a stable batch.
            total = queue.size
        } catch (e: Exception) {
            error = friendlyError(e, strings.errLoadCandidates)
        } finally {
            loading = false
        }
    }

    // Survives scope cancellation so a decision made right before navigating away is still delivered.
    val detachedScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    fun restore(card: SuggestionOut) {
        if (queue.none { it.candidateId == card.candidateId }) queue = listOf(card) + queue
    }

    suspend fun send(p: PendingDecision) {
        val pid = primaryId ?: return
        runCatching { suggestionsRepo.act(pid, p.card.candidateId, p.action) }.onFailure {
            restore(p.card)
            snackbar.currentSnackbarData?.dismiss()
            scope.launch { snackbar.showSnackbar(strings.actionFailed, duration = SnackbarDuration.Short) }
        }
    }

    fun flushPending() {
        val p = pending ?: return
        pending = null
        p.job?.cancel()
        scope.launch { send(p) }
    }

    fun undo() {
        val p = pending ?: return
        pending = null
        p.job?.cancel()
        snackbar.currentSnackbarData?.dismiss()
        restore(p.card)
    }

    fun onDecision(card: SuggestionOut, dir: SwipeDirection) {
        flushPending()
        queue = queue.filterNot { it.candidateId == card.candidateId }
        val p = PendingDecision(card, dir.wire())
        pending = p
        val first = card.candidateName.substringBefore(" ")
        val msg = when (dir) {
            SwipeDirection.Accept -> strings.acceptedSnack(first)
            SwipeDirection.Pass -> strings.passedSnack(first)
            SwipeDirection.Snooze -> strings.snoozedSnack(first)
        }
        snackbar.currentSnackbarData?.dismiss()
        scope.launch {
            val r = snackbar.showSnackbar(msg, actionLabel = strings.undo, duration = SnackbarDuration.Indefinite)
            if (r == SnackbarResult.ActionPerformed && pending === p) undo()
        }
        p.job = scope.launch {
            delay(UNDO_WINDOW_MS)
            if (pending === p) {
                pending = null
                snackbar.currentSnackbarData?.dismiss()
                send(p)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val p = pending
            val pid = primaryId
            if (p != null && pid != null) {
                pending = null
                detachedScope.launch { runCatching { suggestionsRepo.act(pid, p.card.candidateId, p.action) } }
            }
        }
    }

    // Warm everything the next cards need so promoting one shows no loading state: tab data (repository-cached),
    // extra photos, and the hero images beyond the three the stack already composes.
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
                loading && queue.isEmpty() -> SwipeDeckSkeleton(topInset, BottomClearance)
                error != null && queue.isEmpty() -> Column(Modifier.padding(16.dp).padding(top = topInset)) {
                    D2MErrorBanner(error!!, onRetry = { reloadTick++ })
                }
                queue.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp).padding(top = topInset),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
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
                    SwipeDeck(
                        items = queue,
                        itemKey = { it.candidateId },
                        state = deckState,
                        onDecision = ::onDecision,
                        stampAccept = strings.stampAccept,
                        stampPass = strings.stampPass,
                        stampSnooze = strings.stampSnooze,
                        modifier = Modifier.fillMaxSize(),
                    ) { card, isTop ->
                        ProfileCardContent(
                            card = card,
                            isTop = isTop,
                            viewerPrimaryId = primaryId,
                            topInset = topInset,
                            bottomClearance = BottomClearance,
                        )
                    }
                    DeckProgress(
                        position = (total - queue.size + 1).coerceIn(1, maxOf(total, 1)),
                        total = total,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = topInset + 10.dp, start = 16.dp, end = 16.dp),
                    )
                    SwipeActionBar(
                        enabled = !deckState.busy,
                        passLabel = strings.pass,
                        snoozeLabel = strings.snooze,
                        acceptLabel = strings.accept,
                        onPass = { deckState.swipe(SwipeDirection.Pass) },
                        onSnooze = { deckState.swipe(SwipeDirection.Snooze) },
                        onAccept = { deckState.swipe(SwipeDirection.Accept) },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = ActionBarBottomPad),
                    )
                }
            }
            SnackbarHost(
                hostState = snackbar,
                // Top, under the progress counter: at the bottom it covers the card's name/occupation block.
                modifier = Modifier.align(Alignment.TopCenter).padding(top = topInset + 64.dp),
            )
        }
    }
}
