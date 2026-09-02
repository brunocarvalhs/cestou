package br.com.brunocarvalhs.howmuch.feature.products.data.repository

import android.graphics.Bitmap
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import br.com.brunocarvalhs.howmuch.feature.products.data.model.ProductModel
import br.com.brunocarvalhs.howmuch.feature.products.data.services.PriceTagResult
import br.com.brunocarvalhs.howmuch.feature.products.data.services.ProductImageTextRecognizer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProductRepositoryImplTest {

    private val networkService = mockk<NetworkService>()
    private val cloudNetwork = mockk<NetworkService>()
    private val shoppingRepository = mockk<ShoppingRepository>()
    private val imageTextRecognizer = mockk<ProductImageTextRecognizer>()
    private lateinit var repository: ProductRepositoryImpl

    private val shoppingInProgress = Shopping(
        id = "s1",
        title = "Weekly Groceries",
        description = "desc",
        price = 0.0,
        status = Shopping.Status.IN_PROGRESS,
        users = listOf("user-1"),
        roles = mapOf("user-1" to "OWNER")
    )

    private val shoppingFinished = shoppingInProgress.copy(status = Shopping.Status.FINISH)

    private val productModel = ProductModel(id = "p1", name = "Arroz", quantity = 1.0)

    @Before
    fun setup() {
        repository = ProductRepositoryImpl(networkService, cloudNetwork, shoppingRepository, imageTextRecognizer)
    }

    @Test
    fun `saveProduct sends the mapped payload when the list is not locked`() = runTest {
        coEvery { shoppingRepository.getById("s1") } returns shoppingInProgress
        coEvery { networkService.make(any(), String::class, any()) } returns "p1"

        val result = repository.saveProduct(Product(id = "p1", name = "Arroz", quantity = 1.0), "s1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `saveProduct fails without calling the network when the list is finished`() = runTest {
        coEvery { shoppingRepository.getById("s1") } returns shoppingFinished

        val result = repository.saveProduct(Product(id = "p1", name = "Arroz", quantity = 1.0), "s1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getAllProducts maps the observed network response`() = runTest {
        every {
            networkService.observe<List<ProductModel>>(any(), any(), any())
        } returns flowOf(listOf(productModel))

        val result = repository.getAllProducts("s1")

        val products = result.first()
        assertEquals(1, products.size)
        assertEquals("Arroz", products[0].name)
    }

    @Test
    fun `deleteProduct calls the network when the list is not locked`() = runTest {
        coEvery { shoppingRepository.getById("s1") } returns shoppingInProgress
        coEvery { networkService.make(any(), String::class, any()) } returns "ok"

        val result = repository.deleteProduct("p1", "s1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteProduct fails when the list is finished`() = runTest {
        coEvery { shoppingRepository.getById("s1") } returns shoppingFinished

        val result = repository.deleteProduct("p1", "s1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `updateProduct sends the mapped payload when the list is not locked`() = runTest {
        coEvery { shoppingRepository.getById("s1") } returns shoppingInProgress
        coEvery { networkService.make(any(), String::class, any()) } returns "ok"

        val result = repository.updateProduct(Product(id = "p1", name = "Arroz", quantity = 2.0), "s1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `updateProduct fails when the list is finished`() = runTest {
        coEvery { shoppingRepository.getById("s1") } returns shoppingFinished

        val result = repository.updateProduct(Product(id = "p1", name = "Arroz", quantity = 2.0), "s1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `searchProducts maps each entry from the OpenFoodFacts response`() = runTest {
        val json = Json.parseToJsonElement(
            """
            {"products":[{"product_name":"Arroz Tipo 1","categories":"Grãos,Mercearia","code":"789"}]}
            """.trimIndent()
        ) as JsonObject
        coEvery { cloudNetwork.make(any(), JsonObject::class, any()) } returns json

        val result = repository.searchProducts("arroz")

        assertTrue(result.isSuccess)
        val products = result.getOrThrow()
        assertEquals(1, products.size)
        assertEquals("Arroz Tipo 1", products[0].name)
        assertEquals("789", products[0].barcode)
    }

    @Test
    fun `searchProducts returns an empty list when the response has no products field`() = runTest {
        val json = Json.parseToJsonElement("""{}""") as JsonObject
        coEvery { cloudNetwork.make(any(), JsonObject::class, any()) } returns json

        val result = repository.searchProducts("arroz")

        assertEquals(emptyList<Product>(), result.getOrThrow())
    }

    @Test
    fun `searchProducts fails when the network call throws`() = runTest {
        coEvery { cloudNetwork.make(any(), JsonObject::class, any()) } throws IllegalStateException("offline")

        val result = repository.searchProducts("arroz")

        assertTrue(result.isFailure)
    }

    @Test
    fun `analyzeImage builds products straight from OCR candidates without calling Gemini`() = runTest {
        val bitmap = mockk<Bitmap>()
        coEvery { imageTextRecognizer.recognizePriceTag(bitmap) } returns
            PriceTagResult(nameCandidates = listOf("Arroz Branco"), price = 12.5)

        val result = repository.analyzeImage(bitmap)

        assertTrue(result.isSuccess)
        val products = result.getOrThrow()
        assertEquals(1, products.size)
        assertEquals("Arroz Branco", products[0].name)
        assertEquals(12.5, products[0].price)
    }

    @Test
    fun `analyzeImage fails when OCR throws`() = runTest {
        val bitmap = mockk<Bitmap>()
        coEvery { imageTextRecognizer.recognizePriceTag(bitmap) } throws IllegalStateException("ML Kit unavailable")

        val result = repository.analyzeImage(bitmap)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getSuggestions emits nothing when the shopping list can't be found`() = runTest {
        coEvery { shoppingRepository.getById("s1") } returns null

        val emissions = repository.getSuggestions("s1").toList()

        assertEquals(emptyList<Product>(), emissions)
    }

    @Test
    fun `getQuestionSuggestions always emits the static defaults first`() = runTest {
        coEvery { shoppingRepository.getById("s1") } returns null

        val first = repository.getQuestionSuggestions("s1").first()

        assertEquals(3, first.size)
    }
}
