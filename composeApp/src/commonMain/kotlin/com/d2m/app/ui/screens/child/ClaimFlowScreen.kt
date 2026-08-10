package com.d2m.app.ui.screens.child

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.d2m.app.data.model.ChildPreferencesRequest
import com.d2m.app.data.model.InviteRedeemRequest
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.ui.components.D2MButton
import com.d2m.app.ui.components.D2MErrorBanner
import com.d2m.app.ui.components.D2MSelectField
import com.d2m.app.ui.components.D2MTextField
import com.d2m.app.ui.components.Taxonomy
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Mirrors screens/child/ClaimFlow.jsx: invite-redemption flow, internally
 * stepped (landing -> redeem form -> preferences -> photos-skippable). This
 * cut covers landing/redeem/preferences; photos step is intentionally
 * deferred to ChildProfileDialogScreen's Preferences/Photos surface (a
 * PhotoManager-equivalent component isn't built in this pass -- see plan
 * §4/§8, photo upload needs a platform picker actual that lands with the
 * rest of the platform actuals work).
 */
@Composable
fun ClaimFlowScreen(token: String?, onComplete: () -> Unit) {
    val identityRepo: IdentityRepository = koinInject()
    val identityStore: IdentityStore = koinInject()
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(0) } // 0 = landing, 1 = redeem, 2 = preferences
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
                    Text("Someone set this up for you 💌", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Claim it to make it yours.", color = mutedText(0.55f))
                    D2MButton(text = "Claim your profile", onClick = { step = 1 })
                }
                1 -> {
                    Text("Let's make sure it's really you.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                                    step = 2
                                } catch (e: Exception) {
                                    error = friendlyError(e, "That link doesn't look right -- ask for a fresh one.")
                                } finally {
                                    submitting = false
                                }
                            }
                        },
                    )
                }
                2 -> {
                    Text("Who are you hoping to meet?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
