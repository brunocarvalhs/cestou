pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Cestou"
include(":app")
include(":core:common")
include(":core:domain")
include(":core:theme")
include(":core:ui")
include(":core:navigation")
include(":core:auth")
include(":core:data")
include(":core:ai")
include(":core:remote-config")
include(":core:analytics")
include(":feature:shopping")
include(":feature:products")
include(":feature:settings")
include(":feature:auth")
include(":feature:profile")
include(":feature:chat")
include(":feature:cart")
include(":feature:ai-agent")
include(":wear")
