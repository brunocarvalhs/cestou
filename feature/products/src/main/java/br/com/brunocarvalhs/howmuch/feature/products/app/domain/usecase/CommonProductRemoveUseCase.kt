package br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.CommonProductRepository
import javax.inject.Inject

class CommonProductRemoveUseCase @Inject constructor(
    private val repository: CommonProductRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.remove(id)
}
