package com.paceup.android

import android.app.Application
import com.example.paceup.di.sharedModules
import com.example.paceup.platform.LocationPermissionRequester
import com.example.paceup.platform.ImagePicker
import com.example.paceup.platform.NotificationPermissionPrefs
import com.example.paceup.platform.NotificationPermissionRequester
import com.example.paceup.shared.auth.di.androidAuthModule
import com.example.paceup.shared.auth.strava.KtorStravaAuthRepository
import com.example.paceup.shared.auth.strava.StravaAuthRepository
import com.example.paceup.shared.database.androidDatabaseModule
import com.example.paceup.shared.network.logger.AppLogger
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module

/** Android [Application] class. Initializes logging and Koin DI. */
class PaceUpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.setDebugEnabled(BuildConfig.DEBUG)

        // Credentials come from local.properties via BuildConfig — never hardcoded
        val androidModule = module {
            single<StravaAuthRepository> {
                KtorStravaAuthRepository(
                    clientId = BuildConfig.STRAVA_CLIENT_ID,
                    clientSecret = BuildConfig.STRAVA_CLIENT_SECRET
                )
            }
            single { LocationPermissionRequester(get()) }
            single { NotificationPermissionRequester(get()) }
            single { NotificationPermissionPrefs(get()) }
            single { ImagePicker(get()) }
        }

        startKoin {
            androidLogger()
            androidContext(this@PaceUpApplication)
            modules(sharedModules + androidDatabaseModule + androidAuthModule + androidModule)
        }
    }
}
