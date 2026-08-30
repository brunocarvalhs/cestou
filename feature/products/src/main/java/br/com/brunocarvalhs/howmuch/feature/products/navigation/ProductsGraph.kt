package br.com.brunocarvalhs.howmuch.feature.products.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.products.presentation.screen.ProductScreen

@OptIn(ExperimentalMaterial3Api::class)
internal fun NavGraphBuilder.productsGraph(
    navigator: Navigator
) {
    productPickerDestination(navigator)
}

@OptIn(ExperimentalMaterial3Api::class)
private fun NavGraphBuilder.productPickerDestination(navigator: Navigator) {
    dialog<ProductPickerRoute>(
        typeMap = ProductPickerRoute.typeMap
    ) { backStackEntry ->
        val route: ProductPickerRoute = backStackEntry.toRoute()
        ModalBottomSheet(
            onDismissRequest = { navigator.goBack() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ProductScreen(
                shopping = route.shopping,
                onBack = { navigator.goBack() }
            )
        }
    }
}
