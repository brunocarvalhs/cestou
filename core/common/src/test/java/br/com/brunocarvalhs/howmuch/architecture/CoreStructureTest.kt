package br.com.brunocarvalhs.howmuch.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * Guards the `core.common` package against drifting away from the set of layer folders already
 * established across the `core:*` modules. Unlike feature modules, core modules don't follow the
 * domain/data/presentation/navigation/di pattern - they group infra-style concerns instead
 * (repository, model, service, di, ...). This whitelist is shared by every `core:*` module so a
 * brand new, undisciplined folder (e.g. `helper`) fails here instead of silently sneaking in.
 * Root-level files (no subpackage) are always allowed, matching existing core module conventions.
 */
class CoreStructureTest {

    private val corePackage = "br.com.brunocarvalhs.howmuch.core.common"
    private val allowedLayers = setOf(
        "annotation", "base", "cloud", "components", "contract", "di", "dragdrop", "entity",
        "exception", "extensions", "initializer", "mobile", "model", "network", "registry",
        "repository", "security", "service", "services", "usecase", "util", "utils", "wear",
        "wearable",
    )

    @Test
    fun `common package only contains the allowed core layers`() {
        Konsist
            .scopeFromProject()
            .files
            .withPackage("$corePackage..")
            .assertTrue { file ->
                val remainder = file.packagee?.name.orEmpty().removePrefix(corePackage).removePrefix(".")

                remainder.isEmpty() || allowedLayers.contains(remainder.substringBefore("."))
            }
    }
}
