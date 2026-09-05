package com.d2m.app.ui.screens.child

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
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
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MSkeleton
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.components.SubHeading
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.d2m
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.async
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
 */
@Composable
fun ChildHomeScreen(
    onOpenProfile: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenMatches: () -> Unit,
    onOpenChildProfileDialog: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val identityStore: IdentityStore = koinInject()
    val identityRepo: IdentityRepository = koinInject()
    val suggestionsRepo: SuggestionsRepository = koinInject()
    val seriousModeRepo: SeriousModeRepository = koinInject()
    val consentRepo: ConsentRepository = koinInject()
    val apiClient: ApiClient = koinInject()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()

    var profile by remember { mutableStateOf<PrimaryProfileOut?>(null) }
    var ownPhotoUrl by remember { mutableStateOf<String?>(null) }
    var sponsorStatus by remember { mutableStateOf<SponsorStatusOut?>(null) }
    var threads by remember { mutableStateOf<List<ThreadOut>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var received by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var sent by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var consentRequests by remember { mutableStateOf<List<ConsentRequestOut>>(emptyList()) }
    var activeCandidatePhotoUrl by remember { mutableStateOf<String?>(null) }

    // Per-section readiness, not one global gate -- see the doc comment
    // above. Each flips true the moment its own fetch resolves, letting
    // the hero and the list card each show their own shimmer independently
    // instead of the whole page waiting on the slowest of eight calls.
    var profileLoaded by remember { mutableStateOf(false) }
    var threadsLoaded by remember { mutableStateOf(false) }
    var suggestionsLoaded by remember { mutableStateOf(false) }
    var requestsLoaded by remember { mutableStateOf(false) } // received + sent + consent, together

    var error by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var matchBanner by remember { mutableStateOf<String?>(null) }
    var matchBusyId by remember { mutableStateOf<String?>(null) }
    var consentBusyId by remember { mutableStateOf<String?>(null) }

    val primaryId = identity.primaryId

    LaunchedEffect(primaryId) {
        if (primaryId == null) return@LaunchedEffect
        error = null
        try {
            val profileDeferred = async { identityRepo.getPrimaryProfile(primaryId) }
            val photosDeferred = async { runCatching { identityRepo.getPhotos(primaryId) }.getOrDefault(emptyList()) }
            val sponsorStatusDeferred = async { seriousModeRepo.getSponsorStatus(primaryId) }
            val threadsDeferred = async { seriousModeRepo.getThreads(primaryId) }
            val suggestionsDeferred = async { suggestionsRepo.getSuggestions(primaryId) }
            val receivedDeferred = async { suggestionsRepo.getReceivedRequests(primaryId) }
            val sentDeferred = async { suggestionsRepo.getSentRequests(primaryId) }
            val consentDeferred = async { runCatching { consentRepo.getConsentRequests(primaryId) }.getOrDefault(emptyList()) }

            // Await order (not just launch order) doubles as the reveal
            // order -- profile/hero data first since the greeting and hero
            // are the first things on screen, the shared list last since
            // its dependents (showPicks, discoverDisabled) need suggestions
            // and sponsorStatus already resolved by the time it renders.
            profile = profileDeferred.await()
            ownPhotoUrl = photosDeferred.await().firstOrNull()?.url
            profileLoaded = true

            sponsorStatus = sponsorStatusDeferred.await()
            threads = threadsDeferred.await()
            threadsLoaded = true

            val active = threads.filter { it.status != "closed" }
                .let { open -> open.find { it.status == "exclusive" } ?: open.firstOrNull() }
            activeCandidatePhotoUrl = active?.let {
                runCatching { suggestionsRepo.getCandidate(primaryId, it.otherParticipantId).photoUrl }.getOrNull()
            }

            suggestions = suggestionsDeferred.await()
            suggestionsLoaded = true

            received = receivedDeferred.await()
            sent = sentDeferred.await()
            consentRequests = consentDeferred.await()
            requestsLoaded = true
        } catch (e: Exception) {
            error = friendlyError(e, "Couldn't load your home feed.")
        }
    }

    fun handleMatchAction(candidateId: String, action: String) {
        val pid = primaryId ?: return
        scope.launch {
            matchBusyId = candidateId
            try {
                val res = suggestionsRepo.act(pid, candidateId, action)
                if (res.mutualMatch) matchBanner = "It's a match! Head to Matches to say hi."
                received = received.filter { it.candidateId != candidateId }
            } catch (e: Exception) {
                actionError = friendlyError(e, "Couldn't record that decision.")
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
                consentRequests = consentRequests.map { if (it.requestId == requestId) it.copy(status = res.status) else it }
            } catch (e: Exception) {
                actionError = friendlyError(e, "Couldn't record that decision.")
            } finally {
                consentBusyId = null
            }
        }
    }

    D2MTheme(flow = D2MFlow.CHILD) {
        val discoverDisabled = sponsorStatus?.status == "serious_exploration"
        val openThreads = threads.filter { it.status != "closed" }
        val activeThread = openThreads.find { it.status == "exclusive" } ?: openThreads.firstOrNull()
        val matchedIds = openThreads.map { it.otherParticipantId }.toSet()
        val receivedFiltered = received.filter { it.candidateId !in matchedIds }
        val sentFiltered = sent.filter { it.candidateId !in matchedIds }
        val pendingConsentList = consentRequests.filter { it.status == "pending" }
        val pendingOnMe = threads.count { it.pendingSeriousModeRequestId != null && it.pendingSeriousModeRequestedBy != primaryId }
        val showPicks = !discoverDisabled && suggestions.isNotEmpty()

        val firstName = profile?.name?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "there"
        val subtitle = when {
            pendingOnMe > 0 -> "Someone's waiting on your decision in Matches 💛"
            receivedFiltered.isNotEmpty() -> "${receivedFiltered.size} new ${if (receivedFiltered.size == 1) "person" else "people"} sent you a request 👀"
            pendingConsentList.isNotEmpty() -> "Your Sponsor wants to see more about someone you're talking to."
            showPicks -> "A few new profiles showed up for you today ✨"
            else -> "You're all caught up -- nothing waiting on you right now."
        }

        val listItems = buildList {
            if (receivedFiltered.isEmpty() && sentFiltered.isEmpty() && pendingConsentList.isEmpty()) {
                add(HomeListItem.Empty)
            } else {
                receivedFiltered.forEach { add(HomeListItem.Received(it)) }
                pendingConsentList.forEach { add(HomeListItem.ConsentAsk(it)) }
                sentFiltered.forEach { add(HomeListItem.Sent(it)) }
            }
            if (showPicks) add(HomeListItem.Picks(suggestions.size))
            add(HomeListItem.Profile)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        PageTitle("Hey $firstName 👋")
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = mutedText(0.45f), modifier = Modifier.padding(top = 6.dp))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            }

            if (error != null) {
                item { D2MErrorBanner(error!!) }
            }
            if (actionError != null) {
                item { D2MErrorBanner(actionError!!) }
            }
            if (matchBanner != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(D2MRadius.md))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(16.dp),
                    ) {
                        Text("🎉 $matchBanner", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                HeroBanner(
                    loaded = threadsLoaded,
                    activeThread = activeThread,
                    candidatePhotoUrl = activeCandidatePhotoUrl,
                    discoverDisabled = discoverDisabled,
                    apiClient = apiClient,
                    onOpenChat = onOpenMatches,
                    onOpenDiscover = onOpenDiscover,
                )
            }

            item {
                D2MCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        if (!requestsLoaded || !suggestionsLoaded || !profileLoaded) {
                            SkeletonHomeRow()
                            HorizontalDivider(color = mutedText(0.1f))
                            SkeletonHomeRow()
                        } else {
                            listItems.forEachIndexed { index, item ->
                                when (item) {
                                    HomeListItem.Empty -> HomeListRow(
                                        avatar = { GlyphCircle("👀") },
                                        title = "No one new yet",
                                        subtitle = "But they will -- your profile's out there working for you.",
                                    )

                                    is HomeListItem.Received -> {
                                        val busy = matchBusyId == item.s.candidateId
                                        HomeListRow(
                                            avatar = { PhotoCircle(item.s.photoUrl, item.s.candidateName, 40.dp, apiClient) },
                                            title = item.s.candidateName,
                                            subtitle = "Wants to match with you",
                                            onClick = { onOpenProfile(item.s.candidateId) },
                                            action = {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    D2MButton("Accept", size = D2MButtonSize.SM, enabled = !busy, onClick = { handleMatchAction(item.s.candidateId, "accept") })
                                                    D2MButton("Decline", variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM, enabled = !busy, onClick = { handleMatchAction(item.s.candidateId, "reject") })
                                                }
                                            },
                                        )
                                    }

                                    is HomeListItem.ConsentAsk -> {
                                        val busy = consentBusyId == item.r.requestId
                                        HomeListRow(
                                            avatar = { GlyphCircle("💛") },
                                            title = "Your Sponsor wants to see more about ${item.r.prospectName ?: "your match"}",
                                            action = {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    D2MButton("Grant", size = D2MButtonSize.SM, enabled = !busy, onClick = { handleDecide(item.r.requestId, "grant") })
                                                    D2MButton("Deny", variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM, enabled = !busy, onClick = { handleDecide(item.r.requestId, "deny") })
                                                }
                                            },
                                        )
                                    }

                                    is HomeListItem.Sent -> HomeListRow(
                                        avatar = { PhotoCircle(item.s.photoUrl, item.s.candidateName, 40.dp, apiClient) },
                                        title = item.s.candidateName,
                                        subtitle = "Waiting for a response",
                                        onClick = { onOpenProfile(item.s.candidateId) },
                                    )

                                    is HomeListItem.Picks -> HomeListRow(
                                        avatar = { GlyphCircle("💌") },
                                        title = "Today's picks",
                                        subtitle = "${item.count} new ${if (item.count == 1) "profile" else "profiles"} worth a look",
                                        onClick = onOpenDiscover,
                                        action = { D2MButton("Take a look", variant = D2MButtonVariant.OUTLINE, size = D2MButtonSize.SM, onClick = onOpenDiscover) },
                                    )

                                    HomeListItem.Profile -> HomeListRow(
                                        avatar = { PhotoCircle(ownPhotoUrl, profile?.name, 40.dp, apiClient) },
                                        title = "How you're showing up",
                                        subtitle = "A peek at what people see when they check you out.",
                                        onClick = onOpenChildProfileDialog,
                                        action = {
                                            Text(
                                                if (profile?.profileCompleted == true) "View & edit" else "Complete profile",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        },
                                    )
                                }
                                if (index != listItems.lastIndex) HorizontalDivider(color = mutedText(0.1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed class HomeListItem {
    data object Empty : HomeListItem()
    data class Received(val s: SuggestionOut) : HomeListItem()
    data class ConsentAsk(val r: ConsentRequestOut) : HomeListItem()
    data class Sent(val s: SuggestionOut) : HomeListItem()
    data class Picks(val count: Int) : HomeListItem()
    data object Profile : HomeListItem()
}

/**
 * The one big visual moment on the page -- full-bleed photo of whoever
 * you're actually talking to (most-open thread, exclusive wins), name/
 * status on a bottom scrim, "Open chat" floating top-right on the image
 * itself. No active thread yet gets a soft gradient invite into Discover
 * instead, sized to match rather than a smaller fallback -- mirrors
 * HomeScreen.jsx's HeroBanner exactly. `loaded=false` (threads not
 * resolved yet) renders a shimmer block at the same height instead of
 * either real state, same as web's Skeleton fallback there.
 */
@Composable
private fun HeroBanner(
    loaded: Boolean,
    activeThread: ThreadOut?,
    candidatePhotoUrl: String?,
    discoverDisabled: Boolean,
    apiClient: ApiClient,
    onOpenChat: () -> Unit,
    onOpenDiscover: () -> Unit,
) {
    val shape = RoundedCornerShape(D2MRadius.lg)

    if (!loaded) {
        D2MSkeleton(modifier = Modifier.fillMaxWidth(), height = 220.dp, radius = D2MRadius.lg)
        return
    }

    if (activeThread != null) {
        val resolvedPhoto = candidatePhotoUrl?.let(apiClient::resolveMediaUrl)
        val isExclusive = activeThread.status == "exclusive"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(shape)
                .then(
                    if (resolvedPhoto == null) {
                        Modifier.background(SolidColor(d2m.surfaceSunken))
                    } else Modifier,
                )
                .clickable(onClick = onOpenChat),
        ) {
            if (resolvedPhoto != null) {
                AsyncImage(
                    model = resolvedPhoto,
                    contentDescription = activeThread.otherParticipantName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    (activeThread.otherParticipantName.trim().firstOrNull() ?: '?').uppercaseChar().toString(),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = d2m.label,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

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
                        if (isExclusive) "You two are getting serious 💛" else "You matched -- say hi 💬",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            D2MButton(
                "Open chat",
                onClick = onOpenChat,
                size = D2MButtonSize.SM,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(shape)
                .background(SolidColor(d2m.surfaceSunken))
                .then(if (!discoverDisabled) Modifier.clickable(onClick = onOpenDiscover) else Modifier)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("✨", fontSize = 40.sp)
            SubHeading("Your person's out there", Modifier.padding(top = 10.dp))
            Text(
                "No matches yet -- let's find someone worth a hello.",
                style = MaterialTheme.typography.bodyMedium,
                color = d2m.meta,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (!discoverDisabled) {
                D2MButton("Let's go", onClick = onOpenDiscover, size = D2MButtonSize.SM, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

/**
 * One compact row inside the shared list Card -- avatar, title + optional
 * subtitle, optional trailing action. Thin dividers between rows (added by
 * the caller, not this composable) are what make this read as one list
 * rather than several stacked boxes, same as ListRow.jsx.
 */
@Composable
private fun HomeListRow(
    avatar: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        avatar()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = mutedText(0.45f), modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (action != null) action()
    }
}

/** Shimmer placeholder for one HomeListRow -- circle + two bars, same shape/padding as the real row. */
@Composable
private fun SkeletonHomeRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        D2MSkeleton(width = 40.dp, height = 40.dp, radius = 20.dp)
        Column(modifier = Modifier.weight(1f)) {
            D2MSkeleton(width = 140.dp, height = 13.dp)
            Spacer(Modifier.height(6.dp))
            D2MSkeleton(width = 90.dp, height = 11.dp)
        }
    }
}

/** Gradient avatar circle with a real photo (cropped) or an initial fallback -- mirrors PhotoCircle.jsx. */
@Composable
private fun PhotoCircle(url: String?, name: String?, size: Dp, apiClient: ApiClient) {
    val resolved = url?.let(apiClient::resolveMediaUrl)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(SolidColor(d2m.surfaceSunken)),
        contentAlignment = Alignment.Center,
    ) {
        if (resolved != null) {
            AsyncImage(model = resolved, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                (name?.trim()?.firstOrNull() ?: '?').uppercaseChar().toString(),
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36f).sp,
                color = d2m.label,
            )
        }
    }
}

/** Gradient avatar circle with an emoji glyph instead of a photo -- mirrors GlyphCircle.jsx. */
@Composable
private fun GlyphCircle(glyph: String, size: Dp = 40.dp) {
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
