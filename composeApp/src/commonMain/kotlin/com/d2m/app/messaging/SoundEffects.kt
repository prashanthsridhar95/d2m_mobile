package com.d2m.app.messaging

/**
 * In-app sound effects -- direct port of d2m_web's lib/sound.js (see that
 * file's own doc comment for the full "why these three, why not gated on
 * mute" history). Reported directly: "Why notification sounds are not
 * working like how it's working on web?" -- root cause was that mobile had
 * NO equivalent of this file at all. Sound only ever played through
 * Android's OS notification-channel sound (push/LocalNotificationBridge.
 * android.kt), which is explicitly skipped while the app is foregrounded
 * (`if (isForeground) return@collect`) -- and CallLayer.kt's incoming-call
 * UI, despite looking like a ringing phone, was purely visual with zero
 * audio behind it. Web plays these three tones directly off the WebSocket
 * event, regardless of whether the tab is focused, for every incoming call/
 * message; this expect/actual is that same behavior for the foreground case
 * on mobile (the background case stays covered by the existing system
 * notification channel sound -- see AppForegroundState.kt's doc comment on
 * why exactly one of the two ever fires for a given event).
 *
 * playCallTone: loops for the whole "incoming" phase, same as web's shared/
 * reused Audio element. playMsgTone/playInChatTone: one-shot, safe to
 * overlap (mirrors web's "a fresh Audio() per call... overlapping one-shot
 * plays are fine here").
 */
expect object SoundEffects {
    fun playCallTone()
    fun stopCallTone()
    fun playMsgTone()
    fun playInChatTone()
}
