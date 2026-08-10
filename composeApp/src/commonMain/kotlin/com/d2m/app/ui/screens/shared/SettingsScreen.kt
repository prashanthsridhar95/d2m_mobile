package com.d2m.app.ui.screens.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.d2m.app.data.model.NotificationPreferenceIn
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.NotificationsRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Mirrors screens/shared/SettingsScreen.jsx: role-branching -- child gets a
 * tabbed Preferences/Others layout (Preferences moved into
 * ChildProfileDialogScreen here, same "Preferences tab reuses the same
 * fields" split web already made), parent gets a single notifications+
 * logout page (no equivalent basic-data/filters concept for sponsors).
 */
@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    val identityStore: IdentityStore = koinInject()
    val notificationsRepo: NotificationsRepository = koinInject()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()

    var muted by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf("immediate") }

    val accountId = identity.sponsorId ?: identity.primaryId
    LaunchedEffect(accountId) {
        if (accountId == null) return@LaunchedEffect
        runCatching { notificationsRepo.getPreferences(accountId) }.onSuccess {
            muted = it.muted
            frequency = it.frequency
        }
    }

    val flow = if (identity.role == D2MRole.CHILD) D2MFlow.CHILD else D2MFlow.PARENT
    D2MTheme(flow = flow) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Mute notifications", fontWeight = FontWeight.Bold)
                    Text("Turn off all push and in-app alerts", color = mutedText(0.55f), style = MaterialTheme.typography.labelMedium)
                }
                Switch(
                    checked = muted,
                    onCheckedChange = { checked ->
                        muted = checked
                        if (accountId != null) {
                            scope.launch { runCatching { notificationsRepo.setPreferences(accountId, NotificationPreferenceIn(muted = checked)) } }
                        }
                    },
                )
            }

            D2MButton(text = "Log out", variant = D2MButtonVariant.OUTLINE, onClick = {
                identityStore.clear()
                onLogout()
            })
        }
    }
}
