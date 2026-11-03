package com.d2m.app.data.session

import com.d2m.app.data.local.D2M_APP_GROUP_ID
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

/**
 * Backed by the shared App Group suite (same D2M_APP_GROUP_ID as
 * SqlDriverFactory.ios.kt) instead of NSUserDefaults.standardUserDefaults --
 * IdentityStore and everything else layered on top of [Settings] (session
 * tokens, the locally-cached identity, etc.) needs to be visible from the
 * Notification Service Extension process too, same reasoning as the SQLite
 * DB. `NSUserDefaults(suiteName:)` returns nil if the App Group entitlement
 * isn't actually configured for this target yet -- falls back to the
 * standard per-process suite in that case, matching
 * SqlDriverFactory.ios.kt's own fallback behavior for a checkout that
 * hasn't done the Xcode-side App Group setup yet.
 */
actual fun createSettings(): Settings =
    NSUserDefaultsSettings(NSUserDefaults(suiteName = D2M_APP_GROUP_ID) ?: NSUserDefaults.standardUserDefaults)
