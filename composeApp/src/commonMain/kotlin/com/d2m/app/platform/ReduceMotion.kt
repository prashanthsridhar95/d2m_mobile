package com.d2m.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.d2m.app.AppForegroundState

/**
 * True when the user has asked the OS to cut down on motion (Android: animator duration scale 0 / "Remove animations";
 * iOS: Settings > Accessibility > Motion > Reduce Motion).
 */
expect fun systemReduceMotion(): Boolean

/**
 * [systemReduceMotion], re-read whenever the app returns to the foreground (the setting can only be changed outside the
 * app), so a user who flips it and comes back sees the change without a restart.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val foreground by AppForegroundState.isForeground.collectAsState()
    val state: State<Boolean> = remember(foreground) { androidx.compose.runtime.mutableStateOf(systemReduceMotion()) }
    return state.value
}
