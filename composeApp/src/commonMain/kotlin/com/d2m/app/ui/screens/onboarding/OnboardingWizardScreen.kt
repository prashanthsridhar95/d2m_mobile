package com.d2m.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
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
import com.d2m.app.data.model.SponsorCreateRequest
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.ui.components.*
import com.d2m.app.ui.components.SectionHeading
import com.d2m.app.ui.components.DataRow
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.components.D2MStepper
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Mirrors screens/parent/OnboardingWizard.jsx: 5 steps (About you / About
 * your child / Birth details / Preferences / Review), one form-state object,
 * a step indicator instead of separate routes -- here, a simple step index
 * over one screen rather than a HorizontalPager, since each step's content
 * differs enough in shape that a pager buys little over a plain `when`.
 * Submits createSponsor() on the last step, then hands off to
 * HandoffScreen via OnboardingResultHolder (see that file's doc comment).
 */
private data class WizardForm(
    val yourName: String = "",
    val relationship: String = "Mother",
    val childName: String = "",
    val childGender: String = "female",
    val childSeekingGender: String = "male",
    val childDob: String = "",
    val childTob: String = "",
    val childBirthPlace: String = "",
    val childBirthLat: Double = 0.0,
    val childBirthLon: Double = 0.0,
    val childBirthTzOffsetHours: Double = 5.5,
    val ownReligion: String = "Hindu",
    val ownCasteCommunity: String = Taxonomy.COMMUNITIES.first(),
    val acceptReligions: List<String> = listOf("Hindu"),
    val hardCasteCommunity: Boolean = false,
    val minAge: String = "25",
    val maxAge: String = "32",
    val acceptLocations: List<String> = emptyList(),
    val maritalStatusFilter: List<String> = listOf("never_married"),
    val rankedCommunityList: List<String> = emptyList(),
)

@Composable
fun OnboardingWizardScreen(onComplete: () -> Unit) {
    val identityRepo: IdentityRepository = koinInject()
    val resultHolder: OnboardingResultHolder = koinInject()
    val geocodingApi = koinInject<com.d2m.app.data.network.GeocodingApi>()
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(0) }
    var form by remember { mutableStateOf(WizardForm()) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val totalSteps = 5

    fun update(patch: WizardForm.() -> WizardForm) { form = form.patch() }

    D2MTheme(flow = D2MFlow.PARENT) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            /*
             * The comps' named horizontal stepper, replacing a progress
             * bar plus a "Question 3 of 5" caption -- see Stepper.kt for
             * why naming the steps is the point. Completed steps are
             * tappable; upcoming ones are not, since this wizard's
             * validation runs forward (step 2's coordinate guard below
             * depends on it).
             */
            PageTitle("Set up the profile")
            D2MStepper(
                steps = listOf("About you", "About them", "Birth details", "Preferences", "Review"),
                activeIndex = step,
                onStepClick = { step = it },
                modifier = Modifier.padding(top = 14.dp),
            )

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (step) {
                    0 -> {
                        SectionHeading("A little about you first.")
                        D2MTextField("Your name", form.yourName, { update { copy(yourName = it) } })
                        D2MSelectField("Your relationship to them", form.relationship, listOf("Mother", "Father", "Guardian", "Other relative"), { update { copy(relationship = it) } })
                    }
                    1 -> {
                        SectionHeading("Tell us about your child.")
                        D2MTextField("Name", form.childName, { update { copy(childName = it) } })
                        D2MSelectField("Gender", form.childGender, Taxonomy.GENDERS, { update { copy(childGender = it) } }, optionLabel = Taxonomy::toLabel)
                        D2MSelectField("Seeking", form.childSeekingGender, Taxonomy.GENDERS, { update { copy(childSeekingGender = it) } }, optionLabel = Taxonomy::toLabel)
                    }
                    2 -> {
                        SectionHeading("When was she born?")
                        D2MTextField("Date (YYYY-MM-DD)", form.childDob, { update { copy(childDob = it) } })
                        D2MTextField("Time (HH:MM, 24h)", form.childTob, { update { copy(childTob = it) } })
                        CityAutocomplete(
                            label = "Birth place",
                            value = form.childBirthPlace,
                            geocodingApi = geocodingApi,
                            onSelect = { r -> update { copy(childBirthPlace = r.label, childBirthLat = r.lat, childBirthLon = r.lon) } },
                            onRawTextChange = { text -> update { copy(childBirthPlace = text) } },
                        )
                        if (form.childBirthLat == 0.0 && form.childBirthLon == 0.0 && form.childBirthPlace.isNotBlank()) {
                            Text("No coordinates resolved yet for this place -- pick a suggestion from the list, or the chart can't be computed (\"Null Island\" guard).", style = MaterialTheme.typography.labelSmall, color = mutedText(0.55f))
                        }
                    }
                    3 -> {
                        SectionHeading("A few more details.")
                        D2MSelectField("Religion", form.ownReligion, Taxonomy.RELIGIONS, { update { copy(ownReligion = it) } })
                        D2MSelectField("Your community", form.ownCasteCommunity, Taxonomy.COMMUNITIES, { update { copy(ownCasteCommunity = it) } }, optionLabel = Taxonomy::toLabel)
                        D2MChipGroup("Acceptable religions", Taxonomy.RELIGIONS, form.acceptReligions, { update { copy(acceptReligions = it) } })
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            D2MTextField("Min age", form.minAge, { update { copy(minAge = it) } }, modifier = Modifier.weight(1f))
                            D2MTextField("Max age", form.maxAge, { update { copy(maxAge = it) } }, modifier = Modifier.weight(1f))
                        }
                        CityChipPicker("Acceptable locations", form.acceptLocations, { update { copy(acceptLocations = it) } }, geocodingApi)
                        D2MChipGroup("Acceptable marital status", Taxonomy.MARITAL_STATUSES, form.maritalStatusFilter, { update { copy(maritalStatusFilter = it) } }, optionLabel = Taxonomy::toLabel)
                    }
                    4 -> {
                        SectionHeading("Before we save this.")
                        listOf(
                            "Name" to form.childName,
                            "Gender" to Taxonomy.toLabel(form.childGender),
                            "Born" to "${form.childDob} ${form.childTob}",
                            "Birth place" to form.childBirthPlace,
                            "Religion" to form.ownReligion,
                            "Community" to Taxonomy.toLabel(form.ownCasteCommunity),
                            "Match age range" to "${form.minAge}–${form.maxAge}",
                        ).forEach { (label, value) ->
                            DataRow(label, value)
                        }
                        error?.let { D2MErrorBanner(it) }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                if (step > 0) {
                    D2MButton("Back", variant = D2MButtonVariant.OUTLINE, onClick = { step -= 1 })
                } else {
                    androidx.compose.foundation.layout.Spacer(Modifier)
                }
                D2MButton(
                    text = if (step < totalSteps - 1) "Continue" else if (submitting) "Saving…" else "Confirm profile",
                    enabled = !submitting,
                    onClick = {
                        if (step < totalSteps - 1) {
                            step += 1
                        } else {
                            scope.launch {
                                submitting = true
                                error = null
                                try {
                                    val request = SponsorCreateRequest(
                                        name = form.yourName,
                                        contactInfo = "",
                                        relationshipToChild = form.relationship,
                                        ownReligion = form.ownReligion,
                                        ownCasteCommunity = form.ownCasteCommunity,
                                        acceptReligions = form.acceptReligions,
                                        hardCasteCommunity = form.hardCasteCommunity,
                                        minAge = form.minAge.toIntOrNull() ?: 25,
                                        maxAge = form.maxAge.toIntOrNull() ?: 32,
                                        acceptLocations = form.acceptLocations,
                                        maritalStatusFilter = form.maritalStatusFilter,
                                        rankedCommunityList = form.rankedCommunityList,
                                        childName = form.childName,
                                        childGender = form.childGender,
                                        childSeekingGender = form.childSeekingGender,
                                        childDob = form.childDob,
                                        childTob = form.childTob,
                                        childBirthPlace = form.childBirthPlace,
                                        childBirthLat = form.childBirthLat,
                                        childBirthLon = form.childBirthLon,
                                        childBirthTzOffsetHours = form.childBirthTzOffsetHours,
                                    )
                                    val response = identityRepo.createSponsor(request)
                                    resultHolder.current.value = OnboardingHandoff(
                                        sponsorId = response.sponsorId,
                                        inviteToken = response.inviteToken,
                                        childName = form.childName,
                                        inviteExpiresAt = response.inviteExpiresAt,
                                    )
                                    onComplete()
                                } catch (e: Exception) {
                                    error = com.d2m.app.data.network.friendlyError(e, "Couldn't save this profile.")
                                } finally {
                                    submitting = false
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}
