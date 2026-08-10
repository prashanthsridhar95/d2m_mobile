package com.d2m.app.ui.screens.parity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.data.network.friendlyError
import com.d2m.app.domain.repository.AdminRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Mirrors screens/admin/GalleryModerationScreen.jsx -- internal, ungated,
 * single-action "publish by entry id" tool (no real moderation queue exists
 * on the backend either, and this route is only registered when
 * D2M_ENVIRONMENT != production -- see AdminApi.kt's doc comment). Phase 4
 * parity item; not linked from any bottom-tab nav, matching how it's
 * unreachable from normal navigation on web too.
 */
@Composable
fun AdminGalleryModerationScreen() {
    val repo: AdminRepository = koinInject()
    val scope = rememberCoroutineScope()

    var entryId by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    D2MTheme(flow = D2MFlow.GUEST_SYSTEM) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Gallery moderation (internal)", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Not a real queue -- publish a specific entry id, same as the web tool.", color = mutedText(0.55f))
            D2MTextField("Entry id", entryId, { entryId = it })
            status?.let { Text(it) }
            error?.let { D2MErrorBanner(it) }
            D2MButton(
                text = "Publish",
                enabled = entryId.isNotBlank(),
                onClick = {
                    scope.launch {
                        error = null
                        status = null
                        try {
                            repo.publishGalleryEntry(entryId)
                            status = "Published."
                        } catch (e: Exception) {
                            error = friendlyError(e, "Couldn't publish that entry.")
                        }
                    }
                },
            )
        }
    }
}
