package com.example.paceup.shared.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

// iOS: implemented here — verify on Mac before PR
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(PaceUpDatabase.Schema, "paceup.db")
}
