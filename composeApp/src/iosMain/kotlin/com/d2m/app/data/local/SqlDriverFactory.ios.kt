package com.d2m.app.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual fun createSqlDriver(): SqlDriver = NativeSqliteDriver(D2MDatabase.Schema, "d2m_chat.db")
