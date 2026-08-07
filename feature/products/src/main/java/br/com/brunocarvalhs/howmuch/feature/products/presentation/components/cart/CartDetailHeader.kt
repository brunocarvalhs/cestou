package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.cart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CartDetailHeader(
    modifier: Modifier = Modifier,
    title: String = "",
    usersCount: Int = 0,
    onBack: () -> Unit = {},
    onShare: () -> Unit = {},
    onFinish: () -> Unit = {},
    showFinish: Boolean = false,
    actionsMore: @Composable ColumnScope.() -> Unit = {}
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    LargeTopAppBar(
        modifier = modifier.statusBarsPadding(),
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics {
                        heading()
                    }
                )
                if (usersCount > 1) {
                    Text(
                        text = stringResource(R.string.shopping_list_members_collaborating, usersCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(CoreR.string.content_description_back)
                )
            }
        },
        actions = {
            if (showFinish) {
                IconButton(onClick = onFinish) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(CoreR.string.action_finish),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.IosShare,
                    contentDescription = stringResource(CoreR.string.content_description_share_list)
                )
            }

            Box {
                IconButton(
                    onClick = {
                        expanded = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(CoreR.string.content_description_more_options)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {
                    actionsMore()
                }
            }
        }
    )
}

@Preview
@Composable
private fun CartDetailHeaderPreview() {
    CartDetailHeader()
}
