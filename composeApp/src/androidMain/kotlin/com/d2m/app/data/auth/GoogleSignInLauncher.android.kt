package com.d2m.app.data.auth

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

/**
 * Android actual for [rememberGoogleSignInLauncher] -- Credential Manager
 * (androidx.credentials.CredentialManager.getCredential) with Google's
 * GetGoogleIdOption/GoogleIdTokenCredential (com.google.android.libraries.
 * identity.googleid), matching the current documented flow at
 * developer.android.com/identity/sign-in/credential-manager-siwg. This is
 * the Google-recommended replacement for the older
 * com.google.android.gms.auth.api.signin.GoogleSignInClient API, which is
 * deprecated and deliberately not used here.
 *
 * setFilterByAuthorizedAccounts(false) -- shows every Google account on
 * the device, not just ones that have already granted this app consent;
 * correct for a combined login/signup entry point like LoginScreen.kt
 * (a first-time user has no "authorized account" yet, so filtering to
 * true would show them an empty picker).
 *
 * Needs the androidx.credentials / androidx.credentials.play-services-auth
 * / com.google.android.libraries.identity.googleid Gradle dependencies
 * declared in composeApp/build.gradle.kts's androidMain source set, and a
 * real value in GoogleSignInConfig.ANDROID_CLIENT_ID (see that file's doc
 * comment on which Console client type actually belongs there).
 */
@Composable
actual fun rememberGoogleSignInLauncher(onResult: (GoogleSignInResult) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    return {
        scope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(GoogleSignInConfig.ANDROID_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val credentialManager = CredentialManager.create(context)
                val response = credentialManager.getCredential(context, request)

                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
                onResult(GoogleSignInResult.Success(googleIdTokenCredential.idToken))
            } catch (e: GetCredentialCancellationException) {
                onResult(GoogleSignInResult.Cancelled)
            } catch (e: GoogleIdTokenParsingException) {
                Log.w("GoogleSignIn", "Couldn't parse Google's credential", e)
                onResult(GoogleSignInResult.Error("Couldn't read Google's response. Try again."))
            } catch (e: GetCredentialException) {
                Log.w("GoogleSignIn", "Credential Manager sign-in failed", e)
                onResult(GoogleSignInResult.Error(e.message ?: "Google sign-in failed."))
            }
        }
    }
}
