@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.websockets)

                implementation(libs.navigation.compose)
                implementation(libs.lifecycle.viewmodel.compose)
                implementation(libs.lifecycle.runtime.compose)

                implementation(libs.koin.core)
                implementation(libs.koin.compose)

                implementation(libs.multiplatform.settings)
                implementation(libs.multiplatform.settings.coroutines)

                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)

                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                implementation(libs.ktor.client.mock)
                implementation(compose.uiTest)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.android)
                implementation(libs.androidx.activity.compose)
                implementation(libs.sqldelight.android.driver)
                // Requires the google-services Gradle plugin + a real
                // google-services.json (from your own Firebase project) to
                // actually initialize -- see README.md's Phase 3 section.
                // Safe to keep on the classpath without one; PlatformPushInitializer
                // catches init failures rather than crashing the app.
                //
                // Written as a literal coordinate (not the libs.firebase.bom
                // catalog accessor) deliberately: this project's version
                // catalog previously had a `[versions]` key and a
                // `[libraries]` key both literally named "firebase-bom",
                // which produced a broken generated accessor here (Gradle
                // error: "Cannot convert ... map(valueof(DependencyValueSource))"
                // from platform(libs.firebase.bom)). The alias collision is
                // fixed in libs.versions.toml too (renamed to
                // firebase-bom-version), but this call stays a literal to
                // not depend on that accessor working correctly again.
                implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
                implementation(libs.firebase.messaging)
                // Real WebRTC (audio/video calling) -- see messaging/call/WebRtcEngine.android.kt.
                implementation(libs.stream.webrtc.android)
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.sqldelight.native.driver)
            }
        }
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
    }
}

android {
    namespace = "com.d2m.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.d2m.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

sqldelight {
    databases {
        create("D2MDatabase") {
            packageName.set("com.d2m.app.data.local")
        }
    }
}
