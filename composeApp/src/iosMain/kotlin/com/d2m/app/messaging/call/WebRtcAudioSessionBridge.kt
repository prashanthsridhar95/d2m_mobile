package com.d2m.app.messaging.call

/**
 * Decouples IosCallKitBridge.kt (this file's caller) from WebRtcEngine.ios.kt
 * (this file's real implementer) so neither file needs to compile-order
 * depend on the other's internals directly.
 *
 * CallKit OWNS the call's AVAudioSession once a call is answered/started --
 * this is not optional, it's how Apple's CallKit + WebRTC integration is
 * required to work (see WebRtcEngine.ios.kt's own doc comment for the full
 * "useManualAudio" story): WebRTC must NOT activate/configure the audio
 * session itself while a CallKit call is live, or the two fight over the
 * same hardware resource and audio silently breaks (no mic, no speaker, or
 * both). Instead, CXProviderDelegate's didActivate/didDeactivate callbacks
 * (IosCallKitBridge.kt) are the ONLY signal WebRTC's RTCAudioSession should
 * react to -- exactly the pattern every production CallKit+WebRTC iOS app
 * (Signal, Telegram, etc.) uses.
 */
object WebRtcAudioSessionBridge {
    var onAudioSessionActivated: (() -> Unit)? = null
    var onAudioSessionDeactivated: (() -> Unit)? = null

    fun audioSessionDidActivate() {
        onAudioSessionActivated?.invoke()
    }

    fun audioSessionDidDeactivate() {
        onAudioSessionDeactivated?.invoke()
    }
}
