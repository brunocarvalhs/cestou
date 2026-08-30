package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.products.domain.services.SharedService
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.SharedUseCase
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedUseCaseTest {

    private val useCase = SharedUseCase(mockk<SharedService>())

    @Test
    fun `invoke emits the greeting`() = runTest {
        assertEquals("Hello World!", useCase().first())
    }
}
