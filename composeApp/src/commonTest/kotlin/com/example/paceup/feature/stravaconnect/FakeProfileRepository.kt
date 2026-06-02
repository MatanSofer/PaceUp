package com.example.paceup.feature.stravaconnect

import com.example.paceup.shared.auth.profile.ProfileError
import com.example.paceup.shared.auth.profile.ProfileRepository
import com.example.paceup.shared.auth.strava.StravaToken
import com.example.paceup.shared.network.result.EmptyResult
import com.example.paceup.shared.network.result.Result

class FakeProfileRepository : ProfileRepository {
    var saveProfileResult: EmptyResult<ProfileError> = Result.Success(Unit)
    var saveStravaConnectionResult: EmptyResult<ProfileError> = Result.Success(Unit)
    var saveStravaConnectionCallCount = 0

    override suspend fun saveProfile(
        displayName: String,
        avatarBytes: ByteArray?,
    ): EmptyResult<ProfileError> = saveProfileResult

    override suspend fun saveStravaConnection(
        token: StravaToken,
        isVerified: Boolean,
        paceZone: String?,
        avgPaceSeconds: Int?,
        weeklyMileageAvgKm: Float?,
    ): EmptyResult<ProfileError> {
        saveStravaConnectionCallCount++
        return saveStravaConnectionResult
    }
}
