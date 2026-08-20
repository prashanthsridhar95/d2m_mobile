package com.d2m.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.messaging.ChatUiState
import com.d2m.app.messaging.call.ui.CallLayer
import com.d2m.app.messaging.ui.InAppNotificationLayer
import com.d2m.app.push.PlatformPushInitializer
import com.d2m.app.push.ui.rememberNotificationPermissionLauncher
import com.d2m.app.ui.navigation.ChildTabs
import com.d2m.app.ui.navigation.D2MBottomBar
import com.d2m.app.ui.navigation.D2MNavGraph
import com.d2m.app.ui.navigation.ParentTabs
import com.d2m.app.ui.navigation.Routes
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import org.koin.compose.koinInject

/**
 * Root composable -- shared by both androidMain's MainActivity (via
 * setContent { App() }) and iosMain's MainViewController (via
 * ComposeUIViewController { App() }). This is the one place that:
 *  - resolves the cold-start destination from IdentityStore's persisted
 *    identity (mirrors DevIdentity.jsx's "already logged in? skip Login" on
 *    web's App.jsx root)
 *  - hosts the bottom-tab chrome (D2MBottomBar) around the parent/child tab
 *    routes only -- entry/onboarding/detail/dialog routes render full-bleed,
 *    same split as AppScaffold.kt's doc comment describes
 *  - fires the one-time push registration side effect (PlatformPushInitializer)
 *
 * Per-screen flow theming (D2MTheme(flow = ...)) still lives inside each
 * screen composable, same as before -- this outer D2MTheme(ENTRY) only
 * themes the Scaffold/bottom-bar chrome itself, which sits outside any one
 * screen's own flow.
 */
@Composable
fun App() {
    val identityStore: IdentityStore = koinInject()
    val pushInitializer: PlatformPushInitializer = koinInject()
    val chatUiState: ChatUiState = koinInject()
    val identity by identityStore.identity.collectAsState()
    val conversationOpen by chatUiState.conversationOpen.collectAsState()

    // Fire-once: push permission/token registration shouldn't block first
    // paint and isn't tied to any particular screen's lifecycle. Request the
    // OS notification permission FIRST -- reported directly: "Notification
    // allow is not asked on launch" (Android 13+'s POST_NOTIFICATIONS is a
    // runtime permission; declaring it in the manifest alone never prompts
    // anyone -- see push/ui/NotificationPermission.kt). Token fetch/
    // registration doesn't depend on this permission and proceeds either way.
    val requestNotificationPermission = rememberNotificationPermissionLauncher {}
    LaunchedEffect(Unit) {
        requestNotificationPermission()
        runCatching { pushInitializer.initialize() }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Read once at composition start -- NavHost's startDestination is fixed
    // at graph-build time, matching how DevIdentity's persisted role is only
    // consulted once per cold start on web too (subsequent role changes go
    // through explicit navigate() calls in the screens themselves, e.g.
    // HandoffScreen -> PARENT_HOME, ClaimFlowScreen -> CHILD_HOME).
    val startDestination = remember {
        when (identity.role) {
            D2MRole.PARENT -> Routes.PARENT_HOME
            D2MRole.CHILD -> Routes.CHILD_HOME
            null -> Routes.LOGIN
        }
    }

    val tabs = when (identity.role) {
        D2MRole.PARENT -> ParentTabs
        D2MRole.CHILD -> ChildTabs
        null -> null
    }
    // "Botton nav bar is not required inside a person's chat" -- currentRoute
    // alone can't tell us that (the thread list and an open conversation are
    // both the MATCHES route, see MatchesScreen.kt/ChatUiState.kt), so this
    // also factors in the shared conversationOpen flag.
    val showBottomBar = tabs != null && tabs.any { it.route == currentRoute } && !conversationOpen

    D2MTheme(flow = D2MFlow.ENTRY) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    if (showBottomBar && tabs != null) {
                        D2MBottomBar(
                            tabs = tabs,
                            currentRoute = currentRoute,
                            onTabSelected = { tab ->
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                },
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    D2MNavGraph(navController = navController, startDestination = startDestination)
                }
            }

            // Mounted once above the whole nav tree (not per-screen) so an
            // active call survives navigating between screens, same as
            // d2m_web's CallLayer.jsx sitting above its router. Renders
            // nothing while idle -- see CallLayer.kt.
            CallLayer()

            // Instagram-style in-app message banner (point 4 of the
            // notification request) -- mounted the same way as CallLayer so
            // it can show up regardless of which screen is on top. Tapping
            // it records the peer via ChatUiState and navigates to Matches;
            // MatchesScreen.kt picks the pending peer up once its thread
            // list is loaded and opens that conversation directly.
            if (identity.role == D2MRole.CHILD) {
                InAppNotificationLayer(
                    onOpenPeer = { peerUsername ->
                        chatUiState.requestOpenPeer(peerUsername)
                        navController.navigate(Routes.MATCHES) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }
}
