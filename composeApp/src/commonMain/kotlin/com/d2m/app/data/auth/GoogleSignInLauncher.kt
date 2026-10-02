package com.d2m.app.data.auth

import androidx.compose.runtime.Composable

/** Outcome of one native Google Sign-In attempt -- see [rememberGoogleSignInLauncher]. */
sealed interface GoogleSignInResult {
    /** [idToken] is the raw Google-issued ID token JWT, handed to WedLockApi.socialAuth() unmodified. */
    data class Success(val idToken: String) : GoogleSignInResult
    /** The user dismissed the account chooser/consent sheet -- not an error, LoginScreen.kt shows nothing for this. */
    data object Cancelled : GoogleSignInResult
    data class Error(val message: String) : GoogleSignInResult
}

/**
 * Native "Sign in with Google" launcher.
 *
 * Android actual (GoogleSignInLauncher.android.kt): Credential Manager
 * (androidx.credentials + Google's googleid library) -- the current
 * Google-recommended API, not the deprecated GoogleSignInClient
 * (com.google.android.gms.auth.api.signin).
 *
 * iOS actual (GoogleSignInLauncher.ios.kt): the GoogleSignIn-iOS SDK via
 * its CocoaPod, called directly through Kotlin/Native's auto-generated
 * `cocoapods.googlesignin.*` bindings -- no Swift glue code needed, same
 * "straight Kotlin/ObjC interop from iosMain" pattern
 * messaging/call/WebRtcEngine.ios.kt already established for the
 * WebRTC-lib pod (see that file's own doc comment). This app has no
 * existing precedent for a Kotlin<->Swift bridge for anything
 * UI-launching like this, so this follows the WebRTC precedent rather
 * than inventing a new one.
 *
 * `@Composable expect fun ... : () -> Unit` (not a plain suspend fun) --
 * matches messaging/ui/MediaAttachPicker.kt's existing convention for any
 * platform action that needs to launch OS-owned UI from a Compose screen
 * (there: a file/photo picker; here: Google's account chooser/consent
 * sheet) without the commonMain call site (LoginScreen.kt) importing
 * anything Android- or iOS-specific. The returned callback is invoked on
 * button click; [onResult] fires once, later, off of that click -- it is
 * NOT a suspend return value, because both platform SDKs hand results
 * back via a callback/activity-result channel, not a direct suspend
 * return, and MediaAttachPicker's `Context`/root-view-controller needs are
 * only available from `@Composable` scope (LocalContext.current on
 * Android; the current key window's root view controller on iOS) --
 * exactly why this needs to be requested and launched from composition,
 * like that file, rather than a plain top-level suspend fun IdentityStore.kt's
 * createSettings() expect/actual could otherwise have been modeled on.
 */
@Composable
expect fun rememberGoogleSignInLauncher(onResult: (GoogleSignInResult) -> Unit): () -> Unit
