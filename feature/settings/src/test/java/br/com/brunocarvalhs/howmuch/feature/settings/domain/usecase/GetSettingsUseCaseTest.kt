package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.GetSettingsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetSettingsUseCaseTest {

    private val repository = mockk<SettingsRepository>()
    private val useCase = GetSettingsUseCase(repository)

    @Test
    fun `invoke returns the settings flow from the repository`() = runTest {
        val settings = AppSettings(language = "pt")
        every { repository.getSettings() } returns flowOf(settings)

        assertEquals(settings, useCase().first())
    }
}
