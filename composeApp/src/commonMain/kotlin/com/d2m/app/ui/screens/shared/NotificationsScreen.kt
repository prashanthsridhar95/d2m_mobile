package com.d2m.app.ui.screens.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.NotificationOut
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.NotificationsRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Mirrors screens/shared/NotificationsScreen.jsx -- one component mounted
 * for both roles (GET /accounts/{id}/notifications is generic over
 * sponsor/primary), typed notification list with tap-through. Deep-linking
 * a tapped notification into the relevant screen/thread is left as a
 * follow-up (needs a NavController reference threaded in, omitted here to
 * keep this screen's signature simple for the first cut).
 */
@Composable
fun NotificationsScreen() {
    val identityStore: IdentityStore = koinInject()
    val notificationsRepo: NotificationsRepository = koinInject()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()

    var notifications by remember { mutableStateOf<List<NotificationOut>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var markingAll by remember { mutableStateOf(false) }

    val accountId = identity.sponsorId ?: identity.primaryId
    val unreadCount = notifications.count { it.readAt == null }

    LaunchedEffect(accountId) {
        if (accountId == null) return@LaunchedEffect
        loading = true
        error = null
        try {
            notifications = notificationsRepo.getNotifications(accountId)
        } catch (e: Exception) {
            error = friendlyError(e, "Couldn't load notifications.")
        } finally {
            loading = false
        }
    }

    val flow = if (identity.role == D2MRole.CHILD) D2MFlow.CHILD else D2MFlow.PARENT
    D2MTheme(flow = flow) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PageTitle("Notifications")
                // "Notification panel needs a Mark all read button",
                // reported directly. Only shown once there's something to
                // clear -- an always-visible disabled button just adds
                // noise to a page that's usually already read.
                if (unreadCount > 0 && accountId != null) {
                    D2MButton(
                        text = if (markingAll) "Marking…" else "Mark all read",
                        enabled = !markingAll,
                        onClick = {
                            markingAll = true
                            scope.launch {
                                runCatching { notificationsRepo.markAllRead(accountId) }
                                    .onSuccess { notifications = it }
                                markingAll = false
                            }
                        },
                    )
                }
            }
            when {
                loading -> Text("Loading…", color = mutedText(0.55f), modifier = Modifier.padding(top = 12.dp))
                error != null -> D2MErrorBanner(error!!, modifier = Modifier.padding(top = 12.dp))
                notifications.isEmpty() -> D2MEmptyState("Nothing yet", "New suggestions, requests, and match updates will show up here.")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    items(notifications) { n ->
                        D2MCard(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (n.readAt == null && accountId != null) {
                                    scope.launch { runCatching { notificationsRepo.markRead(accountId, n.notificationId) } }
                                }
                            },
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(notificationTitle(n.type), fontWeight = if (n.readAt == null) FontWeight.Bold else FontWeight.Normal)
                                Text(n.createdAt, style = MaterialTheme.typography.labelSmall, color = mutedText(0.45f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun notificationTitle(type: String): String = when (type) {
    "new_suggestion" -> "New suggestion in your feed"
    "mutual_match" -> "You have a new match"
    "serious_mode_request" -> "Someone wants to go Serious"
    "serious_mode_accepted" -> "Serious Mode confirmed"
    "serious_mode_revoked" -> "Serious Mode was revoked"
    "consent_request" -> "A parent requested access to your details"
    "consent_granted" -> "Access request granted"
    "request_received" -> "You received a new request"
    "share_link_viewed" -> "Your shared profile was viewed"
    else -> type
}
