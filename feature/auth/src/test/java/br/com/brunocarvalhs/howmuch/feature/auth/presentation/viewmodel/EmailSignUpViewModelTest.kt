package br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameError
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthError
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthException
import br.com.brunocarvalhs.howmuch.feature.auth.domain.repository.EmailAuthRepository
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.EmailFieldError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.PasswordFieldError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.SignUpField
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Spec EPA-02 (sign-up form) and EPA-13 (name rules applied on the form). */
@Suppress("TooManyFunctions")
class EmailSignUpViewModelTest {

    private val repository = mockk<EmailAuthRepository> {
        coEvery { signUp(any(), any()) } returns Result.success(Unit)
    }
    private val authService = mockk<AuthService> {
        coEvery { updateDisplayName(any()) } returns Result.success(Unit)
    }
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = EmailSignUpViewModel(repository, authService, analyticsTracker)

    private fun EmailSignUpViewModel.fill(
        given: String = "Ana",
        family: String = "Silva",
        email: String = "ana@test.com",
        password: String = "12345678",
    ) = apply {
        intent.onGivenNameChange(given)
        intent.onFamilyNameChange(family)
        intent.onEmailChange(email)
        intent.onPasswordChange(password)
    }

    @Test
    fun `submit is disabled until every field is filled and valid`() {
        val vm = viewModel()
        assertFalse(vm.uiState.value.isSubmitEnabled)

        vm.fill(password = "1234567")
        assertFalse(vm.uiState.value.isSubmitEnabled)

        vm.intent.onPasswordChange("12345678")
        assertTrue(vm.uiState.value.isSubmitEnabled)

        vm.intent.onFamilyNameChange("")
        assertFalse(vm.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `errors only appear after leaving the field, not while typing`() {
        val vm = viewModel()

        vm.intent.onGivenNameChange("aaaa")
        assertNull(vm.uiState.value.givenNameError)

        vm.intent.onFieldFocusLost(SignUpField.GIVEN_NAME)
        assertEquals(PersonNameError.NOT_A_NAME, vm.uiState.value.givenNameError)
    }

    @Test
    fun `typing in a field clears its error`() {
        val vm = viewModel()
        vm.intent.onGivenNameChange("aaaa")
        vm.intent.onFieldFocusLost(SignUpField.GIVEN_NAME)

        vm.intent.onGivenNameChange("Ana")

        assertNull(vm.uiState.value.givenNameError)
    }

    @Test
    fun `leaving each field validates only that field`() {
        val vm = viewModel()
        vm.fill(given = "Ana", family = "", email = "ana@", password = "123")

        vm.intent.onFieldFocusLost(SignUpField.FAMILY_NAME)
        assertEquals(PersonNameError.EMPTY, vm.uiState.value.familyNameError)
        assertNull(vm.uiState.value.emailError)

        vm.intent.onFieldFocusLost(SignUpField.EMAIL)
        assertEquals(EmailFieldError.INVALID, vm.uiState.value.emailError)

        vm.intent.onFieldFocusLost(SignUpField.PASSWORD)
        assertEquals(PasswordFieldError.TOO_SHORT, vm.uiState.value.passwordError)
        assertNull(vm.uiState.value.givenNameError)
    }

    @Test
    fun `name validation failures are tracked by category only, never the typed text`() {
        val vm = viewModel()
        vm.intent.onGivenNameChange("maria@gmail.com")

        vm.intent.onFieldFocusLost(SignUpField.GIVEN_NAME)

        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.AUTH_NAME_VALIDATION_FAILED,
                mapOf(AnalyticsParams.REASON to "contact", AnalyticsParams.SOURCE to "sign_up")
            )
        }
    }

    @Test
    fun `successful sign up creates the account and stores the normalized full name`() {
        val vm = viewModel().fill(given = "  ana ", family = "da   silva", email = " ana@test.com ")

        vm.intent.onSubmit()

        coVerify { repository.signUp("ana@test.com", "12345678") }
        coVerify { authService.updateDisplayName("Ana Da Silva") }
        assertTrue(vm.uiState.value.isSuccess)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `a failure saving the name still finishes sign up, the required-name step catches it`() {
        coEvery { authService.updateDisplayName(any()) } returns Result.failure(IllegalStateException())
        val vm = viewModel().fill()

        vm.intent.onSubmit()

        assertTrue(vm.uiState.value.isSuccess)
    }

    @Test
    fun `e-mail already in use is shown on the e-mail field and keeps what was typed`() {
        coEvery { repository.signUp(any(), any()) } returns
            Result.failure(EmailAuthException(EmailAuthError.EMAIL_ALREADY_IN_USE))
        val vm = viewModel().fill()

        vm.intent.onSubmit()

        val state = vm.uiState.value
        assertEquals(EmailFieldError.ALREADY_IN_USE, state.emailError)
        assertEquals("ana@test.com", state.email)
        assertEquals("Ana", state.givenName)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        coVerify(exactly = 0) { authService.updateDisplayName(any()) }
    }

    @Test
    fun `server-side invalid e-mail and weak password land on their fields`() {
        coEvery { repository.signUp(any(), any()) } returns
            Result.failure(EmailAuthException(EmailAuthError.INVALID_EMAIL))
        val vm = viewModel().fill()
        vm.intent.onSubmit()
        assertEquals(EmailFieldError.INVALID, vm.uiState.value.emailError)

        coEvery { repository.signUp(any(), any()) } returns
            Result.failure(EmailAuthException(EmailAuthError.WEAK_PASSWORD))
        vm.intent.onEmailChange("ana@test.com")
        vm.intent.onSubmit()
        assertEquals(PasswordFieldError.TOO_SHORT, vm.uiState.value.passwordError)
    }

    @Test
    fun `network failure shows a form error, keeps the form and can be dismissed`() {
        coEvery { repository.signUp(any(), any()) } returns
            Result.failure(EmailAuthException(EmailAuthError.NETWORK))
        val vm = viewModel().fill()

        vm.intent.onSubmit()

        assertEquals(EmailAuthError.NETWORK, vm.uiState.value.formError)
        assertEquals("12345678", vm.uiState.value.password)
        assertTrue(vm.uiState.value.isSubmitEnabled)
        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.AUTH_SIGN_UP_FAILED,
                mapOf(AnalyticsParams.REASON to "NETWORK", AnalyticsParams.SOURCE to "email")
            )
        }

        vm.intent.onFormErrorDismissed()
        assertNull(vm.uiState.value.formError)
    }

    @Test
    fun `a second tap while signing up sends a single request`() {
        val pending = CompletableDeferred<Result<Unit>>()
        coEvery { repository.signUp(any(), any()) } coAnswers { pending.await() }
        val vm = viewModel().fill()

        vm.intent.onSubmit()
        assertTrue(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.isSubmitEnabled)
        vm.intent.onSubmit()
        pending.complete(Result.success(Unit))

        coVerify(exactly = 1) { repository.signUp(any(), any()) }
    }

    @Test
    fun `submitting an invalid form shows every error and sends nothing`() {
        val vm = viewModel()

        vm.intent.onSubmit()

        val state = vm.uiState.value
        assertEquals(PersonNameError.EMPTY, state.givenNameError)
        assertEquals(PersonNameError.EMPTY, state.familyNameError)
        assertEquals(EmailFieldError.EMPTY, state.emailError)
        assertEquals(PasswordFieldError.EMPTY, state.passwordError)
        coVerify(exactly = 0) { repository.signUp(any(), any()) }
    }
}
