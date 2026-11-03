plugins {
    id("howmuch.android.feature")
}

android {
    namespace = "br.com.brunocarvalhs.howmuch.feature.feedback"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:remote-config"))

    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.play.review.ktx)
}
