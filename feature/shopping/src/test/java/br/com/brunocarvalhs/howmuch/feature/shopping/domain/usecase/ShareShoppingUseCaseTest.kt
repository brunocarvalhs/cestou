package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShareShoppingUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ShareShoppingUseCaseTest {

    private val productsUseCase = mockk<ProductsUseCase>()
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val useCase = ShareShoppingUseCase(context, productsUseCase)

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap()
    )

    @Test
    fun `invoke starts a share chooser activity with the shopping list content`() = runTest {
        coEvery { productsUseCase(shopping.id) } returns flowOf(
            listOf(Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0, isPurchased = true))
        )

        useCase(shopping)

        val started = Shadows.shadowOf(context as Application).nextStartedActivity
        assert(started != null) { "Expected a share chooser Intent to be started" }
    }
}
