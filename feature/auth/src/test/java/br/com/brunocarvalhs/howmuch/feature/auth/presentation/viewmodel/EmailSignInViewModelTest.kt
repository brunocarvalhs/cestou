package br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthError
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthException
import br.com.brunocarvalhs.howmuch.feature.auth.domain.repository.EmailAuthRepository
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.EmailFieldError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.PasswordFieldError
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

/** Spec EPA-03. */
@Suppress("TooManyFunctions")
class EmailSignInViewModelTest {

    private val repository = mockk<EmailAuthRepository> {
        coEvery { signIn(any(), any()) } returns Result.success(Unit)
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

    private fun viewModel(prefilledEmail: String = "") =
        EmailSignInViewModel(repository, analyticsTracker).apply { start(prefilledEmail) }

    @Test
    fun `prefills the e-mail handed over by the sign-up screen`() {
        assertEquals("ana@test.com", viewModel("ana@test.com").uiState.value.email)
    }

    @Test
    fun `start does not overwrite what the user already typed`() {
        val vm = viewModel("ana@test.com")
        vm.intent.onEmailChange("bia@test.com")

        vm.start("ana@test.com")

        assertEquals("bia@test.com", vm.uiState.value.email)
    }

    @Test
    fun `submit is enabled only with a well-formed e-mail and a password`() {
        val vm = viewModel()
        assertFalse(vm.uiState.value.isSubmitEnabled)

        vm.intent.onEmailChange("ana@test")
        vm.intent.onPasswordChange("x")
        assertFalse(vm.uiState.value.isSubmitEnabled)

        vm.intent.onEmailChange("ana@test.com")
        assertTrue(vm.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `leaving the e-mail field with a malformed address shows the error`() {
        val vm = viewModel()
        vm.intent.onEmailChange("ana@")

        vm.intent.onEmailFocusLost()

        assertEquals(EmailFieldError.INVALID, vm.uiState.value.emailError)
    }

    @Test
    fun `successful sign in`() {
        val vm = viewModel()
        vm.intent.onEmailChange("ana@test.com")
        vm.intent.onPasswordChange("12345678")

        vm.intent.onSubmit()

        coVerify { repository.signIn("ana@test.com", "12345678") }
        assertTrue(vm.uiState.value.isSuccess)
    }

    @Test
    fun `wrong credentials show one generic form error and keep the e-mail`() {
        coEvery { repository.signIn(any(), any()) } returns
            Result.failure(EmailAuthException(EmailAuthError.INVALID_CREDENTIALS))
        val vm = viewModel()
        vm.intent.onEmailChange("ana@test.com")
        vm.intent.onPasswordChange("wrong")

        vm.intent.onSubmit()

        assertEquals(EmailAuthError.INVALID_CREDENTIALS, vm.uiState.value.formError)
        assertEquals("ana@test.com", vm.uiState.value.email)
        assertFalse(vm.uiState.value.isLoading)
        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.AUTH_SIGN_IN_FAILED,
                mapOf(AnalyticsParams.REASON to "INVALID_CREDENTIALS", AnalyticsParams.SOURCE to "email")
            )
        }
    }

    @Test
    fun `typing again clears the form error`() {
        coEvery { repository.signIn(any(), any()) } returns
            Result.failure(EmailAuthException(EmailAuthError.TOO_MANY_REQUESTS))
        val vm = viewModel()
        vm.intent.onEmailChange("ana@test.com")
        vm.intent.onPasswordChange("x")
        vm.intent.onSubmit()
        assertEquals(EmailAuthError.TOO_MANY_REQUESTS, vm.uiState.value.formError)

        vm.intent.onPasswordChange("xy")

        assertNull(vm.uiState.value.formError)
    }

    @Test
    fun `submitting empty fields shows field errors and sends nothing`() {
        val vm = viewModel()

        vm.intent.onSubmit()

        assertEquals(EmailFieldError.EMPTY, vm.uiState.value.emailError)
        assertEquals(PasswordFieldError.EMPTY, vm.uiState.value.passwordError)
        coVerify(exactly = 0) { repository.signIn(any(), any()) }
    }

    @Test
    fun `a second tap while signing in sends a single request`() {
        val pending = CompletableDeferred<Result<Unit>>()
        coEvery { repository.signIn(any(), any()) } coAnswers { pending.await() }
        val vm = viewModel()
        vm.intent.onEmailChange("ana@test.com")
        vm.intent.onPasswordChange("12345678")

        vm.intent.onSubmit()
        vm.intent.onSubmit()
        pending.complete(Result.success(Unit))

        coVerify(exactly = 1) { repository.signIn(any(), any()) }
    }
}
