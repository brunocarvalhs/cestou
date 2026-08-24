package br.com.brunocarvalhs.howmuch.feature.products.app.data.repository

import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import br.com.brunocarvalhs.howmuch.core.domain.services.make
import br.com.brunocarvalhs.howmuch.core.domain.services.observe
import br.com.brunocarvalhs.howmuch.feature.products.app.data.extensions.toDomain
import br.com.brunocarvalhs.howmuch.feature.products.app.data.extensions.toModel
import br.com.brunocarvalhs.howmuch.feature.products.app.data.model.CommonProductModel
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.model.CommonProduct
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.CommonProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
internal class CommonProductRepositoryImpl @Inject constructor(
    private val networkService: NetworkService,
    private val authService: AuthService
) : CommonProductRepository {

    override fun getAll(): Flow<List<CommonProduct>> = flow {
        val userId = authService.getOrCreateUserId().id
        emit(userId)
    }.flatMapLatest { userId ->
        networkService.observe<List<CommonProductModel>>(
            request = NetworkService.NetworkRequest(
                endpoint = endpoint(userId),
                method = NetworkService.Method.GET
            )
        ).map { models -> models?.map { it.toDomain() } ?: emptyList() }
    }

    override suspend fun seedDefaultsIfEmpty(): Result<Unit> = runCatching {
        val userId = authService.getOrCreateUserId().id
        val current = networkService.make<List<CommonProductModel>>(
            request = NetworkService.NetworkRequest(
                endpoint = endpoint(userId),
                method = NetworkService.Method.GET
            )
        )
        if (current.isNullOrEmpty()) {
            Helper.defaultCommonProducts().forEach { product ->
                networkService.make<String>(
                    request = NetworkService.NetworkRequest(
                        endpoint = endpoint(userId),
                        method = NetworkService.Method.POST,
                        payload = product.toModel().toMap()
                    )
                )
            }
        }
    }

    override suspend fun add(item: CommonProduct): Result<Unit> = runCatching {
        val userId = authService.getOrCreateUserId().id
        networkService.make<String>(
            request = NetworkService.NetworkRequest(
                endpoint = endpoint(userId),
                method = NetworkService.Method.POST,
                payload = item.toModel().toMap()
            )
        )
    }

    override suspend fun remove(id: String): Result<Unit> = runCatching {
        val userId = authService.getOrCreateUserId().id
        networkService.make<Boolean>(
            request = NetworkService.NetworkRequest(
                endpoint = "${endpoint(userId)}/$id",
                method = NetworkService.Method.DELETE
            )
        )
    }

    private fun endpoint(userId: String): String = "users/$userId/common-products"

    private object Helper {
        /**
         * A classic Brazilian "cesta básica" starter set, seeded once per user so the
         * list behaves like a normal editable collection from the very first load.
         */
        fun defaultCommonProducts(): List<CommonProduct> = listOf(
            CommonProduct(id = "default-arroz", name = "Arroz", category = "Mercearia", unit = "kg"),
            CommonProduct(id = "default-feijao", name = "Feijão", category = "Mercearia", unit = "kg"),
            CommonProduct(id = "default-oleo", name = "Óleo de Soja", category = "Mercearia", unit = "un"),
            CommonProduct(id = "default-acucar", name = "Açúcar", category = "Mercearia", unit = "kg"),
            CommonProduct(id = "default-cafe", name = "Café", category = "Mercearia", unit = "un"),
            CommonProduct(id = "default-sal", name = "Sal", category = "Mercearia", unit = "kg"),
            CommonProduct(id = "default-farinha", name = "Farinha de Trigo", category = "Mercearia", unit = "kg"),
            CommonProduct(id = "default-leite", name = "Leite", category = "Laticínios", unit = "L"),
            CommonProduct(id = "default-manteiga", name = "Manteiga", category = "Laticínios", unit = "un"),
            CommonProduct(id = "default-pao", name = "Pão Francês", category = "Padaria", unit = "kg"),
            CommonProduct(id = "default-banana", name = "Banana", category = "Hortifruti", unit = "kg"),
            CommonProduct(id = "default-tomate", name = "Tomate", category = "Hortifruti", unit = "kg"),
            CommonProduct(id = "default-cebola", name = "Cebola", category = "Hortifruti", unit = "kg"),
            CommonProduct(id = "default-batata", name = "Batata", category = "Hortifruti", unit = "kg"),
            CommonProduct(id = "default-sabao", name = "Sabão em Pó", category = "Limpeza", unit = "un"),
            CommonProduct(id = "default-papel-higienico", name = "Papel Higiênico", category = "Higiene", unit = "pct")
        )
    }
}
