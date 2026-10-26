package com.d2m.app.ui.screens.child

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.SuggestionOut
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.SuggestionsRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.ProfileCardContent
import com.d2m.app.ui.components.SomethingWentWrong
import com.d2m.app.ui.components.SwipeDeckSkeleton
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme

/**
 * The profile a child opens from a conversation in Matches (web: screens/child/MatchedProfileScreen.jsx). Same layout
 * as Discover's card, read-only -- no hearts, no pass, no swipe -- with a single "Back to messages" action. The
 * candidate is fetched with [SuggestionsRepository.getCandidate]; a failure shows the full-screen error state with retry.
 */
@Composable
fun MatchedProfileScreen(candidateId: String, onBack: () -> Unit) {
    val identityStore = org.koin.compose.koinInject<IdentityStore>()
    val suggestionsRepo = org.koin.compose.koinInject<SuggestionsRepository>()
    val identity by identityStore.identity.collectAsState()
    val strings = LocalStrings.current.discovery
    val primaryId = identity.primaryId

    var candidate by remember(candidateId) { mutableStateOf<SuggestionOut?>(null) }
    var failed by remember(candidateId) { mutableStateOf(false) }
    var reloadTick by remember { mutableStateOf(0) }

    LaunchedEffect(primaryId, candidateId, reloadTick) {
        if (primaryId == null || candidate != null) return@LaunchedEffect
        failed = false
        try {
            candidate = suggestionsRepo.getCandidate(primaryId, candidateId)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = true
        }
    }

    D2MTheme(flow = D2MFlow.CHILD) {
        Column(Modifier.fillMaxSize()) {
            val c = candidate
            Column(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    c != null -> ProfileCardContent(
                        card = c,
                        isTop = true,
                        viewerPrimaryId = primaryId,
                        topInset = 0.dp,
                        bottomClearance = 16.dp,
                        onLike = {},
                        readOnly = true,
                    )
                    failed -> {
                        Text(
                            strings.errOpenProfile,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                .semantics { liveRegion = LiveRegionMode.Assertive },
                        )
                        SomethingWentWrong(onRetry = { reloadTick++ })
                    }
                    else -> SwipeDeckSkeleton(0.dp, 16.dp)
                }
            }
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
                D2MButton(
                    strings.backToMessages,
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}
