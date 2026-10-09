plugins {
    id("howmuch.android.library")
    id("howmuch.android.hilt")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.secrets)
}

android {
    namespace = "br.com.brunocarvalhs.howmuch.feature.ai_agent"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:ai"))
    implementation(project(":core:remote-config"))

    implementation(libs.timber)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.generative.ai)

    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}
