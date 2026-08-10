package com.d2m.app.messaging.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.d2mIdToMessagingUsername
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Real-time conversation pane -- mirrors the visual role of
 * screens/child/MatchesScreen.jsx's MessageList + MessageComposer. Wired
 * end-to-end against MessagingRepository (Phase 6), which today runs on the
 * explicitly non-production StubUnencryptedCryptoProvider -- see
 * messaging/crypto/CryptoProvider.kt. A warning banner surfaces that state
 * visibly rather than silently, since "looks like a real encrypted chat
 * screen" is exactly the failure mode that doc comment warns about.
 */
@Composable
fun ChatPane(peerId: String, peerName: String, modifier: Modifier = Modifier) {
    val identityStore: IdentityStore = koinInject()
    val messagingRepo: MessagingRepository = koinInject()
    val apiClient = koinInject<com.d2m.app.data.network.ApiClient>()
    val scope = rememberCoroutineScope()

    val peerUsername = remember(peerId) { d2mIdToMessagingUsername(peerId) }
    val messages by messagingRepo.messagesFor(peerUsername).collectAsState()
    val peerOnline by messagingRepo.isPeerOnline(peerUsername).collectAsState()
    var draft by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        scope.launch { messagingRepo.start(apiClient.client) }
        onDispose { /* MessagingRepository is a long-lived singleton -- intentionally not stopped per-pane, same "one connection survives navigation" design as d2m_web's shell-hoisted provider. */ }
    }

    Column(modifier = modifier) {
        if (!messagingRepo.isProductionGradeEncryption) {
            Text(
                "Development build: messages are not end-to-end encrypted yet (Phase 6 crypto is stubbed -- see CryptoProvider.kt).",
                style = MaterialTheme.typography.labelSmall,
                color = mutedText(0.55f),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(peerName, style = MaterialTheme.typography.titleMedium)
            if (peerOnline) Text("· online", style = MaterialTheme.typography.labelSmall, color = mutedText(0.55f))
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(messages) { m ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (m.isMine) Arrangement.End else Arrangement.Start) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (m.isMine) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else mutedText(0.08f),
                                RoundedCornerShape(D2MRadius.md),
                            )
                            .padding(10.dp),
                    ) {
                        Text(m.text)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message…") },
            )
            D2MButton(
                text = "Send",
                enabled = draft.isNotBlank(),
                onClick = {
                    val text = draft
                    draft = ""
                    scope.launch { messagingRepo.sendText(peerId, text) }
                },
            )
        }
    }
}
