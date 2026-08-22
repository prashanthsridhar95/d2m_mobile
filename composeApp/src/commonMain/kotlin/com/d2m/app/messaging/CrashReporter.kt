package com.d2m.app.messaging

/**
 * Non-fatal error reporting -- lets commonMain catch blocks that already
 * recover from an exception (so it never crashes the app) still surface
 * that failure somewhere a developer will actually see it, instead of only
 * a `println` that vanishes the moment the device isn't attached to a debugger.
 *
 * Backed by Firebase Crashlytics on Android (see CrashReporter.android.kt --
 * same conditional google-services.json/plugin gating as push, see
 * composeApp/build.gradle.kts). iOS has no equivalent wired up yet (same
 * "not a real target for this build" precedent as CryptoProvider.ios.kt) --
 * falls back to println there so a call site never needs its own
 * platform check.
 *
 * Deliberately NOT a blanket "log everything" hook -- call sites choose
 * what's worth reporting (a recovered-from exception that indicates a real
 * bug is worth it; an expected/handled condition like a 404 usually isn't).
 */
expect fun reportNonFatal(throwable: Throwable, context: String? = null)
