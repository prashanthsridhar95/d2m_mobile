package com.d2m.app.ui.screens.child

import androidx.compose.foundation.layout.Arrangement
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

/** Room kept under the hero's name block and at the end of the scroll for the floating pass button. */
private val BottomClearance = FabBottomPad + 44.dp + 16.dp

/**
 * Child Discover, Hinge-style (mirrors web's screens/child/DiscoveryScreen.jsx): one profile at a time as a single
 * card -- hero photo, then photos interleaved with About-me prompt answers, then the detail tabs.
 *
 * There is no Accept / Snooze / Pass row. Every photo and every prompt answer has its own heart; tapping it opens a
 * small sheet for an optional comment and sends the request tied to that exact item (liked_item_type/id). A swipe
 * right does the same for the profile as a whole (the card springs back and the sheet opens; sending flies it off).
 * The single floating X -- or a swipe left -- is "pass for now": the server snoozes the profile, it is never
 * rejected, and it rotates to the back of the queue so it comes round again.
 *
 * See [SwipeDeck] for the gesture/physics contract and [ProfileCardContent] for the card.
 */
@Composable
fun DiscoveryScreen(onOpenMatches: () -> Unit = {}) {
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
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableStateOf(0) }
    var forceNext by remember { mutableStateOf(false) }

    var likeTarget by remember { mutableStateOf<LikeTarget?>(null) }
    var likeBusy by remember { mutableStateOf(false) }
    var likeError by remember { mutableStateOf<String?>(null) }
    var matchedId by remember { mutableStateOf<String?>(null) }

    val primaryId = identity.primaryId

    LaunchedEffect(primaryId, reloadTick) {
        if (primaryId == null) return@LaunchedEffect
        loading = true
        error = null
        try {
            queue = suggestionsRepo.getSuggestions(primaryId, forceRefresh = forceNext)
            forceNext = false
        } catch (e: Exception) {
            error = friendlyError(e, strings.errLoadCandidates)
        } finally {
            loading = false
        }
    }

    // A pass is fire-and-forget and must survive leaving the screen right after the swipe.
    val detachedScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    fun showSnack(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        snackbar.currentSnackbarData?.dismiss()
        scope.launch {
            val r = snackbar.showSnackbar(message, actionLabel = actionLabel, duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }

    fun onDecision(card: SuggestionOut, dir: SwipeDirection) {
        val pid = primaryId
        val first = card.candidateName.substringBefore(" ")
        if (dir == SwipeDirection.Accept) {
            // The request was already sent by the like sheet before the card flew off.
            queue = queue.filterNot { it.candidateId == card.candidateId }
            if (matchedId == card.candidateId) {
                matchedId = null
                showSnack(strings.matchBanner(first), strings.sayHi, onOpenMatches)
            }
            return
        }
        // Pass for now: snoozed server-side, rotated to the back so unmatched profiles keep cycling.
        queue = queue.filterNot { it.candidateId == card.candidateId } + card
        if (pid != null) detachedScope.launch { runCatching { suggestionsRepo.act(pid, card.candidateId, "pass") } }
        showSnack(strings.passedForNow(first))
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
                loading && queue.isEmpty() -> SwipeDeckSkeleton(topInset, BottomClearance)
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
                    PassFab(
                        label = strings.passFab,
                        enabled = !deckState.busy,
                        onClick = { deckState.swipe(SwipeDirection.Pass) },
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = FabBottomPad),
                    )
                }
            }
            SnackbarHost(
                hostState = snackbar,
                // Top: at the bottom it would cover the card's name block.
                modifier = Modifier.align(Alignment.TopCenter).padding(top = topInset + 12.dp),
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
