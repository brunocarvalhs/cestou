package br.com.brunocarvalhs.howmuch.feature.products.data.services

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class PriceTagResult(
    val nameCandidates: List<String>,
    val price: Double?
)

/**
 * Identifies a product from a photo of its shelf price tag using on-device ML Kit
 * text recognition (free, offline) instead of a paid vision LLM call. Price tags are
 * a much easier OCR target than the product packaging itself: printed in a clean,
 * predictable layout (name, price), with no logos/ingredient lists/decorative text
 * to confuse the recognizer, and they conveniently also carry the price. Quantity is
 * always a unit count set by the user in the confirmation step, never inferred here.
 */
@Singleton
class ProductImageTextRecognizer @Inject constructor() {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun recognizePriceTag(bitmap: Bitmap, maxCandidates: Int = MAX_CANDIDATES): PriceTagResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()
        val lines = result.textBlocks.flatMap { it.lines }
        val rawTexts = lines.map { it.text.trim() }

        val nameCandidates = lines
            .mapNotNull { line ->
                val text = line.text.trim()
                if (!isLikelyProductName(text)) return@mapNotNull null
                val area = line.boundingBox?.let { it.width().toLong() * it.height() } ?: 0L
                text to area
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctBy { it.lowercase() }
            .take(maxCandidates)
            .map { it.toProductName() }

        return PriceTagResult(
            nameCandidates = nameCandidates,
            price = extractPrice(rawTexts)
        )
    }

    private fun isLikelyProductName(text: String): Boolean {
        if (text.length !in MIN_LENGTH..MAX_LENGTH) return false
        if (text.none { it.isLetter() }) return false
        val digitCount = text.count { it.isDigit() }
        if (digitCount >= text.length / 2) return false
        return text.trim().uppercase() !in TAG_BOILERPLATE_WORDS
    }

    private fun extractPrice(lines: List<String>): Double? {
        for (line in lines) {
            val match = PRICE_REGEX.find(line) ?: continue
            val normalized = match.groupValues[1].replace(".", "").replace(",", ".")
            return normalized.toDoubleOrNull()
        }
        return null
    }

    private fun String.toProductName(): String = lowercase().split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }

    private companion object {
        const val MIN_LENGTH = 3
        const val MAX_LENGTH = 40
        const val MAX_CANDIDATES = 3
        val PRICE_REGEX = Regex("""(?:R\$\s?)?(\d{1,3}(?:\.\d{3})*,\d{2})""")
        val TAG_BOILERPLATE_WORDS = setOf(
            "PREÇO", "PRECO", "OFERTA", "PROMOÇÃO", "PROMOCAO", "UNIDADE", "UNITARIO",
            "UNITÁRIO", "CADA", "VALIDADE", "CÓDIGO", "CODIGO", "KG", "UN", "UND"
        )
    }
}
