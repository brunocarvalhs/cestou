package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.CommonProductRepository
import javax.inject.Inject

internal class CommonProductRemoveUseCase @Inject constructor(
    private val repository: CommonProductRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.remove(id)
}
