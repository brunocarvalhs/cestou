package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareCartBottomSheet(
    token: String?,
    onShareList: () -> Unit,
    onShareToken: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        onDismissRequest = { onDismiss() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.share_cart),
                style = MaterialTheme.typography.titleMedium
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.share_shopping_list)) },
                leadingContent = {
                    Icon(
                        painterResource(R.drawable.ic_format_list_bulleted),
                        contentDescription = stringResource(R.string.share_shopping_list)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShareList() }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.share_token)) },
                leadingContent = {
                    Icon(
                        painterResource(R.drawable.ic_key),
                        contentDescription = stringResource(R.string.share_token)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShareToken() },
                supportingContent = { Text(text = stringResource(R.string.token, token.orEmpty())) }
            )
        }
    }
}
