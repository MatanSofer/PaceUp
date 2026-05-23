package com.example.paceup.shared.database

import app.cash.sqldelight.db.SqlDriver

/** Platform-specific factory that creates the SQLDelight driver for PaceUpDatabase. */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
