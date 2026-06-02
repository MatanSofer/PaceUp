package com.example.paceup.feature.rundetail

import com.example.paceup.shared.network.error.AppError
import com.example.paceup.shared.network.error.RunError
import com.example.paceup.shared.network.result.Result
import com.example.paceup.shared.runmatching.domain.CreateRunParams
import com.example.paceup.shared.runmatching.domain.Run
import com.example.paceup.shared.runmatching.domain.RunFilters
import com.example.paceup.shared.runmatching.domain.RunMode
import com.example.paceup.shared.runmatching.domain.RunParticipant
import com.example.paceup.shared.runmatching.domain.RunRepository
import com.example.paceup.shared.runmatching.domain.RunStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

val fakeRun = Run(
    id = "run-1",
    creatorId = "creator-1",
    title = "Morning tempo",
    description = "A brisk morning run along the promenade.",
    mode = RunMode.TEMPO,
    status = RunStatus.OPEN,
    scheduledAt = "2026-06-01T07:00:00Z",
    meetingLat = 32.08,
    meetingLng = 34.78,
    meetingAddress = "Gordon Beach, Tel Aviv",
    city = "Tel Aviv",
    distanceKm = 8f,
    durationMin = null,
    paceMinSec = 300,
    paceMaxSec = 330,
    maxParticipants = 10,
    verifiedOnly = true,
    joinMode = "open",
    isRecurring = false,
    createdAt = "2026-05-25T12:00:00Z",
)

val fakeParticipants = listOf(
    RunParticipant(
        userId = "user-2",
        displayName = "Maya Cohen",
        avatarUrl = null,
        paceZone = "C",
        showUpRate = 0.92f,
        status = "accepted",
    ),
    RunParticipant(
        userId = "user-3",
        displayName = "Dan Levi",
        avatarUrl = null,
        paceZone = "B",
        showUpRate = 0.68f,
        status = "accepted",
    ),
)

class FakeRunRepository : RunRepository {

    var runByIdResult: Result<Run, AppError> = Result.Success(fakeRun)
    var participantsResult: Result<List<RunParticipant>, AppError> = Result.Success(fakeParticipants)
    var joinRunResult: Result<Unit, AppError> = Result.Success(Unit)
    var requestToJoinResult: Result<Unit, AppError> = Result.Success(Unit)
    var acceptParticipantResult: Result<Unit, AppError> = Result.Success(Unit)
    var declineParticipantResult: Result<Unit, AppError> = Result.Success(Unit)
    var cancelParticipationResult: Result<Unit, AppError> = Result.Success(Unit)

    override suspend fun getRunById(runId: String): Result<Run, AppError> = runByIdResult

    override suspend fun getRunParticipants(runId: String): Result<List<RunParticipant>, AppError> =
        participantsResult

    override suspend fun createRun(params: CreateRunParams): Result<Run, AppError> =
        Result.Error(RunError.NETWORK_ERROR)

    override suspend fun getRunsNearLocation(
        lat: Double,
        lng: Double,
        radiusKm: Double,
        filters: RunFilters,
    ): Result<List<Run>, AppError> = Result.Success(emptyList())

    override suspend fun getRunsForUser(userId: String): Result<List<Run>, AppError> =
        Result.Success(emptyList())

    override suspend fun searchRuns(
        query: String,
        filters: RunFilters,
    ): Result<List<Run>, AppError> = Result.Success(emptyList())

    override fun observeRunStatus(runId: String): Flow<RunStatus> = emptyFlow()

    override suspend fun joinRun(runId: String): Result<Unit, AppError> = joinRunResult

    override suspend fun requestToJoin(runId: String): Result<Unit, AppError> = requestToJoinResult

    override suspend fun acceptParticipant(runId: String, userId: String): Result<Unit, AppError> =
        acceptParticipantResult

    override suspend fun declineParticipant(runId: String, userId: String): Result<Unit, AppError> =
        declineParticipantResult

    override suspend fun cancelParticipation(runId: String): Result<Unit, AppError> =
        cancelParticipationResult

    var observeParticipantsFlow: Flow<List<RunParticipant>> = flowOf(fakeParticipants)

    override fun observeParticipants(runId: String): Flow<List<RunParticipant>> =
        observeParticipantsFlow

    var cancelRunResult: Result<Unit, AppError> = Result.Success(Unit)

    override suspend fun cancelRun(runId: String, reason: String): Result<Unit, AppError> =
        cancelRunResult
}
