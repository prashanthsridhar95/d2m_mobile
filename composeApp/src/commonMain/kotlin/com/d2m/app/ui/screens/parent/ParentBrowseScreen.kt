package com.d2m.app.ui.screens.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.BrowseCandidateOut
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.SuggestionsRepository
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.CardSkeletonRow
import com.d2m.app.ui.components.MatchCard
import com.d2m.app.ui.screens.parity.BrowseTable
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import org.koin.compose.koinInject

/**
 * Mirrors screens/parent/BrowseScreen.jsx: "Matching profiles" (ranked
 * feed) / "All profiles" (unranked pool) toggle, card grid (table view is
 * Phase 4, see parity/BrowseTableScreen.kt). The web version's slide-out
 * filter panel becomes a bottom sheet here in a follow-up pass -- omitted
 * from this first cut to keep the list itself real and working first (a
 * sort/filter-free browse is still a fully functional screen, unlike a
 * filter panel with nothing to filter).
 */
@Composable
fun ParentBrowseScreen(onOpenProfile: (String) -> Unit) {
    val identityStore: IdentityStore = koinInject()
    val suggestionsRepo: SuggestionsRepository = koinInject()
    val apiClient: ApiClient = koinInject()
    val identity by identityStore.identity.collectAsState()

    var mode by remember { mutableStateOf(0) } // 0 = matching, 1 = all
    var tableView by remember { mutableStateOf(false) } // Phase 4 parity: grid/table toggle, see parity/BrowseTable.kt
    var matching by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var all by remember { mutableStateOf<List<BrowseCandidateOut>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

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

    D2MTheme(flow = D2MFlow.PARENT) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Browse profiles", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                    SegmentedButton(selected = mode == 0, onClick = { mode = 0 }, shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(0, 2)) { Text("Matching") }
                    SegmentedButton(selected = mode == 1, onClick = { mode = 1 }, shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(1, 2)) { Text("All profiles") }
                }
                if (mode == 1) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(selected = !tableView, onClick = { tableView = false }, shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(0, 2)) { Text("Grid") }
                        SegmentedButton(selected = tableView, onClick = { tableView = true }, shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(1, 2)) { Text("Table") }
                    }
                }
            }

            when {
                loading -> CardSkeletonRow()
                error != null -> D2MErrorBanner(error!!)
                mode == 0 -> LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(matching) { s ->
                        MatchCard(suggestion = s, resolvePhotoUrl = apiClient::resolveMediaUrl, onClick = { onOpenProfile(s.candidateId) })
                    }
                }
                mode == 1 && tableView -> BrowseTable(rows = all, onOpenProfile = onOpenProfile)
                else -> LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(all) { c ->
                        MatchCard(
                            suggestion = SuggestionOut(
                                candidateId = c.candidateId, name = c.name, age = c.age, city = c.city,
                                occupationTitle = c.occupationTitle, gothram = c.gothram, sect = c.sect,
                                photoUrl = c.photoUrl, compositeScore = c.scores?.compositeScore,
                            ),
                            resolvePhotoUrl = apiClient::resolveMediaUrl,
                            onClick = { onOpenProfile(c.candidateId) },
                        )
                    }
                }
            }
        }
    }
}
