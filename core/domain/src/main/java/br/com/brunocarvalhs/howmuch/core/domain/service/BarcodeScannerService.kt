package br.com.brunocarvalhs.howmuch.core.domain.service

import androidx.camera.core.ImageProxy

interface BarcodeScannerService {
    suspend fun scan(image: ImageProxy): Result<List<String>>
}
