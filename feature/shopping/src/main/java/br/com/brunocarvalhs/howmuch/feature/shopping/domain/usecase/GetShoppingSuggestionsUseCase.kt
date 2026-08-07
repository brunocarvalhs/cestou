package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

internal class GetShoppingSuggestionsUseCase @Inject constructor() {
    operator fun invoke(): Flow<List<String>> = flowOf(
        listOf(
            "Qual é a minha lista mais cara?",
            "Sugerir itens para um café da manhã",
            "Quais listas estão pendentes?",
            "Crie uma lista de churrasco para 5 pessoas",
            "Qual o total gasto este mês?"
        )
    )
}
