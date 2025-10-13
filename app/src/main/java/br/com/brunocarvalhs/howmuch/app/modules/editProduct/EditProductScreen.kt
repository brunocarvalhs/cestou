package br.com.brunocarvalhs.howmuch.app.modules.editProduct

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.trackClick
import br.com.brunocarvalhs.howmuch.app.modules.products.components.PriceInput
import br.com.brunocarvalhs.howmuch.app.modules.products.components.ProductNameInput
import br.com.brunocarvalhs.howmuch.app.modules.products.components.QuantitySelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    navController: NavController,
    viewModel: EditProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = {
            navController.popBackStack()
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        EditProductContent(
            uiState = uiState,
            onIntent = viewModel::onIntent,
            navController = navController
        )
    }
}

@Composable
private fun EditProductContent(
    uiState: EditProductUiState,
    onIntent: (EditProductUiIntent) -> Unit,
    navController: NavController
) {
    val nameFocusRequester = remember { FocusRequester() }
    val priceFocusRequester = remember { FocusRequester() }

    var name by remember { mutableStateOf(uiState.name) }
    var price by remember { mutableLongStateOf(uiState.price) }
    var quantity by remember { mutableIntStateOf(uiState.quantity) }

    LaunchedEffect(Unit) {
        if (uiState.isEditName) {
            nameFocusRequester.requestFocus()
        } else if (uiState.isEditPrice) {
            priceFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ProductNameInput(
            enabled = uiState.isEditName,
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
        PriceInput(
            enabled = uiState.isEditPrice,
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
        QuantitySelector(
            enabled = uiState.isEditQuantity,
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
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onIntent(
                    EditProductUiIntent.Save(
                        price = price,
                    )
                )
                navController.popBackStack()
            }
        ) {
            Text(text = stringResource(id = R.string.action_confirm))
        }
    }
}

@Composable
@Preview
private fun PriceFieldBottomSheetPreview() {
    EditProductContent(
        uiState = EditProductUiState(),
        onIntent = {},
        navController = rememberNavController()
    )
}
