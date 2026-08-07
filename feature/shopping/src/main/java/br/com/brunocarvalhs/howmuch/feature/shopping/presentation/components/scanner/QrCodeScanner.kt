package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.scanner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.extensions.hasPermission
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.scanner.QRCodeOverlay
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.scanner.CameraPreview

@Composable
internal fun QrCodeScanner(
    onTokenScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(context.hasPermission(Manifest.permission.CAMERA)) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<Camera?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onBarcodeScanned = onTokenScanned,
                onCameraReady = { cameraControl = it }
            )

            // UI Overlay to make it look like a scanner screen
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                QRCodeOverlay(
                    isFlashOn = isFlashOn,
                    onFlashToggle = {
                        isFlashOn = !isFlashOn
                        cameraControl?.cameraControl?.enableTorch(isFlashOn)
                    }
                )
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fechar",
                tint = Color.White
            )
        }
    }
}
