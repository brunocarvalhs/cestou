package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.products.domain.services.SharedService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class SharedUseCase(
    private val service: SharedService
) {
    operator fun invoke(): Flow<String> = flow {
        emit("Hello World!")
    }
}
