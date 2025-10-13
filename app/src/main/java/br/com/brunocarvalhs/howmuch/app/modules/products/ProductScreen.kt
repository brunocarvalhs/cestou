package br.com.brunocarvalhs.howmuch.app.modules.products

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.AnalyticsEvent
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.AnalyticsEvents.trackEvent
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.AnalyticsParam
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.trackClick
import br.com.brunocarvalhs.howmuch.app.foundation.annotations.DevicesPreview
import br.com.brunocarvalhs.howmuch.app.foundation.constants.EMPTY_LONG
import br.com.brunocarvalhs.howmuch.app.foundation.constants.EMPTY_STRING
import br.com.brunocarvalhs.howmuch.app.foundation.constants.ONE_INT
import br.com.brunocarvalhs.howmuch.app.modules.products.components.PriceInput
import br.com.brunocarvalhs.howmuch.app.modules.products.components.ProductNameInput
import br.com.brunocarvalhs.howmuch.app.modules.products.components.QuantitySelector
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    shoppingCartId: String?,
    isProductListed: Boolean = false,
    navController: NavController,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiEffect by viewModel.uiEffect.collectAsState(initial = null)

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        trackEvent(
            AnalyticsEvent.SCREEN_VIEW,
            mapOf(
                AnalyticsParam.SCREEN_NAME to "ProductFormBottomSheet",
                AnalyticsParam.SHOPPING_CART_ID to (shoppingCartId ?: "unknown")
            )
        )
    }

    LaunchedEffect(uiEffect) {
        uiEffect?.let { effect ->
            when (effect) {
                is ProductUiEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                is ProductUiEffect.ProductAdded -> {
                    scope.launch {
                        sheetState.hide()
                        navController.popBackStack()
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        sheetState = sheetState,
        onDismissRequest = {
            navController.popBackStack()
            trackClick(
                viewId = "product_sheet_dismiss",
                viewName = "Dismiss Product Sheet",
                screenName = "ProductFormBottomSheet"
            )
        },
    ) {
        ProductContent(
            isProductListed = isProductListed,
            onIntent = viewModel::onIntent
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductContent(
    isProductListed: Boolean = false,
    onIntent: (ProductUiIntent) -> Unit
) {
    val nameFocusRequester = remember { FocusRequester() }
    val priceFocusRequester = remember { FocusRequester() }

    var name by remember { mutableStateOf(EMPTY_STRING) }
    var price by remember { mutableLongStateOf(EMPTY_LONG) }
    var quantity by remember { mutableIntStateOf(ONE_INT) }

    LaunchedEffect(Unit) {
        nameFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ProductNameInput(
            name = name,
            onNameChange = { newName ->
                name = newName
                trackClick(
                    viewId = "input_product_name",
                    viewName = "Product Name Changed",
                    screenName = "ProductFormBottomSheet"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocusRequester)
        )
        if (!isProductListed) {
            PriceInput(
                price = price,
                onPriceChange = { newPrice ->
                    price = newPrice
                    trackClick(
                        viewId = "input_product_price",
                        viewName = "Product Price Changed",
                        screenName = "ProductFormBottomSheet"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(priceFocusRequester)
            )
        }
        QuantitySelector(
            quantity = quantity,
            modifier = Modifier.fillMaxWidth(),
            onQuantityChange = { newQuantity ->
                quantity = newQuantity
                trackClick(
                    viewId = "input_product_quantity",
                    viewName = "Product Quantity Changed",
                    screenName = "ProductFormBottomSheet"
                )
            }
        )

        Button(
            onClick = {
                onIntent(
                    if (isProductListed) {
                        ProductUiIntent.AddProductToList(
                            name = name,
                            quantity = quantity
                        )
                    } else {
                        ProductUiIntent.AddProductToCart(
                            name = name,
                            price = price,
                            quantity = quantity
                        )
                    }
                )
                name = EMPTY_STRING
                price = EMPTY_LONG
                quantity = ONE_INT
                nameFocusRequester.requestFocus()
                trackClick(
                    viewId = "btn_add_product",
                    viewName = "Submit Product",
                    screenName = "ProductFormBottomSheet"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.add_product))
        }
    }
}

@Composable
@DevicesPreview
fun ProductContentPreview() {
    ProductContent(
        onIntent = {}
    )
}
