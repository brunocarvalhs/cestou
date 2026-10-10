plugins {
    id("howmuch.android.library")
    id("howmuch.android.hilt")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.secrets)
}

android {
    namespace = "br.com.brunocarvalhs.howmuch.core.common"

    defaultConfig {
        buildConfigField("String", "VERSION_NAME", "\"1.0\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}

dependencies {
    implementation(libs.androidx.startup)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.timber)
    implementation(libs.play.services.wearable)
    implementation(libs.wear.remoteinteractions)
    implementation(libs.kotlinx.coroutines.guava)
}
