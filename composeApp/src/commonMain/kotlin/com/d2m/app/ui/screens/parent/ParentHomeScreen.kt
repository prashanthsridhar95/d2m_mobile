package com.d2m.app.ui.screens.parent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.SponsorDashboardOut
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.DashboardRepository
import com.d2m.app.domain.repository.SuggestionsRepository
import com.d2m.app.ui.components.D2MBadge
import com.d2m.app.ui.components.D2MBadgeTone
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.FieldSkeleton
import com.d2m.app.ui.components.MatchCard
import com.d2m.app.ui.components.MatchCardVariant
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.SubHeading
import com.d2m.app.ui.components.MetaText
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.theme.D2MFlow
import kotlinx.coroutines.launch
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import org.koin.compose.koinInject

/**
 * Mirrors screens/parent/DiscoverScreen.jsx (exported as parent Home) --
 * the heaviest screen on web (~1160 lines: child profile summary, Serious
 * Mode consent card, Panchangam summary, tracked-matches board, activity
 * stats). Ported here as a single scrollable column (the web version's
 * responsive collapse below 860px is a direct 1:1 with "just always render
 * one column" on a phone -- see plan §4), not new design work. The
 * dedup/derivation logic across suggestions/shortlist/prospective-contacts
 * that made this screen worth planning carefully (per the research) lives
 * in DashboardRepository/SuggestionsRepository, not duplicated here.
 */
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
    // (see Dashboard.kt's doc comment -- it's sponsor-safe by design, only
    // ever exposing a count). "Suggested for you to review" sources real
    // SuggestionOut cards from SuggestionsRepository directly instead, same
    // call ParentBrowseScreen.kt's "matching" tab already uses.
    var topSuggestions by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val sponsorId = identity.sponsorId
    val childPrimaryId = identity.childPrimaryId
    val scope = rememberCoroutineScope()

    // Neither list rendered a shortlist star at all before this port (see
    // MatchCard.kt's redesign note) -- now that COMPACT actually shows one,
    // it needs a real handler, not a no-op. Optimistic local update (both
    // lists are updated in place, no round trip needed to reflect the
    // toggle) plus the actual API call; a failed call is rare enough here
    // (add/remove-from-shortlist has no meaningful failure mode beyond
    // "offline") that this doesn't roll back on error, same posture as the
    // rest of this screen's fire-and-forget actions.
    //
    // Takes the whole candidate, not just its id -- adding to `shortlist`
    // needs a full SuggestionOut to append (there's nowhere else to get
    // one from once this function only has an id), and re-deriving it from
    // topSuggestions inside here would silently do nothing for a candidate
    // shortlisted from a context where it isn't already in that list.
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
                if (nowShortlisted) {
                    suggestionsRepo.addToShortlist(primaryId, candidate.candidateId)
                } else {
                    suggestionsRepo.removeFromShortlist(primaryId, candidate.candidateId)
                }
            } catch (_: Exception) {
                // Re-sync from the server rather than leaving the optimistic
                // update wrong if the call actually failed.
                shortlist = suggestionsRepo.getShortlist(primaryId)
                topSuggestions = suggestionsRepo.getSuggestions(primaryId)
            }
        }
    }

    LaunchedEffect(sponsorId, childPrimaryId) {
        if (sponsorId == null) return@LaunchedEffect
        loading = true
        error = null
        try {
            dashboard = dashboardRepo.getSponsorDashboard(sponsorId)
            if (childPrimaryId != null) {
                shortlist = suggestionsRepo.getShortlist(childPrimaryId)
                topSuggestions = suggestionsRepo.getSuggestions(childPrimaryId)
            }
        } catch (e: Exception) {
            error = friendlyError(e, "Couldn't load your dashboard.")
        } finally {
            loading = false
        }
    }

    D2MTheme(flow = D2MFlow.PARENT) {
        // verticalScroll -- this file's own doc comment above already
        // claimed "a single scrollable column", but the modifier was never
        // actually there. Harmless while this Column's content was short
        // (the old horizontal LazyRow sections), which is almost certainly
        // how it went unnoticed; it stopped being harmless the moment the
        // Suggested/Shortlisted sections became vertical stacks of many
        // rows (see MatchCard.kt's COMPACT redesign) -- a bounded, non-
        // scrolling Column measures sequential children against whatever
        // height budget remains, so once that budget ran low, the row
        // straddling the boundary got clamped into a squashed oval photo
        // (Modifier.size() respects incoming constraints, it doesn't
        // override them) and everything after it was simply invisible,
        // with no way to scroll to it. Confirmed live: a "16 suggestions"
        // list only ever showed 5 rows, the 5th visibly distorted.
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            PageTitle("Your child's matches")

            when {
                loading -> Column { repeat(3) { FieldSkeleton(modifier = Modifier.padding(bottom = 12.dp)) } }
                error != null -> D2MErrorBanner(error!!)
                else -> {
                    ProfileSummaryCard(
                        name = "Your child",
                        subtitle = if (dashboard?.childProfileCompleted == true) "Profile complete" else "Profile incomplete",
                        onClick = onOpenChildProfileDialog,
                    )

                    dashboard?.childThreadStatus?.let { statusLabel ->
                        ConsentStatusCard(
                            statusLabel = statusLabel,
                            pendingCount = dashboard?.unreadNotificationCount ?: 0,
                            onOpenMessages = onOpenMessages,
                        )
                    }

                    // Vertical lists of the compact row card, not a
                    // horizontal LazyRow of full photo-band cards -- mirrors
                    // web's actual "Profiles you're tracking" layout
                    // (DiscoverScreen.jsx), reported directly: "whatever
                    // design change we have done here in web, I want the
                    // same implemented for mobile... how it is in web,
                    // exactly." MatchCardVariant.COMPACT had no call sites
                    // anywhere in the app before this -- see its own
                    // redesign note in MatchCard.kt.
                    Column {
                        Text("Suggested for you to review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${topSuggestions.size} suggestions", color = mutedText(0.55f), style = MaterialTheme.typography.labelMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                            topSuggestions.forEach { s ->
                                MatchCard(
                                    suggestion = s,
                                    resolvePhotoUrl = apiClient::resolveMediaUrl,
                                    onClick = { onOpenProfile(s.candidateId) },
                                    variant = MatchCardVariant.COMPACT,
                                    isShortlisted = s.isShortlisted,
                                    onToggleShortlist = { toggleShortlist(s, !s.isShortlisted) },
                                )
                            }
                        }
                    }

                    Column {
                        Text("Shortlisted", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                            shortlist.forEach { s ->
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
    }
}

@Composable
private fun ProfileSummaryCard(name: String, subtitle: String, onClick: () -> Unit) {
    D2MCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            SubHeading(name)
            MetaText(subtitle, Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ConsentStatusCard(statusLabel: String, pendingCount: Int, onOpenMessages: () -> Unit) {
    D2MCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenMessages)) {
        Column(modifier = Modifier.padding(16.dp)) {
            SubHeading("Status: $statusLabel")
            if (pendingCount > 0) {
                D2MBadge("$pendingCount unread", D2MBadgeTone.INFO)
            }
        }
    }
}
