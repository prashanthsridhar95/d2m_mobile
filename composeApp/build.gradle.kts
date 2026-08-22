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

// Applied conditionally, by plugin id (not alias(libs.plugins.googleServices)
// unconditionally in the block above) -- the google-services plugin FAILS
// THE BUILD outright if google-services.json isn't present, which would
// break every checkout that doesn't have its own Firebase project's json
// dropped in (this file is gitignored -- see .gitignore -- so a fresh clone
// or CI never has one by default). Already resolved onto the build
// classpath via the root build.gradle.kts's `apply false`, so applying it
// here by id alone (no version needed again) works.
//
// This is the actual missing piece behind push notifications not arriving
// at all, on or off the app: without it, FirebaseMessaging never mints a
// real token (PlatformPushInitializer.android.kt's FirebaseMessaging.getInstance()
// silently no-ops), so no DeviceToken row is ever registered server-side,
// and every push attempt resolves to "no_device_token" regardless of
// whether the backend's own FCM credentials (D2M_FCM_SERVICE_ACCOUNT_JSON,
// see d2m_core_engine/app/services/push_service.py) are configured. See
// README.md's push section for the exact steps (create a Firebase project,
// register this app under applicationId "com.d2m.app", download
// google-services.json to this directory).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
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
                // OkHttp, not the Android engine -- see HttpEngine.android.kt's
                // doc comment: Ktor's Android engine (HttpURLConnection-based)
                // doesn't implement WebSockets at all, which was the actual
                // root cause of every "messages/calls not sent" report this
                // whole session (confirmed via logcat: "Engine doesn't support
                // WebSocketCapability" on every single connect attempt).
                implementation(libs.ktor.client.okhttp)
                implementation(libs.androidx.activity.compose)
                // NotificationCompat.CallStyle/MessagingStyle + RemoteInput --
                // see push/LocalNotificationBridge.kt.
                implementation(libs.androidx.core.ktx)
                implementation(libs.sqldelight.android.driver)
                // The google-services Gradle plugin (applied conditionally,
                // above) + a real google-services.json (from your own
                // Firebase project, dropped into this directory) are what
                // actually let this initialize -- see README.md's push
                // section. Safe to keep on the classpath without one;
                // PlatformPushInitializer catches init failures rather than
                // crashing the app, it just never gets a real token.
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
                // Real Signal Protocol crypto primitives (X25519/AES/HMAC) --
                // see messaging/crypto/signal/CryptoPrimitives.android.kt.
                // Permissively licensed (MIT-style) -- deliberately NOT
                // Signal's own AGPLv3 `libsignal`, see that file's doc comment.
                implementation(libs.bouncycastle.provider)
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

// GIPHY API key for the GIF picker (messaging/ui/GifPicker.kt) -- read from
// local.properties (gitignored, never committed -- same file Android
// Studio already generates per-checkout for sdk.dir) rather than hardcoded
// in source, so a real per-developer/per-deployment key never ends up in
// git history. Falls back to GIPHY's own public "beta" key (rate-limited,
// shared across every app that hasn't set its own -- get a free key at
// https://developers.giphy.com/dashboard) when local.properties has none,
// matching d2m_web's own GifPicker.jsx fallback exactly.
val localProperties = java.util.Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val giphyApiKey: String = (localProperties.getProperty("GIPHY_API_KEY") ?: "dc6zaTOxFJmzC")

android {
    namespace = "com.d2m.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.d2m.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "GIPHY_API_KEY", "\"$giphyApiKey\"")
    }

    buildFeatures {
        buildConfig = true
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
