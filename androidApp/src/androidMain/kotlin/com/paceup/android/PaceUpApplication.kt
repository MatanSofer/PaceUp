package com.paceup.android

import android.app.Application
import com.example.paceup.shared.network.logger.AppLogger

/** Android [Application] class. Koin initialization will be added here in Task 0.5. */
class PaceUpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.setDebugEnabled(BuildConfig.DEBUG)
        // TODO(paceup): initialize Koin here in Task 0.5
    }
}
