package com.d2m.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom-tab chrome, per app-ui.prashanthsridhar.com's navigation pattern
 * (adopted per plan §5/§8 -- a cleaner mobile-native shape than the web
 * app's sidenav): parent gets Dashboard/Search/Sharing/Settings. Child gets
 * Home/Discover/Matches/Notifications -- four tabs, not web's five.
 *
 * Settings was deliberately pulled out of the child tab bar on request
 * ("Remove settings from bottom nav & move it to home - show an icon in
 * top right") -- a mobile-native pattern (gear icon in the corner of the
 * landing screen) rather than a literal mirror of ChildShell.jsx's five
 * Sidenav items, which does still list Settings as a nav row. See
 * ChildHomeScreen.kt's top-right IconButton for where it moved to.
 *
 * "Sharing" (parent) points at the same messaging surface as parent-to-
 * parent contact (ParentMessagesScreen) since neither the guest/shared-link
 * feature nor a separate parent "Sharing" concept exists on the backend yet
 * (see plan §5's scope boundary) -- unrelated to the child-tab change above.
 */
enum class D2MTab(val label: String, val icon: ImageVector, val route: String) {
    // Parent tabs
    ParentDashboard("Dashboard", Icons.Filled.Home, Routes.PARENT_HOME),
    ParentSearch("Search", Icons.Filled.Search, Routes.PARENT_BROWSE),
    ParentSharing("Sharing", Icons.Filled.Share, Routes.PARENT_MESSAGES),
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
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}
