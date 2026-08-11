package com.d2m.app.ui.screens.child

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.SuggestionsRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.ScoreBadge
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch

/**
 * Mirrors screens/child/DiscoveryScreen.jsx -- one-candidate-at-a-time
 * browser. The web version is explicitly "dating-site style" in framing but
 * button-driven (click Accept/Snooze/Pass); this is the one screen the plan
 * flagged as worth genuinely upgrading rather than 1:1 porting (§4/§8) --
 * real drag-to-decide gestures here (drag right = accept, left = pass, tap
 * buttons still work too), a natural fit for Compose that the web DOM
 * version never had. Auto-advances on action, no going back (Pass keeps
 * the same "no undo, no backing up" behavior as web -- the delayed-commit
 * Undo window is a smaller follow-up, not included in this first cut).
 */
@Composable
fun DiscoveryScreen(onOpenProfile: (String) -> Unit) {
    val identityStore = org.koin.compose.koinInject<IdentityStore>()
    val suggestionsRepo = org.koin.compose.koinInject<SuggestionsRepository>()
    val apiClient = org.koin.compose.koinInject<ApiClient>()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()

    var queue by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionInFlight by remember { mutableStateOf(false) }

    val primaryId = identity.primaryId

    LaunchedEffect(primaryId) {
        if (primaryId == null) return@LaunchedEffect
        loading = true
        error = null
        try {
            queue = suggestionsRepo.getSuggestions(primaryId)
        } catch (e: Exception) {
            error = friendlyError(e, "Couldn't load candidates.")
        } finally {
            loading = false
        }
    }

    fun act(action: String) {
        val pid = primaryId ?: return
        val current = queue.firstOrNull() ?: return
        if (actionInFlight) return
        scope.launch {
            actionInFlight = true
            runCatching { suggestionsRepo.act(pid, current.candidateId, action) }
            queue = queue.drop(1)
            actionInFlight = false
        }
    }

    D2MTheme(flow = D2MFlow.CHILD) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when {
                loading -> Text("Loading…", color = mutedText(0.55f))
                error != null -> D2MErrorBanner(error!!)
                queue.isEmpty() -> D2MEmptyState("No more candidates right now", "Check back later for new suggestions.")
                else -> {
                    val current = queue.first()
                    val offsetX = remember(current.candidateId) { Animatable(0f) }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .graphicsLayer { translationX = offsetX.value; rotationZ = offsetX.value / 40f }
                                .pointerInput(current.candidateId) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            scope.launch {
                                                when {
                                                    offsetX.value > 220f -> { act("accept") }
                                                    offsetX.value < -220f -> { act("reject") }
                                                    else -> offsetX.animateTo(0f)
                                                }
                                            }
                                        },
                                    ) { change, dragAmount ->
                                        change.consume()
                                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                                    }
                                },
                        ) {
                            Column {
                                AsyncImage(
                                    model = current.photoUrl?.let(apiClient::resolveMediaUrl),
                                    contentDescription = current.name,
                                    modifier = Modifier.fillMaxWidth().weight(1f).aspectRatio(0.8f),
                                )
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${current.candidateName}, ${current.age ?: "--"}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    ScoreBadge(current.compositeScore)
                                }
                                Text(current.city ?: "", color = mutedText(0.55f), modifier = Modifier.padding(bottom = 4.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            D2MButton("Pass", variant = D2MButtonVariant.OUTLINE, enabled = !actionInFlight, onClick = { act("reject") }, modifier = Modifier.weight(1f))
                            D2MButton("Snooze", variant = D2MButtonVariant.OUTLINE, enabled = !actionInFlight, onClick = { act("snooze") }, modifier = Modifier.weight(1f))
                            D2MButton("Accept", enabled = !actionInFlight, onClick = { act("accept") }, modifier = Modifier.weight(1f))
                        }
                        D2MButton("View full profile", variant = D2MButtonVariant.GHOST, onClick = { onOpenProfile(current.candidateId) })
                    }
                }
            }
        }
    }
}
