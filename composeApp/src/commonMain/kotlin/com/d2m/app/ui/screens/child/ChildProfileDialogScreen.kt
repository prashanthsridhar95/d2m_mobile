package com.d2m.app.ui.screens.child

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.data.model.AboutMeDataIn
import com.d2m.app.data.model.AboutMePromptEntry
import com.d2m.app.data.model.ChildPreferencesRequest
import com.d2m.app.data.model.ExtendedBioDataIn
import com.d2m.app.data.model.LocationPreferenceIn
import com.d2m.app.data.network.GeocodingApi
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.D2MRole
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.ui.components.*
import com.d2m.app.ui.components.D2MTabs
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Mirrors components/ChildProfileDialog.jsx -- on web a fixed-size modal
 * with Profile/Bio data/Preferences tabs (no Chart tab -- removed there per
 * direct feedback, not re-added here either). Ported as a full-screen
 * destination with a top TabRow instead of a modal, the standard mobile
 * treatment for what's really a settings/edit hub (see plan §4). Bio data
 * fields match the round-2 shape exactly (monthly income + currency, 4
 * sibling counts, closed picklists for mother_tongue/complexion/sect/
 * gothram/other_languages) -- see data/model/Identity.kt's
 * ExtendedBioDataIn.
 */
private data class BioForm(
    val heightCm: String = "", val complexion: String = "", val motherTongue: String = "",
    val otherLanguages: List<String> = emptyList(), val bodyType: String = "",
    val religion: String = "", val casteCommunity: String = "", val sect: String = "", val gothram: String = "",
    val horoscopeMatchPreference: String = "",
    val highestEducation: String = "", val institution: String = "", val occupationTitle: String = "",
    val employer: String = "", val employmentSector: String = "",
    val monthlyIncomeAmount: String = "", val monthlyIncomeCurrency: String = Taxonomy.CURRENCIES.first(),
    val fatherName: String = "", val fatherOccupation: String = "", val motherName: String = "", val motherOccupation: String = "",
    val elderBrothers: String = "", val youngerBrothers: String = "", val elderSisters: String = "", val youngerSisters: String = "",
    val nativity: String = "", val familyType: String = "", val familyValues: String = "", val financialStatus: String = "",
    val citizenshipStatus: String = "",
)

/**
 * "About Me" tab form state -- mirrors ChildProfileDialog.jsx's
 * AboutMeSection (web). Deliberately distinct from BioForm/preferences
 * above -- see data/model/Identity.kt's AboutMeDataIn doc comment for why
 * this doesn't re-ask hobbies/lifestyle_tags/relationship_goal under new
 * names. selectedPrompts/promptAnswers together back the "About me
 * prompts" chip-group-plus-answer-boxes UI below; up to
 * MAX_ABOUT_PROMPTS may be selected at once, same cap the backend enforces
 * (app/schemas.py::AboutMeDataIn._prompts_bounded).
 */
private data class AboutMeForm(
    val fitnessRoutine: String = "", val sleepSchedule: String = "", val pets: String = "", val socialEnergy: String = "",
    val selectedPrompts: List<String> = emptyList(), val promptAnswers: Map<String, String> = emptyMap(),
    val partnerQualities: List<String> = emptyList(), val whatMattersMost: String = "",
    val careerAfterMarriage: String = "", val livingArrangement: String = "", val openToRelocation: String = "",
    val favoriteCuisine: String = "", val dreamDestination: String = "", val loveLanguage: String = "",
)

private const val MAX_ABOUT_PROMPTS = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildProfileDialogScreen(onClose: () -> Unit) {
    val identityStore: IdentityStore = koinInject()
    val identityRepo: IdentityRepository = koinInject()
    val geocodingApi: GeocodingApi = koinInject()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()

    // About Me is parent-invisible ("About me shouldn't be visible for the
    // parents - anywhere", reported directly) and, for the child, leads the
    // tab bar and is the default tab rather than Profile -- two key lists
    // rather than one filtered at render time so that intent reads
    // directly here, same convention as web's ChildProfileDialog.jsx
    // TABS_FOR_CHILD/TABS_FOR_PARENT.
    val isChildViewer = identity.role == D2MRole.CHILD
    val tabKeys = if (isChildViewer) listOf("about", "profile", "bio", "preferences") else listOf("profile", "bio", "preferences")
    val tabLabels = tabKeys.map { key ->
        when (key) {
            "about" -> "About Me"
            "profile" -> "Profile"
            "bio" -> "Bio data"
            else -> "Preferences"
        }
    }
    var tab by remember(isChildViewer) { mutableStateOf(0) }
    var bio by remember { mutableStateOf(BioForm()) }
    var aboutMe by remember { mutableStateOf(AboutMeForm()) }
    // Picking a prompt used to mean "select from all 8 up top, then find
    // the matching card below" -- two lists showing the same 8 prompts in
    // different states, no visual link between a chip and its card ("not
    // good UX", reported directly). Hinge's own answer (the actual
    // reference for a "prompts" UI) is one prompt at a time: pick one,
    // land straight in its answer box, done -- so the picker only ever
    // shows prompts NOT yet answered, and picking one both adds the card
    // and focuses it. Defaults open only once loaded with nothing
    // answered yet -- see the LaunchedEffect below.
    var promptPickerOpen by remember { mutableStateOf(false) }
    var justAddedPrompt by remember { mutableStateOf<String?>(null) }
    var acceptLocations by remember { mutableStateOf<List<String>>(emptyList()) }
    var locationHardFilter by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }

    val primaryId = identity.primaryId
    // accept_locations lives on the linked Sponsor's row (SponsorPreferences),
    // not the Primary's own preferences -- resolve it via GET
    // /primaries/{id}/sponsor when acting as the child (identity.sponsorId is
    // only ever set for a parent session, see IdentityStore.setChild), same
    // "resolve the linked sponsor id" step ChildProfileDialog.jsx's
    // PreferencesSection does on web.
    var resolvedSponsorId by remember { mutableStateOf(identity.sponsorId) }

    LaunchedEffect(primaryId) {
        if (primaryId == null) return@LaunchedEffect
        if (resolvedSponsorId == null) {
            resolvedSponsorId = runCatching { identityRepo.getPrimarySponsor(primaryId).sponsorId }.getOrNull()
        }
        runCatching { identityRepo.getExtendedBio(primaryId) }.onSuccess { b ->
            bio = BioForm(
                heightCm = b.heightCm?.toString() ?: "", complexion = b.complexion ?: "", motherTongue = b.motherTongue ?: "",
                otherLanguages = b.otherLanguages ?: emptyList(), bodyType = b.bodyType ?: "",
                religion = b.religion ?: "", casteCommunity = b.casteCommunity ?: "", sect = b.sect ?: "", gothram = b.gothram ?: "",
                horoscopeMatchPreference = b.horoscopeMatchPreference ?: "",
                highestEducation = b.highestEducation ?: "", institution = b.institution ?: "", occupationTitle = b.occupationTitle ?: "",
                employer = b.employer ?: "", employmentSector = b.employmentSector ?: "",
                monthlyIncomeAmount = b.monthlyIncomeAmount?.toString() ?: "", monthlyIncomeCurrency = b.monthlyIncomeCurrency ?: Taxonomy.CURRENCIES.first(),
                fatherName = b.fatherName ?: "", fatherOccupation = b.fatherOccupation ?: "", motherName = b.motherName ?: "", motherOccupation = b.motherOccupation ?: "",
                elderBrothers = b.elderBrothersCount?.toString() ?: "", youngerBrothers = b.youngerBrothersCount?.toString() ?: "",
                elderSisters = b.elderSistersCount?.toString() ?: "", youngerSisters = b.youngerSistersCount?.toString() ?: "",
                nativity = b.nativity ?: "", familyType = b.familyType ?: "", familyValues = b.familyValues ?: "", financialStatus = b.financialStatus ?: "",
                citizenshipStatus = b.citizenshipStatus ?: "",
            )
        }
        runCatching { identityRepo.getAboutMe(primaryId) }.onSuccess { a ->
            promptPickerOpen = a.aboutPrompts.isEmpty()
            aboutMe = AboutMeForm(
                fitnessRoutine = a.fitnessRoutine ?: "", sleepSchedule = a.sleepSchedule ?: "",
                pets = a.pets ?: "", socialEnergy = a.socialEnergy ?: "",
                selectedPrompts = a.aboutPrompts.map { it.prompt },
                promptAnswers = a.aboutPrompts.associate { it.prompt to it.answer },
                partnerQualities = a.partnerQualities, whatMattersMost = a.whatMattersMost ?: "",
                careerAfterMarriage = a.careerAfterMarriage ?: "", livingArrangement = a.livingArrangement ?: "",
                openToRelocation = a.openToRelocation ?: "",
                favoriteCuisine = a.favoriteCuisine ?: "", dreamDestination = a.dreamDestination ?: "",
                loveLanguage = a.loveLanguage ?: "",
            )
        }
    }

    fun update(patch: BioForm.() -> BioForm) { bio = bio.patch(); saved = false }
    fun updateAboutMe(patch: AboutMeForm.() -> AboutMeForm) { aboutMe = aboutMe.patch(); saved = false }
    fun addPrompt(prompt: String) {
        if (aboutMe.selectedPrompts.size >= MAX_ABOUT_PROMPTS) return
        updateAboutMe { copy(selectedPrompts = selectedPrompts + prompt) }
        justAddedPrompt = prompt
        promptPickerOpen = false
    }
    fun removePrompt(prompt: String) {
        updateAboutMe { copy(selectedPrompts = selectedPrompts - prompt, promptAnswers = promptAnswers - prompt) }
    }

    D2MTheme(flow = D2MFlow.CHILD) {
        Column(modifier = Modifier.fillMaxSize()) {
            // A serif page title on the page ground, not a Material
            // TopAppBar -- see navigation/AppScaffold.kt's ScreenHeader
            // note on why that bar is the wrong shape for this design.
            PageTitle("My profile", Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp))
            D2MTabs(tabLabels, tab, { tab = it })

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (tabKeys[tab]) {
                    "profile" -> Text("Basic data and photos live in Settings today -- see plan §4 for the photo-picker follow-up.", color = mutedText(0.55f))

                    "bio" -> {
                        BioSectionHeader("Basic & personal details")
                        D2MTextField("Height (cm)", bio.heightCm, { update { copy(heightCm = it) } })
                        D2MSelectField("Complexion", bio.complexion, Taxonomy.COMPLEXIONS, { update { copy(complexion = it) } })
                        D2MSelectField("Mother tongue", bio.motherTongue, Taxonomy.LANGUAGES, { update { copy(motherTongue = it) } })
                        D2MChipGroup("Other languages known", Taxonomy.LANGUAGES, bio.otherLanguages, { update { copy(otherLanguages = it) } })
                        D2MSelectField("Body type", bio.bodyType, Taxonomy.BODY_TYPES, { update { copy(bodyType = it) } }, optionLabel = Taxonomy::toLabel)

                        BioSectionHeader("Religious & astrological information")
                        D2MSelectField("Religion", bio.religion, Taxonomy.RELIGIONS, { update { copy(religion = it) } })
                        D2MSelectField("Caste / community", bio.casteCommunity, Taxonomy.COMMUNITIES, { update { copy(casteCommunity = it) } }, optionLabel = Taxonomy::toLabel)
                        D2MSelectField("Sect", bio.sect, Taxonomy.SECTS, { update { copy(sect = it) } })
                        D2MSelectField("Gothram", bio.gothram, Taxonomy.GOTHRAMS, { update { copy(gothram = it) } })
                        D2MSelectField("Horoscope match preference", bio.horoscopeMatchPreference, Taxonomy.HOROSCOPE_MATCH_PREFERENCES, { update { copy(horoscopeMatchPreference = it) } }, optionLabel = Taxonomy::toLabel)

                        BioSectionHeader("Education & career")
                        D2MSelectField("Highest education", bio.highestEducation, Taxonomy.EDUCATION_LEVELS, { update { copy(highestEducation = it) } }, optionLabel = Taxonomy::toLabel)
                        D2MTextField("Institution / university", bio.institution, { update { copy(institution = it) } })
                        D2MSelectField("Employed in", bio.employmentSector, Taxonomy.EMPLOYMENT_SECTORS, { update { copy(employmentSector = it) } }, optionLabel = Taxonomy::toLabel)
                        D2MTextField("Occupation / designation", bio.occupationTitle, { update { copy(occupationTitle = it) } })
                        D2MTextField("Employer", bio.employer, { update { copy(employer = it) } })
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            D2MTextField("Monthly income", bio.monthlyIncomeAmount, { update { copy(monthlyIncomeAmount = it) } }, modifier = Modifier.weight(1f))
                            D2MSelectField("Currency", bio.monthlyIncomeCurrency, Taxonomy.CURRENCIES, { update { copy(monthlyIncomeCurrency = it) } }, modifier = Modifier.weight(1f))
                        }

                        BioSectionHeader("Family background")
                        D2MTextField("Father's name", bio.fatherName, { update { copy(fatherName = it) } })
                        D2MTextField("Father's occupation", bio.fatherOccupation, { update { copy(fatherOccupation = it) } })
                        D2MTextField("Mother's name", bio.motherName, { update { copy(motherName = it) } })
                        D2MTextField("Mother's occupation", bio.motherOccupation, { update { copy(motherOccupation = it) } })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            D2MTextField("Elder brothers", bio.elderBrothers, { update { copy(elderBrothers = it) } }, modifier = Modifier.weight(1f))
                            D2MTextField("Younger brothers", bio.youngerBrothers, { update { copy(youngerBrothers = it) } }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            D2MTextField("Elder sisters", bio.elderSisters, { update { copy(elderSisters = it) } }, modifier = Modifier.weight(1f))
                            D2MTextField("Younger sisters", bio.youngerSisters, { update { copy(youngerSisters = it) } }, modifier = Modifier.weight(1f))
                        }
                        D2MSelectField("Family type", bio.familyType, Taxonomy.FAMILY_TYPES, { update { copy(familyType = it) } }, optionLabel = Taxonomy::toLabel)
                        D2MSelectField("Family values", bio.familyValues, Taxonomy.FAMILY_VALUES, { update { copy(familyValues = it) } }, optionLabel = Taxonomy::toLabel)
                        D2MTextField("Native place / ancestral origin", bio.nativity, { update { copy(nativity = it) } })
                        D2MTextField("Financial status", bio.financialStatus, { update { copy(financialStatus = it) } })

                        BioSectionHeader("Location & contact")
                        D2MSelectField("Citizenship / residing status", bio.citizenshipStatus, Taxonomy.CITIZENSHIP_STATUSES, { update { copy(citizenshipStatus = it) } }, optionLabel = Taxonomy::toLabel)

                        error?.let { D2MErrorBanner(it) }
                        if (saved) Text("Saved.", color = MaterialTheme.colorScheme.primary)
                        D2MButton(
                            text = if (saving) "Saving…" else "Save bio data",
                            enabled = !saving,
                            onClick = {
                                val pid = primaryId ?: return@D2MButton
                                scope.launch {
                                    saving = true
                                    error = null
                                    try {
                                        identityRepo.updateExtendedBio(
                                            pid,
                                            ExtendedBioDataIn(
                                                heightCm = bio.heightCm.toIntOrNull(),
                                                complexion = bio.complexion.ifBlank { null },
                                                motherTongue = bio.motherTongue.ifBlank { null },
                                                otherLanguages = bio.otherLanguages,
                                                bodyType = bio.bodyType.ifBlank { null },
                                                religion = bio.religion.ifBlank { null },
                                                casteCommunity = bio.casteCommunity.ifBlank { null },
                                                sect = bio.sect.ifBlank { null },
                                                gothram = bio.gothram.ifBlank { null },
                                                horoscopeMatchPreference = bio.horoscopeMatchPreference.ifBlank { null },
                                                highestEducation = bio.highestEducation.ifBlank { null },
                                                institution = bio.institution.ifBlank { null },
                                                occupationTitle = bio.occupationTitle.ifBlank { null },
                                                employer = bio.employer.ifBlank { null },
                                                employmentSector = bio.employmentSector.ifBlank { null },
                                                monthlyIncomeAmount = bio.monthlyIncomeAmount.toIntOrNull(),
                                                monthlyIncomeCurrency = bio.monthlyIncomeCurrency.ifBlank { null },
                                                fatherName = bio.fatherName.ifBlank { null },
                                                fatherOccupation = bio.fatherOccupation.ifBlank { null },
                                                motherName = bio.motherName.ifBlank { null },
                                                motherOccupation = bio.motherOccupation.ifBlank { null },
                                                elderBrothersCount = bio.elderBrothers.toIntOrNull(),
                                                youngerBrothersCount = bio.youngerBrothers.toIntOrNull(),
                                                elderSistersCount = bio.elderSisters.toIntOrNull(),
                                                youngerSistersCount = bio.youngerSisters.toIntOrNull(),
                                                nativity = bio.nativity.ifBlank { null },
                                                familyType = bio.familyType.ifBlank { null },
                                                familyValues = bio.familyValues.ifBlank { null },
                                                financialStatus = bio.financialStatus.ifBlank { null },
                                                citizenshipStatus = bio.citizenshipStatus.ifBlank { null },
                                            ),
                                        )
                                        saved = true
                                    } catch (e: Exception) {
                                        error = friendlyError(e, "Couldn't save bio data.")
                                    } finally {
                                        saving = false
                                    }
                                }
                            },
                        )
                    }

                    "about" -> {
                        // Visibility of system status (Norman) -- a long form
                        // with no sense of "how much is left" reads as a
                        // chore. Text only, no progress bar: this codebase
                        // already tried a bar+caption for onboarding and
                        // dropped it for a named Stepper because a filling
                        // bar reads as "something is loading" (see Stepper.kt's
                        // own doc comment) -- same reasoning applies here.
                        val aboutMeFieldsFilled = listOf(
                            aboutMe.selectedPrompts.isNotEmpty(),
                            aboutMe.fitnessRoutine.isNotBlank(), aboutMe.sleepSchedule.isNotBlank(),
                            aboutMe.pets.isNotBlank(), aboutMe.socialEnergy.isNotBlank(),
                            aboutMe.partnerQualities.isNotEmpty(), aboutMe.whatMattersMost.isNotBlank(),
                            aboutMe.careerAfterMarriage.isNotBlank(), aboutMe.livingArrangement.isNotBlank(), aboutMe.openToRelocation.isNotBlank(),
                            aboutMe.favoriteCuisine.isNotBlank(), aboutMe.dreamDestination.isNotBlank(), aboutMe.loveLanguage.isNotBlank(),
                        ).count { it }
                        FieldHint("$aboutMeFieldsFilled of 13 filled in")

                        SectionCard(title = "About me prompts", padding = 16.dp) {
                            FieldHint("Candidates see these first -- pick up to $MAX_ABOUT_PROMPTS and answer honestly.")

                            if (aboutMe.selectedPrompts.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                                    aboutMe.selectedPrompts.forEach { prompt ->
                                        // Plain SectionCard title, not a pill
                                        // -- a pill here read as "just
                                        // another bubble" next to the
                                        // closed-set fields' answer bubbles
                                        // elsewhere in this form, the
                                        // opposite of distinct ("I don't
                                        // even understand what they are" /
                                        // "for custom answers, I don't want
                                        // bubbles for questions", reported
                                        // directly). The bordered textarea
                                        // right below already signals "this
                                        // is where you type".
                                        SectionCard(
                                            title = prompt,
                                            padding = 14.dp,
                                            trailing = { LinkText("Remove", onClick = { removePrompt(prompt) }) },
                                        ) {
                                            D2MTextArea(
                                                label = "",
                                                value = aboutMe.promptAnswers[prompt] ?: "",
                                                onValueChange = { ans -> updateAboutMe { copy(promptAnswers = promptAnswers + (prompt to ans)) } },
                                                minLines = 2,
                                                hint = "${(aboutMe.promptAnswers[prompt] ?: "").length}/300",
                                            )
                                        }
                                    }
                                }
                            }

                            if (aboutMe.selectedPrompts.size < MAX_ABOUT_PROMPTS) {
                                if (promptPickerOpen) {
                                    Column(modifier = Modifier.padding(top = 12.dp)) {
                                        // Reused as a tap-one-to-pick list, not a
                                        // multi-select -- `value` stays empty (no
                                        // chip should ever look "selected" here,
                                        // since picking one removes it from this
                                        // list immediately) and `onChange` always
                                        // hands back a single-item add-diff, which
                                        // is exactly the prompt that was tapped.
                                        D2MChipGroup(
                                            label = null,
                                            options = Taxonomy.ABOUT_ME_PROMPTS.filter { it !in aboutMe.selectedPrompts },
                                            value = emptyList(),
                                            onChange = { next -> next.firstOrNull()?.let(::addPrompt) },
                                        )
                                        if (aboutMe.selectedPrompts.isNotEmpty()) {
                                            LinkText("Cancel", onClick = { promptPickerOpen = false }, modifier = Modifier.padding(top = 8.dp))
                                        }
                                    }
                                } else {
                                    D2MButton(
                                        text = "+ Add a prompt (${aboutMe.selectedPrompts.size}/$MAX_ABOUT_PROMPTS)",
                                        variant = D2MButtonVariant.OUTLINE,
                                        onClick = { promptPickerOpen = true },
                                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                    )
                                }
                            }
                        }

                        SectionCard(title = "Lifestyle", padding = 16.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                D2MOptionGroup("Fitness routine", Taxonomy.FITNESS_ROUTINES, aboutMe.fitnessRoutine, { v -> updateAboutMe { copy(fitnessRoutine = if (v == fitnessRoutine) "" else v) } }, shape = D2MChipShape.PILL)
                                D2MOptionGroup("Sleep schedule", Taxonomy.SLEEP_SCHEDULES, aboutMe.sleepSchedule, { v -> updateAboutMe { copy(sleepSchedule = if (v == sleepSchedule) "" else v) } }, shape = D2MChipShape.PILL)
                                D2MOptionGroup("Pets", Taxonomy.PET_PREFERENCES, aboutMe.pets, { v -> updateAboutMe { copy(pets = if (v == pets) "" else v) } }, shape = D2MChipShape.PILL)
                                D2MOptionGroup("Social energy", Taxonomy.SOCIAL_ENERGIES, aboutMe.socialEnergy, { v -> updateAboutMe { copy(socialEnergy = if (v == socialEnergy) "" else v) } }, shape = D2MChipShape.PILL)
                            }
                        }

                        SectionCard(title = "What I'm looking for", padding = 16.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                D2MChipGroup(
                                    "What I value in a partner",
                                    Taxonomy.PARTNER_QUALITIES,
                                    aboutMe.partnerQualities,
                                    { updateAboutMe { copy(partnerQualities = it) } },
                                )
                                Column {
                                    FieldLabel("What matters most to me")
                                    D2MTextArea("", aboutMe.whatMattersMost, { updateAboutMe { copy(whatMattersMost = it) } }, minLines = 3)
                                    SuggestionChipGroup(Taxonomy.WHAT_MATTERS_MOST_SUGGESTIONS, aboutMe.whatMattersMost) { updateAboutMe { copy(whatMattersMost = it) } }
                                }
                            }
                        }

                        SectionCard(title = "Life & future plans", padding = 16.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                D2MOptionGroup("Career after marriage", Taxonomy.CAREER_AFTER_MARRIAGE_OPTIONS, aboutMe.careerAfterMarriage, { v -> updateAboutMe { copy(careerAfterMarriage = if (v == careerAfterMarriage) "" else v) } }, shape = D2MChipShape.PILL)
                                D2MOptionGroup("Living arrangement", Taxonomy.LIVING_ARRANGEMENTS, aboutMe.livingArrangement, { v -> updateAboutMe { copy(livingArrangement = if (v == livingArrangement) "" else v) } }, shape = D2MChipShape.PILL)
                                D2MOptionGroup("Open to relocation", Taxonomy.RELOCATION_PREFERENCES, aboutMe.openToRelocation, { v -> updateAboutMe { copy(openToRelocation = if (v == openToRelocation) "" else v) } }, shape = D2MChipShape.PILL)
                            }
                        }

                        SectionCard(title = "Quick facts", padding = 16.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                // Dropdown-filtered as you type, not a row of
                                // chips below the field ("show it as autofill
                                // suggestion rather than suggestion bubbles
                                // below", reported directly).
                                TextAutocomplete(
                                    label = "Favorite cuisine",
                                    value = aboutMe.favoriteCuisine,
                                    onValueChange = { updateAboutMe { copy(favoriteCuisine = it) } },
                                    options = Taxonomy.CUISINE_SUGGESTIONS,
                                )
                                // Same live Open-Meteo search as "Locations
                                // you'd accept a match from" below -- a real
                                // geocode lookup is both a better and a more
                                // literally exhaustive suggestion source than
                                // a hand-picked destination list could ever be
                                // ("provide the same suggestion option as
                                // locations", reported directly).
                                CityAutocomplete(
                                    label = "Dream destination",
                                    value = aboutMe.dreamDestination,
                                    geocodingApi = geocodingApi,
                                    onSelect = { r -> updateAboutMe { copy(dreamDestination = r.label) } },
                                    onRawTextChange = { text -> updateAboutMe { copy(dreamDestination = text) } },
                                    placeholder = "Search any city or country…",
                                )
                                D2MOptionGroup("Love language", Taxonomy.LOVE_LANGUAGES, aboutMe.loveLanguage, { v -> updateAboutMe { copy(loveLanguage = if (v == loveLanguage) "" else v) } }, shape = D2MChipShape.PILL)
                            }
                        }

                        error?.let { D2MErrorBanner(it) }
                        if (saved) Text("Saved.", color = MaterialTheme.colorScheme.primary)
                        D2MButton(
                            text = if (saving) "Saving…" else "Save About Me",
                            enabled = !saving,
                            onClick = {
                                val pid = primaryId ?: return@D2MButton
                                scope.launch {
                                    saving = true
                                    error = null
                                    try {
                                        identityRepo.updateAboutMe(
                                            pid,
                                            AboutMeDataIn(
                                                fitnessRoutine = aboutMe.fitnessRoutine.ifBlank { null },
                                                sleepSchedule = aboutMe.sleepSchedule.ifBlank { null },
                                                pets = aboutMe.pets.ifBlank { null },
                                                socialEnergy = aboutMe.socialEnergy.ifBlank { null },
                                                aboutPrompts = aboutMe.selectedPrompts.mapNotNull { prompt ->
                                                    val answer = aboutMe.promptAnswers[prompt]?.trim().orEmpty()
                                                    if (answer.isEmpty()) null else AboutMePromptEntry(prompt, answer)
                                                },
                                                partnerQualities = aboutMe.partnerQualities,
                                                whatMattersMost = aboutMe.whatMattersMost.ifBlank { null },
                                                careerAfterMarriage = aboutMe.careerAfterMarriage.ifBlank { null },
                                                livingArrangement = aboutMe.livingArrangement.ifBlank { null },
                                                openToRelocation = aboutMe.openToRelocation.ifBlank { null },
                                                favoriteCuisine = aboutMe.favoriteCuisine.ifBlank { null },
                                                dreamDestination = aboutMe.dreamDestination.ifBlank { null },
                                                loveLanguage = aboutMe.loveLanguage.ifBlank { null },
                                            ),
                                        )
                                        saved = true
                                    } catch (e: Exception) {
                                        error = friendlyError(e, "Couldn't save About Me.")
                                    } finally {
                                        saving = false
                                    }
                                }
                            },
                        )
                    }

                    else -> {
                        Text("Location preference", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        CityChipPicker("Locations you'd accept a match from", acceptLocations, { acceptLocations = it }, geocodingApi)
                        error?.let { D2MErrorBanner(it) }
                        D2MButton(
                            text = if (saving) "Saving…" else "Save preferences",
                            enabled = !saving,
                            onClick = {
                                val sid = resolvedSponsorId
                                scope.launch {
                                    saving = true
                                    error = null
                                    try {
                                        if (sid != null) {
                                            identityRepo.updateLocationPreference(sid, LocationPreferenceIn(acceptLocations, locationHardFilter))
                                        }
                                        saved = true
                                    } catch (e: Exception) {
                                        error = friendlyError(e, "Couldn't save preferences.")
                                    } finally {
                                        saving = false
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BioSectionHeader(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = mutedText(0.55f), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
}

private const val SUGGESTION_PREVIEW_COUNT = 8

// Suggestion lists are deliberately long now ("I want an exhaustive
// list", reported directly), which would otherwise turn a small field
// into a wall of bubbles by default -- progressive disclosure shows a
// short slice up front and reveals the rest behind one tap, same
// reasoning as d2m_web's SuggestionChips. `value` matching an option past
// the fold auto-expands so the current answer is never hidden behind a
// collapsed "show all." No other call site yet, so this stays local
// rather than a new shared component nobody else asked for.
@Composable
private fun SuggestionChipGroup(options: List<String>, value: String, onPick: (String) -> Unit) {
    val matchIndex = options.indexOf(value)
    var expanded by remember(options) { mutableStateOf(matchIndex >= SUGGESTION_PREVIEW_COUNT) }
    val visible = if (expanded) options else options.take(SUGGESTION_PREVIEW_COUNT)
    Column {
        D2MOptionGroup(label = null, options = visible, value = value, onChange = onPick, shape = D2MChipShape.PILL)
        if (options.size > SUGGESTION_PREVIEW_COUNT) {
            LinkText(
                text = if (expanded) "Show fewer" else "Show all (${options.size})",
                onClick = { expanded = !expanded },
            )
        }
    }
}

// A type-ahead dropdown against a fixed local list, not a row of chips
// underneath the field -- "show it as autofill suggestion rather than
// suggestion bubbles below" (reported directly). Mirrors CityAutocomplete
// (ui/components/CityAutocomplete.kt)'s own OutlinedTextField-plus-
// dropdown-Card look so the two read as one "autocomplete" idiom across
// this form, just backed by a plain in-memory list instead of a
// geocoding API call -- there's no live "cuisine" service to query, and
// CUISINE_SUGGESTIONS is short enough that filtering it on every
// keystroke needs no debounce.
@Composable
private fun TextAutocomplete(label: String, value: String, onValueChange: (String) -> Unit, options: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    val matches = (if (value.isBlank()) options else options.filter { it.contains(value, ignoreCase = true) }).take(8)

    Column {
        D2MTextField(
            label,
            value,
            { onValueChange(it); expanded = true },
        )
        if (expanded && matches.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                LazyColumn {
                    items(matches) { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = { onValueChange(opt); expanded = false },
                        )
                    }
                }
            }
        }
    }
}
