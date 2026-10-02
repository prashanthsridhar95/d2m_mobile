package com.d2m.app.ui.screens.child

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.d2m.app.data.model.ChildPreferencesRequest
import com.d2m.app.data.model.InviteRedeemRequest
import com.d2m.app.data.network.WedLockApi
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MButtonVariant
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MSelectField
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.components.Taxonomy
import com.d2m.app.ui.components.SectionHeading
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Mirrors screens/child/ClaimFlow.jsx: invite-redemption flow, internally
 * stepped (landing -> create account -> redeem form -> preferences ->
 * photos-skippable). This cut covers landing/account/redeem/preferences;
 * photos step is intentionally deferred to ChildProfileDialogScreen's
 * Preferences/Photos surface (a PhotoManager-equivalent component isn't
 * built in this pass -- see plan §4/§8, photo upload needs a platform
 * picker actual that lands with the rest of the platform actuals work).
 *
 * The "create account" step (WedLock otp/send -> otp/verify -> set
 * password -> register/self -> login) is new: d2m_core_engine's
 * POST /invites/redeem forwards the CALLER's own WedLock bearer token to
 * WedLock's linking-accept API server-to-server (see
 * app/services/wedlock_bridge_service.py), so it has to be the CHILD's
 * own SELF-type WedLock account, not the parent's -- registering here,
 * right before redeeming, is the natural point: this is the first and
 * only screen a child (who arrived via their parent's shared invite link)
 * ever sees before they have a D2M identity of their own. That's also why
 * this doesn't reuse LoginScreen.kt's register pane, which is
 * parent-only.
 */
@Composable
fun ClaimFlowScreen(token: String?, onComplete: () -> Unit) {
    val identityRepo: IdentityRepository = koinInject()
    val identityStore: IdentityStore = koinInject()
    val wedLockApi: WedLockApi = koinInject()
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(0) } // 0 = landing, 1 = create account, 2 = redeem, 3 = preferences

    // Step 1 -- WedLock account creation.
    var accountSubStep by remember { mutableStateOf(0) } // 0 = email, 1 = otp, 2 = password
    var wedlockEmail by remember { mutableStateOf("") }
    var wedlockOtp by remember { mutableStateOf("") }
    var wedlockPassword by remember { mutableStateOf("") }
    var accountLoading by remember { mutableStateOf(false) }
    var accountError by remember { mutableStateOf<String?>(null) }

    var name by remember { mutableStateOf("") }
    var contactInfo by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Taxonomy.GENDERS.first()) }
    var seekingGender by remember { mutableStateOf(Taxonomy.GENDERS[1]) }
    var maritalStatus by remember { mutableStateOf(Taxonomy.MARITAL_STATUSES.first()) }
    var location by remember { mutableStateOf("") }
    var relationshipGoal by remember { mutableStateOf(Taxonomy.RELATIONSHIP_GOALS.first()) }
    var minAge by remember { mutableStateOf("25") }
    var maxAge by remember { mutableStateOf("32") }

    var primaryId by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    D2MTheme(flow = D2MFlow.CHILD) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (step) {
                0 -> {
                    SectionHeading("Someone set this up for you 💌")
                    Text("Claim it to make it yours.", color = mutedText(0.55f))
                    D2MButton(text = "Claim your profile", onClick = { step = 1 })
                }
                1 -> {
                    SectionHeading("First, let's create your account.")
                    when (accountSubStep) {
                        0 -> {
                            D2MTextField(label = "Your email", value = wedlockEmail, onValueChange = { wedlockEmail = it; accountError = null }, keyboardType = KeyboardType.Email)
                            accountError?.let { D2MErrorBanner(it) }
                            D2MButton(
                                text = if (accountLoading) "Sending code…" else "Send code",
                                enabled = wedlockEmail.isNotBlank() && !accountLoading,
                                onClick = {
                                    scope.launch {
                                        accountLoading = true
                                        accountError = null
                                        try {
                                            wedLockApi.sendRegistrationOtp(wedlockEmail)
                                            accountSubStep = 1
                                        } catch (e: Exception) {
                                            accountError = friendlyError(e, "Couldn't send a code to that email.")
                                        } finally {
                                            accountLoading = false
                                        }
                                    }
                                },
                            )
                        }
                        1 -> {
                            Text("We sent a 6-digit code to $wedlockEmail.", style = MaterialTheme.typography.bodySmall, color = mutedText(0.55f))
                            D2MTextField(label = "Verification code", value = wedlockOtp, onValueChange = { wedlockOtp = it; accountError = null }, keyboardType = KeyboardType.Number)
                            accountError?.let { D2MErrorBanner(it) }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                D2MButton(
                                    text = if (accountLoading) "Verifying…" else "Verify",
                                    enabled = wedlockOtp.length == 6 && !accountLoading,
                                    onClick = {
                                        scope.launch {
                                            accountLoading = true
                                            accountError = null
                                            try {
                                                wedLockApi.verifyRegistrationOtp(wedlockEmail, wedlockOtp)
                                                accountSubStep = 2
                                            } catch (e: Exception) {
                                                accountError = friendlyError(e, "That code didn't check out.")
                                            } finally {
                                                accountLoading = false
                                            }
                                        }
                                    },
                                )
                                D2MButton(text = "Back", variant = D2MButtonVariant.OUTLINE, onClick = { accountSubStep = 0; wedlockOtp = ""; accountError = null })
                            }
                        }
                        2 -> {
                            D2MTextField(
                                label = "Password",
                                value = wedlockPassword,
                                onValueChange = { wedlockPassword = it; accountError = null },
                                isPassword = true,
                                hint = "At least 8 characters.",
                            )
                            accountError?.let { D2MErrorBanner(it) }
                            D2MButton(
                                text = if (accountLoading) "Creating account…" else "Create account",
                                enabled = wedlockPassword.length >= 8 && !accountLoading,
                                onClick = {
                                    scope.launch {
                                        accountLoading = true
                                        accountError = null
                                        try {
                                            wedLockApi.registerSelf(wedlockEmail, wedlockPassword)
                                            val tokens = wedLockApi.login(emailOrPhone = wedlockEmail, password = wedlockPassword)
                                            identityStore.setWedlockAccessToken(tokens.accessToken)
                                            step = 2
                                        } catch (e: Exception) {
                                            accountError = friendlyError(e, "Couldn't create that account.")
                                        } finally {
                                            accountLoading = false
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
                2 -> {
                    SectionHeading("Let's make sure it's really you.")
                    D2MTextField("Your name", name, { name = it })
                    D2MTextField("Contact info (phone or email)", contactInfo, { contactInfo = it })
                    D2MSelectField("Gender", gender, Taxonomy.GENDERS, { gender = it }, optionLabel = Taxonomy::toLabel)
                    D2MSelectField("Seeking", seekingGender, Taxonomy.GENDERS, { seekingGender = it }, optionLabel = Taxonomy::toLabel)
                    D2MSelectField("Marital status", maritalStatus, Taxonomy.MARITAL_STATUSES, { maritalStatus = it }, optionLabel = Taxonomy::toLabel)
                    D2MTextField("Location", location, { location = it })
                    error?.let { D2MErrorBanner(it) }
                    D2MButton(
                        text = if (submitting) "Verifying…" else "Continue",
                        enabled = !submitting && token != null,
                        onClick = {
                            scope.launch {
                                submitting = true
                                error = null
                                try {
                                    // identityStore already carries this child's own
                                    // WedLock access_token from step 1 above --
                                    // ApiClient attaches it to this call transparently.
                                    val result = identityRepo.redeemInvite(
                                        InviteRedeemRequest(
                                            token = token.orEmpty(),
                                            name = name,
                                            contactInfo = contactInfo,
                                            gender = gender,
                                            seekingGender = seekingGender,
                                            maritalStatus = maritalStatus,
                                            location = location,
                                        ),
                                    )
                                    primaryId = result.primaryId
                                    step = 3
                                } catch (e: Exception) {
                                    error = friendlyError(e, "That link doesn't look right -- ask for a fresh one.")
                                } finally {
                                    submitting = false
                                }
                            }
                        },
                    )
                }
                3 -> {
                    SectionHeading("Who are you hoping to meet?")
                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        D2MTextField("Min age", minAge, { minAge = it }, modifier = Modifier.weight(1f))
                        D2MTextField("Max age", maxAge, { maxAge = it }, modifier = Modifier.weight(1f))
                    }
                    D2MSelectField("Relationship goal", relationshipGoal, Taxonomy.RELATIONSHIP_GOALS, { relationshipGoal = it }, optionLabel = Taxonomy::toLabel)
                    error?.let { D2MErrorBanner(it) }
                    D2MButton(
                        text = if (submitting) "Saving…" else "Looks good, let's go",
                        enabled = !submitting,
                        onClick = {
                            val pid = primaryId ?: return@D2MButton
                            scope.launch {
                                submitting = true
                                error = null
                                try {
                                    identityRepo.setChildPreferences(
                                        pid,
                                        ChildPreferencesRequest(
                                            minAge = minAge.toIntOrNull() ?: 25,
                                            maxAge = maxAge.toIntOrNull() ?: 32,
                                            relationshipGoal = relationshipGoal,
                                        ),
                                    )
                                    identityStore.setChild(pid)
                                    onComplete()
                                } catch (e: Exception) {
                                    error = friendlyError(e, "Couldn't save your preferences.")
                                } finally {
                                    submitting = false
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
