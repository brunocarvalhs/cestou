package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.settings.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateShoppingPreferencesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateShoppingPreferencesUseCaseTest {

    private val repository = mockk<SettingsRepository>()
    private val useCase = UpdateShoppingPreferencesUseCase(repository)

    @Test
    fun `invoke forwards every preference to the repository`() = runTest {
        coEvery { repository.updateShoppingPreferences("list1", "NAME", true) } returns Unit

        useCase(defaultListId = "list1", sortingMode = "NAME", remindersEnabled = true)

        coVerify { repository.updateShoppingPreferences("list1", "NAME", true) }
    }

    @Test
    fun `invoke forwards a null defaultListId`() = runTest {
        coEvery { repository.updateShoppingPreferences(null, "DATE", false) } returns Unit

        useCase(defaultListId = null, sortingMode = "DATE", remindersEnabled = false)

        coVerify { repository.updateShoppingPreferences(null, "DATE", false) }
    }
}
