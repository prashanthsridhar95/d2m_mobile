package com.d2m.app.push.ui

import androidx.compose.runtime.Composable

/**
 * No-op here -- iOS's PlatformPushInitializer.initialize() already calls
 * UNUserNotificationCenter.requestAuthorizationWithOptions itself, so
 * calling this too would just double-prompt. Kept as a real expect/actual
 * (rather than only calling this from androidMain) so App.kt's shared
 * LaunchedEffect stays platform-agnostic.
 */
@Composable
actual fun rememberNotificationPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit {
    return { onResult(true) }
}
