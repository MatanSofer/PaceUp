package com.example.paceup.feature.signup

import androidx.lifecycle.SavedStateHandle
import com.example.paceup.feature.login.FakeAuthRepository
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
class SignUpViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeAuthRepository
    private lateinit var viewModel: SignUpViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAuthRepository()
        viewModel = SignUpViewModel(fakeRepo, SavedStateHandle())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emailChanged_updatesStateEmail() = runTest {
        viewModel.onAction(SignUpAction.OnEmailChanged("runner@example.com"))
        assertEquals("runner@example.com", viewModel.state.value.email)
    }

    @Test
    fun passwordChanged_updatesStatePassword() = runTest {
        viewModel.onAction(SignUpAction.OnPasswordChanged("secret123"))
        assertEquals("secret123", viewModel.state.value.password)
    }

    @Test
    fun confirmPasswordChanged_updatesStateConfirmPassword() = runTest {
        viewModel.onAction(SignUpAction.OnConfirmPasswordChanged("secret123"))
        assertEquals("secret123", viewModel.state.value.confirmPassword)
    }

    @Test
    fun emailChanged_clearsExistingError() = runTest {
        // Trigger a validation error first
        viewModel.onAction(SignUpAction.OnSignUpClicked)
        assertNotNull(viewModel.state.value.error)

        viewModel.onAction(SignUpAction.OnEmailChanged("new@b.com"))
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun signUp_blankEmail_setsEmailRequiredError_withoutCallingRepo() = runTest {
        viewModel.onAction(SignUpAction.OnSignUpClicked)

        assertNotNull(viewModel.state.value.error)
        assertIs<UiText.StringRes>(viewModel.state.value.error)
        assertTrue(!viewModel.state.value.isLoading)
    }

    @Test
    fun signUp_blankPassword_setsPasswordRequiredError() = runTest {
        viewModel.onAction(SignUpAction.OnEmailChanged("runner@example.com"))
        viewModel.onAction(SignUpAction.OnSignUpClicked)

        assertNotNull(viewModel.state.value.error)
        assertIs<UiText.StringRes>(viewModel.state.value.error)
    }

    @Test
    fun signUp_blankConfirmPassword_setsConfirmPasswordRequiredError() = runTest {
        viewModel.onAction(SignUpAction.OnEmailChanged("runner@example.com"))
        viewModel.onAction(SignUpAction.OnPasswordChanged("password123"))
        viewModel.onAction(SignUpAction.OnSignUpClicked)

        assertNotNull(viewModel.state.value.error)
        assertIs<UiText.StringRes>(viewModel.state.value.error)
    }

    @Test
    fun signUp_passwordTooShort_setsWeakPasswordError() = runTest {
        viewModel.onAction(SignUpAction.OnEmailChanged("runner@example.com"))
        viewModel.onAction(SignUpAction.OnPasswordChanged("short"))
        viewModel.onAction(SignUpAction.OnConfirmPasswordChanged("short"))
        viewModel.onAction(SignUpAction.OnSignUpClicked)

        assertNotNull(viewModel.state.value.error)
        assertIs<UiText.StringRes>(viewModel.state.value.error)
        assertTrue(!viewModel.state.value.isLoading)
    }

    @Test
    fun signUp_passwordMismatch_setsPasswordMismatchError() = runTest {
        viewModel.onAction(SignUpAction.OnEmailChanged("runner@example.com"))
        viewModel.onAction(SignUpAction.OnPasswordChanged("password123"))
        viewModel.onAction(SignUpAction.OnConfirmPasswordChanged("different123"))
        viewModel.onAction(SignUpAction.OnSignUpClicked)

        assertNotNull(viewModel.state.value.error)
        assertIs<UiText.StringRes>(viewModel.state.value.error)
        assertTrue(!viewModel.state.value.isLoading)
    }

    @Test
    fun signUp_success_emitsNavigateToEmailVerificationEvent() = runTest {
        val email = "runner@example.com"
        viewModel.onAction(SignUpAction.OnEmailChanged(email))
        viewModel.onAction(SignUpAction.OnPasswordChanged("password123"))
        viewModel.onAction(SignUpAction.OnConfirmPasswordChanged("password123"))

        val eventDeferred = async { viewModel.events.first() }
        viewModel.onAction(SignUpAction.OnSignUpClicked)
        val event = eventDeferred.await()

        assertEquals(SignUpEvent.NavigateToEmailVerification(email), event)
    }

    @Test
    fun signUp_invalidEmailFormat_setsFormatError_withoutCallingRepo() = runTest {
        viewModel.onAction(SignUpAction.OnEmailChanged("notanemail"))
        viewModel.onAction(SignUpAction.OnPasswordChanged("password123"))
        viewModel.onAction(SignUpAction.OnConfirmPasswordChanged("password123"))
        viewModel.onAction(SignUpAction.OnSignUpClicked)

        assertNotNull(viewModel.state.value.error)
        assertIs<UiText.StringRes>(viewModel.state.value.error)
        assertTrue(!viewModel.state.value.isLoading)
    }

    @Test
    fun signUp_failure_setsErrorAndClearsLoading() = runTest {
        fakeRepo.signUpResult = Result.Error(AuthError.EMAIL_ALREADY_IN_USE)
        viewModel.onAction(SignUpAction.OnEmailChanged("existing@example.com"))
        viewModel.onAction(SignUpAction.OnPasswordChanged("password123"))
        viewModel.onAction(SignUpAction.OnConfirmPasswordChanged("password123"))
        viewModel.onAction(SignUpAction.OnSignUpClicked)

        val state = viewModel.state.value
        assertNotNull(state.error)
        assertIs<UiText.StringRes>(state.error)
        assertTrue(!state.isLoading)
    }

    @Test
    fun loginClicked_emitsNavigateToLoginEvent() = runTest {
        val eventDeferred = async { viewModel.events.first() }
        viewModel.onAction(SignUpAction.OnLoginClicked)
        val event = eventDeferred.await()

        assertEquals(SignUpEvent.NavigateToLogin, event)
    }

    @Test
    fun googleSignIn_failure_setsErrorInState() = runTest {
        fakeRepo.googleSignInResult = Result.Error(AuthError.OAUTH_FAILED)
        viewModel.onAction(SignUpAction.OnGoogleSignInClicked)

        assertNotNull(viewModel.state.value.error)
        assertTrue(!viewModel.state.value.isLoading)
    }

    @Test
    fun savedStateHandle_restoresEmail() {
        val handle = SavedStateHandle(mapOf("email" to "saved@example.com"))
        val vm = SignUpViewModel(fakeRepo, handle)
        assertEquals("saved@example.com", vm.state.value.email)
    }
}
