package br.com.brunocarvalhs.howmuch.feature.products.data.services

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import br.com.brunocarvalhs.howmuch.core.domain.service.BarcodeScannerService
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.tasks.await

internal class MlKitBarcodeScannerService : BarcodeScannerService {
    private val scanner = BarcodeScanning.getClient()

    @OptIn(ExperimentalGetImage::class)
    override suspend fun scan(image: ImageProxy): Result<List<String>> = runCatching {
        val mediaImage = image.image ?: throw Exception("Imagem inválida")
        val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
        
        val barcodes = scanner.process(inputImage).await()
        barcodes.mapNotNull { it.rawValue }
    }.also {
        image.close()
    }
}
