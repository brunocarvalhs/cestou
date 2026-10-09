plugins {
    id("howmuch.android.feature")
}

android {
    namespace = "br.com.brunocarvalhs.howmuch.feature.cart"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ai"))
    implementation(project(":feature:chat"))
    implementation(project(":feature:products"))
    implementation(project(":feature:shopping"))

    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation.layout)

    implementation(libs.coil.compose)
}
