package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.product

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import br.com.brunocarvalhs.howmuch.core.extensions.hasPermission
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.scanner.BarcodeScannerOverlay
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.scanner.CameraPreview
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.scanner.PermissionDeniedState
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.intent.ProductBarcodeIntent
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.state.ProductBarcodeUiState

@Composable
internal fun ProductBarcodeForm(
    modifier: Modifier = Modifier,
    uiState: ProductBarcodeUiState,
    intent: ProductBarcodeIntent = ProductBarcodeIntent()
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    var hasCameraPermission by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.CAMERA) || isInspection)
    }
    var cameraControl by remember { mutableStateOf<Camera?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onBarcodeScanned = { intent.onBarcodeScanner(it) },
                onCameraReady = { cameraControl = it }
            )

            BarcodeScannerOverlay(
                isFlashOn = uiState.isFlashOn,
                onFlashToggle = {
                    intent.onFlashToggle()
                    cameraControl?.cameraControl?.enableTorch(!uiState.isFlashOn)
                }
            )
        } else {
            PermissionDeniedState()
        }
    }
}

@Preview(showBackground = true, name = "Flash desligado")
@Composable
private fun ProductBarcodeFormPreview() {
    ProductBarcodeForm(
        uiState = ProductBarcodeUiState()
    )
}

@Preview(showBackground = true, name = "Flash ligado")
@Composable
private fun ProductBarcodeFormFlashOnPreview() {
    ProductBarcodeForm(
        uiState = ProductBarcodeUiState(isFlashOn = true)
    )
}
