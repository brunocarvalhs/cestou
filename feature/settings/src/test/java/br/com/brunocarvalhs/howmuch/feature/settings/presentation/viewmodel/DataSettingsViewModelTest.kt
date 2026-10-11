package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import android.content.Context
import android.widget.Toast
import br.com.brunocarvalhs.howmuch.core.domain.exception.RecentLoginRequiredException
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.ClearCacheUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.DeleteAccountUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.DeleteAllDataUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel.DataSettingsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DataSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val context = mockk<Context>(relaxed = true)
    private val clearCacheUseCase = mockk<ClearCacheUseCase>()
    private val deleteAllDataUseCase = mockk<DeleteAllDataUseCase>()
    private val deleteAccountUseCase = mockk<DeleteAccountUseCase>()
    private val authService = mockk<AuthService>(relaxed = true)
    private val navigator = mockk<Navigator>(relaxed = true)
    private val viewModel = DataSettingsViewModel(
        context,
        clearCacheUseCase,
        deleteAllDataUseCase,
        deleteAccountUseCase,
        authService
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel.setNavigator(navigator)
        mockkStatic(Toast::class)
        every { Toast.makeText(any(), any<CharSequence>(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Toast::class)
    }

    @Test
    fun `onClearCache delegates to the use case`() = runTest {
        coEvery { clearCacheUseCase() } returns Result.success(Unit)

        viewModel.intent.onClearCache()

        coVerify { clearCacheUseCase() }
    }

    @Test
    fun `onDeleteAllData delegates to the use case`() = runTest {
        coEvery { deleteAllDataUseCase() } returns Result.success(Unit)

        viewModel.intent.onDeleteAllData()

        coVerify { deleteAllDataUseCase() }
    }

    @Test
    fun `onDeleteAccount delegates to the use case`() = runTest {
        coEvery { deleteAccountUseCase() } returns Result.success(Unit)

        viewModel.intent.onDeleteAccount()

        coVerify { deleteAccountUseCase() }
    }

    @Test
    fun `onBack navigates back`() {
        viewModel.intent.onBack()

        verify { navigator.goBack() }
    }

    @Test
    fun `stale login asks for reauth and does not sign out yet`() = runTest {
        coEvery { deleteAccountUseCase() } returns Result.failure(RecentLoginRequiredException())

        viewModel.intent.onDeleteAccount()

        assertTrue(viewModel.uiState.value.reauthRequired)
        coVerify(exactly = 0) { authService.signOut() }
    }

    @Test
    fun `cancelling reauth dismisses the dialog and keeps the session`() = runTest {
        coEvery { deleteAccountUseCase() } returns Result.failure(RecentLoginRequiredException())
        viewModel.intent.onDeleteAccount()

        viewModel.intent.onReauthDismissed()

        assertFalse(viewModel.uiState.value.reauthRequired)
        coVerify(exactly = 0) { authService.signOut() }
    }

    @Test
    fun `confirming reauth signs the user out`() = runTest {
        coEvery { deleteAccountUseCase() } returns Result.failure(RecentLoginRequiredException())
        viewModel.intent.onDeleteAccount()

        viewModel.intent.onReauthConfirmed()

        assertFalse(viewModel.uiState.value.reauthRequired)
        coVerify { authService.signOut() }
    }

    @Test
    fun `other failures show the generic error, not the reauth dialog`() = runTest {
        coEvery { deleteAccountUseCase() } returns Result.failure(IllegalStateException("boom"))

        viewModel.intent.onDeleteAccount()

        assertFalse(viewModel.uiState.value.reauthRequired)
        assertTrue(viewModel.uiState.value.message != null)
    }
}
