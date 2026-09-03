package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.ClearCacheUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.DeleteAccountUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.DeleteAllDataUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel.DataSettingsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DataSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val context = mockk<Context>(relaxed = true)
    private val clearCacheUseCase = mockk<ClearCacheUseCase>()
    private val deleteAllDataUseCase = mockk<DeleteAllDataUseCase>()
    private val deleteAccountUseCase = mockk<DeleteAccountUseCase>()
    private val navigator = mockk<Navigator>(relaxed = true)
    private val viewModel = DataSettingsViewModel(
        context,
        clearCacheUseCase,
        deleteAllDataUseCase,
        deleteAccountUseCase
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel.setNavigator(navigator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
}
