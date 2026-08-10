package com.d2m.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
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
 * app's sidenav): parent gets Dashboard/Search/Sharing/Settings, child gets
 * Discover/Matches/Chat/Profile. "Sharing" (parent) and "Chat" (child) both
 * currently point at the same messaging surface -- parent-to-parent contact
 * (ParentMessagesScreen) and the child's Matches thread list respectively --
 * since neither the guest/shared-link feature nor a separate parent
 * "Sharing" concept exists on the backend yet (see plan §5's scope
 * boundary); this tab is a placeholder pointed at the closest real screen
 * rather than a dead end.
 */
enum class D2MTab(val label: String, val icon: ImageVector, val route: String) {
    // Parent tabs
    ParentDashboard("Dashboard", Icons.Filled.Home, Routes.PARENT_HOME),
    ParentSearch("Search", Icons.Filled.Search, Routes.PARENT_BROWSE),
    ParentSharing("Sharing", Icons.Filled.Share, Routes.PARENT_MESSAGES),
    ParentSettings("Settings", Icons.Filled.Settings, Routes.SETTINGS),

    // Child tabs
    ChildDiscover("Discover", Icons.Filled.Home, Routes.DISCOVERY),
    ChildMatches("Matches", Icons.Filled.Notifications, Routes.MATCHES),
    ChildChat("Chat", Icons.Filled.ChatBubble, Routes.MATCHES),
    ChildProfile("Profile", Icons.Filled.Person, Routes.SETTINGS),
}

val ParentTabs = listOf(D2MTab.ParentDashboard, D2MTab.ParentSearch, D2MTab.ParentSharing, D2MTab.ParentSettings)
val ChildTabs = listOf(D2MTab.ChildDiscover, D2MTab.ChildMatches, D2MTab.ChildChat, D2MTab.ChildProfile)

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
