package com.d2m.app.ui.screens.child

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d2m.app.data.model.ConsentRequestOut
import com.d2m.app.data.model.PrimaryProfileOut
import com.d2m.app.data.model.SponsorStatusOut
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.model.ThreadOut
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.ConsentRepository
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.domain.repository.SeriousModeRepository
import com.d2m.app.domain.repository.SuggestionsRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonSize
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MSkeleton
import com.d2m.app.ui.components.LocalD2MSnackbar
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.components.ProfilePhoto
import com.d2m.app.ui.components.ProfileThumb
import com.d2m.app.ui.components.SkeletonListRow
import com.d2m.app.ui.components.SubHeading
import com.d2m.app.ui.components.bounceClickable
import com.d2m.app.ui.components.rememberHaptics
import com.d2m.app.ui.navigation.UnreadBadges
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.d2m
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Mirrors screens/child/HomeScreen.jsx -- see that file's own docstring for
 * the three rounds of direct feedback that shaped this layout there
 * (rejected a stat-tile dashboard twice, landed on one full-bleed photo
 * hero + a single compact list underneath instead of several same-weight
 * boxed cards).
 *
 * This platform's first cut skipped straight to a stack of boxed suggestion
 * cards with raw "78.89/10" score badges and no hero at all -- reported
 * directly with a side-by-side screenshot comparison ("this is home in web
 * & mobile, I want mobile to be similar to web"). Rebuilt as a direct
 * structural port of the real thing: greeting + one-line status (same
 * priority-ordered subtitle logic), a photo hero (whoever you're actually
 * talking to, or a soft gradient invite into Discover), then one shared
 * list card with thin dividers between rows instead of a stack of
 * separately-boxed cards -- received requests / consent asks / sent
 * requests / today's picks / profile peek, same priority order and the
 * same inline accept-decline / grant-deny actions as web.
 *
 * One deliberate gap from web: the hero's "Open chat" doesn't deep-link
 * into a specific thread inside Matches (web passes router state carrying
 * threadId) -- MatchesScreen on this platform doesn't yet accept a
 * preselected thread argument, so this just opens the Matches tab and lets
 * the user pick, same as "View matches" always did.
 *
 * Settings gear (top-right, next to the greeting): moved here on request
 * ("Remove settings from bottom nav & move it to home - show an icon in
 * top right") -- see AppScaffold.kt's doc comment, ChildTabs no longer
 * includes a Settings entry at all.
 *
 * Loading behavior (reported directly -- a blocking "Loading" screen was
 * showing on every single visit to Home, not just the first): this used to
 * gate the ENTIRE screen behind one `loading` boolean that got reset to
 * `true` at the top of every LaunchedEffect run, which fires on every fresh
 * composition -- and since this composable's plain `remember` state doesn't
 * survive navigating to another bottom-tab and back (Compose disposes it,
 * bottom-nav's saveState/restoreState only preserves the NAVIGATION
 * backstack, not this screen's own local state), that was really "every
 * time you open Home." The actual network layer was rarely the bottleneck
 * -- ApiCache is one app-wide singleton (see di/AppModule.kt), so a
 * revisit within its TTL is normally a fast in-memory hit already -- the
 * visible stall was purely the UI blocking on a single all-or-nothing
 * `loading` flag instead of rendering what's already known and filling in
 * the rest as it arrives. Replaced with per-section `*Loaded` flags: the
 * greeting/hero/list shell renders immediately every time, each section
 * shows a shimmer skeleton only until ITS OWN data resolves (near-instant
 * on a warm cache), and there is no more full-screen "Loading…" text at
 * all -- refreshing now genuinely happens in the background the way a
 * revisit should feel.
  *
 * Focus pass (mobile UX upgrade): the screen is now deliberately small --
 * greeting, ONE primary call to action toward Discover (or the active chat
 * when Discover is paused for Serious Mode), a "waiting on you" card only
 * when something actually needs a decision, and a compact summary list
 * (matches / notifications / sent / profile). Pull-to-refresh force-reloads
 * everything; success/failure feedback goes through the app-wide snackbar and
 * haptics instead of inline banners.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildHomeScreen(
    onOpenProfile: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenMatches: () -> Unit,
    onOpenChildProfileDialog: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit = {},
) {
    val identityStore: IdentityStore = koinInject()
    val identityRepo: IdentityRepository = koinInject()
    val suggestionsRepo: SuggestionsRepository = koinInject()
    val seriousModeRepo: SeriousModeRepository = koinInject()
    val consentRepo: ConsentRepository = koinInject()
    val apiClient: ApiClient = koinInject()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = LocalD2MSnackbar.current
    val haptics = rememberHaptics()
    val unreadNotifications by UnreadBadges.notifications.collectAsState()

    var profile by remember { mutableStateOf<PrimaryProfileOut?>(null) }
    var ownPhotoUrl by remember { mutableStateOf<String?>(null) }
    var sponsorStatus by remember { mutableStateOf<SponsorStatusOut?>(null) }
    var threads by remember { mutableStateOf<List<ThreadOut>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var received by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var sent by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var consentRequests by remember { mutableStateOf<List<ConsentRequestOut>>(emptyList()) }
    var activeCandidatePhotoUrl by remember { mutableStateOf<String?>(null) }

    // Per-section readiness (see the loading note above): each flips true the
    // moment its own fetch resolves, so a warm cache paints instantly.
    var profileLoaded by remember { mutableStateOf(false) }
    var threadsLoaded by remember { mutableStateOf(false) }
    var suggestionsLoaded by remember { mutableStateOf(false) }
    var requestsLoaded by remember { mutableStateOf(false) }

    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var matchBusyId by remember { mutableStateOf<String?>(null) }
    var consentBusyId by remember { mutableStateOf<String?>(null) }

    val primaryId = identity.primaryId
    val strings = LocalStrings.current.childHome

    suspend fun loadAll(force: Boolean) {
        val pid = primaryId ?: return
        error = null
        try {
            // coroutineScope{} is load-bearing: it makes the group of async{}
            // calls fail as one unit whose exception THIS try/catch sees,
            // instead of crashing the parent job (see git history: FATAL
            // EXCEPTION on expired session).
            coroutineScope {
                val profileDeferred = async { identityRepo.getPrimaryProfile(pid, forceRefresh = force) }
                val photosDeferred = async { runCatching { identityRepo.getPhotos(pid, forceRefresh = force) }.getOrDefault(emptyList()) }
                val sponsorStatusDeferred = async { seriousModeRepo.getSponsorStatus(pid) }
                val threadsDeferred = async { seriousModeRepo.getThreads(pid, forceRefresh = force) }
                val suggestionsDeferred = async { suggestionsRepo.getSuggestions(pid, forceRefresh = force) }
                val receivedDeferred = async { suggestionsRepo.getReceivedRequests(pid) }
                val sentDeferred = async { suggestionsRepo.getSentRequests(pid) }
                val consentDeferred = async { runCatching { consentRepo.getConsentRequests(pid) }.getOrDefault(emptyList()) }

                // Await order doubles as reveal order.
                profile = profileDeferred.await()
                ownPhotoUrl = photosDeferred.await().firstOrNull()?.url
                profileLoaded = true

                sponsorStatus = sponsorStatusDeferred.await()
                threads = threadsDeferred.await()
                threadsLoaded = true

                val active = threads.filter { it.status != "closed" }
                    .let { open -> open.find { it.status == "exclusive" } ?: open.firstOrNull() }
                activeCandidatePhotoUrl = active?.let {
                    runCatching { suggestionsRepo.getCandidate(pid, it.otherParticipantId).photoUrl }.getOrNull()
                }

                suggestions = suggestionsDeferred.await()
                suggestionsLoaded = true

                received = receivedDeferred.await()
                sent = sentDeferred.await()
                consentRequests = consentDeferred.await()
                requestsLoaded = true
            }
        } catch (e: Exception) {
            error = friendlyError(e, strings.errLoadHomeFeed)
        }
    }

    LaunchedEffect(primaryId) { loadAll(force = false) }

    fun handleMatchAction(candidateId: String, action: String) {
        val pid = primaryId ?: return
        scope.launch {
            matchBusyId = candidateId
            try {
                val res = suggestionsRepo.act(pid, candidateId, action)
                if (res.mutualMatch) {
                    haptics.success()
                    snackbar.showSuccess(strings.matchBanner)
                } else haptics.medium()
                received = received.filter { it.candidateId != candidateId }
            } catch (e: Exception) {
                snackbar.showError(friendlyError(e, strings.errRecordDecision))
            } finally {
                matchBusyId = null
            }
        }
    }

    fun handleDecide(requestId: String, decision: String) {
        val pid = primaryId ?: return
        scope.launch {
            consentBusyId = requestId
            try {
                val res = consentRepo.decide(pid, requestId, decision)
                haptics.medium()
                consentRequests = consentRequests.map { if (it.requestId == requestId) it.copy(status = res.status) else it }
            } catch (e: Exception) {
                snackbar.showError(friendlyError(e, strings.errRecordDecision))
            } finally {
                consentBusyId = null
            }
        }
    }

    // Derived once per data change, not per recomposition.
    val discoverDisabled = sponsorStatus?.status == "serious_exploration"
    val openThreads = remember(threads) { threads.filter { it.status != "closed" } }
    val activeThread = remember(openThreads) { openThreads.find { it.status == "exclusive" } ?: openThreads.firstOrNull() }
    val matchedIds = remember(openThreads) { openThreads.map { it.otherParticipantId }.toSet() }
    val receivedFiltered = remember(received, matchedIds) { received.filter { it.candidateId !in matchedIds } }
    val sentFiltered = remember(sent, matchedIds) { sent.filter { it.candidateId !in matchedIds } }
    val pendingConsentList = remember(consentRequests) { consentRequests.filter { it.status == "pending" } }
    val pendingOnMe = remember(threads, primaryId) {
        threads.count { it.pendingSeriousModeRequestId != null && it.pendingSeriousModeRequestedBy != primaryId }
    }
    val showPicks = !discoverDisabled && suggestions.isNotEmpty()

    val firstName = profile?.name?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: strings.greetingFallbackName
    val subtitle = when {
        pendingOnMe > 0 -> strings.subtitlePendingDecision
        receivedFiltered.isNotEmpty() -> strings.subtitleNewRequest(receivedFiltered.size)
        pendingConsentList.isNotEmpty() -> strings.subtitleConsentPending
        showPicks -> strings.subtitleNewPicks
        else -> strings.subtitleCaughtUp
    }

    D2MTheme(flow = D2MFlow.CHILD) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    loadAll(force = true)
                    refreshing = false
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item(key = "header", contentType = "header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            PageTitle(strings.greeting(firstName))
                            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = mutedText(0.45f), modifier = Modifier.padding(top = 6.dp))
                        }
                        Box(
                            Modifier
                                .bounceClickable(onClick = onOpenSettings)
                                .size(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = strings.settingsContentDescription)
                        }
                    }
                }

                error?.let { msg ->
                    item(key = "error", contentType = "banner") {
                        D2MErrorBanner(msg, onRetry = { scope.launch { loadAll(force = true) } })
                    }
                }

                item(key = "cta", contentType = "cta") {
                    PrimaryCta(
                        loaded = threadsLoaded && suggestionsLoaded,
                        activeThread = activeThread,
                        candidatePhotoUrl = activeCandidatePhotoUrl,
                        discoverDisabled = discoverDisabled,
                        pickCount = suggestions.size,
                        apiClient = apiClient,
                        onOpenChat = onOpenMatches,
                        onOpenDiscover = onOpenDiscover,
                    )
                }

                if (requestsLoaded && (receivedFiltered.isNotEmpty() || pendingConsentList.isNotEmpty())) {
                    item(key = "waiting", contentType = "card") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SubHeading(strings.waitingOnYouTitle)
                            D2MCard(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    receivedFiltered.forEachIndexed { index, r ->
                                        val busy = matchBusyId == r.candidateId
                                        if (index > 0) HorizontalDivider(color = mutedText(0.1f))
                                        SummaryRow(
                                            avatar = { PhotoCircle(r.photoUrl, r.candidateName, 44.dp, apiClient) },
                                            title = r.candidateName,
                                            subtitle = strings.wantsToMatch,
                                            onClick = { onOpenProfile(r.candidateId) },
                                            trailing = {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    D2MButton(strings.accept, size = D2MButtonSize.SM, enabled = !busy, onClick = { handleMatchAction(r.candidateId, "accept") })
                                                    D2MButton(strings.decline, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM, enabled = !busy, onClick = { handleMatchAction(r.candidateId, "reject") })
                                                }
                                            },
                                        )
                                    }
                                    pendingConsentList.forEachIndexed { index, c ->
                                        val busy = consentBusyId == c.requestId
                                        if (index > 0 || receivedFiltered.isNotEmpty()) HorizontalDivider(color = mutedText(0.1f))
                                        SummaryRow(
                                            avatar = { GlyphCircle("💛", 44.dp) },
                                            title = strings.consentRowTitle(c.prospectName ?: strings.consentFallbackName),
                                            trailing = {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    D2MButton(strings.grant, size = D2MButtonSize.SM, enabled = !busy, onClick = { handleDecide(c.requestId, "grant") })
                                                    D2MButton(strings.deny, variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM, enabled = !busy, onClick = { handleDecide(c.requestId, "deny") })
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "summary", contentType = "card") {
                    D2MCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            if (!threadsLoaded || !requestsLoaded || !profileLoaded) {
                                SkeletonListRow(Modifier.padding(horizontal = 18.dp), avatarSize = 44.dp)
                                SkeletonListRow(Modifier.padding(horizontal = 18.dp), avatarSize = 44.dp)
                                SkeletonListRow(Modifier.padding(horizontal = 18.dp), avatarSize = 44.dp)
                            } else {
                                SummaryRow(
                                    avatar = {
                                        if (activeCandidatePhotoUrl != null && activeThread != null) {
                                            PhotoCircle(activeCandidatePhotoUrl, activeThread.otherParticipantName, 44.dp, apiClient)
                                        } else GlyphCircle("💬", 44.dp)
                                    },
                                    title = strings.summaryMatchesTitle,
                                    subtitle = strings.summaryMatchesSubtitle(openThreads.size),
                                    onClick = onOpenMatches,
                                )
                                HorizontalDivider(color = mutedText(0.1f))
                                SummaryRow(
                                    avatar = { GlyphCircle("🔔", 44.dp) },
                                    title = strings.summaryNotificationsTitle,
                                    subtitle = strings.summaryNotificationsSubtitle(unreadNotifications),
                                    onClick = onOpenNotifications,
                                    trailing = if (unreadNotifications > 0) {
                                        { CountDot(unreadNotifications) }
                                    } else null,
                                )
                                if (sentFiltered.isNotEmpty()) {
                                    HorizontalDivider(color = mutedText(0.1f))
                                    SummaryRow(
                                        avatar = { GlyphCircle("✉️", 44.dp) },
                                        title = strings.summarySentTitle,
                                        subtitle = strings.summarySentSubtitle(sentFiltered.size),
                                        onClick = onOpenMatches,
                                    )
                                }
                                HorizontalDivider(color = mutedText(0.1f))
                                SummaryRow(
                                    avatar = { PhotoCircle(ownPhotoUrl, profile?.name, 44.dp, apiClient) },
                                    title = strings.yourProfileTitle,
                                    subtitle = strings.yourProfileSubtitle,
                                    onClick = onOpenChildProfileDialog,
                                    trailing = {
                                        Text(
                                            if (profile?.profileCompleted == true) strings.viewAndEdit else strings.completeProfile,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountDot(count: Int) {
    Box(
        Modifier
            .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
            .background(d2m.accent, CircleShape)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (count > 99) "99+" else count.toString(), color = d2m.accentOn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * The one big visual moment. Discover available: a dark panel inviting you in
 * (with today's pick count when there is one). Discover paused (Serious
 * Mode): the photo of whoever you're talking to, tap to open the chat.
 */
@Composable
private fun PrimaryCta(
    loaded: Boolean,
    activeThread: ThreadOut?,
    candidatePhotoUrl: String?,
    discoverDisabled: Boolean,
    pickCount: Int,
    apiClient: ApiClient,
    onOpenChat: () -> Unit,
    onOpenDiscover: () -> Unit,
) {
    val shape = RoundedCornerShape(D2MRadius.lg)
    val strings = LocalStrings.current.childHome

    if (!loaded) {
        D2MSkeleton(modifier = Modifier.fillMaxWidth(), height = 190.dp, radius = D2MRadius.lg)
        return
    }

    if (discoverDisabled && activeThread != null) {
        val resolvedPhoto = remember(candidatePhotoUrl) { candidatePhotoUrl?.let(apiClient::resolveMediaUrl) }
        val isExclusive = activeThread.status == "exclusive"
        ProfilePhoto(
            photoUrl = resolvedPhoto,
            contentDescription = activeThread.otherParticipantName,
            modifier = Modifier.bounceClickable(onClick = onOpenChat, pressedScale = 0.985f, enforceMinimumTarget = false),
            fixedHeight = 220.dp,
            shape = shape,
            fallback = {
                Text(
                    (activeThread.otherParticipantName.trim().firstOrNull() ?: '?').uppercaseChar().toString(),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = d2m.label,
                )
            },
            overlay = {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0xCC181210))))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Column {
                        Text(activeThread.otherParticipantName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            if (isExclusive) strings.heroGettingSerious else strings.heroMatched,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                D2MButton(
                    strings.openChat,
                    onClick = onOpenChat,
                    size = D2MButtonSize.SM,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                )
            },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SolidColor(d2m.panelBg))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            strings.discoverHeadline.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = d2m.goldButton,
            fontWeight = FontWeight.Bold,
        )
        Text(
            if (pickCount > 0) strings.todaysPicksTitle else strings.emptyHeroTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = d2m.panelFg,
            fontWeight = FontWeight.Bold,
        )
        Text(
            if (pickCount > 0) strings.todaysPicksSubtitle(pickCount) else strings.emptyHeroSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = d2m.panelFgMuted,
        )
        Spacer(Modifier.height(10.dp))
        D2MButton(
            if (pickCount > 0) strings.takeALook else strings.emptyHeroCta,
            onClick = onOpenDiscover,
            variant = D2MButtonVariant.GOLD,
            size = D2MButtonSize.LG,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** One compact row inside a shared list card: avatar, title (+ subtitle), optional trailing action. 56dp+ tap target. */
@Composable
private fun SummaryRow(
    avatar: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.bounceClickable(onClick = onClick, pressedScale = 0.985f, enforceMinimumTarget = false) else Modifier)
            .defaultMinSize(minHeight = 64.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        avatar()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = mutedText(0.45f), modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (trailing != null) trailing()
    }
}

/** Avatar circle with a real photo (cropped) or an initial fallback -- mirrors PhotoCircle.jsx. */
@Composable
private fun PhotoCircle(url: String?, name: String?, size: Dp, apiClient: ApiClient) {
    val resolved = remember(url) { url?.let(apiClient::resolveMediaUrl) }
    ProfileThumb(
        photoUrl = resolved,
        contentDescription = name,
        size = size,
        shape = CircleShape,
        fallback = {
            Text(
                (name?.trim()?.firstOrNull() ?: '?').uppercaseChar().toString(),
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36f).sp,
                color = d2m.label,
            )
        },
    )
}

/** Avatar circle with an emoji glyph instead of a photo -- mirrors GlyphCircle.jsx. */
@Composable
private fun GlyphCircle(glyph: String, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(SolidColor(d2m.surfaceSunken)),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = (size.value * 0.45f).sp)
    }
}
