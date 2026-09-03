package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.products.domain.model.CommonProduct
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.CommonProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class CommonProductGetAllUseCase @Inject constructor(
    private val repository: CommonProductRepository
) {
    suspend operator fun invoke(): Flow<List<CommonProduct>> {
        repository.seedDefaultsIfEmpty()
        return repository.getAll()
    }
}
