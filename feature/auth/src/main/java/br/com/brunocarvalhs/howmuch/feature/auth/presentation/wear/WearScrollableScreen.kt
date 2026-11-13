package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ScreenScaffold

/**
 * Frame for the watch pairing screens. The content is centred when it fits and scrolls when it
 * does not, so a short watch never cuts off the code or its buttons.
 *
 * A plain scrollable column, not a transforming list: the transformation effect of a list shrinks
 * items near the round edge of the screen, which distorted the single action button.
 */
@Composable
internal fun WearScrollableScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    ScreenScaffold { contentPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = HORIZONTAL_PADDING)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content
        )
    }
}

private val HORIZONTAL_PADDING = 16.dp
