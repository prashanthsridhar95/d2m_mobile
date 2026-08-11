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
 * app's sidenav, but the same set of destinations): parent gets Dashboard/
 * Search/Sharing/Settings. Child gets Home/Discover/Matches/Notifications/
 * Settings -- a direct 1:1 mirror of ChildShell.jsx's five Sidenav items in
 * the same order, same destinations (Home is the "check on things" landing
 * page ahead of Discover, same as web's docstring on why it's first).
 *
 * This used to be four child tabs (Discover/Matches/Chat/Profile) with no
 * Home tab at all and "Chat" duplicating Matches' own route -- reported
 * directly ("I want the Home in nav bar to be how it's in web. Also there
 * should be 5 tabs as web"). CHILD_HOME and NOTIFICATIONS were already
 * real, wired-up routes/screens (D2MNavGraph.kt) -- they just weren't in
 * this tab list, so the bottom bar never showed on Home at all (App.kt's
 * showBottomBar is derived from tabs.any { it.route == currentRoute }) and
 * Notifications had no tab entry point.
 *
 * "Sharing" (parent) points at the same messaging surface as parent-to-
 * parent contact (ParentMessagesScreen) since neither the guest/shared-link
 * feature nor a separate parent "Sharing" concept exists on the backend yet
 * (see plan §5's scope boundary) -- unrelated to the child-tab fix above.
 */
enum class D2MTab(val label: String, val icon: ImageVector, val route: String) {
    // Parent tabs
    ParentDashboard("Dashboard", Icons.Filled.Home, Routes.PARENT_HOME),
    ParentSearch("Search", Icons.Filled.Search, Routes.PARENT_BROWSE),
    ParentSharing("Sharing", Icons.Filled.Share, Routes.PARENT_MESSAGES),
    ParentSettings("Settings", Icons.Filled.Settings, Routes.SETTINGS),

    // Child tabs -- order matches ChildShell.jsx's Sidenav exactly.
    ChildHome("Home", Icons.Filled.Home, Routes.CHILD_HOME),
    ChildDiscover("Discover", Icons.Filled.Explore, Routes.DISCOVERY),
    ChildMatches("Matches", Icons.Filled.Favorite, Routes.MATCHES),
    ChildNotifications("Notifications", Icons.Filled.Notifications, Routes.NOTIFICATIONS),
    ChildSettings("Settings", Icons.Filled.Settings, Routes.SETTINGS),
}

val ParentTabs = listOf(D2MTab.ParentDashboard, D2MTab.ParentSearch, D2MTab.ParentSharing, D2MTab.ParentSettings)
val ChildTabs = listOf(D2MTab.ChildHome, D2MTab.ChildDiscover, D2MTab.ChildMatches, D2MTab.ChildNotifications, D2MTab.ChildSettings)

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
