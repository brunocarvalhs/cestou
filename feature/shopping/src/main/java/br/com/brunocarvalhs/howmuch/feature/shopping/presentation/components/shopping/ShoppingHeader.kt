package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShoppingHeader(
    modifier: Modifier = Modifier,
    onAdd: () -> Unit = {},
    onJoin: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Column(modifier = modifier.statusBarsPadding()) {

        TopAppBar(
            title = {
                Text(
                    text = stringResource(CoreR.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics {
                        heading()
                    }
                )
            },
            actions = {
                IconButton(onClick = onJoin) {
                    Icon(
                        Icons.Default.GroupAdd,
                        contentDescription = stringResource(CoreR.string.content_description_join_list)
                    )
                }

                IconButton(onClick = onAdd) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(CoreR.string.action_add)
                    )
                }

                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(CoreR.string.content_description_settings)
                    )
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShoppingHeaderPreview() {
    MaterialTheme {
        ShoppingHeader()
    }
}