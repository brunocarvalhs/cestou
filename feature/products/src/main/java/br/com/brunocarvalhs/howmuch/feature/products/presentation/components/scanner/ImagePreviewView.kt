package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.scanner

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.common.AnalyzingOverlay
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.common.PreviewActions
import coil.compose.AsyncImage
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

@Composable
internal fun ImagePreviewView(
    uri: Uri,
    isAnalyzing: Boolean,
    onRetake: () -> Unit,
    onAnalyze: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = uri,
            contentDescription = stringResource(CoreR.string.content_description_preview),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (isAnalyzing) {
            AnalyzingOverlay()
        } else {
            PreviewActions(
                onRetake = onRetake,
                onAnalyze = onAnalyze,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
