package br.com.brunocarvalhs.howmuch.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.ext.provider.hasAnnotationOf
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlinx.serialization.Serializable
import org.junit.Test

/**
 * Guards the `feature.shopping` package against drifting away from the project's standard
 * feature layering (domain, data, presentation, navigation, di, commons). Any new top-level
 * folder inside the feature (e.g. `util`, `helper`) fails this test instead of silently
 * fragmenting the architecture.
 */
class FeatureStructureTest {

    private val featurePackage = "br.com.brunocarvalhs.howmuch.feature.shopping"
    private val allowedLayers = setOf("data", "di", "domain", "navigation", "presentation", "commons")

    @Test
    fun `shopping package only contains the allowed feature layers`() {
        Konsist
            .scopeFromProject()
            .files
            .withPackage("$featurePackage..")
            .assertTrue { file ->
                val remainder = file.packagee?.name.orEmpty().removePrefix(featurePackage).removePrefix(".")

                if (remainder.isEmpty()) {
                    file.name.contains("Initializer")
                } else {
                    allowedLayers.contains(remainder.substringBefore("."))
                }
            }
    }

    /**
     * Guards against a silent-failure bug: `NetworkManager` resolves the response `KSerializer`
     * reflectively, so a `data.model` class that isn't `@Serializable` doesn't fail to compile —
     * it fails at runtime with a caught `SerializationException`, and the repository quietly
     * returns an empty list/null instead of the Firestore data. `ShoppingModel` shipped without
     * this annotation and every shopping list silently failed to load. See
     * `.claude/skills/firestore-serializable-models/SKILL.md`.
     */
    @Test
    fun `shopping data models are annotated as Serializable`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .filter { it.resideInPackage("$featurePackage.data.model..") }
            .assertTrue { it.hasAnnotationOf<Serializable>() }
    }
}
