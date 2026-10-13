plugins {
    id("howmuch.android.feature")
}

android {
    namespace = "br.com.brunocarvalhs.howmuch.feature.profile"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.navigation3.ui)

    implementation(libs.coil.compose)

    // Wear OS
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.wear.compose.navigation3)
    implementation(libs.androidx.wear.compose.ui.tooling)
}
