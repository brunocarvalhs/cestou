package br.com.brunocarvalhs.howmuch.core.domain.service

import android.graphics.Bitmap

interface ImageAnalyzerService {
    suspend fun analyze(bitmap: Bitmap): Result<ImageAnalysisResult>

    data class ImageAnalysisResult(
        val title: String?,
        val description: String?,
        val brand: String?,
        val confidence: Float
    )
}
