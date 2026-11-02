package br.com.brunocarvalhs.howmuch.core.data.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.AiModel
import br.com.brunocarvalhs.howmuch.core.domain.model.AiProvider
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiModelRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
internal data class OpenRouterModelsResponse(
    @SerialName("data") val data: List<OpenRouterModelDto> = emptyList()
)

@Serializable
internal data class OpenRouterModelDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("pricing") val pricing: OpenRouterPricingDto? = null
)

@Serializable
internal data class OpenRouterPricingDto(
    @SerialName("prompt") val prompt: String? = null,
    @SerialName("completion") val completion: String? = null
)

@Singleton
class AiModelRepositoryImpl @Inject constructor(
    private val client: HttpClient
) : AiModelRepository {

    override suspend fun getAvailableModels(): List<AiModel> {
        return try {
            val response: OpenRouterModelsResponse = client.get("https://openrouter.ai/api/v1/models").body()
            response.data.map { dto ->
                val promptPrice = dto.pricing?.prompt?.toDoubleOrNull() ?: 1.0
                val completionPrice = dto.pricing?.completion?.toDoubleOrNull() ?: 1.0
                val isFree = dto.id.endsWith(":free") || (promptPrice == 0.0 && completionPrice == 0.0)

                val vendor = extractVendor(dto.id)
                val cleanName = dto.name ?: dto.id

                AiModel(
                    id = dto.id,
                    name = cleanName,
                    vendor = vendor,
                    provider = AiProvider.OPENROUTER,
                    isFree = isFree
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractVendor(id: String): String {
        val part = id.substringBefore('/').replaceFirstChar { it.uppercase() }
        return if (part.isNotBlank()) part else "OpenRouter"
    }
}
