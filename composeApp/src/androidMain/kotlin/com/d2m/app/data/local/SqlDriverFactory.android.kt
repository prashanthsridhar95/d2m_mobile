package com.d2m.app.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.d2m.app.data.session.AndroidSettingsContextHolder

// Reuses AndroidSettingsContextHolder (set once in D2MApplication.onCreate,
// same as createSettings() already does) rather than adding a third
// Android-Context holder for what's still just "give me the app context" --
// see that object's doc comment in Settings.android.kt.
actual fun createSqlDriver(): SqlDriver =
    AndroidSqliteDriver(D2MDatabase.Schema, AndroidSettingsContextHolder.appContext, "d2m_chat.db")
