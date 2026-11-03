package com.d2m.app.messaging.call

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges a notification action's Intent extra (Android-specific,
 * MainActivity.kt) into commonMain UI code (CallLayer.kt).
 *
 * Exists because "Accept still doesn't open the app" persisted even after
 * switching the notification action to PendingIntent.getActivity().send()
 * from inside CallActionReceiver (a BroadcastReceiver) -- confirmed this is
 * a real Android limitation, not a bug in that fix: a BroadcastReceiver
 * launching an Activity, even via a freshly-built PendingIntent, does not
 * reliably inherit the "user just tapped this notification" background-
 * activity-start exemption on Android 10+, especially on stricter OEM
 * builds. The only launch style guaranteed to work is the SAME one the
 * notification's own body tap already used successfully: the action's
 * PendingIntent must directly target the Activity (PendingIntent.getActivity,
 * not getBroadcast) -- see postIncomingCallNotification's Accept action.
 *
 * That means the actual accept() work can no longer happen inside a
 * BroadcastReceiver -- it has to happen once MainActivity is genuinely open,
 * in the app's own composition. MainActivity.onCreate/onNewIntent reads the
 * notification's `EXTRA_ACCEPT_CALL_ID` extra and calls [set]; CallLayer.kt
 * observes [pendingCallId] and performs ensureConnected()+accept() once
 * CallManager actually holds a matching, still-INCOMING call (see that
 * file's own LaunchedEffect).
 */
object PendingCallAccept {
    private val _pendingCallId = MutableStateFlow<String?>(null)
    val pendingCallId: StateFlow<String?> = _pendingCallId.asStateFlow()

    fun set(callId: String) {
        _pendingCallId.value = callId
    }

    fun clear() {
        _pendingCallId.value = null
    }
}
