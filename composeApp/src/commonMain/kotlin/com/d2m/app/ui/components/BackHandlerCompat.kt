package com.d2m.app.ui.components

import androidx.compose.runtime.Composable

/**
 * Intercepts the system back gesture/button.
 *
 * Reported directly: "Going into a chat & pressing back takes me home -
 * should take me to matches." MatchesScreen.kt/ParentMessagesScreen.kt both
 * render the thread list and an open conversation as the SAME nav-graph
 * route, switched by local `selected`/`selectedUsername` state rather than a
 * route change (see MatchesScreen.kt's doc comment on why -- it needs to
 * survive App.kt's bottom-tab-bar visibility toggle without a real nav
 * transition). Without this, Android's system back has nothing to intercept
 * inside that single route, so it falls straight through to the
 * NavController's own back stack and pops past the conversation entirely,
 * landing on the start destination (Home) instead of just closing the chat.
 *
 * `androidx.activity.compose.BackHandler` (the real fix) lives in
 * `activity-compose`, an Android-only artifact -- not available from
 * commonMain. expect/actual per this session's established pattern
 * (CrashReporter.kt, GifConfig.kt): Android gets the real intercept, iOS
 * gets a no-op (no real target to verify a back-gesture story against yet,
 * see WebRtcEngine.ios.kt's identical reasoning -- iOS's own swipe-back is a
 * UIKit-level gesture this Compose Multiplatform target doesn't own here).
 */
@Composable
expect fun BackHandlerCompat(enabled: Boolean, onBack: () -> Unit)
