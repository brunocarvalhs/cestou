package br.com.brunocarvalhs.howmuch.core.domain.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.ChainPriceIndex
import br.com.brunocarvalhs.howmuch.core.domain.model.UserLocation
import kotlinx.coroutines.flow.Flow

/** Localização do usuário e índices regionais de preço por rede de supermercado. */
interface SupermarketRepository {

    /** Emite `null` enquanto o usuário não escolheu Estado e Cidade. */
    fun observeLocation(): Flow<UserLocation?>

    suspend fun saveLocation(location: UserLocation)

    /** Redes presentes na praça, com seus índices; resultados por praça ficam em cache local. */
    suspend fun getChainIndexes(location: UserLocation): List<ChainPriceIndex>
}
