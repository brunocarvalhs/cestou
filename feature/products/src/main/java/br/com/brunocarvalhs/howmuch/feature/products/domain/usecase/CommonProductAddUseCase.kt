package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.products.domain.model.CommonProduct
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.CommonProductRepository
import java.util.UUID
import javax.inject.Inject

internal class CommonProductAddUseCase @Inject constructor(
    private val repository: CommonProductRepository
) {
    suspend operator fun invoke(
        name: String,
        category: String = "Outros",
        unit: String = "un"
    ): Result<Unit> = repository.add(
        CommonProduct(
            id = UUID.randomUUID().toString(),
            name = name,
            category = category,
            unit = unit
        )
    )
}
