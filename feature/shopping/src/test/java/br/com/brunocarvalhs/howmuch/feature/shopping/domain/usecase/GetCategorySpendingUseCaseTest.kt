package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.CategorySpending
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class GetCategorySpendingUseCaseTest {

    private val products = mockk<ProductsUseCase>()
    private val cache = CategorySpendingCache()
    private val dispatcher = StandardTestDispatcher()
    private val useCase = GetCategorySpendingUseCase(products, cache, dispatcher)

    private fun product(
        id: String,
        category: String,
        price: Double?,
        quantity: Double = 1.0,
        purchased: Boolean = true
    ) = Product(id = id, name = id, quantity = quantity, price = price, isPurchased = purchased, category = category)

    private fun totals(result: Result<List<CategorySpending>>) =
        result.getOrThrow().associate { it.rawKey to it.total }

    @Test
    fun `sums price times quantity by category`() = runTest(dispatcher) {
        coEvery { products("a") } returns flowOf(
            listOf(product("p1", "carnes", 10.0, 2.0), product("p2", "carnes", 5.0), product("p3", "bebidas", 3.0))
        )

        val result = useCase(listOf(finishedShopping("a")))

        assertEquals(mapOf("carnes" to 25.0, "bebidas" to 3.0), totals(result))
    }

    @Test
    fun `ignores products that were not purchased`() = runTest(dispatcher) {
        coEvery { products("a") } returns flowOf(
            listOf(product("p1", "carnes", 10.0), product("p2", "carnes", 99.0, purchased = false))
        )

        assertEquals(mapOf("carnes" to 10.0), totals(useCase(listOf(finishedShopping("a")))))
    }

    @Test
    fun `a missing price counts as zero and the category disappears`() = runTest(dispatcher) {
        coEvery { products("a") } returns flowOf(listOf(product("p1", "carnes", null), product("p2", "bebidas", 4.0)))

        assertEquals(mapOf("bebidas" to 4.0), totals(useCase(listOf(finishedShopping("a")))))
    }

    @Test
    fun `adds up several lists and unifies case and spaces in the key`() = runTest(dispatcher) {
        coEvery { products("a") } returns flowOf(listOf(product("p1", "Carnes ", 10.0)))
        coEvery { products("b") } returns flowOf(listOf(product("p2", " carnes", 5.0)))

        val result = useCase(listOf(finishedShopping("a"), finishedShopping("b")))

        assertEquals(mapOf("carnes" to 15.0), totals(result))
    }

    @Test
    fun `lists without products give an empty ranking`() = runTest(dispatcher) {
        coEvery { products("a") } returns flowOf(emptyList())

        assertTrue(useCase(listOf(finishedShopping("a"))).getOrThrow().isEmpty())
    }

    @Test
    fun `fails as a whole when one list cannot be read`() = runTest(dispatcher) {
        coEvery { products("a") } returns flowOf(listOf(product("p1", "carnes", 10.0)))
        coEvery { products("b") } throws IllegalStateException("offline")

        val result = useCase(listOf(finishedShopping("a"), finishedShopping("b")))

        assertTrue(result.isFailure)
    }

    @Test
    fun `second call is served from the cache`() = runTest(dispatcher) {
        coEvery { products("a") } returns flowOf(listOf(product("p1", "carnes", 10.0)))
        val list = listOf(finishedShopping("a", finishedAt = 1L))

        useCase(list)
        useCase(list)

        coVerify(exactly = 1) { products("a") }
    }

    @Test
    fun `a new finishedAt invalidates the cached totals`() = runTest(dispatcher) {
        coEvery { products("a") } returns flowOf(listOf(product("p1", "carnes", 10.0)))

        useCase(listOf(finishedShopping("a", finishedAt = 1L)))
        useCase(listOf(finishedShopping("a", finishedAt = 2L)))

        coVerify(exactly = 2) { products("a") }
    }

    @Test
    fun `never reads more than four lists at once`() = runTest(dispatcher) {
        val running = AtomicInteger()
        val peak = AtomicInteger()
        coEvery { products(any()) } coAnswers {
            peak.accumulateAndGet(running.incrementAndGet(), ::maxOf)
            delay(SLOW_READ_MS)
            running.decrementAndGet()
            flowOf(emptyList())
        }

        useCase((1..LISTS).map { finishedShopping("l$it") })

        assertTrue("peak was ${peak.get()}", peak.get() in 1..MAX_PARALLEL)
    }

    private companion object {
        const val LISTS = 12
        const val MAX_PARALLEL = 4
        const val SLOW_READ_MS = 30L
    }
}
