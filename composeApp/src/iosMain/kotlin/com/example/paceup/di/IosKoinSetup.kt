package com.example.paceup.di

import org.koin.core.context.startKoin

/**
 * Called from Swift AppDelegate to initialize Koin on iOS.
 * iOS: call `IosKoinSetupKt.doInitKoin()` from AppDelegate — verify on Mac before PR.
 */
fun initKoin() {
    startKoin {
        modules(sharedModules)
    }
}
