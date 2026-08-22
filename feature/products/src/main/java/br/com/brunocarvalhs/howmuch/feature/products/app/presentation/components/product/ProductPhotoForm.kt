package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.product

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import br.com.brunocarvalhs.howmuch.core.extensions.hasPermission
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.scanner.CameraCaptureView
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.scanner.ImagePreviewView
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.scanner.PermissionDeniedState
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.intent.ProductPhotoIntent
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.state.ProductPhotoUiState

@Composable
internal fun ProductPhotoForm(
    modifier: Modifier = Modifier,
    uiState: ProductPhotoUiState,
    intent: ProductPhotoIntent = ProductPhotoIntent(),
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    var hasCameraPermission by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.CAMERA) || isInspection)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            intent.onImageCaptured(it)
            intent.onAnalyzeImage()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            !hasCameraPermission -> PermissionDeniedState()
            uiState.capturedImageUri == null -> CameraCaptureView(
                onImageCaptured = {
                    intent.onImageCaptured(it)
                    intent.onAnalyzeImage()
                },
                onGalleryClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            else -> ImagePreviewView(
                uri = uiState.capturedImageUri,
                isAnalyzing = uiState.isAnalyzing,
                onRetake = { intent.onRetake() },
                onAnalyze = { intent.onAnalyzeImage() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductPhotoFormPreview() {
    ProductPhotoForm(
        uiState = ProductPhotoUiState()
    )
}

@Preview(showBackground = true)
@Composable
private fun ProductPhotoFormAnalyzingPreview() {
    ProductPhotoForm(
        uiState = ProductPhotoUiState(
            capturedImageUri = Uri.EMPTY,
            isAnalyzing = true
        )
    )
}
