package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouCard
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.model.Recipe
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.intent.ProductSearchIntent
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.state.ProductSearchUiState
import coil.compose.AsyncImage
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

@Composable
internal fun ProductSearchForm(
    modifier: Modifier = Modifier,
    uiState: ProductSearchUiState,
    intent: ProductSearchIntent = ProductSearchIntent(),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.searchMode == ProductSearchUiState.SearchMode.PRODUCT,
                    onClick = { intent.onSearchModeChange(ProductSearchUiState.SearchMode.PRODUCT) },
                    label = { Text(stringResource(R.string.product_search_products_label)) },
                    leadingIcon = { Icon(Icons.Default.ShoppingBag, null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = uiState.searchMode == ProductSearchUiState.SearchMode.RECIPE,
                    onClick = { intent.onSearchModeChange(ProductSearchUiState.SearchMode.RECIPE) },
                    label = { Text(stringResource(R.string.product_search_recipes_label)) },
                    leadingIcon = { Icon(Icons.Default.Restaurant, null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.query,
                onValueChange = { intent.onQueryChange(it) },
                singleLine = true,
                shape = RoundedCornerShape(32.dp),
                placeholder = {
                    val placeholderRes = if (uiState.searchMode == ProductSearchUiState.SearchMode.PRODUCT) {
                        R.string.product_search_placeholder
                    } else {
                        R.string.recipe_search_placeholder
                    }
                    Text(stringResource(placeholderRes))
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, null)
                }
            )
        }

        if (uiState.query.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    val emptyTextRes = if (uiState.searchMode == ProductSearchUiState.SearchMode.PRODUCT) {
                        R.string.product_search_empty_text
                    } else {
                        R.string.recipe_search_empty_text
                    }
                    Text(
                        text = stringResource(emptyTextRes),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.product_search_ai_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(40.dp))

                    val iconImageVector = if (uiState.searchMode == ProductSearchUiState.SearchMode.PRODUCT) {
                        Icons.Default.CameraAlt
                    } else {
                        Icons.Default.Restaurant
                    }
                    Icon(
                        imageVector = iconImageVector,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            if (uiState.searchMode == ProductSearchUiState.SearchMode.PRODUCT) {
                items(uiState.results.size) { index ->
                    val product = uiState.results[index]
                    ProductItem(
                        name = product.name,
                        onClick = { intent.onProductSelected(product) }
                    )
                }
            } else {
                items(uiState.recipes.size) { index ->
                    val recipe = uiState.recipes[index]
                    RecipeSearchItem(
                        recipe = recipe,
                        onClick = { intent.onRecipeSelected(recipe) }
                    )
                }
            }
        }
    }

    if (uiState.selectedRecipe != null) {
        RecipeDetailsDialog(
            recipe = uiState.selectedRecipe,
            onDismiss = { intent.onClearRecipeSelection() },
            onConfirm = { intent.onAddRecipeIngredients(uiState.selectedRecipe) }
        )
    }
}

@Composable
private fun RecipeSearchItem(
    recipe: Recipe,
    onClick: () -> Unit
) {
    CestouCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

private const val DIALOG_MAX_WIDTH_FRACTION = 0.95f

@Composable
private fun RecipeDetailsDialog(
    recipe: Recipe,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(DIALOG_MAX_WIDTH_FRACTION)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    AsyncImage(
                        model = recipe.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    Surface(
                        modifier = Modifier.padding(12.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(24.dp))
                    
                    Text(
                        text = stringResource(R.string.recipe_details_ingredients),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    recipe.ingredients.forEach { ingredient ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Check, 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = ingredient.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    if (!recipe.instructions.isNullOrBlank()) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.recipe_details_instructions),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = recipe.instructions,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                        )
                    }

                    Spacer(Modifier.height(32.dp))
                    
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(stringResource(CoreR.string.action_close))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onConfirm,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(stringResource(R.string.recipe_details_add_to_list))
                        }
                    }
                }
            }
        }
    }
}

private val previewSearchResults = listOf(
    Product(id = "1", name = "Arroz Branco 5kg", quantity = 1.0, price = 25.9),
    Product(id = "2", name = "Feijão Carioca 1kg", quantity = 1.0, price = 8.5)
)

private val previewRecipe = Recipe(
    id = "1",
    name = "Arroz com feijão",
    description = "Prato tradicional brasileiro",
    instructions = "Cozinhe o arroz e o feijão separadamente e sirva juntos.",
    ingredients = previewSearchResults
)

@Preview(showBackground = true, name = "Vazio - Produtos")
@Composable
private fun ProductSearchFormEmptyPreview() {
    ProductSearchForm(
        uiState = ProductSearchUiState()
    )
}

@Preview(showBackground = true, name = "Vazio - Receitas")
@Composable
private fun ProductSearchFormEmptyRecipesPreview() {
    ProductSearchForm(
        uiState = ProductSearchUiState(searchMode = ProductSearchUiState.SearchMode.RECIPE)
    )
}

@Preview(showBackground = true, name = "Resultados - Produtos")
@Composable
private fun ProductSearchFormResultsPreview() {
    ProductSearchForm(
        uiState = ProductSearchUiState(
            query = "arroz",
            results = previewSearchResults
        )
    )
}

@Preview(showBackground = true, name = "Resultados - Receitas")
@Composable
private fun ProductSearchFormRecipesPreview() {
    ProductSearchForm(
        uiState = ProductSearchUiState(
            query = "arroz com feijão",
            searchMode = ProductSearchUiState.SearchMode.RECIPE,
            recipes = listOf(previewRecipe)
        )
    )
}

@Preview(showBackground = true, name = "Detalhes da receita")
@Composable
private fun ProductSearchFormRecipeDetailsPreview() {
    ProductSearchForm(
        uiState = ProductSearchUiState(
            query = "arroz com feijão",
            searchMode = ProductSearchUiState.SearchMode.RECIPE,
            recipes = listOf(previewRecipe),
            selectedRecipe = previewRecipe
        )
    )
}
