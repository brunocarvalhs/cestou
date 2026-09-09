package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.CommonProduct
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.CommonProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommonProductAddAllToShoppingUseCaseTest {

    private val commonProductRepository = mockk<CommonProductRepository>(relaxed = true)
    private val productRepository = mockk<ProductRepository>(relaxed = true)
    private val useCase = CommonProductAddAllToShoppingUseCase(commonProductRepository, productRepository)

    @Test
    fun `invoke seeds defaults then saves every common product into the shopping list`() = runTest {
        val items = listOf(
            CommonProduct(id = "1", name = "Arroz", category = "Graos"),
            CommonProduct(id = "2", name = "Feijao", category = "Graos")
        )
        coEvery { commonProductRepository.seedDefaultsIfEmpty() } returns Result.success(Unit)
        coEvery { commonProductRepository.getAll() } returns flowOf(items)
        coEvery { productRepository.saveProduct(any(), "shopping-1") } returns Result.success(Unit)

        val result = useCase("shopping-1")

        assertTrue(result.isSuccess)
        coVerifyOrder {
            commonProductRepository.seedDefaultsIfEmpty()
            commonProductRepository.getAll()
        }
        coVerify(exactly = 1) {
            productRepository.saveProduct(
                match<Product> { it.name == "Arroz" && it.category == "Graos" && it.quantity == 1.0 && it.price == 0.0 },
                "shopping-1"
            )
        }
        coVerify(exactly = 1) {
            productRepository.saveProduct(
                match<Product> { it.name == "Feijao" && it.category == "Graos" },
                "shopping-1"
            )
        }
    }

    @Test
    fun `invoke does nothing when there are no common products`() = runTest {
        coEvery { commonProductRepository.seedDefaultsIfEmpty() } returns Result.success(Unit)
        coEvery { commonProductRepository.getAll() } returns flowOf(emptyList())

        val result = useCase("shopping-1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { productRepository.saveProduct(any(), any()) }
    }

    @Test
    fun `invoke returns failure when saving a product throws`() = runTest {
        val items = listOf(CommonProduct(id = "1", name = "Arroz"))
        coEvery { commonProductRepository.seedDefaultsIfEmpty() } returns Result.success(Unit)
        coEvery { commonProductRepository.getAll() } returns flowOf(items)
        coEvery { productRepository.saveProduct(any(), any()) } throws IllegalStateException("write failed")

        val result = useCase("shopping-1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `execute resolves shoppingId from arguments first`() = runTest {
        coEvery { commonProductRepository.seedDefaultsIfEmpty() } returns Result.success(Unit)
        coEvery { commonProductRepository.getAll() } returns flowOf(emptyList())

        val result = useCase.execute(
            arguments = mapOf("shoppingId" to "from-arguments"),
            session = mockk(relaxed = true),
            metadata = mapOf("shopping" to "from-metadata")
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `execute falls back to metadata shopping id when argument is missing`() = runTest {
        coEvery { commonProductRepository.seedDefaultsIfEmpty() } returns Result.success(Unit)
        coEvery { commonProductRepository.getAll() } returns flowOf(emptyList())

        val result = useCase.execute(
            arguments = emptyMap(),
            session = mockk(relaxed = true),
            metadata = mapOf("shopping" to "from-metadata")
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `execute fails when no shopping id is available anywhere`() = runTest {
        val result = useCase.execute(
            arguments = emptyMap(),
            session = mockk(relaxed = true),
            metadata = emptyMap()
        )

        assertTrue(result.isFailure)
        assertEquals(
            "ID da lista de compras não encontrado",
            result.exceptionOrNull()?.message
        )
    }
}
