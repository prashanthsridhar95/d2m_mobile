package com.d2m.app.messaging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges a chat-notification tap's Android/iOS-specific intent extra (or
 * equivalent) into commonMain UI code (App.kt), the exact same role
 * messaging/call/PendingCallAccept.kt already plays for the incoming-call
 * notification's Accept action -- see that file's doc comment for why a
 * plain PendingIntent.getActivity() tap target can't run app logic
 * directly and has to hand off to a value MainActivity reads once the
 * Activity is actually open.
 *
 * Root cause this exists to fix: tapping a chat push notification
 * (postMessageNotification's own content Intent, LocalNotificationBridge.kt)
 * previously carried no peer identity at all -- unlike that same
 * notification's Reply/Mark-as-read actions, which already pass
 * EXTRA_PEER_USERNAME to MessageReplyReceiver/MarkReadReceiver. A plain tap
 * landed on whatever screen the app happened to cold/warm-start into, with
 * no active peer/thread selected -- composing and sending from there went
 * nowhere, since nothing told ChatUiState which conversation to open
 * (reported directly: "From notifications, message is not getting sent").
 * MainActivity.kt/iOSApp.swift read the tap's peer identity and call [set];
 * App.kt observes [pendingPeerUsername] and routes it into
 * ChatUiState.requestOpenPeer() -- the exact mechanism that already works
 * correctly for the in-app notification banner's own tap handler.
 */
object PendingOpenPeer {
    private val _pendingPeerUsername = MutableStateFlow<String?>(null)
    val pendingPeerUsername: StateFlow<String?> = _pendingPeerUsername.asStateFlow()

    fun set(peerUsername: String) {
        _pendingPeerUsername.value = peerUsername
    }

    fun clear() {
        _pendingPeerUsername.value = null
    }
}
