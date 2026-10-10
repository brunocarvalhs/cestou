plugins {
    id("howmuch.android.feature")
}

android {
    namespace = "br.com.brunocarvalhs.howmuch.feature.chat"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:data"))
    implementation(project(":core:ai"))

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.markdown.renderer.m3)

    implementation(libs.androidx.compose.material.icons.core)
}
