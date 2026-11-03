package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveHistoryUseCaseTest {

    private val repository = mockk<ShoppingRepository>()
    private val useCase = ObserveHistoryUseCase(repository, GroupHistoryByMonthUseCase(TestClock))

    @Test
    fun `emits no months when the repository has no lists`() = runTest {
        every { repository.observeAll() } returns flowOf(emptyList())

        useCase().test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `emits only the finished lists grouped by month`() = runTest {
        every { repository.observeAll() } returns flowOf(
            listOf(
                finishedShopping("done", finishedAt = millisOf(2026, 11, 1)),
                finishedShopping("open", status = Shopping.Status.IN_PROGRESS)
            )
        )

        useCase().test {
            assertEquals(listOf("done"), awaitItem().single().purchases.map { it.shopping.id })
            awaitComplete()
        }
    }

    @Test
    fun `does not re-emit when a change leaves the history untouched`() = runTest {
        val done = finishedShopping("done", finishedAt = millisOf(2026, 11, 1))
        every { repository.observeAll() } returns flowOf(
            listOf(done),
            listOf(done, finishedShopping("open", status = Shopping.Status.NEW))
        )

        useCase().test {
            awaitItem()
            awaitComplete()
        }
    }

    @Test
    fun `propagates the repository failure`() = runTest {
        every { repository.observeAll() } returns flow { throw IllegalStateException("offline") }

        useCase().test {
            assertEquals("offline", awaitError().message)
        }
    }
}
