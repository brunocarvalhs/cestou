package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.components.UserAvatar
import br.com.brunocarvalhs.howmuch.feature.products.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Product history bottom sheet content — lists every [ProductActivity] entry chronologically
 * (oldest first, spec AC5 example order: added → edited → purchased). Resolves attribution
 * purely from [memberProfiles], the same map [CartViewModel]'s single per-list resolution
 * already populated — this composable never talks to `UserRepository` itself (T10 done-when).
 */
@Composable
internal fun ProductHistoryContent(
    history: List<ProductActivity>,
    memberProfiles: Map<String, UserProfile>,
    modifier: Modifier = Modifier
) {
    val orderedHistory = history.sortedBy { it.timestamp }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.shopping_list_history_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (orderedHistory.isEmpty()) {
            Text(
                text = stringResource(R.string.shopping_list_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn {
                itemsIndexed(orderedHistory) { index, entry ->
                    ProductHistoryRow(
                        entry = entry,
                        profile = memberProfiles[entry.userId]
                    )
                    if (index < orderedHistory.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ProductHistoryRow(entry: ProductActivity, profile: UserProfile?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Same avatar shell as the row's attribution avatar — icon fallback when the profile (or
        // its name) is unresolved, never a blank space (spec AC7).
        UserAvatar(profile = profile)

        Column(modifier = Modifier.weight(1f)) {
            val actionLabel = stringResource(entry.action.labelRes())
            val userName = profile?.name?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.shopping_list_history_unknown_user)
            Text(
                text = stringResource(R.string.shopping_list_history_entry, actionLabel, userName),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            text = formatHistoryTime(entry.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun ProductActivity.Action.labelRes(): Int = when (this) {
    ProductActivity.Action.ADDED -> R.string.shopping_list_history_action_added
    ProductActivity.Action.EDITED -> R.string.shopping_list_history_action_edited
    ProductActivity.Action.PURCHASED -> R.string.shopping_list_history_action_purchased
}

private fun formatHistoryTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

@Preview(showBackground = true, name = "3-entry history (added, edited, purchased)")
@Composable
private fun ProductHistoryContentPreview() {
    val baseTime = 1_700_000_000_000L
    CestouTheme {
        ProductHistoryContent(
            history = listOf(
                ProductActivity(userId = "u1", action = ProductActivity.Action.ADDED, timestamp = baseTime),
                ProductActivity(
                    userId = "u2",
                    action = ProductActivity.Action.EDITED,
                    timestamp = baseTime + 60_000
                ),
                ProductActivity(
                    userId = "u1",
                    action = ProductActivity.Action.PURCHASED,
                    timestamp = baseTime + 120_000
                )
            ),
            memberProfiles = mapOf(
                "u1" to UserProfile(id = "u1", name = "Bruno Carvalhos"),
                "u2" to UserProfile(id = "u2", name = null)
            )
        )
    }
}

@Preview(showBackground = true, name = "Empty history")
@Composable
private fun ProductHistoryContentEmptyPreview() {
    CestouTheme {
        ProductHistoryContent(
            history = emptyList(),
            memberProfiles = emptyMap()
        )
    }
}
