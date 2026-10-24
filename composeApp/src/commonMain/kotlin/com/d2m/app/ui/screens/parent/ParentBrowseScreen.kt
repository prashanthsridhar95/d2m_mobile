package com.d2m.app.ui.screens.parent

import com.d2m.app.ui.components.D2MActionButton
import com.d2m.app.ui.components.D2MActionEmphasis
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.BrowseCandidateOut
import com.d2m.app.data.model.SubScoreBreakdown
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.DashboardRepository
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.domain.repository.SuggestionsRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonSize
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MFilterSheet
import com.d2m.app.ui.components.D2MSkeleton
import com.d2m.app.ui.components.FacetGroup
import com.d2m.app.ui.components.FacetState
import com.d2m.app.ui.components.MatchCard
import com.d2m.app.ui.components.MatchCardVariant
import com.d2m.app.ui.components.MetaText
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.components.PrefetchPhotos
import com.d2m.app.ui.screens.parity.BrowseTable
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.d2m
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** How many cards are composed-ready at a time; the next page is appended as the user nears the end. */
private const val PAGE_SIZE = 24

/**
 * Browse, photo-forward. Sticky chrome (title, ID search, chip row) sits
 * above a two-column LazyVerticalGrid of portrait photo tiles; filters live
 * in the bottom sheet with an active-count on the chip. Everything
 * filtering-related is client-side over the fetched batch, as before.
 *
 * - Shortlist heart is optimistic with rollback; state lives in a
 *   SnapshotStateMap read only inside the item, so a toggle recomposes one tile.
 * - Items have stable keys + contentType; the visible window grows by
 *   PAGE_SIZE as the user nears the end (pagination-ready for a paged API).
 * - The photos of the next few rows are prefetched while scrolling.
 * - Table view (parity feature) lives in the overflow menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentBrowseScreen(onOpenProfile: (String) -> Unit) {
    val identityStore: IdentityStore = koinInject()
    val suggestionsRepo: SuggestionsRepository = koinInject()
    val identityRepo: IdentityRepository = koinInject()
    val dashboardRepo: DashboardRepository = koinInject()
    val apiClient: ApiClient = koinInject()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()

    // "Provide an ID for each profile for easy search & finding" -- a direct
    // server lookup by short_id, distinct from the client-side facet filters.
    var shortIdQuery by remember { mutableStateOf("") }
    var shortIdSearching by remember { mutableStateOf(false) }
    var shortIdError by remember { mutableStateOf<String?>(null) }

    var mode by remember { mutableStateOf(0) } // 0 = matching, 1 = all
    var tableView by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var matching by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var all by remember { mutableStateOf<List<BrowseCandidateOut>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var notClaimed by remember { mutableStateOf(false) }

    var sheetOpen by remember { mutableStateOf(false) }
    var cityFacet by remember { mutableStateOf(FacetState()) }
    var gothramFacet by remember { mutableStateOf(FacetState()) }
    var sectFacet by remember { mutableStateOf(FacetState()) }
    var starFacet by remember { mutableStateOf(FacetState()) }
    var ageRange by remember { mutableStateOf<ClosedFloatingPointRange<Float>?>(null) }

    // candidateId -> optimistic shortlist state (absent = use the server's).
    val shortlistOverrides = remember { mutableStateMapOf<String, Boolean>() }

    val childPrimaryId = identity.childPrimaryId ?: identity.primaryId
    val strings = LocalStrings.current.parentBrowse

    // A Sponsor whose invite hasn't been redeemed yet has no childPrimaryId;
    // same lookup web's ChildIdGate.jsx does (GET /sponsors/{id}/child).
    LaunchedEffect(identity.sponsorId, childPrimaryId, mode, refreshTick) {
        if (childPrimaryId == null) {
            val sponsorId = identity.sponsorId
            if (sponsorId != null) {
                runCatching { dashboardRepo.getSponsorChild(sponsorId) }
                    .onSuccess { identityStore.updateChildPrimaryId(it.primaryId) }
                    .onFailure { notClaimed = true; loading = false }
            }
            return@LaunchedEffect
        }
        notClaimed = false
        val force = refreshTick > 0
        // Only a first load (or a mode with nothing yet) shows the skeleton;
        // a refresh keeps the current tiles on screen under the pull indicator.
        val haveData = if (mode == 0) matching.isNotEmpty() else all.isNotEmpty()
        if (!haveData) loading = true
        error = null
        try {
            if (mode == 0) matching = suggestionsRepo.getSuggestions(childPrimaryId, forceRefresh = force)
            else all = suggestionsRepo.getBrowseAll(childPrimaryId, forceRefresh = force)
            shortlistOverrides.clear()
        } catch (e: Exception) {
            if (!haveData) error = friendlyError(e, strings.errLoadProfiles)
        } finally {
            loading = false
            refreshing = false
        }
    }

    // Both pools are normalised to SuggestionOut so everything below is
    // written once against one shape. (BrowseCandidateOut is the same record
    // minus the ranked-feed fields.)
    val pool: List<SuggestionOut> = remember(mode, matching, all) {
        if (mode == 0) matching else all.map { c ->
            SuggestionOut(
                candidateId = c.candidateId, candidateName = c.candidateName, shortId = c.shortId,
                age = c.age, city = c.city, nativity = c.nativity,
                occupationTitle = c.occupationTitle, gothram = c.gothram, sect = c.sect,
                moonNakshatra = c.moonNakshatra, moonPada = c.moonPada, employer = c.employer,
                photoUrl = c.photoUrl, scores = c.scores ?: SubScoreBreakdown(),
                doshaFlags = c.doshaFlags, isShortlisted = c.isShortlisted,
                isFoundingMember = c.isFoundingMember, isVerified = c.isVerified,
            )
        }
    }

    val ageBounds = remember(pool) {
        val ages = pool.mapNotNull { it.age }
        if (ages.isEmpty()) null else ages.min().toFloat()..ages.max().toFloat()
    }

    fun matchesAge(s: SuggestionOut): Boolean {
        val r = ageRange ?: return true
        val a = s.age ?: return false
        return a >= r.start.toInt() && a <= r.endInclusive.toInt()
    }

    val filtered = remember(pool, cityFacet, gothramFacet, sectFacet, starFacet, ageRange) {
        pool.filter {
            cityFacet.allows(it.city ?: it.nativity) &&
                gothramFacet.allows(it.gothram) &&
                sectFacet.allows(it.sect) &&
                starFacet.allows(it.moonNakshatra) &&
                matchesAge(it)
        }
    }

    /*
     * Faceted counts: a group's own counts are computed against the pool
     * with every OTHER group's filter applied, but not its own. Counting
     * the fully-filtered list instead would show 0 beside every unselected
     * option in an active group, which is exactly backwards -- those are
     * the alternatives you would most want a count for.
     */
    fun countsFor(key: String, extract: (SuggestionOut) -> String?): Map<String, Int> =
        pool.asSequence()
            .filter { s ->
                (key == "city" || cityFacet.allows(s.city ?: s.nativity)) &&
                    (key == "gothram" || gothramFacet.allows(s.gothram)) &&
                    (key == "sect" || sectFacet.allows(s.sect)) &&
                    (key == "star" || starFacet.allows(s.moonNakshatra)) &&
                    matchesAge(s)
            }
            .mapNotNull(extract)
            .groupingBy { it }
            .eachCount()

    val groups = listOf(
        FacetGroup(strings.facetCity, pool.mapNotNull { it.city ?: it.nativity }.distinct().sorted(),
            countsFor("city") { it.city ?: it.nativity }, cityFacet) { cityFacet = it },
        FacetGroup(strings.facetGothram, pool.mapNotNull { it.gothram }.distinct().sorted(),
            countsFor("gothram") { it.gothram }, gothramFacet) { gothramFacet = it },
        FacetGroup(strings.facetSect, pool.mapNotNull { it.sect }.distinct().sorted(),
            countsFor("sect") { it.sect }, sectFacet) { sectFacet = it },
        FacetGroup(strings.facetNakshatra, pool.mapNotNull { it.moonNakshatra }.distinct().sorted(),
            countsFor("star") { it.moonNakshatra }, starFacet) { starFacet = it },
    )
    val activeFacetCount = groups.count { it.state.isActive } + (if (ageRange != null) 1 else 0)

    fun clearFacets() {
        cityFacet = FacetState(); gothramFacet = FacetState()
        sectFacet = FacetState(); starFacet = FacetState(); ageRange = null
    }

    fun toggleShortlist(s: SuggestionOut, now: Boolean) {
        val pid = childPrimaryId ?: return
        shortlistOverrides[s.candidateId] = now
        scope.launch {
            try {
                if (now) suggestionsRepo.addToShortlist(pid, s.candidateId)
                else suggestionsRepo.removeFromShortlist(pid, s.candidateId)
            } catch (_: Exception) {
                shortlistOverrides[s.candidateId] = !now // roll back
            }
        }
    }

    fun findById() {
        val code = shortIdQuery.trim()
        if (code.isEmpty() || shortIdSearching) return
        scope.launch {
            shortIdSearching = true
            shortIdError = null
            try {
                val result = identityRepo.searchByShortId(code)
                shortIdQuery = ""
                onOpenProfile(result.primaryId)
            } catch (e: Exception) {
                shortIdError = friendlyError(e, strings.errSearchById)
            } finally {
                shortIdSearching = false
            }
        }
    }

    D2MTheme(flow = D2MFlow.PARENT) {
        Column(Modifier.fillMaxSize()) {
            // ---- sticky chrome -------------------------------------------
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        PageTitle(strings.browseTitle)
                        if (!loading && error == null) {
                            MetaText(strings.filteredOfPool(filtered.size, pool.size), Modifier.padding(top = 2.dp))
                        }
                    }
                    if (mode == 1) {
                        Box {
                            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Filled.MoreVert, contentDescription = strings.moreOptions)
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(strings.grid, fontWeight = if (!tableView) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = { tableView = false; menuOpen = false },
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.table, fontWeight = if (tableView) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = { tableView = true; menuOpen = false },
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = shortIdQuery,
                    onValueChange = { shortIdQuery = it; shortIdError = null },
                    placeholder = { Text(strings.searchById, style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (shortIdQuery.isNotBlank()) {
                            TextButton(onClick = { findById() }, enabled = !shortIdSearching, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(if (shortIdSearching) strings.finding else strings.find)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(percent = 50),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { findById() }),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                shortIdError?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 6.dp)) }

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrowseChip(strings.matching, selected = mode == 0, onClick = { mode = 0 })
                    BrowseChip(strings.allProfiles, selected = mode == 1, onClick = { mode = 1 })
                    BrowseChip(
                        label = if (activeFacetCount > 0) strings.filtersWithCount(activeFacetCount) else strings.filters,
                        selected = activeFacetCount > 0,
                        onClick = { sheetOpen = true },
                        leading = Icons.Filled.FilterList,
                        outlinedAccent = true,
                    )
                    if (activeFacetCount > 0) {
                        BrowseChip(strings.clearAllFilters, selected = false, onClick = ::clearFacets)
                    }
                }
            }

            // ---- results ---------------------------------------------------
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
                when {
                    notClaimed -> D2MEmptyState(strings.waitingOnChildTitle, strings.waitingOnChildBody)

                    loading -> BrowseSkeleton()

                    error != null -> D2MErrorBanner(error!!, onRetry = { refreshTick++ }, modifier = Modifier.padding(horizontal = 16.dp))

                    mode == 1 && tableView -> BrowseTable(rows = all, onOpenProfile = onOpenProfile)

                    filtered.isEmpty() && pool.isNotEmpty() -> D2MEmptyState(
                        title = strings.nothingMatchesFiltersTitle,
                        subtitle = strings.nothingMatchesFiltersBody,
                        action = {
                            D2MActionButton(strings.clearFilters, onClick = ::clearFacets, emphasis = D2MActionEmphasis.SECONDARY, height = 48.dp, modifier = Modifier.padding(horizontal = 24.dp))
                        },
                    )

                    filtered.isEmpty() -> D2MEmptyState(
                        title = strings.noProfilesYetTitle,
                        subtitle = if (mode == 0) strings.noProfilesMatchingBody else strings.noProfilesAllBody,
                    )

                    else -> {
                        var shownCount by remember(filtered) { mutableStateOf(PAGE_SIZE) }
                        val shown = remember(filtered, shownCount) { filtered.take(shownCount) }
                        val gridState = rememberLazyGridState()

                        // Grow the window when the user is within ~3 rows of its end.
                        LaunchedEffect(filtered, shown.size) {
                            snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                                .distinctUntilChanged()
                                .collect { last ->
                                    if (last >= shown.size - 6 && shown.size < filtered.size) shownCount += PAGE_SIZE
                                }
                        }
                        PrefetchPhotos(
                            count = shown.size,
                            firstVisibleIndex = { gridState.firstVisibleItemIndex },
                            urlAt = { i -> shown.getOrNull(i)?.photoUrl?.let(apiClient::resolveMediaUrl) },
                            ahead = 4,
                            perRow = 2,
                        )

                        PullToRefreshBox(
                            isRefreshing = refreshing,
                            onRefresh = { refreshing = true; refreshTick++ },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                state = gridState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(shown, key = { it.candidateId }, contentType = { "tile" }) { s ->
                                    // Read inside the item: a toggle recomposes this tile only.
                                    val shortlisted = shortlistOverrides[s.candidateId] ?: s.isShortlisted
                                    MatchCard(
                                        suggestion = s,
                                        resolvePhotoUrl = apiClient::resolveMediaUrl,
                                        onClick = { onOpenProfile(s.candidateId) },
                                        variant = MatchCardVariant.TILE,
                                        isShortlisted = shortlisted,
                                        onToggleShortlist = { toggleShortlist(s, !shortlisted) },
                                    )
                                }
                                if (shown.size < filtered.size) {
                                    item(span = { GridItemSpan(maxLineSpan) }, key = "more", contentType = "more") {
                                        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                            MetaText(strings.loadMore)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (sheetOpen) {
            D2MFilterSheet(
                groups = groups,
                resultCount = filtered.size,
                totalCount = pool.size,
                onDismiss = { sheetOpen = false },
                onClearAll = ::clearFacets,
                ageBounds = ageBounds,
                ageRange = ageRange,
                onAgeChange = { ageRange = it },
            )
        }
    }
}

/** A pill chip with a 48dp-tall touch target around a 36dp visual. */
@Composable
private fun BrowseChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leading: ImageVector? = null,
    outlinedAccent: Boolean = false,
) {
    val shape = RoundedCornerShape(percent = 50)
    val bg = if (selected && !outlinedAccent) d2m.accent else if (selected) d2m.accentSoft else MaterialTheme.colorScheme.surface
    val fg = if (selected && !outlinedAccent) d2m.accentOn else if (selected) d2m.accentStrong else d2m.textPrimary
    val borderColor = if (selected) d2m.accent else d2m.border
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .heightIn(min = 36.dp)
                .clip(shape)
                .background(bg)
                .border(BorderStroke(1.dp, borderColor), shape)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (leading != null) Icon(leading, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
        }
    }
}

/** Same two-column silhouette as the loaded grid so tiles don't jump when data lands. */
@Composable
private fun BrowseSkeleton() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(3) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                D2MSkeleton(modifier = Modifier.weight(1f), height = 220.dp, radius = 14.dp)
                D2MSkeleton(modifier = Modifier.weight(1f), height = 220.dp, radius = 14.dp)
            }
        }
    }
}
