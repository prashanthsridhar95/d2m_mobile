package com.d2m.app.messaging.call

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges an Android notification action into common call UI code.
 *
 * The notification action opens the activity with a call id; the activity
 * publishes that id here and CallLayer performs the accept once the incoming
 * call has reached CallManager.
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
