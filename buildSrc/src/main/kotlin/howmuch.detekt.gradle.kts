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

// Caminho absoluto: subprojetos não herdam o `config.setFrom` relativo configurado abaixo só
// para o projeto raiz (cada subprojeto tem sua própria instância de DetektExtension), e um
// caminho relativo a "buildSrc/" resolvido a partir de um subprojeto aponta para o lugar errado.
val detektConfigFile = rootProject.layout.projectDirectory.file("buildSrc/config/detekt/detekt.yml")

configure<DetektExtension> {
    toolVersion = detektVersion
    config.setFrom(detektConfigFile)
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

    // Baseline por módulo: registra as violações já existentes no rewrite (nunca tinha rodado
    // Detekt de verdade) para não bloquear PRs não relacionadas. Só passa a barrar problemas
    // NOVOS a partir daqui — o baseline fica versionado e visível em code review, não escondido.
    // Regenerar com `./gradlew detektBaseline` sempre que decidir zerar o débito de um módulo.
    configure<DetektExtension> {
        toolVersion = detektVersion
        config.setFrom(detektConfigFile)
        parallel = true
        buildUponDefaultConfig = true
        autoCorrect = true
        baseline = file("$projectDir/config/detekt/baseline.xml")
    }
}
