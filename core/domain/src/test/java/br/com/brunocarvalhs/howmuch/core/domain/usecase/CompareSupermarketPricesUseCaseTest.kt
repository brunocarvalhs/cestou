package br.com.brunocarvalhs.howmuch.core.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.ChainPriceIndex
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.SupermarketChain
import br.com.brunocarvalhs.howmuch.core.domain.model.UserLocation
import br.com.brunocarvalhs.howmuch.core.domain.repository.SupermarketRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompareSupermarketPricesUseCaseTest {

    private val location = UserLocation("RJ", "Niterói")
    private val cheap = SupermarketChain("cheap", "Cheap")
    private val mid = SupermarketChain("mid", "Mid")
    private val pricey = SupermarketChain("pricey", "Pricey")
    private val repository = mockk<SupermarketRepository>()

    private fun useCase(testScheduler: kotlinx.coroutines.test.TestCoroutineScheduler) =
        CompareSupermarketPricesUseCase(repository, StandardTestDispatcher(testScheduler))

    private fun product(id: String, price: Double?, quantity: Double = 1.0) =
        Product(id = id, name = id, quantity = quantity, price = price)

    @Test
    fun `ranks chains from cheapest to most expensive with savings against the priciest`() = runTest {
        coEvery { repository.getChainIndexes(location) } returns listOf(
            ChainPriceIndex(pricey, 1.10),
            ChainPriceIndex(cheap, 0.80),
            ChainPriceIndex(mid, 1.00)
        )

        val result = useCase(testScheduler)(listOf(product("a", 10.0, 2.0), product("b", 30.0)), location)

        assertEquals(50.0, result.baseTotal, 0.001)
        assertEquals(listOf(cheap, mid, pricey), result.quotes.map { it.chain })
        assertEquals(40.0, result.quotes[0].estimatedTotal, 0.001)
        assertEquals(15.0, result.quotes[0].savings, 0.001)
        assertEquals(0.0, result.quotes[2].savings, 0.001)
        assertEquals(cheap, result.cheapest?.chain)
        assertEquals(15.0, result.maxSavings, 0.001)
    }

    @Test
    fun `items without price are left out and counted`() = runTest {
        coEvery { repository.getChainIndexes(location) } returns listOf(ChainPriceIndex(cheap, 0.9))

        val result = useCase(testScheduler)(
            listOf(product("a", 10.0), product("b", null), product("c", 0.0)),
            location
        )

        assertEquals(10.0, result.baseTotal, 0.001)
        assertEquals(2, result.unpricedCount)
    }

    @Test
    fun `no chains for the location gives an empty ranking`() = runTest {
        coEvery { repository.getChainIndexes(location) } returns emptyList()

        val result = useCase(testScheduler)(listOf(product("a", 10.0)), location)

        assertEquals(emptyList<Any>(), result.quotes)
        assertNull(result.cheapest)
        assertEquals(0.0, result.maxSavings, 0.001)
    }

    @Test
    fun `empty list gives zero totals`() = runTest {
        coEvery { repository.getChainIndexes(location) } returns listOf(ChainPriceIndex(cheap, 0.9))

        val result = useCase(testScheduler)(emptyList(), location)

        assertEquals(0.0, result.baseTotal, 0.001)
        assertEquals(0, result.unpricedCount)
        assertEquals(0.0, result.maxSavings, 0.001)
    }
}
