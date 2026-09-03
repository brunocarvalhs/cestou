plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Mantenha sincronizado com `kover`/`detekt` em gradle/libs.versions.toml — buildSrc resolve
    // suas próprias dependências e não compartilha o catálogo de versão do build principal.
    implementation("org.jetbrains.kotlinx:kover-gradle-plugin:0.9.8")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")
}
