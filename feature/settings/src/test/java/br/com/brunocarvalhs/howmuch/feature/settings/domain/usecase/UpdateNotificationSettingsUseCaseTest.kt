package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.settings.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateNotificationSettingsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UpdateNotificationSettingsUseCaseTest {

    private val repository = mockk<SettingsRepository>()
    private val useCase = UpdateNotificationSettingsUseCase(repository)

    @Test
    fun `invoke forwards enabled flag and reminder time to the repository`() = runTest {
        coEvery { repository.updateNotificationSettings(true, "08:00") } returns Unit

        useCase(true, "08:00")

        coVerify { repository.updateNotificationSettings(true, "08:00") }
    }
}
