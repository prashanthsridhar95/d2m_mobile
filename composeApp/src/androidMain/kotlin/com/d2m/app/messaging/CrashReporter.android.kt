package com.d2m.app.messaging

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * FirebaseCrashlytics.getInstance() is safe to call even when the app was
 * built WITHOUT google-services.json (no Firebase project configured) --
 * the Crashlytics SDK itself still initializes (it's a plain dependency,
 * unlike the Gradle plugin which is conditionally applied), it just has
 * nowhere real to send reports, so recordException silently no-ops rather
 * than throwing. Wrapped in runCatching anyway as the last line of defense
 * a NON-FATAL reporting path must never itself become a new crash.
 */
actual fun reportNonFatal(throwable: Throwable, context: String?) {
    runCatching {
        val crashlytics = FirebaseCrashlytics.getInstance()
        if (context != null) crashlytics.log(context)
        crashlytics.recordException(throwable)
    }
}
