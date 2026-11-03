pluginManagement {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "app.cash.sqldelight") {
                useModule("app.cash.sqldelight:gradle-plugin:${requested.version}")
            }
        }
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        google()
        mavenCentral()
    }
}

rootProject.name = "D2M"

include(":composeApp")
