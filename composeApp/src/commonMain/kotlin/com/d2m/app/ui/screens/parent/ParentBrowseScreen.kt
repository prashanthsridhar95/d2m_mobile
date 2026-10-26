package com.d2m.app.ui.screens.parent

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import com.d2m.app.platform.rememberReduceMotion
import com.d2m.app.ui.components.FacetMode
import com.d2m.app.ui.components.LocalD2MSnackbar
import com.d2m.app.ui.components.ProfileRow
import com.d2m.app.ui.components.ProfileRowSkeleton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.runtime.SideEffect
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
import com.d2m.app.ui.screens.parity.BrowseTableSkeleton
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.d2m
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** How many cards are composed-ready at a time; the next page is appended as the user nears the end. */
private const val PAGE_SIZE = 24

/** One removable "applied filter" chip. */
private class AppliedChip(val id: String, val label: String, val onRemove: () -> Unit)

/**
 * Browse, synced to web's compact redesign.
 *
 * - Default view is a compact LIST of profile rows (64dp thumb, badges, "age . ft/in . city", level pill + facts,
 *   separate star). View choice List | Cards | Compare is persisted across launches; search/sort/pool/filters are kept
 *   per child profile in memory so opening a profile and coming back never loses them (BrowseState.kt).
 * - Sort chip opens a radio sheet (Best match, Compatibility, Age, Name); unknown values sort last, in every view.
 * - Each applied filter value is its own removable chip; "Reset all" appears from two chips up.
 * - Shortlist is optimistic with rollback, and a successful toggle offers a 6s Undo snackbar.
 * - Filters live in the bottom sheet; everything filtering-related is client-side over the fetched batch.
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
    val snackbar = LocalD2MSnackbar.current
    val reduceMotion = rememberReduceMotion()

    val childPrimaryId = identity.childPrimaryId ?: identity.primaryId
    val strings = LocalStrings.current.parentBrowse
    val stateKey = childPrimaryId ?: ""
    val saved = remember(stateKey) { BrowseStateHolder.load(stateKey) }

    // Name / registration-number search: filters the list live as you type; submitting also tries the server-side
    // short_id lookup (the existing "find profile by ID" behaviour). Kept across navigation.
    var query by remember(stateKey) { mutableStateOf(saved.query) }
    var shortIdSearching by remember { mutableStateOf(false) }
    var shortIdError by remember { mutableStateOf<String?>(null) }

    var mode by remember(stateKey) { mutableStateOf(saved.mode) } // 0 = matching, 1 = all
    var sort by remember(stateKey) { mutableStateOf(saved.sort) }
    var view by remember { mutableStateOf(BrowseStateHolder.loadView()) }
    var matching by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var all by remember { mutableStateOf<List<BrowseCandidateOut>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var notClaimed by remember { mutableStateOf(false) }

    var sheetOpen by remember { mutableStateOf(false) }
    var sortSheetOpen by remember { mutableStateOf(false) }
    var viewSheetOpen by remember { mutableStateOf(false) }
    var cityFacet by remember(stateKey) { mutableStateOf(saved.city) }
    var gothramFacet by remember(stateKey) { mutableStateOf(saved.gothram) }
    var sectFacet by remember(stateKey) { mutableStateOf(saved.sect) }
    var starFacet by remember(stateKey) { mutableStateOf(saved.star) }
    var ageRange by remember(stateKey) { mutableStateOf(saved.ageRange) }

    // Remember it all for this child profile so Back from a profile restores the screen as it was.
    SideEffect {
        if (stateKey.isNotEmpty()) {
            BrowseStateHolder.save(stateKey, BrowseSavedState(query, sort, mode, cityFacet, gothramFacet, sectFacet, starFacet, ageRange))
        }
    }

    // candidateId -> optimistic shortlist state (absent = use the server's).
    val shortlistOverrides = remember { mutableStateMapOf<String, Boolean>() }

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
                heightCm = c.heightCm, financialStatus = c.financialStatus, complexion = c.complexion,
                photoUrl = c.photoUrl, scores = c.scores ?: SubScoreBreakdown(),
                doshaFlags = c.doshaFlags, isShortlisted = c.isShortlisted,
                isFoundingMember = c.isFoundingMember, isVerified = c.isVerified,
            )
        }
    }
    // In the all-profiles pool "best match" floats the candidates that are also in the ranked feed.
    val matchingIds: Set<String>? = remember(mode, all) {
        if (mode == 1) all.filter { it.isMatching }.map { it.candidateId }.toSet() else null
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

    val q = query.trim()
    fun matchesQuery(s: SuggestionOut): Boolean =
        q.isEmpty() || s.candidateName.contains(q, ignoreCase = true) || (s.shortId?.contains(q, ignoreCase = true) == true)

    val filtered = remember(pool, q, cityFacet, gothramFacet, sectFacet, starFacet, ageRange) {
        pool.filter {
            matchesQuery(it) &&
                cityFacet.allows(it.city ?: it.nativity) &&
                gothramFacet.allows(it.gothram) &&
                sectFacet.allows(it.sect) &&
                starFacet.allows(it.moonNakshatra) &&
                matchesAge(it)
        }
    }
    // The chosen sort applies to the filtered list for every view.
    val sorted = remember(filtered, sort, matchingIds) { sortProfiles(filtered, sort, matchingIds) }

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
                matchesQuery(s) &&
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

    // One removable chip per applied value, so "what is applied" is always visible and one tap undoes it.
    val appliedChips = buildList {
        groups.forEach { g ->
            g.state.values.sorted().forEach { v ->
                add(
                    AppliedChip(
                        id = "${g.title}:$v",
                        label = if (g.state.mode == FacetMode.EXCLUDE) strings.chipNot(v) else "${g.title}: $v",
                        onRemove = { g.onChange(g.state.copy(values = g.state.values - v)) },
                    ),
                )
            }
        }
        ageRange?.let { r -> add(AppliedChip("age", strings.chipAge(r.start.toInt(), r.endInclusive.toInt())) { ageRange = null }) }
    }

    fun clearFacets() {
        cityFacet = FacetState(); gothramFacet = FacetState()
        sectFacet = FacetState(); starFacet = FacetState(); ageRange = null
    }
    fun clearAllAndQuery() { clearFacets(); query = "" }

    // Optimistic: flips at once, rolls back (with a message) if the server refuses, and a successful toggle offers a
    // 6s Undo. Runs on a screen-independent scope so Undo still works after the user has opened a profile.
    fun toggleShortlist(s: SuggestionOut, now: Boolean, silent: Boolean = false) {
        val pid = childPrimaryId ?: return
        shortlistOverrides[s.candidateId] = now
        BrowseStateHolder.appScope.launch {
            try {
                if (now) suggestionsRepo.addToShortlist(pid, s.candidateId)
                else suggestionsRepo.removeFromShortlist(pid, s.candidateId)
                if (!silent) {
                    snackbar.show(
                        message = if (now) strings.shortlistAddedToast(s.candidateName) else strings.shortlistRemovedToast(s.candidateName),
                        actionLabel = strings.undo,
                        onAction = { toggleShortlist(s, !now, silent = true) },
                    )
                }
            } catch (_: Exception) {
                shortlistOverrides[s.candidateId] = !now // roll back
                snackbar.showError(strings.couldntUpdateShortlist)
            }
        }
    }

    fun findById() {
        val code = query.trim()
        if (code.isEmpty() || shortIdSearching) return
        scope.launch {
            shortIdSearching = true
            shortIdError = null
            try {
                val result = identityRepo.searchByShortId(code)
                onOpenProfile(result.primaryId)
            } catch (e: Exception) {
                // Not a registration number: the live name filter already shows what matched, so only a
                // genuine failure (not "no such profile") is worth an error banner.
                if (e !is com.d2m.app.data.network.ApiError || e.status != 404) shortIdError = friendlyError(e, strings.errSearchById)
            } finally {
                shortIdSearching = false
            }
        }
    }

    var searchOpen by remember(stateKey) { mutableStateOf(saved.query.isNotBlank()) }
    val searchFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    fun closeSearch() {
        searchOpen = false
        query = ""
        shortIdError = null
        focusManager.clearFocus()
    }
    LaunchedEffect(searchOpen) { if (searchOpen && saved.query.isBlank()) searchFocus.requestFocus() }

    // While a load error (or the 'waiting on your child' state) is showing, the screen offers only that message and its Retry:
    // search, filters, mode chips and the view toggle would act on data that isn't there.
    val showChrome = error == null && !notClaimed

    // "N profiles" with "of M" when something narrows the pool.
    val countText = strings.profilesCount(sorted.size) + if (sorted.size != pool.size) " " + strings.ofTotal(pool.size) else ""

    D2MTheme(flow = D2MFlow.PARENT) {
        Column(Modifier.fillMaxSize()) {
            // ---- sticky chrome -------------------------------------------
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Crossfade(
                    targetState = searchOpen && showChrome,
                    animationSpec = if (reduceMotion) snap() else tween(300),
                    label = "browse-header",
                ) { searching ->
                    if (searching) {
                        // Search by name / registration number, only while asked for: it replaces the title row instead of
                        // permanently taking a third of the screen above the results.
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ToolbarIcon(Icons.AutoMirrored.Filled.ArrowBack, strings.closeSearch, onClick = ::closeSearch, bordered = false)
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it; shortIdError = null },
                                placeholder = { Text(strings.searchShortPlaceholder, style = MaterialTheme.typography.bodyMedium) },
                                trailingIcon = {
                                    if (query.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { query = ""; shortIdError = null }, modifier = Modifier.size(48.dp)) {
                                                Icon(Icons.Filled.Close, contentDescription = strings.clearSearch, modifier = Modifier.size(18.dp))
                                            }
                                            TextButton(onClick = { findById() }, enabled = !shortIdSearching, modifier = Modifier.heightIn(min = 48.dp)) {
                                                Text(if (shortIdSearching) strings.finding else strings.find)
                                            }
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(percent = 50),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { findById() }),
                                modifier = Modifier.weight(1f).focusRequester(searchFocus),
                            )
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                PageTitle(strings.browseTitle)
                                if (!loading && error == null) {
                                    MetaText(countText, Modifier.padding(top = 2.dp).semantics { liveRegion = LiveRegionMode.Polite })
                                }
                            }
                            if (showChrome) {
                                ToolbarIcon(Icons.Filled.Search, strings.searchById, onClick = { searchOpen = true })
                                ToolbarIcon(
                                    Icons.Filled.FilterList,
                                    if (activeFacetCount > 0) strings.filtersWithCount(activeFacetCount) else strings.filters,
                                    onClick = { sheetOpen = true },
                                    badge = activeFacetCount,
                                )
                                ToolbarIcon(view.icon(), strings.viewButtonDesc(view.label(strings)), onClick = { viewSheetOpen = true })
                            }
                        }
                    }
                }
                shortIdError?.let { D2MErrorBanner(it, modifier = Modifier.padding(top = 6.dp)) }
                if (searchOpen && showChrome && !loading) {
                    MetaText(countText, Modifier.padding(start = 4.dp, top = 2.dp).semantics { liveRegion = LiveRegionMode.Polite })
                }

                if (showChrome) {
                    // Matching | All profiles
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp).horizontalScroll(rememberScrollState()).selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BrowseChip(strings.matching, selected = mode == 0, onClick = { mode = 0 })
                        BrowseChip(strings.allProfiles, selected = mode == 1, onClick = { mode = 1 })
                    }
                    // Sort chip, then one removable chip per applied filter value, then Reset all (from two chips up).
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .semantics { contentDescription = strings.appliedFilters },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BrowseChip(
                            strings.sortChip(sort.label(strings)),
                            selected = false,
                            onClick = { sortSheetOpen = true },
                            leading = Icons.AutoMirrored.Filled.Sort,
                            trailing = Icons.Filled.ExpandMore,
                            isAction = true,
                        )
                        appliedChips.forEach { c ->
                            BrowseChip(
                                c.label, selected = true, onClick = c.onRemove, outlinedAccent = true, trailing = Icons.Filled.Close,
                                isAction = true, actionDescription = strings.removeFilter(c.label),
                            )
                        }
                        if (appliedChips.size >= 2) {
                            BrowseChip(strings.resetAll, selected = false, onClick = ::clearFacets, isAction = true)
                        }
                    }
                }
            }

            // ---- results ---------------------------------------------------
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp)) {
                when {
                    notClaimed -> D2MEmptyState(strings.waitingOnChildTitle, strings.waitingOnChildBody)

                    loading -> when (view) {
                        BrowseView.LIST -> BrowseRowSkeleton()
                        BrowseView.CARDS -> BrowseSkeleton()
                        BrowseView.COMPARE -> BrowseTableSkeleton()
                    }

                    error != null -> D2MErrorBanner(error!!, onRetry = { refreshTick++ }, modifier = Modifier.padding(horizontal = 16.dp))

                    sorted.isEmpty() && pool.isNotEmpty() -> D2MEmptyState(
                        title = strings.nothingMatchesFiltersTitle,
                        subtitle = strings.nothingMatchesFiltersBody,
                        action = {
                            D2MActionButton(strings.clearFilters, onClick = ::clearAllAndQuery, emphasis = D2MActionEmphasis.SECONDARY, height = 48.dp, modifier = Modifier.padding(horizontal = 24.dp))
                        },
                    )

                    sorted.isEmpty() -> D2MEmptyState(
                        title = strings.noProfilesYetTitle,
                        subtitle = if (mode == 0) strings.noProfilesMatchingBody else strings.noProfilesAllBody,
                    )

                    view == BrowseView.COMPARE -> BrowseTable(
                        rows = sorted,
                        onOpenProfile = onOpenProfile,
                        shortlistOverrides = shortlistOverrides,
                        onToggleShortlist = { s, now -> toggleShortlist(s, now) },
                        resolvePhotoUrl = apiClient::resolveMediaUrl,
                        refreshing = refreshing,
                        onRefresh = { refreshing = true; refreshTick++ },
                    )

                    view == BrowseView.LIST -> {
                        val listState = rememberLazyListState()
                        PrefetchPhotos(
                            count = sorted.size,
                            firstVisibleIndex = { listState.firstVisibleItemIndex },
                            urlAt = { i -> sorted.getOrNull(i)?.photoUrl?.let(apiClient::resolveMediaUrl) },
                            ahead = 8,
                            perRow = 1,
                        )
                        PullToRefreshBox(
                            isRefreshing = refreshing,
                            onRefresh = { refreshing = true; refreshTick++ },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().semantics { contentDescription = strings.profilesListLabel },
                                contentPadding = PaddingValues(bottom = 24.dp),
                            ) {
                                listItems(sorted, key = { it.candidateId }, contentType = { "row" }) { s ->
                                    // Read inside the item: a toggle recomposes this row only.
                                    val shortlisted = shortlistOverrides[s.candidateId] ?: s.isShortlisted
                                    Column {
                                        ProfileRow(
                                            suggestion = s,
                                            resolvePhotoUrl = apiClient::resolveMediaUrl,
                                            onOpen = { onOpenProfile(s.candidateId) },
                                            isShortlisted = shortlisted,
                                            onToggleShortlist = { toggleShortlist(s, !shortlisted) },
                                        )
                                        Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(d2m.borderSoft))
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        var shownCount by remember(sorted) { mutableStateOf(PAGE_SIZE) }
                        val shown = remember(sorted, shownCount) { sorted.take(shownCount) }
                        val gridState = rememberLazyGridState()

                        // Grow the window when the user is within ~3 rows of its end.
                        LaunchedEffect(sorted, shown.size) {
                            snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                                .distinctUntilChanged()
                                .collect { last ->
                                    if (last >= shown.size - 6 && shown.size < sorted.size) shownCount += PAGE_SIZE
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
                                if (shown.size < sorted.size) {
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
        if (sortSheetOpen) {
            BrowseSortSheet(value = sort, strings = strings, onChange = { sort = it }, onDismiss = { sortSheetOpen = false })
        }
        if (viewSheetOpen) {
            BrowseViewSheet(
                value = view, strings = strings,
                onChange = { view = it; BrowseStateHolder.saveView(it) },
                onDismiss = { viewSheetOpen = false },
            )
        }
    }
}

/** Round toolbar button: 44dp visual (inside a 48dp target), optional count badge for an active state. */
@Composable
private fun ToolbarIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    badge: Int = 0,
    bordered: Boolean = true,
) {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (badge > 0) d2m.accentSoft else if (bordered) MaterialTheme.colorScheme.surface else Color.Transparent)
                .then(if (bordered) Modifier.border(BorderStroke(1.dp, if (badge > 0) d2m.accent else d2m.border), CircleShape) else Modifier)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (badge > 0) d2m.accentStrong else d2m.textPrimary, modifier = Modifier.size(22.dp))
        }
        if (badge > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(d2m.accent),
                contentAlignment = Alignment.Center,
            ) { Text(badge.toString(), style = MaterialTheme.typography.labelSmall, color = d2m.accentOn, fontWeight = FontWeight.Bold) }
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
    /** A one-shot action ("Clear filters") rather than one of a set of options: announced as a button, not as selected/not selected. */
    isAction: Boolean = false,
    trailing: ImageVector? = null,
    /** Spoken label for an action chip whose visible text alone is ambiguous ("Remove filter City: Chennai"). */
    actionDescription: String? = null,
) {
    val shape = RoundedCornerShape(percent = 50)
    val bg = if (selected && !outlinedAccent) d2m.accent else if (selected) d2m.accentSoft else MaterialTheme.colorScheme.surface
    val fg = if (selected && !outlinedAccent) d2m.accentOn else if (selected) d2m.accentStrong else d2m.textPrimary
    val borderColor = if (selected) d2m.accent else d2m.border
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .then(
                if (isAction) Modifier
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                    .then(if (actionDescription != null) Modifier.semantics { contentDescription = actionDescription } else Modifier)
                else Modifier.selectable(selected = selected, interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton, onClick = onClick),
            ),
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
            if (trailing != null) Icon(trailing, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Card-view placeholder: the loaded grid's portrait tiles (same 3:4 ratio and two-across layout), each with a match-pill
 * bar at the top-left and name + place bars at the bottom -- so tiles don't jump when data lands. The table view has its
 * own, different placeholder (BrowseTableSkeleton).
 */
@Composable
private fun BrowseSkeleton() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(3) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    BoxWithConstraints(Modifier.weight(1f)) {
                        D2MSkeleton(height = maxWidth * 4f / 3f, radius = 14.dp)
                        D2MSkeleton(width = 48.dp, height = 22.dp, radius = 6.dp, modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
                        Column(Modifier.align(Alignment.BottomStart).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            D2MSkeleton(width = 96.dp, height = 16.dp)
                            D2MSkeleton(width = 64.dp, height = 12.dp)
                        }
                    }
                }
            }
        }
    }
}

/** List-view placeholder: the loaded rows' geometry, so nothing jumps when data lands. */
@Composable
private fun BrowseRowSkeleton() {
    Column(Modifier.fillMaxSize()) { repeat(7) { ProfileRowSkeleton() } }
}
