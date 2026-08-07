package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.scanner

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.ai.AnalyzingOverlay
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.common.PreviewActions
import coil.compose.AsyncImage

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
            contentDescription = "Preview",
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
