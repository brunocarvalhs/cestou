plugins {
    id("howmuch.android.library")
    id("howmuch.android.hilt")
}

android {
    namespace = "br.com.brunocarvalhs.howmuch.core.analytics"
}

dependencies {
    implementation(project(":core:common"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    implementation(libs.timber)
}
