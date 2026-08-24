import io.gitlab.arturbosch.detekt.extensions.DetektExtension

// Plugin de convenção: mantém toda a configuração do Detekt fora do build.gradle.kts raiz.
// Aplicado no root via `id("howmuch.detekt")`.
//
// Scripts pré-compilados em buildSrc não têm acesso ao catálogo `libs` do build principal,
// então as versões abaixo são declaradas diretamente — mantenha sincronizadas com
// `detekt`/`rules-detekt-compose` em gradle/libs.versions.toml.
plugins {
    id("io.gitlab.arturbosch.detekt")
}

val detektVersion = "1.23.8"
val detektComposeRulesVersion = "0.4.22"

configure<DetektExtension> {
    toolVersion = detektVersion
    config.setFrom(files("config/detekt/detekt.yml"))
    parallel = true
    buildUponDefaultConfig = true
    autoCorrect = true
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    dependencies {
        "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
        "detektPlugins"("io.nlopez.compose.rules:detekt:$detektComposeRulesVersion")
    }
}
