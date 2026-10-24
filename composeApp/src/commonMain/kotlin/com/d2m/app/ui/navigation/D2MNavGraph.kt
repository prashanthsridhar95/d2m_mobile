package com.d2m.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
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
import com.d2m.app.ui.screens.public.ProfileLinkScreen
import com.d2m.app.ui.screens.shared.IdentityVerificationScreen
import com.d2m.app.ui.screens.shared.NotificationsScreen
import com.d2m.app.ui.screens.shared.SettingsScreen
import com.d2m.app.ui.screens.shared.ShareLinksScreen
import com.d2m.app.ui.screens.shared.TrustScreen

private const val PUSH_MS = 300
private const val PUSH_FADE_MS = 160
private val PushEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * Root NavHost -- mirrors src/App.jsx's route tree (Routes.kt). Bottom-tab
 * chrome per role (parent: Dashboard/Search/Sharing/Settings; child:
 * Discover/Matches/Chat/Profile, per the app-ui.prashanthsridhar.com visual
 * reference adopted in plan §5) wraps PARENT_HOME/PARENT_BROWSE/etc and
 * CHILD_HOME/DISCOVERY/MATCHES/etc respectively -- see AppScaffold.kt for
 * that chrome; this graph only owns route -> screen wiring.
 */
@Composable
fun D2MNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.LOGIN,
    /** Routes currently shown as bottom tabs for this role; tab<->tab hops crossfade instead of sliding. */
    tabRoutes: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
) {
    // Transition rules (read lazily at transition time, so they follow role changes):
    //  - tab <-> tab, or into/out of Login: short crossfade (no spatial relationship)
    //  - everything else: iOS-style push -- new screen slides in from the right
    //    while the old one drifts left at ~30% speed (parallax); pop reverses it.
    fun AnimatedContentTransitionScope<NavBackStackEntry>.isFade(): Boolean {
        val from = initialState.destination.route
        val to = targetState.destination.route
        return (from in tabRoutes && to in tabRoutes) || from == Routes.LOGIN || to == Routes.LOGIN
    }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            if (isFade()) fadeIn(tween(PUSH_FADE_MS, delayMillis = 40))
            else slideInHorizontally(tween(PUSH_MS, easing = PushEasing)) { it } + fadeIn(tween(PUSH_MS / 2))
        },
        exitTransition = {
            if (isFade()) fadeOut(tween(PUSH_FADE_MS))
            else slideOutHorizontally(tween(PUSH_MS, easing = PushEasing)) { -(it * 3) / 10 } + fadeOut(tween(PUSH_MS, easing = PushEasing), targetAlpha = 0.6f)
        },
        popEnterTransition = {
            if (isFade()) fadeIn(tween(PUSH_FADE_MS, delayMillis = 40))
            else slideInHorizontally(tween(PUSH_MS, easing = PushEasing)) { -(it * 3) / 10 } + fadeIn(tween(PUSH_MS, easing = PushEasing), initialAlpha = 0.6f)
        },
        popExitTransition = {
            if (isFade()) fadeOut(tween(PUSH_FADE_MS))
            else slideOutHorizontally(tween(PUSH_MS, easing = PushEasing)) { it } + fadeOut(tween(PUSH_MS / 2, delayMillis = PUSH_MS / 2))
        },
    ) {

        d2mComposable(Routes.LOGIN) {
            LoginScreen(
                onLoginAsParent = { navController.navigate(Routes.PARENT_HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onLoginAsChild = { navController.navigate(Routes.CHILD_HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onRegister = { navController.navigate(Routes.ROLE_PICKER) },
            )
        }

        d2mComposable(Routes.ROLE_PICKER) {
            RolePickerScreen(
                onPickParent = { navController.navigate(Routes.ONBOARDING_WIZARD) },
                onPickChild = { navController.navigate(Routes.claim("")) },
            )
        }

        d2mComposable(Routes.ONBOARDING_WIZARD) {
            OnboardingWizardScreen(
                onComplete = { navController.navigate(Routes.HANDOFF) },
            )
        }

        d2mComposable(Routes.HANDOFF) {
            HandoffScreen(
                onBrowseProfiles = { navController.navigate(Routes.PARENT_HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
            )
        }

        d2mComposable(
            route = Routes.CLAIM,
            arguments = listOf(navArgument(Routes.CLAIM_ARG_TOKEN) { type = NavType.StringType; defaultValue = "" }),
            deepLinks = listOf(
                androidx.navigation.navDeepLink { uriPattern = Routes.CLAIM_DEEPLINK_HTTPS },
                androidx.navigation.navDeepLink { uriPattern = Routes.CLAIM_DEEPLINK_SCHEME },
            ),
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.read { getStringOrNull(Routes.CLAIM_ARG_TOKEN) }
            ClaimFlowScreen(
                token = token?.takeIf { it.isNotEmpty() },
                onComplete = { navController.navigate(Routes.CHILD_HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
            )
        }

        d2mComposable(Routes.PARENT_HOME) {
            ParentHomeScreen(
                onOpenProfile = { candidateId -> navController.navigate(Routes.profileDetail(candidateId)) },
                onOpenChildProfileDialog = { navController.navigate(Routes.CHILD_PROFILE_DIALOG) },
                onOpenMessages = { navController.navigate(Routes.PARENT_MESSAGES) },
            )
        }

        d2mComposable(Routes.PARENT_BROWSE) {
            ParentBrowseScreen(onOpenProfile = { candidateId -> navController.navigate(Routes.profileDetail(candidateId)) })
        }

        d2mComposable(Routes.PARENT_MESSAGES) {
            ParentMessagesScreen()
        }

        d2mComposable(Routes.CHILD_HOME) {
            ChildHomeScreen(
                onOpenProfile = { candidateId -> navController.navigate(Routes.profileDetail(candidateId)) },
                onOpenDiscover = { navController.navigateToTab(Routes.DISCOVERY) },
                onOpenMatches = { navController.navigateToTab(Routes.MATCHES) },
                onOpenNotifications = { navController.navigateToTab(Routes.NOTIFICATIONS) },
                onOpenChildProfileDialog = { navController.navigate(Routes.CHILD_PROFILE_DIALOG) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        d2mComposable(Routes.DISCOVERY) {
            DiscoveryScreen(onOpenMatches = { navController.navigateToTab(Routes.MATCHES) })
        }

        d2mComposable(Routes.MATCHES) {
            MatchesScreen(
                onOpenProfile = { candidateId -> navController.navigate(Routes.profileDetail(candidateId)) },
                onOpenGallery = { navController.navigate(Routes.SUCCESS_GALLERY) },
            )
        }

        d2mComposable(
            route = Routes.PROFILE_DETAIL,
            arguments = listOf(navArgument(Routes.PROFILE_ARG_CANDIDATE_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val candidateId = backStackEntry.arguments?.read { getStringOrNull(Routes.PROFILE_ARG_CANDIDATE_ID) }.orEmpty()
            ProfileDetailScreen(
                candidateId = candidateId,
                onBack = { navController.popBackStack() },
                // Same plain navigate() ParentHomeScreen's own onOpenMessages
                // already uses -- "Message their parent" leaves this profile
                // on the back stack, same as any other forward navigation
                // here, so Back from Messages returns to it.
                onOpenMessages = { navController.navigate(Routes.PARENT_MESSAGES) },
            )
        }

        d2mComposable(Routes.NOTIFICATIONS) { NotificationsScreen() }

        d2mComposable(Routes.SHARE_LINKS) { ShareLinksScreen() }

        d2mComposable(Routes.TRUST) { TrustScreen() }

        d2mComposable(Routes.IDENTITY_VERIFICATION) { IdentityVerificationScreen(onBack = { navController.popBackStack() }) }

        d2mComposable(Routes.SETTINGS) {
            SettingsScreen(
                onLogout = { navController.navigate(Routes.LOGIN) { popUpTo(0) } },
                onOpenShareLinks = { navController.navigate(Routes.SHARE_LINKS) },
                onOpenTrust = { navController.navigate(Routes.TRUST) },
                onOpenVerification = { navController.navigate(Routes.IDENTITY_VERIFICATION) },
                onOpenProfile = { navController.navigate(Routes.CHILD_PROFILE_DIALOG) },
                // "I want it to be exactly the same" as viewing any other
                // profile (reported directly, correcting an earlier
                // from-scratch PreviewProfileScreen that only approximated
                // it) -- this is literally ProfileDetailScreen itself, not
                // a lookalike, same route every other "open this
                // candidate" call site uses, just pointed at your own id.
                // ProfileDetailScreen hides the handful of actions that
                // don't make sense pointed at yourself once it detects
                // viewerPrimaryId == candidateId -- see its own `isSelf`.
                onOpenProfilePreview = { candidateId -> navController.navigate(Routes.profileDetail(candidateId)) },
            )
        }

        d2mComposable(Routes.CHILD_PROFILE_DIALOG) {
            ChildProfileDialogScreen(onClose = { navController.popBackStack() })
        }

        d2mComposable(Routes.SUCCESS_GALLERY) { SuccessGalleryScreen() }
        d2mComposable(Routes.ADMIN_GALLERY_MODERATION) { AdminGalleryModerationScreen() }
        d2mComposable(Routes.PANCHANGAM_CALENDAR) { PanchangamCalendarScreen() }

        d2mComposable(
            route = Routes.PROFILE_LINK,
            arguments = listOf(navArgument(Routes.PROFILE_LINK_ARG_CODE) { type = NavType.StringType; defaultValue = "" }),
            deepLinks = listOf(
                androidx.navigation.navDeepLink { uriPattern = Routes.PROFILE_LINK_DEEPLINK_HTTPS },
                androidx.navigation.navDeepLink { uriPattern = Routes.PROFILE_LINK_DEEPLINK_SCHEME },
            ),
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.read { getStringOrNull(Routes.PROFILE_LINK_ARG_CODE) }.orEmpty()
            ProfileLinkScreen(
                code = code,
                onReachOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) } },
            )
        }
    }
}

/**
 * Every destination gets an opaque page-ground behind it: during push/pop the
 * outgoing and incoming screens overlap, and screens draw no background of
 * their own, so without this the old screen would show through the new one.
 */
private fun NavGraphBuilder.d2mComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable(route = route, arguments = arguments, deepLinks = deepLinks) { entry ->
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content(entry) }
    }
}
