package com.d2m.app.messaging

// No Crashlytics wired up on iOS yet (not a real target for this build --
// same precedent as CryptoProvider.ios.kt/WebRtcEngine.ios.kt). println
// rather than a silent no-op so a call site's failure is still at least
// visible in a local debug session.
actual fun reportNonFatal(throwable: Throwable, context: String?) {
    println("reportNonFatal${context?.let { " [$it]" } ?: ""}: $throwable")
}
