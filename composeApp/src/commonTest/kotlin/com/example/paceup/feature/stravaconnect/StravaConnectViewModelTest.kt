package com.example.paceup.feature.stravaconnect

import com.example.paceup.shared.auth.strava.StravaActivityMetrics
import com.example.paceup.shared.auth.strava.StravaOAuthCodeStore
import com.example.paceup.shared.auth.strava.StravaToken
import com.example.paceup.shared.network.error.AuthError
import com.example.paceup.shared.network.error.NetworkError
import com.example.paceup.shared.network.result.Result
import com.example.paceup.ui.UiText
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StravaConnectViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeStravaAuthRepository
    private lateinit var viewModel: StravaConnectViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        StravaOAuthCodeStore.consumeCode() // clear any leftover state
        fakeRepo = FakeStravaAuthRepository()
        viewModel = StravaConnectViewModel(fakeRepo)
    }

    @AfterTest
    fun tearDown() {
        // Cancel the viewModelScope to stop the long-running StateFlow collector in init
        viewModel.viewModelScope.cancel()
        StravaOAuthCodeStore.consumeCode()
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isIdle() {
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isConnected)
        assertNull(state.paceZone)
        assertNull(state.avgPaceDisplay)
        assertNull(state.error)
    }

    @Test
    fun connectStravaClicked_emitsLaunchOAuthUrlEvent() = runTest {
        val eventDeferred = async { viewModel.events.first() }
        viewModel.onAction(StravaConnectAction.OnConnectStravaClicked)
        val event = eventDeferred.await()

        assertIs<StravaConnectEvent.LaunchOAuthUrl>(event)
        assertEquals(fakeRepo.oauthUrl, (event as StravaConnectEvent.LaunchOAuthUrl).url)
    }

    @Test
    fun skipClicked_emitsNavigateToLocationPermission() = runTest {
        val eventDeferred = async { viewModel.events.first() }
        viewModel.onAction(StravaConnectAction.OnSkipClicked)
        val event = eventDeferred.await()

        assertEquals(StravaConnectEvent.NavigateToLocationPermission, event)
    }

    @Test
    fun continueClicked_emitsNavigateToLocationPermission() = runTest {
        val eventDeferred = async { viewModel.events.first() }
        viewModel.onAction(StravaConnectAction.OnContinueClicked)
        val event = eventDeferred.await()

        assertEquals(StravaConnectEvent.NavigateToLocationPermission, event)
    }

    @Test
    fun oauthCode_success_withActivities_setsConnectedStateWithPaceZone() = runTest {
        StravaOAuthCodeStore.submitCode("valid-code")
        // Wait for the OAuth flow to complete (loading clears, connected is set)
        val state = viewModel.state.first { it.isConnected || it.error != null }

        assertTrue(state.isConnected)
        assertFalse(state.isLoading)
        assertNotNull(state.paceZone)
        assertNotNull(state.avgPaceDisplay)
        assertNull(state.error)
    }

    @Test
    fun oauthCode_success_noActivities_setsConnectedStateWithNullZone() = runTest {
        fakeRepo.activitiesResult = Result.Success(emptyList())
        StravaOAuthCodeStore.submitCode("valid-code")
        val state = viewModel.state.first { it.isConnected || it.error != null }

        assertTrue(state.isConnected)
        assertFalse(state.isLoading)
        assertNull(state.paceZone)
        assertNull(state.avgPaceDisplay)
        assertNull(state.error)
    }

    @Test
    fun oauthCode_tokenExchangeFailed_setsErrorAndNotConnected() = runTest {
        fakeRepo.tokenResult = Result.Error(AuthError.OAUTH_FAILED)
        StravaOAuthCodeStore.submitCode("bad-code")
        val state = viewModel.state.first { it.error != null || it.isConnected }

        assertFalse(state.isConnected)
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertIs<UiText.StringRes>(state.error)
    }

    @Test
    fun oauthCode_activitiesFetchFailed_setsErrorAndNotConnected() = runTest {
        fakeRepo.activitiesResult = Result.Error(NetworkError.UNKNOWN)
        StravaOAuthCodeStore.submitCode("valid-code")
        val state = viewModel.state.first { it.error != null || it.isConnected }

        assertFalse(state.isConnected)
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertIs<UiText.StringRes>(state.error)
    }

    @Test
    fun retryClicked_clearsErrorAndLaunchesOAuth() = runTest {
        // Trigger an initial failure
        fakeRepo.tokenResult = Result.Error(AuthError.OAUTH_FAILED)
        StravaOAuthCodeStore.submitCode("bad-code")
        viewModel.state.first { it.error != null }

        // Restore success and retry
        fakeRepo.tokenResult = Result.Success(
            StravaToken(
                accessToken = "token",
                refreshToken = "refresh",
                expiresAt = Long.MAX_VALUE,
                athleteId = 1L
            )
        )
        val eventDeferred = async { viewModel.events.first() }
        viewModel.onAction(StravaConnectAction.OnRetryClicked)
        val event = eventDeferred.await()

        assertNull(viewModel.state.value.error)
        assertIs<StravaConnectEvent.LaunchOAuthUrl>(event)
    }

    @Test
    fun oauthCode_consumedAfterProcessing_storeIsEmpty() = runTest {
        StravaOAuthCodeStore.submitCode("valid-code")
        viewModel.state.first { it.isConnected || it.error != null }

        // After ViewModel processes the code, the store must be cleared
        assertNull(StravaOAuthCodeStore.pendingCode.value)
    }

    @Test
    fun avgPaceDisplay_formatIsCorrect() = runTest {
        // 300 sec/km = exactly 5:00 /km
        fakeRepo.activitiesResult = Result.Success(
            listOf(StravaActivityMetrics(avgPaceSecondsPerKm = 300, distanceKm = 10f, date = "2025-01-01"))
        )
        StravaOAuthCodeStore.submitCode("code")
        val state = viewModel.state.first { it.isConnected || it.error != null }

        assertEquals("5:00 /km", state.avgPaceDisplay)
    }
}
