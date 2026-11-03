package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class UpdateAnalyticsEnabledUseCaseTest {

    private val repository = mockk<SettingsRepository>(relaxed = true)
    private val useCase = UpdateAnalyticsEnabledUseCase(repository)

    @Test
    fun `persists the choice`() = runTest {
        assertTrue(useCase(false).isSuccess)

        coVerify { repository.updateAnalyticsEnabled(false) }
    }

    @Test
    fun `a storage failure becomes a failed result`() = runTest {
        val error = IOException("disk")
        coEvery { repository.updateAnalyticsEnabled(any()) } throws error

        assertEquals(error, useCase(true).exceptionOrNull())
    }

    @Test(expected = CancellationException::class)
    fun `cancellation is never swallowed`() = runTest {
        coEvery { repository.updateAnalyticsEnabled(any()) } throws CancellationException("cancelled")

        useCase(true)
    }
}
