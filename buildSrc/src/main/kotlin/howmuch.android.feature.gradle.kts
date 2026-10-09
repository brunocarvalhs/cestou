// Plugin de convenção: módulo feature/* (Compose, Hilt, navegação, serialização e o stack de UI
// que toda feature usa). Cada feature declara só o namespace e as dependências próprias.
// Aplicado via `id("howmuch.android.feature")`.
import com.android.build.api.dsl.LibraryExtension

plugins {
    id("howmuch.android.library")
    id("howmuch.android.hilt")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<LibraryExtension> {
    buildFeatures {
        compose = true
    }
}

dependencies {
    add("implementation", platform(libs.findLibrary("androidx-compose-bom").get()))
    listOf(
        "androidx-compose-ui",
        "androidx-compose-material3",
        "androidx-compose-material3-window-size",
        "androidx-compose-material-icons-extended",
        "androidx-compose-ui-tooling-preview",
        "androidx-lifecycle-runtime-compose",
        "androidx-lifecycle-viewmodel-compose",
        "androidx-navigation-compose",
        "androidx-navigation3-runtime",
        "hilt-navigation-compose",
        "timber",
        "kotlinx-serialization-json",
    ).forEach { add("implementation", libs.findLibrary(it).get()) }
    add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
    add("testImplementation", libs.findLibrary("androidx-compose-ui-test-junit4").get())
    add("testImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
}
