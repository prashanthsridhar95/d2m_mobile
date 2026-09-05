package com.d2m.app.ui.screens.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.ui.components.D2MBrand
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.MetaText
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.components.D2MTabs
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Mirrors screens/entry/LoginScreen.jsx: there is no real backend auth (see
 * ApiClient.kt / IdentityStore.kt doc comments), so "logging in" here means
 * looking up an existing sponsorId/primaryId and showing a confirm-before-
 * committing preview, exactly like the web dev-mode login -- not a
 * credential check. SSO buttons are omitted entirely on mobile rather than
 * shown-disabled (no backend to back them, see plan §4 screen mapping).
 */
@Composable
fun LoginScreen(
    onLoginAsParent: (sponsorId: String) -> Unit,
    onLoginAsChild: (primaryId: String) -> Unit,
    onRegister: () -> Unit,
) {
    val identityRepo: IdentityRepository = koinInject()
    val identityStore: IdentityStore = koinInject()
    val scope = rememberCoroutineScopeSafe()

    var tab by remember { mutableStateOf(0) } // 0 = Login, 1 = Register
    var idInput by remember { mutableStateOf("") }
    var idIsParent by remember { mutableStateOf(true) }
    var confirmName by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    D2MTheme(flow = D2MFlow.ENTRY) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            D2MBrand(markSize = 34.dp)
            MetaText(
                "Every union begins with the stars.",
                Modifier.padding(top = 8.dp),
            )

            Column(modifier = Modifier.padding(top = 24.dp).widthIn(max = 420.dp)) {
                D2MTabs(
                    titles = listOf("Log in", "Register"),
                    selectedIndex = tab,
                    onSelect = { i ->
                        tab = i
                        // Register isn't a tab pane here -- it hands off to
                        // the onboarding wizard, same as web's Login/Register
                        // toggle does.
                        if (i == 1) onRegister()
                    },
                )

                if (tab == 0) {
                    Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Paste an existing sponsor or primary id (dev-mode login -- see plan §1 on real auth).", style = MaterialTheme.typography.bodyMedium, color = mutedText(0.55f))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            D2MButton(
                                text = "I'm a parent",
                                onClick = { idIsParent = true },
                                variant = if (idIsParent) com.d2m.app.ui.components.D2MButtonVariant.SOLID else com.d2m.app.ui.components.D2MButtonVariant.OUTLINE,
                            )
                            D2MButton(
                                text = "I'm the child",
                                onClick = { idIsParent = false },
                                variant = if (!idIsParent) com.d2m.app.ui.components.D2MButtonVariant.SOLID else com.d2m.app.ui.components.D2MButtonVariant.OUTLINE,
                            )
                        }

                        D2MTextField(label = if (idIsParent) "Sponsor id" else "Primary id", value = idInput, onValueChange = { idInput = it; confirmName = null })

                        error?.let { D2MErrorBanner(it) }

                        if (confirmName == null) {
                            D2MButton(
                                text = if (loading) "Checking…" else "Continue",
                                enabled = idInput.isNotBlank() && !loading,
                                onClick = {
                                    scope.launch {
                                        loading = true
                                        error = null
                                        try {
                                            confirmName = if (idIsParent) {
                                                identityRepo.getSponsorProfile(idInput).name
                                            } else {
                                                identityRepo.getPrimaryProfile(idInput).name
                                            }
                                        } catch (e: Exception) {
                                            // Was a hardcoded "couldn't find that id" for every
                                            // exception -- indistinguishable from a real 404 in
                                            // the UI whether the id was wrong, the network was
                                            // down, or (as with the snake_case/camelCase mismatch
                                            // fixed in ApiClient.kt's Json config) the response
                                            // came back fine but failed to deserialize. Surfacing
                                            // the real message makes that class of bug visible
                                            // without needing a debugger attached.
                                            error = friendlyError(e, "Couldn't find that id. Double check and try again.")
                                        } finally {
                                            loading = false
                                        }
                                    }
                                },
                            )
                        } else {
                            Text("Logging in as $confirmName — confirm?", style = MaterialTheme.typography.bodyLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                D2MButton(
                                    text = "Confirm",
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
                                D2MButton(text = "Not me", variant = com.d2m.app.ui.components.D2MButtonVariant.OUTLINE, onClick = { confirmName = null; idInput = "" })
                            }
                        }
                    }
                }
            }
        }
    }
}

// Small helper so this file doesn't need an extra import block per screen --
// every screen in this app follows this same rememberCoroutineScope() pattern.
@Composable
private fun rememberCoroutineScopeSafe() = androidx.compose.runtime.rememberCoroutineScope()
