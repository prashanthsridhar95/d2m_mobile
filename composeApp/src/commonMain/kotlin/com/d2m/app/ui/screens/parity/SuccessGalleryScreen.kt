package com.d2m.app.ui.screens.parity

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.SuccessGalleryEntryOut
import com.d2m.app.data.network.friendlyError
import com.d2m.app.domain.repository.OffboardingRepository
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import org.koin.compose.koinInject

/** Mirrors screens/public/SuccessGalleryScreen.jsx -- public, unauthenticated (GET /success-gallery, no pagination). Phase 4 parity item, per plan §4/§8. */
@Composable
fun SuccessGalleryScreen() {
    val repo: OffboardingRepository = koinInject()
    var entries by remember { mutableStateOf<List<SuccessGalleryEntryOut>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            entries = repo.getSuccessGallery()
        } catch (e: Exception) {
            error = friendlyError(e, "Couldn't load the gallery.")
        } finally {
            loading = false
        }
    }

    D2MTheme(flow = D2MFlow.GUEST_SYSTEM) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Success stories", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            when {
                loading -> Text("Loading…", color = mutedText(0.55f))
                error != null -> D2MErrorBanner(error!!)
                entries.isEmpty() -> D2MEmptyState("Nothing published yet")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                    items(entries) { e ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(e.story)
                            }
                        }
                    }
                }
            }
        }
    }
}
