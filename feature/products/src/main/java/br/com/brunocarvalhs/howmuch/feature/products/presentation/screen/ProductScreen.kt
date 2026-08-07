package br.com.brunocarvalhs.howmuch.feature.products.presentation.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.common.Options
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.ProductBarcodeForm
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.ProductHeader
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.ProductPhotoForm
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.ProductSearchForm
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.ProductSuggestionForm
import br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel.ProductBarcodeViewModel
import br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel.ProductPhotoViewModel
import br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel.ProductSearchViewModel
import br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel.ProductSuggestionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductScreen(
    shopping: Shopping,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedOption = Options.entries.find { it.name == currentRoute } ?: Options.BARCODE

    val viewModelStoreOwner = LocalViewModelStoreOwner.current!!
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            ProductHeader(
                selectedOption = selectedOption,
                onOptionSelected = { option ->
                    navController.navigate(option.name) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Options.SUGGESTIONS.name,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(Options.BARCODE.name) {
                val viewModel: ProductBarcodeViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ProductBarcodeForm(
                    uiState = uiState,
                    intent = viewModel.intent
                )
            }
            composable(Options.SEARCH.name) {
                val viewModel: ProductSearchViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ProductSearchForm(
                    uiState = uiState,
                    intent = viewModel.intent
                )
            }
            composable(Options.PHOTO.name) {
                val viewModel: ProductPhotoViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ProductPhotoForm(
                    uiState = uiState,
                    intent = viewModel.intent
                )
            }
            composable(Options.SUGGESTIONS.name) {
                val viewModel: ProductSuggestionViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ProductSuggestionForm(
                    uiState = uiState,
                    intent = viewModel.intent,
                    events = viewModel.events,
                    snackbarHostState = snackbarHostState,
                    onBack = onBack
                )
            }
        }
    }
}
