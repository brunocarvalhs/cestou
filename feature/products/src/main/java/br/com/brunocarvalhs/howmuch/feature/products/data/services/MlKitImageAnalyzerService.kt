package br.com.brunocarvalhs.howmuch.feature.products.data.services

import android.graphics.Bitmap
import br.com.brunocarvalhs.howmuch.core.domain.service.ImageAnalyzerService
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await

internal class MlKitImageAnalyzerService : ImageAnalyzerService {
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    override suspend fun analyze(bitmap: Bitmap): Result<ImageAnalyzerService.ImageAnalysisResult> = runCatching {
        val image = InputImage.fromBitmap(bitmap, 0)
        val labels = labeler.process(image).await()
        
        val topLabel = labels.maxByOrNull { it.confidence }
        
        ImageAnalyzerService.ImageAnalysisResult(
            title = topLabel?.text,
            description = labels.joinToString(", ") { it.text },
            brand = null,
            confidence = topLabel?.confidence ?: 0f
        )
    }
}
