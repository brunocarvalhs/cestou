plugins {
    id("howmuch.android.library")
    id("howmuch.android.hilt")
}

android {
    namespace = "br.com.brunocarvalhs.howmuch.core.network"
}

dependencies {
    implementation(libs.androidx.startup)
    implementation(project(":core:common"))
    implementation(project(":core:domain"))

    // Network
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.coroutines.core)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
