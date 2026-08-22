package com.d2m.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the app currently has a visible/foregrounded Activity. Needed so
 * the in-app sound-effects layer (messaging/SoundEffects.kt) and the
 * existing background system-notification bridge
 * (push/LocalNotificationBridge.android.kt) never both fire for the same
 * event -- exactly one of "in-app tone" or "OS notification with its own
 * channel sound" should ever play for a given incoming call/message.
 *
 * A plain top-level object (not DI-injected) is deliberate: LocalNotification
 * Bridge.android.kt already tracks this exact thing internally via its own
 * Application.ActivityLifecycleCallbacks (installed from D2MApplication.
 * onCreate, before Koin starts) -- this just gives that same signal a
 * commonMain-visible home so CallLayer.kt/ChatPane.kt/InAppNotificationLayer.kt
 * can read it too, without duplicating a second lifecycle-callback
 * registration or routing Android's Application through Koin. Defaults to
 * true (safe default -- worst case on a race is one extra/missing tone on
 * the very first event, never a crash).
 */
object AppForegroundState {
    private val _isForeground = MutableStateFlow(true)
    val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

    fun set(foreground: Boolean) {
        _isForeground.value = foreground
    }
}
