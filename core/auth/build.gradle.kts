plugins {
    id("howmuch.android.library")
    id("howmuch.android.hilt")
}

android {
    namespace = "br.com.brunocarvalhs.howmuch.core.auth"
}

dependencies {
    implementation(libs.androidx.startup)
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.crashlytics)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.timber)
    implementation(libs.androidx.datastore.preferences)
}
