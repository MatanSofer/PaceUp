package com.example.paceup.shared.notifications

import com.example.paceup.shared.network.result.EmptyResult
import com.example.paceup.shared.network.error.AppError

/** Contract for push-notification token management. Implementation is shared; token source is platform-specific. */
interface NotificationRepository {
    /** Saves [token] (FCM on Android, APNs on iOS) to the current user's row in Supabase. */
    suspend fun registerPushToken(token: String): EmptyResult<AppError>

    /** Clears the push token for the current user. Called before sign-out and on account delete. */
    suspend fun deletePushToken(): EmptyResult<AppError>
}
