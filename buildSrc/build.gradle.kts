plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Kover e detekt não estão no catálogo de versões; as versões ficam aqui.
    implementation("org.jetbrains.kotlinx:kover-gradle-plugin:0.9.11")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")

    // Plugins aplicados pelas convenções howmuch.android.*. Com eles no classpath do buildSrc, os
    // build scripts devem aplicá-los por id e sem versão (o Gradle recusa versão para plugin que
    // já está no classpath).
    implementation(plugin(libs.plugins.android.library))
    implementation(plugin(libs.plugins.kotlin.compose))
    implementation(plugin(libs.plugins.kotlin.serialization))
    implementation(plugin(libs.plugins.google.devtools.ksp))
    implementation(plugin(libs.plugins.hilt))
}

// Converte um alias de plugin do catálogo no artefato marcador do plugin no Gradle.
fun plugin(plugin: Provider<PluginDependency>) =
    plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version.requiredVersion}" }
