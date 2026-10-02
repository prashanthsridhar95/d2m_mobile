@file:OptIn(
    org.jetbrains.compose.ExperimentalComposeLibrary::class,
    org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class,
)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
// Explicit import (rather than the fully-qualified java.util.Properties()
// inline) -- the Android/Java Gradle plugin (applied below via
// androidApplication) exposes its own top-level `java { ... }` extension
// accessor (JavaPluginExtension), which shadows the bare `java` package
// identifier at this script's top level. A bare `java.util.Properties()`
// reference resolves `java` to THAT accessor first, not the java.util
// package, and fails with "Unresolved reference: util" -- a known Gradle
// Kotlin DSL gotcha in any AGP/Java-plugin project, not specific to this
// file. Importing the class directly sidesteps the ambiguity entirely.
import java.util.Properties
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
    // Applied via plain `id(...)` with no version, not `alias(libs.plugins...)`
    // -- see gradle/libs.versions.toml's kotlinCocoapods comment for why
    // (both a version-catalog alias with a pinned version AND one with no
    // version at all fail here, for two different reasons -- confirmed
    // directly). This plugin ships bundled inside the same
    // kotlin-gradle-plugin artifact `org.jetbrains.kotlin.multiplatform`
    // above already resolved, so it needs no version of its own.
    id("org.jetbrains.kotlin.native.cocoapods")
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
    // Crash/non-fatal/ANR reporting -- same conditional-apply reasoning as
    // google-services above (this plugin also needs a real Firebase project
    // to report anywhere, and would break a checkout with no google-services.json).
    apply(plugin = "com.google.firebase.crashlytics")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // iosX64 (the legacy Intel-simulator target) deliberately dropped --
    // confirmed directly: Compose Multiplatform 1.11.1 doesn't publish an
    // iosX64 variant of its own artifacts ("Couldn't resolve dependency
    // 'org.jetbrains.compose.ui:ui:1.11.1' in 'iosMain' for all target
    // platforms... Unresolved platforms: [iosX64]"), and because `iosMain`
    // is the shared intermediate source set every iOS target (including
    // iosX64) depends on, that one unresolvable target was poisoning
    // dependency resolution for the WHOLE iosMain hierarchy -- symptom:
    // even fully valid, confirmed-present symbols (kotlinx-datetime's
    // Clock.System, verified present via `klib dump-metadata` against the
    // real downloaded klib) were unresolvable project-wide. iosX64 has no
    // practical use here anyway -- this project only ever builds/runs for
    // iosSimulatorArm64 (Apple Silicon simulator) or a physical arm64
    // device, never the old Intel simulator.
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            // Works around a real Kotlin/Native compiler-cache bug when
            // building the cache for androidx.navigation's navigation-runtime
            // klib (a reified inline function, getBackStackEntry<T>, isn't
            // found by the cache serializer during linkPodDebugFramework*,
            // confirmed directly). gradle.properties'
            // kotlin.native.cacheKind.<target>=none was the pre-2.3.20 way
            // to do this; it's deprecated/unsupported now and silently did
            // nothing here -- this DSL call is the current replacement (see
            // https://kotl.in/disable-native-cache).
            // DisableCacheInKotlinVersion is a sealed class of version-
            // specific singleton objects (confirmed: it has no `.entries`,
            // that's an enum-only member), named with backticks since they
            // start with a digit -- `2_4_0` is the highest defined for this
            // project's Kotlin Gradle Plugin version (2.4.10).
            // `2_4_0` was itself flagged deprecated at build time ("update
            // to the latest version constant") -- this project is on Kotlin
            // 2.4.10, so using that exact constant instead.
            disableNativeCache(
                version = DisableCacheInKotlinVersion.`2_4_10`,
                reason = "Compiler cache build fails on androidx.navigation's navigation-runtime klib (getBackStackEntry<T> not found by cache serializer)",
            )
        }
    }

    // WebRtcEngine.ios.kt's real (not stubbed) WebRTC implementation needs
    // this pod's Kotlin/Native bindings (`platform.WebRTC.*`) -- see that
    // file's own doc comment for the full design. The kotlinCocoapods
    // plugin generates a `ComposeApp.podspec` in this module's directory on
    // the next Gradle sync; iosApp/Podfile (see that file's own comments)
    // references it so `pod install` pulls both GoogleWebRTC AND this
    // module's own framework into one Xcode workspace -- this is the
    // standard KMP+CocoaPods+Xcode wiring, not a d2m-specific pattern.
    //
    // A checkout with no Xcode project yet (iosApp/README.md) is entirely
    // unaffected by this block -- CocoaPods integration only matters once
    // `pod install` is actually run against a real .xcodeproj/.xcworkspace,
    // which doesn't exist in this repo yet either way.
    cocoapods {
        // Explicit, deliberately -- without this, the plugin derives the
        // podspec's internal `s.name` from the raw Gradle module directory
        // name ("composeApp", lowercase c), while the GENERATED FILE itself
        // is still named ComposeApp.podspec (capital C, matching
        // `framework.baseName` below) -- CocoaPods then refuses the
        // mismatch outright ("name of the given podspec `composeApp`
        // doesn't match the expected one `ComposeApp`", confirmed
        // directly). Setting `name` here makes both consistent.
        name = "ComposeApp"
        version = "1.0.0"
        summary = "D2M shared Kotlin Multiplatform module"
        homepage = "https://d2m.app"
        ios.deploymentTarget = "15.0"

        framework {
            baseName = "ComposeApp"
            isStatic = true
            // This is a SEPARATE Framework object from the plain
            // iosTarget.binaries.framework {} block above -- the cocoapods
            // plugin's own linkPodDebugFramework* task (the one that was
            // actually failing with "Failed to build cache for .../
            // navigation-runtime.klib") reads from THIS block, confirmed by
            // the task name itself ("Pod"). Setting disableNativeCache only
            // on the other framework block had no effect on this failure.
            disableNativeCache(
                version = DisableCacheInKotlinVersion.`2_4_10`,
                reason = "Compiler cache build fails on androidx.navigation's navigation-runtime klib (getBackStackEntry<T> not found by cache serializer)",
            )
        }

        // GoogleWebRTC (Google's own official pod) has been deprecated
        // and device-binary-only since M80 (2020) -- confirmed directly via
        // a real link failure: "ld: building for iOS-simulator, but linking
        // in dylib ... built for iOS" when linking against it for the
        // simulator, because its WebRTC.framework is a legacy single-slice
        // fat framework, not an xcframework, and can't represent a device +
        // simulator arm64 split at all. stasel/WebRTC's `WebRTC-lib` pod is
        // the standard, actively-maintained community replacement --
        // ships a real xcframework with proper device AND simulator
        // (arm64 + x86_64) slices, is a drop-in (`import WebRTC`, same
        // Clang module name), and is what most real-world WebRTC-on-iOS
        // projects have used since Google's deprecation.
        //
        // moduleName = "WebRTC" -- REQUIRED, confirmed directly ("fatal
        // error: module 'GoogleWebRTC' not found" from cinterop otherwise
        // when this was still the GoogleWebRTC pod). WebRTC-lib ships the
        // same `WebRTC` Clang module name, so this stays unchanged.
        // packageName is set explicitly too, so the generated Kotlin
        // bindings land at a package this project controls (cocoapods.webrtc.*,
        // matching WebRtcEngine.ios.kt's imports) rather than guessing at
        // whatever the plugin would have derived from the pod name by
        // default -- this also means WebRtcEngine.ios.kt's imports don't
        // need to change at all for this swap.
        pod("WebRTC-lib") {
            moduleName = "WebRTC"
            packageName = "cocoapods.webrtc"
        }

        // Native Google Sign-In on iOS (GoogleSignInLauncher.ios.kt) --
        // Google's official pod, same "Kotlin/Native cinterop bindings
        // generated straight from the pod, no Swift glue" wiring as
        // WebRTC-lib above. moduleName/packageName set explicitly for the
        // same reason as that block: this project prefers an explicit,
        // predictable generated package over whatever the plugin would
        // derive from the pod name on its own.
        pod("GoogleSignIn") {
            version = "~> 7.1"
            moduleName = "GoogleSignIn"
            packageName = "cocoapods.googlesignin"
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
                // project.dependencies.platform(...) -- NOT the bare
                // platform(...) KMP DSL convenience call this used to be.
                // Confirmed directly: after bumping to Kotlin/KGP 2.4.10
                // (see libs.versions.toml's own comment for why), the bare
                // form fails with "Unresolved reference: platform" inside
                // this source set's dependencies {} block. Going through
                // project.dependencies explicitly reaches Gradle's own
                // stable, version-independent DependencyHandler.platform()
                // instead of whatever KotlinDependencyHandler's own
                // convenience method resolved to (or stopped resolving to)
                // in this KGP version.
                implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.4.0"))
                implementation(libs.firebase.messaging)
                // Crash/non-fatal/ANR reporting -- see GifConfig.android.kt's
                // sibling, messaging/CrashReporter.android.kt, for where
                // recordException actually gets called from commonMain code.
                implementation(libs.firebase.crashlytics)
                // Real WebRTC (audio/video calling) -- see messaging/call/WebRtcEngine.android.kt.
                implementation(libs.stream.webrtc.android)
                // Real Signal Protocol crypto primitives (X25519/AES/HMAC) --
                // see messaging/crypto/signal/CryptoPrimitives.android.kt.
                // Permissively licensed (MIT-style) -- deliberately NOT
                // Signal's own AGPLv3 `libsignal`, see that file's doc comment.
                implementation(libs.bouncycastle.provider)
                // Animated-GIF decoding for chat media bubbles -- Android-only
                // artifact (no iOS/Native variant), see messaging/ui/GifImageLoader.kt.
                implementation(libs.coil.gif)
                // In-app full-screen video playback for the media viewer
                // ("provide a image & video viewer - shouldnt be going
                // outside the app") -- see messaging/ui/VideoPlayerView.android.kt.
                implementation(libs.media3.exoplayer)
                implementation(libs.media3.ui)
                // Native Google Sign-In (Credential Manager, replacing the
                // deprecated GoogleSignInClient) -- see
                // data/auth/GoogleSignInLauncher.android.kt and
                // GoogleSignInConfig.kt's ANDROID_CLIENT_ID placeholder.
                implementation(libs.androidx.credentials)
                implementation(libs.androidx.credentials.play.services.auth)
                implementation(libs.googleid)
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.sqldelight.native.driver)
            }
        }
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
val localProperties = Properties().apply {
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

    // Pre-existing, and not related to any UI work: two jars already on
    // this module's Android classpath ship the same OSGi metadata path --
    // bcprov-jdk18on (the Signal Protocol crypto primitives, see
    // messaging/crypto/signal/CryptoPrimitives.android.kt) and jspecify
    // (pulled in transitively) both carry
    // META-INF/versions/9/OSGI-INF/MANIFEST.MF, which fails
    // mergeDebugJavaResource outright:
    //   "2 files found with path 'META-INF/versions/9/OSGI-INF/MANIFEST.MF'"
    // so `assembleDebug` could not produce an APK at all. Nothing reads
    // these files at runtime (OSGi bundle metadata is for OSGi
    // containers), so excluding the whole META-INF/versions/9/OSGI-INF
    // tree is the standard fix rather than picking one jar's copy.
    packaging {
        resources {
            excludes += "/META-INF/versions/9/OSGI-INF/**"
        }
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

// Compose UI tests (`runComposeUiTest`, see commonTest's D2MButtonTest) need
// a real Android environment on the Android target -- on the plain JVM
// `testDebugUnitTest` task they fail with an NPE inside setContent, because
// the Android artifact behind compose.uiTest delegates to
// ui-test-junit4 and there is no Activity/Looper/resource table there.
// On Android these are INSTRUMENTED tests by nature; running them here would
// need Robolectric wired up specifically for that.
//
// They are excluded here rather than deleted because they are not
// Android-specific tests at all -- they live in commonTest and DO run, and
// pass, on iosSimulatorArm64Test, which is where this project actually
// needed Compose UI coverage. Excluding keeps `testDebugUnitTest` honest
// (green means green) without giving up the coverage on the target that can
// provide it.
tasks.withType<Test>().configureEach {
    filter {
        excludeTestsMatching("com.d2m.app.ui.components.*")
        isFailOnNoMatchingTests = false
    }
}

sqldelight {
    databases {
        create("D2MDatabase") {
            packageName.set("com.d2m.app.data.local")
        }
    }
}
