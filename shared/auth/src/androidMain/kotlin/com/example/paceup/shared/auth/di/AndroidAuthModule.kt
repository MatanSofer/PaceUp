package com.example.paceup.shared.auth.di

import com.example.paceup.shared.auth.strava.OAuthBrowserLauncher
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/** Android-specific auth DI bindings. Requires [androidContext] to be set in Koin. */
val androidAuthModule: Module = module {
    single { OAuthBrowserLauncher(androidContext()) }
}
