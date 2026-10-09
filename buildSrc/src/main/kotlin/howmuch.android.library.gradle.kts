// Plugin de convenção: base de todo módulo Android library do projeto (SDKs, Java, runner de
// teste e dependências de teste unitário comuns). Aplicado via `id("howmuch.android.library")`.
import com.android.build.api.dsl.LibraryExtension

plugins {
    id("com.android.library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<LibraryExtension> {
    compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()

    defaultConfig {
        minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    listOf("junit", "mockk", "kotlinx-coroutines-test", "turbine", "robolectric", "androidx-junit", "androidx-core")
        .forEach { add("testImplementation", libs.findLibrary(it).get()) }
}
