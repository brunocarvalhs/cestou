package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ScreenScaffold

/**
 * Scrollable frame for every watch screen of the pairing flow. The round watch screen cuts the
 * corners, and the code plus its button do not fit on small watches, so content must scroll.
 * Same structure as the other Wear screens of the app (ScreenScaffold + TransformingLazyColumn).
 */
@Composable
internal fun WearScrollableScreen(
    modifier: Modifier = Modifier,
    content: TransformingLazyColumnScope.() -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = modifier,
            content = content
        )
    }
}
