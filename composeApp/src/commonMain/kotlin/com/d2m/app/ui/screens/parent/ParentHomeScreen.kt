package com.d2m.app.ui.screens.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.ProspectCardOut
import com.d2m.app.data.model.SponsorDashboardOut
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.network.ApiError
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.ConsentRepository
import com.d2m.app.domain.repository.DashboardRepository
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.domain.repository.SuggestionsRepository
import com.d2m.app.messaging.ChatUiState
import com.d2m.app.messaging.ParentContactsStore
import com.d2m.app.messaging.d2mIdToMessagingUsername
import com.d2m.app.ui.components.D2MBadge
import com.d2m.app.ui.components.D2MBadgeTone
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MSkeleton
import com.d2m.app.ui.components.MatchCard
import com.d2m.app.ui.components.MatchCardVariant
import com.d2m.app.ui.components.MetaText
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.components.PrefetchPhotos
import com.d2m.app.ui.components.ProfileThumb
import com.d2m.app.ui.components.SubHeading
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.strings.ParentHomeStrings
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.d2m
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Parent Home, simplified. The first screen is four things: a compact
 * header (title + child's profile status), ONE next-best-action card, a
 * paged carousel of top suggestions, and a stack of collapsed rows
 * (Shortlisted / Serious mode / Activity) that expand in place -- progressive
 * disclosure instead of the previous long scroll of every section at once.
 *
 * Perf notes: the page is a LazyColumn with keyed items; the carousel is a
 * HorizontalPager keyed by candidateId; shortlist toggles copy exactly one
 * SuggestionOut so only that card recomposes; pull-to-refresh refetches the
 * three feeds in parallel (async) and keeps the old data on screen if the
 * refresh fails.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentHomeScreen(
    onOpenProfile: (String) -> Unit,
    onOpenChildProfileDialog: () -> Unit,
    onOpenMessages: () -> Unit,
) {
    val identityStore: IdentityStore = koinInject()
    val dashboardRepo: DashboardRepository = koinInject()
    val suggestionsRepo: SuggestionsRepository = koinInject()
    val apiClient: ApiClient = koinInject()
    val identity by identityStore.identity.collectAsState()

    var dashboard by remember { mutableStateOf<SponsorDashboardOut?>(null) }
    var shortlist by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    // SponsorDashboardOut has no top_suggestions field on the real backend
    // (it is sponsor-safe by design, only ever exposing counts), so the
    // carousel sources real SuggestionOut cards from SuggestionsRepository,
    // same call ParentBrowseScreen's "matching" tab uses.
    var topSuggestions by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshError by remember { mutableStateOf<String?>(null) }
    var notClaimed by remember { mutableStateOf(false) }
    var consentGranted by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableStateOf(0) }

    val sponsorId = identity.sponsorId
    val childPrimaryId = identity.childPrimaryId
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current.parentHome

    // Optimistic shortlist toggle: both lists update immediately, then the
    // API call runs; on failure the lists are re-synced from the server.
    // Takes the whole candidate because adding to `shortlist` needs a full
    // SuggestionOut to append.
    fun toggleShortlist(candidate: SuggestionOut, nowShortlisted: Boolean) {
        val primaryId = childPrimaryId ?: return
        val updated = candidate.copy(isShortlisted = nowShortlisted)
        topSuggestions = topSuggestions.map { if (it.candidateId == candidate.candidateId) updated else it }
        shortlist = if (nowShortlisted) {
            if (shortlist.none { it.candidateId == candidate.candidateId }) shortlist + updated else shortlist
        } else {
            shortlist.filterNot { it.candidateId == candidate.candidateId }
        }
        scope.launch {
            try {
                if (nowShortlisted) suggestionsRepo.addToShortlist(primaryId, candidate.candidateId)
                else suggestionsRepo.removeFromShortlist(primaryId, candidate.candidateId)
            } catch (_: Exception) {
                runCatching {
                    shortlist = suggestionsRepo.getShortlist(primaryId)
                    topSuggestions = suggestionsRepo.getSuggestions(primaryId)
                }
            }
        }
    }

    // The three feeds are independent, so they load in parallel.
    suspend fun loadAll(sponsor: String, primary: String, force: Boolean) = coroutineScope {
        val d = async { dashboardRepo.getSponsorDashboard(sponsor, forceRefresh = force) }
        val sl = async { suggestionsRepo.getShortlist(primary) }
        val ts = async { suggestionsRepo.getSuggestions(primary, forceRefresh = force) }
        dashboard = d.await()
        shortlist = sl.await()
        topSuggestions = ts.await()
    }

    // A Sponsor whose invite hasn't been redeemed yet has no childPrimaryId;
    // same lookup web's ChildIdGate.jsx does (GET /sponsors/{id}/child).
    LaunchedEffect(sponsorId, childPrimaryId) {
        if (sponsorId == null) return@LaunchedEffect
        if (childPrimaryId == null) {
            runCatching { dashboardRepo.getSponsorChild(sponsorId) }
                .onSuccess { identityStore.updateChildPrimaryId(it.primaryId) }
                .onFailure { notClaimed = true; loading = false }
            return@LaunchedEffect
        }
        notClaimed = false
        loading = true
        error = null
        try {
            loadAll(sponsorId, childPrimaryId, force = false)
        } catch (e: Exception) {
            error = friendlyError(e, strings.errLoadDashboard)
        } finally {
            loading = false
        }
    }

    fun refresh() {
        val sponsor = sponsorId ?: return
        val primary = childPrimaryId ?: return
        scope.launch {
            refreshing = true
            refreshError = null
            try {
                loadAll(sponsor, primary, force = true)
                error = null
                refreshTick++
            } catch (e: Exception) {
                if (dashboard == null) error = friendlyError(e, strings.errLoadDashboard)
                else refreshError = strings.refreshFailed
            } finally {
                refreshing = false
            }
        }
    }

    D2MTheme(flow = D2MFlow.PARENT) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when {
                    notClaimed -> item(key = "not-claimed", contentType = "state") {
                        D2MEmptyState(strings.waitingOnChildTitle, strings.waitingOnChildBody)
                    }

                    loading -> item(key = "skeleton", contentType = "skeleton") { HomeSkeleton() }

                    error != null -> item(key = "error", contentType = "state") {
                        D2MErrorBanner(error!!, onRetry = { refresh() })
                    }

                    else -> {
                        item(key = "header", contentType = "header") {
                            HomeHeader(
                                title = strings.pageTitle,
                                complete = dashboard?.childProfileCompleted == true,
                                strings = strings,
                                onClick = onOpenChildProfileDialog,
                            )
                        }
                        if (refreshError != null) {
                            item(key = "refresh-error", contentType = "state") { D2MErrorBanner(refreshError!!) }
                        }
                        item(key = "next-action", contentType = "action") {
                            val profileComplete = dashboard?.childProfileCompleted == true
                            NextActionCard(
                                kind = when {
                                    consentGranted -> NextKind.MESSAGE
                                    topSuggestions.isNotEmpty() -> NextKind.REVIEW
                                    !profileComplete -> NextKind.PROFILE
                                    else -> NextKind.CAUGHT_UP
                                },
                                reviewCount = topSuggestions.size,
                                strings = strings,
                                onClick = when {
                                    consentGranted -> onOpenMessages
                                    topSuggestions.isNotEmpty() -> ({ onOpenProfile(topSuggestions.first().candidateId) })
                                    !profileComplete -> onOpenChildProfileDialog
                                    else -> null
                                },
                            )
                        }
                        item(key = "top-picks", contentType = "carousel") {
                            TopPicks(
                                suggestions = topSuggestions,
                                strings = strings,
                                resolvePhotoUrl = apiClient::resolveMediaUrl,
                                onOpenProfile = onOpenProfile,
                                onToggleShortlist = ::toggleShortlist,
                            )
                        }
                        item(key = "shortlist-row", contentType = "disclosure") {
                            var expanded by rememberSaveable { mutableStateOf(false) }
                            DisclosureRow(
                                title = strings.shortlistedCount(shortlist.size),
                                summary = null,
                                expanded = expanded,
                                onToggle = { expanded = !expanded },
                            ) {
                                if (shortlist.isEmpty()) {
                                    MetaText(strings.shortlistEmpty, Modifier.padding(top = 4.dp))
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                                        shortlist.forEach { s ->
                                            androidx.compose.runtime.key(s.candidateId) {
                                                MatchCard(
                                                    suggestion = s,
                                                    resolvePhotoUrl = apiClient::resolveMediaUrl,
                                                    onClick = { onOpenProfile(s.candidateId) },
                                                    variant = MatchCardVariant.COMPACT,
                                                    isShortlisted = true,
                                                    onToggleShortlist = { toggleShortlist(s, false) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (sponsorId != null) {
                            item(key = "serious-row", contentType = "disclosure") {
                                ConsentProspectCard(
                                    sponsorId = sponsorId,
                                    apiClient = apiClient,
                                    refreshTick = refreshTick,
                                    onOpenProfile = onOpenProfile,
                                    onOpenMessages = onOpenMessages,
                                    onGrantedChanged = { consentGranted = it },
                                    strings = strings,
                                )
                            }
                        }
                        item(key = "activity-row", contentType = "disclosure") {
                            var expanded by rememberSaveable { mutableStateOf(false) }
                            val d = dashboard
                            DisclosureRow(
                                title = strings.activityRow,
                                summary = d?.let { "${strings.statSuggestionsSent}: ${it.suggestionsSentCount}" },
                                expanded = expanded,
                                onToggle = { expanded = !expanded },
                            ) {
                                if (d != null) {
                                    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        StatLine(strings.statSuggestionsSent, d.suggestionsSentCount.toString())
                                        StatLine(strings.statUnreadAlerts, d.unreadNotificationCount.toString())
                                        StatLine(strings.statPendingRequests, d.pendingConsentRequestCount.toString())
                                        StatLine(
                                            strings.statProfile,
                                            if (d.childProfileCompleted) strings.profileComplete else strings.profileIncomplete,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Warm the photos of the carousel pages that are not on screen yet.
    // (Cheap no-op when the list is empty.)
    PrefetchPhotos(
        count = topSuggestions.size,
        firstVisibleIndex = { 0 },
        urlAt = { i -> topSuggestions.getOrNull(i)?.photoUrl?.let(apiClient::resolveMediaUrl) },
        ahead = 4,
    )
}

private enum class NextKind { REVIEW, MESSAGE, PROFILE, CAUGHT_UP }

@Composable
private fun HomeHeader(title: String, complete: Boolean, strings: ParentHomeStrings, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) { PageTitle(title) }
        Box(
            Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(percent = 50))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            D2MBadge(
                if (complete) strings.profileComplete else strings.profileIncomplete,
                if (complete) D2MBadgeTone.SUCCESS else D2MBadgeTone.WARNING,
            )
        }
    }
}

/** The single "do this next" card: accent ground, one icon, one sentence, one tap. */
@Composable
private fun NextActionCard(kind: NextKind, reviewCount: Int, strings: ParentHomeStrings, onClick: (() -> Unit)?) {
    val (icon, title, body) = when (kind) {
        NextKind.REVIEW -> Triple(Icons.Filled.AutoAwesome, strings.nextReviewTitle(reviewCount), strings.nextReviewBody)
        NextKind.MESSAGE -> Triple(Icons.Filled.ChatBubble, strings.nextMessageTitle, strings.nextMessageBody)
        NextKind.PROFILE -> Triple(Icons.Filled.Person, strings.nextProfileTitle, strings.nextProfileBody)
        NextKind.CAUGHT_UP -> Triple(Icons.Filled.CheckCircle, strings.allCaughtUpTitle, strings.allCaughtUpBody)
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = animateFloatAsState(if (pressed && onClick != null) 0.98f else 1f, label = "next-press")
    val shape = RoundedCornerShape(18.dp)
    val ground = if (onClick != null) d2m.accent else d2m.surfaceSunken
    val ink = if (onClick != null) d2m.accentOn else d2m.textPrimary
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(shape)
            .background(ground)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), onClick = onClick) else Modifier)
            .heightIn(min = 88.dp)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ink)
            Text(body, style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.85f), modifier = Modifier.padding(top = 2.dp))
        }
        if (onClick != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = ink)
    }
}

@Composable
private fun TopPicks(
    suggestions: List<SuggestionOut>,
    strings: ParentHomeStrings,
    resolvePhotoUrl: (String) -> String,
    onOpenProfile: (String) -> Unit,
    onToggleShortlist: (SuggestionOut, Boolean) -> Unit,
) {
    Column {
        Text(strings.topPicks, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(10.dp))
        if (suggestions.isEmpty()) {
            D2MCard(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    MetaText(strings.noTopPicks)
                }
            }
        } else {
            // Negative content padding on the parent would be needed to bleed
            // to the screen edge; instead the pager is inset by the list's
            // own 16dp and peeks the next card via a fixed page width.
            val pagerState = rememberPagerState { suggestions.size }
            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fixed(264.dp),
                pageSpacing = 12.dp,
                beyondViewportPageCount = 1,
                key = { suggestions[it].candidateId },
                contentPadding = PaddingValues(0.dp),
            ) { page ->
                val s = suggestions[page]
                MatchCard(
                    suggestion = s,
                    resolvePhotoUrl = resolvePhotoUrl,
                    onClick = { onOpenProfile(s.candidateId) },
                    variant = MatchCardVariant.FEATURE,
                    isShortlisted = s.isShortlisted,
                    onToggleShortlist = { onToggleShortlist(s, !s.isShortlisted) },
                    modifier = Modifier.width(264.dp),
                )
            }
        }
    }
}

/**
 * A collapsed summary row that expands in place. State lives with the
 * caller (rememberSaveable) so it survives rotation and navigation.
 */
@Composable
private fun DisclosureRow(
    title: String,
    summary: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    D2MCard(Modifier.fillMaxWidth().animateContentSize()) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (summary != null) MetaText(summary)
                }
                trailing?.invoke()
                Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = d2m.label, modifier = Modifier.rotate(rotation))
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) { content() }
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        MetaText(label)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

/** Same silhouette as the loaded page, so nothing shifts when data lands. */
@Composable
private fun HomeSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            D2MSkeleton(width = 200.dp, height = 28.dp)
            D2MSkeleton(width = 96.dp, height = 24.dp, radius = 12.dp)
        }
        D2MSkeleton(height = 88.dp, radius = 18.dp)
        Column {
            D2MSkeleton(width = 100.dp, height = 18.dp)
            Spacer(Modifier.size(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                D2MSkeleton(width = 264.dp, height = 330.dp, radius = 20.dp)
                D2MSkeleton(width = 40.dp, height = 330.dp, radius = 20.dp)
            }
        }
        repeat(3) { D2MSkeleton(height = 56.dp, radius = 6.dp) }
    }
}

// "Request Access" consent card -- mirrors d2m_web's DiscoverScreen.jsx
// ConsentCard: auto-discovers the Sponsor's child's current Serious Mode
// prospect (GET /sponsors/{id}/prospect, 404 when there isn't one yet --
// the common case, not a load failure), then renders one of: masked card
// + Request Access button, a pending badge, or (once granted) the
// unlocked card with View profile / Message their parent. "After consent
// is given, a card is shown. But what is it is not known... the parent
// should be able to check the profile, talk to her or her parents"
// (reported directly, same wording web's own docstring quotes).
@Composable
private fun ConsentProspectCard(
    sponsorId: String,
    apiClient: ApiClient,
    refreshTick: Int,
    onOpenProfile: (String) -> Unit,
    onOpenMessages: () -> Unit,
    onGrantedChanged: (Boolean) -> Unit,
    strings: ParentHomeStrings,
) {
    val consentRepo: ConsentRepository = koinInject()
    val identityRepo: IdentityRepository = koinInject()
    val contactsStore: ParentContactsStore = koinInject()
    val chatUiState: ChatUiState = koinInject()
    val scope = rememberCoroutineScope()

    var card by remember { mutableStateOf<ProspectCardOut?>(null) }
    var loading by remember { mutableStateOf(true) }
    var noProspect by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var requestBusy by remember { mutableStateOf(false) }
    var requestError by remember { mutableStateOf<String?>(null) }
    var messageParentBusy by remember { mutableStateOf(false) }
    var messageParentError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sponsorId, refreshTick) {
        if (card == null) loading = true
        noProspect = false
        loadError = null
        try {
            card = consentRepo.getCurrentProspect(sponsorId)
        } catch (e: ApiError) {
            if (e.status == 404) noProspect = true else loadError = friendlyError(e, strings.consentCouldntLoad)
        } catch (e: Exception) {
            loadError = friendlyError(e, strings.consentCouldntLoad)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(card?.consentStatus) { onGrantedChanged(card?.consentStatus == "granted") }

    fun requestAccess() {
        val prospectId = card?.prospectId ?: return
        scope.launch {
            requestBusy = true
            requestError = null
            try {
                val res = consentRepo.requestAccess(sponsorId, prospectId)
                card = card?.copy(consentStatus = res.status)
            } catch (e: Exception) {
                requestError = friendlyError(e, strings.consentCouldntSendRequest)
            } finally {
                requestBusy = false
            }
        }
    }

    // Open, no request/accept gate -- same as ProfileDetailScreen.kt's
    // identical messageParent() (see that file's own doc comment). prospectId
    // is the CHILD's own id; their Sponsor's id has to be resolved first.
    fun messageParent() {
        val prospectId = card?.prospectId ?: return
        scope.launch {
            messageParentBusy = true
            messageParentError = null
            try {
                val sponsor = identityRepo.getPrimarySponsor(prospectId)
                val sponsorProfile = identityRepo.getSponsorProfile(sponsor.sponsorId)
                val myUsername = d2mIdToMessagingUsername(sponsorId)
                val peerUsername = d2mIdToMessagingUsername(sponsor.sponsorId)
                contactsStore.registerContact(
                    myUsername = myUsername,
                    peerUsername = peerUsername,
                    peerD2mId = sponsor.sponsorId,
                    name = sponsorProfile.name,
                    kind = "parent",
                    childId = prospectId,
                    childName = sponsorProfile.childName,
                )
                chatUiState.requestOpenPeer(peerUsername)
                onOpenMessages()
            } catch (e: Exception) {
                messageParentError = friendlyError(e, strings.consentErrStartConversation)
            } finally {
                messageParentBusy = false
            }
        }
    }

    var expanded by rememberSaveable { mutableStateOf(false) }
    val c = card
    when {
        loading -> D2MSkeleton(height = 56.dp, radius = 6.dp)
        loadError != null -> D2MErrorBanner(loadError!!)
        noProspect -> DisclosureRow(
            title = strings.seriousModeRow,
            summary = strings.consentNoSeriousExplorationTitle,
            expanded = expanded,
            onToggle = { expanded = !expanded },
        ) { MetaText(strings.consentNoSeriousExplorationDesc, Modifier.padding(top = 4.dp)) }

        c != null -> DisclosureRow(
            title = strings.seriousModeRow,
            summary = when (c.consentStatus) {
                "granted" -> strings.consentSeriousWithPerson
                "pending" -> strings.consentWaitingOnDecision
                else -> strings.consentSeriousModeHeading
            },
            expanded = expanded,
            onToggle = { expanded = !expanded },
            trailing = when (c.consentStatus) {
                "pending" -> ({ D2MBadge(strings.consentPendingBadge, D2MBadgeTone.WARNING) })
                else -> null
            },
        ) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                    if (c.consentStatus != "granted") {
                        SubHeading(strings.consentSeriousModeHeading)
                        Text(
                            strings.consentSeriousModeBody,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileThumb(photoUrl = null, contentDescription = null, size = 56.dp)
                            Column(modifier = Modifier.padding(start = 14.dp)) {
                                Text(c.nameMasked ?: "", fontWeight = FontWeight.Bold)
                                MetaText(c.ageBucket ?: "", Modifier.padding(top = 2.dp))
                            }
                        }
                        MetaText(strings.consentHiddenUntilAccess, Modifier.padding(top = 14.dp))
                        requestError?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 10.dp)) }
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            if (c.consentStatus == "pending") {
                                D2MBadge(strings.consentPendingBadge, D2MBadgeTone.WARNING)
                                MetaText(strings.consentWaitingOnDecision, Modifier.padding(top = 6.dp))
                            } else {
                                D2MButton(
                                    text = strings.consentRequestAccess,
                                    enabled = !requestBusy,
                                    onClick = { requestAccess() },
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            D2MBadge(strings.consentSeriousWithPerson, D2MBadgeTone.SUCCESS)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp)) {
                            ProfileThumb(
                                photoUrl = c.photoUrl?.let(apiClient::resolveMediaUrl),
                                contentDescription = null,
                                size = 72.dp,
                            )
                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                SubHeading("${c.name ?: ""}, ${c.age ?: ""}")
                                MetaText("${c.religion ?: ""} · ${c.community ?: ""}", Modifier.padding(top = 4.dp))
                            }
                        }
                        MetaText(strings.consentFullAccessBody, Modifier.padding(top = 14.dp))
                        messageParentError?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 10.dp)) }
                        Row(modifier = Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            D2MButton(
                                text = strings.consentViewFullProfile,
                                modifier = Modifier.weight(1f),
                                onClick = { onOpenProfile(c.prospectId) },
                            )
                            D2MButton(
                                text = if (messageParentBusy) strings.consentOpening else strings.consentMessageTheirParent,
                                variant = D2MButtonVariant.OUTLINE,
                                enabled = !messageParentBusy,
                                modifier = Modifier.weight(1f),
                                onClick = { messageParent() },
                            )
                        }
                    }
                }
            }
        }
}
