package br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.util.PersonNameError
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.NameField
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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

/** Spec EPA-06 (required-name step) with the EPA-13 rules. */
class CompleteNameViewModelTest {

    private val authService = mockk<AuthService> {
        every { currentUser } returns AuthenticatedUser(id = "u1")
        coEvery { updateDisplayName(any()) } returns Result.success(Unit)
        coEvery { signOut() } returns Result.success(Unit)
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

    private fun viewModel() = CompleteNameViewModel(authService, analyticsTracker)

    @Test
    fun `continue is disabled until both names are valid`() {
        val vm = viewModel()
        assertFalse(vm.uiState.value.isSubmitEnabled)

        vm.intent.onGivenNameChange("Ana")
        assertFalse(vm.uiState.value.isSubmitEnabled)

        vm.intent.onFamilyNameChange("xxx")
        assertFalse(vm.uiState.value.isSubmitEnabled)

        vm.intent.onFamilyNameChange("Silva")
        assertTrue(vm.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `prefills a one-word existing name into the first-name field`() {
        every { authService.currentUser } returns AuthenticatedUser(id = "u1", displayName = "Raoni")

        val state = viewModel().uiState.value

        assertEquals("Raoni", state.givenName)
        assertEquals("", state.familyName)
    }

    @Test
    fun `leaving a field shows its error and tracks the category`() {
        val vm = viewModel()
        vm.intent.onFamilyNameChange("Teste")

        vm.intent.onFieldFocusLost(NameField.FAMILY_NAME)

        assertEquals(PersonNameError.NOT_A_NAME, vm.uiState.value.familyNameError)
        assertNull(vm.uiState.value.givenNameError)
        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.AUTH_NAME_VALIDATION_FAILED,
                mapOf(AnalyticsParams.REASON to "blocked", AnalyticsParams.SOURCE to "name_step")
            )
        }
    }

    @Test
    fun `saving stores exactly the normalized full name`() {
        val vm = viewModel()
        vm.intent.onGivenNameChange(" ana ")
        vm.intent.onFamilyNameChange("silva")

        vm.intent.onSubmit()

        coVerify { authService.updateDisplayName("Ana Silva") }
        assertTrue(vm.uiState.value.isSuccess)
    }

    @Test
    fun `a failed save shows a form error and keeps the step open`() {
        coEvery { authService.updateDisplayName(any()) } returns Result.failure(IllegalStateException())
        val vm = viewModel()
        vm.intent.onGivenNameChange("Ana")
        vm.intent.onFamilyNameChange("Silva")

        vm.intent.onSubmit()

        assertTrue(vm.uiState.value.hasSaveError)
        assertFalse(vm.uiState.value.isSuccess)
        assertTrue(vm.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `a second tap while saving sends a single request`() {
        val pending = CompletableDeferred<Result<Unit>>()
        coEvery { authService.updateDisplayName(any()) } coAnswers { pending.await() }
        val vm = viewModel()
        vm.intent.onGivenNameChange("Ana")
        vm.intent.onFamilyNameChange("Silva")

        vm.intent.onSubmit()
        vm.intent.onSubmit()
        pending.complete(Result.success(Unit))

        coVerify(exactly = 1) { authService.updateDisplayName(any()) }
    }

    @Test
    fun `use another account signs out`() {
        viewModel().intent.onUseAnotherAccount()

        coVerify { authService.signOut() }
    }
}
