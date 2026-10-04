package com.d2m.app.data.auth

/**
 * Google OAuth Client IDs for native Sign in with Google. Google Cloud
 * Console (console.cloud.google.com -> APIs & Services -> Credentials)
 * issues a separate OAuth 2.0 Client ID per platform identity, so Android
 * and iOS each need their own here -- neither is interchangeable with the
 * other, or with any web client ID an eventual d2m_web Google Sign-In
 * would use.
 *
 * Real values below (Google Cloud Console project "project-8a229145-29c3-
 * 44c7-92d" / Firebase project "zyke-31dd1"), filled in from the client
 * JSON/plist Console issued. ANDROID_CLIENT_ID is deliberately the WEB-
 * application client (see that constant's own comment on why); a
 * separate Android-type registration (package com.d2m.app + the debug/
 * release keystore SHA-1) also has to exist in Console for Google to
 * recognize the signed APK at all, even though its own client ID string
 * is never used here.
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
    const val ANDROID_CLIENT_ID: String = "835063984447-poe8is01fjqen38lko8m789c2me2bb3m.apps.googleusercontent.com"

    // iOS: passed to GIDConfiguration(clientID:) (see
    // GoogleSignInLauncher.ios.kt) -- this one really is the iOS-type
    // OAuth client ID Console issues for this app's bundle id
    // ("com.d2m.app" or whatever iosApp's real bundle id ends up being),
    // used directly, no web-client substitution needed. Once real, iosApp
    // also needs a URL scheme registered in Info.plist equal to this
    // client id's "reversed client ID" (e.g.
    // com.googleusercontent.apps.XXXXXXXX) -- standard GoogleSignIn-iOS
    // setup step, see GoogleSignInLauncher.ios.kt's doc comment.
    const val IOS_CLIENT_ID: String = "835063984447-cbblmgiad01ce020fmmr1bmbvdd8e42a.apps.googleusercontent.com"
}
