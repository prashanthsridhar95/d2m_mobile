package com.d2m.app.ui.screens.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.NotificationOut
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.NotificationsRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MEmptyState
import com.d2m.app.ui.components.D2MErrorState
import com.d2m.app.ui.components.D2MSkeleton
import com.d2m.app.ui.components.LocalD2MSnackbar
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.components.SkeletonList
import com.d2m.app.ui.components.bounceClickable
import com.d2m.app.ui.navigation.UnreadBadges
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.strings.NotificationsStrings
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.d2m
import com.d2m.app.ui.theme.mutedText
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

/** A notification plus its parsed time, computed once per list change (never during scroll). */
private data class NotifUi(val n: NotificationOut, val epochMs: Long, val date: LocalDate, val time: String)

private sealed interface NotifRow {
    val key: String
    data class Header(val date: LocalDate) : NotifRow { override val key get() = "h:$date" }
    data class Item(val ui: NotifUi) : NotifRow { override val key get() = "n:${ui.n.notificationId}" }
}

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun parseNotif(n: NotificationOut): NotifUi {
    val tz = TimeZone.currentSystemDefault()
    val ldt: LocalDateTime = runCatching { Instant.parse(n.createdAt).toLocalDateTime(tz) }
        .recoverCatching { LocalDateTime.parse(n.createdAt.take(19)) }
        .getOrElse { LocalDateTime(1970, 1, 1, 0, 0) }
    val epoch = runCatching { ldt.toInstantSafe(tz) }.getOrDefault(0L)
    val h12 = when (val h = ldt.hour % 12) { 0 -> 12; else -> h }
    val time = "$h12:${ldt.minute.toString().padStart(2, '0')} ${if (ldt.hour < 12) "AM" else "PM"}"
    return NotifUi(n, epoch, ldt.date, time)
}

private fun LocalDateTime.toInstantSafe(tz: TimeZone): Long =
    date.atStartOfDayIn(tz).toEpochMilliseconds() + (hour * 3600L + minute * 60L + second) * 1000L

private fun buildRows(list: List<NotificationOut>): List<NotifRow> {
    val parsed = list.map(::parseNotif).sortedByDescending { it.epochMs }
    val rows = ArrayList<NotifRow>(parsed.size + 6)
    var last: LocalDate? = null
    for (p in parsed) {
        if (p.date != last) { rows += NotifRow.Header(p.date); last = p.date }
        rows += NotifRow.Item(p)
    }
    return rows
}

private fun dayLabel(date: LocalDate, today: LocalDate, strings: NotificationsStrings): String {
    val tz = TimeZone.currentSystemDefault()
    val diff = (today.atStartOfDayIn(tz) - date.atStartOfDayIn(tz)).inWholeDays
    return when {
        diff == 0L -> strings.groupToday
        diff == 1L -> strings.groupYesterday
        else -> "${date.dayOfMonth} ${MONTHS[date.monthNumber - 1]}" + if (date.year != today.year) " ${date.year}" else ""
    }
}

/**
 * Mirrors screens/shared/NotificationsScreen.jsx -- one component mounted
 * for both roles (GET /accounts/{id}/notifications is generic over
 * sponsor/primary). Grouped by day, unread dot, pull-to-refresh, shimmer
 * skeleton on first load, and an error state with retry. Reading a
 * notification updates the list and the tab badge immediately (optimistic),
 * then confirms with the server. Deep-linking a tapped notification into
 * the relevant screen is still a follow-up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen() {
    val identityStore: IdentityStore = koinInject()
    val notificationsRepo: NotificationsRepository = koinInject()
    val identity by identityStore.identity.collectAsState()
    val strings = LocalStrings.current.notifications
    val snackbar = LocalD2MSnackbar.current
    val scope = rememberCoroutineScope()

    var notifications by remember { mutableStateOf<List<NotificationOut>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var markingAll by remember { mutableStateOf(false) }

    val accountId = identity.sponsorId ?: identity.primaryId
    val unreadCount = remember(notifications) { notifications.count { it.readAt == null } }
    val rows = remember(notifications) { buildRows(notifications) }
    val today = remember(notifications) {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    suspend fun load(force: Boolean) {
        val id = accountId ?: return
        error = null
        try {
            notifications = notificationsRepo.getNotifications(id, forceRefresh = force)
            UnreadBadges.notifications.value = notifications.count { it.readAt == null }
        } catch (e: Exception) {
            if (notifications.isEmpty()) error = friendlyError(e, strings.loadError)
            else snackbar.showError(friendlyError(e, strings.loadError))
        }
    }

    LaunchedEffect(accountId) {
        if (accountId == null) return@LaunchedEffect
        loading = notifications.isEmpty()
        load(force = false)
        loading = false
    }

    val flow = if (identity.role == D2MRole.CHILD) D2MFlow.CHILD else D2MFlow.PARENT
    D2MTheme(flow = flow) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) { PageTitle(strings.pageTitle) }
                if (unreadCount > 0 && accountId != null) {
                    D2MButton(
                        text = if (markingAll) strings.marking else strings.markAllRead,
                        enabled = !markingAll,
                        onClick = {
                            markingAll = true
                            scope.launch {
                                runCatching { notificationsRepo.markAllRead(accountId) }
                                    .onSuccess {
                                        notifications = it
                                        UnreadBadges.notifications.value = it.count { n -> n.readAt == null }
                                    }
                                    .onFailure { snackbar.showError(strings.markAllError) }
                                markingAll = false
                            }
                        },
                    )
                }
            }
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    scope.launch {
                        refreshing = true
                        load(force = true)
                        refreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                ) {
                    when {
                        loading -> item(key = "skeleton", contentType = "skeleton") { SkeletonList(count = 7, avatarSize = 14.dp) }
                        error != null -> item(key = "error", contentType = "state") {
                            D2MErrorState(error!!, onRetry = { scope.launch { loading = true; load(force = true); loading = false } })
                        }
                        rows.isEmpty() -> item(key = "empty", contentType = "state") {
                            D2MEmptyState(strings.emptyTitle, strings.emptyDescription)
                        }
                        else -> items(rows, key = { it.key }, contentType = { if (it is NotifRow.Header) "header" else "item" }) { row ->
                            when (row) {
                                is NotifRow.Header -> Text(
                                    dayLabel(row.date, today, strings),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = d2m.label,
                                    modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
                                )
                                is NotifRow.Item -> NotificationRow(
                                    ui = row.ui,
                                    strings = strings,
                                    onClick = {
                                        val id = accountId ?: return@NotificationRow
                                        if (row.ui.n.readAt == null) {
                                            val nid = row.ui.n.notificationId
                                            // Optimistic: flip locally + badge first, then tell the server.
                                            notifications = notifications.map { if (it.notificationId == nid) it.copy(readAt = "now") else it }
                                            UnreadBadges.notifications.value = (UnreadBadges.notifications.value - 1).coerceAtLeast(0)
                                            scope.launch { runCatching { notificationsRepo.markRead(id, nid) } }
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

@Composable
private fun NotificationRow(ui: NotifUi, strings: NotificationsStrings, onClick: () -> Unit) {
    val unread = ui.n.readAt == null
    val title = notificationTitle(ui.n.type, strings)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bounceClickable(onClick = onClick, pressedScale = 0.985f)
            .padding(vertical = 10.dp),
    ) {
        Box(
            Modifier
                .size(10.dp)
                .background(if (unread) d2m.accent else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                .semantics { if (unread) contentDescription = strings.unreadLabel },
        )
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
            color = if (unread) MaterialTheme.colorScheme.onSurface else mutedText(0.7f),
            modifier = Modifier.weight(1f),
        )
        Text(ui.time, style = MaterialTheme.typography.labelSmall, color = mutedText(0.45f))
    }
}

private fun notificationTitle(type: String, strings: NotificationsStrings): String = when (type) {
    "new_suggestion" -> strings.typeNewSuggestion
    "mutual_match" -> strings.typeMutualMatch
    "serious_mode_request" -> strings.typeSeriousModeRequest
    "serious_mode_accepted" -> strings.typeSeriousModeAccepted
    "serious_mode_revoked" -> strings.typeSeriousModeRevoked
    "consent_request" -> strings.typeConsentRequest
    "consent_granted" -> strings.typeConsentGranted
    "request_received" -> strings.typeRequestReceived
    "share_link_viewed" -> strings.typeShareLinkViewed
    "mutual_sponsor_interest" -> strings.typeMutualSponsorInterest
    "shortlisted_profile_updated" -> strings.typeShortlistedProfileUpdated
    else -> type
}
