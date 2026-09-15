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
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.components.CategoryPickerDialog
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.common.Options
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.FormProduct
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.ProductHeader
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.ProductPhotoForm
import br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel.ProductFormViewModel
import br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel.ProductPhotoViewModel

private const val CATEGORY_PICKER_ROUTE = "product_form/category_picker"

/**
 * One natural flow, no tab picker: [Options.FORM] (manual entry) is the only start destination,
 * with a camera shortcut pushing [Options.PHOTO] on top. Quick Add / Search / Suggestions / AI
 * chat still exist as composables+ViewModels elsewhere in this module — they're just not wired
 * into this NavHost anymore (recipe/AI surfaces are moving to their own modules; the rest can be
 * re-wired here later if this screen ever needs more than manual-or-camera).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductScreen(
    shopping: Shopping,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    // Checked against Photo specifically (not "route != Form") so the category picker dialog —
    // which also pushes a back-stack entry — doesn't cause the Form step's own header to
    // reappear behind it.
    val canNavigateBack = navBackStackEntry?.destination?.route == Options.PHOTO.name

    val viewModelStoreOwner = LocalViewModelStoreOwner.current!!
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            // The Form step is a plain bottom-sheet form (mirrors EditItemContent: no app bar,
            // dismiss via the sheet's own drag handle/backdrop tap). Only the camera step is a
            // genuine full-screen surface, so it's the only one that needs a back affordance.
            if (canNavigateBack) {
                ProductHeader(
                    shoppingTitle = shopping.title,
                    canNavigateBack = true,
                    onBack = { navController.popBackStack() }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Options.FORM.name,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(Options.FORM.name) {
                val viewModel: ProductFormViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                FormProduct(
                    uiState = uiState,
                    intent = viewModel.intent,
                    snackbarHostState = snackbarHostState,
                    shoppingTitle = shopping.title,
                    onNavigateToPhoto = {
                        navController.navigate(Options.PHOTO.name) {
                            launchSingleTop = true
                        }
                    },
                    onOpenCategoryPicker = {
                        navController.navigate(CATEGORY_PICKER_ROUTE) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Options.PHOTO.name) {
                val viewModel: ProductPhotoViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ProductPhotoForm(
                    uiState = uiState,
                    intent = viewModel.intent,
                    snackbarHostState = snackbarHostState
                )
            }
            dialog(CATEGORY_PICKER_ROUTE) {
                // Same ProductFormViewModel instance as the Form step (shared viewModelStoreOwner),
                // so picking a category here writes straight back into the form's own state.
                val viewModel: ProductFormViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                CategoryPickerDialog(
                    selected = uiState.category,
                    onSelect = { category ->
                        viewModel.intent.onCategorySelected(category)
                        navController.popBackStack()
                    },
                    onDismiss = { navController.popBackStack() }
                )
            }
        }
    }
}

@Preview
@Composable
private fun ProductScreenPreview() {
    ProductScreen(
        shopping = Shopping(
            id = "1",
            title = "Supermercado",
            description = "Compras do mês",
            price = 0.0,
            status = Shopping.Status.NEW,
            users = emptyList(),
            roles = emptyMap()
        )
    )
}
