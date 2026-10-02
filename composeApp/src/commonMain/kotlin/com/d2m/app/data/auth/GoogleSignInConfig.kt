package com.d2m.app.data.auth

/**
 * Google OAuth Client IDs for native Sign in with Google. Google Cloud
 * Console (console.cloud.google.com -> APIs & Services -> Credentials)
 * issues a separate OAuth 2.0 Client ID per platform identity, so Android
 * and iOS each need their own here -- neither is interchangeable with the
 * other, or with any web client ID an eventual d2m_web Google Sign-In
 * would use.
 *
 * PLACEHOLDERS -- the user is still setting up the Google Cloud OAuth
 * consent screen/clients as of this change. Both native SDK calls below
 * (GoogleSignInLauncher.android.kt / .ios.kt) are fully wired to read
 * these two constants; dropping in real values is the only remaining step
 * to make Google Sign-In actually work end to end. Until then, both calls
 * fail at runtime with an invalid-client error (Android: Credential
 * Manager's GetCredentialException with a 16-not-found-style message;
 * iOS: GIDSignIn's completion handler receives a non-nil NSError) --
 * neither crashes, both surface through GoogleSignInResult.Error, caught
 * in LoginScreen.kt like any other sign-in failure.
 */
object GoogleSignInConfig {
    // Android: passed to Credential Manager's
    // GetGoogleIdOption.Builder().setServerClientId(...) (see
    // GoogleSignInLauncher.android.kt). Important nuance worth getting
    // right in Google Cloud Console: Credential Manager's own docs
    // (developer.android.com/identity/sign-in/credential-manager-siwg)
    // say to use "your server's client ID, not your Android client ID" --
    // i.e. this constant should hold the WEB-application-type OAuth
    // client ID that Console associates with this app, NOT the separate
    // Android-type client you also register there (with this app's
    // applicationId "com.d2m.app" + your debug/release keystore SHA-1
    // fingerprints). That Android-type registration still has to exist
    // for Google's servers to recognize the calling app at all -- its own
    // client ID string is just never the one that goes here or over the
    // wire. Named "ANDROID_CLIENT_ID" to match this object's per-platform
    // naming, not because the value itself is an Android-type client.
    const val ANDROID_CLIENT_ID: String = "REPLACE_WITH_ANDROID_ASSOCIATED_WEB_CLIENT_ID.apps.googleusercontent.com"

    // iOS: passed to GIDConfiguration(clientID:) (see
    // GoogleSignInLauncher.ios.kt) -- this one really is the iOS-type
    // OAuth client ID Console issues for this app's bundle id
    // ("com.d2m.app" or whatever iosApp's real bundle id ends up being),
    // used directly, no web-client substitution needed. Once real, iosApp
    // also needs a URL scheme registered in Info.plist equal to this
    // client id's "reversed client ID" (e.g.
    // com.googleusercontent.apps.XXXXXXXX) -- standard GoogleSignIn-iOS
    // setup step, see GoogleSignInLauncher.ios.kt's doc comment.
    const val IOS_CLIENT_ID: String = "REPLACE_WITH_IOS_OAUTH_CLIENT_ID.apps.googleusercontent.com"
}
