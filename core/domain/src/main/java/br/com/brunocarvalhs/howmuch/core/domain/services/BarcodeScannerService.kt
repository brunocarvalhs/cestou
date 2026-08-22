package br.com.brunocarvalhs.howmuch.core.domain.services

interface BarcodeScannerService {
    suspend fun scan(imageSource: Any): Result<List<String>>
}
