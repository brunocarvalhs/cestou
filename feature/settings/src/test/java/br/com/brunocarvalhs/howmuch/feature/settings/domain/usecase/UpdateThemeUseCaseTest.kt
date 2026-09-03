package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.feature.settings.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateThemeUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateThemeUseCaseTest {

    private val repository = mockk<SettingsRepository>()
    private val useCase = UpdateThemeUseCase(repository)

    @Test
    fun `invoke updates the theme through the repository`() = runTest {
        coEvery { repository.updateTheme(ThemeMode.DARK) } returns Unit

        val result = useCase(ThemeMode.DARK)

        assertTrue(result.isSuccess)
        coVerify { repository.updateTheme(ThemeMode.DARK) }
    }

    @Test
    fun `invoke fails when the repository throws`() = runTest {
        coEvery { repository.updateTheme(ThemeMode.DARK) } throws IllegalStateException("boom")

        assertTrue(useCase(ThemeMode.DARK).isFailure)
    }
}
