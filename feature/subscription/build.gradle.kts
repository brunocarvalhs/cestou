plugins {
    id("howmuch.android.feature")
}

android {
    namespace = "br.com.brunocarvalhs.howmuch.feature.subscription"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
    implementation(project(":core:billing"))
    implementation(project(":core:remote-config"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
}
