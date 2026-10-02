package com.d2m.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.d2m.app.data.network.ApiClient
import com.d2m.app.data.network.AuditApi
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.SeriousModeRepository
import com.d2m.app.messaging.ChatUiState
import com.d2m.app.messaging.MessagingRepository
import com.d2m.app.messaging.ParentContactsStore
import com.d2m.app.messaging.d2mIdToMessagingUsername
import com.d2m.app.messaging.call.ui.CallLayer
import com.d2m.app.messaging.ui.InAppNotificationLayer
import com.d2m.app.push.PlatformPushInitializer
import com.d2m.app.push.ui.rememberBatteryOptimizationRequester
import com.d2m.app.push.ui.rememberNotificationPermissionLauncher
import com.d2m.app.security.observeAppBackgrounded
import com.d2m.app.security.observeScreenCapture
import com.d2m.app.security.observeScreenshotTaken
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.LocalPhotoImageLoader
import com.d2m.app.ui.components.StepUpConfirmDialogHost
import com.d2m.app.ui.components.StepUpController
import com.d2m.app.ui.components.WatermarkOverlay
import com.d2m.app.ui.navigation.ChildTabs
import com.d2m.app.ui.navigation.D2MBottomBar
import com.d2m.app.ui.navigation.D2MNavGraph
import com.d2m.app.ui.navigation.ParentTabs
import com.d2m.app.ui.navigation.Routes
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import kotlinx.coroutines.launch
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
    val messagingRepo: MessagingRepository = koinInject()
    val apiClient: ApiClient = koinInject()
    val auditApi: AuditApi = koinInject()
    val seriousModeRepo: SeriousModeRepository = koinInject()
    val parentContactsStore: ParentContactsStore = koinInject()
    val stepUpController: StepUpController = koinInject()
    // Authenticated profile-photo ImageLoader (data/network/PhotoImageLoader.kt)
    // -- provided once here so every ProfilePhoto/ProfileThumb below picks it
    // up via LocalPhotoImageLoader instead of the app-wide Coil default.
    val photoImageLoader: ImageLoader = koinInject()
    val identity by identityStore.identity.collectAsState()
    val conversationOpen by chatUiState.conversationOpen.collectAsState()
    val messagingStartupError by messagingRepo.startupError.collectAsState()
    val scope = rememberCoroutineScope()

    // "Messages sent on web is received on web, but not on mobile" --
    // root cause: MessagingRepository.start() (which opens the WebSocket)
    // was only ever called from ChatPane.kt's DisposableEffect(peerId), i.e.
    // only while a specific 1:1 conversation screen was actually open. Any
    // other time -- thread list, other tabs, backgrounded -- mobile had no
    // live socket at all, unlike d2m_web's MessagingProvider (mounted once
    // at the shell level, above the whole route tree, specifically so it
    // "survive[s] navigating anywhere else in the app" per that file's own
    // doc comment). Hoisted the same way here: fires once an identity
    // exists, and MessagingRepository.start() is already idempotent (a
    // `started` guard that's only set once a connection attempt actually
    // begins), so this is safe to run alongside ChatPane's own call too.
    LaunchedEffect(identity.primaryId, identity.sponsorId) {
        if (identity.primaryId != null || identity.sponsorId != null) {
            runCatching { messagingRepo.start(apiClient.client) }
            // Reported directly: "When app is not alive, I don't receive
            // notification (even now)." Root cause: this used to live in the
            // LaunchedEffect(Unit) below, firing exactly once at first
            // composition -- BEFORE identity is guaranteed to be loaded (a
            // fresh install has no identity at all until onboarding
            // finishes; even a returning user's IdentityStore restore can
            // race the async FirebaseMessaging.getInstance().token callback
            // this kicks off). PushTokenRegistrar.registerCurrentToken reads
            // identityStore.identity.value at the moment the token callback
            // resolves and silently no-ops if there's no accountId yet --
            // and since it's only ever invoked from that one FCM token
            // fetch (onNewToken fires again only when the token itself
            // rotates, e.g. months later), a registration that lost that
            // race was never retried. This device's DeviceToken row was
            // simply never created server-side, so every push attempt is
            // "no_device_token" regardless of whether the backend's own FCM
            // config (D2M_FCM_SERVICE_ACCOUNT_JSON) is correct. Re-running
            // here, keyed on identity like messagingRepo.start() above,
            // guarantees at least one registration attempt happens at a
            // point identity is guaranteed non-null; FirebaseMessaging's
            // token fetch is cheap/idempotent so re-firing on every identity
            // change (login, role switch) is harmless.
            runCatching { pushInitializer.initialize() }

            // "In app notification still shows id instead of name." Root
            // cause: messagingRepo.peerDisplayName() only ever gets fed a
            // real name via rememberPeerName() -- and that call previously
            // only happened inside MatchesScreen.kt (child) or
            // ParentMessagesScreen.kt (parent), i.e. only once THAT specific
            // screen had actually composed this session. InAppNotificationLayer
            // (mounted globally, can fire from any tab) and
            // LocalNotificationBridge both read the exact same map -- a
            // message arriving while the user's sitting on Home/Discover/
            // Settings, having never opened Matches this session, fell back
            // to the raw derived username. Hoisting this here (same
            // identity-gated effect as messagingRepo.start()/pushInitializer
            // above) means every match/contact's real name is known as soon
            // as identity loads, regardless of which screen is showing --
            // SeriousModeRepository.getThreads is already cache-backed
            // (15s TTL, see that class), and ParentContactsStore.attach is a
            // synchronous local read, so neither adds a real network hit
            // beyond what those screens would do anyway.
            runCatching {
                identity.primaryId?.let { primaryId ->
                    seriousModeRepo.getThreads(primaryId).forEach { t ->
                        messagingRepo.rememberPeerName(t.otherParticipantId, t.otherParticipantName)
                    }
                }
                identity.sponsorId?.let { sponsorId ->
                    val myUsername = d2mIdToMessagingUsername(sponsorId)
                    parentContactsStore.attach(myUsername)
                    parentContactsStore.contacts.value.values.forEach { c ->
                        messagingRepo.rememberPeerName(c.d2mId, c.name)
                    }
                }
            }
        }
    }

    // Fire-once: the OS notification permission prompt isn't tied to
    // identity or any particular screen's lifecycle -- reported directly:
    // "Notification allow is not asked on launch" (Android 13+'s
    // POST_NOTIFICATIONS is a runtime permission; declaring it in the
    // manifest alone never prompts anyone -- see push/ui/NotificationPermission.kt).
    val requestNotificationPermission = rememberNotificationPermissionLauncher {}
    // Reported directly: "runs in the same machine - but notifications not
    // received when app is not live" -- after confirming client/server FCM
    // config already matched. OEM battery management (Xiaomi/Samsung/
    // OnePlus etc.) killing the process or throttling FCM delivery well
    // beyond stock Android's Doze exceptions is the most common remaining
    // cause of exactly that symptom. See BatteryOptimizationCompat.kt.
    val requestBatteryOptimizationExemption = rememberBatteryOptimizationRequester()
    LaunchedEffect(Unit) {
        requestNotificationPermission()
        requestBatteryOptimizationExemption()
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Screenshot/recording hardening (security/ScreenCapture.kt) -- both
    // effects are permanent no-ops on Android, where FLAG_SECURE
    // (MainActivity.onCreate) already blocks capture outright at the OS
    // level; only iOS's actuals do anything, since Apple gives apps no way
    // to prevent a screenshot or recording, only to react to one.
    var isRecordingCaptured by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val remove = observeScreenCapture { captured -> isRecordingCaptured = captured }
        onDispose { remove() }
    }
    // rememberUpdatedState so this doesn't need to re-subscribe to the OS
    // notification (and risk missing an event mid-resubscribe) every time
    // the route changes -- the callback below always reads the CURRENT
    // route at the moment a screenshot actually happens.
    val latestRoute by rememberUpdatedState(currentRoute)
    DisposableEffect(Unit) {
        val remove = observeScreenshotTaken {
            scope.launch {
                auditApi.recordScreenView("SCREENSHOT_DETECTED:${latestRoute ?: "unknown"}")
            }
        }
        onDispose { remove() }
    }
    // Blanks the screen the instant iOS is about to background the app --
    // before the OS takes its app-switcher snapshot, unlike the
    // screenshot observer above, which only ever fires after the fact.
    // No-op on Android, same reasoning as observeScreenCapture above.
    var isBackgrounded by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val remove = observeAppBackgrounded { backgrounded -> isBackgrounded = backgrounded }
        onDispose { remove() }
    }

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

    CompositionLocalProvider(LocalPhotoImageLoader provides photoImageLoader) {
    D2MTheme(flow = D2MFlow.ENTRY) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                // Explicitly systemBars only -- NOT Material3's own default,
                // which folds the IME inset into this same Scaffold-wide
                // padding. Since that padding wraps the ENTIRE nav graph
                // (every screen's content, including a chat's header AND its
                // message list, all as one block -- see D2MNavGraph below),
                // letting it react to the keyboard meant the whole screen
                // compressed/rose as a unit whenever it opened, not just the
                // composer (reported directly: "the entire view moves up").
                // ChatPane now claims its own `imePadding()` locally instead
                // (see MatchesScreen.kt) so only its message-list+composer
                // area shrinks to make room, while everything above it --
                // this Scaffold's own chrome, and each screen's header --
                // stays completely still.
                contentWindowInsets = WindowInsets.systemBars,
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

            // Reusable trust-subsystem step-up ("confirm your password")
            // dialog -- mounted once here, same reasoning as CallLayer
            // above, so any screen/ViewModel can just call
            // stepUpController.confirmStepUp() without wiring its own
            // Dialog. Renders nothing while no confirmation is pending --
            // see ui/components/StepUpConfirmDialog.kt.
            StepUpConfirmDialogHost(stepUpController)

            // Instagram-style in-app message banner (point 4 of the
            // notification request) -- mounted the same way as CallLayer so
            // it can show up regardless of which screen is on top. Tapping
            // it records the peer via ChatUiState and navigates to the
            // right role's messaging surface; that screen (MatchesScreen.kt
            // for Child, ParentMessagesScreen.kt for Parent) picks the
            // pending peer up once its list is loaded and opens that
            // conversation directly.
            //
            // Previously gated to `identity.role == D2MRole.CHILD` only --
            // Parent had ZERO foreground notification signal at all as a
            // result (reported directly: "when app is not in the
            // background, notifications are not received" -- traced to this
            // gate, not a bug in the banner mechanism itself, see
            // ParentMessagesScreen.kt's doc comment on why Parent messaging
            // didn't exist to notify about in the first place). Now that
            // Parent has a real messaging screen, both roles get the same
            // foreground signal.
            if (identity.role != null) {
                InAppNotificationLayer(
                    onOpenPeer = { peerUsername ->
                        chatUiState.requestOpenPeer(peerUsername)
                        val target = if (identity.role == D2MRole.PARENT) Routes.PARENT_MESSAGES else Routes.MATCHES
                        navController.navigate(target) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }

            // Standing banner (not a toast -- doesn't auto-dismiss) for when
            // MessagingRepository.start() itself fails: identity/key
            // generation or the initial WebSocket connect. Previously this
            // failure was invisible -- the LaunchedEffect above wraps
            // start() in runCatching, so calls/messages just silently never
            // worked with nothing on screen to explain why (reported
            // directly: "totally silent, no error shown", even with the
            // backend confirmed healthy). Retry re-runs start(), which is
            // safe to call repeatedly (its own `started` guard no-ops once
            // it actually succeeds).
            AnimatedVisibility(
                visible = messagingStartupError != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp, start = 10.dp, end = 10.dp),
            ) {
                val message = messagingStartupError
                if (message != null) {
                    D2MErrorBanner(
                        message = message,
                        onRetry = { scope.launch { messagingRepo.start(apiClient.client) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Forensic screen watermark (d2m_web's WatermarkOverlay.jsx
            // counterpart) -- drawn above everything else here, including
            // the error banner above, so it's captured in any screenshot.
            if (identity.role != null) {
                WatermarkOverlay(screen = currentRoute ?: "unknown")
            }

            // Live screen-recording cover (iOS only -- see
            // security/ScreenCapture.kt's doc comment on why Android has
            // nothing to cover, FLAG_SECURE already blocked the capture).
            // Opaque and LAST in this Box, above even the watermark: the
            // watermark is meant to be captured if something leaks despite
            // this, but while a recording is actively running, hiding the
            // content outright is strictly better than relying on someone
            // noticing a faint overlay in the recorded footage.
            if (isRecordingCaptured) {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "Screen recording detected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Profile content is hidden while your screen is being recorded or mirrored.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // App-switcher cover (iOS only -- see security/ScreenCapture.kt's
            // observeAppBackgrounded doc comment). LAST of all, above even
            // the recording cover: this one has no animation and no delay,
            // because whatever's on screen the instant this composes IS
            // what iOS snapshots for the app-switcher a moment later. Plain
            // wordmark rather than the recording cover's explanatory text --
            // nobody actually reads this frame, it exists purely to be
            // what gets captured, not to be looked at.
            if (isBackgrounded) {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "D2M",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
    }
}
