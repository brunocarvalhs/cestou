package br.com.brunocarvalhs.data.services

import br.com.brunocarvalhs.data.model.ShoppingCartModel
import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.services.IDataStorageService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CartLocalStorageTest {

    private val dataStorageService: IDataStorageService = mockk(relaxed = true)
    private val cartLocalStorage = CartLocalStorage(dataStorageService)

    @Test
    fun `saveCart should add cart to history and clear current cart`() = runBlocking {
        // Arrange
        val cart = ShoppingCartModel(id = "cart123")
        val existingHistory = mutableListOf<ShoppingCartModel>()

        coEvery {
            dataStorageService.getValue(
                CartLocalStorage.CART_HISTORY_KEY,
                Array<ShoppingCartModel>::class.java
            )
        } returns existingHistory.toTypedArray()
        coEvery {
            dataStorageService.saveValue<MutableList<ShoppingCartModel>>(
                any(),
                any(),
                any()
            )
        } returns Unit
        coEvery { dataStorageService.removeValue(CartLocalStorage.CART_LOCAL_STORAGE_KEY) } returns Unit

        // Act
        cartLocalStorage.saveCart(cart)

        // Assert
        coVerify {
            dataStorageService.saveValue(CartLocalStorage.CART_HISTORY_KEY, withArg {
                assertEquals(1, it.size)
                assertEquals(cart, it[0])
            }, Array<ShoppingCartModel>::class.java)
        }
        coVerify { dataStorageService.removeValue(CartLocalStorage.CART_LOCAL_STORAGE_KEY) }
    }

    @Test
    fun `getCartHistory should return cart history`() = runBlocking {
        // Arrange
        val cartHistory = listOf(ShoppingCartModel(id = "cart123"))

        coEvery {
            dataStorageService.getValue(
                CartLocalStorage.CART_HISTORY_KEY,
                Array<ShoppingCartModel>::class.java
            )
        } returns cartHistory.toTypedArray()

        // Act
        val result = cartLocalStorage.getCartHistory()

        // Assert
        assertEquals(cartHistory, result)
    }

    @Test
    fun `getCartHistory should return empty list when no history exists`() = runBlocking {
        // Arrange
        coEvery {
            dataStorageService.getValue(
                CartLocalStorage.CART_HISTORY_KEY,
                Array<ShoppingCartModel>::class.java
            )
        } returns null

        // Act
        val result = cartLocalStorage.getCartHistory()

        // Assert
        assertEquals(emptyList<ShoppingCart>(), result)
    }

    @Test
    fun `clearCartHistory should remove cart history`() = runBlocking {
        // Arrange
        coEvery { dataStorageService.removeValue(CartLocalStorage.CART_HISTORY_KEY) } returns Unit

        // Act
        cartLocalStorage.clearCartHistory()

        // Assert
        coVerify { dataStorageService.removeValue(CartLocalStorage.CART_HISTORY_KEY) }
    }

    @Test
    fun `getCartNow should return current cart`() = runBlocking {
        // Arrange
        val cart = ShoppingCartModel(id = "cart123")

        coEvery {
            dataStorageService.getValue(
                CartLocalStorage.CART_LOCAL_STORAGE_KEY,
                ShoppingCartModel::class.java
            )
        } returns cart

        // Act
        val result = cartLocalStorage.getCartNow()

        // Assert
        assertEquals(cart, result)
    }

    @Test
    fun `getCartNow should return null when no current cart exists`() = runBlocking {
        // Arrange
        coEvery {
            dataStorageService.getValue(
                CartLocalStorage.CART_LOCAL_STORAGE_KEY,
                ShoppingCartModel::class.java
            )
        } returns null

        // Act
        val result = cartLocalStorage.getCartNow()

        // Assert
        assertNull(result)
    }

    @Test
    fun `saveCartNow should save current cart`() = runBlocking {
        // Arrange
        val cart = ShoppingCartModel(id = "cart123")

        coEvery {
            dataStorageService.saveValue<ShoppingCartModel>(
                any(),
                any(),
                any()
            )
        } returns Unit

        // Act
        cartLocalStorage.saveCartNow(cart)

        // Assert
        coVerify {
            dataStorageService.saveValue(
                CartLocalStorage.CART_LOCAL_STORAGE_KEY,
                cart,
                ShoppingCartModel::class.java
            )
        }
    }
}
