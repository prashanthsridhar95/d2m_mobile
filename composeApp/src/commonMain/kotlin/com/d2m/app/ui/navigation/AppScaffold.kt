package com.d2m.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.NotificationsRepository
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.ui.components.pressScale
import com.d2m.app.ui.components.rememberHaptics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.components.D2MDivider
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.strings.CommonStrings
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.d2m

/**
 * Bottom-tab chrome: parent gets Dashboard/Search/Sharing/Settings, child
 * gets Home/Discover/Matches/Notifications -- four tabs each.
 *
 * Settings was deliberately pulled out of the child tab bar on request
 * ("Remove settings from bottom nav & move it to home - show an icon in
 * top right") -- a mobile-native pattern rather than a literal mirror of
 * the web app's nav. See ChildHomeScreen.kt's top-right IconButton.
 *
 * "Sharing" (parent) now opens ShareLinksScreen -- the profile share-link
 * feature this tab was originally reserved for (see the previous version
 * of this comment, which pointed it at parent-to-parent messaging as a
 * stand-in "since neither... exists on the backend yet"). Parent-to-
 * parent messaging loses its only top-level tab-bar entry as a result --
 * it's still reachable from a candidate's profile ("message their
 * parent") and from a tapped chat notification (App.kt) -- a real gap
 * worth its own top-level nav slot as a fast-follow, not solved here.
 *

 * Retheme note: the bar itself is restyled below rather than left to
 * Material's defaults. On web this chrome became a two-tier top header;
 * on a phone a bottom tab bar is the right shape and stays, but it now
 * sits on the cream page ground with a hairline above it and a maroon
 * active tab, instead of Material's elevated tonal container with a
 * pill-shaped indicator.
 */
enum class D2MTab(val label: String, val icon: ImageVector, val route: String) {
    // Parent tabs
    ParentDashboard("Dashboard", Icons.Filled.Home, Routes.PARENT_HOME),
    ParentSearch("Browse", Icons.Filled.GridView, Routes.PARENT_BROWSE),
    ParentSharing("Sharing", Icons.Filled.Share, Routes.SHARE_LINKS),
    ParentSettings("Settings", Icons.Filled.Settings, Routes.SETTINGS),

    // Child tabs -- Settings intentionally omitted, see doc comment above.
    ChildHome("Home", Icons.Filled.Home, Routes.CHILD_HOME),
    ChildDiscover("Discover", Icons.Filled.Explore, Routes.DISCOVERY),
    ChildMatches("Matches", Icons.Filled.Favorite, Routes.MATCHES),
    ChildNotifications("Notifications", Icons.Filled.Notifications, Routes.NOTIFICATIONS),
}

val ParentTabs = listOf(D2MTab.ParentDashboard, D2MTab.ParentSearch, D2MTab.ParentSharing, D2MTab.ParentSettings)
val ChildTabs = listOf(D2MTab.ChildHome, D2MTab.ChildDiscover, D2MTab.ChildMatches, D2MTab.ChildNotifications)

/** D2MTab.label is a fixed English key; this resolves the localized copy to actually show. */
fun D2MTab.localizedLabel(common: CommonStrings): String = when (this) {
    D2MTab.ParentDashboard -> common.tabDashboard
    D2MTab.ParentSearch -> common.tabSearch
    D2MTab.ParentSharing -> common.tabSharing
    D2MTab.ParentSettings -> common.tabSettings
    D2MTab.ChildHome -> common.tabHome
    D2MTab.ChildDiscover -> common.tabDiscover
    D2MTab.ChildMatches -> common.tabMatches
    D2MTab.ChildNotifications -> common.tabNotifications
}

/**
 * Cross-screen unread counters for the tab badges. A tiny process-wide
 * holder (not DI) because NotificationsScreen writes it when the user reads
 * things and the shell bar reads it from outside the nav graph.
 */
object UnreadBadges {
    val notifications = MutableStateFlow(0)
}

/**
 * Tab navigation that preserves each tab's back stack, scroll position and
 * state. Reselecting the current tab is a no-op (launchSingleTop), so there
 * is no rebuild or flicker.
 */
fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * The bar App.kt mounts. Owns its own badge sources so unread-count churn
 * recomposes only this bar, never the whole app shell.
 */
@Composable
fun D2MShellBottomBar(tabs: List<D2MTab>, currentRoute: String?, onTabSelected: (D2MTab) -> Unit) {
    val messagingRepo: MessagingRepository = koinInject()
    val identityStore: IdentityStore = koinInject()
    val notificationsRepo: NotificationsRepository = koinInject()
    val identity by identityStore.identity.collectAsState()
    val isChild = identity.role == D2MRole.CHILD

    val unreadMessages by remember(messagingRepo) {
        messagingRepo.unreadByPeer.map { m -> m.values.sum() }.distinctUntilChanged()
    }.collectAsState(0)
    val unreadNotifications by UnreadBadges.notifications.collectAsState()

    val accountId = identity.primaryId
    LaunchedEffect(accountId, currentRoute, isChild) {
        if (!isChild || accountId == null) return@LaunchedEffect
        // Cached for 20s in the repository, so tab hops cost nothing.
        runCatching { notificationsRepo.getNotifications(accountId) }
            .onSuccess { list -> UnreadBadges.notifications.value = list.count { it.readAt == null } }
    }

    val badges = remember(unreadMessages, unreadNotifications, isChild) {
        if (!isChild) emptyMap() else buildMap {
            if (unreadMessages > 0) put(D2MTab.ChildMatches, unreadMessages)
            if (unreadNotifications > 0) put(D2MTab.ChildNotifications, unreadNotifications)
        }
    }
    D2MBottomBar(tabs = tabs, currentRoute = currentRoute, onTabSelected = onTabSelected, badges = badges)
}

@Composable
fun D2MBottomBar(
    tabs: List<D2MTab>,
    currentRoute: String?,
    onTabSelected: (D2MTab) -> Unit,
    badges: Map<D2MTab, Int> = emptyMap(),
) {
    val common = LocalStrings.current.common
    val haptics = rememberHaptics()
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            // Home indicator / gesture bar: iOS safe-area and Android
            // edge-to-edge both surface as navigationBars insets.
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        // A hairline instead of Material's tonal elevation -- nothing in
        // this design floats.
        D2MDivider(soft = false)
        Row(Modifier.fillMaxWidth().height(64.dp).selectableGroup()) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                BottomBarItem(
                    tab = tab,
                    label = tab.localizedLabel(common),
                    selected = selected,
                    badge = badges[tab] ?: 0,
                    onClick = {
                        if (!selected) haptics.light()
                        onTabSelected(tab)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    tab: D2MTab,
    label: String,
    selected: Boolean,
    badge: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val tint by animateColorAsState(if (selected) d2m.accent else d2m.label, tween(180), label = "tabTint")
    val pill = animateFloatAsState(if (selected) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "tabPill")
    val lift = animateFloatAsState(if (selected) 1.12f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tabLift")
    val pillColor = d2m.surfaceSunken

    Column(
        modifier = modifier
            .fillMaxHeight()
            .pressScale(source, 0.92f)
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .height(30.dp)
                .width(58.dp)
                // Selected pill grows from the centre; read in draw phase.
                .drawBehind {
                    val p = pill.value
                    if (p > 0.01f) {
                        val w = size.width * p
                        drawRoundRect(
                            color = pillColor,
                            topLeft = Offset((size.width - w) / 2f, 0f),
                            size = Size(w, size.height),
                            cornerRadius = CornerRadius(size.height / 2f),
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.graphicsLayer {
                    val l = lift.value
                    scaleX = l
                    scaleY = l
                },
            ) {
                Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                        .background(d2m.accent, CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (badge > 99) "99+" else badge.toString(),
                        color = d2m.accentOn,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * A screen header: the display-serif page title, an optional provenance
 * line under it, and an optional trailing action.
 *
 * New in the retheme pass, and deliberately NOT a Material TopAppBar.
 * Every comp opens the same way -- a large serif title sitting directly
 * on the page ground, with one muted line of provenance under it
 * ("1,284 live profiles · last validated 28 August 2026") and controls on
 * the same baseline. A TopAppBar gives a fixed-height bar with a
 * centre-ish 22sp title and its own container colour, which is the
 * opposite shape. Screens compose this at the top of their scroll content
 * instead, so the title scrolls away like it does in the comps.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            PageTitle(title)
            if (meta != null) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = d2m.meta,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
        if (trailing != null) Box(Modifier.padding(start = 12.dp)) { trailing() }
    }
}


/** Pushed screens that have no header of their own: the shell gives them one slim bar with a back arrow. */
val BackBarRoutes: Set<String> = setOf(
    Routes.SETTINGS, Routes.TRUST, Routes.IDENTITY_VERIFICATION, Routes.CHILD_PROFILE_DIALOG,
    Routes.SUCCESS_GALLERY, Routes.ADMIN_GALLERY_MODERATION, Routes.PANCHANGAM_CALENDAR,
    Routes.SHARE_LINKS, Routes.PARENT_MESSAGES, Routes.NOTIFICATIONS,
)

/** A 56dp bar holding one 48dp back button (arrow mirrors for RTL). Sits on the page ground, no divider, so the screen's own title follows naturally. */
@Composable
fun D2MBackBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val label = LocalStrings.current.common.back
    Row(modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = label, tint = d2m.textPrimary)
        }
    }
}
