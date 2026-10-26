package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.spendhistory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.feature.cart.R
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

private const val EMPTY_ICON_ALPHA = 0.3f
private const val EMPTY_TEXT_ALPHA = 0.7f

/** Centered spinner shown while the (future) ViewModel is loading month data. */
@Composable
internal fun SpendHistoryLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Inline, retryable error — never a blank or zeroed screen that could read as "no spending". */
@Composable
internal fun SpendHistoryErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )
        CestouButton(
            text = stringResource(CoreR.string.action_retry),
            onClick = onRetry,
            modifier = Modifier.testTag("spend_history_retry_button")
        )
    }
}

/**
 * Doubles as the "no purchases this month yet" state (AC9, [monthLabel] set) and the
 * degenerate "no months at all" fallback ([monthLabel] null) — same visual language as
 * [br.com.brunocarvalhs.howmuch.core.ui.components.CestouEmptyState] (muted icon, title,
 * description) but with on-topic copy instead of that component's cart-specific strings.
 */
@Composable
internal fun SpendHistoryEmptyMonthState(monthLabel: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = EMPTY_ICON_ALPHA)
        )
        Text(
            text = if (monthLabel != null) {
                stringResource(R.string.spend_history_month_empty_title, monthLabel)
            } else {
                stringResource(R.string.spend_history_no_data_title)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = stringResource(R.string.spend_history_month_empty_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline.copy(alpha = EMPTY_TEXT_ALPHA),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, name = "Carregando")
@Composable
private fun SpendHistoryLoadingStatePreview() {
    CestouTheme { SpendHistoryLoadingState() }
}

@Preview(showBackground = true, name = "Erro, com retry")
@Composable
private fun SpendHistoryErrorStatePreview() {
    CestouTheme {
        SpendHistoryErrorState(
            message = stringResource(R.string.spend_history_error_message),
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, name = "Mês sem compras")
@Composable
private fun SpendHistoryEmptyMonthStatePreview() {
    CestouTheme { SpendHistoryEmptyMonthState(monthLabel = "junho") }
}
