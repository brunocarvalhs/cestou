plugins {
    id("howmuch.android.library")
    id("howmuch.android.hilt")
}

android {
    namespace = "br.com.brunocarvalhs.howmuch.core.appfunctions"

    buildTypes {
        getByName("release") {
            consumerProguardFiles("consumer-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:remote-config"))
    implementation(project(":core:analytics"))

    // AppFunctions (API support for future annotation-based integration)
    // Note: AppFunctions compilation support to be enabled when API stabilizes
    implementation(libs.androidx.appfunctions)
    implementation(libs.androidx.appfunctions.service)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    // Coroutines
    api(libs.kotlinx.coroutines.core)

    // Timber for logging
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
