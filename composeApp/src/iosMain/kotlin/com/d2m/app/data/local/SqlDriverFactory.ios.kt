package com.d2m.app.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import platform.Foundation.NSFileManager

/**
 * D2M_APP_GROUP_ID must match the App Group capability added to BOTH the
 * main app target AND the Notification Service Extension target in Xcode
 * (Signing & Capabilities > + Capability > App Groups -- create/select the
 * same group id on each) -- see iosApp/README.md's setup steps. Without a
 * shared App Group, each process gets its own private sandbox and the
 * extension can never see the same SQLite DB (message history, Signal
 * session/ratchet state) the main app already has, meaning
 * NotificationService.swift's decrypt attempt would fail on every single
 * message (no session to decrypt against) even with everything else wired
 * up correctly.
 */
internal const val D2M_APP_GROUP_ID = "group.com.d2m.app"

/**
 * Routes the SQLite file into the shared App Group container instead of
 * this process's own private sandbox -- required so the Notification
 * Service Extension (NotificationService.swift, a SEPARATE OS process with
 * its own private sandbox by default) can open the exact same database the
 * main app writes to. Falls back to the extension-less default location
 * (this target's own container) if the App Group entitlement isn't
 * present/configured yet, so a checkout that hasn't done the Xcode-side App
 * Group setup still runs correctly for everything except the extension's
 * rich-content decrypt (which itself already falls back to generic
 * title/body copy on any failure -- see NotificationService.swift).
 *
 * LEAST CERTAIN PART OF THIS FILE: `DatabaseConfiguration.Extended(basePath = ...)`
 * is written from SQLDelight/SQLiter's documented app-group/custom-location
 * pattern from memory, not compiled -- verify `co.touchlab.sqliter.
 * DatabaseConfiguration.Extended`'s exact constructor against whatever
 * SQLDelight 2.0.2 actually ships if Xcode reports a mismatch here.
 */
actual fun createSqlDriver(): SqlDriver {
    val basePath = NSFileManager.defaultManager
        .containerURLForSecurityApplicationGroupIdentifier(D2M_APP_GROUP_ID)
        ?.path

    return NativeSqliteDriver(
        schema = D2MDatabase.Schema,
        name = "d2m_chat.db",
        onConfiguration = { config ->
            if (basePath != null) {
                config.copy(extendedConfig = DatabaseConfiguration.Extended(basePath = basePath))
            } else {
                config
            }
        },
    )
}
