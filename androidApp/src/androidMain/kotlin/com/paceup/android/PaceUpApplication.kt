package com.paceup.android

import android.app.Application
import com.example.paceup.di.sharedModules
import com.example.paceup.shared.auth.di.androidAuthModule
import com.example.paceup.shared.database.androidDatabaseModule
import com.example.paceup.shared.network.logger.AppLogger
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/** Android [Application] class. Initializes logging and Koin DI. */
class PaceUpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.setDebugEnabled(BuildConfig.DEBUG)
        startKoin {
            androidLogger()
            androidContext(this@PaceUpApplication)
            modules(sharedModules + androidDatabaseModule + androidAuthModule)
        }
    }
}
