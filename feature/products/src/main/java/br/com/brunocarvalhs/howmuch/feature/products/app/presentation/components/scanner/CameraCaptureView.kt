package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.scanner

import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.brunocarvalhs.howmuch.feature.products.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun CameraCaptureView(
    onImageCaptured: (Uri) -> Unit,
    onGalleryClick: () -> Unit
) {
    if (LocalInspectionMode.current) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        PriceTagScannerCanvas()

        Text(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp, start = 24.dp, end = 24.dp),
            text = stringResource(R.string.product_photo_instruction),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        CaptureControls(
            onGalleryClick = onGalleryClick,
            onCaptureClick = {
                val photoFile = File(
                    context.cacheDir,
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
                )
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            onImageCaptured(Uri.fromFile(photoFile))
                        }
                        override fun onError(exc: ImageCaptureException) {
                            exc.printStackTrace()
                        }
                    }
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
