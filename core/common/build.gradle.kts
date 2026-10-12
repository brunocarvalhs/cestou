plugins {
    id("howmuch.android.library")
    id("howmuch.android.hilt")
    alias(libs.plugins.secrets)
}

android {
    namespace = "br.com.brunocarvalhs.howmuch.core.common"

    defaultConfig {
        buildConfigField("String", "VERSION_NAME", "\"1.0\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
}
