package com.d2m.app.ui.screens.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.BrowseCandidateOut
import com.d2m.app.data.model.SubScoreBreakdown
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.SuggestionsRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonSize
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MFilterSheet
import com.d2m.app.ui.components.D2MSegmented
import com.d2m.app.ui.components.D2MSkeleton
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.components.FacetGroup
import com.d2m.app.ui.components.FacetState
import com.d2m.app.ui.components.MatchCard
import com.d2m.app.ui.navigation.ScreenHeader
import com.d2m.app.ui.screens.parity.BrowseTable
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.d2m
import com.d2m.app.domain.repository.IdentityRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Browse: "Matching profiles" (the ranked feed) / "All profiles" (the
 * unranked pool), with the comps' filter surface as a bottom sheet and a
 * single-column card list.
 *
 * Two retheme-pass changes worth naming.
 *
 * 1. One column, not two. The card is now a photograph band over a text
 *    block over an action (see MatchCard.kt) -- at two-across on a 375pt
 *    phone each card is ~165pt wide, which ellipsises every value in the
 *    fact rows and shrinks the photograph to a thumbnail, i.e. undoes the
 *    entire point of the new card. The comps run three wide columns on a
 *    desktop; the honest phone translation of "wide column" is one.
 *
 * 2. Filters exist now. This screen shipped without any ("omitted from
 *    this first cut to keep the list itself real and working first"), and
 *    the web app has since settled on the comps' persistent rail. The rail
 *    becomes a modal bottom sheet here -- see FilterSheet.kt -- with the
 *    same facets, the same per-option match counts, the same
 *    include/exclude switch and the same live "N of M match" readout.
 *    Everything is client-side over the batch already fetched, exactly as
 *    on web: no new endpoint, and every option list is derived from
 *    whichever candidates are actually in the pool right now rather than
 *    from a fixed taxonomy, so there is never a choice that matches
 *    nothing.
 */
@Composable
fun ParentBrowseScreen(onOpenProfile: (String) -> Unit) {
    val identityStore: IdentityStore = koinInject()
    val suggestionsRepo: SuggestionsRepository = koinInject()
    val identityRepo: IdentityRepository = koinInject()
    val apiClient: ApiClient = koinInject()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()

    // "Provide an ID for each profile for easy search & finding" (reported
    // directly) -- a direct server lookup by short_id, distinct from the
    // client-side facet filters below: this can land on any profile at
    // all, not just one already in `pool` (which only ever holds this
    // sponsor's own matching/browse-all candidates).
    var shortIdQuery by remember { mutableStateOf("") }
    var shortIdSearching by remember { mutableStateOf(false) }
    var shortIdError by remember { mutableStateOf<String?>(null) }

    var mode by remember { mutableStateOf(0) } // 0 = matching, 1 = all
    var tableView by remember { mutableStateOf(false) }
    var matching by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var all by remember { mutableStateOf<List<BrowseCandidateOut>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var sheetOpen by remember { mutableStateOf(false) }
    var cityFacet by remember { mutableStateOf(FacetState()) }
    var gothramFacet by remember { mutableStateOf(FacetState()) }
    var sectFacet by remember { mutableStateOf(FacetState()) }
    var starFacet by remember { mutableStateOf(FacetState()) }
    var ageRange by remember { mutableStateOf<ClosedFloatingPointRange<Float>?>(null) }

    val childPrimaryId = identity.childPrimaryId ?: identity.primaryId

    LaunchedEffect(childPrimaryId, mode) {
        if (childPrimaryId == null) return@LaunchedEffect
        loading = true
        error = null
        try {
            if (mode == 0) matching = suggestionsRepo.getSuggestions(childPrimaryId)
            else all = suggestionsRepo.getBrowseAll(childPrimaryId)
        } catch (e: Exception) {
            error = friendlyError(e, "Couldn't load profiles.")
        } finally {
            loading = false
        }
    }

    // Both pools are normalised to SuggestionOut so everything below --
    // facets, counts, filtering, the cards -- is written once against one
    // shape and doesn't care which endpoint it came from. (BrowseCandidateOut
    // is the same record minus the ranked-feed fields.)
    val pool: List<SuggestionOut> = remember(mode, matching, all) {
        if (mode == 0) matching else all.map { c ->
            SuggestionOut(
                candidateId = c.candidateId, candidateName = c.candidateName, age = c.age, city = c.city,
                occupationTitle = c.occupationTitle, gothram = c.gothram, sect = c.sect,
                photoUrl = c.photoUrl, scores = c.scores ?: SubScoreBreakdown(),
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
        FacetGroup("City", pool.mapNotNull { it.city ?: it.nativity }.distinct().sorted(),
            countsFor("city") { it.city ?: it.nativity }, cityFacet) { cityFacet = it },
        FacetGroup("Gothram", pool.mapNotNull { it.gothram }.distinct().sorted(),
            countsFor("gothram") { it.gothram }, gothramFacet) { gothramFacet = it },
        FacetGroup("Sect", pool.mapNotNull { it.sect }.distinct().sorted(),
            countsFor("sect") { it.sect }, sectFacet) { sectFacet = it },
        FacetGroup("Nakshatra", pool.mapNotNull { it.moonNakshatra }.distinct().sorted(),
            countsFor("star") { it.moonNakshatra }, starFacet) { starFacet = it },
    )
    val activeFacetCount = groups.count { it.state.isActive } + (if (ageRange != null) 1 else 0)

    D2MTheme(flow = D2MFlow.PARENT) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            ScreenHeader(
                title = "Browse",
                meta = if (!loading && error == null) {
                    "${filtered.size} of ${pool.size} profile${if (pool.size == 1) "" else "s"}"
                } else null,
                modifier = Modifier.padding(top = 12.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                D2MTextField(
                    label = "",
                    value = shortIdQuery,
                    onValueChange = { shortIdQuery = it; shortIdError = null },
                    placeholder = "Find profile by ID…",
                    modifier = Modifier.weight(1f),
                )
                D2MButton(
                    text = if (shortIdSearching) "Finding…" else "Find",
                    enabled = shortIdQuery.isNotBlank() && !shortIdSearching,
                    size = D2MButtonSize.SM,
                    onClick = {
                        val code = shortIdQuery.trim()
                        scope.launch {
                            shortIdSearching = true
                            shortIdError = null
                            try {
                                val result = identityRepo.searchByShortId(code)
                                shortIdQuery = ""
                                onOpenProfile(result.primaryId)
                            } catch (e: Exception) {
                                shortIdError = friendlyError(e, "Couldn't search for that ID.")
                            } finally {
                                shortIdSearching = false
                            }
                        }
                    },
                )
            }
            shortIdError?.let { D2MErrorBanner(it) }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            ) {
                D2MSegmented(
                    options = listOf("Matching", "All profiles"),
                    selectedIndex = mode,
                    onSelect = { mode = it },
                )
                if (mode == 1) {
                    D2MSegmented(
                        options = listOf("Grid", "Table"),
                        selectedIndex = if (tableView) 1 else 0,
                        onSelect = { tableView = it == 1 },
                    )
                }
                D2MButton(
                    text = if (activeFacetCount > 0) "Filters ($activeFacetCount)" else "Filters",
                    onClick = { sheetOpen = true },
                    variant = if (activeFacetCount > 0) D2MButtonVariant.ACCENT_OUTLINE else D2MButtonVariant.OUTLINE,
                    size = D2MButtonSize.SM,
                )
            }

            when {
                loading -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Previews the real card's shape (band, then text, then
                    // action) so the list doesn't visibly reflow the moment
                    // data lands.
                    repeat(2) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            D2MSkeleton(height = 180.dp)
                            D2MSkeleton(width = 120.dp, height = 12.dp)
                            D2MSkeleton(width = 200.dp, height = 18.dp)
                        }
                    }
                }

                error != null -> D2MErrorBanner(error!!)

                mode == 1 && tableView -> BrowseTable(rows = all, onOpenProfile = onOpenProfile)

                filtered.isEmpty() && pool.isNotEmpty() -> D2MEmptyState(
                    title = "Nothing matches those filters",
                    subtitle = "Clear a filter or two and the list will fill back in.",
                    action = {
                        D2MButton("Clear filters", variant = D2MButtonVariant.ACCENT_OUTLINE, size = D2MButtonSize.SM, onClick = {
                            cityFacet = FacetState(); gothramFacet = FacetState()
                            sectFacet = FacetState(); starFacet = FacetState(); ageRange = null
                        })
                    },
                )

                filtered.isEmpty() -> D2MEmptyState(
                    title = "No profiles yet",
                    subtitle = if (mode == 0) {
                        "Once the matching engine has scored some candidates for your child, they show up here."
                    } else {
                        "There are no active profiles in the pool right now."
                    },
                )

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                ) {
                    items(filtered, key = { it.candidateId }) { s ->
                        MatchCard(
                            suggestion = s,
                            resolvePhotoUrl = apiClient::resolveMediaUrl,
                            onClick = { onOpenProfile(s.candidateId) },
                        )
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
                onClearAll = {
                    cityFacet = FacetState(); gothramFacet = FacetState()
                    sectFacet = FacetState(); starFacet = FacetState(); ageRange = null
                },
                ageBounds = ageBounds,
                ageRange = ageRange,
                onAgeChange = { ageRange = it },
            )
        }
    }
}
