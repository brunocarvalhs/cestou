package br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.products.app.domain.services.SharedService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SharedUseCase(
    private val service: SharedService
) {
    operator fun invoke(): Flow<String> = flow {
        emit("Hello World!")
    }
}
