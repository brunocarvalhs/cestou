package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyVisualTransformation
import br.com.brunocarvalhs.howmuch.feature.shopping.R

private const val CURRENCY_DIVISOR = 100.0
private const val MAX_PRICE_LENGTH = 12
private const val CARD_ALPHA = 0.5f

@Composable
internal fun EditShoppingContent(
    shopping: Shopping,
    onSave: (Shopping) -> Unit,
    onCancel: () -> Unit,
    onShareToken: () -> Unit = {},
    sharingToken: String? = null
) {
    var title by remember { mutableStateOf(shopping.title) }
    var description by remember { mutableStateOf(shopping.description) }
    var isCategorized by remember { mutableStateOf(shopping.isCategorized) }
    var price by remember { mutableStateOf((shopping.price * CURRENCY_DIVISOR).toLong().toString()) }
    val currentBudget = shopping.budget
    var budget by remember {
        mutableStateOf(
            if (currentBudget != null) (currentBudget * CURRENCY_DIVISOR).toLong().toString() else ""
        )
    }

    val visualTransformation = rememberCurrencyVisualTransformation()
    val currencyFormatter = rememberCurrencyFormatter()
    val currencySymbol = remember(currencyFormatter) {
        currencyFormatter.currency?.symbol ?: "R$"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.shopping_management_edit_list_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.shopping_management_label_list_title)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.shopping_management_label_description)) },
            placeholder = { Text(stringResource(R.string.shopping_management_placeholder_description)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (shopping.status == Shopping.Status.FINISH) {
            OutlinedTextField(
                value = price,
                onValueChange = {
                    if (it.all { char -> char.isDigit() }) {
                        price = it.take(MAX_PRICE_LENGTH)
                    }
                },
                label = { Text(stringResource(R.string.shopping_management_label_total_paid)) },
                prefix = { Text(currencySymbol) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = visualTransformation,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = budget,
            onValueChange = {
                if (it.all { char -> char.isDigit() }) {
                    budget = it.take(MAX_PRICE_LENGTH)
                }
            },
            label = { Text(stringResource(R.string.shopping_management_label_budget)) },
            prefix = { Text(currencySymbol) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(12.dp),
            supportingText = { Text(stringResource(R.string.shopping_management_budget_hint)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.shopping_management_label_categorize),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.shopping_management_categorize_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isCategorized,
                onCheckedChange = { isCategorized = it }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = stringResource(R.string.shopping_management_label_members),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (sharingToken == null) {
            Button(
                onClick = onShareToken,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.shopping_management_button_generate_token))
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = CARD_ALPHA)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.shopping_management_token_title),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = sharingToken,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.shopping_management_token_hint),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(CoreR.string.action_cancel))
            }
            Button(
                onClick = {
                    onSave(
                        shopping.copy(
                            title = title,
                            description = description,
                            isCategorized = isCategorized,
                            price = price.toDoubleOrNull()?.div(CURRENCY_DIVISOR) ?: shopping.price,
                            budget = budget.toDoubleOrNull()?.div(CURRENCY_DIVISOR)
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = title.isNotBlank()
            ) {
                Text(stringResource(CoreR.string.action_save))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Preview(showBackground = true)
@Composable
private fun EditShoppingContentPreview() {
    MaterialTheme {
        EditShoppingContent(
            shopping = Shopping(
                id = "1",
                title = "Lista de Teste",
                description = "Descrição da lista",
                price = 150.0,
                status = Shopping.Status.NEW,
                users = listOf("user1"),
                roles = mapOf("user1" to "OWNER")
            ),
            onSave = {},
            onCancel = {}
        )
    }
}
