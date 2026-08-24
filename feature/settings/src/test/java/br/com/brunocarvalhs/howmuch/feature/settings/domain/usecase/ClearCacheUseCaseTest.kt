package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.ClearCacheUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ClearCacheUseCaseTest {

    private val repository = mockk<SettingsRepository>()
    private val useCase = ClearCacheUseCase(repository)

    @Test
    fun `invoke clears the cache through the repository`() = runTest {
        coEvery { repository.clearCache() } returns Unit

        val result = useCase()

        assertTrue(result.isSuccess)
        coVerify { repository.clearCache() }
    }

    @Test
    fun `invoke fails when the repository throws`() = runTest {
        coEvery { repository.clearCache() } throws IllegalStateException("boom")

        assertTrue(useCase().isFailure)
    }
}
