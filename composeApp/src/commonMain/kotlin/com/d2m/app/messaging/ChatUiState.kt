package com.d2m.app.messaging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small shared UI-chrome flag, not message state -- lets MatchesScreen.kt
 * (where "is a conversation open" is local Compose state, since the thread
 * list and the open conversation are the same MATCHES nav route switched by
 * a `selected` variable, not two different routes) tell App.kt's bottom-tab
 * Scaffold to hide while chatting -- every real messaging app does this
 * (point 2 of the reported UX gap: "bottom nav bar is not required inside a
 * person's chat"), and lets a tapped in-app notification banner
 * (messaging/ui/InAppNotificationLayer.kt) tell MatchesScreen.kt which
 * thread to jump straight to (point 4's banner needs somewhere to send the
 * user). A Koin singleton is cheaper and lower-risk here than converting the
 * open conversation into its own nav route (which would also need correct
 * back-stack/deep-link/predictive-back handling redone) -- same reasoning as
 * MessagingRepository/CallManager already being the cross-screen state-
 * sharing mechanism in this app.
 */
class ChatUiState {
    private val _conversationOpen = MutableStateFlow(false)
    val conversationOpen: StateFlow<Boolean> = _conversationOpen.asStateFlow()

    fun setConversationOpen(open: Boolean) {
        _conversationOpen.value = open
    }

    private val _pendingOpenPeerUsername = MutableStateFlow<String?>(null)
    val pendingOpenPeerUsername: StateFlow<String?> = _pendingOpenPeerUsername.asStateFlow()

    /** Called by the in-app notification banner's tap handler. */
    fun requestOpenPeer(peerUsername: String) {
        _pendingOpenPeerUsername.value = peerUsername
    }

    /** Called by MatchesScreen.kt once it has acted on (or given up trying to act on) a pending request. */
    fun clearPendingOpenPeer() {
        _pendingOpenPeerUsername.value = null
    }

    // "open profile & trigger system back - moves to matches page - should
    // go to chat only." The actual root cause (a second one, separate from
    // the back-handler-priority fix in MatchesScreen.kt): MatchesScreen's
    // own `selected` thread was plain `remember` state, which does NOT
    // survive Compose Navigation disposing MatchesScreen's composition when
    // a different destination (ProfileDetailScreen, pushed by tapping the
    // avatar) is on top -- NavHost only guarantees restoring state that's
    // routed through a saveable-state registry, not arbitrary `remember`
    // values. The result: navigating to the profile and back recomposed
    // MatchesScreen from scratch with `selected` reset to null, landing on
    // the bare thread list regardless of which back-press handler fired.
    // Storing just the id here (this Koin singleton survives navigation the
    // same way it already does for pendingOpenPeerUsername above) lets
    // MatchesScreen re-derive `selected` from `threads` on every
    // recomposition instead of depending on in-memory state surviving.
    private val _activeThreadId = MutableStateFlow<String?>(null)
    val activeThreadId: StateFlow<String?> = _activeThreadId.asStateFlow()

    fun setActiveThreadId(threadId: String?) {
        _activeThreadId.value = threadId
    }
}
