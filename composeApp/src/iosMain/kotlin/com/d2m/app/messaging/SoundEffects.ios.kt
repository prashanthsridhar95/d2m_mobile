package com.d2m.app.messaging

// No Xcode project exists to run this in yet (same precedent as
// CrashReporter.ios.kt/WebRtcEngine.ios.kt) -- stubbed no-op rather than
// wired to AVAudioPlayer until there's a real iOS target to hear it on.
actual object SoundEffects {
    actual fun playCallTone() = Unit
    actual fun stopCallTone() = Unit
    actual fun playMsgTone() = Unit
    actual fun playInChatTone() = Unit
}
