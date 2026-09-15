package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.ui.entity.ProductCategory
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductDuplicateCheckUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.navigation.ProductPickerRoute
import br.com.brunocarvalhs.howmuch.feature.products.presentation.intent.ProductFormIntent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.ProductFormUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val SOURCE_FORM = "form"
private const val MAX_PRICE_LENGTH = 12

/**
 * Backs [Options.FORM], the layout's main/default entry point: a full name + quantity + price
 * form, complementing [QuickAddViewModel]'s name-only flow with a complete [Product] on save
 * (reuses [ProductSaveUseCase]'s `(Product, shoppingId)` overload).
 */
@HiltViewModel
internal class ProductFormViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val productSaveUseCase: ProductSaveUseCase,
    private val productDuplicateCheckUseCase: ProductDuplicateCheckUseCase,
    private val userRepository: UserRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val shopping = savedStateHandle.toRoute<ProductPickerRoute>(ProductPickerRoute.typeMap).shopping

    private val _uiState = MutableStateFlow(ProductFormUiState())
    val uiState = _uiState.asStateFlow()

    val intent = ProductFormIntent(
        onNameChange = { name ->
            _uiState.update { state ->
                // Keep suggesting live while typing, but stop overriding once the user has
                // picked a category themselves via the picker dialog.
                val category = if (state.isCategoryManuallySet) {
                    state.category
                } else {
                    ProductCategory.suggestFromProductName(name)
                        ?.let { context.getString(it.displayNameRes) }
                        ?: state.category
                }
                state.copy(name = name, category = category)
            }
        },
        onQuantityChange = { quantity ->
            if (quantity.all(Char::isDigit)) _uiState.update { it.copy(quantity = quantity) }
        },
        onPriceChange = { price ->
            if (price.all(Char::isDigit)) _uiState.update { it.copy(price = price.take(MAX_PRICE_LENGTH)) }
        },
        onCategorySelected = { category ->
            _uiState.update { it.copy(category = category, isCategoryManuallySet = true) }
        },
        onSave = { save() },
        onSaveErrorShown = { _uiState.update { it.copy(saveError = null) } },
        onDuplicateWarningShown = { _uiState.update { it.copy(duplicateWarning = null) } }
    )

    private fun save() {
        val state = _uiState.value
        // Guards both a repeated tap on Save and a re-submit while the previous one is still
        // in flight (same double-fire guard QuickAddViewModel uses).
        if (state.isSaving || !state.isValid) return

        val name = state.name.trim()
        val quantity = state.quantityValue ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            // Informational only (spec item-add-authorship P2, IAA-02) — the item is saved
            // either way, same as QuickAddViewModel/ProductSaveUseCase's AI path.
            val duplicate = productDuplicateCheckUseCase(name, shopping.id)

            val product = Product(
                id = UUID.randomUUID().toString(),
                name = name,
                quantity = quantity,
                price = state.priceValue,
                category = state.category.ifBlank { context.getString(ProductCategory.OUTROS.displayNameRes) }
            )
            val result = productSaveUseCase(product, shopping.id)

            if (result.isSuccess) {
                analyticsTracker.trackEvent(
                    AnalyticsEvents.PRODUCT_ADDED,
                    mapOf(
                        AnalyticsParams.SHOPPING_ID to shopping.id,
                        AnalyticsParams.SOURCE to SOURCE_FORM
                    )
                )
            }

            _uiState.update {
                if (result.isSuccess) {
                    ProductFormUiState(
                        duplicateWarning = duplicate?.let { match ->
                            duplicateWarningMessage(match.addedBy, match.name)
                        }
                    )
                } else {
                    it.copy(isSaving = false, saveError = context.getString(R.string.quick_add_save_error, name))
                }
            }
        }
    }

    private suspend fun duplicateWarningMessage(addedByUserId: String?, name: String): String {
        val adderName = addedByUserId?.let { userRepository.getUserProfile(it).first()?.name }
        return if (adderName != null) {
            context.getString(R.string.quick_add_duplicate_warning_by, name, adderName)
        } else {
            context.getString(R.string.quick_add_duplicate_warning, name)
        }
    }
}
