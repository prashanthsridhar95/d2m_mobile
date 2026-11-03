package com.d2m.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.d2m.app.ui.theme.d2m
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
import com.d2m.app.ui.strings.LocalAppLocale
import com.d2m.app.ui.strings.LocalStrings
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
@Serializable
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

/**
 * Draft persistence: rememberSaveable, so rotation and process death keep a half-filled wizard but a cold start
 * does not (web uses sessionStorage for the same reason). Birth date and time are blanked before saving, so the
 * most sensitive fields never sit in saved instance state -- the user re-enters those two after a process kill.
 */
private val WizardFormSaver: Saver<WizardForm, String> = Saver(
    save = { Json.encodeToString(WizardForm.serializer(), it.copy(childDob = "", childTob = "")) },
    restore = { runCatching { Json.decodeFromString(WizardForm.serializer(), it) }.getOrNull() },
)

@Composable
fun OnboardingWizardScreen(onComplete: () -> Unit) {
    val identityRepo: IdentityRepository = koinInject()
    val resultHolder: OnboardingResultHolder = koinInject()
    val geocodingApi = koinInject<com.d2m.app.data.network.GeocodingApi>()
    val scope = rememberCoroutineScope()

    var step by rememberSaveable { mutableStateOf(0) }
    var form by rememberSaveable(stateSaver = WizardFormSaver) { mutableStateOf(WizardForm()) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val totalSteps = 5
    val strings = LocalStrings.current.onboarding
    val locale = LocalAppLocale.current
    val relationshipLabel: (String) -> String = {
        when (it) {
            "Mother" -> strings.relationshipMother
            "Father" -> strings.relationshipFather
            "Guardian" -> strings.relationshipGuardian
            else -> strings.relationshipOther
        }
    }

    fun update(patch: WizardForm.() -> WizardForm) { form = form.patch() }

    D2MTheme(flow = D2MFlow.PARENT) {
        Column(modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 20.dp).padding(top = 20.dp)) {
            /*
             * The comps' named horizontal stepper, replacing a progress
             * bar plus a "Question 3 of 5" caption -- see Stepper.kt for
             * why naming the steps is the point. Completed steps are
             * tappable; upcoming ones are not, since this wizard's
             * validation runs forward (step 2's coordinate guard below
             * depends on it).
             */
            PageTitle(strings.setUpProfile)
            val stepNames = listOf(strings.stepAboutYou, strings.stepAboutThem, strings.stepBirthDetails, strings.stepPreferences, strings.stepReview)
            val progressText = strings.stepProgress(step + 1, totalSteps, stepNames[step])
            D2MStepper(
                steps = stepNames,
                activeIndex = step,
                onStepClick = { step = it },
                modifier = Modifier.padding(top = 14.dp),
                progressDescription = progressText,
            )
            D2MStepProgressBar((step + 1).toFloat() / totalSteps, Modifier.padding(top = 10.dp))
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(progressText, style = MaterialTheme.typography.labelMedium, color = d2m.meta, modifier = Modifier.clearAndSetSemantics {})
                Text(strings.draftSaved, style = MaterialTheme.typography.labelMedium, color = d2m.meta)
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (step) {
                    0 -> {
                        SectionHeading(strings.aboutYouHeading)
                        D2MTextField(strings.yourName, form.yourName, { update { copy(yourName = it) } })
                        D2MSelectField(strings.yourRelationship, form.relationship, listOf("Mother", "Father", "Guardian", "Other relative"), { update { copy(relationship = it) } }, optionLabel = relationshipLabel)
                    }
                    1 -> {
                        SectionHeading(strings.aboutChildHeading)
                        D2MTextField(strings.name, form.childName, { update { copy(childName = it) } })
                        D2MSelectField(strings.gender, form.childGender, Taxonomy.GENDERS, { update { copy(childGender = it) } }, optionLabel = { Taxonomy.toLabel(it, locale) })
                        D2MSelectField(strings.seeking, form.childSeekingGender, Taxonomy.GENDERS, { update { copy(childSeekingGender = it) } }, optionLabel = { Taxonomy.toLabel(it, locale) })
                    }
                    2 -> {
                        SectionHeading(strings.whenWasSheBorn)
                        D2MTextField(strings.dateLabel, form.childDob, { update { copy(childDob = it) } })
                        D2MTextField(strings.timeLabel, form.childTob, { update { copy(childTob = it) } })
                        CityAutocomplete(
                            label = strings.birthPlace,
                            value = form.childBirthPlace,
                            geocodingApi = geocodingApi,
                            onSelect = { r -> update { copy(childBirthPlace = r.label, childBirthLat = r.lat, childBirthLon = r.lon) } },
                            onRawTextChange = { text -> update { copy(childBirthPlace = text) } },
                        )
                        if (form.childBirthLat == 0.0 && form.childBirthLon == 0.0 && form.childBirthPlace.isNotBlank()) {
                            Text(strings.noCoordinatesWarning, style = MaterialTheme.typography.labelSmall, color = mutedText(0.55f))
                        }
                    }
                    3 -> {
                        SectionHeading(strings.moreDetailsHeading)
                        D2MSelectField(strings.religion, form.ownReligion, Taxonomy.RELIGIONS, { update { copy(ownReligion = it) } })
                        D2MSelectField(strings.yourCommunity, form.ownCasteCommunity, Taxonomy.COMMUNITIES, { update { copy(ownCasteCommunity = it) } }, optionLabel = { Taxonomy.toLabel(it, locale) })
                        D2MChipGroup(strings.acceptableReligions, Taxonomy.RELIGIONS, form.acceptReligions, { update { copy(acceptReligions = it) } })
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            D2MTextField(strings.minAge, form.minAge, { update { copy(minAge = it) } }, modifier = Modifier.weight(1f))
                            D2MTextField(strings.maxAge, form.maxAge, { update { copy(maxAge = it) } }, modifier = Modifier.weight(1f))
                        }
                        CityChipPicker(strings.acceptableLocations, form.acceptLocations, { update { copy(acceptLocations = it) } }, geocodingApi)
                        D2MChipGroup(strings.acceptableMaritalStatus, Taxonomy.MARITAL_STATUSES, form.maritalStatusFilter, { update { copy(maritalStatusFilter = it) } }, optionLabel = { Taxonomy.toLabel(it, locale) })
                    }
                    4 -> {
                        SectionHeading(strings.beforeWeSave)
                        listOf(
                            strings.name to form.childName,
                            strings.gender to Taxonomy.toLabel(form.childGender, locale),
                            strings.reviewBorn to "${form.childDob} ${form.childTob}",
                            strings.birthPlace to form.childBirthPlace,
                            strings.religion to form.ownReligion,
                            strings.yourCommunity to Taxonomy.toLabel(form.ownCasteCommunity, locale),
                            strings.reviewMatchAgeRange to "${form.minAge}–${form.maxAge}",
                        ).forEach { (label, value) ->
                            DataRow(label, value)
                        }
                        error?.let { D2MErrorBanner(it) }
                    }
                }
            }

            // Sticky action bar: outside the scroller, lifted by the keyboard (imePadding above) and clear of the nav bar.
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step > 0) {
                    D2MButton(strings.back, variant = D2MButtonVariant.OUTLINE, modifier = Modifier.heightIn(min = 48.dp), onClick = { step -= 1 })
                } else {
                    androidx.compose.foundation.layout.Spacer(Modifier)
                }
                D2MButton(
                    text = if (step < totalSteps - 1) strings.continueAction else if (submitting) strings.saving else strings.confirmProfile,
                    enabled = !submitting,
                    modifier = Modifier.heightIn(min = 48.dp),
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
                                        hardCasteCommunity = if (form.hardCasteCommunity) form.ownCasteCommunity else null,
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
                                    // Success: drop the draft so a second visit starts clean.
                                    form = WizardForm()
                                    step = 0
                                    onComplete()
                                } catch (e: Exception) {
                                    error = com.d2m.app.data.network.friendlyError(e, strings.errCouldntSaveProfile)
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
