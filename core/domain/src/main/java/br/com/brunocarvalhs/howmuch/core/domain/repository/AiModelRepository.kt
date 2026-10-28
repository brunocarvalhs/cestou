package br.com.brunocarvalhs.howmuch.core.domain.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.AiModel

interface AiModelRepository {
    suspend fun getAvailableModels(): List<AiModel>
}
