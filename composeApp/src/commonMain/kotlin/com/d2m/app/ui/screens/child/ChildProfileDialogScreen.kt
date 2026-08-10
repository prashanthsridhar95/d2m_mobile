package com.d2m.app.ui.screens.child

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.d2m.app.data.model.ChildPreferencesRequest
import com.d2m.app.data.model.ExtendedBioDataIn
import com.d2m.app.data.model.LocationPreferenceIn
import com.d2m.app.data.network.GeocodingApi
import com.d2m.app.data.network.friendlyError
import com.d2m.app.data.session.IdentityStore
import com.d2m.app.domain.repository.IdentityRepository
import com.d2m.app.ui.components.*
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

@Composable
fun ChildProfileDialogScreen(onClose: () -> Unit) {
    val identityStore: IdentityStore = koinInject()
    val identityRepo: IdentityRepository = koinInject()
    val geocodingApi: GeocodingApi = koinInject()
    val identity by identityStore.identity.collectAsState()
    val scope = rememberCoroutineScope()

    var tab by remember { mutableStateOf(0) } // 0 Profile, 1 Bio data, 2 Preferences
    var bio by remember { mutableStateOf(BioForm()) }
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
    }

    fun update(patch: BioForm.() -> BioForm) { bio = bio.patch(); saved = false }

    D2MTheme(flow = D2MFlow.CHILD) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(title = { Text("My profile") })
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Profile") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Bio data") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Preferences") })
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (tab) {
                    0 -> Text("Basic data and photos live in Settings today -- see plan §4 for the photo-picker follow-up.", color = mutedText(0.55f))

                    1 -> {
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

                    2 -> {
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
