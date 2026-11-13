package br.com.brunocarvalhs.howmuch.core.ads.service

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Acesso ao grafo de anúncios a partir de composables. `core:*` não hospeda ViewModels (a
 * regra de arquitetura exige `.presentation.viewmodel` nas features), então o banner resolve
 * as dependências por aqui, com o mesmo escopo singleton.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface AdsEntryPoint {
    fun adsPolicy(): AdsPolicy
    fun adsSdk(): AdsSdk
    fun adUnitIds(): AdUnitIds
}
