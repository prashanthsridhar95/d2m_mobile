package com.d2m.app.ui.screens.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import coil3.compose.AsyncImage
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.SuggestionsRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MProfileTabsPanel
import com.d2m.app.ui.components.ScoreBadge
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Mirrors screens/parent/ProfileDetailScreen.jsx -- shared by both parent
 * and child shells despite living in the "parent" screens package (matches
 * the web app's own file layout, kept for the same reason: the component
 * really is shared, moving it wouldn't change what it does). Role-aware
 * action row: parent gets Suggest to child, child gets Accept/Snooze/Pass.
 */
@Composable
fun ProfileDetailScreen(candidateId: String, onBack: () -> Unit) {
    val identityStore: IdentityStore = koinInject()
    val suggestionsRepo: SuggestionsRepository = koinInject()
    val apiClient: ApiClient = koinInject()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()

    var candidate by remember { mutableStateOf<SuggestionOut?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionInFlight by remember { mutableStateOf(false) }

    val viewerPrimaryId = identity.primaryId ?: identity.childPrimaryId

    LaunchedEffect(candidateId, viewerPrimaryId) {
        if (viewerPrimaryId == null) return@LaunchedEffect
        loading = true
        error = null
        try {
            candidate = suggestionsRepo.getCandidate(viewerPrimaryId, candidateId)
        } catch (e: Exception) {
            error = friendlyError(e, "Couldn't load this profile.")
        } finally {
            loading = false
        }
    }

    D2MTheme(flow = if (identity.role == D2MRole.CHILD) D2MFlow.CHILD else D2MFlow.PARENT) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            D2MButton("Back", variant = D2MButtonVariant.GHOST, onClick = onBack)

            when {
                loading -> Text("Loading…", color = mutedText(0.55f))
                error != null -> D2MErrorBanner(error!!)
                candidate != null -> {
                    val c = candidate!!
                    AsyncImage(
                        model = c.photoUrl?.let(apiClient::resolveMediaUrl),
                        contentDescription = c.candidateName,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1.2f),
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${c.candidateName}${c.age?.let { ", $it" } ?: ""}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        ScoreBadge(c.compositeScore)
                    }
                    Text(listOfNotNull(c.city, c.occupationTitle).joinToString(" · "), color = mutedText(0.55f))

                    D2MProfileTabsPanel(candidateId = candidateId, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (identity.role == D2MRole.PARENT) {
                            D2MButton(
                                text = if (actionInFlight) "Suggesting…" else "Suggest to child",
                                enabled = !actionInFlight,
                                onClick = {
                                    val sponsorId = identity.sponsorId ?: return@D2MButton
                                    scope.launch {
                                        actionInFlight = true
                                        runCatching { suggestionsRepo.suggestToChild(sponsorId, candidateId) }
                                        actionInFlight = false
                                    }
                                },
                            )
                        } else {
                            listOf("accept" to "Send request", "snooze" to "Snooze", "reject" to "Pass").forEach { (action, label) ->
                                D2MButton(
                                    text = label,
                                    variant = if (action == "accept") D2MButtonVariant.SOLID else D2MButtonVariant.OUTLINE,
                                    enabled = !actionInFlight,
                                    onClick = {
                                        val pid = viewerPrimaryId ?: return@D2MButton
                                        scope.launch {
                                            actionInFlight = true
                                            runCatching { suggestionsRepo.act(pid, candidateId, action) }
                                            actionInFlight = false
                                            onBack()
                                        }
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
