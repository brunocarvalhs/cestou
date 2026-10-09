plugins {
    id("howmuch.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "br.com.brunocarvalhs.howmuch.core.theme"

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
}
