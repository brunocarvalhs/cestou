package br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthError
import br.com.brunocarvalhs.howmuch.feature.auth.domain.model.EmailAuthException
import br.com.brunocarvalhs.howmuch.feature.auth.domain.repository.EmailAuthRepository
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.EmailFieldError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Spec EPA-04. */
@Suppress("MagicNumber")
@OptIn(ExperimentalCoroutinesApi::class)
class PasswordResetViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<EmailAuthRepository> {
        coEvery { sendPasswordReset(any()) } returns Result.success(Unit)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(prefilledEmail: String = "") =
        PasswordResetViewModel(repository).apply { start(prefilledEmail) }

    @Test
    fun `prefills the e-mail typed on the sign-in screen`() {
        assertEquals("ana@test.com", viewModel("ana@test.com").uiState.value.email)
    }

    @Test
    fun `sending shows the same confirmation whether or not the account exists`() = runTest(dispatcher) {
        val vm = viewModel("ana@test.com")

        vm.intent.onSubmit()
        runCurrent()

        coVerify { repository.sendPasswordReset("ana@test.com") }
        assertTrue(vm.uiState.value.isSent)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `resend is blocked for 30 seconds with a visible countdown`() = runTest(dispatcher) {
        val vm = viewModel("ana@test.com")
        vm.intent.onSubmit()
        runCurrent()

        assertEquals(30, vm.uiState.value.resendCountdownSeconds)
        assertFalse(vm.uiState.value.isSubmitEnabled)
        vm.intent.onSubmit()
        runCurrent()
        coVerify(exactly = 1) { repository.sendPasswordReset(any()) }

        advanceTimeBy(29_000)
        runCurrent()
        assertEquals(1, vm.uiState.value.resendCountdownSeconds)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(0, vm.uiState.value.resendCountdownSeconds)
        assertTrue(vm.uiState.value.isSubmitEnabled)

        vm.intent.onSubmit()
        runCurrent()
        coVerify(exactly = 2) { repository.sendPasswordReset(any()) }
    }

    @Test
    fun `malformed e-mail is shown on the field and nothing is sent`() = runTest(dispatcher) {
        val vm = viewModel("ana@")

        vm.intent.onSubmit()
        runCurrent()

        assertEquals(EmailFieldError.INVALID, vm.uiState.value.emailError)
        coVerify(exactly = 0) { repository.sendPasswordReset(any()) }
    }

    @Test
    fun `network failure shows a form error and allows retrying right away`() = runTest(dispatcher) {
        coEvery { repository.sendPasswordReset(any()) } returns
            Result.failure(EmailAuthException(EmailAuthError.NETWORK))
        val vm = viewModel("ana@test.com")

        vm.intent.onSubmit()
        runCurrent()

        assertEquals(EmailAuthError.NETWORK, vm.uiState.value.formError)
        assertFalse(vm.uiState.value.isSent)
        assertTrue(vm.uiState.value.isSubmitEnabled)

        vm.intent.onEmailChange("ana@test.com.br")
        assertNull(vm.uiState.value.formError)
    }
}
