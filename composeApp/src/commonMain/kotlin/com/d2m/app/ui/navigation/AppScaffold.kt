package com.d2m.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.components.D2MDivider
import com.d2m.app.ui.components.PageTitle
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
    ParentSearch("Search", Icons.Filled.Search, Routes.PARENT_BROWSE),
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

@Composable
fun D2MBottomBar(tabs: List<D2MTab>, currentRoute: String?, onTabSelected: (D2MTab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        // A hairline instead of Material's tonal elevation -- nothing in
        // this design floats, and an elevated bar over a cream page reads
        // as a different material from everything above it.
        D2MDivider(soft = false)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = d2m.meta,
            tonalElevation = 0.dp,
        ) {
            tabs.forEach { tab ->
                NavigationBarItem(
                    selected = currentRoute == tab.route,
                    onClick = { onTabSelected(tab) },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = d2m.accent,
                        selectedTextColor = d2m.accent,
                        unselectedIconColor = d2m.label,
                        unselectedTextColor = d2m.label,
                        // The indicator is the warm sunken tone, not a
                        // maroon-tinted pill: at 4 tabs a saturated pill
                        // under the active icon competes with whatever
                        // primary action the screen itself is showing.
                        indicatorColor = d2m.surfaceSunken,
                    ),
                )
            }
        }
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
