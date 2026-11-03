package com.d2m.app.data.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import cocoapods.googlesignin.GIDConfiguration
import cocoapods.googlesignin.GIDSignIn
import cocoapods.googlesignin.GIDSignInResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS actual for [rememberGoogleSignInLauncher] -- the GoogleSignIn-iOS
 * CocoaPod (see composeApp/build.gradle.kts's `cocoapods { pod("GoogleSignIn") }`
 * block and iosApp/Podfile), called directly through Kotlin/Native's
 * auto-generated `cocoapods.googlesignin.*` bindings. Same "no Swift glue,
 * straight ObjC interop from iosMain" pattern
 * messaging/call/WebRtcEngine.ios.kt already established for the
 * WebRTC-lib pod -- see that file's own doc comment; this app has no
 * precedent for a Kotlin<->Swift bridge file for anything like this, so
 * this follows the WebRTC precedent instead of introducing a new pattern.
 *
 * NOT COMPILED against a real Apple toolchain/pod checkout as part of this
 * change (this sandbox has no Apple toolchain -- same caveat
 * WebRtcEngine.ios.kt's own doc comment states). Written from
 * GoogleSignIn-iOS's documented, long-stable public API shape:
 * `GIDSignIn.sharedInstance()`, `.configuration = GIDConfiguration(clientID:)`,
 * and `signInWithPresentingViewController:completion:` (unchanged since
 * SDK v6, current in v7.x) with a `GIDSignInResult(user: GIDGoogleUser)`
 * completion argument whose `.user?.idToken?.tokenString` is the raw
 * Google ID token JWT this app needs. The one spot most likely to need a
 * small fix once this actually builds against the real generated klib:
 * [GIDSignInErrorCode]'s exact cancellation code value used in
 * [isUserCancellation] below -- documented as -5
 * (kGIDSignInErrorCodeCanceled) in the pod's public header at
 * implementation time, but not independently confirmed against a real
 * compiled binding here, so worth double-checking once this is actually
 * run in Xcode. Any error that doesn't match falls back to
 * GoogleSignInResult.Error(message) either way, so a wrong constant here
 * degrades to "shows a real error message on cancel" rather than crashing
 * or silently misbehaving.
 *
 * Presenting view controller: `UIApplication.sharedApplication.keyWindow?
 * .rootViewController` -- the simplest correct source for a single-window
 * SwiftUI app like this one (see iosApp/iosApp/iOSApp.swift +
 * ContentView.swift: one `WindowGroup`, no multi-scene/multi-window UIKit
 * setup). `keyWindow` is deprecated in favor of per-`UIWindowScene`
 * lookup, but still functional for exactly this single-window shape; swap
 * to the scene-based API here (not at any call site) if that ever
 * actually breaks.
 *
 * Needs a real value in GoogleSignInConfig.IOS_CLIENT_ID, plus iosApp's
 * Info.plist registering a URL scheme equal to that client id's
 * "reversed client ID" (standard GoogleSignIn-iOS setup step -- see the
 * SDK's own "Start integrating Google Sign-In into your iOS app" guide)
 * once that id is real. Neither exists yet (see GoogleSignInConfig.kt).
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberGoogleSignInLauncher(onResult: (GoogleSignInResult) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()

    return {
        scope.launch {
            @Suppress("DEPRECATION")
            val presentingViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (presentingViewController == null) {
                onResult(GoogleSignInResult.Error("No presenting screen available for Google sign-in."))
                return@launch
            }

            try {
                val idToken = signInWithGoogle(presentingViewController)
                if (idToken.isNullOrBlank()) {
                    onResult(GoogleSignInResult.Error("Google didn't return an id token."))
                } else {
                    onResult(GoogleSignInResult.Success(idToken))
                }
            } catch (e: GoogleSignInCancelledException) {
                onResult(GoogleSignInResult.Cancelled)
            } catch (e: Exception) {
                onResult(GoogleSignInResult.Error(e.message ?: "Google sign-in failed."))
            }
        }
    }
}

private class GoogleSignInCancelledException : Exception()

/** GIDSignIn's own documented cancellation code -- see this file's class doc comment on why this isn't independently confirmed against a compiled binding. */
private const val GID_SIGN_IN_ERROR_CODE_CANCELED = -5L

@OptIn(ExperimentalForeignApi::class)
private suspend fun signInWithGoogle(presentingViewController: UIViewController): String? =
    suspendCancellableCoroutine { cont ->
        GIDSignIn.sharedInstance().configuration = GIDConfiguration(clientID = GoogleSignInConfig.IOS_CLIENT_ID)
        GIDSignIn.sharedInstance().signInWithPresentingViewController(presentingViewController) { result: GIDSignInResult?, error: NSError? ->
            when {
                error != null -> {
                    if (error.code == GID_SIGN_IN_ERROR_CODE_CANCELED) {
                        cont.resumeWithException(GoogleSignInCancelledException())
                    } else {
                        cont.resumeWithException(Exception(error.localizedDescription))
                    }
                }
                else -> cont.resume(result?.user?.idToken?.tokenString)
            }
        }
    }
