package com.example.paceup.shared.database

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/** Android-specific Koin binding for [DatabaseDriverFactory]. Include alongside [databaseModule]. */
val androidDatabaseModule: Module = module {
    single { DatabaseDriverFactory(androidContext()) }
}
