package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductDuplicateCheckUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.products.navigation.ProductPickerRoute
import br.com.brunocarvalhs.howmuch.feature.products.presentation.intent.QuickAddIntent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.QuickAddUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs [Options.QUICK_ADD] (`.specs/features/item-add-authorship/design.md`): a lightweight
 * mini header showing the list's running total/budget, plus the free-text "type and add" field.
 *
 * The total is observed directly from [ProductsUseCase] (same collection pattern
 * `ShoppingDuplicateUseCase` already uses) rather than reaching into `CartViewModel` — `AD-005`
 * keeps feature internals from reaching across module/ViewModel boundaries, and `ProductScreen` is
 * a separate `ModalBottomSheet`/ViewModel scope from `CartScreen`.
 */
@HiltViewModel
internal class QuickAddViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val productsUseCase: ProductsUseCase,
    private val productSaveUseCase: ProductSaveUseCase,
    private val productDuplicateCheckUseCase: ProductDuplicateCheckUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val shopping = savedStateHandle.toRoute<ProductPickerRoute>(ProductPickerRoute.typeMap).shopping

    private val _uiState = MutableStateFlow(QuickAddUiState(budget = shopping.budget))
    val uiState = _uiState.asStateFlow()

    val intent = QuickAddIntent(
        onNewItemNameChange = { name -> _uiState.update { it.copy(newItemName = name, duplicateWarning = null) } },
        onSubmit = { submit() },
        onDuplicateWarningShown = { _uiState.update { it.copy(duplicateWarning = null) } },
        onSaveErrorShown = { _uiState.update { it.copy(saveError = null) } }
    )

    init {
        viewModelScope.launch {
            productsUseCase(shopping.id).collect { products ->
                _uiState.update { it.copy(totalAmount = products.sumOf { product -> product.total }) }
            }
        }
    }

    private fun submit() {
        // Guards both the keyboard "Done" action and the Add icon from double-firing a save
        // while the previous one is still in flight (e.g. double-tap, or Done then Add).
        if (_uiState.value.isSaving) return

        val name = _uiState.value.newItemName.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            // Same duplicate-check use case the AI path uses (IAA-02) — no hard block, the item
            // is saved either way, the warning is purely informational (spec P2 AC2).
            val duplicate = productDuplicateCheckUseCase(name, shopping.id)

            val result = productSaveUseCase(name = name, quantity = 1.0, shoppingId = shopping.id)

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        newItemName = "",
                        isSaving = false,
                        duplicateWarning = duplicate?.let { match ->
                            duplicateWarningMessage(match.addedBy, match.name)
                        }
                    )
                } else {
                    // Save failed (network error, repository exception, ...) — surface it instead
                    // of silently clearing the field as if the item had been added.
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
