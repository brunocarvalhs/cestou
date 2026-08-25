package br.com.brunocarvalhs.howmuch.feature.products.app.data.repository

import android.graphics.Bitmap
import br.com.brunocarvalhs.howmuch.core.common.BuildConfig
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import br.com.brunocarvalhs.howmuch.core.domain.services.observe
import br.com.brunocarvalhs.howmuch.core.ui.entity.ProductCategory
import br.com.brunocarvalhs.howmuch.feature.products.app.data.extensions.toDomain
import br.com.brunocarvalhs.howmuch.feature.products.app.data.extensions.toModel
import br.com.brunocarvalhs.howmuch.feature.products.app.data.model.ProductModel
import br.com.brunocarvalhs.howmuch.feature.products.app.data.services.ProductImageTextRecognizer
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.ProductRepository
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class ProductRepositoryImpl @Inject constructor(
    private val networkService: NetworkService,
    @Named("CloudNetwork")
    private val cloudNetwork: NetworkService,
    private val shoppingRepository: ShoppingRepository,
    private val imageTextRecognizer: ProductImageTextRecognizer
) : ProductRepository {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = BuildConfig.GEMINI_AGENT,
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    override fun getSuggestions(shoppingId: String): Flow<List<Product>> = flow {
        runCatching {
            val shopping = shoppingRepository.getById(shoppingId) ?: return@runCatching
            val currentProducts = getAllProducts(shoppingId).first()
            val productNames = currentProducts.joinToString { it.name }

            val response = generativeModel.generateContent(
                Helper.createSuggestionsPrompt(shopping.title, shopping.description, productNames)
            )
            val fullText = response.text ?: return@runCatching

            val startIndex = fullText.indexOf("[")
            val endIndex = fullText.lastIndexOf("]")

            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                val jsonText = fullText.substring(startIndex, endIndex + 1)
                val jsonArray = Json.parseToJsonElement(jsonText).jsonArray
                emit(Helper.parseAiSuggestions(jsonArray))
            }
        }.onFailure {
            emit(Helper.getDefaultSuggestions())
        }
    }

    override suspend fun saveProduct(product: Product, shoppingId: String): Result<Unit> {
        val lockCheck = checkIsLocked(shoppingId)
        if (lockCheck.isFailure) return lockCheck

        return runCatching {
            networkService.make(
                request = NetworkService.NetworkRequest(
                    endpoint = Helper.getEndpoint(shoppingId),
                    method = NetworkService.Method.POST,
                    payload = product.toModel().toMap()
                ),
                response = String::class
            )
        }
    }

    override suspend fun getAllProducts(shoppingId: String): Flow<List<Product>> {
        return networkService.observe<List<ProductModel>>(
            request = NetworkService.NetworkRequest(
                endpoint = Helper.getEndpoint(shoppingId),
                method = NetworkService.Method.GET,
            )
        ).map { models ->
            models?.map { it.toDomain() } ?: emptyList()
        }
    }

    override suspend fun deleteProduct(productId: String, shoppingId: String): Result<Unit> {
        val lockCheck = checkIsLocked(shoppingId)
        if (lockCheck.isFailure) return lockCheck

        return runCatching {
            networkService.make(
                request = NetworkService.NetworkRequest(
                    endpoint = "${Helper.getEndpoint(shoppingId)}/$productId",
                    method = NetworkService.Method.DELETE,
                ),
                response = String::class
            )
        }
    }

    override suspend fun updateProduct(product: Product, shoppingId: String): Result<Unit> {
        val lockCheck = checkIsLocked(shoppingId)
        if (lockCheck.isFailure) return lockCheck

        return runCatching {
            networkService.make(
                request = NetworkService.NetworkRequest(
                    endpoint = "${Helper.getEndpoint(shoppingId)}/${product.id}",
                    method = NetworkService.Method.PUT,
                    payload = product.toModel().toMap()
                ),
                response = String::class
            )
        }
    }

    override suspend fun searchProducts(query: String): Result<List<Product>> {
        return runCatching {
            val response = cloudNetwork.make(
                request = NetworkService.NetworkRequest(
                    endpoint = "https://world.openfoodfacts.org/cgi/search.pl",
                    query = mapOf(
                        "search_terms" to query,
                        "search_simple" to 1,
                        "action" to "process",
                        "json" to 1
                    ),
                    method = NetworkService.Method.GET
                ),
                response = JsonObject::class
            )

            val productsArray = response?.get("products")?.jsonArray ?: return@runCatching emptyList()

            productsArray.map { it.jsonObject }.map { productJson ->
                Product(
                    id = UUID.randomUUID().toString(),
                    name = productJson["product_name"]?.jsonPrimitive?.content ?: "Produto Desconhecido",
                    quantity = 1.0,
                    price = 0.0,
                    category = ProductCategory.fromString(
                        productJson["categories"]?.jsonPrimitive?.content?.split(",")?.firstOrNull()
                    ).name,
                    barcode = productJson["code"]?.jsonPrimitive?.content
                )
            }
        }
    }

    override fun getQuestionSuggestions(shoppingId: String): Flow<List<String>> = flow {
        emit(listOf("O que falta na lista?", "Compare preços", "Sugira uma receita"))

        runCatching {
            val shopping = shoppingRepository.getById(shoppingId) ?: return@runCatching
            val currentProducts = getAllProducts(shoppingId).first()
            val productNames = currentProducts.joinToString { it.name }

            val prompt = """
                Aja como um assistente de compras inteligente. 
                O usuário tem uma lista chamada '${shopping.title}' que contém: [$productNames].
                Sugira 4 perguntas ou comandos curtos (máximo 40 caracteres cada) que o usuário poderia me enviar para ajudar no planejamento ou economia.
                Exemplos: "Quanto já gastei?", "O que falta para o jantar?", "Compare o preço do arroz".
                Responda apenas com um array JSON de strings: ["Pergunta 1", "Pergunta 2", ...]
                Considere as novas categorias: Legumes, Perecíveis e Congelados se relevante.
                Não inclua nenhuma outra explicação ou formatação além do JSON.
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            val fullText = response.text ?: return@runCatching

            val startIndex = fullText.indexOf("[")
            val endIndex = fullText.lastIndexOf("]")

            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                val jsonText = fullText.substring(startIndex, endIndex + 1)
                val jsonArray = Json.parseToJsonElement(jsonText).jsonArray
                val questions = jsonArray.map { it.jsonPrimitive.content }
                emit(questions)
            }
        }
    }

    /**
     * Tries on-device ML Kit text recognition first (free, offline): it reads the
     * product name and price straight off the shelf price tag. Only falls back to the
     * paid Gemini vision call when OCR finds nothing (e.g. produce with no printed
     * tag in frame), so the AI cost is only paid when it's actually needed.
     */
    override suspend fun analyzeImage(bitmap: Bitmap): Result<List<Product>> {
        return runCatching {
            val tag = imageTextRecognizer.recognizePriceTag(bitmap)
            if (tag.nameCandidates.isNotEmpty()) {
                tag.nameCandidates.map { name ->
                    Product(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        quantity = 1.0,
                        price = tag.price ?: 0.0
                    )
                }
            } else {
                analyzeImageWithGemini(bitmap)
            }
        }.onFailure {
            Timber.e(it, "Falha ao analisar imagem")
        }
    }

    private suspend fun analyzeImageWithGemini(bitmap: Bitmap): List<Product> {
        val prompt = """
            Analise esta imagem e identifique todos os produtos de supermercado presentes nela.
            Para cada produto encontrado, extraia: nome, quantidade estimada, unidade de medida (un, kg, L, pct, etc) e a categoria mais provável.
            Responda apenas com um array JSON válido no formato: [{"name": "...", "quantity": 0.0, "unit": "...", "category": "..."}]
            Categorias válidas: Hortifruti, Carnes, Laticínios, Bebidas, Limpeza, Higiene, Mercearia, Legumes, Perecíveis, Congelados, Padaria, Outros.
            Não inclua nenhuma outra explicação ou formatação markdown além do JSON.
        """.trimIndent()

        val content = com.google.ai.client.generativeai.type.content {
            image(bitmap)
            text(prompt)
        }

        val response = generativeModel.generateContent(content)
        val fullText = response.text ?: return emptyList()

        val startIndex = fullText.indexOf("[")
        val endIndex = fullText.lastIndexOf("]")

        if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) return emptyList()

        val jsonArray = Json.parseToJsonElement(fullText.substring(startIndex, endIndex + 1)).jsonArray
        return jsonArray.map { element ->
            val obj = element.jsonObject
            Product(
                id = UUID.randomUUID().toString(),
                name = obj["name"]?.jsonPrimitive?.content ?: "Produto Desconhecido",
                quantity = obj["quantity"]?.jsonPrimitive?.doubleOrNull ?: 1.0,
                price = 0.0,
                category = obj["category"]?.jsonPrimitive?.content ?: "Outros",
            )
        }
    }

    private suspend fun checkIsLocked(shoppingId: String): Result<Unit> {
        val shopping = shoppingRepository.getById(shoppingId)
        return if (shopping?.status == Shopping.Status.FINISH) {
            Result.failure(IllegalStateException("Esta lista está finalizada e não pode ser alterada."))
        } else {
            Result.success(Unit)
        }
    }

    private object Helper {
        fun getEndpoint(shoppingId: String): String = "shopping/$shoppingId/products"

        fun createSuggestionsPrompt(title: String, description: String, productNames: String): String {
            return """
                Aja como um assistente de compras experiente para o mercado brasileiro. 
                O usuário tem uma lista chamada '$title' ($description) que já contém os itens: [$productNames].
                Sugira 8 produtos adicionais que façam sentido para esta lista ou que sejam itens essenciais comuns que possam estar faltando.
                Evite sugerir itens que já estão na lista.
                Responda apenas com um array JSON válido no formato: [{"name": "...", "category": "...", "unit": "..."}]
                As categorias permitidas são: Hortifruti, Carnes, Laticínios, Bebidas, Limpeza, Higiene, Mercearia, Legumes, Perecíveis, Congelados, Padaria, Outros.
                Não inclua nenhuma outra explicação ou formatação markdown além do JSON.
            """.trimIndent()
        }

        fun parseAiSuggestions(jsonArray: JsonArray): List<Product> {
            return jsonArray.map { element ->
                val obj = element.jsonObject
                Product(
                    id = UUID.randomUUID().toString(),
                    name = obj["name"]?.jsonPrimitive?.content ?: "",
                    quantity = 1.0,
                    price = 0.0,
                    category = obj["category"]?.jsonPrimitive?.content ?: "Outros",
                )
            }
        }

        fun getDefaultSuggestions(): List<Product> {
            return listOf(
                Product(
                    id = UUID.randomUUID().toString(),
                    name = "Arroz",
                    quantity = 1.0,
                    price = 0.0,
                    category = "Mercearia"
                ),
                Product(
                    id = UUID.randomUUID().toString(),
                    name = "Feijão",
                    quantity = 1.0,
                    price = 0.0,
                    category = "Mercearia"
                ),
                Product(
                    id = UUID.randomUUID().toString(),
                    name = "Óleo",
                    quantity = 1.0,
                    price = 0.0,
                    category = "Mercearia"
                )
            )
        }
    }
}
