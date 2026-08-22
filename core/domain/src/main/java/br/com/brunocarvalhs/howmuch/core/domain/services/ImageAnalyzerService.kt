package br.com.brunocarvalhs.howmuch.core.domain.services

interface ImageAnalyzerService {
    suspend fun analyze(imageSource: Any): Result<ImageAnalysisResult>

    data class ImageAnalysisResult(
        val title: String?,
        val description: String?,
        val brand: String?,
        val confidence: Float
    )
}
