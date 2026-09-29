package br.com.brunocarvalhs.howmuch.core.data.repository

import br.com.brunocarvalhs.howmuch.core.domain.services.StorageService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.reflect.KType

class AiTrialRepositoryImplTest {

    private val storageService = mockk<StorageService>()
    private val repository = AiTrialRepositoryImpl(storageService)

    @Test
    fun `hasUsedFreeMessage defaults to false when nothing was ever saved`() = runTest {
        every {
            storageService.observe(any(), Boolean::class, any<KType>())
        } returns flowOf(null)

        var value = true
        repository.hasUsedFreeMessage().collect { value = it }

        assertEquals(false, value)
    }

    @Test
    fun `hasUsedFreeMessage reflects a previously saved value`() = runTest {
        every {
            storageService.observe(any(), Boolean::class, any<KType>())
        } returns flowOf(true)

        var value = false
        repository.hasUsedFreeMessage().collect { value = it }

        assertEquals(true, value)
    }

    @Test
    fun `markFreeMessageUsed saves true`() = runTest {
        coEvery { storageService.save(any(), true) } returns Unit

        repository.markFreeMessageUsed()

        coVerify { storageService.save(any(), true) }
    }
}
