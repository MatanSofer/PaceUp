package com.example.paceup.shared.database

import org.koin.core.module.Module
import org.koin.dsl.module

/** Shared Koin module. Requires a platform-specific [DatabaseDriverFactory] binding to be present. */
val databaseModule: Module = module {
    single { get<DatabaseDriverFactory>().createDriver() }
    single { PaceUpDatabase(get()) }
}
