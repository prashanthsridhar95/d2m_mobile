package com.d2m.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import com.d2m.app.di.KoinBootstrap
import org.koin.compose.KoinContext

/**
 * iOS entry point -- called from iosApp/iosApp/iOSApp.swift's
 * `ComposeView()` wrapper (see that file's doc comment). Koin startup
 * itself now happens in KoinBootstrap.ensureStarted() (commonMain), called
 * from here AND from AppDelegate.didFinishLaunchingWithOptions (before this
 * ever runs, on every launch) -- see that class's doc comment for why a
 * normal foreground launch can no longer be the only place this happens.
 *
 * KoinContext { } wrapper -- confirmed directly via lldb (breakpoint on
 * ThrowException) that every koinInject() call in App()'s composition tree
 * was crashing with an uncaught exception thrown from koin-compose's own
 * LocalKoinScope default CompositionLocal value getter
 * (KoinApplication.kt:52, LocalKoinScope$1.invoke), reached from
 * currentKoinScope() -> koinInject() at App.kt:70. koin-compose's docs mark
 * KoinContext as deprecated/"not needed anymore" on the theory that plain
 * startKoin{} (which KoinBootstrap.ensureStarted() already calls, before
 * this composable's body runs) is enough for the LocalKoinScope default to
 * find the started instance on its own -- that doesn't hold on this
 * project's Kotlin/Native iOS target: the default lazy still throws.
 * KoinContext explicitly resolves KoinPlatform.getKoin() itself and
 * provides it via CompositionLocalProvider, which sidesteps whatever's
 * wrong with the automatic default path here. Deprecated annotation is
 * intentionally overridden with @Suppress below -- this is the fix, not a
 * leftover.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
fun MainViewController() = ComposeUIViewController(
    configure = {
        // Compose Multiplatform 1.11.0 turned "concurrent rendering"
        // (rendering commands encoded on a separate thread,
        // ComposeUIViewControllerConfiguration.parallelRendering, formerly
        // opt-in as useSeparateRenderThingWhenPossible in 1.8.0) on by
        // default. Confirmed directly via computer-use screenshots + the
        // accessibility tree: LoginScreen's text field and Continue button
        // exist in the layout/accessibility hierarchy (hit-testable, correct
        // bounds) but never actually get painted -- clicking the region
        // where the AX tree says they are does nothing visible, while the
        // toggle buttons ABOVE them (composed on the very first frame,
        // before any conditional `if (tab == 0)` content mounts) paint and
        // respond fine. That's consistent with a render-thread frame drop on
        // newly-mounted content rather than a hit-testing bug. Turning this
        // back off trades some render-thread performance for correctness
        // until this is fixed upstream or root-caused further.
        parallelRendering = false
    },
) {
    KoinBootstrap.ensureStarted()
    KoinContext {
        App()
    }
}
