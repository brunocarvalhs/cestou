package br.com.brunocarvalhs.howmuch.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * Guards the `feature.auth` package against drifting away from the project's standard
 * feature layering (domain, data, presentation, navigation, di, commons). Any new top-level
 * folder inside the feature (e.g. `util`, `helper`) fails this test instead of silently
 * fragmenting the architecture.
 */
class FeatureStructureTest {

    private val featurePackage = "br.com.brunocarvalhs.howmuch.feature.auth"
    private val allowedLayers = setOf("data", "di", "domain", "navigation", "presentation", "commons")

    @Test
    fun `auth package only contains the allowed feature layers`() {
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
}
