package com.d2m.app.ui.screens.child

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.SuggestionsRepository
import com.d2m.app.ui.components.D2MBadge
import com.d2m.app.ui.components.D2MBadgeTone
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import org.koin.compose.koinInject

/**
 * Mirrors screens/child/HomeScreen.jsx -- one shared compact list in
 * priority order (received match requests, sent requests waiting on a
 * response, today's picks), rather than a stat-tile dashboard like parent
 * Home (see that screen's own doc comment on why: "shouldn't remind of
 * work"). Absorbed the old separate Requests tab (accept/reject happens
 * inline here on web) -- this cut surfaces the lists; wiring the inline
 * accept/reject actions directly on each row is the natural next pass once
 * this screen is confirmed useful, same incremental approach the plan takes
 * throughout.
 */
@Composable
fun ChildHomeScreen(
    onOpenProfile: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenMatches: () -> Unit,
    onOpenChildProfileDialog: () -> Unit,
) {
    val identityStore: IdentityStore = koinInject()
    val suggestionsRepo: SuggestionsRepository = koinInject()
    val identity by identityStore.identity.collectAsState()

    var received by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var sent by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var todaysPicks by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val primaryId = identity.primaryId

    LaunchedEffect(primaryId) {
        if (primaryId == null) return@LaunchedEffect
        loading = true
        error = null
        try {
            received = suggestionsRepo.getReceivedRequests(primaryId)
            sent = suggestionsRepo.getSentRequests(primaryId)
            todaysPicks = suggestionsRepo.getSuggestions(primaryId).take(5)
        } catch (e: Exception) {
            error = friendlyError(e, "Couldn't load your home feed.")
        } finally {
            loading = false
        }
    }

    D2MTheme(flow = D2MFlow.CHILD) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Hey there 👋", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            when {
                loading -> Text("Loading…", color = mutedText(0.55f), modifier = Modifier.padding(top = 12.dp))
                error != null -> D2MErrorBanner(error!!, modifier = Modifier.padding(top = 12.dp))
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                    if (received.isNotEmpty()) {
                        item { SectionLabel("Waiting on you") }
                        items(received) { s -> ListRow(s, "Wants to connect", onClick = { onOpenProfile(s.candidateId) }) }
                    }
                    if (sent.isNotEmpty()) {
                        item { SectionLabel("Sent, awaiting response") }
                        items(sent) { s -> ListRow(s, "Request sent", onClick = { onOpenProfile(s.candidateId) }) }
                    }
                    item { SectionLabel("Today's picks") }
                    items(todaysPicks) { s -> ListRow(s, "New suggestion", onClick = { onOpenProfile(s.candidateId) }) }

                    item {
                        Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            D2MButton("Keep exploring", onClick = onOpenDiscover)
                            D2MButton("View matches", onClick = onOpenMatches)
                            D2MButton("My profile", onClick = onOpenChildProfileDialog)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = mutedText(0.55f), modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun ListRow(s: SuggestionOut, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${s.name}${s.age?.let { ", $it" } ?: ""}", fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = mutedText(0.55f))
            s.compositeScore?.let { D2MBadge("$it/10", D2MBadgeTone.INFO) }
        }
    }
}
