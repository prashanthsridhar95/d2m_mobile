package com.d2m.app.ui.screens.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import com.d2m.app.ui.theme.D2MFlow
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
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Your child's matches", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

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

                    Column {
                        Text("Suggested for you to review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${topSuggestions.size} suggestions", color = mutedText(0.55f), style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                            items(topSuggestions) { s ->
                                MatchCard(
                                    suggestion = s,
                                    resolvePhotoUrl = apiClient::resolveMediaUrl,
                                    onClick = { onOpenProfile(s.candidateId) },
                                    modifier = Modifier.width(160.dp),
                                )
                            }
                        }
                    }

                    Column {
                        Text("Shortlisted", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                            items(shortlist) { s ->
                                MatchCard(
                                    suggestion = s,
                                    resolvePhotoUrl = apiClient::resolveMediaUrl,
                                    onClick = { onOpenProfile(s.candidateId) },
                                    isShortlisted = true,
                                    modifier = Modifier.width(160.dp),
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
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = mutedText(0.55f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ConsentStatusCard(statusLabel: String, pendingCount: Int, onOpenMessages: () -> Unit) {
    Card(onClick = onOpenMessages, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Status: $statusLabel", fontWeight = FontWeight.Bold)
            if (pendingCount > 0) {
                D2MBadge("$pendingCount unread", D2MBadgeTone.INFO)
            }
        }
    }
}
