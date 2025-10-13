package br.com.brunocarvalhs.howmuch.app.modules.history

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.trackClick
import br.com.brunocarvalhs.howmuch.app.foundation.annotations.DevicesPreview
import br.com.brunocarvalhs.howmuch.app.foundation.constants.TIPS
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.DateFormat
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.isWithinLastDays
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.setStatusBarIconColor
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.toFormatDate
import br.com.brunocarvalhs.howmuch.app.modules.history.components.HistoryFilter
import br.com.brunocarvalhs.howmuch.app.modules.history.components.HistoryFilterType
import br.com.brunocarvalhs.howmuch.app.modules.history.components.HistoryItem
import br.com.brunocarvalhs.howmuch.app.modules.history.components.HistoryTopBar
import br.com.brunocarvalhs.howmuch.app.modules.history.components.TipsComponent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onIntent(HistoryUiIntent.Retry)
    }

    HistoryContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onShared = { cart ->
            viewModel.sharedCart(context, cart)
            trackClick(
                viewId = "history_item_shared",
                viewName = "History Item Shared",
                screenName = "HistoryScreen"
            )
        }
    )
}

@Composable
fun HistoryContent(
    uiState: HistoryUiState = HistoryUiState(),
    onIntent: (HistoryUiIntent) -> Unit = {},
    onShared: (ShoppingCart) -> Unit = {}
) {
    var selectedFilter by rememberSaveable { mutableStateOf(HistoryFilterType.ALL) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(listOf<ShoppingCart>()) }
    val filteredItems = remember(uiState.historyItems, selectedFilter) {
        uiState.historyItems.filter { item ->
            when (selectedFilter) {
                HistoryFilterType.ALL ->
                    true
                HistoryFilterType.TODAY ->
                    item.purchaseDate
                        ?.toFormatDate(DateFormat.DAY_MONTH_YEAR) == getTodayDate()

                HistoryFilterType.LAST_SEVEN_DAYS, HistoryFilterType.LAST_THIRTY_DAYS ->
                    item.purchaseDate
                        ?.isWithinLastDays(HistoryFilterType.LAST_SEVEN_DAYS.value) == true

                HistoryFilterType.CURRENT_MONTH ->
                    item.purchaseDate
                        ?.toFormatDate(DateFormat.MONTH_YEAR) == getCurrentMonthYear()
            }
        }
    }

    Scaffold(
        topBar = {
            HistoryTopBar(
                title = R.string.nav_bar_history,
                selectionMode = selectionMode,
                selectedCount = selectedItems.size,
                totalItems = filteredItems.size,
                onCancelSelection = {
                    selectionMode = false
                    selectedItems = emptyList()
                },
                onEnterSelectionMode = { selectionMode = true },
                onDeleteSelected = {
                    onIntent(HistoryUiIntent.DeleteSelected(selectedItems))
                    selectedItems = emptyList()
                    selectionMode = false
                },
                onSelectAll = {
                    selectedItems = filteredItems
                },
                onNotSelectAll = {
                    selectedItems = emptyList()
                },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AnimatedVisibility(
                        visible = !selectionMode
                    ) {
                        TipsComponent(
                            context = LocalContext.current,
                            tips = TIPS
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HistoryFilter(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { selectedFilter = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(filteredItems) { item ->
                    HistoryItem(
                        item = item,
                        isSelected = selectedItems.contains(item),
                        isSelectionMode = selectionMode,
                        onCheckedChange = { checked ->
                            selectedItems =
                                if (checked) selectedItems + item else selectedItems - item
                        },
                        onClick = {
                            if (!selectionMode) onShared(item)
                        }
                    )
                }
            }
        }
    }
}

fun getCurrentMonthYear(): String {
    val sdf = SimpleDateFormat("MM/yyyy", Locale.getDefault())
    return sdf.format(Date())
}

fun getTodayDate(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date())
}

@Composable
@DevicesPreview
private fun HistoryPreview() {
    HistoryContent()
}
