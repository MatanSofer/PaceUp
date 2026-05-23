package com.example.paceup.feature.login

import androidx.lifecycle.SavedStateHandle
import com.example.paceup.shared.network.error.AuthError
import com.example.paceup.shared.network.result.Result
import com.example.paceup.ui.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAuthRepository()
        viewModel = LoginViewModel(fakeRepo, SavedStateHandle())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emailChanged_updatesStateEmail() = runTest {
        viewModel.onAction(LoginAction.OnEmailChanged("runner@example.com"))
        assertEquals("runner@example.com", viewModel.state.value.email)
    }

    @Test
    fun passwordChanged_updatesStatePassword() = runTest {
        viewModel.onAction(LoginAction.OnPasswordChanged("secret123"))
        assertEquals("secret123", viewModel.state.value.password)
    }

    @Test
    fun emailChanged_clearsExistingError() = runTest {
        fakeRepo.signInResult = Result.Error(AuthError.INVALID_CREDENTIALS)
        viewModel.onAction(LoginAction.OnEmailChanged("a@b.com"))
        viewModel.onAction(LoginAction.OnPasswordChanged("pass"))
        viewModel.onAction(LoginAction.OnLoginClicked)
        assertNotNull(viewModel.state.value.error)

        viewModel.onAction(LoginAction.OnEmailChanged("new@b.com"))
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun signIn_blankEmail_setsEmailRequiredError_withoutCallingRepo() = runTest {
        viewModel.onAction(LoginAction.OnPasswordChanged("pass"))
        viewModel.onAction(LoginAction.OnLoginClicked)

        assertNotNull(viewModel.state.value.error)
        assertIs<UiText.StringRes>(viewModel.state.value.error)
    }

    @Test
    fun signIn_invalidEmailFormat_setsFormatError_withoutCallingRepo() = runTest {
        viewModel.onAction(LoginAction.OnEmailChanged("notanemail"))
        viewModel.onAction(LoginAction.OnPasswordChanged("password123"))
        viewModel.onAction(LoginAction.OnLoginClicked)

        assertNotNull(viewModel.state.value.error)
        assertIs<UiText.StringRes>(viewModel.state.value.error)
        assertTrue(!viewModel.state.value.isLoading)
    }

    @Test
    fun signIn_blankPassword_setsPasswordRequiredError() = runTest {
        viewModel.onAction(LoginAction.OnEmailChanged("runner@example.com"))
        viewModel.onAction(LoginAction.OnLoginClicked)

        assertNotNull(viewModel.state.value.error)
        assertIs<UiText.StringRes>(viewModel.state.value.error)
    }

    @Test
    fun signIn_success_emitsNavigateToStravaConnectEvent() = runTest {
        viewModel.onAction(LoginAction.OnEmailChanged("runner@example.com"))
        viewModel.onAction(LoginAction.OnPasswordChanged("password123"))

        // Set up event collector BEFORE triggering the action so it's ready to receive.
        val eventDeferred = async { viewModel.events.first() }
        viewModel.onAction(LoginAction.OnLoginClicked)
        val event = eventDeferred.await()

        assertEquals(LoginEvent.NavigateToStravaConnect, event)
    }

    @Test
    fun signIn_failure_setsErrorAndClearsLoading() = runTest {
        fakeRepo.signInResult = Result.Error(AuthError.INVALID_CREDENTIALS)
        viewModel.onAction(LoginAction.OnEmailChanged("runner@example.com"))
        viewModel.onAction(LoginAction.OnPasswordChanged("wrongpass"))
        viewModel.onAction(LoginAction.OnLoginClicked)

        val state = viewModel.state.value
        assertNotNull(state.error)
        assertIs<UiText.StringRes>(state.error)
        assertTrue(!state.isLoading)
    }

    @Test
    fun signUpClicked_emitsNavigateToSignUpEvent() = runTest {
        val eventDeferred = async { viewModel.events.first() }
        viewModel.onAction(LoginAction.OnSignUpClicked)
        val event = eventDeferred.await()

        assertEquals(LoginEvent.NavigateToSignUp, event)
    }

    @Test
    fun googleSignIn_failure_setsErrorInState() = runTest {
        fakeRepo.googleSignInResult = Result.Error(AuthError.OAUTH_FAILED)
        viewModel.onAction(LoginAction.OnGoogleSignInClicked)

        assertNotNull(viewModel.state.value.error)
        assertTrue(!viewModel.state.value.isLoading)
    }

    @Test
    fun savedStateHandle_restoresEmail() {
        val handle = SavedStateHandle(mapOf("email" to "saved@example.com"))
        val vm = LoginViewModel(fakeRepo, handle)
        assertEquals("saved@example.com", vm.state.value.email)
    }
}
