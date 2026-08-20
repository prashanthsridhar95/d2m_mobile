package com.d2m.app.push.ui

import androidx.compose.runtime.Composable

/**
 * Requests the OS notification permission on launch -- reported directly:
 * "Notification allow is not asked on launch." Root cause: Android 13+
 * (API 33, "TIRAMISU") made POST_NOTIFICATIONS a runtime permission, same
 * shape as RECORD_AUDIO/CAMERA (see call/ui/CallPermissions.kt) -- declaring
 * it in AndroidManifest.xml (already done) is necessary but not sufficient;
 * something has to actually call the runtime prompt, and nothing did.
 * FCM token registration (PlatformPushInitializer.android.kt) doesn't need
 * this permission and was working regardless -- so the token half was
 * "getting registered" fine, but no notification could ever actually be
 * shown (LocalNotificationBridge.kt, D2MFirebaseMessagingService.kt) until
 * the user granted this. iOS's actual already requests permission itself
 * inside PlatformPushInitializer.initialize() (UNUserNotificationCenter),
 * so the iOS actual here is a same-turn no-op to avoid double-prompting.
 */
@Composable
expect fun rememberNotificationPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit
