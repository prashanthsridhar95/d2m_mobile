package com.d2m.app.ui.screens.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.d2m.app.data.auth.GoogleSignInResult
import com.d2m.app.data.auth.rememberGoogleSignInLauncher
import com.d2m.app.data.network.WedLockApi
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.ui.components.D2MBrand
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.LanguageSwitcherCompact
import com.d2m.app.ui.components.MetaText
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.components.D2MTabs
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.strings.LocaleStore
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Mirrors screens/entry/LoginScreen.jsx, now backed by WedLock IAM's real
 * email+password+OTP auth (see data/network/WedLockApi.kt) instead of the
 * old dev-mode "paste an id" stand-in this replaces.
 *
 * Two panes:
 *
 * Log in -- a real WedLock POST /auth/login credential check, then GET /me
 * (IdentityRepository.getMe(), app/routers/identity.py) to resolve that
 * straight to a D2M account -- the common case (a fully onboarded WedLock
 * account) needs nothing further. The old "confirm sponsor/primary id"
 * step still exists as a fallback for GET /me's own 404: a WedLock account
 * that logged in fine but never completed D2M's own onboarding (POST
 * /sponsors or /invites/redeem) has no Sponsor/Primary row to resolve to
 * yet.
 *
 * Register -- WedLock's OTP send/verify -> set password -> register/parent
 * -> auto-login sequence (registration returns no token, so login() always
 * follows it). Deliberately parent-only: a PARENT_GUARDIAN WedLock account
 * is what OnboardingWizardScreen.kt's createSponsor() call needs to have
 * already stored before it runs (it forwards the token transparently via
 * ApiClient, no changes needed there). A child's own SELF WedLock account
 * is registered later, inside ClaimFlowScreen.kt, at the point they redeem
 * an invite link -- see that file's doc comment for why that's a separate
 * flow rather than reusing this one.
 *
 * Google -- both panes also offer a native "Continue with Google" button
 * (GoogleAuthButton() below), backed by data/auth/GoogleSignInLauncher.kt's
 * platform actuals and WedLockApi.socialAuth(). It always sends
 * accountType = "PARENT_GUARDIAN" on both panes, matching this whole
 * screen's existing parent-only convention above -- a brand-new Google
 * signup here creates the same kind of account the password Register pane
 * does, never a SELF account (SELF signup, Google or otherwise, only
 * happens from ClaimFlowScreen.kt's own invite-redemption flow, which this
 * change doesn't touch). Because WedLock looks up an EXISTING account by
 * the Google token's "sub" claim regardless of accountType (see
 * WedLockApi.kt's socialAuth() doc comment), sending PARENT_GUARDIAN
 * unconditionally is also safe for a returning user on the Log in pane
 * who originally registered some other way.
 */
@Composable
fun LoginScreen(
    onLoginAsParent: (sponsorId: String) -> Unit,
    onLoginAsChild: (primaryId: String) -> Unit,
    onRegister: () -> Unit,
) {
    val identityRepo: IdentityRepository = koinInject()
    val identityStore: IdentityStore = koinInject()
    val wedLockApi: WedLockApi = koinInject()
    val localeStore: LocaleStore = koinInject()
    val scope = rememberCoroutineScopeSafe()

    var tab by remember { mutableStateOf(0) } // 0 = Login, 1 = Register
    val strings = LocalStrings.current.login

    D2MTheme(flow = D2MFlow.ENTRY) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        // "Change language from... login screen," reported directly --
        // available before any identity exists, same reasoning as
        // d2m_web's own LoginScreen.jsx LanguageSwitcherCompact placement.
        LanguageSwitcherCompact(
            localeStore = localeStore,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.TopEnd)
                .padding(top = 24.dp, end = 24.dp),
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            D2MBrand(markSize = 34.dp)
            MetaText(
                strings.tagline,
                Modifier.padding(top = 8.dp),
            )

            Column(modifier = Modifier.padding(top = 24.dp).widthIn(max = 420.dp)) {
                D2MTabs(
                    titles = listOf(strings.tabLogIn, strings.tabRegister),
                    selectedIndex = tab,
                    onSelect = { i -> tab = i },
                )

                if (tab == 0) {
                    LoginPane(
                        identityRepo = identityRepo,
                        identityStore = identityStore,
                        wedLockApi = wedLockApi,
                        scope = scope,
                        onLoginAsParent = onLoginAsParent,
                        onLoginAsChild = onLoginAsChild,
                    )
                } else {
                    RegisterPane(
                        identityStore = identityStore,
                        wedLockApi = wedLockApi,
                        scope = scope,
                        onRegistered = onRegister,
                    )
                }
            }
        }
        }
    }
}

/** Phase A: real WedLock credential check. Phase B (only on GET /me's own
 * 404 -- see LoginScreen's doc comment): existing D2M sponsor/primary id
 * confirm, as a fallback for a WedLock account with no D2M record yet. */
@Composable
private fun LoginPane(
    identityRepo: IdentityRepository,
    identityStore: IdentityStore,
    wedLockApi: WedLockApi,
    scope: kotlinx.coroutines.CoroutineScope,
    onLoginAsParent: (sponsorId: String) -> Unit,
    onLoginAsChild: (primaryId: String) -> Unit,
) {
    var authenticated by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var authLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    var idInput by remember { mutableStateOf("") }
    var idIsParent by remember { mutableStateOf(true) }
    var confirmName by remember { mutableStateOf<String?>(null) }
    var idLoading by remember { mutableStateOf(false) }
    var idError by remember { mutableStateOf<String?>(null) }
    val strings = LocalStrings.current.login

    // Tries GET /me right after a WedLock session is established (see this
    // file's own doc comment). True on success -- identity is set and the
    // relevant onLoginAs*/onComplete callback has already fired, nothing
    // left for the caller to do. False means "couldn't resolve, fall back
    // to the manual id-entry step" -- covers both the real 404 (no D2M
    // account linked yet) and any other failure (network, backend down),
    // since stranding the user instead of falling back to the
    // already-working manual flow would be strictly worse.
    suspend fun resolveAndEnter(): Boolean {
        resolving = true
        try {
            val me = identityRepo.getMe()
            val sponsorId = me.sponsorId
            val primaryId = me.primaryId
            return when {
                me.role == "parent" && sponsorId != null -> {
                    identityStore.setParent(sponsorId)
                    onLoginAsParent(sponsorId)
                    true
                }
                me.role == "child" && primaryId != null -> {
                    identityStore.setChild(primaryId)
                    onLoginAsChild(primaryId)
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            return false
        } finally {
            resolving = false
        }
    }

    Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (resolving) {
            Text(strings.resolving, style = MaterialTheme.typography.bodyMedium, color = mutedText(0.55f))
        } else if (!authenticated) {
            Text(strings.logInWithWedlock, style = MaterialTheme.typography.bodyMedium, color = mutedText(0.55f))

            D2MTextField(label = strings.emailLabel, value = email, onValueChange = { email = it; authError = null }, keyboardType = KeyboardType.Email)
            D2MTextField(label = strings.passwordLabel, value = password, onValueChange = { password = it; authError = null }, isPassword = true)

            authError?.let { D2MErrorBanner(it) }

            D2MButton(
                text = if (authLoading) strings.logInBusy else strings.logIn,
                enabled = email.isNotBlank() && password.isNotBlank() && !authLoading,
                onClick = {
                    scope.launch {
                        authLoading = true
                        authError = null
                        try {
                            val tokens = wedLockApi.login(emailOrPhone = email, password = password)
                            identityStore.setWedlockAccessToken(tokens.accessToken)
                            if (!resolveAndEnter()) authenticated = true
                        } catch (e: Exception) {
                            authError = friendlyError(e, strings.errLoginFailed)
                        } finally {
                            authLoading = false
                        }
                    }
                },
            )

            MetaText(strings.or, Modifier.padding(top = 4.dp))

            GoogleAuthButton(
                label = strings.continueWithGoogle,
                enabled = !authLoading,
                identityStore = identityStore,
                wedLockApi = wedLockApi,
                scope = scope,
                onError = { authError = it },
                onSuccess = { scope.launch { if (!resolveAndEnter()) authenticated = true } },
            )
        } else {
            Text(strings.whichProfile, style = MaterialTheme.typography.bodyMedium, color = mutedText(0.55f))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                D2MButton(
                    text = strings.imAParent,
                    onClick = { idIsParent = true },
                    variant = if (idIsParent) com.d2m.app.ui.components.D2MButtonVariant.SOLID else com.d2m.app.ui.components.D2MButtonVariant.OUTLINE,
                )
                D2MButton(
                    text = strings.imTheChild,
                    onClick = { idIsParent = false },
                    variant = if (!idIsParent) com.d2m.app.ui.components.D2MButtonVariant.SOLID else com.d2m.app.ui.components.D2MButtonVariant.OUTLINE,
                )
            }

            D2MTextField(label = if (idIsParent) strings.sponsorIdLabel else strings.primaryIdLabel, value = idInput, onValueChange = { idInput = it; confirmName = null })

            idError?.let { D2MErrorBanner(it) }

            if (confirmName == null) {
                D2MButton(
                    text = if (idLoading) strings.checking else strings.continueAction,
                    enabled = idInput.isNotBlank() && !idLoading,
                    onClick = {
                        scope.launch {
                            idLoading = true
                            idError = null
                            try {
                                confirmName = if (idIsParent) {
                                    identityRepo.getSponsorProfile(idInput).name
                                } else {
                                    identityRepo.getPrimaryProfile(idInput).name
                                }
                            } catch (e: Exception) {
                                // Surfaces the real message rather than a hardcoded
                                // "couldn't find that id" for every exception -- see
                                // the equivalent comment this replaced for why that
                                // used to hide real bugs.
                                idError = friendlyError(e, strings.errIdNotFound)
                            } finally {
                                idLoading = false
                            }
                        }
                    },
                )
            } else {
                Text(strings.loggingInAsConfirm(confirmName!!), style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    D2MButton(
                        text = strings.confirm,
                        onClick = {
                            if (idIsParent) {
                                identityStore.setParent(idInput)
                                onLoginAsParent(idInput)
                            } else {
                                identityStore.setChild(idInput)
                                onLoginAsChild(idInput)
                            }
                        },
                    )
                    D2MButton(text = strings.notMe, variant = com.d2m.app.ui.components.D2MButtonVariant.OUTLINE, onClick = { confirmName = null; idInput = "" })
                }
            }
        }
    }
}

/** WedLock's otp/send -> otp/verify -> register/parent -> login sequence, then hands off to onRegistered() (RolePickerScreen -> OnboardingWizardScreen). */
@Composable
private fun RegisterPane(
    identityStore: IdentityStore,
    wedLockApi: WedLockApi,
    scope: kotlinx.coroutines.CoroutineScope,
    onRegistered: () -> Unit,
) {
    var step by remember { mutableStateOf(0) } // 0 = email, 1 = otp, 2 = password
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val strings = LocalStrings.current.login

    Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(strings.setUpAsParent, style = MaterialTheme.typography.bodyMedium, color = mutedText(0.55f))

        when (step) {
            0 -> {
                D2MTextField(label = strings.emailLabel, value = email, onValueChange = { email = it; error = null }, keyboardType = KeyboardType.Email)
                error?.let { D2MErrorBanner(it) }
                D2MButton(
                    text = if (loading) strings.sendCodeBusy else strings.sendCode,
                    enabled = email.isNotBlank() && !loading,
                    onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                wedLockApi.sendRegistrationOtp(email)
                                step = 1
                            } catch (e: Exception) {
                                error = friendlyError(e, strings.errSendCodeFailed)
                            } finally {
                                loading = false
                            }
                        }
                    },
                )

                MetaText(strings.or, Modifier.padding(top = 4.dp))

                // Skips OTP/password entirely -- a verified Google identity
                // already establishes ownership of the email address, same
                // reason WedLockApi.socialAuth() is one round trip instead
                // of register-then-login. See this file's own top doc
                // comment on why accountType is always PARENT_GUARDIAN here.
                GoogleAuthButton(
                    label = strings.signUpWithGoogle,
                    enabled = !loading,
                    identityStore = identityStore,
                    wedLockApi = wedLockApi,
                    scope = scope,
                    onError = { error = it },
                    onSuccess = onRegistered,
                )
            }
            1 -> {
                Text(strings.otpSentTo(email), style = MaterialTheme.typography.bodySmall, color = mutedText(0.55f))
                D2MTextField(label = strings.verificationCodeLabel, value = otp, onValueChange = { otp = it; error = null }, keyboardType = KeyboardType.Number)
                error?.let { D2MErrorBanner(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    D2MButton(
                        text = if (loading) strings.verifyBusy else strings.verify,
                        enabled = otp.length == 6 && !loading,
                        onClick = {
                            scope.launch {
                                loading = true
                                error = null
                                try {
                                    wedLockApi.verifyRegistrationOtp(email, otp)
                                    step = 2
                                } catch (e: Exception) {
                                    error = friendlyError(e, strings.errOtpInvalid)
                                } finally {
                                    loading = false
                                }
                            }
                        },
                    )
                    D2MButton(text = strings.back, variant = com.d2m.app.ui.components.D2MButtonVariant.OUTLINE, onClick = { step = 0; otp = ""; error = null })
                }
            }
            2 -> {
                D2MTextField(
                    label = strings.passwordLabel,
                    value = password,
                    onValueChange = { password = it; error = null },
                    isPassword = true,
                    hint = strings.passwordHint,
                )
                error?.let { D2MErrorBanner(it) }
                D2MButton(
                    text = if (loading) strings.createAccountBusy else strings.createAccount,
                    enabled = password.length >= 8 && !loading,
                    onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                wedLockApi.registerParent(email, password)
                                val tokens = wedLockApi.login(emailOrPhone = email, password = password)
                                identityStore.setWedlockAccessToken(tokens.accessToken)
                                onRegistered()
                            } catch (e: Exception) {
                                error = friendlyError(e, strings.errCreateAccountFailed)
                            } finally {
                                loading = false
                            }
                        }
                    },
                )
            }
        }
    }
}

// Small helper so this file doesn't need an extra import block per screen --
// every screen in this app follows this same rememberCoroutineScope() pattern.
@Composable
private fun rememberCoroutineScopeSafe() = androidx.compose.runtime.rememberCoroutineScope()

/**
 * Shared "Continue/Sign up with Google" button for both LoginPane and
 * RegisterPane above -- one native sign-in call
 * (rememberGoogleSignInLauncher(), see data/auth/GoogleSignInLauncher.kt),
 * then WedLockApi.socialAuth() with the resulting Google ID token,
 * identical token storage to the password flows' wedLockApi.login()/
 * identityStore.setWedlockAccessToken() calls above. [onSuccess] intentionally
 * takes no parameters -- LoginPane and RegisterPane each already know what
 * "signed in" means for their own step-machine (flip `authenticated`, or
 * call onRegistered()), so this stays agnostic to both.
 *
 * rememberGoogleSignInLauncher() is called unconditionally here (every
 * composition of this button), matching the exact call-then-invoke-later
 * pattern messaging/ui/ChatPane.kt already uses for
 * rememberMediaAttachLauncher() -- required because it's itself
 * @Composable (it needs LocalContext.current on Android / the current key
 * window on iOS, both only available from composition), not something
 * that can be deferred into the onClick lambda below.
 */
@Composable
private fun GoogleAuthButton(
    label: String,
    enabled: Boolean,
    identityStore: IdentityStore,
    wedLockApi: WedLockApi,
    scope: kotlinx.coroutines.CoroutineScope,
    onError: (String) -> Unit,
    onSuccess: () -> Unit,
) {
    var signingIn by remember { mutableStateOf(false) }
    val strings = LocalStrings.current.login

    val launchGoogleSignIn = rememberGoogleSignInLauncher { result ->
        when (result) {
            is GoogleSignInResult.Success -> {
                scope.launch {
                    signingIn = true
                    try {
                        // See this file's top doc comment for why
                        // accountType is always PARENT_GUARDIAN here.
                        val tokens = wedLockApi.socialAuth(idToken = result.idToken, accountType = "PARENT_GUARDIAN")
                        identityStore.setWedlockAccessToken(tokens.accessToken)
                        onSuccess()
                    } catch (e: Exception) {
                        onError(friendlyError(e, strings.errGoogleSignIn))
                    } finally {
                        signingIn = false
                    }
                }
            }
            // Not an error -- the user dismissed Google's own account
            // chooser/consent sheet, nothing to surface.
            is GoogleSignInResult.Cancelled -> Unit
            is GoogleSignInResult.Error -> onError(result.message)
        }
    }

    D2MButton(
        text = if (signingIn) strings.signingIn else label,
        variant = com.d2m.app.ui.components.D2MButtonVariant.OUTLINE,
        enabled = enabled && !signingIn,
        onClick = { launchGoogleSignIn() },
    )
}
