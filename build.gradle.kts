plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.sqldelight) apply false
    // Resolved onto the classpath here (apply false) so composeApp/build.gradle.kts
    // can apply it conditionally by plugin id -- only when a real
    // google-services.json is actually present, see that file's comment.
    // Push notifications don't work at all without this (no FCM token can
    // ever be minted), so this alone isn't sufficient -- see README.md.
    alias(libs.plugins.googleServices) apply false
    // Same conditional-apply pattern as googleServices above -- Crashlytics
    // needs the same real google-services.json to actually report anywhere.
    alias(libs.plugins.firebaseCrashlytics) apply false
}
