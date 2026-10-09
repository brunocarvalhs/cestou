package br.com.brunocarvalhs.howmuch.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Architecture rules for every module, in one place (replaces the per-module
 * FeatureStructureTest/CoreStructureTest copies). Lives in :app only because :app sees the
 * whole project; Konsist reads sources, not the classpath.
 *
 * Rules that the codebase doesn't meet yet carry an explicit list of known violations
 * (see .specs/ARCHITECTURE-AUDIT.md). A rule fails on a new violation AND on a listed one that
 * no longer happens, so the lists only ever shrink.
 */
class ArchitectureTest {

    private val production = Konsist.scopeFromProduction().files

    @Test
    fun `feature packages only contain the allowed layers`() {
        val violations = production
            .filter { it.packageName.startsWith("$ROOT.feature.") }
            .filterNot { file ->
                val layer = file.packageName.removePrefix("$ROOT.feature.").substringAfter(".", "")
                if (layer.isEmpty()) "Initializer" in file.name else layer.substringBefore(".") in FEATURE_LAYERS
            }
            .map { it.path() }
            .toSet()

        assertKnown("feature layers", violations, known = emptySet())
    }

    @Test
    fun `core packages only contain the allowed layers`() {
        val violations = production
            .filter { it.packageName.startsWith("$ROOT.core.") }
            .filterNot { file ->
                val layer = file.packageName.removePrefix("$ROOT.core.").substringAfter(".", "")
                layer.isEmpty() || layer.substringBefore(".") in CORE_LAYERS
            }
            .map { it.path() }
            .toSet()

        assertKnown("core layers", violations, known = emptySet())
    }

    @Test
    fun `domain does not depend on data or presentation`() {
        val violations = production
            .filter { ".domain" in it.packageName }
            .flatMap { file ->
                file.imports
                    .filter { it.name.startsWith(ROOT) && (".data." in it.name || ".presentation." in it.name) }
                    .map { "${file.path()} -> ${it.name.removePrefix("$ROOT.")}" }
            }
            .toSet()

        assertKnown("domain -> data/presentation", violations, known = emptySet())
    }

    @Test
    fun `domain does not depend on Android, Compose or Firebase`() {
        val violations = production
            .filter { ".domain" in it.packageName }
            .flatMap { file ->
                file.imports
                    .filter { import -> PLATFORM_PREFIXES.any { import.name.startsWith(it) } }
                    .map { "${file.path()} -> ${it.name.removePrefix("$ROOT.")}" }
            }
            .toSet()

        assertKnown("domain -> platform", violations, KNOWN_DOMAIN_PLATFORM)
    }

    @Test
    fun `features do not depend on other features except their navigation`() {
        val violations = production
            .filter { it.packageName.startsWith("$ROOT.feature.") }
            .flatMap { file ->
                val own = file.packageName.removePrefix("$ROOT.feature.").substringBefore(".")
                file.imports
                    .filter { it.name.startsWith("$ROOT.feature.") }
                    .filterNot { it.name.removePrefix("$ROOT.feature.").substringBefore(".") == own }
                    .filterNot { it.name.removePrefix("$ROOT.feature.").substringAfter(".").startsWith("navigation.") }
                    .map { "${file.path()} -> ${it.name.removePrefix("$ROOT.")}" }
            }
            .toSet()

        assertKnown("feature -> feature", violations, KNOWN_FEATURE_TO_FEATURE)
    }

    @Test
    fun `classes live in the package their suffix implies`() {
        val violations = production
            .flatMap { it.classes() + it.interfaces() }
            .mapNotNull { declaration ->
                val expected = SUFFIX_PACKAGES.entries
                    .firstOrNull { (suffix, _) -> declaration.name.endsWith(suffix) }
                    ?.value
                    ?: return@mapNotNull null
                val pkg = declaration.packagee?.name.orEmpty()
                // ".presentation.viewmodel" also matches ".presentation.wear.viewmodel" (Wear screens).
                val (layer, leaf) = expected.removePrefix(".").split(".")
                val ok = ".$layer." in "$pkg." && pkg.endsWith(".$leaf")
                if (ok) null else "${declaration.name} in ${pkg.removePrefix("$ROOT.")}"
            }
            .toSet()

        assertKnown("suffix -> package", violations, KNOWN_SUFFIX_PACKAGE)
    }

    private fun assertKnown(rule: String, actual: Set<String>, known: Set<String>) {
        val new = (actual - known).sorted()
        val fixed = (known - actual).sorted()
        assertTrue(
            buildString {
                appendLine("Architecture rule '$rule' changed.")
                if (new.isNotEmpty()) appendLine("New violations:\n  " + new.joinToString("\n  "))
                if (fixed.isNotEmpty()) {
                    appendLine("Fixed, remove from the known list:\n  " + fixed.joinToString("\n  "))
                }
            },
            new.isEmpty() && fixed.isEmpty()
        )
    }

    private val KoFileDeclaration.packageName get() = packagee?.name.orEmpty()

    private fun KoFileDeclaration.path() = "${packageName.removePrefix("$ROOT.")}.${name}"

    @Suppress("MaximumLineLength") // Known-violation lists are data, one entry per line.
    private companion object {
        const val ROOT = "br.com.brunocarvalhs.howmuch"

        val FEATURE_LAYERS = setOf("data", "di", "domain", "navigation", "presentation", "commons")

        val CORE_LAYERS = setOf(
            "annotation", "base", "cloud", "components", "contract", "di", "dragdrop", "entity",
            "exception", "extensions", "initializer", "mobile", "model", "network", "registry",
            "repository", "security", "service", "services", "usecase", "util", "utils", "wear",
            "wearable",
        )

        val PLATFORM_PREFIXES = listOf("android.", "androidx.", "com.google.firebase.", "com.firebase.")

        // Audit item 4 (phase 2): domain still using Context, Intent, Bitmap, FirebaseUI, Compose.
        val KNOWN_DOMAIN_PLATFORM = setOf(
            "feature.auth.domain.usecase.AuthConfigUseCase -> android.content.Context",
            "feature.auth.domain.usecase.AuthConfigUseCase -> com.firebase.ui.auth.configuration.AuthUIConfiguration",
            "feature.auth.domain.usecase.AuthConfigUseCase -> com.firebase.ui.auth.configuration.authUIConfiguration",
            "feature.auth.domain.usecase.GoogleProviderUseCase -> com.firebase.ui.auth.configuration.auth_provider.AuthProvider",
            "feature.chat.domain.entity.ChatMessage -> androidx.compose.runtime.Stable",
            "feature.products.domain.usecase.ProductAnalyzeImageUseCase -> android.graphics.Bitmap",
            "feature.products.domain.usecase.ShareShoppingUseCaseImpl -> android.content.Context",
            "feature.products.domain.usecase.ShareShoppingUseCaseImpl -> android.content.Intent",
            "feature.shopping.domain.usecase.ShoppingCreateUseCase -> android.content.Context",
            "feature.shopping.domain.usecase.ShoppingDuplicateUseCase -> android.content.Context",
            "feature.shopping.domain.usecase.ShoppingJoinUseCase -> android.content.Context",
        )

        // Audit item 2 (phase 2): shared UI/resources/use cases still owned by another feature.
        val KNOWN_FEATURE_TO_FEATURE = setOf(
            "feature.auth.presentation.viewmodel.WelcomeViewModel -> feature.settings.domain.usecase.UpdateLanguageUseCase",
            "feature.cart.navigation.CartGraph -> feature.products.presentation.components.common.ShareOptionsBottomSheet",
            "feature.cart.presentation.components.ConfirmItemContent -> feature.products.R",
            "feature.cart.presentation.components.EditItemContent -> feature.products.R",
            "feature.cart.presentation.components.FinishPurchaseContent -> feature.products.R",
            "feature.cart.presentation.components.MoveToBar -> feature.products.R",
            "feature.cart.presentation.components.ProductHistoryContent -> feature.products.R",
            "feature.cart.presentation.components.ProductListItem -> feature.products.R",
            "feature.cart.presentation.components.ai.AiMessageBubble -> feature.chat.domain.entity.ChatMessage",
            "feature.cart.presentation.components.ai.CartAssistantDock -> feature.chat.domain.entity.ChatMessage",
            "feature.cart.presentation.components.ai.CartAssistantDock -> feature.products.presentation.components.product.Suggestions",
            "feature.cart.presentation.components.ai.ChatContent -> feature.chat.domain.entity.ChatMessage",
            "feature.cart.presentation.screen.CartScreen -> feature.products.R",
            "feature.cart.presentation.state.CartUiState -> feature.chat.domain.entity.ChatMessage",
            "feature.cart.presentation.viewmodel.CartViewModel -> feature.products.domain.usecase.ProductsUseCase",
            "feature.cart.presentation.viewmodel.CartViewModel -> feature.products.domain.usecase.ShoppingClearPurchasedUseCase",
            "feature.cart.presentation.viewmodel.CartViewModel -> feature.products.domain.usecase.SortProductsUseCase",
            "feature.cart.presentation.viewmodel.ConfirmItemViewModel -> feature.products.domain.usecase.ProductsUseCase",
            "feature.cart.presentation.viewmodel.EditItemViewModel -> feature.products.domain.usecase.ProductsUseCase",
            "feature.shopping.domain.usecase.ShoppingDuplicateUseCase -> feature.products.domain.usecase.ProductsUseCase",
            "feature.shopping.domain.usecase.ShoppingGetDetailsUseCase -> feature.products.domain.usecase.ProductsUseCase",
            "feature.shopping.presentation.wear.viewmodel.ShoppingDetailViewModel -> feature.products.domain.usecase.ProductsUseCase",
        )

        // Audit items 6/7 (phase 3): AD-011 port and AI base class outside domain.usecase; app root ViewModel.
        val KNOWN_SUFFIX_PACKAGE = setOf(
            "AgentActionUseCase in core.ai.base",
            "MainViewModel in br.com.brunocarvalhs.howmuch",
            "ShareShoppingUseCase in core.domain.services",
        )

        // Order matters: the first matching suffix wins ("RepositoryImpl" before "Repository").
        val SUFFIX_PACKAGES = linkedMapOf(
            "UseCase" to ".domain.usecase",
            "ViewModel" to ".presentation.viewmodel",
            "RepositoryImpl" to ".data.repository",
            "UiState" to ".presentation.state",
        )
    }
}
