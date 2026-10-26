package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ProductReader
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

private const val NOON_HOUR = 12
private const val MIDNIGHT_MINUTE = 0
private const val ZERO_DELTA = 0.0

private const val SEPTEMBER_LIST_PRICE = 120.0
private const val OCTOBER_LIST_PRICE = 80.0
private const val NOVEMBER_LIST_PRICE = 50.0
private const val SMALL_ITEM_PRICE = 10.0
private const val MEDIUM_ITEM_PRICE = 5.50
private const val UNPURCHASED_ITEM_PRICE = 999.0
private const val ESTIMATED_TOTAL = SMALL_ITEM_PRICE + MEDIUM_ITEM_PRICE
private const val IN_PROGRESS_ITEM_PRICE = 12.0
private const val BUDGET_AMOUNT = 100.0
private const val NO_BUDGET_LIST_PRICE = 20.0
private const val SINGLE_LIST_PRICE = 10.0
private const val MONTHS_SHOWN = 6

private fun noonUtc(year: Int, month: Int, day: Int): Long =
    LocalDate.of(year, month, day)
        .atTime(NOON_HOUR, MIDNIGHT_MINUTE)
        .atZone(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

private fun yearMonth(year: Int, month: Int): YearMonth = YearMonth.of(year, month)

private fun finishedList(id: String, price: Double, createdAt: Long, budget: Double? = null) = Shopping(
    id = id,
    title = "List $id",
    description = "",
    price = price,
    status = Shopping.Status.FINISH,
    users = listOf("u1"),
    roles = mapOf("u1" to "OWNER"),
    createdAt = createdAt,
    budget = budget
)

private fun inProgressList(id: String, createdAt: Long) = Shopping(
    id = id,
    title = "List $id",
    description = "",
    price = 0.0,
    status = Shopping.Status.IN_PROGRESS,
    users = listOf("u1"),
    roles = mapOf("u1" to "OWNER"),
    createdAt = createdAt
)

private fun product(
    id: String,
    price: Double?,
    isPurchased: Boolean = false,
    history: List<ProductActivity> = emptyList()
) = Product(id = id, name = "Product $id", quantity = 1.0, price = price, isPurchased = isPurchased, history = history)

class GetSpendHistoryUseCaseTest {

    private val shoppingRepository = mockk<ShoppingRepository>()
    private val productReader = mockk<ProductReader>()
    private val useCase = GetSpendHistoryUseCase(shoppingRepository, productReader)

    private fun stubProducts(shoppingId: String, products: List<Product>) {
        coEvery { productReader.getAllProducts(shoppingId) } returns flowOf(products)
    }

    @Test
    fun `invoke returns an empty list when the account has no shopping lists`() = runTest {
        coEvery { shoppingRepository.getAll() } returns emptyList()

        val result = useCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke falls back to createdAt when no product has a PURCHASED activity`() = runTest {
        val createdAt = noonUtc(year = 2026, month = 9, day = 10)
        val list = finishedList(id = "s1", price = SEPTEMBER_LIST_PRICE, createdAt = createdAt)
        coEvery { shoppingRepository.getAll() } returns listOf(list)
        stubProducts("s1", emptyList())

        val result = useCase(now = createdAt)

        val month = result.single { it.yearMonth == yearMonth(year = 2026, month = 9) }
        assertEquals(createdAt, month.purchases.single().purchaseDate)
    }

    @Test
    fun `invoke prefers the latest PURCHASED activity over createdAt`() = runTest {
        val createdAt = noonUtc(year = 2026, month = 10, day = 30)
        val purchasedAt = noonUtc(year = 2026, month = 11, day = 2)
        val list = finishedList(id = "s1", price = NOVEMBER_LIST_PRICE, createdAt = createdAt)
        coEvery { shoppingRepository.getAll() } returns listOf(list)
        stubProducts(
            "s1",
            listOf(
                product(
                    id = "p1",
                    price = SMALL_ITEM_PRICE,
                    isPurchased = true,
                    history = listOf(
                        ProductActivity(
                            userId = "u1",
                            action = ProductActivity.Action.PURCHASED,
                            timestamp = purchasedAt
                        )
                    )
                )
            )
        )

        val result = useCase(now = purchasedAt)

        val november = result.single { it.yearMonth == yearMonth(year = 2026, month = 11) }
        val october = result.single { it.yearMonth == yearMonth(year = 2026, month = 10) }
        assertEquals(1, november.purchases.size)
        assertTrue(october.purchases.isEmpty())
        assertEquals(NOVEMBER_LIST_PRICE, november.total, ZERO_DELTA)
    }

    @Test
    fun `invoke uses the confirmed price when positive and does not mark it estimated`() = runTest {
        val createdAt = noonUtc(year = 2026, month = 10, day = 3)
        val list = finishedList(id = "s1", price = OCTOBER_LIST_PRICE, createdAt = createdAt)
        coEvery { shoppingRepository.getAll() } returns listOf(list)
        stubProducts("s1", emptyList())

        val result = useCase(now = createdAt)

        val purchase = result.single { it.yearMonth == yearMonth(year = 2026, month = 10) }.purchases.single()
        assertEquals(OCTOBER_LIST_PRICE, purchase.total, ZERO_DELTA)
        assertFalse(purchase.isEstimated)
    }

    @Test
    fun `invoke sums purchased items and flags the total as estimated when price is not positive`() = runTest {
        val createdAt = noonUtc(year = 2026, month = 10, day = 28)
        val list = finishedList(id = "s1", price = 0.0, createdAt = createdAt)
        coEvery { shoppingRepository.getAll() } returns listOf(list)
        stubProducts(
            "s1",
            listOf(
                product(id = "p1", price = SMALL_ITEM_PRICE, isPurchased = true),
                product(id = "p2", price = MEDIUM_ITEM_PRICE, isPurchased = true),
                product(id = "p3", price = UNPURCHASED_ITEM_PRICE, isPurchased = false)
            )
        )

        val result = useCase(now = createdAt)

        val purchase = result.single { it.yearMonth == yearMonth(year = 2026, month = 10) }.purchases.single()
        assertEquals(ESTIMATED_TOTAL, purchase.total, ZERO_DELTA)
        assertTrue(purchase.isEstimated)
    }

    @Test
    fun `invoke counts only finished lists toward a month's total, excluding in-progress lists`() = runTest {
        val now = noonUtc(year = 2026, month = 10, day = 30)
        val finished = finishedList(
            id = "finished",
            price = OCTOBER_LIST_PRICE,
            createdAt = noonUtc(year = 2026, month = 10, day = 3)
        )
        val inProgress = inProgressList(id = "open", createdAt = noonUtc(year = 2026, month = 10, day = 20))
        coEvery { shoppingRepository.getAll() } returns listOf(finished, inProgress)
        stubProducts("finished", emptyList())
        stubProducts("open", listOf(product(id = "p1", price = IN_PROGRESS_ITEM_PRICE, isPurchased = true)))

        val result = useCase(now = now)

        val october = result.single { it.yearMonth == yearMonth(year = 2026, month = 10) }
        assertEquals(OCTOBER_LIST_PRICE, october.total, ZERO_DELTA)
        assertEquals(1, october.purchases.size)
    }

    @Test
    fun `invoke reports in-progress purchased totals only on the current month`() = runTest {
        val now = noonUtc(year = 2026, month = 10, day = 30)
        val olderFinished = finishedList(
            id = "older",
            price = SEPTEMBER_LIST_PRICE,
            createdAt = noonUtc(year = 2026, month = 9, day = 10)
        )
        val finished = finishedList(
            id = "finished",
            price = OCTOBER_LIST_PRICE,
            createdAt = noonUtc(year = 2026, month = 10, day = 3)
        )
        val inProgress = inProgressList(id = "open", createdAt = noonUtc(year = 2026, month = 10, day = 20))
        coEvery { shoppingRepository.getAll() } returns listOf(olderFinished, finished, inProgress)
        stubProducts("older", emptyList())
        stubProducts("finished", emptyList())
        stubProducts("open", listOf(product(id = "p1", price = IN_PROGRESS_ITEM_PRICE, isPurchased = true)))

        val result = useCase(now = now)

        val october = result.single { it.yearMonth == yearMonth(year = 2026, month = 10) }
        val september = result.single { it.yearMonth == yearMonth(year = 2026, month = 9) }
        assertEquals(IN_PROGRESS_ITEM_PRICE, october.inProgress?.total ?: ZERO_DELTA, ZERO_DELTA)
        assertEquals(1, october.inProgress?.listCount)
        assertNull(september.inProgress)
    }

    @Test
    fun `invoke scopes the month budget to only lists that had a budget set`() = runTest {
        val now = noonUtc(year = 2026, month = 10, day = 30)
        val withBudget = finishedList(
            id = "a",
            price = OCTOBER_LIST_PRICE,
            createdAt = noonUtc(year = 2026, month = 10, day = 3),
            budget = BUDGET_AMOUNT
        )
        val withoutBudget = finishedList(
            id = "b",
            price = NO_BUDGET_LIST_PRICE,
            createdAt = noonUtc(year = 2026, month = 10, day = 15)
        )
        coEvery { shoppingRepository.getAll() } returns listOf(withBudget, withoutBudget)
        stubProducts("a", emptyList())
        stubProducts("b", emptyList())

        val result = useCase(now = now)

        val october = result.single { it.yearMonth == yearMonth(year = 2026, month = 10) }
        assertEquals(BUDGET_AMOUNT, october.budget?.amount ?: ZERO_DELTA, ZERO_DELTA)
        assertEquals(OCTOBER_LIST_PRICE, october.budget?.spent ?: ZERO_DELTA, ZERO_DELTA)
        assertEquals(OCTOBER_LIST_PRICE + NO_BUDGET_LIST_PRICE, october.total, ZERO_DELTA)
    }

    @Test
    fun `invoke does not pad months before the account's earliest list`() = runTest {
        val now = noonUtc(year = 2026, month = 10, day = 30)
        val list = finishedList(id = "a", price = SINGLE_LIST_PRICE, createdAt = now)
        coEvery { shoppingRepository.getAll() } returns listOf(list)
        stubProducts("a", emptyList())

        val result = useCase(now = now)

        assertEquals(1, result.size)
        assertEquals(yearMonth(year = 2026, month = 10), result.single().yearMonth)
    }

    @Test
    fun `invoke shows the current month plus up to 5 previous months when history is older`() = runTest {
        val now = noonUtc(year = 2026, month = 10, day = 30)
        val list = finishedList(
            id = "a",
            price = SINGLE_LIST_PRICE,
            createdAt = noonUtc(year = 2025, month = 1, day = 1)
        )
        coEvery { shoppingRepository.getAll() } returns listOf(list)

        val result = useCase(now = now)

        assertEquals(MONTHS_SHOWN, result.size)
        assertEquals(yearMonth(year = 2026, month = 5), result.first().yearMonth)
        assertEquals(yearMonth(year = 2026, month = 10), result.last().yearMonth)
    }
}
