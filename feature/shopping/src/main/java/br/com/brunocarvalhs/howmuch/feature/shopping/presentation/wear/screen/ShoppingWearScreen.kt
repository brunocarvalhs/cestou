package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatPrice
import br.com.brunocarvalhs.howmuch.core.ui.utils.LocalCurrency
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.WearHomeIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.WearHomeUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearHomeViewModel

private const val SKELETON_ROWS = 3
private const val TAG_SEARCH = "wear_home_search"
private const val TAG_MENU = "wear_home_menu"
private const val TAG_CREATE_ON_PHONE = "wear_home_create_on_phone"
private const val TAG_MORE = "wear_home_more_button"
private const val TAG_SKELETON = "wear_home_skeleton"

@Composable
internal fun ShoppingWearScreen(
    viewModel: WearHomeViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ShoppingWearContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToSearch = onNavigateToSearch,
        onNavigateToMenu = onNavigateToMenu,
        onNavigateToSettings = onNavigateToSettings
    )
}

@Composable
private fun ShoppingWearContent(
    state: WearHomeUiState,
    onIntent: (WearHomeIntent) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val lists = state.lists.items

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            // Pílulas do topo com ícone: lupa (Pesquisar) e menu (Histórico) lado a lado.
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, spec),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.weight(1f).testTag(TAG_SEARCH),
                        colors = wearCardColors(),
                        transformation = SurfaceTransformation(spec)
                    ) {
                        WearPillIcon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.shopping_wear_home_search)
                        )
                    }
                    Button(
                        onClick = onNavigateToMenu,
                        modifier = Modifier.weight(1f).testTag(TAG_MENU),
                        colors = wearCardColors(),
                        transformation = SurfaceTransformation(spec)
                    ) {
                        WearPillIcon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.shopping_wear_menu)
                        )
                    }
                }
            }

            if (state.isLoading) {
                items(SKELETON_ROWS) {
                    WearSkeletonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, spec)
                            .testTag(TAG_SKELETON)
                    )
                }
            } else {
                items(lists.size, key = { lists[it].id }) { index ->
                    val shopping = lists[index]
                    WearHomeRow(
                        shopping = shopping,
                        onClick = { onNavigateToDetail(shopping.id) },
                        spec = spec,
                        scope = this
                    )
                }

                if (lists.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.shopping_wear_no_lists),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Criar lista é feito no celular (Q3); o botão só leva até lá.
                item {
                    Button(
                        onClick = { onIntent(WearHomeIntent.CreateListOnPhone) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, spec)
                            .testTag(TAG_CREATE_ON_PHONE),
                        transformation = SurfaceTransformation(spec)
                    ) {
                        Text(stringResource(R.string.shopping_wear_create_on_phone))
                    }
                }

                if (state.phoneUnavailable) {
                    item {
                        Text(
                            text = stringResource(R.string.shopping_wear_phone_unavailable),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (state.hasMore) {
                    item {
                        OutlinedButton(
                            onClick = { onIntent(WearHomeIntent.ToggleShowAll) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, spec)
                                .testTag(TAG_MORE),
                            transformation = SurfaceTransformation(spec)
                        ) {
                            Text(
                                stringResource(
                                    if (state.showAll) R.string.shopping_wear_less else R.string.shopping_wear_more
                                )
                            )
                        }
                    }
                }

                // Fim da home, como o "Configurações" do Spotify (Q14).
                item {
                    OutlinedButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec)
                    ) {
                        Text(stringResource(R.string.shopping_wear_settings))
                    }
                }
            }
        }
    }
}

/**
 * Ícone de pílula centralizado no botão. Ocupa a largura toda para o alinhamento
 * não depender do conteúdo padrão do Button.
 */
@Composable
private fun WearPillIcon(imageVector: ImageVector, contentDescription: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Linha da home: mostra o orçamento, como o card do celular (Q7). */
@Composable
private fun WearHomeRow(
    shopping: Shopping,
    onClick: () -> Unit,
    spec: TransformationSpec,
    scope: TransformingLazyColumnItemScope
) {
    WearListRow(
        emoji = shopping.emoji,
        title = shopping.title,
        subtitle = shopping.budget?.let {
            stringResource(R.string.shopping_wear_budget, it.formatPrice(LocalCurrency.current))
        },
        pinnedLabel = if (shopping.isFavorite) stringResource(R.string.shopping_wear_pinned) else null,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(scope, spec),
        transformation = scope.SurfaceTransformation(spec)
    )
}

private val previewShoppingLists = listOf(
    Shopping(
        id = "1",
        title = "Café da manhã",
        description = "",
        price = 0.0,
        budget = 500.0,
        status = Shopping.Status.IN_PROGRESS,
        users = listOf("user1"),
        roles = emptyMap(),
        isFavorite = true
    ),
    Shopping(
        id = "2",
        title = "Jantar de domingo",
        description = "",
        price = 0.0,
        budget = 200.0,
        status = Shopping.Status.NEW,
        users = listOf("user3"),
        roles = emptyMap()
    )
)

@WearPreviewDevices
@Composable
private fun ShoppingWearContentPreview() {
    MaterialTheme {
        ShoppingWearContent(
            state = WearHomeUiState(lists = StableList(previewShoppingLists), isLoading = false),
            onIntent = {},
            onNavigateToDetail = {},
            onNavigateToSearch = {},
            onNavigateToMenu = {},
            onNavigateToSettings = {}
        )
    }
}

@WearPreviewDevices
@Composable
private fun ShoppingWearContentLoadingPreview() {
    MaterialTheme {
        ShoppingWearContent(
            state = WearHomeUiState(isLoading = true),
            onIntent = {},
            onNavigateToDetail = {},
            onNavigateToSearch = {},
            onNavigateToMenu = {},
            onNavigateToSettings = {}
        )
    }
}

@WearPreviewDevices
@Composable
private fun ShoppingWearContentEmptyPreview() {
    MaterialTheme {
        ShoppingWearContent(
            state = WearHomeUiState(isLoading = false),
            onIntent = {},
            onNavigateToDetail = {},
            onNavigateToSearch = {},
            onNavigateToMenu = {},
            onNavigateToSettings = {}
        )
    }
}
