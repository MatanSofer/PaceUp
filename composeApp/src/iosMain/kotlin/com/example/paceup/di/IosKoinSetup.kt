package com.example.paceup.di

import com.example.paceup.platform.ImagePicker
import com.example.paceup.platform.LocationPermissionRequester
import com.example.paceup.platform.NotificationPermissionPrefs
import com.example.paceup.platform.NotificationPermissionRequester
import com.example.paceup.platform.OnboardingPrefs
import com.example.paceup.shared.auth.strava.KtorStravaAuthRepository
import com.example.paceup.shared.auth.strava.OAuthBrowserLauncher
import com.example.paceup.shared.auth.strava.StravaAuthRepository
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Called from Swift entry point to initialize Koin on iOS.
 * iOS: call `IosKoinSetupKt.doInitKoin(clientId:clientSecret:)` from iOSApp — verify on Mac before PR.
 *
 * @param stravaClientId Strava OAuth client ID — read from Info.plist in Swift.
 * @param stravaClientSecret Strava OAuth client secret — read from Info.plist in Swift.
 */
fun initKoin(stravaClientId: String, stravaClientSecret: String) {
    val iosModule = module {
        single { OAuthBrowserLauncher() }
        single<StravaAuthRepository> {
            KtorStravaAuthRepository(
                clientId = stravaClientId,
                clientSecret = stravaClientSecret
            )
        }
        single { LocationPermissionRequester() }
        single { NotificationPermissionRequester() }
        single { NotificationPermissionPrefs() }
        single { OnboardingPrefs() }
        single { ImagePicker() }
    }
    startKoin {
        modules(sharedModules + iosModule)
    }
}
