package br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.products.app.domain.model.CommonProduct
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.CommonProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CommonProductGetAllUseCase @Inject constructor(
    private val repository: CommonProductRepository
) {
    suspend operator fun invoke(): Flow<List<CommonProduct>> {
        repository.seedDefaultsIfEmpty()
        return repository.getAll()
    }
}
