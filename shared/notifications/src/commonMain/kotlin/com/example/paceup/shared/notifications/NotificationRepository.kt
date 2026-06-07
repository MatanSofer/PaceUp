package com.example.paceup.shared.notifications

import com.example.paceup.shared.network.result.EmptyResult
import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.result.Result

/** Contract for push-notification token management and user preferences. */
interface NotificationRepository {
    /** Saves [token] (FCM on Android, APNs on iOS) to the current user's row in Supabase. */
    suspend fun registerPushToken(token: String): EmptyResult<AppError>

    /** Clears the push token for the current user. Called before sign-out and on account delete. */
    suspend fun deletePushToken(): EmptyResult<AppError>

    /** Loads the current user's notification preferences from Supabase. */
    suspend fun getPreferences(): Result<NotificationPreferences, AppError>

    /** Persists updated notification preferences for the current user. */
    suspend fun updatePreferences(prefs: NotificationPreferences): EmptyResult<AppError>
}
