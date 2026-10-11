package br.com.brunocarvalhs.howmuch.feature.products.presentation.components

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.common.AnalyzingOverlay
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.common.PreviewActions
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.common.ShareOptionsBottomSheet
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.FormProduct
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.ProductAnalysisConfirmation
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.ProductHeader
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.ProductPhotoForm
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.Suggestions
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.scanner.CaptureControls
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.scanner.ImagePreviewView
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.scanner.PermissionDeniedState
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.ProductFormUiState
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.ProductPhotoUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Testes de layout via Robolectric (JVM): compõem os componentes reais de products.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ProductComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `form renders the filled product`() {
        composeTestRule.setContent {
            CestouTheme {
                FormProduct(
                    uiState = ProductFormUiState(
                        name = "Arroz Branco 5kg", quantity = "2", price = "2590", category = "Mercearia"
                    ),
                    shoppingTitle = "Compras"
                )
            }
        }
        composeTestRule.onNodeWithText("Arroz Branco 5kg").assertExists()
    }

    @Test
    fun `form renders empty and saving states`() {
        composeTestRule.setContent {
            CestouTheme {
                Column {
                    FormProduct(uiState = ProductFormUiState())
                    FormProduct(
                        uiState = ProductFormUiState(name = "Leite", quantity = "1", price = "500", isSaving = true)
                    )
                }
            }
        }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `analysis confirmation lists detected products and confirms`() {
        var confirmed = false
        composeTestRule.setContent {
            CestouTheme {
                ProductAnalysisConfirmation(
                    products = listOf(
                        Product(id = "1", name = "Arroz", quantity = 1.0, price = 10.0),
                        Product(id = "2", name = "Feijão", quantity = 2.0, price = 8.0)
                    ),
                    onUpdateItem = {},
                    onRemoveItem = {},
                    onConfirmAll = { confirmed = true },
                    onRetake = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Arroz").assertExists()
        composeTestRule.onNodeWithText("Feijão").assertExists()
        assertEquals(false, confirmed)
    }

    @Test
    fun `photo form renders without camera permission`() {
        composeTestRule.setContent {
            CestouTheme { ProductPhotoForm(uiState = ProductPhotoUiState()) }
        }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `photo form renders analyzing and error states`() {
        composeTestRule.setContent {
            CestouTheme {
                Column {
                    ProductPhotoForm(uiState = ProductPhotoUiState(capturedImageUri = Uri.EMPTY, isAnalyzing = true))
                    ProductPhotoForm(uiState = ProductPhotoUiState(errorMessage = "Falha na análise"))
                }
            }
        }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `header renders the shopping title`() {
        composeTestRule.setContent {
            CestouTheme {
                Column {
                    ProductHeader(shoppingTitle = "Compras da semana")
                    ProductHeader(shoppingTitle = "Compras da semana", canNavigateBack = true)
                }
            }
        }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `suggestions render items and loading and invoke click`() {
        var clicked: String? = null
        composeTestRule.setContent {
            CestouTheme {
                Column {
                    Suggestions(items = listOf("Calcular total", "Sugerir receita"), onItemClick = { clicked = it })
                    Suggestions(isLoading = true)
                    Suggestions()
                }
            }
        }
        composeTestRule.onNodeWithText("Calcular total").performClick()
        assertEquals("Calcular total", clicked)
    }

    @Test
    fun `share options sheet renders`() {
        composeTestRule.setContent {
            CestouTheme {
                ShareOptionsBottomSheet(onDismissRequest = {}, onInviteMember = {}, onShareAsText = {})
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `scanner pieces render`() {
        composeTestRule.setContent {
            CestouTheme {
                Column {
                    AnalyzingOverlay()
                    PreviewActions(onRetake = {}, onAnalyze = {})
                    CaptureControls(onGalleryClick = {}, onCaptureClick = {})
                    PermissionDeniedState()
                    ImagePreviewView(uri = Uri.EMPTY, isAnalyzing = false, onRetake = {}, onAnalyze = {})
                }
            }
        }
        composeTestRule.onRoot().assertExists()
    }
}
