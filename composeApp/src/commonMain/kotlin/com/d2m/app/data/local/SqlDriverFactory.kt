package com.d2m.app.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform SQLite driver for [D2MDatabase] (declared in
 * composeApp/build.gradle.kts's `sqldelight { databases { create("D2MDatabase") ... } }`,
 * schema in ChatDatabase.sq -- see that file's doc comment for why this
 * exists: local persistent chat cache, task #57/#56). expect/actual per this
 * codebase's established pattern (Settings.kt/createSettings(), WebRtcEngine.kt) --
 * Android gets a real on-disk SQLite driver, iOS gets SQLDelight's native
 * driver (also real, unlike this app's other iOS actuals such as
 * CryptoProvider.ios.kt -- SQLDelight's native driver needs no external SDK
 * or unverified crypto work, just the sqldelight-native-driver dependency
 * already declared in composeApp/build.gradle.kts's iosMain sourceSet).
 */
expect fun createSqlDriver(): SqlDriver
