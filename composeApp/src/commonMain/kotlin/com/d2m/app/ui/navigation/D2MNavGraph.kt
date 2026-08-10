package com.d2m.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.d2m.app.ui.screens.child.ChildHomeScreen
import com.d2m.app.ui.screens.child.ChildProfileDialogScreen
import com.d2m.app.ui.screens.child.ClaimFlowScreen
import com.d2m.app.ui.screens.child.DiscoveryScreen
import com.d2m.app.ui.screens.child.MatchesScreen
import com.d2m.app.ui.screens.entry.LoginScreen
import com.d2m.app.ui.screens.entry.RolePickerScreen
import com.d2m.app.ui.screens.onboarding.HandoffScreen
import com.d2m.app.ui.screens.onboarding.OnboardingWizardScreen
import com.d2m.app.ui.screens.parent.ParentBrowseScreen
import com.d2m.app.ui.screens.parent.ParentHomeScreen
import com.d2m.app.ui.screens.parent.ParentMessagesScreen
import com.d2m.app.ui.screens.parent.ProfileDetailScreen
import com.d2m.app.ui.screens.parity.AdminGalleryModerationScreen
import com.d2m.app.ui.screens.parity.PanchangamCalendarScreen
import com.d2m.app.ui.screens.parity.SuccessGalleryScreen
import com.d2m.app.ui.screens.shared.NotificationsScreen
import com.d2m.app.ui.screens.shared.SettingsScreen

/**
 * Root NavHost -- mirrors src/App.jsx's route tree (Routes.kt). Bottom-tab
 * chrome per role (parent: Dashboard/Search/Sharing/Settings; child:
 * Discover/Matches/Chat/Profile, per the app-ui.prashanthsridhar.com visual
 * reference adopted in plan §5) wraps PARENT_HOME/PARENT_BROWSE/etc and
 * CHILD_HOME/DISCOVERY/MATCHES/etc respectively -- see AppScaffold.kt for
 * that chrome; this graph only owns route -> screen wiring.
 */
@Composable
fun D2MNavGraph(navController: NavHostController = rememberNavController(), startDestination: String = Routes.LOGIN) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginAsParent = { navController.navigate(Routes.PARENT_HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onLoginAsChild = { navController.navigate(Routes.CHILD_HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onRegister = { navController.navigate(Routes.ROLE_PICKER) },
            )
        }

        composable(Routes.ROLE_PICKER) {
            RolePickerScreen(
                onPickParent = { navController.navigate(Routes.ONBOARDING_WIZARD) },
                onPickChild = { navController.navigate(Routes.claim("")) },
            )
        }

        composable(Routes.ONBOARDING_WIZARD) {
            OnboardingWizardScreen(
                onComplete = { navController.navigate(Routes.HANDOFF) },
            )
        }

        composable(Routes.HANDOFF) {
            HandoffScreen(
                onBrowseProfiles = { navController.navigate(Routes.PARENT_HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
            )
        }

        composable(
            route = Routes.CLAIM,
            arguments = listOf(navArgument(Routes.CLAIM_ARG_TOKEN) { type = NavType.StringType; defaultValue = "" }),
            deepLinks = listOf(
                androidx.navigation.navDeepLink { uriPattern = Routes.CLAIM_DEEPLINK_HTTPS },
                androidx.navigation.navDeepLink { uriPattern = Routes.CLAIM_DEEPLINK_SCHEME },
            ),
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString(Routes.CLAIM_ARG_TOKEN)
            ClaimFlowScreen(
                token = token?.takeIf { it.isNotEmpty() },
                onComplete = { navController.navigate(Routes.CHILD_HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
            )
        }

        composable(Routes.PARENT_HOME) {
            ParentHomeScreen(
                onOpenProfile = { candidateId -> navController.navigate(Routes.profileDetail(candidateId)) },
                onOpenChildProfileDialog = { navController.navigate(Routes.CHILD_PROFILE_DIALOG) },
                onOpenMessages = { navController.navigate(Routes.PARENT_MESSAGES) },
            )
        }

        composable(Routes.PARENT_BROWSE) {
            ParentBrowseScreen(onOpenProfile = { candidateId -> navController.navigate(Routes.profileDetail(candidateId)) })
        }

        composable(Routes.PARENT_MESSAGES) {
            ParentMessagesScreen()
        }

        composable(Routes.CHILD_HOME) {
            ChildHomeScreen(
                onOpenProfile = { candidateId -> navController.navigate(Routes.profileDetail(candidateId)) },
                onOpenDiscover = { navController.navigate(Routes.DISCOVERY) },
                onOpenMatches = { navController.navigate(Routes.MATCHES) },
                onOpenChildProfileDialog = { navController.navigate(Routes.CHILD_PROFILE_DIALOG) },
            )
        }

        composable(Routes.DISCOVERY) {
            DiscoveryScreen(onOpenProfile = { candidateId -> navController.navigate(Routes.profileDetail(candidateId)) })
        }

        composable(Routes.MATCHES) {
            MatchesScreen()
        }

        composable(
            route = Routes.PROFILE_DETAIL,
            arguments = listOf(navArgument(Routes.PROFILE_ARG_CANDIDATE_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val candidateId = backStackEntry.arguments?.getString(Routes.PROFILE_ARG_CANDIDATE_ID).orEmpty()
            ProfileDetailScreen(candidateId = candidateId, onBack = { navController.popBackStack() })
        }

        composable(Routes.NOTIFICATIONS) { NotificationsScreen() }

        composable(Routes.SETTINGS) {
            SettingsScreen(onLogout = { navController.navigate(Routes.LOGIN) { popUpTo(0) } })
        }

        composable(Routes.CHILD_PROFILE_DIALOG) {
            ChildProfileDialogScreen(onClose = { navController.popBackStack() })
        }

        composable(Routes.SUCCESS_GALLERY) { SuccessGalleryScreen() }
        composable(Routes.ADMIN_GALLERY_MODERATION) { AdminGalleryModerationScreen() }
        composable(Routes.PANCHANGAM_CALENDAR) { PanchangamCalendarScreen() }
    }
}
