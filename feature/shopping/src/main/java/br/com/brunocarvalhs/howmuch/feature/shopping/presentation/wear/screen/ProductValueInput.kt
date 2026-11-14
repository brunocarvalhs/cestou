package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatPrice
import br.com.brunocarvalhs.howmuch.feature.shopping.R

internal const val TAG_DETAIL_PRICE_FIELD = "wear_detail_price_field"
internal const val TAG_DETAIL_QUANTITY_FIELD = "wear_detail_quantity_field"
internal const val TAG_DETAIL_SAVE_VALUE = "wear_detail_save_value"

/**
 * Produto sem valor pede o valor no toque, em vez de marcar como comprado. Preço zero também conta
 * como sem valor, porque é o que o item recebe quando é criado pelo relógio.
 */
internal val Product.hasValue: Boolean
    get() = (price ?: 0.0) > 0.0

/** Aceita vírgula ou ponto como separador decimal. Texto vazio ou inválido vira null. */
internal fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

/** Limite de dígitos do preço: 9 dígitos são 9.999.999,99, bem acima de qualquer compra. */
private const val MAX_PRICE_DIGITS = 9

/** A máscara guarda centavos: 100 centavos formam 1 real. */
private const val CENTS_PER_UNIT = 100.0

/**
 * Máscara de preço: só os dígitos digitados contam, e o último dígito é sempre os centavos.
 * Digitar 1250 mostra R$ 12,50; apagar um dígito desloca a vírgula de volta.
 */
internal fun priceDigitsOf(text: String): String = text.filter { it.isDigit() }.take(MAX_PRICE_DIGITS)

/** Valor em reais a partir dos dígitos da máscara (centavos); vazio ou zero vira null. */
internal fun priceFromDigits(digits: String): Double? =
    digits.toLongOrNull()?.takeIf { it > 0L }?.div(CENTS_PER_UNIT)

/**
 * Entrada de valor e quantidade de um produto sem valor. Só habilita "Salvar" com preço e
 * quantidade maiores que zero. Ao salvar, devolve o produto com esses dois campos preenchidos.
 */
@Composable
internal fun ProductValueInput(
    product: Product,
    onSave: (Product) -> Unit,
    onCancel: () -> Unit
) {
    var priceDigits by rememberSaveable(product.id) { mutableStateOf("") }
    var quantityText by rememberSaveable(product.id) { mutableStateOf(product.quantity.toPlainText()) }
    val price = priceFromDigits(priceDigits)
    val quantity = parseDecimal(quantityText)?.takeIf { it > 0.0 }
    val priceText = price?.formatPrice().orEmpty()
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                Text(
                    text = product.name,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                )
            }

            item {
                Text(
                    text = stringResource(R.string.shopping_wear_detail_value_title),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec)
                )
            }

            item {
                WearSearchField(
                    query = priceText,
                    onQueryChange = { priceDigits = priceDigitsOf(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    hint = stringResource(R.string.shopping_wear_detail_price_hint),
                    tag = TAG_DETAIL_PRICE_FIELD,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            item {
                WearSearchField(
                    query = quantityText,
                    onQueryChange = { quantityText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    hint = stringResource(R.string.shopping_wear_detail_quantity_hint),
                    tag = TAG_DETAIL_QUANTITY_FIELD,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            item {
                Button(
                    onClick = {
                        if (price != null && quantity != null) {
                            onSave(product.copy(price = price, quantity = quantity))
                        }
                    },
                    enabled = price != null && quantity != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TAG_DETAIL_SAVE_VALUE)
                        .transformedHeight(this, spec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(spec),
                    colors = wearCardColors()
                ) {
                    Text(stringResource(R.string.shopping_wear_detail_save))
                }
            }

            item {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec)
                ) {
                    Text(stringResource(R.string.shopping_wear_detail_cancel))
                }
            }
        }
    }
}

/** Quantidade inteira sem casas decimais; fracionária mantém o ponto, que o campo também aceita. */
private fun Double.toPlainText(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()
