package br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.DeleteAllDataUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteAllDataUseCaseTest {

    private val repository = mockk<SettingsRepository>()
    private val useCase = DeleteAllDataUseCase(repository)

    @Test
    fun `invoke deletes all data through the repository`() = runTest {
        coEvery { repository.deleteAllData() } returns Unit

        val result = useCase()

        assertTrue(result.isSuccess)
        coVerify { repository.deleteAllData() }
    }

    @Test
    fun `invoke fails when the repository throws`() = runTest {
        coEvery { repository.deleteAllData() } throws IllegalStateException("boom")

        assertTrue(useCase().isFailure)
    }
}
