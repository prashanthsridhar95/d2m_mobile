package com.d2m.app.ui.screens.child

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.SuggestionsRepository
import com.d2m.app.ui.components.D2MBadge
import com.d2m.app.ui.components.D2MBadgeTone
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MLevelPill
import com.d2m.app.ui.components.astrologicalCompatibility
import com.d2m.app.ui.components.overallRating
import com.d2m.app.ui.components.preferenceCompatibility
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MRadius
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
 *
 * Visual pass (UX audit fix -- "web to mobile translation is very poor...
 * compare it to the UI & UX in web, it's just day & night", with screenshots
 * of this exact screen showing a flat placeholder photo and sparse layout):
 * this used to be a single flat AsyncImage on a solid mutedText background
 * with no rating/compatibility signal anywhere on the card -- everything
 * web shows (LevelPill overall rating, astro/preference compatibility
 * pills, dosha flag chips, the "X of Y" progress line) was silently
 * dropped even though SuggestionOut already carries the scores needed to
 * compute all of it (see Compatibility.kt, a direct port of lib/
 * compatibility.js). Restoring those closes the actual "day and night"
 * gap -- it isn't a skin/polish issue, it's that most of the card's real
 * content was never wired up on this platform.
 */
@Composable
fun DiscoveryScreen(onOpenProfile: (String) -> Unit) {
    val identityStore = org.koin.compose.koinInject<IdentityStore>()
    val suggestionsRepo = org.koin.compose.koinInject<SuggestionsRepository>()
    val apiClient = org.koin.compose.koinInject<ApiClient>()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()

    var queue by remember { mutableStateOf<List<SuggestionOut>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
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
            // Seeded once per load, same as web's totalSeededForRef -- "X of
            // Y" should count down within a stable batch, not re-inflate
            // toward the current queue size on every optimistic mutation.
            total = queue.size
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
                    val seen = total - queue.size
                    val offsetX = remember(current.candidateId) { Animatable(0f) }
                    val rating = overallRating(current.scores.compositeScore)
                    val astroLevel = astrologicalCompatibility(current.scores)
                    val prefLevel = preferenceCompatibility(current.scores)

                    Column(modifier = Modifier.fillMaxSize()) {
                        if (total > 0) {
                            Text(
                                "${seen + 1} of $total",
                                style = MaterialTheme.typography.labelMedium,
                                color = mutedText(0.45f),
                                modifier = Modifier.padding(bottom = 10.dp),
                            )
                        }

                        Card(
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
                            shape = RoundedCornerShape(D2MRadius.lg),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(bottom = 4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(topStart = D2MRadius.lg, topEnd = D2MRadius.lg))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(mutedText(0.12f), mutedText(0.22f)),
                                            ),
                                        ),
                                ) {
                                    AsyncImage(
                                        model = current.photoUrl?.let(apiClient::resolveMediaUrl),
                                        contentDescription = current.candidateName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }

                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Column {
                                            Text(
                                                "${current.candidateName}${current.age?.let { ", $it" } ?: ""}",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            val subline = listOfNotNull(current.city, current.occupationTitle).joinToString(" · ")
                                            if (subline.isNotEmpty()) {
                                                Text(subline, style = MaterialTheme.typography.bodyMedium, color = mutedText(0.55f))
                                            }
                                        }
                                        D2MLevelPill(rating)
                                    }

                                    if (current.doshaFlags.isNotEmpty()) {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(top = 12.dp),
                                        ) {
                                            items(current.doshaFlags) { flag ->
                                                D2MBadge(flag, D2MBadgeTone.WARNING)
                                            }
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 12.dp),
                                    ) {
                                        D2MBadge("Astrology: $astroLevel", D2MBadgeTone.NEUTRAL)
                                        D2MBadge("Preferences: $prefLevel", D2MBadgeTone.NEUTRAL)
                                    }
                                }
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
